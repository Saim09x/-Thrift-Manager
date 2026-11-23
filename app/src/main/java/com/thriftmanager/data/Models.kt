package com.thriftmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val category: String = "",
    val brand: String = "",
    val size: String = "",
    val condition: String = "Good",
    val color: String = "",
    val purchasePrice: Long,
    val sellingPrice: Long,
    val quantity: Int,
    val damagedQuantity: Int = 0,
    val lowStockAt: Int = 2,
    val supplierId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = ""
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = ""
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long? = null,
    val invoice: String = "",
    val productId: Long,
    val quantity: Int,
    val totalCost: Long,
    val paidAmount: Long,
    val transportCost: Long = 0,
    val otherCharges: Long = 0,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long? = null,
    val totalAmount: Long,
    val paidAmount: Long,
    val discount: Long = 0,
    val paymentMethod: String,
    val soldAt: Long = System.currentTimeMillis(),
    val cancelled: Boolean = false
)

@Entity(tableName = "sale_lines", primaryKeys = ["saleId", "productId"])
data class SaleLine(
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val unitCost: Long
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String = "",
    val amount: Long,
    val spentAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyType: String,
    val partyId: Long,
    val amount: Long,
    val note: String = "",
    val paidAt: Long = System.currentTimeMillis()
)
