package cn.edu.bnbu.bplan.data.model

data class ScheduleEvent(
    val id: String,
    val title: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val location: String? = null,
    val notes: String? = null,
    val source: String = "manual", // "manual", "email", "ispace"
    val color: String = "#8B5CF6"
)
