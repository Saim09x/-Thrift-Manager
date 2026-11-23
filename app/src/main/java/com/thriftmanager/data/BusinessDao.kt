package com.thriftmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun products(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun product(id: Long): Product?

    @Query("SELECT * FROM suppliers ORDER BY name")
    fun suppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM customers ORDER BY name")
    fun customers(): Flow<List<Customer>>

    @Query("SELECT * FROM purchases ORDER BY purchasedAt DESC")
    fun purchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM sales ORDER BY soldAt DESC")
    fun sales(): Flow<List<Sale>>

    @Query("SELECT * FROM expenses ORDER BY spentAt DESC")
    fun expenses(): Flow<List<Expense>>

    @Query("SELECT * FROM payments ORDER BY paidAt DESC")
    fun payments(): Flow<List<Payment>>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId")
    suspend fun saleLines(saleId: Long): List<SaleLine>

    @Query("SELECT * FROM sale_lines")
    suspend fun allSaleLines(): List<SaleLine>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE cancelled = 0 AND soldAt >= :from AND soldAt < :until")
    fun salesTotal(from: Long, until: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM sales WHERE cancelled = 0 AND soldAt >= :from AND soldAt < :until")
    fun collectedTotal(from: Long, until: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE spentAt >= :from AND spentAt < :until")
    fun expenseTotal(from: Long, until: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(s.totalAmount - (SELECT COALESCE(SUM(l.unitCost * l.quantity), 0) FROM sale_lines l WHERE l.saleId = s.id)), 0) FROM sales s WHERE s.cancelled = 0 AND s.soldAt >= :from AND s.soldAt < :until")
    fun grossProfit(from: Long, until: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM((quantity + damagedQuantity) * purchasePrice), 0) FROM products")
    fun inventoryValue(): Flow<Long>

    @Query("SELECT COUNT(*) FROM products")
    fun productCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE quantity <= lowStockAt")
    fun lowStockCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(damagedQuantity), 0) FROM products")
    fun damagedStockCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalCost - paidAmount), 0) FROM purchases")
    fun supplierDue(): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalAmount - paidAmount), 0) FROM sales WHERE cancelled = 0")
    fun customerDue(): Flow<Long>

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun productBySku(sku: String): Product?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Insert
    suspend fun insertSupplier(supplier: Supplier): Long

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Insert
    suspend fun insertPayment(payment: Payment)

    @Query("UPDATE sales SET paidAmount = paidAmount + :amount WHERE id = :id AND paidAmount + :amount <= totalAmount")
    suspend fun collectCustomerPayment(id: Long, amount: Long): Int

    @Query("UPDATE purchases SET paidAmount = paidAmount + :amount WHERE id = :id AND paidAmount + :amount <= totalCost")
    suspend fun recordSupplierPayment(id: Long, amount: Long): Int

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleLines(lines: List<SaleLine>)

    @Insert
    suspend fun insertPurchase(purchase: Purchase): Long

    @Query("UPDATE products SET purchasePrice = CASE WHEN quantity + :quantity > 0 THEN (purchasePrice * quantity + :unitCost * :quantity) / (quantity + :quantity) ELSE :unitCost END, quantity = quantity + :quantity WHERE id = :productId")
    suspend fun receiveStock(productId: Long, quantity: Int, unitCost: Long)

    @Query("UPDATE products SET quantity = quantity - :quantity WHERE id = :productId AND quantity >= :quantity")
    suspend fun deductStock(productId: Long, quantity: Int): Int

    @Transaction
    suspend fun saveSale(sale: Sale, lines: List<SaleLine>): Long {
        require(lines.isNotEmpty()) { "Add at least one product to the sale." }
        for (line in lines) {
            require(line.quantity > 0) { "Quantity must be greater than zero." }
            check(deductStock(line.productId, line.quantity) == 1) {
                "Not enough stock for ${line.productName}."
            }
        }
        val saleId = insertSale(sale)
        insertSaleLines(lines.map { it.copy(saleId = saleId) })
        return saleId
    }

    @Transaction
    suspend fun savePurchase(purchase: Purchase): Long {
        require(purchase.quantity > 0) { "Quantity must be greater than zero." }
        val purchaseId = insertPurchase(purchase)
        receiveStock(purchase.productId, purchase.quantity, purchase.totalCost / purchase.quantity)
        return purchaseId
    }

    @Transaction
    suspend fun collectPayment(payment: Payment) {
        require(payment.amount > 0) { "Payment must be greater than zero." }
        val updated = when (payment.partyType) {
            "customer" -> collectCustomerPayment(payment.partyId, payment.amount)
            "supplier" -> recordSupplierPayment(payment.partyId, payment.amount)
            else -> error("Unsupported payment type.")
        }
        check(updated == 1) { "Payment exceeds the outstanding balance or record does not exist." }
        insertPayment(payment)
    }

    @Query("DELETE FROM sale_lines")
    suspend fun clearSaleLines()

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM purchases")
    suspend fun clearPurchases()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM payments")
    suspend fun clearPayments()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM suppliers")
    suspend fun clearSuppliers()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Insert
    suspend fun restoreProducts(rows: List<Product>)

    @Insert
    suspend fun restoreSuppliers(rows: List<Supplier>)

    @Insert
    suspend fun restoreCustomers(rows: List<Customer>)

    @Insert
    suspend fun restorePurchases(rows: List<Purchase>)

    @Insert
    suspend fun restoreSales(rows: List<Sale>)

    @Insert
    suspend fun restoreSaleLines(rows: List<SaleLine>)

    @Insert
    suspend fun restoreExpenses(rows: List<Expense>)

    @Insert
    suspend fun restorePayments(rows: List<Payment>)
}
