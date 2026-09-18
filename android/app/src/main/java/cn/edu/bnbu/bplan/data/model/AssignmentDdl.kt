package cn.edu.bnbu.bplan.data.model

data class SubmittedFile(
    val fileName: String,
    val fileSize: String,
    val fileUrl: String? = null,
    val uploadTimestamp: Long = System.currentTimeMillis()
)

data class AssignmentDdl(
    val id: String,
    val courseName: String,
    val title: String,
    val dueTimestamp: Long, // Epoch millis
    val isSubmitted: Boolean = false,
    val url: String? = null,
    val description: String? = null,
    val submittedAt: Long? = null,
    val submissionStatusText: String? = null, // e.g. "已提交以供评分 (Submitted for grading)"
    val gradingStatusText: String? = null,    // e.g. "尚未评分 (Not graded)"
    val submittedFiles: List<SubmittedFile> = emptyList(),
    val isDueModified: Boolean = false // Protected from being overwritten by stale sync data
) {
    val remainingHours: Float
        get() {
            val delta = dueTimestamp - System.currentTimeMillis()
            return (delta / (1000f * 3600f)).coerceAtLeast(0f)
        }

    val isExpired: Boolean
        get() = System.currentTimeMillis() > dueTimestamp

    val isUrgent: Boolean
        get() = !isSubmitted && !isExpired && remainingHours <= 24f
}

