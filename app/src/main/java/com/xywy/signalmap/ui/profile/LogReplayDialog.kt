package com.xywy.signalmap.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xywy.signalmap.domain.cell.LogReplayState
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.ui.cell.NetworkEventText
import com.xywy.signalmap.ui.cell.SampleDiagnostics
import com.xywy.signalmap.ui.cell.diagnosticTime
import com.xywy.signalmap.ui.components.DetailItem
import com.xywy.signalmap.ui.components.EmptyHint
import com.xywy.signalmap.ui.components.IconTile
import com.xywy.signalmap.ui.components.SectionHeader
import com.xywy.signalmap.ui.components.StatusDot
import com.xywy.signalmap.ui.components.TagChip
import com.xywy.signalmap.ui.components.XyCard
import com.xywy.signalmap.ui.theme.SignalColors
import com.xywy.signalmap.ui.theme.xywy
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LogReplayDialog(
    state: LogReplayState,
    onClose: () -> Unit,
    onSeek: (Int) -> Unit,
    onPlay: () -> Unit,
    onLocate: () -> Unit,
) {
    val count = state.document.samples.size
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(
                            icon = Icons.Filled.Timeline,
                            tint = MaterialTheme.colorScheme.primary,
                            tileSize = 38.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "记录回放与分析",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                state.document.meta.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Keep playback and analysis in one scroll container, including in landscape.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        // 播放进度卡片
                        XyCard(contentPadding = 14.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TagChip(
                                        text = if (count == 0) "无采样点" else "${state.index + 1} / $count",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    state.sample?.let {
                                        Text(
                                            diagnosticTime(it.timestampMillis),
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Slider(
                                    value = state.index.toFloat(),
                                    onValueChange = { onSeek(it.roundToInt()) },
                                    valueRange = 0f..(count - 1).coerceAtLeast(1).toFloat(),
                                    enabled = count > 1,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = { onSeek(state.index - 1) },
                                        enabled = state.index > 0,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    ) {
                                        Icon(Icons.Filled.SkipPrevious, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("上一个", maxLines = 1)
                                    }
                                    Button(
                                        onClick = onPlay,
                                        enabled = count > 0,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    ) {
                                        Icon(
                                            if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (state.playing) "暂停" else "播放", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onSeek(state.index + 1) },
                                        enabled = state.index + 1 < count,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    ) {
                                        Text("下一个", maxLines = 1)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                // 详情列表
                    state.sample?.let { sample ->
                        item {
                            XyCard(contentPadding = 14.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SectionHeader(
                                        title = "当前采样详情",
                                        icon = Icons.Filled.History,
                                        accent = MaterialTheme.colorScheme.primary,
                                    )
                                    SampleDiagnostics(sample)
                                    sample.point?.let { point ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "经纬度：${point.longitude}, ${point.latitude}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    Text(
                                                        "定位精度：${point.accuracyMeters ?: "未知"} 米",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                FilledTonalButton(
                                                    onClick = onLocate,
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                ) {
                                                    Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("地图定位", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } ?: Text(
                                        "此采样无定位，不能定位到地图",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "全程统计",
                            icon = Icons.Filled.Timeline,
                            accent = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.5f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DetailItem(modifier = Modifier.weight(1f), label = "总采样点", value = "$count")
                                    DetailItem(modifier = Modifier.weight(1f), label = "有效定位", value = "${state.analysis.locatedSamples}")
                                    DetailItem(modifier = Modifier.weight(1f), label = "采样不连续", value = "${state.analysis.gaps} 处")
                                }
                                Text(
                                    "没有 SIM 测量信息的采样：${state.analysis.samplesWithoutSimInfo} 个（不计入任何制式的有效指标）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "弱覆盖阈值 RSRP < -110 dBm，低 SINR < 0 dB；比例仅以相应有效指标为分母。缺测、过期不算无覆盖，采样占比不等于里程或时间占比。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    items(state.analysis.statistics) { stat ->
                        val techColor = SignalColors.forTechnology(stat.technology)
                        XyCard(contentPadding = 12.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusDot(color = techColor, size = 7.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "SIM${stat.slotIndex + 1} / 订阅 ${stat.subscriptionId ?: "未知"}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    TagChip(text = stat.technology.label, tint = techColor)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DetailItem(modifier = Modifier.weight(1f), label = "总采样", value = "${stat.total}")
                                    DetailItem(modifier = Modifier.weight(1f), label = "过期数", value = "${stat.stale}")
                                    DetailItem(modifier = Modifier.weight(1f), label = "RSRP 均值", value = "${number(stat.averageRsrp)} dBm")
                                    DetailItem(modifier = Modifier.weight(1f), label = "SINR 均值", value = "${number(stat.averageSinr)} dB")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DetailItem(
                                        modifier = Modifier.weight(1f),
                                        label = "弱覆盖比例",
                                        value = ratio(stat.weakCount, stat.rsrpCount),
                                        valueColor = if (stat.weakCount > 0) MaterialTheme.colorScheme.error else SignalColors.Excellent,
                                    )
                                    DetailItem(
                                        modifier = Modifier.weight(1f),
                                        label = "低 SINR 比例",
                                        value = ratio(stat.lowSinrCount, stat.sinrCount),
                                        valueColor = if (stat.lowSinrCount > 0) MaterialTheme.xywy.warning else SignalColors.Excellent,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "事件时间线",
                            subtitle = "点击事件可直接跳转至对应采样点",
                            icon = Icons.Filled.Timeline,
                            accent = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (state.analysis.events.isEmpty()) {
                        item {
                            EmptyHint(message = "未发现可判断的网络事件")
                        }
                    }
                    items(state.analysis.events) { indexed ->
                        NetworkEventText(
                            event = indexed.event,
                            onClick = { onSeek(indexed.sampleIndex) },
                            selected = indexed.sampleIndex == state.index,
                        )
                    }
                }
            }
        }
    }
}

private fun number(value: Double?): String = value?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "--"
private fun ratio(numerator: Int, denominator: Int): String = if (denominator == 0) "--（无有效指标）"
    else "$numerator/$denominator（${number(numerator * 100.0 / denominator)}%）"

@Composable
fun ReplayMapControls(
    state: LogReplayState,
    onSeek: (Int) -> Unit,
    onPlay: () -> Unit,
    onAnalysis: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.xywy.glass,
        border = BorderStroke(1.dp, MaterialTheme.xywy.glassBorder),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(
                        icon = Icons.Filled.History,
                        tint = MaterialTheme.colorScheme.primary,
                        tileSize = 28.dp,
                        iconSize = 15.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "历史回放",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TagChip(
                        text = if (state.document.samples.isEmpty()) "无采样点" else "${state.index + 1}/${state.document.samples.size}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    state.sample?.let { diagnosticTime(it.timestampMillis) } ?: "--",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.sample?.sims?.forEach { sim ->
                val tech = sim.servingCell?.radioTechnology ?: RadioTechnology.UNKNOWN
                val techColor = SignalColors.forTechnology(tech)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = techColor.copy(alpha = 0.08f),
                    border = BorderStroke(0.8.dp, techColor.copy(alpha = 0.2f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "SIM${sim.slotIndex + 1} ${sim.servingCell?.radioTechnology?.label ?: "未知"} · ${sim.servingCell?.cellId ?: "无小区"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = techColor,
                        )
                        Text(
                            if (sim.servingCell?.isStale == true) "测量已过期"
                            else "RSRP ${sim.servingCell?.rsrp ?: "--"} · SINR ${sim.servingCell?.sinr ?: "--"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (state.sample?.point == null) {
                Text(
                    "此采样无定位，地图保留上一个位置",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Slider(
                value = state.index.toFloat(),
                onValueChange = { onSeek(it.roundToInt()) },
                valueRange = 0f..state.document.samples.lastIndex.coerceAtLeast(1).toFloat(),
                enabled = state.document.samples.size > 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPlay,
                    enabled = state.document.samples.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.playing) "暂停" else "播放", fontSize = 13.sp)
                }
                FilledTonalButton(
                    onClick = onAnalysis,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("事件 / 统计", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("结束回放", fontSize = 13.sp)
                }
            }
        }
    }
}
