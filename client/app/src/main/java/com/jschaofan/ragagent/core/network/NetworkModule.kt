package com.jschaofan.ragagent.core.network

import com.jschaofan.ragagent.BuildConfig
import com.jschaofan.ragagent.data.remote.api.AuthApi
import com.jschaofan.ragagent.data.repository.ChatRepositoryImpl
import com.jschaofan.ragagent.data.repository.AuthRepositoryImpl
import com.jschaofan.ragagent.data.repository.ProductRepositoryImpl
import com.jschaofan.ragagent.data.remote.api.ChatApi
import com.jschaofan.ragagent.data.remote.api.ProductApi
import com.jschaofan.ragagent.data.remote.api.PortalApi
import com.jschaofan.ragagent.data.remote.sse.ChatSseClient
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 集中创建并管理 JSON、OkHttp、Retrofit 和 SSE 客户端。
 *
 * 普通 HTTP 请求和 SSE 流式请求共用相同的鉴权与 JSON 规则，避免配置不一致。
 */
class NetworkModule(
    tokenProvider: TokenProvider,
    onUnauthorized: () -> Unit = {},
    baseUrl: String = BuildConfig.API_BASE_URL,
    isDebug: Boolean = BuildConfig.DEBUG,
) {
    val json: Json = Json {
        // 后端新增字段时忽略未知字段，旧版 App 不会因此直接解析失败。
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(tokenProvider, onUnauthorized))
        .apply {
            if (isDebug) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    // 普通接口需要有限超时；SSE 必须持续读取，直到完成、失败或主动取消。
    private val sseOkHttpClient = okHttpClient.newBuilder()
        .readTimeout(SSE_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val chatSseClient: ChatSseClient = ChatSseClient(
        client = sseOkHttpClient,
        json = json,
        baseUrl = baseUrl,
    )

    val chatApi: ChatApi = retrofit.create(ChatApi::class.java)
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val productApi: ProductApi = retrofit.create(ProductApi::class.java)
    val portalApi: PortalApi = retrofit.create(PortalApi::class.java)

    val authRepository by lazy {
        AuthRepositoryImpl(authApi = authApi)
    }

    val chatRepository by lazy {
        ChatRepositoryImpl(
            streamDataSource = chatSseClient,
            chatApi = chatApi,
        )
    }

    val productRepository by lazy {
        ProductRepositoryImpl(productApi = productApi)
    }

    inline fun <reified T> createApi(): T = retrofit.create(T::class.java)

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val HTTP_READ_TIMEOUT_SECONDS = 30L
        const val SSE_READ_TIMEOUT_SECONDS = 0L
        const val WRITE_TIMEOUT_SECONDS = 30L
        const val JSON_MEDIA_TYPE = "application/json"
    }
}
