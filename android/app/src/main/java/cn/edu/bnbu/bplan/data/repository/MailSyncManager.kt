package cn.edu.bnbu.bplan.data.repository

import android.util.Base64
import android.util.Log
import cn.edu.bnbu.bplan.data.model.EmailMessage
import cn.edu.bnbu.bplan.data.model.MailConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * 校园邮箱协议管理器 (真实 IMAP / POP3 客户端实现)
 * 专门适配 BNBU 腾讯企业邮箱 (imap.exmail.qq.com:993 / pop.exmail.qq.com:995)
 */
object MailSyncManager {
    private const val TAG = "MailSyncManager"
    private const val CONNECT_TIMEOUT_MS = 12000
    private const val READ_TIMEOUT_MS = 15000

    suspend fun testConnectionAndFetch(config: MailConfig, limit: Int = 15): Result<List<EmailMessage>> = withContext(Dispatchers.IO) {
        if (config.emailAddress.isBlank()) {
            return@withContext Result.failure(Exception("请先填写邮箱账号（例如: student@mail.bnbu.edu.cn）"))
        }
        if (config.password.isBlank()) {
            return@withContext Result.failure(Exception("请先填写邮箱密码或客户端专用授权码"))
        }

        try {
            if (config.protocol.equals("POP3", ignoreCase = true)) {
                fetchViaPop3(config, limit)
            } else {
                fetchViaImap(config, limit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mail sync error", e)
            val friendlyMsg = formatErrorMessage(e, config)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    // --- IMAP 协议实现 ---
    private fun fetchViaImap(config: MailConfig, limit: Int): Result<List<EmailMessage>> {
        var socket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            socket = createSocket(config.incomingServer, config.incomingPort, config.useIncomingSsl)
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)

            // 1. 读取服务器欢迎问候语
            val greeting = reader.readLine() ?: ""
            Log.d(TAG, "IMAP Greeting: $greeting")

            // 2. 登录认证 (TAG1 LOGIN "user" "pass")
            val cleanUser = config.emailAddress.trim().replace("\"", "\\\"")
            val cleanPass = config.password.replace("\"", "\\\"")
            writer.print("A01 LOGIN \"$cleanUser\" \"$cleanPass\"\r\n")
            writer.flush()

            var loginOk = false
            var loginError = ""
            while (true) {
                val line = reader.readLine() ?: break
                Log.d(TAG, "IMAP Login response: $line")
                if (line.startsWith("A01 OK")) {
                    loginOk = true
                    break
                } else if (line.startsWith("A01 NO") || line.startsWith("A01 BAD")) {
                    loginError = line.removePrefix("A01 NO").removePrefix("A01 BAD").trim()
                    break
                }
            }

            if (!loginOk) {
                return Result.failure(Exception(buildAuthErrorMessage(loginError, config)))
            }

            // 3. 选择收件箱 (TAG2 SELECT "INBOX")
            writer.print("A02 SELECT \"INBOX\"\r\n")
            writer.flush()
            var selectOk = false
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith("A02 OK")) {
                    selectOk = true
                    break
                } else if (line.startsWith("A02 NO") || line.startsWith("A02 BAD")) {
                    break
                }
            }
            if (!selectOk) {
                return Result.failure(Exception("成功登录但无法打开收件箱 (SELECT INBOX 失败)"))
            }

            // 4. 搜索所有邮件 ID (TAG3 SEARCH ALL)
            writer.print("A03 SEARCH ALL\r\n")
            writer.flush()
            val msgIds = mutableListOf<String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith("* SEARCH")) {
                    val parts = line.removePrefix("* SEARCH").trim().split("\\s+".toRegex())
                    for (p in parts) {
                        if (p.isNotBlank()) msgIds.add(p)
                    }
                }
                if (line.startsWith("A03 OK") || line.startsWith("A03 NO") || line.startsWith("A03 BAD")) {
                    break
                }
            }

            Log.d(TAG, "IMAP found ${msgIds.size} messages")
            if (msgIds.isEmpty()) {
                sendLogout(writer, reader, "A99")
                return Result.success(emptyList())
            }

            // 取最新的 N 封邮件
            val idsToFetch = msgIds.takeLast(limit).reversed()
            val messages = mutableListOf<EmailMessage>()

            var tagIndex = 10
            for (msgId in idsToFetch) {
                tagIndex++
                val tag = "A$tagIndex"
                // 抓取 Subject, From, Date, Content-Type, Content-Transfer-Encoding 以及正文前 4000 字节
                writer.print("$tag FETCH $msgId (BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE CONTENT-TYPE CONTENT-TRANSFER-ENCODING)] BODY.PEEK[TEXT]<0.4000>)\r\n")
                writer.flush()

                val rawLines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("$tag OK") || line.startsWith("$tag NO") || line.startsWith("$tag BAD")) {
                        break
                    }
                    rawLines.add(line)
                }

                val email = parseImapFetchResponse(msgId, rawLines)
                messages.add(email)
            }

            // 5. 退出连接 (LOGOUT)
            sendLogout(writer, reader, "A99")
            return Result.success(messages)
        } finally {
            try { writer?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    // --- POP3 协议实现 ---
    private fun fetchViaPop3(config: MailConfig, limit: Int): Result<List<EmailMessage>> {
        var socket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            socket = createSocket(config.incomingServer, config.incomingPort, config.useIncomingSsl)
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)

            // 1. 欢迎信息 (+OK)
            val greeting = reader.readLine() ?: ""
            if (!greeting.startsWith("+OK")) {
                return Result.failure(Exception("POP3 服务器响应异常: $greeting"))
            }

            // 2. USER
            writer.print("USER ${config.emailAddress.trim()}\r\n")
            writer.flush()
            val userResp = reader.readLine() ?: ""
            if (!userResp.startsWith("+OK")) {
                return Result.failure(Exception("POP3 用户名验证失败: $userResp"))
            }

            // 3. PASS
            writer.print("PASS ${config.password}\r\n")
            writer.flush()
            val passResp = reader.readLine() ?: ""
            if (!passResp.startsWith("+OK")) {
                return Result.failure(Exception(buildAuthErrorMessage(passResp, config)))
            }

            // 4. STAT 获取邮件总数
            writer.print("STAT\r\n")
            writer.flush()
            val statResp = reader.readLine() ?: ""
            val totalCount = statResp.split(" ").getOrNull(1)?.toIntOrNull() ?: 0

            if (totalCount <= 0) {
                writer.print("QUIT\r\n")
                writer.flush()
                return Result.success(emptyList())
            }

            val messages = mutableListOf<EmailMessage>()
            val start = maxOf(1, totalCount - limit + 1)
            for (idx in totalCount downTo start) {
                // TOP idx 60 (读取头部 + 前 60 行正文)
                writer.print("TOP $idx 60\r\n")
                writer.flush()
                val topResp = reader.readLine() ?: continue
                if (!topResp.startsWith("+OK")) continue

                val rawLines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line == ".") break
                    rawLines.add(line)
                }
                val email = parsePop3TopResponse("pop_$idx", rawLines)
                messages.add(email)
            }

            writer.print("QUIT\r\n")
            writer.flush()
            return Result.success(messages)
        } finally {
            try { writer?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun createSocket(host: String, port: Int, useSsl: Boolean): Socket {
        val socket = if (useSsl) {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = factory.createSocket() as SSLSocket
            sslSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            sslSocket.soTimeout = READ_TIMEOUT_MS
            sslSocket.startHandshake()
            sslSocket
        } else {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            s.soTimeout = READ_TIMEOUT_MS
            s
        }
        return socket
    }

    private fun sendLogout(writer: PrintWriter, reader: BufferedReader, tag: String) {
        try {
            writer.print("$tag LOGOUT\r\n")
            writer.flush()
            var count = 0
            while (count < 5) {
                val l = reader.readLine() ?: break
                if (l.startsWith("$tag OK")) break
                count++
            }
        } catch (_: Exception) {}
    }

    private fun parseImapFetchResponse(id: String, lines: List<String>): EmailMessage {
        val headerLines = mutableListOf<String>()
        val bodyLines = mutableListOf<String>()

        var inHeaders = false
        var inBody = false

        for (line in lines) {
            val trimmed = line.trim()

            if (!inHeaders && !inBody) {
                // 协议起始行：* <id> FETCH (BODY[HEADER.FIELDS ...] {length}
                if (line.startsWith("* ") && (line.contains("FETCH", ignoreCase = true) || line.contains("HEADER", ignoreCase = true))) {
                    inHeaders = true
                    continue
                }
                // 或者直接已经是头部行
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0 && !line.startsWith("*") && !line.startsWith(")")) {
                    inHeaders = true
                    headerLines.add(line)
                    continue
                }
            } else if (inHeaders) {
                // 空行代表 RFC 822 头部字段结束
                if (trimmed.isEmpty()) {
                    inHeaders = false
                    inBody = true
                    continue
                }
                // 部分服务器直接返回 BODY[TEXT 协议段
                if (trimmed.startsWith("BODY[") || trimmed.startsWith("BODY.PEEK[") || trimmed.contains("BODY[TEXT") || (trimmed.startsWith(")") && trimmed.contains("BODY["))) {
                    inHeaders = false
                    inBody = true
                    continue
                }
                headerLines.add(line)
            } else if (inBody) {
                // 正文区：过滤 IMAP 协议包装符
                if (trimmed == ")" || trimmed.startsWith("* ") || trimmed.startsWith("BODY[") || trimmed.startsWith("BODY.PEEK[") || trimmed.matches(Regex("""^\)?\s*BODY\[.*"""))) {
                    continue
                }
                bodyLines.add(line)
            }
        }

        val headers = parseHeaders(headerLines)
        val rawSubject = headers["subject"] ?: "(无主题)"
        val rawFrom = headers["from"] ?: "未知发件人"
        val rawDate = headers["date"] ?: ""

        val subject = decodeMimeHeader(rawSubject).ifBlank { "(无主题)" }
        val sender = cleanSenderDisplay(rawFrom).ifBlank { "未知发件人" }
        val dateTimestamp = parseEmailDate(rawDate)

        val cleanBody = extractReadableBody(bodyLines.joinToString("\n"), headers)
        val extracted = EmailParser.parseEventsFromEmail(id, subject, cleanBody, dateTimestamp)

        return EmailMessage(
            id = "imap_$id",
            subject = subject,
            sender = sender,
            dateTimestamp = dateTimestamp,
            bodyText = cleanBody,
            extractedEvents = extracted,
            isRead = false,
            hasAttachments = false
        )
    }

    private fun parsePop3TopResponse(id: String, lines: List<String>): EmailMessage {
        val headerLines = mutableListOf<String>()
        val bodyLines = mutableListOf<String>()
        var inHeader = true

        for (line in lines) {
            if (inHeader) {
                if (line.isEmpty() || line.trim().isEmpty()) {
                    inHeader = false
                    continue
                }
                headerLines.add(line)
            } else {
                bodyLines.add(line)
            }
        }

        val headers = parseHeaders(headerLines)
        val rawSubject = headers["subject"] ?: "(无主题)"
        val rawFrom = headers["from"] ?: "未知发件人"
        val rawDate = headers["date"] ?: ""

        val subject = decodeMimeHeader(rawSubject).ifBlank { "(无主题)" }
        val sender = cleanSenderDisplay(rawFrom).ifBlank { "未知发件人" }
        val dateTimestamp = parseEmailDate(rawDate)

        val cleanBody = extractReadableBody(bodyLines.joinToString("\n"), headers)
        val extracted = EmailParser.parseEventsFromEmail(id, subject, cleanBody, dateTimestamp)

        return EmailMessage(
            id = id,
            subject = subject,
            sender = sender,
            dateTimestamp = dateTimestamp,
            bodyText = cleanBody,
            extractedEvents = extracted,
            isRead = false,
            hasAttachments = false
        )
    }

    /**
     * RFC 2822 头部解析（支持折行 unfolding，即以空格或制表符开头的续行）
     */
    private fun parseHeaders(lines: List<String>): Map<String, String> {
        val result = mutableMapOf<String, StringBuilder>()
        var currentKey: String? = null

        for (line in lines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && currentKey != null) {
                result[currentKey]?.append(" ")?.append(line.trim())
            } else {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    currentKey = key
                    result.getOrPut(key) { StringBuilder() }.let {
                        if (it.isNotEmpty()) it.append(" ")
                        it.append(value)
                    }
                } else {
                    currentKey = null
                }
            }
        }
        return result.mapValues { it.value.toString().trim() }
    }

    /**
     * 智能提取邮件正文（支持 Content-Transfer-Encoding base64/quoted-printable 解码、MIME multipart 解析与 HTML 标签过滤）
     */
    private fun extractReadableBody(rawBody: String, headers: Map<String, String> = emptyMap()): String {
        if (rawBody.isBlank()) return "无正文内容"

        val encoding = headers["content-transfer-encoding"] ?: ""
        val contentType = headers["content-type"] ?: ""
        val charset = extractCharset(contentType)

        // 顶层 Base64 / Quoted-Printable 解码处理
        var text = rawBody
        if (encoding.contains("base64", ignoreCase = true) || isLikelyBase64(rawBody)) {
            try {
                val cleanB64 = rawBody.replace("\r", "").replace("\n", "").trim()
                val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
                text = String(bytes, charset)
            } catch (_: Exception) {}
        } else if (encoding.contains("quoted-printable", ignoreCase = true)) {
            try {
                text = decodeQuotedPrintable(rawBody, charset.name())
            } catch (_: Exception) {}
        }

        // 处理带 boundary 的多部分 MIME 邮件
        if (text.contains("Content-Type:", ignoreCase = true) && text.contains("--")) {
            // 优先提取纯文本 text/plain
            val textPlain = extractMimePart(text, "text/plain")
            if (!textPlain.isNullOrBlank()) {
                val cleaned = cleanEmailText(textPlain)
                if (cleaned.isNotBlank()) return cleaned
            }
            // 备选提取 text/html 并清洗
            val textHtml = extractMimePart(text, "text/html")
            if (!textHtml.isNullOrBlank()) {
                val cleaned = cleanHtmlEmail(textHtml)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // 非多部分或纯 HTML/文本邮件
        return if (text.contains("<html", ignoreCase = true) ||
            text.contains("<body", ignoreCase = true) ||
            text.contains("<div", ignoreCase = true) ||
            text.contains("<p", ignoreCase = true) ||
            text.contains("<br", ignoreCase = true) ||
            text.contains("<style", ignoreCase = true) ||
            text.contains("<table", ignoreCase = true)
        ) {
            cleanHtmlEmail(text)
        } else {
            cleanEmailText(text)
        }
    }

    private fun isLikelyBase64(raw: String): Boolean {
        val clean = raw.replace("\r", "").replace("\n", "").trim()
        if (clean.length < 32 || clean.length % 4 != 0) return false
        return clean.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' }
    }

    private fun extractMimePart(body: String, mimeType: String): String? {
        val lower = body.lowercase()
        val typeIdx = lower.indexOf("content-type: $mimeType")
        if (typeIdx == -1) return null

        val afterType = body.substring(typeIdx)
        val headerEndIdx = afterType.indexOf("\n\n").let {
            if (it == -1) afterType.indexOf("\r\n\r\n") else it
        }
        if (headerEndIdx == -1) return null

        val partHeaders = afterType.substring(0, headerEndIdx)
        val partContentWithRest = afterType.substring(headerEndIdx).trimStart('\r', '\n')

        val nextBoundaryIdx = partContentWithRest.indexOf("\n--").let {
            if (it == -1) partContentWithRest.indexOf("\r\n--") else it
        }
        val partRawContent = if (nextBoundaryIdx != -1) {
            partContentWithRest.substring(0, nextBoundaryIdx).trim()
        } else {
            partContentWithRest.trim()
        }

        val isBase64 = partHeaders.contains("Content-Transfer-Encoding: base64", ignoreCase = true)
        val isQp = partHeaders.contains("Content-Transfer-Encoding: quoted-printable", ignoreCase = true)
        val charset = extractCharset(partHeaders)

        return try {
            if (isBase64) {
                val cleanB64 = partRawContent.replace("\r", "").replace("\n", "").trim()
                val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
                String(bytes, charset)
            } else if (isQp) {
                decodeQuotedPrintable(partRawContent, charset.name())
            } else {
                partRawContent
            }
        } catch (_: Exception) {
            partRawContent
        }
    }

    private fun extractCharset(headers: String): Charset {
        val match = Regex("""charset=["']?([^"';\r\n]+)["']?""", RegexOption.IGNORE_CASE).find(headers)
        val name = match?.groupValues?.getOrNull(1)?.trim() ?: "utf-8"
        return resolveCharset(name)
    }

    private fun cleanHtmlEmail(html: String): String {
        var text = html
        text = text.replace(Regex("""<!--[\s\S]*?-->"""), "")
        text = text.replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<head[\s\S]*?</head>""", RegexOption.IGNORE_CASE), "")

        text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("""<p[^>]*>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""</tr>""", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("""</li>""", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("""</div>""", RegexOption.IGNORE_CASE), "\n")

        text = text.replace(Regex("""<[^>]+>"""), "")

        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val result = lines.joinToString("\n").trim()
        return if (result.isBlank()) "无正文内容或正文为纯附件" else result
    }

    private fun cleanEmailText(raw: String): String {
        var text = raw
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val result = lines.joinToString("\n").trim()
        return if (result.isBlank()) "无正文内容" else result
    }

    /**
     * 发件人姓名规范化显示：
     * 例如："邮件助手" <10000@exmail.weixin.qq.com> -> 邮件助手 <10000@exmail.weixin.qq.com>
     */
    private fun cleanSenderDisplay(rawSender: String): String {
        val decoded = decodeMimeHeader(rawSender).trim()
        val match = Regex("""^(?:"?([^"<]+?)"?\s*)?<([^>]+)>$""").find(decoded)
        return if (match != null) {
            val name = match.groupValues[1].trim().removeSurrounding("\"")
            val email = match.groupValues[2].trim()
            if (name.isNotBlank()) {
                "$name <$email>"
            } else {
                email
            }
        } else {
            decoded.removeSurrounding("\"").trim()
        }
    }

    private fun buildAuthErrorMessage(rawResponse: String, config: MailConfig): String {
        return """
登录认证未通过：
$rawResponse

⚠️ 腾讯企业邮箱 (exmail.qq.com) 无法拉取邮件的常见原因与解决指引：
1. 账号格式：请确保填写完整的校园邮箱账号（例如: student@mail.bnbu.edu.cn）；
2. 客户端专用密码：腾讯企业邮（特别是绑定了微信或企业微信扫码登录的账号）默认禁止使用网页普通登录密码登录第三方客户端。请登录学校腾讯企业邮箱网页版 (https://exmail.qq.com)：
   👉 进入「设置」→「客户端设置」/「账户安全」
   👉 确认开启「POP3/IMAP 服务」
   👉 点击「生成客户端专用密码」或「授权码」，将生成的 16 位专用密码复制并填入本软件密码框；
3. 校园网络环境：若在校园内网之外，请确认服务器地址为 imap.exmail.qq.com (端口 993 SSL)；
4. 如多次失败，可尝试切换收信协议为 POP3 (pop.exmail.qq.com 端口 995 SSL)。
        """.trimIndent()
    }

    private fun formatErrorMessage(e: Exception, config: MailConfig): String {
        val msg = e.message ?: e.toString()
        return when {
            msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) ->
                "连接邮件服务器超时 (${config.incomingServer}:${config.incomingPort})。请检查手机网络连接或 DNS 设置。"
            msg.contains("Connection refused", ignoreCase = true) ->
                "无法连接到服务器 (${config.incomingServer}:${config.incomingPort})，连接被拒绝。请检查端口与 SSL 是否匹配。"
            msg.contains("UnknownHost", ignoreCase = true) ->
                "找不到邮件服务器域名 (${config.incomingServer})，请检查拼写是否正确。"
            else -> msg
        }
    }

    /**
     * RFC 2047 MIME 头部解码（支持 GB18030 / GBK / GB2312 / UTF-8 以及多段拼接）
     */
    private fun decodeMimeHeader(raw: String): String {
        if (!raw.contains("=?")) return raw.trim()
        // RFC 2047: 相邻 encoded-word 之间的空白字符必须忽略
        val normalized = raw.replace(Regex("""\?=\s+=\?"""), "?==?")
        val regex = Regex("""=\?([^?]+)\?([bBqQ])\?([^?]+)\?=""")
        var result = normalized
        val matches = regex.findAll(normalized)
        for (match in matches) {
            try {
                val rawCharset = match.groupValues[1]
                val encoding = match.groupValues[2].uppercase()
                val encodedText = match.groupValues[3]
                val charset = resolveCharset(rawCharset)
                val decoded = if (encoding == "B") {
                    val bytes = Base64.decode(encodedText, Base64.DEFAULT)
                    String(bytes, charset)
                } else if (encoding == "Q") {
                    decodeQuotedPrintable(encodedText, charset.name())
                } else {
                    encodedText
                }
                result = result.replace(match.value, decoded)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode MIME word: ${match.value}", e)
            }
        }
        return result.trim()
    }

    private fun resolveCharset(charsetName: String): Charset {
        return try {
            when (charsetName.lowercase().replace("-", "").replace("_", "")) {
                "gb2312", "gbk", "gb18030", "chinese", "isoir58" -> Charset.forName("GB18030")
                "utf8" -> StandardCharsets.UTF_8
                "gb231280" -> Charset.forName("GB18030")
                else -> Charset.forName(charsetName)
            }
        } catch (_: Exception) {
            StandardCharsets.UTF_8
        }
    }

    private fun decodeQuotedPrintable(input: String, charsetName: String): String {
        val baos = ByteArrayOutputStream()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '=') {
                if (i + 2 < input.length) {
                    val hex = input.substring(i + 1, i + 3)
                    val byteVal = hex.toIntOrNull(16)
                    if (byteVal != null) {
                        baos.write(byteVal)
                        i += 3
                        continue
                    }
                }
                i++
            } else if (c == '_') {
                baos.write(' '.code)
                i++
            } else {
                baos.write(c.code)
                i++
            }
        }
        return try {
            val charset = resolveCharset(charsetName)
            String(baos.toByteArray(), charset)
        } catch (_: Exception) {
            String(baos.toByteArray(), StandardCharsets.UTF_8)
        }
    }

    private fun parseEmailDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss z",
            "d MMM yyyy HH:mm:ss Z",
            "d MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                val date = sdf.parse(dateStr.trim())
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
