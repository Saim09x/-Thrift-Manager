package com.thriftmanager.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BusinessDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: BusinessDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.businessDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saleDeductsStockAndStoresCostSnapshotAtomically() = runBlocking {
        val id = dao.insertProduct(
            Product(name = "Vintage coat", sku = "COAT-1", purchasePrice = 800, sellingPrice = 1_500, quantity = 3)
        )
        val saleId = dao.saveSale(
            Sale(totalAmount = 3_000, paidAmount = 1_000, paymentMethod = "Credit"),
            listOf(
                SaleLine(
                    saleId = 0, productId = id, productName = "Vintage coat",
                    quantity = 2, unitPrice = 1_500, unitCost = 800
                )
            )
        )

        assertEquals(1, dao.product(id)?.quantity)
        assertEquals(1_600L, dao.saleLines(saleId).single().unitCost * dao.saleLines(saleId).single().quantity)
        assertEquals(1_400L, dao.grossProfit(0, Long.MAX_VALUE).first())
    }

    @Test
    fun insufficientStockRollsBackSaleAndStockChanges() = runBlocking {
        val id = dao.insertProduct(
            Product(name = "Jacket", sku = "J-1", purchasePrice = 1_000, sellingPrice = 2_000, quantity = 1)
        )
        try {
            dao.saveSale(
                Sale(totalAmount = 4_000, paidAmount = 4_000, paymentMethod = "Cash"),
                listOf(SaleLine(0, id, "Jacket", 2, 2_000, 1_000))
            )
            throw AssertionError("Expected the transaction to reject overselling.")
        } catch (_: IllegalStateException) {
            assertEquals(1, dao.product(id)?.quantity)
            assertTrue(dao.sales().first().isEmpty())
        }
    }
}
