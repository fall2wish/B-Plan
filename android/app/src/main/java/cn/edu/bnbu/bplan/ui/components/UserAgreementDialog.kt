package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.bnbu.bplan.ui.theme.BnbuNavy
import cn.edu.bnbu.bplan.ui.theme.SurfaceLight
import cn.edu.bnbu.bplan.ui.theme.TextPrimary
import cn.edu.bnbu.bplan.ui.theme.TextSecondary

val LocalAgreementGuard = compositionLocalOf<((() -> Unit) -> Unit)> { { action -> action() } }

/**
 * 【用户须知与免责协议】弹窗
 * 目的：全面规避校规处分、网络安全指控、学业纠纷及法律连带风险（全方位严密“叠甲”）
 * 支持：同意并进入（完整功能）、不同意仅浏览模式（锁定功能）、常驻查阅模式
 */
@Composable
fun UserAgreementDialog(
    isFirstLaunch: Boolean,
    onAccept: () -> Unit,
    onBrowseOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    var hasConfirmedCheckbox by remember { mutableStateOf(!isFirstLaunch) }
    val configuration = LocalConfiguration.current
    val localeTag = configuration.locales.get(0)?.toLanguageTag() ?: "zh-CN"
    val isEnglish = localeTag.startsWith("en", ignoreCase = true)
    val isTraditional = localeTag.contains("HK", ignoreCase = true) || localeTag.contains("TW", ignoreCase = true)

    Dialog(
        onDismissRequest = {
            if (!isFirstLaunch) {
                onDismiss()
            } else {
                onBrowseOnly()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = !isFirstLaunch,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLight,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BnbuNavy.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BnbuNavy,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when {
                                    isEnglish -> "User Notice & Disclaimers"
                                    isTraditional -> "用戶須知與免責協議"
                                    else -> "用户须知与免责协议"
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = BnbuNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    isEnglish -> "B-Plan · Independent Open-Source Tool"
                                    isTraditional -> "B-Plan · 獨立開源學子輔助工具"
                                    else -> "B-Plan · 独立开源学子辅助工具"
                                },
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (!isFirstLaunch) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 核心属性标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AgreementBadge("🛡️ " + if (isEnglish) "Unofficial" else if (isTraditional) "非官方非商業" else "非官方非商业")
                    AgreementBadge("🔒 " + if (isEnglish) "Local TEE Storage" else if (isTraditional) "芯片隔離無雲端" else "芯片隔离无云端")
                    AgreementBadge("⚠️ " + if (isEnglish) "Reference Only" else if (isTraditional) "僅供輔助參考" else "仅供辅助参考")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 核心协议条款正文（可滚动）
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 重要警示背景框
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isEnglish -> "CRITICAL NOTICE: B-Plan is an unofficial third-party client developed by independent students. All timetable and assignment reminders are for informal reference only. All official academic and exam regulations must strictly follow BNBU official websites. The developer is legally exempted from any direct or indirect liabilities."
                                        isTraditional -> "【特別聲明與法律須知】本軟件為個人開發者課餘研發的獨立第三方客戶端，非校方官方軟件。所有課表、作息與作業提醒僅供個人參考，一切出勤、考試與成績評定一律以學校官方網頁為準。用戶須自願承擔使用風險，開發者在法律允許的最大限度內完全免除一切學業、法律與連帶責任。"
                                        else -> "【特别声明与法律须知】本软件为个人开发者课余研发的独立第三方客户端，非校方官方软件。所有课表、作息与作业提醒仅供个人参考，一切出勤、考试与成绩评定一律以学校官方网页为准。用户须自愿承担使用风险，开发者在法律允许的最大限度内完全免除一切学业、法律与连带责任。"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 第一条：非官方声明与责任切割
                    item {
                        AgreementSection(
                            icon = Icons.Default.Public,
                            title = when {
                                isEnglish -> "1. Non-Official Nature & Complete Independence"
                                isTraditional -> "一、 獨立第三方工具性質與主體責任切割"
                                else -> "一、 独立第三方工具性质与主体责任切割"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "1.1 Absolute Unofficial Identity: B-Plan is a personal hobby and open-source utility developed independently by individual student contributors. It is strictly NOT funded, commissioned, sponsored, endorsed, supervised, or operated by Beijing Normal University - Hong Kong Baptist University (BNBU), the Academic Registry, the Information Technology Services Centre (ITSC), or any administrative office.",
                                    "1.2 No Official Representation: The developer team has never claimed nor possessed any authority to represent BNBU or publish official university policies.",
                                    "1.3 Intellectual Property & Trademarks: All system names, software copyrights, UI assets, and API trademarks associated with GSAS (教务系统), iSpace / Moodle, Unified Authentication (SSO/CAS), and Tencent Enterprise Mail belong exclusively to BNBU and their respective authorized vendors."
                                )
                                isTraditional -> listOf(
                                    "1.1 絕對非官方性質：本軟件（B-Plan）為個人開發者基於興趣與便利同學自主研發之開源工具，嚴格獨立於校方行政體系。本軟件未獲北師香港浸會大學（BNBU）、教務處、資訊科技服務中心（ITSC）或任何官方部門之出資、授權、背書、審批或指定運營。",
                                    "1.2 無任何官方代表權限：開發者個人及本軟件無權代表學校發布任何學業通告或政策解釋，軟件內之任何呈現均不具備官方效力。",
                                    "1.3 知識產權與商標歸屬：軟件所涉及之教務管理系統（GSAS）、教學平台（iSpace/Moodle）、校園統一身份認證（CAS/SSO）、騰訊企業郵箱等系統名稱、商標、著作權及網絡接口，其知識產權與所有權均完全歸屬於北師香港浸會大學及各自主管商業實體所有。"
                                )
                                else -> listOf(
                                    "1.1 绝对非官方性质：本软件（B-Plan）为个人开发者基于兴趣与便利同学自主研发之开源工具，严格独立于校方行政体系。本软件未获北师香港浸会大学（BNBU）、教务处、信息技术服务中心（ITSC）或任何官方部门之出资、授权、背书、审批或指定运营。",
                                    "1.2 无任何官方代表权限：开发者个人及本软件无权代表学校发布任何学业通告或政策解释，软件内之任何呈现均不具备官方效力。",
                                    "1.3 知识产权与商标归属：软件所涉及之教务管理系统（GSAS）、教学平台（iSpace/Moodle）、校园统一身份认证（CAS/SSO）、腾讯企业邮箱等系统名称、商标、著作权及网络接口，其知识产权与所有权均完全归属于北师香港浸会大学及各自主管商业实体所有。"
                                )
                            }
                        )
                    }

                    // 第二条：学业免责与二次核查义务（核心叠甲：防止旷课/挂科甩锅）
                    item {
                        AgreementSection(
                            icon = Icons.Default.Gavel,
                            title = when {
                                isEnglish -> "2. Information for Reference Only & Complete Liability Waiver"
                                isTraditional -> "二、 數據僅供參考與學業免責條款（核心免責）"
                                else -> "二、 数据仅供参考与学业免责条款（核心免责）"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "2.1 Secondary Derivative Reference: Timetables, course locations, TA tutorials, assignment DDL countdowns, and holiday make-up days displayed within B-Plan are parsed automatically on the client side. They are provided solely as informal study-planning references and do NOT substitute official university notices.",
                                    "2.2 Absolute Supremacy of Official Web Portals: All official attendance records, exam dates, room allocations, assignment submission deadlines, and make-up adjustments are SOLELY determined by the official GSAS website (gsas.bnbu.edu.cn), iSpace website (ispace.bnbu.edu.cn), and direct communications from instructors/TAs.",
                                    "2.3 Technical Disclaimers: University portal upgrades, maintenance downtime, network latency, VPN tunnel timeouts, or algorithmic edge cases may lead to delayed or inaccurate display of schedules. The developer expressly DISCLAIMS any and all liabilities (direct, indirect, consequential, or punitive) for missed lectures, assignment late penalties, examination absences, grade deductions, or academic sanctions. Users bear the sole obligation to cross-check all vital dates on official web portals."
                                )
                                isTraditional -> listOf(
                                    "2.1 數據二次衍生性：本軟件所呈現的課表節次、教室地點、TA 輔導課、iSpace 作業截止倒計時、已提交附件及校歷調休等，均為客戶端本地自動化算法解析之二次信息，僅供個人日常時間規劃輔助參考，不具備任何行政或學籍法律約束力。",
                                    "2.2 學校官方渠道具有絕對唯一效力：一切選課結果、考勤出勤、平時作業截止判定、期末考試日程、教室臨時調整及法定假期補課，一律以學校官方 GSAS 教務網（gsas.bnbu.edu.cn）、iSpace 平台（ispace.bnbu.edu.cn）網頁端公示及任課教師/助教的官方書面通告為唯一準則。",
                                    "2.3 開發者完全免責與用戶核實義務：鑑於校方服務器維護、網絡波動、接口結構調整或個人網絡環境限制，課表或待辦可能出現未及時更新或解析異常。用戶因依賴本軟件提示而導致的遲到、缺勤、誤課、錯過考試、作業逾期或學分扣減等任何學業後果或人身財產損失，在法律允許的最大限度內，均由用戶自行承擔二次核實缺失之後果，開發者不承擔任何直接、間接或連帶法律責任。"
                                )
                                else -> listOf(
                                    "2.1 数据二次衍生性：本软件所呈现的课表节次、教室地点、TA 辅导课、iSpace 作业截止倒计时、已提交附件及校历调休等，均为客户端本地自动化算法解析之二次信息，仅供个人日常时间规划辅助参考，不具备任何行政或学籍法律约束力。",
                                    "2.2 学校官方渠道具有绝对唯一效力：一切选课结果、考勤出勤、平时作业截止判定、期末考试日程、教室临时调整及法定假期补课，一律以学校官方 GSAS 教务网（gsas.bnbu.edu.cn）、iSpace 平台（ispace.bnbu.edu.cn）网页端公示及任课教师/助教的官方书面通告为唯一准则。",
                                    "2.3 开发者完全免责与用户核实义务：鉴于校方服务器维护、网络波动、接口结构调整或个人网络环境限制，课表或待办可能出现未及时更新或解析异常。用户因依赖本软件提示而导致的迟到、缺勤、误课、错过考试、作业逾期或学分扣减等任何学业后果或人身财产损失，在法律允许的最大限度内，均由用户自行承担二次核实缺失之后果，开发者不承担任何直接、间接或连带法律责任。"
                                )
                            }
                        )
                    }

                    // 第三条：网络安全与校规合规（核心叠甲：防止网信/ITSC处分）
                    item {
                        AgreementSection(
                            icon = Icons.Default.Lock,
                            title = when {
                                isEnglish -> "3. Campus Network Compliance & Zero-Cloud Security"
                                isTraditional -> "三、 校園網絡安全合規與零雲端中轉承諾"
                                else -> "三、 校园网络安全合规与零云端中转承诺"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "3.1 Zero Developer Cloud Infrastructure: The developer does NOT deploy or maintain any intermediary server, database, or remote relay. No personal user credentials, student IDs, passwords, timetables, or emails are ever uploaded to any third-party or developer-controlled server. There is ZERO risk of developer-side data aggregation or leakage.",
                                    "3.2 Pure Local Client Communication: All network requests initiated by B-Plan are strictly client-to-server TLS connections originating directly from the user's mobile device to official university endpoints (gsas.bnbu.edu.cn, ispace.bnbu.edu.cn) or Tencent enterprise mail servers.",
                                    "3.3 Hardware-Level Cryptographic Isolation: Credential storage utilizes Android KeyStore TEE / StrongBox hardware chips (AES-256-GCM), strictly guarded by OS sandbox mechanisms. The developer has no mechanical means to access users' passwords.",
                                    "3.4 Campus Network Guidelines Compliance: Sync mechanisms are rate-limited to standard browser request intervals, strictly forbidding automated scraping loops. The software complies with campus IT policies for normal personal client browsing."
                                )
                                isTraditional -> listOf(
                                    "3.1 零開發者雲端服務器：本軟件自身不架設、不租用、不運營任何遠程後端服務器、雲數據庫或數據中轉節點。開發者在任何情況下均不收集、不存儲、不分析、不轉發任何使用者的學號、姓名、密碼、課表或郵件內容。在技術與架構層面完全杜絕了開發者端洩露個人隱私之可能。",
                                    "3.2 純本地端到端安全通信：軟件之所有網絡通信，本質均為用戶個人終端設備在本地發起、經由標準 HTTPS/TLS 加密直連北師香港浸會大學官方服務器或騰訊企業郵服務器，無任何中間人介入。",
                                    "3.3 硬件芯片物理隔離：若用戶選擇在本地保存登錄憑證，學號密碼由手機獨立安全芯片（TEE/StrongBox）採用 AES-256-GCM 硬件加密保護，主系統與任何第三方軟件均無法導出密鑰，安全等級等同於手機銀行與指紋安全隔離。",
                                    "3.4 符合校園網個人使用規範：軟件內置嚴格的請求間隔與頻次防護，禁止任何惡意並發，純屬個人正常查詢之模擬瀏覽器客戶端行為，完全符合《校園網絡及計算機使用守則》。"
                                )
                                else -> listOf(
                                    "3.1 零开发者云端服务器：本软件自身不架设、不租用、不运营任何远程后端服务器、云数据库或数据中转节点。开发者在任何情况下均不收集、不存储、不分析、不转发任何使用者的学号、姓名、密码、课表或邮件内容。在技术与架构层面完全杜绝了开发者端泄露个人隐私之可能。",
                                    "3.2 纯本地端到端安全通信：软件之所有网络通信，本质均为用户个人终端设备在本地发起、经由标准 HTTPS/TLS 加密直连北师香港浸会大学官方服务器或腾讯企业邮服务器，无任何中间人介入。",
                                    "3.3 硬件芯片物理隔离：若用户选择在本地保存登录凭证，学号密码由手机独立安全芯片（TEE/StrongBox）采用 AES-256-GCM 硬件加密保护，主系统与任何第三方软件均无法导出密钥，安全等级等同于手机银行与指纹安全隔离。",
                                    "3.4 符合校园网个人使用规范：软件内置严格的请求间隔与频次防护，禁止任何恶意并发，纯属个人正常查询之模拟浏览器客户端行为，完全符合《校园网络及计算机使用守则》。"
                                )
                            }
                        )
                    }

                    // 第四条：用户行为限制与恶意逆向切割（核心叠甲：防止恶意篡改责任栽赃）
                    item {
                        AgreementSection(
                            icon = Icons.Default.Security,
                            title = when {
                                isEnglish -> "4. User Prohibitions & Malicious Modification Disclaimer"
                                isTraditional -> "四、 用戶合規約束與惡意改裝免責切割"
                                else -> "四、 用户合规约束与恶意改装免责切割"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "4.1 Prohibition of System Abuse: Users shall not use, modify, or reverse-engineer this software to launch DoS attacks, unauthorized stress tests, or exploit security vulnerabilities on BNBU digital infrastructure.",
                                    "4.2 Independent Liability for Modified Versions: The developer is ONLY responsible for the original open-source baseline. If any third party or user repacks, modifies, introduces malicious code, or uses automated scripts to overload university networks, all legal liabilities, campus penalties, and police actions shall be borne SOLELY by that respective party.",
                                    "4.3 Personal Security Safeguards: Users must safeguard their own physical devices. Risks resulting from device loss, rooting, jailbreaking, or sharing credentials with others are entirely the user's responsibility."
                                )
                                isTraditional -> listOf(
                                    "4.1 嚴禁任何破壞性行為：任何用戶均不得利用本軟件或對本軟件進行逆向修改後，對學校官方網絡、教務服務器實施拒絕服務攻擊（DoS）、未授權漏洞探測或任何威脅校園網安全穩定運行的違規行為。",
                                    "4.2 二次修改版本之責任獨立切割：開發者僅對官方發布之開源代碼基準負責。若有任何第三方或個人私自反編譯、篡改代碼、植入惡意腳本或濫用本工具造成校園服務器過載、癱瘓或安全事件，所產生之一切法律責任、校紀處分及經濟賠償，均由該直接實施違規行為之人全額自行承擔，與本軟件原作者概無關聯。",
                                    "4.3 用戶自身設備保管義務：用戶應妥善保管個人手機及校園賬號，因手機丟失、出借他人、系統 Root/越獄破解導致之密鑰暴露或隱私風險，由用戶自行負責。"
                                )
                                else -> listOf(
                                    "4.1 严禁任何破坏性行为：任何用户均不得利用本软件或对本软件进行逆向修改后，对学校官方网络、教务服务器实施拒绝服务攻击（DoS）、未授权漏洞探测或任何威胁校园网安全稳定运行的违规行为。",
                                    "4.2 二次修改版本之责任独立切割：开发者仅对官方发布之开源代码基准负责。若有任何第三方或个人私自反编译、篡改代码、植入恶意脚本或滥用本工具造成校园服务器过载、瘫痪或安全事件，所产生之一切法律责任、校纪处分及经济赔偿，均由该直接实施违规行为之人全额自行承担，与本软件原作者概无关联。",
                                    "4.3 用户自身设备保管义务：用户应妥善保管个人手机及校园账号，因手机丢失、出借他人、系统 Root/越狱破解导致之密钥暴露或隐私风险，由用户自行负责。"
                                )
                            }
                        )
                    }

                    // 第五条：开源许可、除作者外严禁商用与强传染性开源条款
                    item {
                        AgreementSection(
                            icon = Icons.Default.Gavel,
                            title = when {
                                isEnglish -> "5. Open-Source License, Anti-Commercial & Viral Copyleft"
                                isTraditional -> "五、 開源許可、除原作者外嚴禁商用與強傳染性開源"
                                else -> "五、 开源许可、除原作者外严禁商用与强传染性开源"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "5.1 CC BY-NC-SA 4.0 & Software Copyleft Rider: This project is licensed under CC BY-NC-SA 4.0 coupled with the B-Plan Software Copyleft Rider. Non-commercial and open-source principles are absolute prerequisites for using this software.",
                                    "5.2 Exclusive Commercial Rights & Strict Third-Party Prohibition: All rights to commercial operation, monetization, and commercial licensing are strictly and exclusively reserved by the original author (fall2wish). All third parties (individuals, commercial companies, student groups, or organizations) are strictly forbidden from using this project or any part thereof for any direct or indirect commercial profit (including paid distribution, selling pre-packaged APKs, embedding commercial ads, paid support, or inclusion in paid services).",
                                    "5.3 Viral Copyleft & Mandatory Source Code Disclosure: Anyone modifying, forking, distributing, or integrating code from this project into other software must license the entire derivative work under the exact same CC BY-NC-SA 4.0 license and must make 100% of the complete source code publicly available. Closed-source proprietary forks are strictly prohibited.",
                                    "5.4 Automatic Termination & Legal Remedies: Any breach of the non-commercial restriction or failure to disclose full source code results in immediate and automatic termination of the license. The original author reserves all legal rights to claim statutory damages, cease-and-desist injunctions, and full disgorgement of commercial profits."
                                )
                                isTraditional -> listOf(
                                    "5.1 許可協議依據：本項目依據 CC BY-NC-SA 4.0（署名-非商業性使用-相同方式共享 4.0 國際）協議及 B-Plan 軟件反商用與傳染性開源專用條款發布，非商業性與強傳染開源為本軟件之根本紅線。",
                                    "5.2 除原作者外嚴禁任何商業盈利使用：本項目之全部商業運營、商業變現及商業授權專有權完全且排他性地歸原作者本人（fall2wish）單獨所有。嚴禁任何第三方個人、商業實體、社團或學生團隊將本項目（或其任何部分、模塊、編譯產物）直接或間接用於任何商業盈利目的（包括但不限於付費下載、打包販賣、植入商業推廣、付費諮詢或作為商業收費服務之基礎組件）。",
                                    "5.3 強傳染性開源義務（用了也必須開源）：任何個人或組織若對本項目進行二次開發、衍生修改、分發或作為模塊依賴集成，其衍生工程整體必須無條件強制採用相同的 CC BY-NC-SA 4.0 協議，並向社會公眾無保留完全開源其全部源代碼，嚴禁閉源私有化封裝或以專有許可證發布。",
                                    "5.4 違約授權自動失效與法律追責：任何違反上述非商業限制或閉源發布者，其獲得的本軟件授權自違約行為發生之日起自動永久終止，原作者保留依法追究侵權法律責任、申請下架並全額追索商業非法所得之全部訴訟權利。"
                                )
                                else -> listOf(
                                    "5.1 许可协议依据：本项目依据 CC BY-NC-SA 4.0（署名-非商业性使用-相同方式共享 4.0 国际）协议及 B-Plan 软件反商用与传染性开源专用条款发布，非商业性与强传染开源为本软件之根本红线。",
                                    "5.2 除原作者外严禁任何商业盈利使用：本项目之全部商业运营、商业变现及商业授权专有权完全且排他性地归原作者本人（fall2wish）单独所有。严禁任何第三方个人、商业实体、社团或学生团队将本项目（或其任何部分、模块、编译产物）直接或间接用于任何商业盈利目的（包括但不限于付费下载、打包贩卖、植入商业推广、付费咨询或作为商业收费服务之基础组件）。",
                                    "5.3 强传染性开源义务（用了也必须开源）：任何个人或组织若对本项目进行二次开发、衍生修改、分发或作为模块依赖集成，其衍生工程整体必须无条件强制采用相同的 CC BY-NC-SA 4.0 协议，并向社会公众无保留完全开源其全部源代码，严禁闭源私有化封装或以专有许可证发布。",
                                    "5.4 违约授权自动失效与法律追责：任何违反上述非商业限制或闭源发布者，其获得的本软件授权自违约行为发生之日起自动永久终止，原作者保留依法追究侵权法律责任、申请下架并全额追索商业非法所得之全部诉讼权利。"
                                )
                            }
                        )
                    }

                    // 第六条：协议生效与最终准据
                    item {
                        AgreementSection(
                            icon = Icons.Default.Gavel,
                            title = when {
                                isEnglish -> "6. Effectiveness & Dispute Resolution"
                                isTraditional -> "六、 協議效力與法律爭議處置"
                                else -> "六、 协议效力与法律争议处置"
                            },
                            paragraphs = when {
                                isEnglish -> listOf(
                                    "6.1 Full Agreement & Browse-Only Protection: By checking the confirmation box and entering the software, you affirm that you have read, understood, and consented to each clause in this agreement without coercion. If you choose 'Browse Only', the software strictly remains in read-only mode and locks all sync and credential features until you explicitly consent, ensuring complete informed consent under data privacy regulations.",
                                    "6.2 Severability: If any provision of this agreement is deemed invalid under applicable law, the remaining provisions shall remain in full force and effect."
                                )
                                isTraditional -> listOf(
                                    "6.1 充分知情與僅瀏覽模式權利保障：用戶勾選確認框並點擊進入軟件，即視為用戶已完全知悉、理解並完全接受本協議之全部內容；若用戶選擇「不同意，僅瀏覽」，軟件將保持只讀預覽狀態並鎖定所有功能，直至用戶明確簽署本協議，保障用戶知情同意與數據自決權利。",
                                    "6.2 可分割性：若本協議中的任何條款因法律法規被認定為無效，其餘條款之效力均不受任何影響，繼續保持完整法律約束力。"
                                )
                                else -> listOf(
                                    "6.1 充分知情与仅浏览模式权利保障：用户勾选确认框并点击进入软件，即视为用户已完全知悉、理解并完全接受本协议之全部内容；若用户选择「不同意，仅浏览」，软件将保持只读预览状态并锁定所有功能，直至用户明确签署本协议，保障用户知情同意与数据自决权利。",
                                    "6.2 可分割性：若本协议中的任何条款因法律法规被认定为无效，其余条款之效力均不受任何影响，继续保持完整法律约束力。"
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部强制确认与操作按钮区域
                if (isFirstLaunch) {
                    // 强制勾选确认框
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasConfirmedCheckbox) BnbuNavy.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable { hasConfirmedCheckbox = !hasConfirmedCheckbox }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasConfirmedCheckbox,
                            onCheckedChange = { hasConfirmedCheckbox = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BnbuNavy,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isEnglish -> "I have thoroughly read and understood all disclaimers above. I acknowledge B-Plan is an unofficial tool and assume full responsibility for academic verification."
                                isTraditional -> "我已詳細閱讀並充分理解上述免責協議全部條款，知曉本軟件為非官方第三方輔助工具，自願承擔學業二次核對責任，免除開發者一切連帶責任。"
                                else -> "我已详细阅读并充分理解上述免责协议全部条款，知晓本软件为非官方第三方辅助工具，自愿承担学业二次核对责任，免除开发者一切连带责任。"
                            },
                            fontSize = 11.sp,
                            color = if (hasConfirmedCheckbox) BnbuNavy else TextPrimary,
                            fontWeight = if (hasConfirmedCheckbox) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 仅浏览与同意进入按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBrowseOnly,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Browse Only" else if (isTraditional) "不同意，僅瀏覽" else "不同意，仅浏览",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onAccept,
                            enabled = hasConfirmedCheckbox,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BnbuNavy,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Agree & Continue" else if (isTraditional) "同意並進入" else "同意并进入",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasConfirmedCheckbox) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    // 常驻查阅模式（从设置页打开）
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Read & Close" else if (isTraditional) "已閱讀並返回" else "已阅读并返回",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgreementBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AgreementSection(
    icon: ImageVector,
    title: String,
    paragraphs: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BnbuNavy,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BnbuNavy
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 6.dp)
        ) {
            paragraphs.forEach { p ->
                Text(
                    text = p,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 16.5.sp
                )
            }
        }
    }
}
