package cn.edu.bnbu.bplan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.outlined.Circle

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.data.model.AssignmentDdl
import cn.edu.bnbu.bplan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DdlCard(
    ddl: AssignmentDdl,
    onToggleSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSubmission: ((AssignmentDdl) -> Unit)? = null,
    onAddToCalendar: ((AssignmentDdl) -> Unit)? = null,
    onDeleteWithConfirm: ((AssignmentDdl) -> Unit)? = null
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
    val dueDateStr = sdf.format(Date(ddl.dueTimestamp))

    val cardContent = @Composable {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ddl.isSubmitted) Color(0xFFF8FAFC) else SurfaceLight
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (ddl.isSubmitted) 0.dp else 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (ddl.isSubmitted && onViewSubmission != null) {
                        onViewSubmission(ddl)
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox Button
                    IconButton(
                        onClick = onToggleSubmitted,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (ddl.isSubmitted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "已完成",
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = "未完成",
                                tint = if (ddl.isUrgent) UrgentRed else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Course tag + countdown pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = ddl.courseName,
                                color = BnbuBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // Countdown Badge
                            CountdownBadge(ddl)
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Title
                        Text(
                            text = ddl.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (ddl.isSubmitted) TextMuted else TextPrimary,
                            textDecoration = if (ddl.isSubmitted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Due date & modified indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "截止时间: $dueDateStr",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                            if (ddl.isDueModified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "已更新",
                                        color = BnbuBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick action icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onAddToCalendar != null) {
                            IconButton(
                                onClick = { onAddToCalendar(ddl) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "加入系统日历",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (!ddl.url.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ddl.url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "在浏览器中打开",
                                    tint = BnbuNavy,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Submitted details button if submitted
                if (ddl.isSubmitted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4), // Very light emerald
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewSubmission?.invoke(ddl) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val fileCount = ddl.submittedFiles.size
                                val fileHint = if (fileCount > 0) "已提交 $fileCount 个附件文件" else "已完成在线提交"
                                Text(
                                    text = fileHint,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "查看详情 >",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (onAddToCalendar != null && onDeleteWithConfirm != null) {
        BidirectionalSwipeBox(
            onSwipeRightCalendar = { onAddToCalendar(ddl) },
            onSwipeLeftDelete = { onDeleteWithConfirm(ddl) },
            modifier = modifier.padding(vertical = 4.dp)
        ) {
            cardContent()
        }
    } else {
        Box(modifier = modifier.padding(vertical = 4.dp)) {
            cardContent()
        }
    }
}


@Composable
private fun CountdownBadge(ddl: AssignmentDdl) {
    val (bgColor, textColor, text) = when {
        ddl.isSubmitted -> Triple(SuccessBg, SuccessGreen, "已提交")
        ddl.isExpired -> Triple(Color(0xFFF1F5F9), TextMuted, "已截止")
        ddl.isUrgent -> {
            val hours = ddl.remainingHours.toInt()
            Triple(UrgentBg, UrgentRed, "仅剩 ${hours}h 截止")
        }
        ddl.remainingHours <= 72f -> {
            val days = (ddl.remainingHours / 24f).toInt() + 1
            Triple(WarningBg, WarningAmber, "剩 ${days} 天")
        }
        else -> {
            val days = (ddl.remainingHours / 24f).toInt()
            Triple(Color(0xFFE0F2FE), BnbuBlue, "剩 ${days} 天")
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
