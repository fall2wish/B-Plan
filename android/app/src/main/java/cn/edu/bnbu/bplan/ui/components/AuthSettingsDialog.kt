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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.ui.theme.*

@Composable
fun AuthSettingsDialog(
    currentName: String,
    currentId: String,
    currentProgramme: String,
    currentGsasToken: String,
    currentIspaceCookie: String,
    isAuthorized: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, id: String, prog: String, gsasToken: String, ispaceCookie: String) -> Unit,
    onLogout: () -> Unit
) {
    var studentName by remember { mutableStateOf(currentName) }
    var studentId by remember { mutableStateOf(currentId) }
    var programme by remember { mutableStateOf(currentProgramme) }
    var gsasToken by remember { mutableStateOf(currentGsasToken) }
    var ispaceCookie by remember { mutableStateOf(currentIspaceCookie) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Icon(Icons.Default.Badge, contentDescription = null, tint = BnbuBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.auth_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BnbuNavy
                            )
                            Text(
                                text = stringResource(R.string.auth_dialog_subtitle),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Student Name
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text(stringResource(R.string.auth_name_label)) },
                    placeholder = { Text(stringResource(R.string.auth_name_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BnbuNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Student ID
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text(stringResource(R.string.auth_student_id_label)) },
                    placeholder = { Text(stringResource(R.string.auth_student_id_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = BnbuNavy) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Programme
                OutlinedTextField(
                    value = programme,
                    onValueChange = { programme = it },
                    label = { Text(stringResource(R.string.auth_programme_label)) },
                    placeholder = { Text(stringResource(R.string.auth_programme_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = BnbuNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.auth_credentials_section),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GSAS Access Token
                OutlinedTextField(
                    value = gsasToken,
                    onValueChange = { gsasToken = it },
                    label = { Text(stringResource(R.string.auth_gsas_token_label)) },
                    placeholder = { Text("例如: eyJhbGciOiJIUzI1NiIs...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = BnbuBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // iSpace MoodleSession Cookie
                OutlinedTextField(
                    value = ispaceCookie,
                    onValueChange = { ispaceCookie = it },
                    label = { Text(stringResource(R.string.auth_ispace_cookie_label)) },
                    placeholder = { Text("例如: MoodleSession 字符串") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFD97706)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Button(
                    onClick = {
                        onSave(
                            studentName.trim(),
                            studentId.trim(),
                            programme.trim(),
                            gsasToken.trim(),
                            ispaceCookie.trim()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.auth_save_btn), fontWeight = FontWeight.Bold)
                }

                if (isAuthorized || currentName.isNotBlank() || currentId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.auth_logout_btn))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
