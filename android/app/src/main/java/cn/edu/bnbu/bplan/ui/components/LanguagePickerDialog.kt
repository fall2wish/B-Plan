package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.ui.theme.*
import cn.edu.bnbu.bplan.util.AppLanguage

@Composable
fun LanguagePickerDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    var selected by remember { mutableStateOf(currentLanguage) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceLight,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BnbuNavy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = BnbuNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "语言设置 / Language",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy
                        )
                        Text(
                            text = "选择显示语言 / Select Display Language",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val options = listOf(
                    AppLanguage.SIMPLIFIED_CHINESE,
                    AppLanguage.TRADITIONAL_CHINESE_HK,
                    AppLanguage.ENGLISH
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { lang ->
                        val isSelected = (selected == lang)
                        val borderColor = if (isSelected) BnbuBlue else DividerColor
                        val bgColor = if (isSelected) BnbuBlue.copy(alpha = 0.08f) else SurfaceVariantLight.copy(alpha = 0.35f)

                        val (primaryLabel, subLabel) = when (lang) {
                            AppLanguage.SIMPLIFIED_CHINESE -> Pair("简体中文 / Simplified Chinese", "中文（中国大陆）")
                            AppLanguage.TRADITIONAL_CHINESE_HK -> Pair("繁體中文 / Traditional Chinese", "中文（香港特別行政區）")
                            AppLanguage.ENGLISH -> Pair("English / 英语", "English (International)")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { selected = lang }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = primaryLabel,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BnbuNavy else TextPrimary
                                )
                                Text(
                                    text = subLabel,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(BnbuBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("取消 / Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onSelectLanguage(selected)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy)
                    ) {
                        Text("确认 / Confirm", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
