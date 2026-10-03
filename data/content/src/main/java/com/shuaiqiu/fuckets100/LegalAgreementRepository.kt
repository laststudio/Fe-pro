package com.shuaiqiu.fuckets100

import android.content.Context
import android.util.Log
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用户协议与法务条款统一仓库
 * 支持云端动态获取、本地缓存持久化与离线 Fallback 兜底机制
 */
class LegalAgreementRepository(
    private val appContext: Context,
    private val buildInfo: AppBuildInfo,
) {
    companion object {
        private const val TAG = "LegalAgreementRepo"
        private const val PREFS_NAME = "fe_legal_agreement_cache"
        private const val KEY_CACHED_JSON = "cached_agreement_json"
        private const val TIMEOUT_MS = 6000
        private val ENDPOINTS = listOf(
            "https://api.lastudio.cc/legal/agreement",
            "https://api.lastudio.cc/terms",
        )

        val FALLBACK_AGREEMENT = LegalAgreement(
        title = "LastStudio 服务协议、隐私政策与免责声明",
        effectiveDate = "2026 年 9 月 25 日",
        version = "2026-09-25",
        scope = "LastStudio 官网、Fe Web 作业工作台、Fuck ETS100（Fe）客户端、LastStudio 账号系统、云端解析与相关技术服务。",
        sections = listOf(
            LegalAgreementSection(
                id = "preface",
                title = "阅读提示与协议范围",
                badge = "请先阅读",
                warning = true,
                content = "欢迎使用 LastStudio 服务。本文档同时包含《服务协议》《隐私政策》和《免责声明》，用于说明服务范围、账号使用规则、数据处理方式及使用风险。\n\n请在注册、登录、绑定第三方账号、上传内容或使用解析功能前阅读并理解本文档。您勾选“同意”或继续使用服务，表示您已阅读并同意适用的条款；如果您不同意，请停止使用相关功能。需要单独授权的敏感信息或第三方凭据，会在具体操作页面再次提示并由您主动确认。\n\n本文档不替代法律、医疗、教育或其他专业意见。我们会按照适用法律和实际业务变化更新条款，并在更新时标注生效日期；重大变化会通过站内提示或其他合理方式告知。",
            ),
            LegalAgreementSection(
                id = "section-1",
                title = "一、服务协议与软件许可",
                badge = "服务规则",
                items = listOf(
                    LegalAgreementItem(
                        title = "1. 服务内容",
                        content = "LastStudio 提供账号认证、作业信息同步、内容解析、答案查看、语音相关工具、公告与其他由页面明确展示的技术服务。具体功能、可用范围、会员权益和调用额度以当前页面及服务端配置为准。",
                    ),
                    LegalAgreementItem(
                        title = "2. 软件许可与第三方组件",
                        content = "在遵守本协议及适用法律的前提下，我们授予您一项个人、有限、非排他、不可转让且不可分许可的使用许可，仅用于合法的学习参考和个人数据处理。LastStudio 自有代码、品牌、界面和服务标识的权利归其权利人所有；服务中使用的第三方或开源组件继续适用其各自许可证，本协议不会取代或限制这些许可证授予的权利。",
                    ),
                    LegalAgreementItem(
                        title = "3. 账号与安全",
                        content = "您应提供真实、准确且属于自己的注册信息，妥善保管密码、验证码、访问令牌和设备。发现账号被盗用、凭据泄露或异常访问时，应立即通过账号中心修改凭据并联系我们。因您主动共享、出租、转让账号或未采取合理保护措施造成的损失，由您依法承担相应责任。",
                    ),
                    LegalAgreementItem(
                        title = "4. 禁止行为",
                        content = "不得利用服务实施违法犯罪、侵犯他人权益、绕过访问控制、探测或攻击系统、反向工程专有服务、伪造请求或签名、批量滥用接口、传播恶意内容、未经授权代他人登录，或以任何方式干扰服务的正常运行。不得将服务用于考试作弊、代写代答或规避学校和平台的管理要求。",
                    ),
                    LegalAgreementItem(
                        title = "5. 会员、额度与退款",
                        content = "会员、卡密、额度等属于页面所示的虚拟服务权益，购买或兑换前请核对服务内容、有效期和适用范围。因法律法规、第三方平台变更或用户违反规则导致服务受限的处理，以页面公示和适用法律为准；如页面另有退款规则，按该规则执行。",
                    ),
                ),
            ),
            LegalAgreementSection(
                id = "section-2",
                title = "二、账号、内容与服务管理",
                badge = "账号管理",
                items = listOf(
                    LegalAgreementItem(
                        title = "1. 用户提交的内容",
                        content = "您对自己提交、上传或通过服务处理的账号资料、文本、音频、作业数据及其他内容负责，并应确保拥有必要的权利和授权。为提供、维护、排查和改进服务，我们会在必要范围内处理这些内容，不会将其用于与服务无关的目的。",
                    ),
                    LegalAgreementItem(
                        title = "2. 服务调整与终止",
                        content = "我们会根据安全、维护、产品规划、法律法规或第三方接口变化，对功能进行增加、调整、暂停或终止。对于影响核心权益的重大调整，我们会在合理范围内提前提示。您可以停止使用服务；如需注销账号或处理历史数据，请通过账号中心或产品反馈入口提交申请。",
                    ),
                    LegalAgreementItem(
                        title = "3. 违规处置",
                        content = "如果有合理依据认为账号或内容存在违法、侵权、欺诈、攻击、滥用或其他违反本协议的行为，我们可以采取提醒、限流、暂停功能、冻结会话、删除内容或终止账号等措施，并保留必要的审计记录。处置会遵循适用法律，并在可行范围内提供申诉或说明渠道。",
                    ),
                ),
            ),
            LegalAgreementSection(
                id = "section-3",
                title = "三、隐私政策与个人信息处理",
                badge = "隐私政策",
                warning = true,
                items = listOf(
                    LegalAgreementItem(
                        title = "1. 个人信息处理者与联系方式",
                        content = "个人信息处理者为 LastStudio 运营团队。您可以通过 LastStudio 官网的账号中心、服务页面或产品内反馈入口提交查阅、更正、删除、撤回授权、投诉和安全事件报告。我们会在核验请求人身份后，在适用法律规定的期限内处理。",
                    ),
                    LegalAgreementItem(
                        title = "2. 我们收集哪些信息",
                        content = "根据您使用的功能，我们可能处理：注册和登录所需的用户名、邮箱或验证码信息；账号安全所需的设备标识、登录时间、IP 地址、浏览器或客户端信息；您主动绑定的第三方平台账号标识、登录凭据、会话令牌及平台返回的作业、班级或用户资料；您主动上传或生成的文本、音频、作业和语音模型数据；以及用于故障排查、安全审计、计费和客服的操作记录。没有使用相应功能时，不会因为本政策而额外收集该功能所需的信息。",
                    ),
                    LegalAgreementItem(
                        title = "3. 处理目的和必要性",
                        content = "我们仅在提供账号登录、身份验证、第三方账号绑定、作业同步与解析、语音功能、客服支持、反滥用和履行法律义务所必需的范围内处理信息。拒绝提供必要信息可能导致对应功能无法使用，但不影响您使用不依赖该信息的其他功能。",
                    ),
                    LegalAgreementItem(
                        title = "4. 第三方账号凭据专项授权",
                        content = "绑定 ETS100、e听说或其他第三方平台时，您可以选择将账号标识、密码或会话凭据提交给服务端，由服务端代您完成登录、状态维持和作业数据读取。该信息属于高敏感凭据，仅用于您明确选择的功能；我们不会出售或用于与该功能无关的用途。提交前请确认账号属于您本人并了解第三方平台的规则和风险。您可以在账号中心解绑，解绑后我们会停止使用新的绑定凭据，并按保留期限删除或去标识化相关数据。",
                    ),
                    LegalAgreementItem(
                        title = "5. 存储、委托处理和共享",
                        content = "我们会根据数据敏感程度采取访问控制、传输加密、密钥管理、日志审计、最小权限和备份保护等措施。为完成基础设施托管、邮件发送、验证码、人机验证、存储或监控等工作，部分数据可能由受托服务商在我们的指示下处理；我们会要求其遵守保密和安全义务。除获得您的授权、履行服务所必需、应对安全事件或法律法规要求外，不会向无关第三方出售、出租或共享个人信息。",
                    ),
                    LegalAgreementItem(
                        title = "6. 保存期限与删除",
                        content = "账号资料和绑定信息通常在账号存续期间保存；第三方平台凭据在解绑、替换或不再需要后删除或去标识化；解析缓存通常保留 24 小时，自动完成任务记录通常保留 30 天；安全事件记录按后台安全策略保留 7 至 365 天，默认 30 天。法律、争议处理或安全审计确需继续保留的，会在必要期限届满后删除或匿名化。您可以通过账号中心申请查看、更正、导出、删除或解绑相关信息；删除可能导致账号或依赖数据的功能无法继续使用。",
                    ),
                    LegalAgreementItem(
                        title = "7. 您的权利与未成年人",
                        content = "在适用法律允许的范围内，您有权了解、更正、删除或限制我们处理您的个人信息，并可撤回可撤回的授权。未满 18 周岁的用户应在监护人同意和指导下使用服务；处理不满 14 周岁未成年人的个人信息时，我们会按照适用法律取得监护人同意并采取更严格的保护措施。监护人发现未成年人未经同意提交个人信息时，可以通过账号中心联系我们处理。",
                    ),
                    LegalAgreementItem(
                        title = "8. 本地存储、Cookie 与安全事件",
                        content = "网页可能使用 localStorage、sessionStorage 或类似技术保存登录会话、偏好设置和首次同意状态，以维持功能正常运行。您可以通过浏览器设置清理这些数据，但可能需要重新登录或重新确认。若发生可能影响个人信息安全的事件，我们会按适用法律采取补救措施并在必要时通知相关用户。",
                    ),
                ),
            ),
            LegalAgreementSection(
                id = "section-4",
                title = "四、免责声明与使用风险",
                badge = "风险告知",
                warning = true,
                items = listOf(
                    LegalAgreementItem(
                        title = "1. 按现状提供",
                        content = "服务会尽力保持稳定和准确，但按现有技术条件提供，不保证始终不中断、无延迟、无错误或适用于您的特定目的。第三方平台、网络、设备、系统更新和不可抗力都可能导致登录失败、数据延迟、答案缺失或功能暂时不可用。",
                    ),
                    LegalAgreementItem(
                        title = "2. 第三方平台风险",
                        content = "LastStudio 与 ETS100、e听说、畅言、学校或其他第三方平台不存在当然的隶属、代理或官方背书关系。第三方平台可能因安全策略调整而要求重新登录、使设备下线、限制接口访问或封禁账号。您应自行评估并承担使用第三方账号、代登录和网络请求产生的账号、数据和合规风险。",
                    ),
                    LegalAgreementItem(
                        title = "3. 学习、考试与内容准确性",
                        content = "解析结果、答案建议、语音生成和自动化功能仅供课后学习参考，不构成成绩、考试结果或专业意见的保证。严禁在考试、测验、实时课堂或其他禁止使用辅助工具的场景使用。因用户未复核结果、违反学校规则、第三方规则或法律法规产生的后果，由用户依法承担。",
                    ),
                    LegalAgreementItem(
                        title = "4. 用户设备与数据备份",
                        content = "您应自行确认设备、网络、浏览器和第三方账号处于可用状态，并对重要内容保留独立备份。因设备故障、误操作、网络中断、第三方服务或超出合理控制范围的原因造成的数据丢失或业务影响，我们会在法律允许范围内承担责任。",
                    ),
                    LegalAgreementItem(
                        title = "5. 责任范围",
                        content = "在适用法律允许的最大范围内，对于间接损失、预期收益损失、商誉损失、数据丢失或因第三方原因造成的损失，我们不承担责任；但法律明确不得排除或限制的责任除外。本条不影响您依法享有的消费者或个人信息保护权利。",
                    ),
                ),
            ),
            LegalAgreementSection(
                id = "section-5",
                title = "五、变更、通知与争议处理",
                badge = "协议效力",
                items = listOf(
                    LegalAgreementItem(
                        title = "1. 条款更新",
                        content = "我们可能因服务、法律或安全变化更新本文档。更新后的版本会显示新的生效日期；重大变化会通过页面提示、站内消息或其他合理方式通知。更新生效后继续使用服务，表示您接受更新后的条款；如不同意，应停止使用受影响的服务。",
                    ),
                    LegalAgreementItem(
                        title = "2. 适用法律与争议解决",
                        content = "本协议的订立、生效、解释和履行适用中华人民共和国大陆地区的法律法规。争议应先通过账号中心或客服渠道协商解决；协商不成的，提交有管辖权的法院处理。若部分条款被认定无效，不影响其余条款的效力。",
                    ),
                    LegalAgreementItem(
                        title = "3. 联系与投诉",
                        content = "您可以通过 LastStudio 官网的账号中心、服务页面或产品内反馈入口提交隐私请求、账号安全报告、投诉和其他问题。为保护账号安全，我们可能要求您完成身份核验后再处理相关请求。",
                    ),
                ),
            ),
        ),
    )

        fun parseAgreement(jsonStr: String): LegalAgreement {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", FALLBACK_AGREEMENT.title)
            val effectiveDate = root.optString("effective_date", FALLBACK_AGREEMENT.effectiveDate)
            val version = root.optString("version", FALLBACK_AGREEMENT.version)
            val scope = root.optString("scope", FALLBACK_AGREEMENT.scope)

            val sectionsArray = root.optJSONArray("sections") ?: JSONArray()
            val sections = mutableListOf<LegalAgreementSection>()
            for (i in 0 until sectionsArray.length()) {
                val secObj = sectionsArray.getJSONObject(i)
                val id = secObj.optString("id", "section-$i")
                val secTitle = secObj.optString("title", "")
                val badge = if (secObj.has("badge") && !secObj.isNull("badge")) secObj.optString("badge") else null
                val warning = secObj.optBoolean("warning", false)
                val content = if (secObj.has("content") && !secObj.isNull("content")) secObj.optString("content") else null

                val itemsList = mutableListOf<LegalAgreementItem>()
                if (secObj.has("items")) {
                    val itemsArr = secObj.optJSONArray("items") ?: JSONArray()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        itemsList.add(
                            LegalAgreementItem(
                                title = itemObj.optString("title", ""),
                                content = itemObj.optString("content", ""),
                            )
                        )
                    }
                }

                sections.add(
                    LegalAgreementSection(
                        id = id,
                        title = secTitle,
                        badge = badge,
                        warning = warning,
                        content = content,
                        items = itemsList,
                    )
                )
            }

            return LegalAgreement(
                title = title,
                effectiveDate = effectiveDate,
                version = version,
                scope = scope,
                sections = if (sections.isNotEmpty()) sections else FALLBACK_AGREEMENT.sections,
            )
        }
    }


    private val _currentAgreement = MutableStateFlow(FALLBACK_AGREEMENT)
    val currentAgreement: StateFlow<LegalAgreement> = _currentAgreement.asStateFlow()

    init {
        loadCached()
    }

    fun loadCached() {
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedJson = prefs.getString(KEY_CACHED_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                val parsed = parseAgreement(cachedJson)
                _currentAgreement.value = parsed
                Log.d(TAG, "已成功从本地缓存载入用户协议版本: ${parsed.version}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取本地缓存用户协议失败，将使用内置 Fallback", e)
        }
    }


    /**
     * 从服务端异步拉取最新用户协议，成功后自动写入持久化缓存
     */
    suspend fun refresh(): Result<LegalAgreement> = withContext(Dispatchers.IO) {
        val ctx = appContext
        var lastException: Exception? = null

        for (endpoint in ENDPOINTS) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "Fe-Android-Client/${buildInfo.versionName}")

                val code = conn.responseCode
                if (code == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseAgreement(jsonStr)
                    _currentAgreement.value = parsed

                    // 保存到本地持久化缓存
                    try {
                        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit { putString(KEY_CACHED_JSON, jsonStr) }
                    } catch (e: Exception) {
                        Log.w(TAG, "写入用户协议本地缓存失败", e)
                    }

                    Log.i(TAG, "已成功从云端获取最新用户协议版本: ${parsed.version}")
                    return@withContext Result.success(parsed)
                } else {
                    lastException = Exception("HTTP $code from $endpoint")
                    Log.w(TAG, "从 $endpoint 获取用户协议返回 HTTP $code")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "请求 $endpoint 失败: ${e.message}")
            }
        }

        Result.failure(lastException ?: Exception("未能从云端获取用户协议"))
    }
}
