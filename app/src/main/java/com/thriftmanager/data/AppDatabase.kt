package com.thriftmanager.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class, Supplier::class, Customer::class, Purchase::class,
        Sale::class, SaleLine::class, Expense::class, Payment::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
}
