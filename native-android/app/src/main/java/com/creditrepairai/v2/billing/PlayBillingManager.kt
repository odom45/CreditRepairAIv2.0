package com.creditrepairai.v2.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.creditrepairai.v2.BuildConfig
import com.creditrepairai.v2.ai.SecureService
import com.creditrepairai.v2.auth.SessionFence
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class BillingUiState(val price: String = "", val available: Boolean = false, val busy: Boolean = false, val message: String = "Refresh to check Google Play.", val entitlement: Entitlement = Entitlement())
class PlayBillingManager(context: Context, private val service: SecureService, private val subject: () -> String?) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(BillingUiState())
    val ui: StateFlow<BillingUiState> = state.asStateFlow()
    private var connecting = false
    private val sessionFence = SessionFence(subject())
    private var statusJob: Job? = null
    private val verifying = mutableSetOf<String>()
    private val client = BillingClient.newBuilder(context).enableAutoServiceReconnection()
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> process(purchases.orEmpty())
                BillingClient.BillingResponseCode.USER_CANCELED -> state.update { it.copy(busy = false, message = "Purchase canceled. No new access was granted.") }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
                else -> state.update { it.copy(busy = false, message = "Google Play purchase did not complete. Refresh or try again.") }
            }
        }.build()
    fun refresh() {
        val uid = subject() ?: return
        if (sessionFence.ticket().owner != uid) reset()
        val ticket = sessionFence.ticket()
        if (statusJob?.isActive != true) statusJob = scope.launch {
            runCatching { service.post("/billing/status", uid) }.onSuccess { json ->
                if (sessionFence.accepts(ticket, subject())) state.update { it.copy(entitlement = decode(json)) }
            }.onFailure {
                if (it is CancellationException) throw it
                if (sessionFence.accepts(ticket, subject())) state.update { it.copy(entitlement = Entitlement(), message = "Subscription verification unavailable. Refresh when connected.") }
            }
        }
        if (client.isReady) query(ticket) else if (!connecting) {
            connecting = true
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (!sessionFence.accepts(ticket, subject())) return
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) query(ticket)
                    else state.update { it.copy(available = false, busy = false, message = "Google Play Billing is unavailable for this build/account. Local tools remain available.") }
                }
                override fun onBillingServiceDisconnected() { connecting = false; state.update { it.copy(available = false, message = "Google Play disconnected. Refresh to retry.") } }
            })
        }
    }
    private fun query(ticket: SessionFence.Ticket) {
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PLAY_PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build())).build()) { result, details ->
            if (!sessionFence.accepts(ticket, subject())) return@queryProductDetailsAsync
            val offer = details.productDetailsList.firstOrNull()?.subscriptionOfferDetails?.firstOrNull { it.offerId == null && it.pricingPhases.pricingPhaseList.singleOrNull()?.billingPeriod == "P1M" }
            state.update { it.copy(available = result.responseCode == BillingClient.BillingResponseCode.OK && offer != null,
                price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice.orEmpty(), message = if (offer == null) "Monthly plan is not configured in Play for this build/account." else "Monthly software access. Cancel anytime in Google Play. Results are not guaranteed.") }
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            if (sessionFence.accepts(ticket, subject()) && result.responseCode == BillingClient.BillingResponseCode.OK) process(purchases)
        }
    }
    fun purchase(activity: Activity) {
        val uid = subject() ?: return
        val ticket = sessionFence.ticket()
        if (!sessionFence.accepts(ticket, uid)) return
        if (!BuildConfig.PAID_ACCESS_ENABLED || state.value.busy || state.value.entitlement.hasAccess(System.currentTimeMillis())) return
        state.update { it.copy(busy = true) }
        // Fetch fresh details immediately before launch, not a stale cached ProductDetails.
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PLAY_PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build())).build()) { result, details ->
            if (!sessionFence.accepts(ticket, subject())) return@queryProductDetailsAsync
            val product = details.productDetailsList.firstOrNull()
            val offer = product?.subscriptionOfferDetails?.firstOrNull { it.offerId == null && it.pricingPhases.pricingPhaseList.singleOrNull()?.billingPeriod == "P1M" }
            if (result.responseCode != BillingClient.BillingResponseCode.OK || product == null || offer == null) {
                state.update { it.copy(busy = false, message = "Monthly plan unavailable. Refresh to retry.") }
            } else scope.launch {
                if (!sessionFence.accepts(ticket, subject())) return@launch
                val launch = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setObfuscatedAccountId(BillingPolicy.accountId(uid))
                    .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build())).build())
                if (launch.responseCode != BillingClient.BillingResponseCode.OK) state.update { it.copy(busy = false, message = "Purchase could not start. No new access granted.") }
            }
        }
    }
    private fun process(purchases: List<Purchase>) {
        val uid = subject() ?: return
        val ticket = sessionFence.ticket()
        if (!sessionFence.accepts(ticket, uid)) return
        purchases.forEach { purchase ->
            when (BillingPolicy.disposition(BuildConfig.PLAY_PRODUCT_ID in purchase.products, purchase.purchaseState == Purchase.PurchaseState.PURCHASED, purchase.purchaseState == Purchase.PurchaseState.PENDING)) {
                PurchaseDisposition.IGNORE -> Unit
                PurchaseDisposition.PENDING -> state.update { it.copy(busy = false, message = "Payment pending. Access is not granted until verified.") }
                PurchaseDisposition.VERIFY -> if (verifying.add(purchase.purchaseToken)) scope.launch {
                    try {
                        service.post("/billing/verify", uid, JSONObject().put("purchaseToken", purchase.purchaseToken))
                        val aggregate = service.post("/billing/status", uid)
                        if (sessionFence.accepts(ticket, subject())) state.update { it.copy(busy = false, entitlement = decode(aggregate), message = "Subscription checked with Google Play.") }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        if (sessionFence.accepts(ticket, subject())) state.update { it.copy(busy = false, entitlement = Entitlement(), message = "Verification unavailable. Refresh to retry; no unverified access granted.") }
                    } finally { verifying.remove(purchase.purchaseToken) }
                }
            }
        }
    }
    private fun decode(json: JSONObject) = Entitlement(json.optBoolean("active"), json.optLong("expiresAt"), json.optString("state", "Not subscribed"))
    fun reset() { sessionFence.invalidate(subject()); scope.coroutineContext.cancelChildren(); verifying.clear(); state.value = BillingUiState() }
    fun close() { client.endConnection(); scope.cancel() }
}
