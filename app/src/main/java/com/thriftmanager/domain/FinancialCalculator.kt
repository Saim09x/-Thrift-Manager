package com.thriftmanager.domain

object FinancialCalculator {
    fun grossProfit(revenue: Long, costOfGoodsSold: Long): Long {
        require(revenue >= 0 && costOfGoodsSold >= 0)
        return revenue - costOfGoodsSold
    }

    fun netOperatingProfit(grossProfit: Long, operatingExpenses: Long): Long {
        require(operatingExpenses >= 0)
        return grossProfit - operatingExpenses
    }

    fun outstanding(total: Long, paid: Long): Long {
        require(total >= 0 && paid in 0..total)
        return total - paid
    }
}
