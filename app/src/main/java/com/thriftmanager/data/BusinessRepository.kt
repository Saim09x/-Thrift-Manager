package com.thriftmanager.data

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class BusinessRepository @Inject constructor(private val database: AppDatabase) {
    private val dao = database.businessDao()

    val products = dao.products()
    val suppliers = dao.suppliers()
    val customers = dao.customers()
    val purchases = dao.purchases()
    val sales = dao.sales()
    val expenses = dao.expenses()
    val payments = dao.payments()

    suspend fun saleLines(saleId: Long) = dao.saleLines(saleId)

    fun salesTotal(from: Long, until: Long) = dao.salesTotal(from, until)
    fun collectedTotal(from: Long, until: Long) = dao.collectedTotal(from, until)
    fun expenseTotal(from: Long, until: Long) = dao.expenseTotal(from, until)
    fun grossProfit(from: Long, until: Long) = dao.grossProfit(from, until)
    val inventoryValue = dao.inventoryValue()
    val productCount = dao.productCount()
    val lowStockCount = dao.lowStockCount()
    val damagedStockCount = dao.damagedStockCount()
    val supplierDue = dao.supplierDue()
    val customerDue = dao.customerDue()

    suspend fun saveProduct(product: Product) {
        require(product.name.isNotBlank()) { "Product name is required." }
        require(product.sku.isNotBlank()) { "SKU is required." }
        require(product.purchasePrice >= 0 && product.sellingPrice >= 0) { "Prices cannot be negative." }
        require(product.quantity >= 0 && product.damagedQuantity >= 0) { "Stock cannot be negative." }
        if (product.id == 0L) {
            check(dao.productBySku(product.sku) == null) { "That SKU is already in use." }
            dao.insertProduct(product)
        } else {
            val duplicate = dao.productBySku(product.sku)
            check(duplicate == null || duplicate.id == product.id) { "That SKU is already in use." }
            dao.updateProduct(product)
        }
    }

    suspend fun deleteProduct(id: Long) = dao.deleteProduct(id)

    suspend fun saveSale(
        product: Product,
        quantity: Int,
        total: Long,
        paid: Long,
        paymentMethod: String,
        customerId: Long?
    ) {
        require(quantity > 0) { "Quantity must be greater than zero." }
        require(total >= 0 && paid in 0..total) { "Payment must be between zero and the sale total." }
        val current = checkNotNull(dao.product(product.id)) { "Product no longer exists." }
        dao.saveSale(
            Sale(
                customerId = customerId,
                totalAmount = total,
                paidAmount = paid,
                paymentMethod = paymentMethod
            ),
            listOf(
                SaleLine(
                    saleId = 0,
                    productId = current.id,
                    productName = current.name,
                    quantity = quantity,
                    unitPrice = total / quantity,
                    unitCost = current.purchasePrice
                )
            )
        )
    }

    suspend fun saveSale(
        items: List<Pair<Product, Int>>,
        discount: Long,
        paid: Long,
        paymentMethod: String,
        customerId: Long?
    ) {
        require(items.isNotEmpty()) { "Add at least one product to the sale." }
        require(items.all { it.second > 0 }) { "Quantities must be greater than zero." }
        val lines = items.map { (item, quantity) ->
            val current = checkNotNull(dao.product(item.id)) { "A product no longer exists." }
            SaleLine(
                saleId = 0,
                productId = current.id,
                productName = current.name,
                quantity = quantity,
                unitPrice = current.sellingPrice,
                unitCost = current.purchasePrice
            )
        }
        val total = lines.sumOf { it.unitPrice * it.quantity } - discount
        require(discount >= 0 && total >= 0) { "Discount cannot exceed the sale total." }
        require(paid in 0..total) { "Payment must be between zero and the sale total." }
        dao.saveSale(
            Sale(
                customerId = customerId,
                totalAmount = total,
                paidAmount = paid,
                discount = discount,
                paymentMethod = paymentMethod
            ),
            lines
        )
    }

    suspend fun savePurchase(purchase: Purchase) {
        require(purchase.totalCost >= 0 && purchase.paidAmount in 0..purchase.totalCost) {
            "Paid amount must be between zero and the purchase cost."
        }
        check(dao.product(purchase.productId) != null) { "Select an existing product." }
        dao.savePurchase(purchase)
    }

    suspend fun addExpense(expense: Expense) {
        require(expense.category.isNotBlank()) { "Choose an expense category." }
        require(expense.amount > 0) { "Expense amount must be greater than zero." }
        dao.insertExpense(expense)
    }

    suspend fun addSupplier(supplier: Supplier) {
        require(supplier.name.isNotBlank()) { "Supplier name is required." }
        dao.insertSupplier(supplier)
    }

    suspend fun addCustomer(customer: Customer) {
        require(customer.name.isNotBlank()) { "Customer name is required." }
        dao.insertCustomer(customer)
    }

    suspend fun collectPayment(payment: Payment) = dao.collectPayment(payment)

    suspend fun restore(data: BackupData) {
        require(data.formatVersion == 1) { "This backup version is not supported." }
        database.withTransaction {
            dao.clearSaleLines()
            dao.clearSales()
            dao.clearPurchases()
            dao.clearExpenses()
            dao.clearPayments()
            dao.clearProducts()
            dao.clearSuppliers()
            dao.clearCustomers()
            dao.restoreSuppliers(data.suppliers)
            dao.restoreCustomers(data.customers)
            dao.restoreProducts(data.products)
            dao.restorePurchases(data.purchases)
            dao.restoreSales(data.sales)
            dao.restoreSaleLines(data.saleLines)
            dao.restoreExpenses(data.expenses)
            dao.restorePayments(data.payments)
        }
    }

    suspend fun exportData(): BackupData = BackupData(
        products = products.firstValue(),
        suppliers = suppliers.firstValue(),
        customers = customers.firstValue(),
        purchases = purchases.firstValue(),
        sales = sales.firstValue(),
        saleLines = dao.allSaleLines(),
        expenses = expenses.firstValue(),
        payments = payments.firstValue()
    )

    private suspend fun <T> Flow<T>.firstValue(): T = first()
}
