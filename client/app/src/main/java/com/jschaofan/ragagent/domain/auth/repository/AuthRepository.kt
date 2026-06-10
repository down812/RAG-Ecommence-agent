package com.jschaofan.ragagent.domain.auth.repository

import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.domain.auth.model.LoginSession

interface AuthRepository {
    suspend fun login(identifier: String, password: String): ApiResult<LoginSession>
}
