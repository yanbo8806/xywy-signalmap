package com.xywy.signalmap.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconCatalog
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RasterLayerInfo
import com.xywy.signalmap.domain.model.isRaster
import kotlin.math.roundToInt

@Composable
fun LayerAppearanceDialog(
    layer: LayerMetadata,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String?, String?, LayerPointIconPreset, String?, Float?) -> Unit,
) {
    var showLabels by remember(layer.id) { mutableStateOf(layer.showLabels) }
    var labelTextColor by remember(layer.id) { mutableStateOf(layer.labelTextColor ?: LABEL_TEXT_PRESETS.first().hex) }
    var labelHaloColor by remember(layer.id) { mutableStateOf(layer.labelHaloColor ?: LABEL_HALO_PRESETS.first().hex) }
    var pointIconPreset by remember(layer.id) { mutableStateOf(layer.pointIconPreset) }
    var rasterColorHex by remember(layer.id) {
        mutableStateOf(layer.rasterInfo?.colorHex ?: RASTER_COLOR_PRESETS.first().hex)
    }
    var rasterOpacity by remember(layer.id) {
        mutableFloatStateOf(layer.rasterInfo?.opacity ?: RasterLayerInfo.DEFAULT_RASTER_OPACITY)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        showLabels,
                        labelTextColor,
                        labelHaloColor,
                        pointIconPreset,
                        if (layer.isRaster) rasterColorHex else null,
                        if (layer.isRaster) rasterOpacity else null,
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("图层样式")
                Text(
                    layer.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (layer.isRaster) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("栅格颜色", fontWeight = FontWeight.SemiBold)
                            Text(
                                "所有有效像素会统一渲染为单色块，透明度按下方设置统一生效。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ColorPresetRow(
                        presets = RASTER_COLOR_PRESETS,
                        selectedHex = rasterColorHex,
                        onSelected = { rasterColorHex = it },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "透明度：${(rasterOpacity * 100f).roundToInt()}%",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Slider(
                            value = rasterOpacity,
                            onValueChange = { rasterOpacity = it.coerceIn(0.05f, 1f) },
                            valueRange = 0.05f..1f,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("显示标签", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "关闭后该图层站名/点名不再显示。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = showLabels,
                                onCheckedChange = { showLabels = it },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("标签文字颜色", fontWeight = FontWeight.SemiBold)
                        ColorPresetRow(
                            presets = LABEL_TEXT_PRESETS,
                            selectedHex = labelTextColor,
                            onSelected = { labelTextColor = it },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("标签描边颜色", fontWeight = FontWeight.SemiBold)
                        ColorPresetRow(
                            presets = LABEL_HALO_PRESETS,
                            selectedHex = labelHaloColor,
                            onSelected = { labelHaloColor = it },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("点图标", fontWeight = FontWeight.SemiBold)
                        Text(
                            "图标点随本图层显示，不受工参站点开关影响。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PointIconPresetRow(
                            selected = pointIconPreset,
                            onSelected = { pointIconPreset = it },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ColorPresetRow(
    presets: List<ColorPreset>,
    selectedHex: String,
    onSelected: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presets) { preset ->
            val selected = preset.hex.equals(selectedHex, ignoreCase = true)
            Surface(
                onClick = { onSelected(preset.hex) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = parseColor(preset.hex),
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        preset.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PointIconPresetRow(
    selected: LayerPointIconPreset,
    onSelected: (LayerPointIconPreset) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(LayerPointIconPreset.entries) { preset ->
            val isSelected = preset == selected
            Surface(
                onClick = { onSelected(preset) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PointIconPreview(preset = preset)
                    Text(
                        preset.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PointIconPreview(preset: LayerPointIconPreset) {
    val style = LayerPointIconCatalog.style(preset)
    val fill = parseColor(style.fillColor)
    val stroke = parseColor(style.strokeColor)
    Canvas(modifier = Modifier.size(28.dp)) {
        if (style.targetMode) {
            drawCircle(color = stroke, radius = size.minDimension * 0.28f, style = Stroke(width = size.minDimension * 0.10f))
            drawCircle(color = stroke, radius = size.minDimension * 0.17f, style = Stroke(width = size.minDimension * 0.08f))
            drawCircle(color = fill, radius = size.minDimension * 0.08f)
            drawLine(
                color = stroke,
                start = center.copy(y = center.y - size.minDimension * 0.46f),
                end = center.copy(y = center.y - size.minDimension * 0.18f),
                strokeWidth = size.minDimension * 0.08f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = stroke,
                start = center.copy(y = center.y + size.minDimension * 0.18f),
                end = center.copy(y = center.y + size.minDimension * 0.46f),
                strokeWidth = size.minDimension * 0.08f,
                cap = StrokeCap.Round,
            )
        } else {
            val path = Path().apply {
                moveTo(center.x, size.height * 0.92f)
                lineTo(size.width * 0.34f, size.height * 0.55f)
                lineTo(size.width * 0.66f, size.height * 0.55f)
                close()
            }
            val circleCenter = center.copy(y = size.height * 0.34f)
            drawCircle(color = fill, radius = size.minDimension * 0.22f, center = circleCenter)
            drawPath(path = path, color = fill)
            drawCircle(color = stroke, radius = size.minDimension * 0.22f, center = circleCenter, style = Stroke(width = size.minDimension * 0.08f))
            drawPath(path = path, color = stroke, style = Stroke(width = size.minDimension * 0.08f))
            drawCircle(color = Color.White, radius = size.minDimension * 0.09f, center = circleCenter)
        }
    }
}

private fun parseColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

private data class ColorPreset(
    val title: String,
    val hex: String,
)

private val LABEL_TEXT_PRESETS = listOf(
    ColorPreset("白色", "#FFFFFF"),
    ColorPreset("淡蓝", "#9AD4FF"),
    ColorPreset("薄荷", "#B7EB8F"),
    ColorPreset("金黄", "#FFD666"),
    ColorPreset("橙色", "#FF9C6E"),
    ColorPreset("粉紫", "#FFADD2"),
)

private val LABEL_HALO_PRESETS = listOf(
    ColorPreset("黑色", "#111111"),
    ColorPreset("深蓝", "#10239E"),
    ColorPreset("金色", "#D4AF37"),
    ColorPreset("紫色", "#531DAB"),
    ColorPreset("棕色", "#5C3A21"),
    ColorPreset("深绿", "#135200"),
)

private val RASTER_COLOR_PRESETS = listOf(
    ColorPreset("红色", "#FF4D4F"),
    ColorPreset("橙色", "#FA8C16"),
    ColorPreset("黄色", "#FADB14"),
    ColorPreset("绿色", "#52C41A"),
    ColorPreset("青色", "#13C2C2"),
    ColorPreset("蓝色", "#1677FF"),
    ColorPreset("紫色", "#722ED1"),
)
