package cn.edu.bnbu.bplan.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全自动后台静默登录认证引擎 (SilentCampusAuthEngine)
 *
 * 核心设计原则：
 * 1. 彻底脱离 UI 视图树，使用 Headless 独立 WebKit 自动化，零弹窗阻断；
 * 2. 依次静默自动完成 CAS 统一认证、GSAS 教务系统 Token 收割以及 iSpace 教学平台 MoodleSession 获取；
 * 3. 失败计数器 (silentFailureCount)：连续失败达到 2 次后才激活 requiresManualIntervention 标志唤起可见弹窗兜底；
 * 4. 成功后自动调用 BPlanRepository 差量同步并重置计数器为 0。
 */
class SilentCampusAuthEngine(
    private val context: Context,
    private val repository: BPlanRepository
) {
    companion object {
        private const val TAG = "SilentCampusAuth"
        private const val GSAS_URL = "https://gsas.bnbu.edu.cn"
        private const val ISPACE_URL = "https://ispace.bnbu.edu.cn/login/index.php"
        private const val AUTH_TIMEOUT_MS = 28000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _requiresManualIntervention = MutableStateFlow(false)
    val requiresManualIntervention: StateFlow<Boolean> = _requiresManualIntervention.asStateFlow()

    private var silentFailureCount = 0

    fun resetFailureCount() {
        silentFailureCount = 0
        _requiresManualIntervention.value = false
    }

    /**
     * 触发全自动后台静默认证与数据抓取
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun executeSilentAuthAndSync(forceReauth: Boolean = false): Result<String> {
        val creds = repository.getCampusAccount()
        if (creds == null || creds.first.isBlank() || creds.second.isBlank()) {
            return Result.failure(IllegalStateException("尚未绑定学号密码，无法自动登录"))
        }

        if (_isAuthenticating.value) {
            return Result.failure(IllegalStateException("后台静默登录已在进行中"))
        }

        _isAuthenticating.value = true
        _progressMessage.value = "正在后台静默验证校园统一身份..."

        val safeU = creds.first.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
        val safeP = creds.second.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")

        return withContext(Dispatchers.Main) {
            val completionDeferred = CompletableDeferred<Result<String>>()
            var headlessWebView: WebView? = null
            var timeoutJob: Job? = null

            fun cleanup() {
                timeoutJob?.cancel()
                try {
                    headlessWebView?.apply {
                        stopLoading()
                        webViewClient = WebViewClient()
                        webChromeClient = null
                        destroy()
                    }
                } catch (_: Exception) {}
                headlessWebView = null
                _isAuthenticating.value = false
            }

            try {
                var gsasTokenCaptured = false
                var ispaceSessionCaptured = false

                val wv = WebView(context.applicationContext)
                headlessWebView = wv

                if (forceReauth) {
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()
                    try {
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                    } catch (_: Exception) {}
                }

                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                }

                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(wv, true)

                fun handleAutoFill(view: WebView) {
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
                                setTimeout(function() { btn.click(); }, 350);
                            }
                            return filled;
                        })()
                    """.trimIndent()
                    view.evaluateJavascript(autoFillJs, null)
                }

                fun onGsasTokenExtracted(rawToken: String) {
                    if (gsasTokenCaptured) return
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

                    gsasTokenCaptured = true
                    _progressMessage.value = "GSAS 凭据捕获成功，正在前往 iSpace..."
                    Log.d(TAG, "GSAS token successfully captured silently")

                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateGsasCredentials(cleanToken)
                        repository.syncGsasTimetable()

                        withContext(Dispatchers.Main) {
                            // 前往 iSpace 获取 Moodle 凭据
                            wv.loadUrl(ISPACE_URL)
                        }
                    }
                }

                fun onIspaceExtracted(cookies: String, sesskey: String) {
                    if (ispaceSessionCaptured) return
                    val regex = Regex("""MoodleSession=([^;]+)""")
                    val match = regex.find(cookies) ?: return
                    val sessionCookie = match.groupValues[1].trim()
                    if (sessionCookie.isBlank() || sesskey.isBlank() || sesskey.equals("null", ignoreCase = true)) return

                    ispaceSessionCaptured = true
                    _progressMessage.value = "全自动认证成功！已同步教务课表与 iSpace 待办"
                    Log.d(TAG, "iSpace session successfully captured silently")

                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateIspaceCredentials(sessionCookie, sesskey)
                        repository.syncIspaceData()

                        resetFailureCount()
                        cleanup()
                        if (!completionDeferred.isCompleted) {
                            completionDeferred.complete(Result.success("全自动登录与同步成功完成！"))
                        }
                    }
                }

                wv.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains("gsas.bnbu.edu.cn")) {
                            val headers = request?.requestHeaders ?: emptyMap()
                            val token = headers["pro__Access-Token"]
                                ?: headers["Access-Token"]
                                ?: headers["Authorization"]
                                ?: headers["access-token"]
                                ?: headers["authorization"]

                            if (!token.isNullOrBlank() && !token.contains("null") && !token.contains("undefined") && !gsasTokenCaptured) {
                                view?.post { onGsasTokenExtracted(token) }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val curUrl = url ?: ""
                        if (curUrl.contains("/login/") || curUrl.contains("cas/login") || curUrl.contains("/cas/")) {
                            view?.let { handleAutoFill(it) }
                            return
                        }

                        if (!gsasTokenCaptured && curUrl.contains("gsas.bnbu.edu.cn")) {
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
                            view?.evaluateJavascript(jsCheck) { res ->
                                val clean = res?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                if (!clean.isNullOrBlank() && clean != "null" && clean != "undefined") {
                                    onGsasTokenExtracted(clean)
                                }
                            }
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
                                                    view.post { onIspaceExtracted(cookies, sess) }
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }

                // 定时轮询兜底提取
                val pollJob = CoroutineScope(Dispatchers.Main).launch {
                    while (isActive && (!gsasTokenCaptured || !ispaceSessionCaptured)) {
                        delay(1200)
                        if (!gsasTokenCaptured) {
                            val checkJs = "try { localStorage.getItem('pro__Access-Token') || localStorage.getItem('Access-Token') || '' } catch(e) { '' }"
                            wv.evaluateJavascript(checkJs) { res ->
                                val clean = res?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                if (!clean.isNullOrBlank() && clean != "null" && clean != "undefined") {
                                    onGsasTokenExtracted(clean)
                                }
                            }
                        }
                        if (gsasTokenCaptured && !ispaceSessionCaptured) {
                            val cookies = CookieManager.getInstance().getCookie(wv.url ?: "") ?: ""
                            if (cookies.contains("MoodleSession=")) {
                                val sessJs = "try { (window.M && window.M.cfg && window.M.cfg.sesskey) || '' } catch(e) { '' }"
                                wv.evaluateJavascript(sessJs) { res ->
                                    val clean = res?.removeSurrounding("\"")?.replace("\\\"", "")
                                    if (!clean.isNullOrBlank() && clean != "null") {
                                        onIspaceExtracted(cookies, clean)
                                    }
                                }
                            }
                        }
                    }
                }

                // 启动加载第一站 GSAS
                wv.loadUrl(GSAS_URL)

                // 超时监控机制
                timeoutJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(AUTH_TIMEOUT_MS)
                    pollJob.cancel()
                    if (!completionDeferred.isCompleted) {
                        silentFailureCount++
                        Log.w(TAG, "Silent auth timed out, continuous failures: $silentFailureCount")
                        if (silentFailureCount >= 2) {
                            _requiresManualIntervention.value = true
                        }
                        cleanup()
                        completionDeferred.complete(
                            Result.failure(Exception("后台静默登录超时 (连续失败 $silentFailureCount 次)，若失败达到 2 次将弹出辅助窗口"))
                        )
                    }
                }

            } catch (e: Exception) {
                silentFailureCount++
                if (silentFailureCount >= 2) {
                    _requiresManualIntervention.value = true
                }
                cleanup()
                completionDeferred.complete(Result.failure(e))
            }

            completionDeferred.await()
        }
    }
}
