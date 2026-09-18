package cn.edu.bnbu.bplan.ui.components

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.*
import cn.edu.bnbu.bplan.data.security.TrustedTimeManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.theme.*
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class WebSyncTarget {
    GSAS,
    ISPACE
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusWebAuthDialog(
    repository: BPlanRepository,
    initialTarget: WebSyncTarget = WebSyncTarget.GSAS,
    forceReauth: Boolean = false,
    onDismiss: () -> Unit,
    onSyncSuccess: (message: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTarget by remember { mutableStateOf(initialTarget) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                webViewClient = android.webkit.WebViewClient()
                webChromeClient = null
                destroy()
            }
            webViewRef = null
        }
    }

    var isGsasSynced by remember { mutableStateOf(!forceReauth && repository.gsasToken.isNotBlank() && repository.courses.value.isNotEmpty()) }
    var isIspaceSynced by remember { mutableStateOf(!forceReauth && repository.ispaceCookie.isNotBlank() && repository.ddls.value.isNotEmpty()) }
    var isExtracting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("请在下方网页登录校园账号，将自动同步课表与作业") }

    val gsasUrl = "https://gsas.bnbu.edu.cn"
    val ispaceUrl = "https://ispace.bnbu.edu.cn/login/index.php"

    // 处理 GSAS Token
    fun handleGsasToken(rawToken: String) {
        if (isExtracting || isGsasSynced) return
        var cleanToken = rawToken.trim().removeSurrounding("\"")
        if (cleanToken.startsWith("{")) {
            try {
                val json = JsonParser.parseString(cleanToken).asJsonObject
                cleanToken = when {
                    json.has("value") -> json.get("value").asString
                    json.has("token") -> json.get("token").asString
                    else -> cleanToken
                }
            } catch (_: Exception) {}
        }
        if (cleanToken.isBlank() || cleanToken.equals("null", ignoreCase = true) || cleanToken.equals("undefined", ignoreCase = true)) return

        isExtracting = true
        statusText = "检测到教务系统登录凭据，正在提取课表并验证身份..."

        coroutineScope.launch {
            repository.updateGsasCredentials(cleanToken)
            val result = repository.syncGsasTimetable()
            isExtracting = false
            if (result.isSuccess) {
                isGsasSynced = true
                val courseCount = result.getOrNull()?.size ?: repository.courses.value.size
                statusText = "GSAS 课表同步成功 (已收录 $courseCount 门课)！"
                delay(1200)
                // 自动导航至 iSpace
                currentTarget = WebSyncTarget.ISPACE
                statusText = "正在前往 iSpace 教学平台同步作业待办..."
                webViewRef?.loadUrl(ispaceUrl)
            } else {
                val err = result.exceptionOrNull()?.message ?: "同步失败"
                statusText = "教务同步未完成: $err"
                if (result.exceptionOrNull() is SecurityException) {
                    // 学号不匹配，彻底清理会话，绝不前进至 iSpace
                    repository.clearSessionTokensAndWebStorage()
                } else {
                    delay(1500)
                    currentTarget = WebSyncTarget.ISPACE
                    webViewRef?.loadUrl(ispaceUrl)
                }
            }
        }
    }

    // 处理 iSpace 会话 Cookie
    fun handleIspaceSession(cookieStr: String, sesskey: String = "") {
        if (isExtracting || isIspaceSynced) return
        val regex = Regex("""MoodleSession=([^;]+)""")
        val match = regex.find(cookieStr) ?: return
        val sessionCookie = match.groupValues[1].trim()
        if (sessionCookie.isBlank() || sesskey.isBlank() || sesskey.equals("null", ignoreCase = true)) return

        isExtracting = true
        statusText = "检测到 iSpace 登录凭证，正在同步作业待办与 TA 辅导课..."

        coroutineScope.launch {
            repository.updateIspaceCredentials(sessionCookie, sesskey)
            val result = repository.syncIspaceData()
            isExtracting = false
            if (result.isSuccess) {
                isIspaceSynced = true
                val ddlCount = result.getOrNull()?.size ?: repository.ddls.value.size
                statusText = "iSpace 同步成功 (已更新 $ddlCount 项待办及 TA 辅导课)！"
                delay(1200)
                onSyncSuccess("课表与作业已成功同步！")
                onDismiss()
            } else {
                val err = result.exceptionOrNull()?.message ?: "同步失败"
                statusText = "同步未完成: $err，请确认登录成功后点击重试"
            }
        }
    }

    fun tryAutoFillCredentials(wv: WebView?) {
        val view = wv ?: webViewRef ?: return
        val creds = repository.getCampusAccount() ?: return
        val safeU = creds.first.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
        val safeP = creds.second.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
        val autoFillJs = """
            (function() {
                function setNativeValue(element, value) {
                    var valueSetter = Object.getOwnPropertyDescriptor(element, 'value') ? Object.getOwnPropertyDescriptor(element, 'value').set : null;
                    var prototype = Object.getPrototypeOf(element);
                    var prototypeValueSetter = Object.getOwnPropertyDescriptor(prototype, 'value') ? Object.getOwnPropertyDescriptor(prototype, 'value').set : null;
                    if (prototypeValueSetter && valueSetter !== prototypeValueSetter) {
                        prototypeValueSetter.call(element, value);
                    } else if (valueSetter) {
                        valueSetter.call(element, value);
                    } else {
                        element.value = value;
                    }
                    element.dispatchEvent(new Event('input', { bubbles: true }));
                    element.dispatchEvent(new Event('change', { bubbles: true }));
                }

                var u = document.querySelector('input[name="username"], #username, input[id*="user" i], input[name*="user" i], input[placeholder*="账号"], input[placeholder*="学号"], input[placeholder*="User" i], input[placeholder*="用户名"]');
                var p = document.querySelector('input[name="password"], #password, input[type="password"], input[placeholder*="Password" i], input[placeholder*="密码"]');
                var filled = false;
                if (u && (!u.value || u.value !== "$safeU")) {
                    setNativeValue(u, "$safeU");
                    filled = true;
                }
                if (p && !p.value) {
                    setNativeValue(p, "$safeP");
                    filled = true;
                }
                var btn = document.querySelector('button[type="submit"], input[type="submit"], .btn-login, #login-btn, #loginBtn, .login-button, button.btn-primary, button.ant-btn-primary');
                if (btn && !window.__auto_submitted__) {
                    window.__auto_submitted__ = true;
                    setTimeout(function() { btn.click(); }, 400);
                }
                return filled;
            })()
        """.trimIndent()
        view.evaluateJavascript(autoFillJs) { res ->
            if (res == "true") {
                statusText = "检测到硬件加密凭据，正在自动填入并登录..."
            }
        }
    }

    // 轮询检查 WebView 本地存储与 Cookie 及时间完整性
    LaunchedEffect(currentTarget, isGsasSynced, isIspaceSynced) {
        while (true) {
            delay(1500)
            val wv = webViewRef ?: continue
            val curUrl = wv.url ?: ""

            if (TrustedTimeManager.isSystemTimeTampered()) {
                statusText = "安全警告: 系统本地时间与权威授时偏差过大，请校准手机时间以防重放攻击！"
            }

            if (curUrl.contains("/login") || curUrl.contains("cas/login") || curUrl.contains("/cas/")) {
                tryAutoFillCredentials(wv)
                val errorDetectJs = """
                    (function() {
                        try {
                            var err = document.querySelector('.errors, .error, #msg.errors, .alert-danger, .login-error, #error_box, [class*="error"], [id*="error"]');
                            if (err && err.innerText && err.innerText.trim().length > 0) {
                                return err.innerText.trim();
                            }
                            return "";
                        } catch(e) { return ""; }
                    })()
                """.trimIndent()
                wv.evaluateJavascript(errorDetectJs) { errMsg ->
                    val clean = errMsg?.removeSurrounding("\"")?.replace("\\\"", "\"")?.trim() ?: ""
                    if (clean.isNotBlank() && clean != "null" && clean != "undefined") {
                        statusText = "统一认证失败: $clean"
                    }
                }
            }

            if (currentTarget == WebSyncTarget.GSAS && !isGsasSynced && !isExtracting) {
                // 严禁在登录页面或认证中提取 Token，确保用户真实完成认证重定向后才收割 Token
                if (!curUrl.contains("/login") && !curUrl.contains("cas/login")) {
                    val jsCheck = """
                        (function() {
                            try {
                                var t = null;
                                for (var i = 0; i < localStorage.length; i++) {
                                    var k = localStorage.key(i);
                                    if (k === 'pro__Access-Token' || k === 'Access-Token' || k.indexOf('token') !== -1) {
                                        t = localStorage.getItem(k);
                                        break;
                                    }
                                }
                                return t || "";
                            } catch(e) { return ""; }
                        })()
                    """.trimIndent()
                    wv.evaluateJavascript(jsCheck) { res ->
                        val clean = res?.removeSurrounding("\"")?.replace("\\\"", "\"")
                        if (!clean.isNullOrBlank() && clean != "null" && clean != "undefined") {
                            handleGsasToken(clean)
                        }
                    }
                }
            } else if (currentTarget == WebSyncTarget.ISPACE && !isIspaceSynced && !isExtracting) {
                // 严禁在登录页触发会话同步
                if (!curUrl.contains("/login/") && !curUrl.contains("cas/login")) {
                    val cookies = CookieManager.getInstance().getCookie("https://ispace.bnbu.edu.cn") ?: ""
                    if (cookies.contains("MoodleSession=")) {
                        val jsCheck = """
                            (function() {
                                try {
                                    var isNotLoggedIn = document.body.classList.contains('notloggedin');
                                    var hasUserMenu = !!document.querySelector('.usermenu, .userpicture, a[href*="logout.php"], #page-my-index');
                                    var sesskey = (window.M && window.M.cfg && window.M.cfg.sesskey) || "";
                                    if (!sesskey) {
                                        var m = document.documentElement.innerHTML.match(/"sesskey":\s*"([a-zA-Z0-9]+)"/);
                                        if (m) sesskey = m[1];
                                    }
                                    var loggedIn = (!isNotLoggedIn || hasUserMenu) && sesskey.length > 0;
                                    return JSON.stringify({ loggedIn: loggedIn, sesskey: sesskey });
                                } catch(e) { return JSON.stringify({ loggedIn: false }); }
                            })()
                        """.trimIndent()
                        wv.evaluateJavascript(jsCheck) { res ->
                            try {
                                val clean = res?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                if (!clean.isNullOrBlank() && clean.startsWith("{")) {
                                    val obj = JsonParser.parseString(clean).asJsonObject
                                    if (obj.get("loggedIn")?.asBoolean == true) {
                                        val sess = obj.get("sesskey")?.asString ?: ""
                                        if (sess.isNotBlank()) {
                                            handleIspaceSession(cookies, sess)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "平台登录与数据自动同步",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy
                        )
                        Text(
                            text = "登录学校统一认证即可自动抓取课表与作业",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = BnbuBlue, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Step Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Step 1: GSAS
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentTarget == WebSyncTarget.GSAS) BnbuNavy.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentTarget = WebSyncTarget.GSAS
                                webViewRef?.loadUrl(gsasUrl)
                            }
                            .padding(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isGsasSynced) SuccessGreen else if (currentTarget == WebSyncTarget.GSAS) BnbuNavy else TextMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGsasSynced) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text("1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "1. GSAS 教务系统",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentTarget == WebSyncTarget.GSAS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTarget == WebSyncTarget.GSAS) BnbuNavy else TextSecondary
                                )
                                Text(
                                    text = if (isGsasSynced) "已同步课表" else "登录抓取课表",
                                    fontSize = 10.sp,
                                    color = if (isGsasSynced) SuccessGreen else TextMuted
                                )
                            }
                        }
                    }

                    // Step 2: iSpace
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentTarget == WebSyncTarget.ISPACE) BnbuNavy.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentTarget = WebSyncTarget.ISPACE
                                webViewRef?.loadUrl(ispaceUrl)
                            }
                            .padding(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isIspaceSynced) SuccessGreen else if (currentTarget == WebSyncTarget.ISPACE) BnbuNavy else TextMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isIspaceSynced) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text("2", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "2. iSpace 教学平台",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentTarget == WebSyncTarget.ISPACE) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTarget == WebSyncTarget.ISPACE) BnbuNavy else TextSecondary
                                )
                                Text(
                                    text = if (isIspaceSynced) "已同步待办" else "登录抓取作业",
                                    fontSize = 10.sp,
                                    color = if (isIspaceSynced) SuccessGreen else TextMuted
                                )
                            }
                        }
                    }
                }

                // Status Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isExtracting) BnbuBlue.copy(alpha = 0.1f) else SurfaceVariantLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = BnbuNavy
                            )
                        } else {
                            Icon(
                                imageVector = if (isGsasSynced && isIspaceSynced) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isGsasSynced && isIspaceSynced) SuccessGreen else BnbuBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = if (isExtracting) BnbuNavy else TextPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                if (forceReauth) {
                                    clearCache(true)
                                    clearFormData()
                                    clearHistory()
                                    try {
                                        android.webkit.WebStorage.getInstance().deleteAllData()
                                        val cm = CookieManager.getInstance()
                                        cm.removeAllCookies(null)
                                        cm.flush()
                                    } catch (_: Exception) {}
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val url = request?.url?.toString() ?: ""
                                        if (url.contains("gsas.bnbu.edu.cn")) {
                                            val headers = request?.requestHeaders ?: emptyMap()
                                            val token = headers["pro__Access-Token"]
                                                ?: headers["Access-Token"]
                                                ?: headers["Authorization"]
                                                ?: headers["access-token"]
                                                ?: headers["authorization"]

                                            if (!token.isNullOrBlank() && !token.contains("null") && !token.contains("undefined") && !isGsasSynced) {
                                                view?.post { handleGsasToken(token) }
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        handler?.cancel() // 绝对严禁 proceed()，遭受篡改/非法证书时立即断开连接
                                        val reason = when (error?.primaryError) {
                                            SslError.SSL_EXPIRED -> "服务器证书已过期"
                                            SslError.SSL_IDMISMATCH -> "服务器证书域名不匹配"
                                            SslError.SSL_UNTRUSTED -> "证书不受信任（可能存在代理抓包或中间人劫持）"
                                            SslError.SSL_NOTYETVALID -> "服务器证书尚未生效（请检查手机时间）"
                                            else -> "网络连接安全校验未通过"
                                        }
                                        statusText = "安全警告: $reason，已终止连接以保护账号安全！"
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val curUrl = url ?: ""
                                        if (curUrl.contains("/login/") || curUrl.contains("cas/login") || curUrl.contains("/cas/")) {
                                            tryAutoFillCredentials(view)
                                            return
                                        }
                                        if (curUrl.contains("ispace.bnbu.edu.cn")) {
                                            val cookies = CookieManager.getInstance().getCookie(curUrl) ?: ""
                                            if (cookies.contains("MoodleSession=")) {
                                                val jsCheck = """
                                                    (function() {
                                                        try {
                                                            var isNotLoggedIn = document.body.classList.contains('notloggedin');
                                                            var hasUserMenu = !!document.querySelector('.usermenu, .userpicture, a[href*="logout.php"], #page-my-index');
                                                            var sesskey = (window.M && window.M.cfg && window.M.cfg.sesskey) || "";
                                                            if (!sesskey) {
                                                                var m = document.documentElement.innerHTML.match(/"sesskey":\s*"([a-zA-Z0-9]+)"/);
                                                                if (m) sesskey = m[1];
                                                            }
                                                            var loggedIn = (!isNotLoggedIn || hasUserMenu) && sesskey.length > 0;
                                                            return JSON.stringify({ loggedIn: loggedIn, sesskey: sesskey });
                                                        } catch(e) { return JSON.stringify({ loggedIn: false }); }
                                                    })()
                                                """.trimIndent()
                                                view?.evaluateJavascript(jsCheck) { res ->
                                                    try {
                                                        val clean = res?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                                        if (!clean.isNullOrBlank() && clean.startsWith("{")) {
                                                            val obj = JsonParser.parseString(clean).asJsonObject
                                                            if (obj.get("loggedIn")?.asBoolean == true) {
                                                                val sess = obj.get("sesskey")?.asString ?: ""
                                                                if (sess.isNotBlank()) {
                                                                    view.post { handleIspaceSession(cookies, sess) }
                                                                }
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }
                                    }
                                }

                                loadUrl(if (currentTarget == WebSyncTarget.GSAS) gsasUrl else ispaceUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentTarget == WebSyncTarget.GSAS) {
                        OutlinedButton(
                            onClick = {
                                currentTarget = WebSyncTarget.ISPACE
                                webViewRef?.loadUrl(ispaceUrl)
                                statusText = "已跳过教务系统，前往 iSpace 教学平台..."
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("跳过教务", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                currentTarget = WebSyncTarget.GSAS
                                webViewRef?.loadUrl(gsasUrl)
                                statusText = "已切换回 GSAS 教务系统..."
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("返回教务", fontSize = 12.sp)
                        }
                    }

                    if (currentTarget == WebSyncTarget.ISPACE && !isIspaceSynced) {
                        Button(
                            onClick = {
                                val wv = webViewRef ?: return@Button
                                val curUrl = wv.url ?: ""
                                if (curUrl.contains("/login/") || curUrl.contains("cas/login")) {
                                    statusText = "请先在下方网页完成登录认证后再点击同步"
                                    return@Button
                                }
                                val cookies = CookieManager.getInstance().getCookie("https://ispace.bnbu.edu.cn") ?: ""
                                val jsCheck = """
                                    (function() {
                                        try {
                                            var sesskey = (window.M && window.M.cfg && window.M.cfg.sesskey) || "";
                                            if (!sesskey) {
                                                var m = document.documentElement.innerHTML.match(/"sesskey":\s*"([a-zA-Z0-9]+)"/);
                                                if (m) sesskey = m[1];
                                            }
                                            return sesskey;
                                        } catch(e) { return ""; }
                                    })()
                                """.trimIndent()
                                wv.evaluateJavascript(jsCheck) { res ->
                                    val cleanSess = res?.removeSurrounding("\"")?.replace("\\\"", "") ?: ""
                                    if (cleanSess.isNotBlank() && cleanSess != "null") {
                                        handleIspaceSession(cookies, cleanSess)
                                    } else {
                                        statusText = "尚未获取到登录会话，请先在下方完成登录"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("立即同步待办与TA课", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (repository.hasSecureCredentials.value) {
                        OutlinedButton(
                            onClick = { tryAutoFillCredentials(webViewRef) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp), tint = BnbuNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("自动填密", fontSize = 11.5.sp, color = BnbuNavy)
                        }
                    }

                    Button(
                        onClick = {
                            if (isGsasSynced || isIspaceSynced) {
                                onSyncSuccess("同步完成！")
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(if (isGsasSynced || isIspaceSynced) "完成" else "关闭", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
