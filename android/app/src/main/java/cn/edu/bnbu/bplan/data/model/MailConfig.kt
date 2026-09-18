package cn.edu.bnbu.bplan.data.model

data class MailConfig(
    val protocol: String = "IMAP", // "IMAP" or "POP3"
    val emailAddress: String = "",
    val password: String = "",
    val incomingServer: String = "imap.exmail.qq.com",
    val incomingPort: Int = 993,
    val useIncomingSsl: Boolean = true,
    val outgoingServer: String = "smtp.exmail.qq.com",
    val outgoingPort: Int = 465,
    val useOutgoingSsl: Boolean = true,
    val isAutoSyncEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncTime: Long = 0L
)
