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

data class ChangePasswordUiState(val isSubmitting: Boolean = false, val error: String? = null)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(private val manager: FeAccountManager) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun submit(old: String, new: String, confirmation: String, onSuccess: () -> Unit) {
        if (_uiState.value.isSubmitting) return
        val validationError = validateAccountPasswordChange(old, new, confirmation)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                FeAccountProfileApi().changePassword(manager.requireAccessToken(), old, new, confirmation)
                manager.revalidateOnline()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = (e as? FeAccountApiException)?.error?.message ?: e.message ?: "修改失败") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
