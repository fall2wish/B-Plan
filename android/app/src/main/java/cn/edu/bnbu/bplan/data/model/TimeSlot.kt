package cn.edu.bnbu.bplan.data.model

data class TimeSlot(
    val period: Int,
    val startTime: String, // e.g. "08:00"
    val endTime: String    // e.g. "08:50"
)
