package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

import com.shuaiqiu.fuckets100.FeAccountGateState
@Composable
fun FeAccountGate(
    viewModel: AccountViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) { viewModel.checkGate() }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val resumedState = viewModel.gateState.value
            if (resumedState is FeAccountGateState.Ready ||
                resumedState is FeAccountGateState.Offline
            ) {
                viewModel.revalidateOnline()
            }
            while (true) {
                delay(ONLINE_REVALIDATION_INTERVAL_MS)
                if (viewModel.gateState.value is FeAccountGateState.Ready ||
                    viewModel.gateState.value is FeAccountGateState.Offline
                ) {
                    viewModel.revalidateOnline()
                }
            }
        }
    }

    content()
}


internal const val ONLINE_REVALIDATION_INTERVAL_MS = 60_000L
