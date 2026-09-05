package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LegalScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("用户协议与免责声明") },
                subtitle = { Text("商业专有软件许可 · 隐私政策 · 全链路免责") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            LegalContent(showHeader = true)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun LegalAcceptanceDialog(onAccepted: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                "用户服务协议与全链路免责声明",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegalContent(showHeader = false)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "我已完整阅读并自愿同意《专有商业软件许可》、《第三方凭据隐私授权》与《全链路免责声明》，自愿承担使用风险并承诺合规使用",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccepted,
                enabled = checked
            ) {
                Text("同意并继续")
            }
        }
    )
}

@Composable
fun LegalContent(showHeader: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showHeader) {
            TonalCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "重要合规提示与使用前提",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "本软件（Fuck ETS100 / Fe）及配套的 LA Studio 云端关联服务为专有商业软件体系，历史开源协议（包括但不限于 GPL 3.0）已全数终止并废除。使用本软件或关联云端服务即代表你已年满并具备完全民事行为能力，并无条件理解、签署并自愿接受本协议全部条款约束。如你不同意本协议的任一内容，请立即停止使用并卸载本软件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        LegalSection(
            title = "一、专有软件许可与知识产权保护（EULA）",
            icon = Icons.Default.Gavel,
            items = listOf(
                "商业专有属性与开源终止：本软件及其关联的后端架构、通信协议已全面转为专有商业软件。历史版本所适用的任何开源许可证（包含但不限于 GPL 3.0）已自本版本起彻底终止且不再具有约束力。本软件之源代码、底层 Native 动态链接库（.so）、通信协议、UI 设计与商标所有权均独家归属于 LA Studio 研发团队所有，受《中华人民共和国著作权法》等法律保护。",
                "受限使用许可：开发者仅授予用户一项个人的、非排他性、不可转让、不可分许可且可随时撤销的有限客户端使用许可，仅用于用户本人在经授权的移动设备上进行合法、正当的个人学习交流与本地数据解析参考。",
                "禁止逆向工程与破解：严禁任何个人、机构或商业组织对本软件客户端、底层 native 库、通信接口、中继网关及 LA Studio 后端鉴权系统进行反编译、反汇编、动态调试、脱壳解密、抓包拦截分析、接口劫持、伪造服务端响应，或制作任何形式的外挂、辅助补丁及衍生破解版本。",
                "严禁未授权转售与商业牟利：严禁在未经开发者官方书面许可的情况下，将本软件安装包、授权验证码、LA Studio 会员账号、云端解析接口等资源进行二次打包分发、公开倒卖、收费转售、租借，或利用本软件对外提供任何形式的有偿代读、代刷作业服务。一经发现，平台有权直接永久封禁关联账号及设备，并依法追究民事赔偿及刑事责任。"
            )
        )

        LegalSection(
            title = "二、LA Studio 账号系统与会员权益规范",
            icon = Icons.Default.Security,
            items = listOf(
                "账号使用权与安全保管：用户注册的 LA Studio 账号所有权归平台所有，用户仅享有有限使用权。用户须自行妥善保管账号凭证、密码及 Session Token。因用户主动泄露、共享、出租、转借他人账号导致的一切损失、数据异常与安全事故，均由用户自行全权承担。",
                "设备绑定与防刷风控机制：为保障系统安全与商业秩序，平台实行严格的设备绑定与防刷策略（包括但不限于一机一码、多端登录限制、高频 API 并发限流）。若系统检测到多设备异常共享、批量刷取接口等违规行为，LA Studio 后端将自动触发风控拦截与账号冻结。",
                "虚拟增值服务与违规封禁：用户通过购买或官方活动获得的会员等级、解析额度均属于平台虚拟增值服务。若用户违反法律法规、学术道德或本协议任一规定，服务方有权在无需事先通知且不予退款的前提下，独立作出限制部分或全部功能、重置会员权益或永久封禁账号的处罚措施。"
            )
        )

        LegalSection(
            title = "三、第三方凭据收集、云端存储与代登录专项隐私授权",
            icon = Icons.Default.Lock,
            items = listOf(
                "凭据收集与加密存储告知：本软件的“云端模式”及远程作业拉取功能需与第三方在线教育平台（包括但不限于 ETS100 / e听说 / 畅言教育等官方系统）进行网络交互。用户在此充分知悉、完全理解并主动授权：为了免除繁琐的手动本地导题流程，实现云端自动化登录、拉取作答列表及试卷答案数据，LA Studio 后端服务器系统将收集、传输并在服务器端加密存储用户提交的第三方平台（ETS100/e听说）账号（手机号/学号）、密码凭据、Session Token 及服务端设备标识码。",
                "排他性合法用途承诺：开发者与 LA Studio 服务平台作出严肃郑重承诺：服务端收集与存储的前述第三方账号密码凭据，仅严格、排他性地用于系统代用户向第三方平台接口发起合法的身份校验、状态维持与作业答案解析等本软件明示之必要技术功能；开发者绝不将用户第三方账号密码用于任何超出本软件功能之外的用途，绝不用于侵犯用户隐私、破坏数据完整性或其他违法违规用途，绝不向任何与服务无关的第三方出售、出租、共享或披露。",
                "传输与存储安全保障：平台采用行业标准的高强度传输层加密协议（TLS/HTTPS）及严密的后端安全存储策略对用户凭据进行防护，采取合理技术手段防范未授权访问或数据泄漏。",
                "代理授权与用户自愿原则：用户明确知晓：提交第三方平台账号密码并在云端进行代登录，属于用户个人自主发起并全权委托服务端的代理行为。用户必须确保其提交的账号密码为其本人合法拥有，用户完全自愿承担向第三方平台发起网络请求的一切平台规则与法律风险。"
            )
        )

        LegalSection(
            title = "四、全链路无敌免责声明（核心免责）",
            icon = Icons.Default.Warning,
            items = listOf(
                "全链路服务范围覆盖：本免责条款不可分割地适用于本产品生态下的所有组成部分，包括但不限于：本软件客户端应用程序（Fuck ETS100 / Fe 各版本）、底层解析库、LA Studio 账号系统、云端鉴权中心、云端中继代理服务器、远程配置接口、题目解密分发系统以及所有关联的技术工具与服务节点（统称“关联服务”）。",
                "按“现状”（AS-IS）提供与无担保：软件及全部关联服务均严格按“现状”（AS IS）及“现有”（AS AVAILABLE）的基础提供。开发者及服务提供方在适用法律允许的最大范围内，明确否认并免除任何明示、默示或法定的保证与担保，包括但不限于适销性、特定用途的适用性、服务持续不中断、无差错、无漏洞或不受第三方网络干扰。",
                "第三方平台非官方与无背书声明：本软件及关联服务为独立技术研究与效率辅助工具，与科大讯飞、广东音像教材出版社、e听说（ETS100）、畅言、各级学校、教育管理部门等任何官方机构无任何隶属、投资、合作、代理或官方背书关系。",
                "第三方账号“顶号”与封号风险免责：用户知悉第三方在线教育平台实行单设备互斥登录原则。使用云端模式可能导致官方客户端被退出登录（即“顶号”）；第三方平台可能随时调整风控规则，对非官方接口调用判定为异常并采取异地警告、限制功能、甚至封禁第三方账号。因使用本软件或关联云端服务导致的任何第三方平台账号异常、被迫下线、无法提交答卷、账号被风控或封禁等一切损失，开发者及关联服务方概不承担任何责任与补偿。",
                "学业与考试风险绝对免责（严禁作弊）：严禁用户在正规考试、模拟测验、随堂测验、课堂实时练习或实时录音提交等关键场景中使用本软件或关联服务！本软件仅供课后个人技术交流、数据解析与作业备份。用户必须恪守学术诚信与校纪校规，严禁用于考试作弊、弄虚作假或规避教学监管。因用户违规使用导致考试中断、无法交卷、答案丢失、被判定作弊、成绩作废、受校纪处分或引发任何纠纷的，所有后果与法律责任均由用户个人全权承担，开发者及关联服务方概不承担任何连带法律责任。",
                "用户违规责任自负与赔偿承诺：若用户利用本软件从事任何违法违规、侵犯第三方知识产权或妨害计算机信息系统安全的行为，一切民事、行政或刑事法律后果均由用户个人独立承担。若用户的违规行为导致开发者或 LA Studio 关联服务方遭受行政处罚、民事诉讼或商誉损失，用户承诺无条件全额赔偿开发者所承受的一切直接与间接经济损失（包括但不限于诉讼费、律师费、罚金与赔偿金）。",
                "间接损害与赔偿责任排除：在法律法规允许的最大限度内，无论是基于侵权、违约、过失还是任何其他法律理论，开发者、运营者及关联服务提供方均不对用户或任何第三方所遭受的任何间接、附带、惩罚性、特殊或后果性损害（包括学业受损、预期收益损失、数据损坏、硬件故障等）承担任何赔偿责任。"
            )
        )

        LegalSection(
            title = "五、服务的变更、中断、限制与终止",
            icon = Icons.Default.Info,
            items = listOf(
                "服务调整与终止权：开发者及服务运营方保留根据业务运营调整、政策法规监管、第三方接口重大变动或服务器技术升级需要，在无需向用户发出个别事先通知的情况下，随时修改、限制、暂停、中断或永久终止部分或全部客户端功能与云端服务的权利。",
                "免责保护：对于因第三方接口失效、网络攻击、通信故障、机房停电、政策调整等不可抗力或常规维护导致的服务中断、延迟或数据无法读取，服务方无需对用户或任何第三方承担任何违约或补偿责任。"
            )
        )

        LegalSection(
            title = "六、适用法律与争议解决",
            icon = Icons.Default.Gavel,
            items = listOf(
                "准据法：本协议之订立、生效、解释、履行及纠纷解决均适用中华人民共和国大陆地区之法律法规。",
                "条款独立性：本协议任何条款若因违反强制性法律规范而被具有管辖权的法院认定为无效或不可执行，该条款在必要范围内予以剥离，不影响其余条款的法律效力与可执行性。"
            )
        )
    }
}

@Composable
private fun LegalSection(
    title: String,
    icon: ImageVector,
    items: List<String>
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
