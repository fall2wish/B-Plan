package cn.edu.bnbu.bplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.screens.*
import cn.edu.bnbu.bplan.ui.theme.BPlanTheme
import cn.edu.bnbu.bplan.ui.theme.BnbuNavy

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import cn.edu.bnbu.bplan.ui.components.CampusCredentialDialog
import cn.edu.bnbu.bplan.ui.components.CampusWebAuthDialog
import cn.edu.bnbu.bplan.ui.components.LocalAgreementGuard
import cn.edu.bnbu.bplan.ui.components.UserAgreementDialog
import cn.edu.bnbu.bplan.ui.components.WebSyncTarget
import cn.edu.bnbu.bplan.ui.theme.BnbuBlue
import cn.edu.bnbu.bplan.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String, @StringRes val titleRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Timetable : Screen("timetable", R.string.nav_timetable, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Ddl : Screen("ddl", R.string.nav_ddl, Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object Mail : Screen("mail", R.string.nav_mail, Icons.Filled.Email, Icons.Outlined.Email)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: BPlanRepository

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = BPlanRepository(applicationContext)

        lifecycleScope.launch(Dispatchers.IO) {
            if (repository.hasAgreedUserAgreement.value) {
                repository.autoSyncOnAppLaunch()
            }
        }

        setContent {
            val appLanguage by repository.appLanguage.collectAsState()
            val localizedContext = remember(appLanguage) {
                LocaleHelper.wrap(this, appLanguage)
            }
            val localizedConfig = remember(appLanguage) {
                LocaleHelper.getLocalizedConfiguration(this, appLanguage)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig
            ) {
                BPlanTheme {
                    MainAppScaffold(repository = repository)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(repository: BPlanRepository) {
    val items = listOf(
        Screen.Timetable,
        Screen.Ddl,
        Screen.Mail,
        Screen.Settings
    )
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Timetable) }
    val hasAgreedUserAgreement by repository.hasAgreedUserAgreement.collectAsState()
    val hasSecureCredentials by repository.hasSecureCredentials.collectAsState()
    var showAgreementDialog by remember { mutableStateOf(!hasAgreedUserAgreement) }
    var showFirstTimeCredentialDialog by remember { mutableStateOf(false) }
    var showRestrictedNoticeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isSilentAuthenticating by repository.isSilentAuthenticating.collectAsState()
    val silentAuthProgress by repository.silentAuthProgress.collectAsState()
    val requiresManualIntervention by repository.requiresManualIntervention.collectAsState()
    var showManualFallbackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requiresManualIntervention) {
        if (requiresManualIntervention) {
            showManualFallbackDialog = true
        }
    }

    val guardAction: (() -> Unit) -> Unit = { action ->
        if (hasAgreedUserAgreement) {
            action()
        } else {
            showRestrictedNoticeDialog = true
        }
    }

    if (showAgreementDialog) {
        UserAgreementDialog(
            isFirstLaunch = !hasAgreedUserAgreement,
            onAccept = {
                repository.setHasAgreedUserAgreement(true)
                showAgreementDialog = false
                if (!hasSecureCredentials) {
                    showFirstTimeCredentialDialog = true
                } else {
                    scope.launch(Dispatchers.IO) {
                        repository.autoSyncOnAppLaunch()
                    }
                }
            },
            onBrowseOnly = {
                showAgreementDialog = false
            },
            onDismiss = {
                showAgreementDialog = false
            }
        )
    }

    if (showFirstTimeCredentialDialog) {
        CampusCredentialDialog(
            repository = repository,
            onDismiss = {
                showFirstTimeCredentialDialog = false
            },
            onSaveAndSync = {
                showFirstTimeCredentialDialog = false
                scope.launch(Dispatchers.IO) {
                    repository.triggerSilentAuthAndSync(forceReauth = false)
                }
            },
            onCleared = {
                showFirstTimeCredentialDialog = false
            }
        )
    }

    if (showManualFallbackDialog) {
        CampusWebAuthDialog(
            repository = repository,
            initialTarget = WebSyncTarget.GSAS,
            forceReauth = false,
            onDismiss = {
                showManualFallbackDialog = false
                repository.resetManualInterventionFlag()
            },
            onSyncSuccess = {
                showManualFallbackDialog = false
                repository.resetManualInterventionFlag()
            }
        )
    }

    if (showRestrictedNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedNoticeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "功能受限提醒",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BnbuNavy
                )
            },
            text = {
                Text(
                    text = "您当前处于「仅浏览模式」（未签署免责协议）。\n\n根据学校网络安全合规与个人责任防范要求，账号绑定、教务/iSpace同步及数据修改功能已锁定。\n\n如需使用完整功能，请阅读并签署《用户须知与免责协议》。",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedNoticeDialog = false
                        showAgreementDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("立即签署解锁", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRestrictedNoticeDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("继续仅浏览", fontSize = 13.sp)
                }
            }
        )
    }

    CompositionLocalProvider(LocalAgreementGuard provides guardAction) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    items.forEach { screen ->
                        val isSelected = currentScreen.route == screen.route
                        val screenTitle = stringResource(screen.titleRes)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screenTitle
                                )
                            },
                            label = {
                                Text(
                                    text = screenTitle,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BnbuNavy,
                                selectedTextColor = BnbuNavy,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!hasAgreedUserAgreement) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAgreementDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "仅浏览模式：教务同步与日程修改功能已锁定",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "签署解锁 >",
                                    fontSize = 11.5.sp,
                                    color = BnbuNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isSilentAuthenticating) {
                        Surface(
                            color = BnbuBlue.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp),
                                    color = BnbuNavy,
                                    trackColor = BnbuBlue.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = BnbuBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = silentAuthProgress.ifBlank { "正在全自动静默同步教务课表与 iSpace 待办..." },
                                        fontSize = 11.5.sp,
                                        color = BnbuNavy,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (currentScreen) {
                            Screen.Timetable -> TimetableScreen(
                                repository = repository,
                                onNavigateToDdl = { currentScreen = Screen.Ddl }
                            )
                            Screen.Ddl -> DdlScreen(repository = repository)
                            Screen.Mail -> MailScreen(repository = repository)
                            Screen.Settings -> SettingsScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }
}
