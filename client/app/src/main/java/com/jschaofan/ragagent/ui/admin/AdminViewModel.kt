package com.jschaofan.ragagent.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.data.remote.api.PortalApi
import com.jschaofan.ragagent.data.remote.dto.DatasetCreateDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountCreateDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountQueryDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountUpdateDto
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

data class DatasetItem(
    val id: Long,
    val name: String,
    val description: String = "",
    val disabled: Int = 0,
)

data class AdminUiState(
    val users: List<SubaccountDto> = emptyList(),
    val datasets: List<DatasetItem> = emptyList(),
    val isLoading: Boolean = false,
    val identifier: String = "",
    val password: String = "",
    val userType: Int = 2,
    val datasetName: String = "",
    val datasetDescription: String = "",
    val selectedFileName: String? = null,
    val uploadDatasetId: Long? = null,
    val message: String? = null,
)

class AdminViewModel(
    private val api: PortalApi,
    private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state = _state.asStateFlow()
    private var selectedFile: File? = null

    init {
        loadUsers()
        loadDatasets()
    }

    fun loadUsers() = launchAction {
        val users = api.getUsers(pageSize = 100, query = SubaccountQueryDto()).data?.rows.orEmpty()
        _state.update { it.copy(users = users) }
    }

    fun createUser() {
        val state = _state.value
        if (state.identifier.isBlank() || state.password.isBlank()) {
            showMessage("账号和密码不能为空")
            return
        }
        launchAction {
            api.createUser(SubaccountCreateDto(state.identifier.trim(), state.password, state.userType))
            _state.update { it.copy(identifier = "", password = "") }
            loadUsers()
        }
    }

    fun updateUser(user: SubaccountDto, password: String, type: Int) = launchAction {
        require(type in 1..2) { "用户类型只能在普通管理员和普通用户之间修改" }
        api.updateUser(
            user.subaccountId,
            SubaccountUpdateDto(
                password = password.takeIf(String::isNotBlank),
                type = type,
            ),
        )
        loadUsers()
    }

    fun deleteUser(id: Long) = launchAction {
        api.deleteUser(id)
        loadUsers()
    }

    fun loadDatasets() = launchAction {
        _state.update { it.copy(datasets = parseDatasets(api.getDatasets().data)) }
    }

    fun createDataset() {
        val state = _state.value
        if (state.datasetName.isBlank()) {
            showMessage("数据集名称不能为空")
            return
        }
        launchAction {
            api.createDataset(DatasetCreateDto(state.datasetName.trim(), state.datasetDescription.trim()))
            _state.update { it.copy(datasetName = "", datasetDescription = "") }
            loadDatasets()
        }
    }

    fun toggleDataset(item: DatasetItem) = launchAction {
        api.toggleDataset(item.id, if (item.disabled == 0) 1 else 0)
        loadDatasets()
    }

    fun deleteDataset(id: Long) = launchAction {
        api.deleteDataset(id)
        loadDatasets()
    }

    fun selectDatasetForUpload(id: Long) = _state.update { it.copy(uploadDatasetId = id) }
    fun onIdentifierChanged(value: String) = _state.update { it.copy(identifier = value) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value) }
    fun onUserTypeChanged(value: Int) = _state.update { it.copy(userType = value) }
    fun onDatasetNameChanged(value: String) = _state.update { it.copy(datasetName = value) }
    fun onDatasetDescriptionChanged(value: String) = _state.update { it.copy(datasetDescription = value) }

    fun selectFile(uri: Uri) {
        runCatching {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: "knowledge-file"
            val target = File(context.cacheDir, "dataset-${System.currentTimeMillis()}-$name")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input).copyTo(target.outputStream())
            }
            selectedFile?.delete()
            selectedFile = target
            _state.update { it.copy(selectedFileName = name) }
        }.onFailure { showMessage(it.message ?: "读取文件失败") }
    }

    fun upload() {
        val datasetId = _state.value.uploadDatasetId
        val file = selectedFile
        if (datasetId == null || file == null) {
            showMessage("请先选择数据集和文件")
            return
        }
        launchAction {
            val body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            api.uploadDatasetFile(
                datasetId,
                MultipartBody.Part.createFormData("file", _state.value.selectedFileName, body),
            )
            showMessage("文件上传成功")
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            runCatching { block() }
                .onFailure { showMessage(it.message ?: "操作失败") }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun showMessage(message: String) = _state.update { it.copy(message = message) }

    private fun parseDatasets(data: JsonElement?): List<DatasetItem> {
        val array = when (data) {
            is JsonArray -> data
            is JsonObject -> data["rows"] as? JsonArray
                ?: data["records"] as? JsonArray
                ?: data["list"] as? JsonArray
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: item["datasetId"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: return@mapNotNull null
            DatasetItem(
                id = id,
                name = item["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                description = item["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                disabled = item["disabled"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
    }

    override fun onCleared() {
        selectedFile?.delete()
        super.onCleared()
    }

    class Factory(private val api: PortalApi, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(api, context) as T
    }
}
