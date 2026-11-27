package com.xywy.signalmap.ui.cell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xywy.signalmap.domain.cell.LiveNetworkEvents
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestNetworkEvent
import com.xywy.signalmap.ui.components.CountBadge
import com.xywy.signalmap.ui.components.DetailItem
import com.xywy.signalmap.ui.components.EmptyHint
import com.xywy.signalmap.ui.components.InfoBanner
import com.xywy.signalmap.ui.components.InfoTone
import com.xywy.signalmap.ui.components.SectionHeader
import com.xywy.signalmap.ui.components.StatusDot
import com.xywy.signalmap.ui.components.TagChip
import com.xywy.signalmap.ui.components.XyCard
import com.xywy.signalmap.ui.theme.SignalColors
import com.xywy.signalmap.ui.theme.xywy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun diagnosticTime(time: Long): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(Date(time))

@Composable
private fun statusColor(tone: NetworkStatusTone): Color = when (tone) {
    NetworkStatusTone.SUCCESS -> MaterialTheme.xywy.success
    NetworkStatusTone.WARNING -> MaterialTheme.xywy.warning
    NetworkStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun SampleDiagnostics(sample: TestLogSample, showMatch: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sample.airplaneModeOn == true) {
            InfoBanner(message = "飞行模式已开启", tone = InfoTone.WARNING)
        }
        sample.diagnosticMessage?.let {
            InfoBanner(message = it, tone = InfoTone.ERROR)
        }
        if (sample.sims.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder),
            ) {
                Text(
                    "本次采样没有可用的 SIM 小区信息，不代表已确认无覆盖",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        sample.sims.forEach { sim ->
            val cell = sim.servingCell
            val tech = cell?.radioTechnology ?: RadioTechnology.UNKNOWN
            val techColor = SignalColors.forTechnology(tech)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, techColor.copy(alpha = 0.25f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(color = techColor, size = 8.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                sim.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        TagChip(
                            text = cell?.radioTechnology?.label ?: "无小区测量",
                            tint = techColor,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                Text(
                                    "注册状态",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    sim.serviceState ?: "未上报",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor(serviceStateTone(sim.serviceState)),
                                )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                Text(
                                    "数据连接",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    sim.dataConnectionState ?: "未上报",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor(dataConnectionTone(sim.dataConnectionState)),
                                )
                                }
                            }
                        }
                    }

                    if (cell != null) {
                        val quality = when {
                            cell.isStale -> "测量已过期"
                            cell.measurementAgeMillis == null -> "时效未知"
                            cell.rsrp == null && cell.sinr == null -> "指标未上报"
                            else -> "测量有效"
                        }
                        val qualityColor = when {
                            cell.isStale -> MaterialTheme.colorScheme.error
                            cell.measurementAgeMillis == null -> MaterialTheme.colorScheme.outline
                            cell.rsrp == null && cell.sinr == null -> MaterialTheme.xywy.warning
                            else -> SignalColors.Excellent
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(color = qualityColor, size = 6.dp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    quality,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qualityColor,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "测量年龄：${cell.measurementAgeMillis?.let { "${it / 1000} 秒" } ?: "未知"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "来源：${cell.measurementSource ?: "旧记录未保存"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DetailItem(
                                modifier = Modifier.weight(1f),
                                label = "ECI/NCI",
                                value = cell.cellId ?: "--",
                            )
                            DetailItem(
                                modifier = Modifier.weight(1f),
                                label = "PCI",
                                value = cell.pci ?: "--",
                            )
                            DetailItem(
                                modifier = Modifier.weight(1f),
                                label = "频点",
                                value = cell.channelNumber ?: "--",
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DetailItem(
                                modifier = Modifier.weight(1f),
                                label = "RSRP",
                                value = if (cell.isStale) "已过期" else cell.rsrp?.let { "$it dBm" } ?: "未上报",
                                valueColor = cell.rsrp?.takeUnless { cell.isStale }?.let {
                                    when {
                                        it >= -95 -> SignalColors.Excellent
                                        it >= -105 -> SignalColors.Fair
                                        else -> SignalColors.Bad
                                    }
                                } ?: Color.Unspecified,
                            )
                            DetailItem(
                                modifier = Modifier.weight(1f),
                                label = "SINR",
                                value = if (cell.isStale) "已过期" else cell.sinr?.let { "$it dB" } ?: "未上报",
                                valueColor = cell.sinr?.takeUnless { cell.isStale }?.let {
                                    if (it >= 5) SignalColors.Excellent else SignalColors.Fair
                                } ?: Color.Unspecified,
                            )
                            if (showMatch) {
                                DetailItem(
                                    modifier = Modifier.weight(1f),
                                    label = "工参",
                                    value = cell.matchedSectorName ?: cell.matchedSiteName ?: "未匹配",
                                    valueColor = if (cell.matchedSectorName != null || cell.matchedSiteName != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                )
                            }
                        }
                    }

                    sim.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    sim.matchDiagnostic?.takeIf { showMatch && it.method != "未知" }?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    "${it.method} · 候选 ${it.candidateCount} 条",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    it.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NetworkEventText(
    event: TestNetworkEvent,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val evidenceColor = when (event.evidence) {
        "系统状态" -> MaterialTheme.colorScheme.primary
        "参数变化推断" -> MaterialTheme.xywy.info
        else -> MaterialTheme.colorScheme.secondary
    }

    val eventContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        diagnosticTime(event.timestampMillis),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TagChip(
                        text = event.slotIndex?.let { "SIM${it + 1}" } ?: "全局",
                        tint = if (event.slotIndex != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                }
                TagChip(
                    text = event.evidence,
                    tint = evidenceColor,
                )
            }
            Text(
                event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                event.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
    val border = BorderStroke(
        if (selected) 1.5.dp else 1.dp,
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.xywy.cardBorder.copy(alpha = 0.7f),
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            border = border,
            content = eventContent,
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            border = border,
            content = eventContent,
        )
    }
}

@Composable
fun NetworkDiagnosticsPanel(state: LiveNetworkEvents) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }

    XyCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "实时采样诊断",
                icon = Icons.Outlined.Analytics,
                accent = MaterialTheme.colorScheme.primary,
                trailing = {
                    Surface(
                        onClick = { showDetails = !showDetails },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (showDetails) "收起诊断" else "查看来源与状态",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                imageVector = if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )

            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.previous?.let {
                        SampleDiagnostics(it, showMatch = false)
                    } ?: Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    ) {
                        Text(
                            "等待首次采样...",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "系统接口约每秒观察一次，短于采样间隔的事件可能无法捕获。开始测试后诊断随日志保存。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.xywy.cardBorder.copy(alpha = 0.6f),
                thickness = 0.8.dp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "网络事件",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CountBadge(count = state.events.size, color = MaterialTheme.colorScheme.primary)
                }
                if (state.events.size > 5) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(
                            if (expanded) "收起事件" else "查看全部 ${state.events.size} 条",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (state.events.isEmpty()) {
                EmptyHint(
                    message = "尚未观测到网络状态变化",
                    icon = Icons.Outlined.Info,
                )
            } else {
                val displayEvents = state.events.asReversed().take(if (expanded) 100 else 5)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayEvents.forEach { event ->
                        NetworkEventText(event = event)
                    }
                }
            }
        }
    }
}
