package com.thriftmanager.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.thriftmanager.data.BackupData
import com.thriftmanager.data.BusinessRepository
import com.thriftmanager.data.Customer
import com.thriftmanager.data.Expense
import com.thriftmanager.data.Payment
import com.thriftmanager.data.Product
import com.thriftmanager.data.Purchase
import com.thriftmanager.data.Supplier
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    val business: BusinessRepository,
    private val preferences: DataStore<Preferences>
) : ViewModel() {
    private val gson = Gson()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone = _onboardingDone.asStateFlow()
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")

    init {
        viewModelScope.launch {
            _onboardingDone.value = preferences.data
                .map { it[onboardingKey] ?: false }
                .first()
        }
    }

    fun finishOnboarding() = viewModelScope.launch {
        preferences.edit { it[onboardingKey] = true }
        _onboardingDone.value = true
    }

    fun saveProduct(product: Product) = action { business.saveProduct(product) }
    fun deleteProduct(id: Long) = action { business.deleteProduct(id) }
    fun saveExpense(expense: Expense) = action { business.addExpense(expense) }
    fun addSupplier(supplier: Supplier) = action { business.addSupplier(supplier) }
    fun addCustomer(customer: Customer) = action { business.addCustomer(customer) }
    fun savePurchase(purchase: Purchase) = action { business.savePurchase(purchase) }
    fun collectPayment(payment: Payment) = action { business.collectPayment(payment) }

    fun saveSale(
        product: Product,
        quantity: Int,
        total: Long,
        paid: Long,
        method: String,
        customerId: Long?
    ) = action { business.saveSale(product, quantity, total, paid, method, customerId) }

    fun saveSale(
        items: List<Pair<Product, Int>>,
        discount: Long,
        paid: Long,
        method: String,
        customerId: Long?
    ) = action { business.saveSale(items, discount, paid, method, customerId) }

    suspend fun backupJson(): String = gson.toJson(business.exportData())

    fun restoreJson(json: String) = action {
        val data = gson.fromJson(json, BackupData::class.java)
            ?: error("The selected file is not a valid Thrift Manager backup.")
        business.restore(data)
    }

    fun reportPeriods(): Pair<LongRange, LongRange> {
        val now = Calendar.getInstance()
        val today = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val month = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return (today until today + 86_400_000L) to (month until now.timeInMillis + 1)
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _message.value = "Saved successfully."
            } catch (error: Exception) {
                _message.value = error.message ?: "Unable to save this record."
            }
        }
    }
}
