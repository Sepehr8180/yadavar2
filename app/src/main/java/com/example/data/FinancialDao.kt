package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_items ORDER BY jalaliYear ASC, jalaliMonth ASC, jalaliDay ASC, id DESC")
    fun getAllFinancialItems(): Flow<List<FinancialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialItem(item: FinancialEntity): Long

    @Update
    suspend fun updateFinancialItem(item: FinancialEntity)

    @Delete
    suspend fun deleteFinancialItem(item: FinancialEntity)

    @Query("DELETE FROM financial_items WHERE id = :id")
    suspend fun deleteFinancialItemById(id: Long)
}
