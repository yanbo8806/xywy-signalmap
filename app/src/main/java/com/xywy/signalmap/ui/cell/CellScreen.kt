package com.xywy.signalmap.ui.cell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xywy.signalmap.MainUiState
import com.xywy.signalmap.domain.cell.CellDisplayDetails
import com.xywy.signalmap.domain.cell.CellDisplayFormatter
import com.xywy.signalmap.domain.cell.SignalHistoryRecord
import com.xywy.signalmap.domain.cell.SimCellMatcher
import com.xywy.signalmap.domain.cell.SimSignalHistory
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.ui.CellNeighborText
import com.xywy.signalmap.ui.components.DetailItem
import com.xywy.signalmap.ui.components.EmptyHint
import com.xywy.signalmap.ui.components.SectionHeader
import com.xywy.signalmap.ui.components.SignalBars
import com.xywy.signalmap.ui.components.StatTile
import com.xywy.signalmap.ui.components.StatusDot
import com.xywy.signalmap.ui.components.TagChip
import com.xywy.signalmap.ui.components.XyCard
import com.xywy.signalmap.ui.theme.SignalColors
import com.xywy.signalmap.ui.theme.xywy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CellScreen(
    state: MainUiState,
    paddingValues: PaddingValues,
    onRequestPermissions: () -> Unit,
    diagnostics: com.xywy.signalmap.domain.cell.LiveNetworkEvents = com.xywy.signalmap.domain.cell.LiveNetworkEvents(),
) {
    if (!state.locationPermissionGranted || !state.phonePermissionGranted) {
        // 权限未授权页面
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                "需要权限才能读取小区信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "请授予定位和电话权限",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(26.dp))
            Button(
                onClick = onRequestPermissions,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("立即授权", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CellsPageHeader() }
        item { NetworkDiagnosticsPanel(diagnostics) }

        item {
            SummaryCard(
                simCount = state.cellSnapshot.simCards.size,
                servingCount = state.simMatches.count { it.servingCell != null },
                neighborCount = state.simMatches.sumOf { it.neighbors.size },
                historyCount = state.signalHistories.sumOf { it.records.size },
            )
        }

        state.cellSnapshot.errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            item { MessageCard(message = message) }
        }
        state.cellSnapshot.simCards.forEach { sim ->
            sim.errorMessage?.takeIf(String::isNotBlank)?.let { message ->
                item { MessageCard(message = "${sim.label}：$message") }
            }
        }

        if (state.simMatches.isEmpty()) {
            item {
                MessageCard(message = "暂未读取到双卡小区信息，请确认手机已插卡、已联网，并且当前 ROM 允许读取小区信息。")
            }
        } else {
            items(
                items = state.simMatches,
                key = { "${it.subscriptionId}-${it.slotIndex}" },
            ) { match ->
                val history = state.signalHistories.firstOrNull {
                    it.subscriptionId == match.subscriptionId && it.slotIndex == match.slotIndex
                }
                SimSection(
                    match = match,
                    history = history,
                )
            }
        }

        // 底部留白
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/** 页面顶部品牌横幅：品牌渐变 + 标题 + 副标题。 */
@Composable
private fun CellsPageHeader() {
    val xywy = MaterialTheme.xywy
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(brush = xywy.heroBrush)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "占用小区",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = xywy.heroText,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "双卡实时状态 · 服务小区与邻区一览",
                    style = MaterialTheme.typography.bodySmall,
                    color = xywy.heroSubText,
                )
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = Color.White.copy(alpha = 0.16f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCell,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    simCount: Int,
    servingCount: Int,
    neighborCount: Int,
    historyCount: Int,
) {
    XyCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "占用小区概览",
                icon = Icons.Filled.NetworkCell,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "SIM 卡",
                    value = "$simCount",
                    icon = Icons.Filled.SimCard,
                    tint = MaterialTheme.colorScheme.primary,
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "服务小区",
                    value = "$servingCount",
                    icon = Icons.Filled.CellTower,
                    tint = SignalColors.NR,
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "邻区",
                    value = "$neighborCount",
                    icon = Icons.Filled.SignalCellularAlt,
                    tint = SignalColors.LTE,
                )
            }
        }
    }
}

@Composable
private fun SimSection(
    match: SimCellMatch,
    history: SimSignalHistory?,
) {
    val simTitle = match.label.ifBlank { "SIM ${match.slotIndex + 1}" }
    val servingTech = match.servingCell?.radioTechnology ?: RadioTechnology.UNKNOWN
    val techColor = SignalColors.forTechnology(servingTech)
    val servingName = displaySectorName(
        sector = match.servingSector,
        fallbackCellId = match.servingCell?.cellId,
    )

    XyCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 卡片头部：SIM 编号瓷贴 + 名称 + 制式徽标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = MaterialTheme.shapes.small,
                    color = techColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, techColor.copy(alpha = 0.3f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${match.slotIndex + 1}",
                            fontWeight = FontWeight.ExtraBold,
                            color = techColor,
                            fontSize = 17.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(simTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "卡槽 ${match.slotIndex + 1} · ${match.servingCell?.radioTechnology?.label ?: "无服务"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (match.servingCell != null) {
                    SignalBars(
                        rsrp = match.servingCell?.takeUnless { it.isStale }?.rsrp,
                        tint = techColor,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TagChip(
                        text = match.servingCell.radioTechnology.label,
                        tint = techColor,
                    )
                }
            }

            // 当前占用小区横条
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = techColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, techColor.copy(alpha = 0.22f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(color = techColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "占用",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = servingName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = techColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ServingCellCard(
                signal = match.servingCell,
                matchedSector = match.servingSector,
            )
            Text("工参诊断：${match.matchDiagnostic.method} · 候选 ${match.matchDiagnostic.candidateCount} 条\n${match.matchDiagnostic.message}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            RecentRecordsSection(
                history = history,
                currentTech = match.servingCell?.radioTechnology,
            )

            NeighborSection(
                neighbors = match.neighbors.take(8),
                neighborMatches = match.neighborMatches.take(8),
            )
        }
    }
}

@Composable
private fun ServingCellCard(
    signal: CellSignal?,
    matchedSector: SectorRecord?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            SectionHeader(
                title = "服务小区",
                icon = Icons.Filled.CellTower,
                accent = signal?.radioTechnology?.let(SignalColors::forTechnology)
                    ?: MaterialTheme.colorScheme.primary,
            )

            if (signal == null) {
                EmptyHint(message = "暂无服务小区数据", icon = Icons.Filled.CellTower)
                return@Column
            }
            if (signal.isStale) {
                com.xywy.signalmap.ui.components.InfoBanner(
                    message = "信号过期，等待新的有效测量",
                    tone = com.xywy.signalmap.ui.components.InfoTone.WARNING,
                )
            }

            val details = CellDisplayFormatter.from(signal)
            MatchedSectorBanner(
                title = "占用小区",
                primary = displaySectorName(
                    sector = matchedSector,
                    fallbackCellId = signal.cellId,
                ),
                secondary = matchedSector?.let { sector ->
                    listOfNotNull(
                        sector.siteName?.takeIf { it != sector.cellName },
                        sector.band?.takeIf(String::isNotBlank),
                    ).joinToString(" · ").takeIf(String::isNotBlank)
                },
            )

            DetailGridRow(
                listOf(
                    "网络" to signal.radioTechnology.label,
                    "MCC" to details.mcc,
                    "MNC" to details.mnc,
                    details.modeOrNetworkLabel to details.modeOrNetworkValue,
                ),
            )

            matchedSector?.let { sector ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "工参匹配",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        DetailGridRow(
                            listOf(
                                "小区" to (sector.cellName ?: sector.siteName ?: sector.id),
                                "频段" to sector.band,
                                "方向角" to sector.azimuth?.roundToInt()?.let { "$it°" },
                                "站点ID" to sector.siteId,
                            ),
                        )
                    }
                }
            }

            when (signal.radioTechnology) {
                RadioTechnology.NR -> NrServingGrid(details = details, matchedSector = matchedSector)
                RadioTechnology.LTE -> LteServingGrid(details = details, matchedSector = matchedSector)
                else -> GenericServingGrid(details = details)
            }

            MeasurementBoard(signal = signal)
        }
    }
}

@Composable
private fun NrServingGrid(
    details: CellDisplayDetails,
    matchedSector: SectorRecord?,
) {
    val bandwidthLabel = CellDisplayFormatter.resolveBandwidthLabel(details.bandwidthLabel, matchedSector?.bandwidth)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailGridRow(
            listOf(
                "基站ID" to details.baseStationId,
                "CellID" to details.localCellId,
                "NCI" to details.cellIdentityValue,
                details.tacLabel to details.tacValue,
            ),
        )
        DetailGridRow(
            listOf(
                "PCI" to details.pciValue,
                "Mod3" to details.pciMod3,
                details.channelLabel to details.channelValue,
                "GSCN" to details.gscn,
            ),
        )
        DetailGridRow(
            listOf(
                "频率" to details.frequencyLabel,
                "Band" to details.bandLabel,
                "带宽" to bandwidthLabel,
            ),
        )
    }
}

@Composable
private fun LteServingGrid(
    details: CellDisplayDetails,
    matchedSector: SectorRecord?,
) {
    val bandwidthLabel = CellDisplayFormatter.resolveBandwidthLabel(details.bandwidthLabel, matchedSector?.bandwidth)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailGridRow(
            listOf(
                "基站ID" to details.baseStationId,
                "CellID" to details.localCellId,
                "ECI" to details.cellIdentityValue,
                details.tacLabel to details.tacValue,
            ),
        )
        DetailGridRow(
            listOf(
                details.modeOrNetworkLabel to details.modeOrNetworkValue,
                "PCI" to details.pciValue,
                "Mod3" to details.pciMod3,
                "Band" to details.bandLabel,
            ),
        )
        DetailGridRow(
            listOf(
                details.channelLabel to details.channelValue,
                "频率" to details.frequencyLabel,
                "带宽" to bandwidthLabel,
            ),
        )
    }
}

@Composable
private fun GenericServingGrid(details: CellDisplayDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailGridRow(
            listOf(
                details.cellIdentityLabel to details.cellIdentityValue,
                details.tacLabel to details.tacValue,
                details.pciLabel to details.pciValue,
                details.channelLabel to details.channelValue,
            ),
        )
    }
}

@Composable
private fun MeasurementBoard(signal: CellSignal) {
    val metrics = when (signal.radioTechnology) {
        RadioTechnology.NR -> listOf(
            MetricData("SS-RSRP", signal.rsrp),
            MetricData("SS-RSRQ", signal.rsrq),
            MetricData("SS-SINR", signal.sinr),
        )
        RadioTechnology.LTE -> listOf(
            MetricData("RSRP", signal.rsrp),
            MetricData("RSRQ", signal.rsrq),
            MetricData("RSSI", signal.dbm),
            MetricData("SINR", signal.sinr),
        )
        else -> listOf(MetricData("dBm", signal.dbm))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        metrics.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { metric ->
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = metric.label,
                        value = metric.value.takeUnless { signal.isStale },
                    )
                }
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun RecentRecordsSection(
    history: SimSignalHistory?,
    currentTech: RadioTechnology?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SectionHeader(
                title = "最近信号记录",
                icon = Icons.Filled.SignalCellularAlt,
                accent = currentTech?.let(SignalColors::forTechnology)
                    ?: MaterialTheme.colorScheme.primary,
            )
            if (history == null || history.records.isEmpty()) {
                EmptyHint(message = "当前暂无历史记录", icon = Icons.Filled.SignalCellularAlt)
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                history.records.groupBy { it.servingCell.radioTechnology }.forEach { (displayTech, records) ->
                    Text(
                        text = displayTech.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = SignalColors.forTechnology(displayTech),
                    )
                when (displayTech) {
                        RadioTechnology.NR -> {
                            RecordHeaderRow(
                                columns = listOf(
                                    TableColumn("Time", 60.dp),
                                    TableColumn("PCI", 40.dp),
                                    TableColumn("Arfcn", 64.dp),
                                    TableColumn("SS-RSRP", 58.dp),
                                    TableColumn("SS-RSRQ", 58.dp),
                                    TableColumn("SS-SINR", 58.dp),
                                ),
                            )
                            records.forEach { record ->
                                NrRecordRow(record = record)
                            }
                        }
                        RadioTechnology.LTE -> {
                            RecordHeaderRow(
                                columns = listOf(
                                    TableColumn("Time", 60.dp),
                                    TableColumn("TAC", 48.dp),
                                    TableColumn("PCI", 40.dp),
                                    TableColumn("eNB", 48.dp),
                                    TableColumn("CellID", 48.dp),
                                    TableColumn("RSRP", 52.dp),
                                    TableColumn("RSRQ", 52.dp),
                                    TableColumn("SINR", 52.dp),
                                ),
                            )
                            records.forEach { record ->
                                LteRecordRow(record = record)
                            }
                        }
                        else -> {
                            RecordHeaderRow(
                                columns = listOf(
                                    TableColumn("Time", 60.dp),
                                    TableColumn("CellId", 72.dp),
                                    TableColumn("PCI", 40.dp),
                                    TableColumn("Freq", 56.dp),
                                    TableColumn("dBm", 52.dp),
                                ),
                            )
                            records.forEach { record ->
                                GenericRecordRow(record = record)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NrRecordRow(record: SignalHistoryRecord) {
    val details = CellDisplayFormatter.from(record.servingCell)
    RecordValueRow(
        columns = listOf(
            TableValue(formatHistoryTime(record.recordedAtMillis), 60.dp),
            TableValue(details.pciValue ?: "-", 40.dp),
            TableValue(details.channelValue ?: "-", 64.dp),
            TableValue(metricText(record.servingCell.rsrp), 58.dp, metricColor("RSRP", record.servingCell.rsrp)),
            TableValue(metricText(record.servingCell.rsrq), 58.dp, metricColor("RSRQ", record.servingCell.rsrq)),
            TableValue(metricText(record.servingCell.sinr), 58.dp, metricColor("SINR", record.servingCell.sinr)),
        ),
    )
}

@Composable
private fun LteRecordRow(record: SignalHistoryRecord) {
    val details = CellDisplayFormatter.from(record.servingCell)
    RecordValueRow(
        columns = listOf(
            TableValue(formatHistoryTime(record.recordedAtMillis), 60.dp),
            TableValue(details.tacValue ?: "-", 48.dp),
            TableValue(details.pciValue ?: "-", 40.dp),
            TableValue(details.baseStationId ?: "-", 48.dp),
            TableValue(details.localCellId ?: "-", 48.dp),
            TableValue(metricText(record.servingCell.rsrp), 52.dp, metricColor("RSRP", record.servingCell.rsrp)),
            TableValue(metricText(record.servingCell.rsrq), 52.dp, metricColor("RSRQ", record.servingCell.rsrq)),
            TableValue(metricText(record.servingCell.sinr), 52.dp, metricColor("SINR", record.servingCell.sinr)),
        ),
    )
}

@Composable
private fun GenericRecordRow(record: SignalHistoryRecord) {
    val details = CellDisplayFormatter.from(record.servingCell)
    RecordValueRow(
        columns = listOf(
            TableValue(formatHistoryTime(record.recordedAtMillis), 60.dp),
            TableValue(details.cellIdentityValue.ifBlank { "-" }, 72.dp),
            TableValue(details.pciValue ?: "-", 40.dp),
            TableValue(details.channelValue ?: "-", 56.dp),
            TableValue(metricText(record.servingCell.dbm), 52.dp, metricColor("dBm", record.servingCell.dbm)),
        ),
    )
}

@Composable
private fun NeighborSection(
    neighbors: List<CellSignal>,
    neighborMatches: List<SectorRecord?>,
) {
    val displayNeighbors = neighbors
        .zip(neighborMatches + List((neighbors.size - neighborMatches.size).coerceAtLeast(0)) { null })
        .filter { (signal, _) -> signal.radioTechnology == RadioTechnology.NR || signal.radioTechnology == RadioTechnology.LTE }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SectionHeader(
                title = CellNeighborText.sectionTitle,
                icon = Icons.Filled.SignalCellularAlt,
                accent = SignalColors.LTE,
            )
            if (displayNeighbors.isEmpty()) {
                EmptyHint(message = CellNeighborText.emptyMessage, icon = Icons.Filled.CellTower)
                return@Column
            }

            displayNeighbors
                .groupBy { it.first.radioTechnology }
                .toList()
                .sortedBy { neighborTechOrder(it.first) }
                .forEach { (technology, group) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(color = SignalColors.forTechnology(technology))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CellNeighborText.groupTitle(technology.label, group.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = SignalColors.forTechnology(technology),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { (signal, matchedSector) ->
                            NeighborCard(
                                signal = signal,
                                matchedSector = matchedSector,
                            )
                        }
                    }
                    if (technology != displayNeighbors.last().first.radioTechnology) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
        }
    }
}

@Composable
private fun MatchedSectorBanner(
    title: String,
    primary: String,
    secondary: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = primary.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            secondary?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NeighborCard(
    signal: CellSignal,
    matchedSector: SectorRecord?,
) {
    val clipboardManager = LocalClipboardManager.current
    var showDetailDialog by remember { mutableStateOf(false) }
    val details = CellDisplayFormatter.from(signal)
    val techColor = SignalColors.forTechnology(signal.radioTechnology)
    val name = displaySectorName(
        sector = matchedSector,
        fallbackCellId = signal.cellId ?: signal.pci,
    )
    val identitySummary = when (signal.radioTechnology) {
        RadioTechnology.NR -> listOfNotNull(
            "PCI ${details.pciValue ?: "-"}",
            details.channelValue?.let { "NRARFCN $it" },
            (details.bandLabel ?: matchedSector?.band)?.let { "Band $it" },
        )
        RadioTechnology.LTE -> listOfNotNull(
            "PCI ${details.pciValue ?: "-"}",
            details.channelValue?.let { "EARFCN $it" },
            (details.bandLabel ?: matchedSector?.band)?.let { "Band $it" },
        )
        else -> listOfNotNull(
            details.pciValue?.let { "PCI $it" },
            details.channelValue?.let { "Freq $it" },
        )
    }.joinToString(" · ")
    val detailText = buildString {
        appendLine(CellNeighborText.detailNameLine(name))
        if (identitySummary.isNotBlank()) appendLine(identitySummary)
        when (signal.radioTechnology) {
            RadioTechnology.NR -> {
                appendLine("PCI：${details.pciValue ?: "-"}")
                appendLine("GSCN：${details.gscn ?: "-"}")
                appendLine("TAC：${details.tacValue ?: matchedSector?.tacOrLac ?: "-"}")
                appendLine("SS-RSRP：${metricText(signal.rsrp)}")
                appendLine("SS-RSRQ：${metricText(signal.rsrq)}")
                appendLine("SS-SINR：${metricText(signal.sinr)}")
            }
            RadioTechnology.LTE -> {
                appendLine("TAC：${details.tacValue ?: matchedSector?.tacOrLac ?: "-"}")
                appendLine("PCI：${details.pciValue ?: "-"}")
                appendLine("Band：${details.bandLabel ?: matchedSector?.band ?: "-"}")
                appendLine("${CellNeighborText.frequencyLabel}：${details.channelValue ?: "-"}")
                appendLine("RSRP：${metricText(signal.rsrp)}")
                appendLine("RSRQ：${metricText(signal.rsrq)}")
                appendLine("SINR：${metricText(signal.sinr)}")
            }
            else -> Unit
        }
    }.trim()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDetailDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, techColor.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = techColor)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (identitySummary.isNotBlank()) {
                        Text(
                            text = identitySummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TagChip(
                    text = signal.radioTechnology.label,
                    tint = techColor,
                )
            }

            when (signal.radioTechnology) {
                RadioTechnology.NR -> {
                    DetailGridRow(
                        listOf(
                            "Beam" to extractBeam(matchedSector),
                            "PCI" to details.pciValue,
                            "Mod3" to details.pciMod3,
                            "GSCN" to details.gscn,
                        ),
                    )
                    DetailGridRow(
                        listOf(
                            "SS-RSRP" to metricText(signal.rsrp),
                            "SS-RSRQ" to metricText(signal.rsrq),
                            "SS-SINR" to metricText(signal.sinr),
                            "TAC" to (details.tacValue ?: matchedSector?.tacOrLac),
                        ),
                    )
                }
                RadioTechnology.LTE -> {
                    DetailGridRow(
                        listOf(
                            "TAC" to (details.tacValue ?: matchedSector?.tacOrLac),
                            "PCI" to details.pciValue,
                            "Band" to (details.bandLabel ?: matchedSector?.band),
                            CellNeighborText.frequencyLabel to details.channelValue,
                        ),
                    )
                    DetailGridRow(
                        listOf(
                            "RSRP" to metricText(signal.rsrp),
                            "RSRQ" to metricText(signal.rsrq),
                            "SINR" to metricText(signal.sinr),
                            "" to null,
                        ),
                    )
                }
                else -> Unit
            }

            Text(
                text = CellNeighborText.cardHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text(CellNeighborText.detailTitle) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(detailText))
                        showDetailDialog = false
                    },
                ) {
                    Text(CellNeighborText.copyAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false }) {
                    Text(CellNeighborText.closeAction)
                }
            },
        )
    }
}

@Composable
private fun NrNeighborTable(
    neighbors: List<CellSignal>,
    matchedSectors: List<SectorRecord>,
) {
    RecordHeaderRow(
        columns = listOf(
            TableColumn("Beam", 48.dp),
            TableColumn("Arfcn", 64.dp),
            TableColumn("PCI", 40.dp),
            TableColumn("Mod3", 44.dp),
            TableColumn("RSRP", 52.dp),
            TableColumn("RSRQ", 52.dp),
            TableColumn("SINR", 52.dp),
        ),
    )
    neighbors.forEach { signal ->
        val details = CellDisplayFormatter.from(signal)
        val matchedSector = findMatchedSector(signal, matchedSectors)
        RecordValueRow(
            columns = listOf(
                TableValue(extractBeam(matchedSector), 48.dp),
                TableValue(details.channelValue ?: "-", 64.dp),
                TableValue(details.pciValue ?: "-", 40.dp),
                TableValue(details.pciMod3 ?: "-", 44.dp),
                TableValue(metricText(signal.rsrp), 52.dp, metricColor("RSRP", signal.rsrp)),
                TableValue(metricText(signal.rsrq), 52.dp, metricColor("RSRQ", signal.rsrq)),
                TableValue(metricText(signal.sinr), 52.dp, metricColor("SINR", signal.sinr)),
            ),
        )
    }
}

@Composable
private fun LteNeighborTable(
    neighbors: List<CellSignal>,
    matchedSectors: List<SectorRecord>,
) {
    RecordHeaderRow(
        columns = listOf(
            TableColumn("TAC", 48.dp),
            TableColumn("PCI", 40.dp),
            TableColumn("EARFCN", 64.dp),
            TableColumn("Band", 52.dp),
            TableColumn("RSRP", 52.dp),
            TableColumn("RSRQ", 52.dp),
            TableColumn("SINR", 52.dp),
        ),
    )
    neighbors.forEach { signal ->
        val details = CellDisplayFormatter.from(signal)
        val matchedSector = findMatchedSector(signal, matchedSectors)
        RecordValueRow(
            columns = listOf(
                TableValue(details.tacValue ?: matchedSector?.tacOrLac ?: "-", 48.dp),
                TableValue(details.pciValue ?: "-", 40.dp),
                TableValue(details.channelValue ?: "-", 64.dp),
                TableValue(details.bandLabel ?: matchedSector?.band ?: "-", 52.dp),
                TableValue(metricText(signal.rsrp), 52.dp, metricColor("RSRP", signal.rsrp)),
                TableValue(metricText(signal.rsrq), 52.dp, metricColor("RSRQ", signal.rsrq)),
                TableValue(metricText(signal.sinr), 52.dp, metricColor("SINR", signal.sinr)),
            ),
        )
    }
}

@Composable
private fun GenericNeighborTable(
    neighbors: List<CellSignal>,
    matchedSectors: List<SectorRecord>,
) {
    RecordHeaderRow(
        columns = listOf(
            TableColumn("CellId", 72.dp),
            TableColumn("PCI", 40.dp),
            TableColumn("Freq", 56.dp),
            TableColumn("Band", 52.dp),
            TableColumn("dBm", 52.dp),
        ),
    )
    neighbors.forEach { signal ->
        val details = CellDisplayFormatter.from(signal)
        val matchedSector = findMatchedSector(signal, matchedSectors)
        RecordValueRow(
            columns = listOf(
                TableValue(details.cellIdentityValue.ifBlank { "-" }, 72.dp),
                TableValue(details.pciValue ?: "-", 40.dp),
                TableValue(details.channelValue ?: "-", 56.dp),
                TableValue(details.bandLabel ?: matchedSector?.band ?: "-", 52.dp),
                TableValue(metricText(signal.dbm), 52.dp, metricColor("dBm", signal.dbm)),
            ),
        )
    }
}

@Composable
private fun DetailGridRow(items: List<Pair<String, String?>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, value) ->
            DetailItem(
                modifier = Modifier.weight(1f),
                label = label,
                value = value,
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: Int?,
) {
    val progress = normalizeMetric(label = label, value = value)
    val color = metricColor(label = label, value = value)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                metricText(value),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = color,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun RecordHeaderRow(columns: List<TableColumn>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEach { column ->
            TableText(
                text = column.label,
                width = column.width,
                bold = true,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RecordValueRow(columns: List<TableValue>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEach { column ->
            TableText(
                text = column.text,
                width = column.width,
                color = column.color,
            )
        }
    }
}

@Composable
private fun TableText(
    text: String,
    width: Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
    )
}

@Composable
private fun MessageCard(message: String) {
    val xywy = MaterialTheme.xywy
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = xywy.warningContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, xywy.warning.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = xywy.warning.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = xywy.warning,
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private data class MetricData(
    val label: String,
    val value: Int?,
)

private data class TableColumn(
    val label: String,
    val width: Dp,
)

private data class TableValue(
    val text: String,
    val width: Dp,
    val color: Color = Color.Unspecified,
)

private fun metricText(value: Int?): String = value?.toString() ?: "-"

private fun formatHistoryTime(timeMillis: Long): String =
    HISTORY_TIME_FORMAT.format(Date(timeMillis))

private fun displaySectorName(
    sector: SectorRecord?,
    fallbackCellId: String?,
): String = sector?.cellName?.takeIf(String::isNotBlank)
    ?: sector?.siteName?.takeIf(String::isNotBlank)
    ?: fallbackCellId
    ?: "-"

private fun findMatchedSector(
    signal: CellSignal,
    matchedSectors: List<SectorRecord>,
): SectorRecord? = SimCellMatcher.findNeighborDisplayMatch(signal, matchedSectors)

private fun extractBeam(sector: SectorRecord?): String {
    if (sector == null) return "-"
    val beamField = sector.rawFields.entries.firstOrNull { (key, _) ->
        key.contains("beam", ignoreCase = true) || key.contains("波束")
    }?.value
    return beamField?.takeIf(String::isNotBlank)
        ?: sector.azimuth?.roundToInt()?.let { "Az$it" }
        ?: "-"
}

private fun neighborTechOrder(technology: RadioTechnology): Int = when (technology) {
    RadioTechnology.NR -> 0
    RadioTechnology.LTE -> 1
    RadioTechnology.WCDMA -> 2
    RadioTechnology.GSM -> 3
    RadioTechnology.CDMA -> 4
    RadioTechnology.UNKNOWN -> 5
}

private fun normalizeMetric(
    label: String,
    value: Int?,
): Float {
    if (value == null) return 0f
    return when {
        label.contains("RSRP", ignoreCase = true) -> ((value + 140f) / 80f).coerceIn(0f, 1f)
        label.contains("RSRQ", ignoreCase = true) -> ((value + 30f) / 30f).coerceIn(0f, 1f)
        label.contains("SINR", ignoreCase = true) -> ((value + 20f) / 50f).coerceIn(0f, 1f)
        label.contains("RSSI", ignoreCase = true) || label.contains("DBM", ignoreCase = true) -> ((value + 120f) / 80f).coerceIn(0f, 1f)
        else -> 0f
    }
}

private fun metricColor(
    label: String,
    value: Int?,
): Color {
    if (value == null) return Color.Gray
    return when {
        label.contains("RSRP", ignoreCase = true) -> when {
            value >= -95 -> SignalColors.Excellent
            value >= -110 -> SignalColors.Fair
            else -> SignalColors.Bad
        }
        label.contains("RSRQ", ignoreCase = true) -> when {
            value >= -10 -> SignalColors.Excellent
            value >= -15 -> SignalColors.Fair
            else -> SignalColors.Bad
        }
        label.contains("SINR", ignoreCase = true) -> when {
            value >= 10 -> SignalColors.Excellent
            value >= 0 -> SignalColors.Fair
            else -> SignalColors.Bad
        }
        label.contains("RSSI", ignoreCase = true) || label.contains("DBM", ignoreCase = true) -> when {
            value >= -85 -> SignalColors.Excellent
            value >= -100 -> SignalColors.Fair
            else -> SignalColors.Bad
        }
        else -> Color.Gray
    }
}

private val HISTORY_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
