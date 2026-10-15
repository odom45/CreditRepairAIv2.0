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

data class PlanOffer(val price: String, val offerToken: String)
data class BillingUiState(
    val offers: Map<AccessPlan, PlanOffer> = emptyMap(), val busy: Boolean = false,
    val message: String = "Refresh to check Google Play.", val entitlement: Entitlement = Entitlement(),
)
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
                if (sessionFence.accepts(ticket, subject())) state.update { it.copy(entitlement = Entitlement(), message = "Access verification unavailable. Refresh when connected.") }
            }
        }
        if (client.isReady) query(ticket) else if (!connecting) {
            connecting = true
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (!sessionFence.accepts(ticket, subject())) return
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) query(ticket)
                    else state.update { it.copy(offers = emptyMap(), busy = false, message = "Google Play Billing is unavailable for this build/account. Local tools remain available.") }
                }
                override fun onBillingServiceDisconnected() { connecting = false; state.update { it.copy(offers = emptyMap(), busy = false, message = "Google Play disconnected. Refresh to retry.") } }
            })
        }
    }
    private fun params(plan: AccessPlan) = QueryProductDetailsParams.Product.newBuilder()
        .setProductId(plan.productId).setProductType(if (plan == AccessPlan.CASE) BillingClient.ProductType.INAPP else BillingClient.ProductType.SUBS).build()
    private fun offer(product: ProductDetails, plan: AccessPlan): PlanOffer? {
        if (plan == AccessPlan.CASE) {
            // Only standard buy options; no rental/preorder/discount promises.
            val selected = product.oneTimePurchaseOfferDetailsList?.singleOrNull {
                it.offerId == null && it.rentalDetails == null && it.preorderDetails == null
            } ?: return null
            val token = selected.offerToken?.takeIf { it.isNotBlank() } ?: return null
            return PlanOffer(selected.formattedPrice, token)
        }
        val selected = product.subscriptionOfferDetails?.singleOrNull {
            it.offerId == null && it.pricingPhases.pricingPhaseList.singleOrNull()?.let { phase ->
                phase.billingPeriod == plan.period && phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
            } == true
        } ?: return null
        return PlanOffer(selected.pricingPhases.pricingPhaseList.single().formattedPrice, selected.offerToken)
    }
    private fun query(ticket: SessionFence.Ticket) {
        AccessPlan.entries.forEach { plan ->
            client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(params(plan))).build()) { result, details ->
                if (!sessionFence.accepts(ticket, subject())) return@queryProductDetailsAsync
                val selected = details.productDetailsList.singleOrNull { it.productId == plan.productId }?.let { offer(it, plan) }
                state.update { current -> current.copy(offers = current.offers - plan +
                    (if (result.responseCode == BillingClient.BillingResponseCode.OK && selected != null) mapOf(plan to selected) else emptyMap()),
                    message = "Prices and eligibility come from Google Play. Restore to verify existing access.") }
            }
        }
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach { type ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (sessionFence.accepts(ticket, subject()) && result.responseCode == BillingClient.BillingResponseCode.OK) process(purchases)
            }
        }
    }
    fun purchase(activity: Activity, plan: AccessPlan, disputeId: String? = null) {
        val uid = subject() ?: return
        val ticket = sessionFence.ticket()
        if (!sessionFence.accepts(ticket, uid) || !BuildConfig.PAID_ACCESS_ENABLED || state.value.busy) return
        if (!client.isReady) { refresh(); return }
        if (state.value.entitlement.hasAccess(System.currentTimeMillis())) return // Avoid simultaneous subscriptions/duplicate case purchase.
        if (plan == AccessPlan.CASE && (disputeId.isNullOrBlank() || state.value.entitlement.hasCaseAccess(uid, disputeId, System.currentTimeMillis()))) return
        val displayedOffer = state.value.offers[plan] ?: return
        state.update { it.copy(busy = true) }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(params(plan))).build()) { result, details ->
            if (!sessionFence.accepts(ticket, subject())) return@queryProductDetailsAsync
            val product = details.productDetailsList.singleOrNull { it.productId == plan.productId }
            val selected = product?.let { offer(it, plan) }
            if (result.responseCode != BillingClient.BillingResponseCode.OK || product == null || selected == null || selected != displayedOffer) {
                state.update { it.copy(busy = false, message = "Plan price or availability changed. Refresh and review before purchasing.") }
            } else scope.launch {
                if (!sessionFence.accepts(ticket, subject())) return@launch
                val flow = BillingFlowParams.newBuilder().setObfuscatedAccountId(BillingPolicy.accountId(uid))
                    .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(selected.offerToken).build()))
                if (plan == AccessPlan.CASE) flow.setObfuscatedProfileId(BillingPolicy.caseId(uid, requireNotNull(disputeId)))
                val launch = client.launchBillingFlow(activity, flow.build())
                if (launch.responseCode != BillingClient.BillingResponseCode.OK) state.update { it.copy(busy = false, message = "Purchase could not start. No new access granted.") }
            }
        }
    }
    private fun process(purchases: List<Purchase>) {
        val uid = subject() ?: return
        val ticket = sessionFence.ticket()
        if (!sessionFence.accepts(ticket, uid)) return
        purchases.forEach { purchase ->
            val product = purchase.products.singleOrNull()?.takeIf { id -> AccessPlan.entries.any { it.productId == id } }
            when (BillingPolicy.disposition(product != null, purchase.purchaseState == Purchase.PurchaseState.PURCHASED, purchase.purchaseState == Purchase.PurchaseState.PENDING)) {
                PurchaseDisposition.IGNORE -> state.update { it.copy(busy = false) }
                PurchaseDisposition.PENDING -> state.update { it.copy(busy = false, message = "Payment pending. Access is not granted until verified.") }
                PurchaseDisposition.VERIFY -> if (verifying.add(purchase.purchaseToken)) scope.launch {
                    try {
                        service.post("/billing/verify", uid, JSONObject().put("purchaseToken", purchase.purchaseToken).put("productId", product))
                        val aggregate = service.post("/billing/status", uid)
                        if (sessionFence.accepts(ticket, subject())) state.update { it.copy(busy = false, entitlement = decode(aggregate), message = "Access checked with Google Play.") }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        if (sessionFence.accepts(ticket, subject())) state.update { it.copy(busy = false, entitlement = Entitlement(), message = "Verification unavailable. Restore to retry; no unverified access granted.") }
                    } finally { verifying.remove(purchase.purchaseToken) }
                }
            }
        }
        if (purchases.isEmpty()) state.update { it.copy(busy = false) }
    }
    private fun decode(json: JSONObject): Entitlement {
        val hashes = json.optJSONArray("caseHashes")
        return Entitlement(json.optBoolean("active"), json.optLong("expiresAt"), json.optString("state", "Not subscribed"),
            (0 until (hashes?.length() ?: 0)).mapNotNull { hashes?.optString(it)?.takeIf { value -> value.matches(Regex("[a-f0-9]{64}")) } }.toSet())
    }
    fun reset() { sessionFence.invalidate(subject()); scope.coroutineContext.cancelChildren(); verifying.clear(); state.value = BillingUiState() }
    fun close() { client.endConnection(); scope.cancel() }
}
