package com.shuaiqiu.fuckets100.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.FeAccountApiException
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.FeAccountProfileApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

internal enum class AccountRecordKind { Device, Session }

data class AccountRecordsUiState(
    val records: List<JSONObject>? = null,
    val isRefreshing: Boolean = false,
    val isRevoking: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AccountRecordsViewModel @Inject constructor(private val manager: FeAccountManager) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountRecordsUiState())
    val uiState = _uiState.asStateFlow()

    internal fun refresh(kind: AccountRecordKind, force: Boolean = false) {
        if (_uiState.value.isRefreshing || _uiState.value.isRevoking) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val records = when (kind) {
                    AccountRecordKind.Device -> manager.accountDevices(force)
                    AccountRecordKind.Session -> manager.accountSessions(force)
                }
                _uiState.update { it.copy(records = records) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e, "加载失败，请下拉重试")
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    internal fun revoke(kind: AccountRecordKind, id: Int, onSuccess: () -> Unit) {
        if (_uiState.value.isRevoking || _uiState.value.isRefreshing) return
        if (id < 0) {
            _uiState.update { it.copy(error = "未找到记录标识，请下拉刷新后重试") }
            return
        }
        _uiState.update { it.copy(isRevoking = true, error = null) }
        viewModelScope.launch {
            var succeeded = false
            try {
                val api = FeAccountProfileApi()
                val token = manager.requireAccessToken()
                when (kind) {
                    AccountRecordKind.Device -> api.revokeDevice(token, id)
                    AccountRecordKind.Session -> api.revokeSession(token, id)
                }
                manager.invalidateAccountData()
                // 保留列表内容，只更新已完成的下线状态，随后再同步后台结果。
                _uiState.update { state ->
                    state.copy(records = state.records?.map { record ->
                        if (record.optInt("id", -1) == id) JSONObject(record.toString()).put("status", "revoked") else record
                    })
                }
                succeeded = true
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e, "下线失败")
            } finally {
                _uiState.update { it.copy(isRevoking = false) }
            }
            if (succeeded) refresh(kind, force = true)
        }
    }

    private suspend fun handleError(error: Exception, fallback: String) {
        _uiState.update { it.copy(error = (error as? FeAccountApiException)?.error?.message ?: error.message ?: fallback) }
        if (error is FeAccountApiException && error.error.statusCode == 401) manager.revalidateOnline()
    }
}
