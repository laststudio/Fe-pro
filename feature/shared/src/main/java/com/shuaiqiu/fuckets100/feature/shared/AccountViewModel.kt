package com.shuaiqiu.fuckets100.feature.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.FeAccountGateState
import com.shuaiqiu.fuckets100.FeAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    val manager: FeAccountManager,
) : ViewModel() {
    val gateState: StateFlow<FeAccountGateState> = manager.gateState

    fun checkGate() {
        viewModelScope.launch { manager.checkGate() }
    }

    fun revalidateOnline() {
        viewModelScope.launch { manager.revalidateOnline() }
    }
}
