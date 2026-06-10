package com.jschaofan.ragagent.core.network

/**
 * 向网络层提供 Token，同时避免网络层依赖具体的 Token 存储方式。
 */
fun interface TokenProvider {
    fun getToken(): String?
}

/**
 * 暂时使用的内存实现，后续登录模块完成后可替换为持久化存储。
 */
class MutableTokenProvider(
    initialToken: String? = null,
) : TokenProvider {
    @Volatile
    private var token: String? = initialToken

    override fun getToken(): String? = token

    fun updateToken(value: String?) {
        token = value
    }
}

/**
 * 将登录令牌持久化到应用私有存储，供普通 HTTP 和 SSE 请求统一读取。
 */
class PersistentTokenProvider(
    context: android.content.Context,
    fallbackToken: String? = null,
) : TokenProvider {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        android.content.Context.MODE_PRIVATE,
    )

    @Volatile
    private var token: String? = preferences.getString(TOKEN_KEY, null) ?: fallbackToken

    override fun getToken(): String? = token

    fun getUserId(): Long? = preferences.getLong(USER_ID_KEY, -1L).takeIf { it >= 0 }
    fun getUserType(): Int? = preferences.getInt(USER_TYPE_KEY, -1).takeIf { it >= 0 }
    fun getIdentifier(): String? = preferences.getString(IDENTIFIER_KEY, null)

    fun updateSession(token: String, userId: Long, userType: Int, identifier: String) {
        this.token = token
        preferences.edit()
            .putString(TOKEN_KEY, token)
            .putLong(USER_ID_KEY, userId)
            .putInt(USER_TYPE_KEY, userType)
            .putString(IDENTIFIER_KEY, identifier)
            .apply()
    }

    fun updateToken(value: String?) {
        token = value
        preferences.edit().apply {
            if (value.isNullOrBlank()) remove(TOKEN_KEY) else putString(TOKEN_KEY, value)
        }.apply()
    }

    fun clearSession() {
        token = null
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "authentication"
        const val TOKEN_KEY = "token"
        const val USER_ID_KEY = "user_id"
        const val USER_TYPE_KEY = "user_type"
        const val IDENTIFIER_KEY = "identifier"
    }
}
