package com.creditrepairai.v2.billing

import java.math.BigDecimal
import java.math.RoundingMode

enum class PriceRecurrence { RECURRING, LIMITED, ONCE, UNSUPPORTED }
data class PricePhase(
    val price: String, val micros: Long, val currency: String, val period: String,
    val cycles: Int, val recurrence: PriceRecurrence,
)
data class SubscriptionOption(
    val basePlanId: String, val offerId: String?, val token: String, val phases: List<PricePhase>,
)
data class PlanOffer(val offerToken: String, val phases: List<PricePhase>, val introductory: Boolean = false) {
    val price: String get() = phases.first().price
    val discount: String? get() = if (introductory) OfferPricing.percentOff(phases.first().micros, phases.last().micros) else null
    fun disclosure(plan: AccessPlan): String {
        if (plan == AccessPlan.CASE) return "$price once for the selected dispute. No automatic renewal."
        val unit = if (plan == AccessPlan.MONTHLY) "month" else "year"
        val renewal = phases.last().price
        if (!introductory) return "$price per $unit. Automatically renews at $renewal per $unit until canceled."
        val cycles = if (phases.first().recurrence == PriceRecurrence.ONCE) 1 else phases.first().cycles
        val duration = if (cycles == 1) "first $unit" else "first $cycles ${unit}s"
        return "$price per $unit for the $duration, then $renewal per $unit. Automatically renews until canceled."
    }
}

/** Only the named, finite launch offer or an unambiguous regular plan is supported.
 * Prices/eligibility always come from Play; missing offers never fabricate a discount. */
object OfferPricing {
    const val INTRO_OFFER_ID = "launch-intro"
    fun select(plan: AccessPlan, options: List<SubscriptionOption>): PlanOffer? {
        if (plan == AccessPlan.CASE) return null
        val baseId = if (plan == AccessPlan.MONTHLY) "monthly" else "yearly"
        val candidates = options.filter { option ->
            option.basePlanId == baseId && option.token.isNotBlank() && option.phases.all {
                it.price.isNotBlank() && it.micros > 0 && it.currency.matches(Regex("[A-Z]{3}")) && it.period == plan.period
            } && option.phases.lastOrNull()?.let {
                it.recurrence == PriceRecurrence.RECURRING && it.cycles == 0
            } == true
        }
        val intro = candidates.filter { option ->
            option.offerId == INTRO_OFFER_ID && option.phases.size == 2 && option.phases.first().let { first ->
                ((first.recurrence == PriceRecurrence.LIMITED && first.cycles == (if (plan == AccessPlan.MONTHLY) 3 else 1)) ||
                    (plan == AccessPlan.YEARLY && first.recurrence == PriceRecurrence.ONCE && first.cycles == 0)) &&
                    first.currency == option.phases.last().currency && first.micros < option.phases.last().micros
            }
        }
        if (intro.size > 1) return null
        intro.singleOrNull()?.let { return PlanOffer(it.token, it.phases.toList(), introductory = true) }
        val regular = candidates.singleOrNull { it.offerId == null && it.phases.size == 1 } ?: return null
        return PlanOffer(regular.token, regular.phases.toList())
    }

    fun percentOff(discounted: Long, regular: Long): String? {
        if (discounted <= 0 || regular <= discounted) return null
        return BigDecimal.valueOf(regular).subtract(BigDecimal.valueOf(discounted))
            .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(regular), 1, RoundingMode.DOWN)
            .stripTrailingZeros().toPlainString() + "%"
    }

    fun annualComparison(yearly: PlanOffer, monthly: PlanOffer): String? {
        val annual = yearly.phases.firstOrNull() ?: return null
        val month = monthly.phases.lastOrNull() ?: return null
        if (annual.period != "P1Y" || month.period != "P1M" || annual.currency != month.currency ||
            month.recurrence != PriceRecurrence.RECURRING || month.micros > Long.MAX_VALUE / 12) return null
        val savings = percentOff(annual.micros, month.micros * 12) ?: return null
        return "$savings less ${if (yearly.introductory) "for the first year " else "per year "}than 12 payments at the standard monthly rate. Annual payment is charged upfront."
    }
}
