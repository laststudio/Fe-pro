package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.LegalAgreement
import com.shuaiqiu.fuckets100.LegalAgreementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val legalAgreementRepository: LegalAgreementRepository,
) : ViewModel() {
    val currentAgreement: StateFlow<LegalAgreement> = legalAgreementRepository.currentAgreement

    fun refresh() {
        viewModelScope.launch { legalAgreementRepository.refresh() }
    }
}
