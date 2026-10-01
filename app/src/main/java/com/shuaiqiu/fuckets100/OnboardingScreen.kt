package com.shuaiqiu.fuckets100

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.feature.settings.LegalContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 软件初始化冷启动引导向导 (Material 3 Expressive 风格)
 * 4 步流程：1.欢迎与协议 -> 2.账号登录 -> 3.模式与按需权限 -> 4.完成
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val accountManager = remember { FeAccountManager.get(context) }
    val shizukuState = rememberShizukuState()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedMode by rememberSaveable { mutableStateOf(ActivationMode.CLOUD) }

    // 处理系统返回键：第一步退出应用，后几步回退上一向导页
    BackHandler {
        if (currentStep > 0) {
            currentStep -= 1
        } else {
            activity?.finish()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OnboardingStepIndicator(
                currentStep = currentStep,
                totalSteps = 4,
                onBack = {
                    if (currentStep > 0) currentStep -= 1
                    else activity?.finish()
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width / 2 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width / 2 } + fadeOut())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "onboarding_step_content",
        ) { step ->
            when (step) {
                0 -> WelcomeStep(
                    onNext = {
                        SettingsManager.saveLegalAccepted(true)
                        currentStep = 1
                    },
                )
                1 -> AccountStep(
                    accountManager = accountManager,
                    onNext = { currentStep = 2 },
                )
                2 -> ModeAndPermissionStep(
                    initialMode = selectedMode,
                    shizukuState = shizukuState,
                    onNext = { mode ->
                        selectedMode = mode
                        currentStep = 3
                    },
                )
                3 -> if (selectedMode == ActivationMode.CLOUD) {
                    CloudBindingStep(
                        accountManager = accountManager,
                        onFinish = {
                            SettingsManager.saveActivationMode(selectedMode)
                            SettingsManager.saveLegalAccepted(true)
                            SettingsManager.saveOnboardingCompleted(true)
                            onFinished()
                        },
                    )
                } else {
                    FinishStep(
                        selectedMode = selectedMode,
                        onFinish = {
                            SettingsManager.saveActivationMode(selectedMode)
                            SettingsManager.saveLegalAccepted(true)
                            SettingsManager.saveOnboardingCompleted(true)
                            onFinished()
                        },
                    )
                }
            }
        }
    }
}

/** 顶部紧凑指示条 */
@Composable
private fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
) {
    val stepTitles = listOf("欢迎", "Fe 账号", "模式与权限", "绑定 e听说")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "返回或退出",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "${currentStep + 1} / $totalSteps  ${stepTitles.getOrElse(currentStep) { "" }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(36.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { index ->
                val active = index <= currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                )
            }
        }
    }
}

// ============================================================================
// 步骤 1: 欢迎与幽默标语 + 用户协议
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf(false) }
    var legalAccepted by rememberSaveable { mutableStateOf(SettingsManager.hasAcceptedLegal()) }
    var showLegalSheet by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        entered = true
        LegalAgreementRepository.refresh(context)
    }

    // Logo 弹性缩放与渐显
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "logo_scale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400),
        label = "logo_alpha",
    )

    // 标语文本渐显与上移
    val textAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 160),
        label = "text_alpha",
    )
    val textOffsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 24f,
        animationSpec = tween(400, delayMillis = 160),
        label = "text_offset_y",
    )

    // 底部操作区渐显与上移
    val bottomAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 280),
        label = "bottom_alpha",
    )
    val bottomOffsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 20f,
        animationSpec = tween(400, delayMillis = 280),
        label = "bottom_offset_y",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
    ) {
        // 中间主体（Logo + 标题 + 幽默标语），居中稳定布局
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 图标展示 (Spring 缩放 + 淡入)
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fe_logo),
                    contentDescription = "Fuck ETS100",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(MaterialTheme.shapes.large),
                )
            }

            Spacer(Modifier.height(24.dp))

            // 标题与幽默标语 (平滑位移与淡入，不改变占位大小)
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha
                    translationY = textOffsetY
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Fuck ETS100",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = RighteousFont),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "把做口语的痛苦留给算法，把大好青春还给生活喵~",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 底部协议与行动按钮，固定底部锚点
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .graphicsLayer {
                    alpha = bottomAlpha
                    translationY = bottomOffsetY
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 协议勾选行 (带 shakeOffset 左右抖动提醒，不产生纵向位移)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeOffset.value
                    }
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { legalAccepted = !legalAccepted }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = legalAccepted,
                    onCheckedChange = { legalAccepted = it },
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "已阅读并同意",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "《用户协议与全链路免责声明》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showLegalSheet = true },
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    if (legalAccepted) {
                        onNext()
                    } else {
                        Toast.makeText(context, "请先阅读并勾选同意用户协议喵~", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            shakeOffset.snapTo(0f)
                            shakeOffset.animateTo(14f, tween(50))
                            shakeOffset.animateTo(-14f, tween(50))
                            shakeOffset.animateTo(10f, tween(50))
                            shakeOffset.animateTo(-10f, tween(50))
                            shakeOffset.animateTo(0f, tween(50))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("开启旅程", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    // 用户协议查看抽屉
    if (showLegalSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLegalSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "用户协议与全链路免责声明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showLegalSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    LegalContent(showHeader = false)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        legalAccepted = true
                        showLegalSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("我已阅读并知悉")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================================
// 步骤 2: 账号登录 (融入向导)
// ============================================================================

@Composable
private fun AccountStep(
    accountManager: FeAccountManager,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gateState by accountManager.gateState.collectAsState()
    val session = gateState.cachedSessionOrNull

    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var captchaVisible by remember { mutableStateOf(false) }
    var pendingCaptchaToken by remember { mutableStateOf<String?>(null) }
    var showRegister by remember { mutableStateOf(false) }

    fun submit(capToken: String? = pendingCaptchaToken) {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入用户名和密码"
            return
        }
        scope.launch {
            loading = true
            error = null
            val result = accountManager.login(username, password, capToken)
            loading = false
            result.onSuccess {
                onNext()
            }.onFailure { failure ->
                val apiError = (failure as? FeAccountApiException)?.error
                if (apiError?.captchaRequired == true) {
                    captchaVisible = true
                    error = "请先完成人机验证"
                } else {
                    error = apiError?.message ?: failure.message ?: "登录失败"
                }
            }
            pendingCaptchaToken = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // 头部图标与凝练标题
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "连接 LA Studio 账号",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "同步会员特权与云端解析服务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))
        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "先登录 Fe 账号",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "这是 LA Studio 账号，用来保存设置和使用云端服务。注册完成后回到这里登录；下一步会单独引导你绑定 e听说账号。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 如果已经持有有效登录会话 (如老用户或刚才登录成功)
        if (session != null) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "已登录为 ${session.user.username}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(session.user.tierDisplayName) },
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("继续下一步")
                    }
                }
            }
        } else {
            // 未登录：极简卡片表单
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Text("登录并继续", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    enabled = !loading,
                    onClick = { showRegister = true },
                ) {
                    Text("没有账号？注册新账号")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showRegister) {
        InAppRegisterScreen(
            onBack = { showRegister = false },
            onRegistered = {
                showRegister = false
                Toast.makeText(
                    context,
                    "注册成功，请使用刚注册的 LA Studio 账号登录",
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }

    if (captchaVisible) {
        CapVerificationDialog(
            onDismiss = {
                captchaVisible = false
                pendingCaptchaToken = null
            },
            onToken = { token ->
                captchaVisible = false
                pendingCaptchaToken = token
                submit(token)
            },
        )
    }
}

// ============================================================================
// 云端模式：明确完成 e听说账号绑定
// ============================================================================

@Composable
private fun CloudBindingStep(
    accountManager: FeAccountManager,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val changyanLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "绑定 e听说账号",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "云端模式还需要一个 e听说（ETS100）学生账号来读取作业。它和刚才的 LA Studio 账号不是同一个账号。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        EtingshuoBindingCard(
            manager = accountManager,
            modifier = Modifier.fillMaxWidth(),
            onChangyanLogin = {
                changyanLoginLauncher.launch(ChangyanWebLoginActivity.createIntent(context))
            },
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text("完成设置，进入 Fe", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        TextButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("暂不绑定，先进入应用")
        }
        Text(
            "之后仍可在「设置 > 云端模式」或「个人中心」补充绑定。未绑定时云端作业读取无法使用。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================
// 步骤 3: 模式先导与按需精准授权 (不索取多余权限，无悬浮窗干扰)
// ============================================================================

@Composable
private fun ModeAndPermissionStep(
    initialMode: ActivationMode,
    shizukuState: ShizukuState,
    onNext: (ActivationMode) -> Unit,
) {
    val context = LocalContext.current
    var selectedMode by rememberSaveable {
        mutableStateOf(if (initialMode == ActivationMode.DEFAULT) ActivationMode.CLOUD else initialMode)
    }

    // 动态权限状态
    var hasFilesPerm by remember { mutableStateOf(PermissionsHelper.hasAllFilesAccess()) }
    var isRootGranted by remember { mutableStateOf(RootManager.isRootAvailable()) }
    var showMissingPermissionDialog by remember { mutableStateOf(false) }
    var pendingMissingList by remember { mutableStateOf<List<String>>(emptyList()) }

    // 当用户从系统设置或授权软件返回时，刷新权限状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
        RootManager.clearCache()
        isRootGranted = RootManager.isRootAvailable()
    }

    val isGeekMode = selectedMode == ActivationMode.SHIZUKU || selectedMode == ActivationMode.ROOT

    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        when (selectedMode) {
            ActivationMode.CLOUD -> {
                // 云端模式不需要本地敏感权限
            }
            ActivationMode.SHIZUKU -> {
                if (!shizukuState.isRunning || !shizukuState.permissionGranted) {
                    missing.add("Shizuku 服务运行与授权")
                }
                if (!hasFilesPerm) {
                    missing.add("所有文件访问权限")
                }
            }
            ActivationMode.ROOT -> {
                if (!isRootGranted) {
                    missing.add("Root (SU) 超级用户权限")
                }
                if (!hasFilesPerm) {
                    missing.add("所有文件访问权限")
                }
            }
            ActivationMode.DIRECT_READ, ActivationMode.DEFAULT -> {
                if (!hasFilesPerm) {
                    missing.add("所有文件访问权限")
                }
            }
        }
        return missing
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = "选择运行模式",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "按需索取权限，避免不必要的系统授权",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))
        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "不知道怎么选？大多数用户选「云端模式」即可；只有想读取手机本地缓存，或已经在使用 Shizuku / Root 时，才选择本地模式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 模式 1: 云端模式
        ModeSelectionCard(
            title = "云端模式",
            subtitle = "推荐：免 Root，在线读取作业与答案",
            badge = "适合大多数人",
            icon = Icons.Default.Cloud,
            selected = selectedMode == ActivationMode.CLOUD,
            onClick = { selectedMode = ActivationMode.CLOUD },
        )

        Spacer(Modifier.height(10.dp))

        // 模式 2: 极客提权模式 (Shizuku 与 Root 可选)
        ModeSelectionCard(
            title = "极客提权模式",
            subtitle = "需要 Shizuku 或 Root，适合高级用户",
            badge = "本地读取",
            icon = if (selectedMode == ActivationMode.ROOT) Icons.Default.Security else Icons.Default.CheckCircle,
            selected = isGeekMode,
            onClick = {
                if (!isGeekMode) {
                    selectedMode = ActivationMode.SHIZUKU
                }
            },
            extraContent = if (isGeekMode) {
                {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Shizuku Tab
                        val isShizuku = selectedMode == ActivationMode.SHIZUKU
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isShizuku) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedMode = ActivationMode.SHIZUKU }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Shizuku 授权",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isShizuku) FontWeight.Bold else FontWeight.Normal,
                                color = if (isShizuku) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Root Tab
                        val isRoot = selectedMode == ActivationMode.ROOT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isRoot) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedMode = ActivationMode.ROOT }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Root 权限",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRoot) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else null,
        )

        Spacer(Modifier.height(10.dp))

        // 模式 3: 本地漏洞直读模式
        ModeSelectionCard(
            title = "本地直读模式",
            subtitle = "读取手机已缓存的作业数据，不走云端",
            badge = "无需 Root",
            icon = Icons.Default.Bolt,
            selected = selectedMode == ActivationMode.DIRECT_READ,
            onClick = { selectedMode = ActivationMode.DIRECT_READ },
        )

        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + slideInVertically { it / 5 }) togetherWith
                    (fadeOut(animationSpec = tween(120)) + slideOutVertically { -it / 5 })
            },
            label = "mode_explanation",
        ) { mode ->
            ModeExplanation(mode = mode)
        }

        Spacer(Modifier.height(18.dp))

        // 动态权限卡片 (按选定模式展示，避免多余权限)
        Text(
            text = "所需权限与就绪状态",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))

        when (selectedMode) {
            ActivationMode.CLOUD -> {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "无需额外敏感权限",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "云端模式直接通过安全通信接口工作，不读取本地文件与应用列表，纯净轻量。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ActivationMode.SHIZUKU -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shizuku 授权卡片
                    val shizukuReady = shizukuState.isRunning && shizukuState.permissionGranted
                    PermissionStatusCard(
                        title = "Shizuku 服务授权",
                        desc = if (shizukuReady) "Shizuku 服务已连接且授权成功" else if (shizukuState.isRunning) "Shizuku 服务运行中，点击申请授权" else "尚未检测到 Shizuku 服务运行，点击前往启动",
                        isGranted = shizukuReady,
                        actionLabel = if (shizukuState.isRunning) "申请授权" else "打开 Shizuku",
                        onAction = {
                            if (shizukuState.isRunning) {
                                if (ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(context, "Shizuku 已授权", Toast.LENGTH_SHORT).show()
                                } else {
                                    ShizukuManager.requestPermission(ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION)
                                }
                            } else {
                                ShizukuManager.openShizukuApp(context)
                            }
                        },
                    )

                    // 文件访问权限
                    PermissionStatusCard(
                        title = "所有文件访问权限",
                        desc = if (hasFilesPerm) "本地存储访问已就绪" else "用于读取本地题库与音频文件",
                        isGranted = hasFilesPerm,
                        actionLabel = "去授权",
                        onAction = { PermissionsHelper.requestAllFilesAccess(context) },
                    )
                }
            }

            ActivationMode.ROOT -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Root 授权卡片
                    PermissionStatusCard(
                        title = "Root (SU) 权限授权",
                        desc = if (isRootGranted) "Root 权限已获取，享受最高提权权限" else "尚未获取 Root 权限，点击申请或检测",
                        isGranted = isRootGranted,
                        actionLabel = if (isRootGranted) "已授权" else "申请/检测",
                        onAction = {
                            RootManager.clearCache()
                            isRootGranted = RootManager.isRootAvailable()
                            if (isRootGranted) {
                                Toast.makeText(context, "Root 权限已成功获取！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "未获取到 Root 权限，请在授权管理软件(如Magisk/KSU/APatch)中放行", Toast.LENGTH_LONG).show()
                            }
                        },
                    )

                    // 文件访问权限
                    PermissionStatusCard(
                        title = "所有文件访问权限",
                        desc = if (hasFilesPerm) "本地存储访问已就绪" else "用于读取本地题库与音频文件",
                        isGranted = hasFilesPerm,
                        actionLabel = "去授权",
                        onAction = { PermissionsHelper.requestAllFilesAccess(context) },
                    )
                }
            }

            ActivationMode.DIRECT_READ, ActivationMode.DEFAULT -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 本地直读仅需文件访问权限
                    PermissionStatusCard(
                        title = "所有文件访问权限",
                        desc = if (hasFilesPerm) "本地存储访问已就绪" else "用于读取本地试卷缓存",
                        isGranted = hasFilesPerm,
                        actionLabel = "去授权",
                        onAction = { PermissionsHelper.requestAllFilesAccess(context) },
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val missing = getMissingPermissions()
                if (missing.isEmpty()) {
                    onNext(selectedMode)
                } else {
                    pendingMissingList = missing
                    showMissingPermissionDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text("完成配置", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(16.dp))
    }

    // 未完全授权提示弹窗
    if (showMissingPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPermissionDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = {
                Text(
                    text = "部分权限尚未授予",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text(
                        text = "当前选定的模式需要以下权限以确保核心功能正常运作：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    pendingMissingList.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "若未完全授权，对应模式可能无法成功读取答题试卷或检测应用状态。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMissingPermissionDialog = false }) {
                    Text("去授权")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMissingPermissionDialog = false
                        onNext(selectedMode)
                    }
                ) {
                    Text("暂不授权，继续")
                }
            },
        )
    }
}

/**
 * 用一句话解释当前模式适合谁、需要什么、会怎么工作。
 * 放在模式卡片和权限卡片之间，让用户先理解选择，再处理授权。
 */
@Composable
private fun ModeExplanation(mode: ActivationMode) {
    val (title, summary, bullets) = when (mode) {
        ActivationMode.CLOUD -> Triple(
            "推荐给大多数用户",
            "不读取手机本地题库，作业和答案由云端服务处理。",
            listOf(
                "需要：LA Studio 账号 + e听说账号",
                "优点：免 Root、免 Shizuku，配置最少",
                "适合：想快速开始、只关心作业和答案的用户",
            ),
        )
        ActivationMode.SHIZUKU -> Triple(
            "本地读取：Shizuku",
            "直接读取 e听说在手机中缓存的作业数据，不依赖云端绑定。",
            listOf(
                "需要：启动并授权 Shizuku，以及所有文件访问权限",
                "优点：数据留在本机，不需要上传到云端",
                "适合：已经在使用 Shizuku 的高级用户",
            ),
        )
        ActivationMode.ROOT -> Triple(
            "本地读取：Root",
            "使用 Root 权限访问 e听说本地缓存，能力最完整但风险和配置成本更高。",
            listOf(
                "需要：Root 授权，以及所有文件访问权限",
                "优点：本地读取能力最强，不依赖云端绑定",
                "适合：熟悉 Magisk、KernelSU 或 APatch 的用户",
            ),
        )
        ActivationMode.DIRECT_READ, ActivationMode.DEFAULT -> Triple(
            "本地读取：无需 Root",
            "读取手机中已经缓存的作业数据，不使用云端服务。",
            listOf(
                "需要：所有文件访问权限",
                "优点：不需要 Root 或 Shizuku",
                "适合：不想绑定云端，也不想使用提权工具的用户",
            ),
        )
    }

    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 1.dp).size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** 单选模式卡片 */
@Composable
private fun ModeSelectionCard(
    title: String,
    subtitle: String,
    badge: String? = null,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(220),
        label = "mode_card_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "mode_card_border_color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(220),
        label = "mode_card_border_width",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "mode_icon_container_color",
    )

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (badge != null) {
                            SuggestionChip(
                                onClick = onClick,
                                label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                                enabled = !selected,
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                )
            }
            if (extraContent != null) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(160)),
                ) {
                    extraContent()
                }
            }
        }
    }
}

/** 引导权限项卡片 */
@Composable
private fun PermissionStatusCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isGranted) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ============================================================================
// 步骤 4: 结束页面 (凝练欢迎)
// ============================================================================

@Composable
private fun FinishStep(
    selectedMode: ActivationMode,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(0.7f))

        // 成功图标
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "配置就绪，欢迎使用！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "所有初始化项目已就绪，开启畅爽学习体验",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // 清单卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryItem(text = "用户服务协议与免责声明已确认")
                SummaryItem(text = "LA Studio 云端服务已连接")
                SummaryItem(text = "默认运行模式：${selectedMode.title}")
            }
        }

        Spacer(Modifier.height(16.dp))

        // 贴心小提示
        Text(
            text = "💡 提示：在主界面顶部卡片可随时切换运行授权模式喵~",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text("进入主界面", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
