package cn.edu.bnbu.bplan.data.model

data class EmailMessage(
    val id: String,
    val subject: String,
    val sender: String,
    val dateTimestamp: Long,
    val bodyText: String,
    val hasAttachments: Boolean = false,
    val isRead: Boolean = false,
    val extractedEvents: List<ScheduleEvent> = emptyList()
)
