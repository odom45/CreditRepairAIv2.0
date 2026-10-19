package com.creditrepairai.v2.billing

import org.junit.Assert.*
import org.junit.Test

class OfferPricingTest {
    private fun regular(plan: AccessPlan = AccessPlan.MONTHLY) = SubscriptionOption(
        if (plan == AccessPlan.MONTHLY) "monthly" else "yearly", null, "base-token",
        listOf(PricePhase(if (plan == AccessPlan.MONTHLY) "$30.00" else "$249.99",
            if (plan == AccessPlan.MONTHLY) 30_000_000 else 249_990_000, "USD", plan.period, 0, PriceRecurrence.RECURRING)),
    )
    private fun intro(plan: AccessPlan = AccessPlan.MONTHLY): SubscriptionOption {
        val base = regular(plan)
        return base.copy(offerId = "launch-intro", token = "intro-token", phases = listOf(
            PricePhase(if (plan == AccessPlan.MONTHLY) "$24.99" else "$199.99",
                if (plan == AccessPlan.MONTHLY) 24_990_000 else 199_990_000, "USD", plan.period,
                if (plan == AccessPlan.MONTHLY) 3 else 1, PriceRecurrence.LIMITED), base.phases.single()))
    }
    @Test fun eligibleLaunchOfferDisclosesDiscountDurationAndFullRenewal() {
        val offer = requireNotNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(regular(), intro())))
        assertEquals("intro-token", offer.offerToken)
        assertEquals("16.7%", offer.discount)
        assertEquals("$24.99 per month for the first 3 months, then $30.00 per month. Automatically renews until canceled.", offer.disclosure(AccessPlan.MONTHLY))
        assertEquals("20%", OfferPricing.select(AccessPlan.YEARLY, listOf(intro(AccessPlan.YEARLY)))?.discount)
    }
    @Test fun ineligibleCustomerGetsRegularTermsWithoutDiscountClaim() {
        val offer = requireNotNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(regular())))
        assertFalse(offer.introductory)
        assertNull(offer.discount)
        assertEquals("$30.00 per month. Automatically renews at $30.00 per month until canceled.", offer.disclosure(AccessPlan.MONTHLY))
        assertNull(OfferPricing.select(AccessPlan.MONTHLY, emptyList()))
    }
    @Test fun unsupportedOrMalformedPhasesCannotBecomeAPromisedIntro() {
        val expected = intro()
        val first = expected.phases.first()
        val bad = listOf(expected.copy(offerId = "unknown"), expected.copy(basePlanId = "other"),
            expected.copy(token = ""), expected.copy(phases = emptyList()),
            expected.copy(phases = listOf(first.copy(cycles = 2), expected.phases.last())),
            expected.copy(phases = listOf(first.copy(currency = "EUR"), expected.phases.last())),
            expected.copy(phases = listOf(first.copy(micros = 0), expected.phases.last())),
            expected.copy(phases = listOf(first.copy(micros = 31_000_000), expected.phases.last())),
            expected.copy(phases = listOf(first.copy(period = "P3M"), expected.phases.last())),
            expected.copy(phases = listOf(first, first, expected.phases.last())),
            expected.copy(phases = listOf(first, expected.phases.last().copy(recurrence = PriceRecurrence.ONCE))))
        bad.forEach { assertNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(it))) }
        assertNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(expected, expected.copy(token = "duplicate"))))
        assertNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(regular(), regular().copy(token = "duplicate"))))
    }
    @Test fun annualFirstPaymentCanBeNonRecurringBeforeAutoRenewal() {
        val yearly = intro(AccessPlan.YEARLY)
        val one = yearly.copy(phases = listOf(yearly.phases.first().copy(cycles = 0, recurrence = PriceRecurrence.ONCE), yearly.phases.last()))
        val offer = requireNotNull(OfferPricing.select(AccessPlan.YEARLY, listOf(one)))
        assertEquals("$199.99 per year for the first year, then $249.99 per year. Automatically renews until canceled.", offer.disclosure(AccessPlan.YEARLY))
        assertNull(OfferPricing.select(AccessPlan.YEARLY, listOf(one.copy(phases = listOf(
            one.phases.first().copy(recurrence = PriceRecurrence.UNSUPPORTED), one.phases.last())))))
    }
    @Test fun annualComparisonUsesStandardMonthlyRateAndMatchingCurrencies() {
        val yearly = requireNotNull(OfferPricing.select(AccessPlan.YEARLY, listOf(intro(AccessPlan.YEARLY))))
        val monthly = requireNotNull(OfferPricing.select(AccessPlan.MONTHLY, listOf(intro())))
        assertEquals("44.4% less for the first year than 12 payments at the standard monthly rate. Annual payment is charged upfront.", OfferPricing.annualComparison(yearly, monthly))
        assertNull(OfferPricing.annualComparison(yearly, monthly.copy(phases = monthly.phases.map { it.copy(currency = "EUR") })))
        assertNull(OfferPricing.percentOff(100, 100))
        assertNull(OfferPricing.percentOff(0, 100))
    }
    @Test fun changedRenewalTermsRequireCheckoutReviewEvenIfIntroPriceIsUnchanged() {
        val option = intro()
        val first = OfferPricing.select(AccessPlan.MONTHLY, listOf(option))
        val changed = OfferPricing.select(AccessPlan.MONTHLY, listOf(option.copy(phases = listOf(option.phases.first(), option.phases.last().copy(micros = 31_000_000, price = "$31.00")))))
        assertNotEquals(first, changed)
    }
}
