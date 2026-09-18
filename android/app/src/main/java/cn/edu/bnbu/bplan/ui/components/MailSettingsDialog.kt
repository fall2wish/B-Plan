package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.data.model.MailConfig
import cn.edu.bnbu.bplan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSettingsDialog(
    currentConfig: MailConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (MailConfig) -> Unit
) {
    val guardAction = LocalAgreementGuard.current
    var protocol by remember { mutableStateOf(currentConfig.protocol) } // "IMAP" or "POP3"
    var emailAddress by remember { mutableStateOf(currentConfig.emailAddress) }
    var password by remember { mutableStateOf(currentConfig.password) }
    var showPassword by remember { mutableStateOf(false) }

    var incomingServer by remember { mutableStateOf(currentConfig.incomingServer) }
    var incomingPort by remember { mutableStateOf(currentConfig.incomingPort.toString()) }
    var useIncomingSsl by remember { mutableStateOf(currentConfig.useIncomingSsl) }

    var outgoingServer by remember { mutableStateOf(currentConfig.outgoingServer) }
    var outgoingPort by remember { mutableStateOf(currentConfig.outgoingPort.toString()) }
    var useOutgoingSsl by remember { mutableStateOf(currentConfig.useOutgoingSsl) }

    var isAutoSyncEnabled by remember { mutableStateOf(currentConfig.isAutoSyncEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = BnbuBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "校园邮箱协议与登录设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "快速预设服务商:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = incomingServer.contains("exmail.qq.com") || incomingServer.contains("bnbu.edu.cn"),
                        onClick = {
                            incomingServer = if (protocol == "IMAP") "imap.exmail.qq.com" else "pop.exmail.qq.com"
                            outgoingServer = "smtp.exmail.qq.com"
                            incomingPort = if (protocol == "IMAP") "993" else "995"
                            outgoingPort = "465"
                            useIncomingSsl = true
                            useOutgoingSsl = true
                        },
                        label = { Text("BNBU 校园邮箱 (腾讯企业邮)", fontSize = 11.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Protocol Selector
                Text(
                    text = "收信协议选择 (POP3 / IMAP):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = protocol == "IMAP",
                        onClick = {
                            protocol = "IMAP"
                            if (incomingPort == "995" || incomingPort == "110") {
                                incomingPort = if (useIncomingSsl) "993" else "143"
                            }
                            if (incomingServer == "pop.exmail.qq.com") {
                                incomingServer = "imap.exmail.qq.com"
                            }
                        },
                        label = { Text("IMAP (推荐 · 双向同步)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BnbuBlue.copy(alpha = 0.15f),
                            selectedLabelColor = BnbuBlue
                        )
                    )
                    FilterChip(
                        selected = protocol == "POP3",
                        onClick = {
                            protocol = "POP3"
                            if (incomingPort == "993" || incomingPort == "143") {
                                incomingPort = if (useIncomingSsl) "995" else "110"
                            }
                            if (incomingServer == "imap.exmail.qq.com") {
                                incomingServer = "pop.exmail.qq.com"
                            }
                        },
                        label = { Text("POP3 (单向拉取)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BnbuGold.copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB45309)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                val emailSuffix = "@mail.bnbu.edu.cn"
                val prefix = emailAddress.substringBefore("@").trim()
                var showEmailDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = {
                            emailAddress = it
                            showEmailDropdown = it.isNotBlank() && !it.endsWith(emailSuffix)
                        },
                        label = { Text("邮箱账号 / 地址 *") },
                        placeholder = { Text("例如: s123456789@mail.bnbu.edu.cn") },
                        singleLine = true,
                        trailingIcon = {
                            if (prefix.isNotEmpty() && !emailAddress.endsWith(emailSuffix)) {
                                IconButton(onClick = {
                                    emailAddress = "$prefix$emailSuffix"
                                    showEmailDropdown = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "自动补全邮箱后缀",
                                        tint = BnbuBlue
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = showEmailDropdown && prefix.isNotEmpty() && !emailAddress.endsWith(emailSuffix),
                        onDismissRequest = { showEmailDropdown = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AlternateEmail,
                                        contentDescription = null,
                                        tint = BnbuBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "$prefix$emailSuffix",
                                            fontWeight = FontWeight.Bold,
                                            color = BnbuNavy,
                                            fontSize = 13.5.sp
                                        )
                                        Text(
                                            text = "点击自动补全 @mail.bnbu.edu.cn",
                                            color = BnbuBlue,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                emailAddress = "$prefix$emailSuffix"
                                showEmailDropdown = false
                            }
                        )
                        if (!emailAddress.endsWith("@bnbu.edu.cn")) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AlternateEmail,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "$prefix@bnbu.edu.cn",
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "教工/公用邮箱后缀",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    emailAddress = "$prefix@bnbu.edu.cn"
                                    showEmailDropdown = false
                                }
                            )
                        }
                    }
                }

                if (prefix.isNotEmpty() && !emailAddress.endsWith(emailSuffix)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {
                            emailAddress = "$prefix$emailSuffix"
                            showEmailDropdown = false
                        },
                        label = { Text("一键补全: $prefix$emailSuffix", fontSize = 11.sp, color = BnbuBlue) },
                        icon = {
                            Icon(
                                Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = BnbuBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = BnbuBlue.copy(alpha = 0.08f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("邮箱密码 / 客户端授权码 *") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 腾讯企业邮箱客户端专用密码指引
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = BnbuBlue.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BnbuBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("腾讯企业邮箱特别注意：", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BnbuNavy)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "若您的邮箱开通了微信/企业微信安全扫码登录，请使用【客户端专用密码】（而非网页登录密码）！\n\n获取方式：\n电脑登录学校腾讯企业邮网页版 (exmail.qq.com) → 进入「设置」→「客户端设置」开启 POP3/IMAP 服务 → 点击「生成新密码」并填入上方密码框。",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            lineHeight = 14.5.sp
                        )
                    }
                }

                // 高级服务器配置 (折叠面板)
                var showAdvancedSettings by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .clickable { showAdvancedSettings = !showAdvancedSettings },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = BnbuBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showAdvancedSettings) "收起高级服务器设置" else "展开高级服务器配置 ($protocol / 端口)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BnbuBlue
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = BnbuBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showAdvancedSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "服务器连接参数 ($protocol / SMTP):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = incomingServer,
                                onValueChange = { incomingServer = it },
                                label = { Text("$protocol 收件服务器") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = incomingPort,
                                onValueChange = { incomingPort = it },
                                label = { Text("端口") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "收信启用 SSL/TLS 加密传输", fontSize = 12.sp, color = TextSecondary)
                            Switch(
                                checked = useIncomingSsl,
                                onCheckedChange = {
                                    useIncomingSsl = it
                                    incomingPort = if (it) (if (protocol == "IMAP") "993" else "995") else (if (protocol == "IMAP") "143" else "110")
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = outgoingServer,
                                onValueChange = { outgoingServer = it },
                                label = { Text("SMTP 发件服务器") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = outgoingPort,
                                onValueChange = { outgoingPort = it },
                                label = { Text("端口") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "发件启用 SSL/TLS 加密传输", fontSize = 12.sp, color = TextSecondary)
                            Switch(
                                checked = useOutgoingSsl,
                                onCheckedChange = {
                                    useOutgoingSsl = it
                                    outgoingPort = if (it) "465" else "25"
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "启动时自动拉取最新邮件与日程", fontSize = 12.sp, color = TextSecondary)
                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { isAutoSyncEnabled = it },
                        modifier = Modifier.height(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            guardAction {
                                val inPort = incomingPort.toIntOrNull() ?: 993
                                val outPort = outgoingPort.toIntOrNull() ?: 465
                                val newConfig = currentConfig.copy(
                                    protocol = protocol,
                                    emailAddress = emailAddress.trim(),
                                    password = password,
                                    incomingServer = incomingServer.trim(),
                                    incomingPort = inPort,
                                    useIncomingSsl = useIncomingSsl,
                                    outgoingServer = outgoingServer.trim(),
                                    outgoingPort = outPort,
                                    useOutgoingSsl = useOutgoingSsl,
                                    isAutoSyncEnabled = isAutoSyncEnabled,
                                    isConnected = true,
                                    lastSyncTime = System.currentTimeMillis()
                                )
                                onSaveConfig(newConfig)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存并连接邮箱")
                    }
                }
            }
        }
    }
}
