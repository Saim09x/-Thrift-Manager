package com.thriftmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculatorTest {
    @Test
    fun grossAndNetProfitUsePersistedRevenueCostsAndExpenses() {
        val gross = FinancialCalculator.grossProfit(revenue = 12_500, costOfGoodsSold = 7_000)
        assertEquals(5_500, gross)
        assertEquals(3_000, FinancialCalculator.netOperatingProfit(gross, operatingExpenses = 2_500))
    }

    @Test
    fun outstandingBalanceReflectsPartialPayment() {
        assertEquals(3_750, FinancialCalculator.outstanding(total = 10_000, paid = 6_250))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPaymentAboveTotal() {
        FinancialCalculator.outstanding(total = 100, paid = 101)
    }
}
