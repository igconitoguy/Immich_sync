package com.immich.autodownloader.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ImmichApi {
    @POST("api/search/metadata")
    suspend fun searchAssets(@Body request: SearchAssetsRequest): SearchAssetsResponse

    @GET("api/assets/{assetId}/original")
    @Streaming
    suspend fun downloadOriginal(@Path("assetId") assetId: String): ResponseBody
}

@Serializable
data class SearchAssetsRequest(
    val size: Int = 1000,
    val page: Int = 1,
    val type: AssetBucketType = AssetBucketType.ALL,
)

@Serializable
enum class AssetBucketType {
    @SerialName("all")
    ALL,
}

@Serializable
data class SearchAssetsResponse(
    val assets: SearchAssetPage = SearchAssetPage(),
)

@Serializable
data class SearchAssetPage(
    val items: List<ImmichAsset> = emptyList(),
    val total: Int = 0,
    val count: Int = 0,
)

@Serializable
data class ImmichAsset(
    val id: String,
    @SerialName("originalFileName")
    val originalFileName: String,
    @SerialName("originalPath")
    val originalPath: String? = null,
    @SerialName("checksum")
    val checksum: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("exifInfo")
    val exifInfo: ExifInfo? = null,
)

@Serializable
data class ExifInfo(
    @SerialName("fileSizeInByte")
    val fileSizeInByte: Long? = null,
)
