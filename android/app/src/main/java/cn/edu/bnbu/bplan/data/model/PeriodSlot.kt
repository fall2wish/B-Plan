package cn.edu.bnbu.bplan.data.model

import java.util.UUID

data class PeriodSlot(
    val id: String = UUID.randomUUID().toString(),
    val periodNumber: Int,         // 序号: 1, 2, 3...
    val label: String,             // 标签名称: "第 1 节", "第 1 大节", "TA 辅导"
    val startTime: String,         // "09:00"
    val endTime: String,           // "11:50"
    val isCustom: Boolean = false
) {
    /**
     * 计算开始时间相对于午夜的分钟数 (0~1440)
     */
    val startMinutes: Int
        get() {
            val parts = startTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }

    /**
     * 计算结束时间相对于午夜的分钟数 (0~1440)
     */
    val endMinutes: Int
        get() {
            val parts = endTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 10
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }

    /**
     * 该节课程持续时长 (分钟)
     */
    val durationMinutes: Int
        get() = (endMinutes - startMinutes).coerceAtLeast(10)
}
