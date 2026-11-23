package com.thriftmanager.data

data class BackupData(
    val formatVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val products: List<Product>,
    val suppliers: List<Supplier>,
    val customers: List<Customer>,
    val purchases: List<Purchase>,
    val sales: List<Sale>,
    val saleLines: List<SaleLine>,
    val expenses: List<Expense>,
    val payments: List<Payment>
)
