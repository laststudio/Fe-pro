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

data class AccountBindingsUiState(
    val bindings: FeAccountProfileApi.AccountBindings? = null,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AccountBindingsViewModel @Inject constructor(
    private val manager: FeAccountManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountBindingsUiState())
    val uiState = _uiState.asStateFlow()

    fun refresh(force: Boolean = false) {
        if (_uiState.value.isRefreshing || _uiState.value.isSubmitting) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val bindings = manager.accountBindings(force)
                _uiState.update { it.copy(bindings = bindings) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e, "加载绑定信息失败")
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun bind(account: String, password: String, onSuccess: () -> Unit) {
        submit("绑定失败", onSuccess) {
            manager.bindPlatformAccount(account.trim(), password)
            _uiState.update {
                val previous = it.bindings ?: FeAccountProfileApi.AccountBindings(null, null)
                it.copy(bindings = previous.copy(etingshuo = FeAccountProfileApi.AccountBindings.EtingshuoBinding(account.trim())))
            }
        }
    }

    fun unbind(onSuccess: () -> Unit) {
        submit("解除绑定失败", onSuccess) {
            manager.unbindPlatformAccount()
            _uiState.update { it.copy(bindings = it.bindings?.copy(etingshuo = null)) }
        }
    }

    private fun submit(fallback: String, onSuccess: () -> Unit, operation: suspend () -> Unit) {
        if (_uiState.value.isSubmitting || _uiState.value.isRefreshing) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                operation()
                // 变更已经成功，不让随后刷新状态的网络失败被误报成变更失败。
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e, fallback)
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
            if (_uiState.value.error == null) refresh(force = true)
        }
    }

    private suspend fun handleError(error: Exception, fallback: String) {
        _uiState.update { it.copy(error = (error as? FeAccountApiException)?.error?.message ?: error.message ?: fallback) }
        if (error is FeAccountApiException && error.error.statusCode == 401) manager.revalidateOnline()
    }
}
