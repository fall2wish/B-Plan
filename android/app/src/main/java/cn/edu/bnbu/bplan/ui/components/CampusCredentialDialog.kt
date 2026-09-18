package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.theme.*

@Composable
fun CampusCredentialDialog(
    repository: BPlanRepository,
    onDismiss: () -> Unit,
    onSaveAndSync: () -> Unit,
    onCleared: () -> Unit
) {
    val currentSavedUsername by repository.campusAccountUsername.collectAsState()
    val hasCredentials by repository.hasSecureCredentials.collectAsState()

    var username by remember {
        val existing = repository.getCampusAccount()
        mutableStateOf(existing?.first ?: currentSavedUsername.ifBlank { repository.studentId.value })
    }
    var password by remember {
        val existing = repository.getCampusAccount()
        mutableStateOf(existing?.second ?: "")
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = BnbuNavy,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "校园统一身份认证",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BnbuNavy
                            )
                            Text(
                                text = "芯片级物理隔离安全存储",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Security Explainer Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BnbuBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BnbuBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Android KeyStore (AES-256-GCM) 硬件加密",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BnbuNavy
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "凭据直接由设备独立安全芯片（TEE/StrongBox）硬件隔离加密，不上传服务器，数据仅在本地加密保存。任何其他第三方软件均无法跨沙盒读取。",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Student ID Input
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("学生学号 / 账号 (Student ID)", fontSize = 13.sp) },
                    placeholder = { Text("例如: s123456789", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = BnbuBlue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("统一身份认证密码 (SSO Password)", fontSize = 13.sp) },
                    placeholder = { Text("输入学校统一认证密码", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = BnbuBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "隐藏密码" else "显示密码",
                                tint = TextMuted
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Button(
                    onClick = {
                        if (username.isBlank()) {
                            errorMessage = "请输入学号 / 账号"
                            return@Button
                        }
                        if (password.isBlank()) {
                            errorMessage = "请输入统一身份认证密码"
                            return@Button
                        }
                        repository.saveCampusAccount(username.trim(), password)
                        onSaveAndSync()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasCredentials) "更新加密凭据并同步数据" else "安全保存并立即同步",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }

                if (hasCredentials) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            repository.clearCampusAccount()
                            username = ""
                            password = ""
                            onCleared()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("解除绑定并清除本地凭据", fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
