package com.immich.autodownloader.device

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DeviceInfoSnapshot(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val fingerprint: String,
    val bootloader: String,
    val buildId: String,
    val display: String,
    val host: String,
    val tags: String,
    val buildType: String,
    val buildUser: String,
    val supportedAbis: List<String>,
    val supported32BitAbis: List<String>,
    val supported64BitAbis: List<String>,
    val sdkInt: Int,
    val release: String,
    val codename: String,
    val incremental: String,
    val securityPatch: String?,
    val socManufacturer: String?,
    val socModel: String?,
    val sku: String?,
    val odmSku: String?,
    val appPackageName: String,
    val appVersionName: String,
    val appVersionCode: Long,
)

fun buildDeviceInfoSnapshot(context: Context): DeviceInfoSnapshot {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    return DeviceInfoSnapshot(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        board = Build.BOARD,
        hardware = Build.HARDWARE,
        fingerprint = Build.FINGERPRINT,
        bootloader = Build.BOOTLOADER,
        buildId = Build.ID,
        display = Build.DISPLAY,
        host = Build.HOST,
        tags = Build.TAGS,
        buildType = Build.TYPE,
        buildUser = Build.USER,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        supported32BitAbis = Build.SUPPORTED_32_BIT_ABIS.toList(),
        supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS.toList(),
        sdkInt = Build.VERSION.SDK_INT,
        release = Build.VERSION.RELEASE ?: "",
        codename = Build.VERSION.CODENAME ?: "",
        incremental = Build.VERSION.INCREMENTAL ?: "",
        securityPatch = Build.VERSION.SECURITY_PATCH,
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null,
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null,
        sku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SKU else null,
        odmSku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.ODM_SKU else null,
        appPackageName = context.packageName,
        appVersionName = packageInfo.versionName ?: "",
        appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun DeviceInfoSnapshot.toPrettyJson(): String {
    return PrettyJson.encodeToString(this)
}

@OptIn(ExperimentalSerializationApi::class)
private val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
}
