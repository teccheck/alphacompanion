package xyz.teccheck.alphacompanion.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "cameras")
data class SonyCamera(
    @PrimaryKey @ColumnInfo("mac_address") val macAddress: String,
    @ColumnInfo("association_id") val associationId: Int,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("model_version") val modelVersion: Short,
    @ColumnInfo("model_code") val modeCode: String,
)
