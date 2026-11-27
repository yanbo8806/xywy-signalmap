package com.xywy.signalmap.ui.map

import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xywy.signalmap.MainUiState
import com.xywy.signalmap.MapCameraTarget
import com.xywy.signalmap.SignalMapApplication
import com.xywy.signalmap.UserShapeType
import com.xywy.signalmap.data.local.RasterLayerAssetPaths
import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.isRaster
import com.xywy.signalmap.domain.map.GeoBounds
import com.xywy.signalmap.domain.map.MapCoordinateProjector
import com.xywy.signalmap.domain.map.MapMeasurementMode
import com.xywy.signalmap.domain.map.MapOverlayRenderPolicy
import com.xywy.signalmap.domain.map.MapRecordFilter
import com.xywy.signalmap.map.GeoJsonFeatureFactory
import com.xywy.signalmap.map.MapStyleFactory
import com.xywy.signalmap.map.PreparedMapVectors
import com.xywy.signalmap.map.SitePointBitmapFactory
import com.xywy.signalmap.map.SiteSymbolLayout
import com.xywy.signalmap.ui.MapFilterText
import com.xywy.signalmap.ui.MapTestRecordText
import com.xywy.signalmap.ui.MapToolText
import com.xywy.signalmap.ui.components.GlassPanelCard
import com.xywy.signalmap.ui.components.PanelCollapseButton
import com.xywy.signalmap.ui.components.PanelHeader
import com.xywy.signalmap.ui.components.PulsingDot
import com.xywy.signalmap.ui.components.SignalBars
import com.xywy.signalmap.ui.components.StatusDot
import com.xywy.signalmap.ui.components.TagChip
import com.xywy.signalmap.ui.components.ToggleChip
import com.xywy.signalmap.ui.theme.SignalColors
import com.xywy.signalmap.ui.theme.xywy
import kotlinx.coroutines.flow.Flow
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.Property.NONE
import org.maplibre.android.style.layers.Property.RASTER_RESAMPLING_NEAREST
import org.maplibre.android.style.layers.Property.TEXT_ANCHOR_CENTER
import org.maplibre.android.style.layers.Property.VISIBLE
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.circleTranslate
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.PropertyFactory.rasterResampling
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textMaxWidth
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.ImageSource
import org.maplibre.geojson.FeatureCollection
import java.io.File
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun MapScreen(
    state: MainUiState,
    preparedVectors: Map<String, PreparedMapVectors>,
    onRenderCenterChanged: (String, GeoPoint?) -> Unit,
    deviceSensorSnapshotFlow: Flow<DeviceSensorSnapshot>,
    cameraTargets: Flow<MapCameraTarget>,
    viewportSnapshots: Map<String, MapViewportSnapshot>,
    paddingValues: PaddingValues,
    onSearchQueryChanged: (String) -> Unit,
    onSubmitSearchQuery: () -> Unit,
    onJumpToCoordinate: (String) -> Unit,
    onFocusSector: (SectorRecord) -> Unit,
    onCenterLocation: () -> Unit,
    onToggleTechnology: (RadioTechnology) -> Unit,
    onToggleBand: (String) -> Unit,
    onToggleShowSectorFill: () -> Unit,
    onToggleShowSitePoints: () -> Unit,
    onToggleShowSiteLabels: () -> Unit,
    onToggleShowServingLine: () -> Unit,
    onToggleShowNeighborLines: () -> Unit,
    onToggleSimServingLine: (Int) -> Unit,
    onToggleSimNeighborLines: (Int) -> Unit,
    onToggleMapDisplayMode: () -> Unit,
    panelExpansionState: MapPanelExpansionState,
    onSearchExpandedChange: (Boolean) -> Unit,
    onFilterExpandedChange: (Boolean) -> Unit,
    onTestRecordExpandedChange: (Boolean) -> Unit,
    onToolExpandedChange: (Boolean) -> Unit,
    onCreateExpandedChange: (Boolean) -> Unit,
    onCreateUserLayer: (String) -> Unit,
    onAddUserPoint: (String, String, LayerPointIconPreset, Double?, Double?, String) -> Unit,
    onStartUserLine: (String, String) -> Unit,
    onStartUserPolygon: (String, String) -> Unit,
    onAddUserPolygonPoint: (Double, Double) -> Unit,
    onUndoUserPolygonPoint: () -> Unit,
    onFinishUserPolygon: () -> Unit,
    onCancelUserPolygon: () -> Unit,
    onUpdateUserRecord: (String, String, String, Double?, Double?, String, LayerPointIconPreset?) -> Unit,
    onDeleteUserRecord: (String, String) -> Unit,
    onEditUserPolygon: (String, String) -> Unit,
    onMoveUserPoint: (String, String, Double, Double) -> Unit,
    onStartTestRecording: () -> Unit,
    onStopTestRecording: () -> Unit,
    onToggleTestMetricMode: () -> Unit,
    onToggleSimTestPoints: (Int) -> Unit,
    onStartDistanceMeasurement: () -> Unit,
    onStartAreaMeasurement: () -> Unit,
    onAddMeasurementPoint: (Double, Double) -> Unit,
    onFinishMeasurement: () -> Unit,
    onClearMeasurement: () -> Unit,
    onViewportSnapshotChanged: (String, MapViewportSnapshot) -> Unit,
    onRequestPermissions: () -> Unit,
) {
    var pendingUserPointDraft by remember { mutableStateOf<PendingUserPointDraft?>(null) }
    var movingUserPoint by remember { mutableStateOf<MapSelectionCandidate?>(null) }
    // 地图全屏作为背景，不使用 paddingValues 的底部padding
    Box(modifier = Modifier.fillMaxSize()) {
        val deviceSensorSnapshot by deviceSensorSnapshotFlow.collectAsStateWithLifecycle(
            initialValue = DeviceSensorSnapshot.Empty,
        )
        val simSignalBadges = remember(state.simPreferenceStates, state.simMatches) {
            MapSimSignalBadgeFactory.create(
                simPreferences = state.simPreferenceStates,
                simMatches = state.simMatches,
            )
        }
        val deviceSensorSummary = remember(deviceSensorSnapshot) {
            MapDeviceSensorSummaryFactory.create(deviceSensorSnapshot)
        }
        val panels = when {
            state.mapPanels.isNotEmpty() -> state.mapPanels
            else -> emptyList()
        }

        // 地图铺满全屏
        if (state.mapDisplayMode == MapDisplayMode.Split && panels.size > 1) {
            Column(modifier = Modifier.fillMaxSize()) {
                panels.forEach { panel ->
                    SplitMapPanel(
                        state = state,
                        preparedVectors = preparedVectors[panel.id] ?: PreparedMapVectors.Empty,
                        onRenderCenterChanged = onRenderCenterChanged,
                        panelId = panel.id,
                        title = panel.title,
                        simMatches = panel.simMatches,
                        cameraTargets = cameraTargets,
                        savedViewportSnapshot = viewportSnapshots[panel.id],
                        pendingUserPointDraft = pendingUserPointDraft,
                        movingUserPoint = movingUserPoint,
                        onAddMeasurementPoint = onAddMeasurementPoint,
                        onAddUserPolygonPoint = onAddUserPolygonPoint,
                        onPlacePendingUserPoint = { longitude, latitude ->
                            val draft = pendingUserPointDraft ?: return@SplitMapPanel
                            onAddUserPoint(
                                draft.layerId,
                                draft.name,
                                draft.iconPreset,
                                longitude,
                                latitude,
                                draft.remark,
                            )
                            pendingUserPointDraft = null
                        },
                        onFinishMeasurement = onFinishMeasurement,
                        onUpdateUserRecord = onUpdateUserRecord,
                        onDeleteUserRecord = onDeleteUserRecord,
                        onEditUserPolygon = onEditUserPolygon,
                        onStartMoveUserPoint = { candidate ->
                            movingUserPoint = candidate
                        },
                        onMoveUserPoint = { longitude, latitude ->
                            val candidate = movingUserPoint ?: return@SplitMapPanel
                            onMoveUserPoint(candidate.record.layerId, candidate.record.id, longitude, latitude)
                            movingUserPoint = null
                        },
                        onViewportSnapshotChanged = { snapshot ->
                            onViewportSnapshotChanged(panel.id, snapshot)
                        },
                    )
                }
            }
        } else {
            val overlayPanel = panels.firstOrNull()
            val overlayPanelKey = overlayPanel?.id ?: "overlay"
            key(overlayPanel?.id ?: "overlay") {
                MapCanvas(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    preparedVectors = preparedVectors[overlayPanelKey] ?: PreparedMapVectors.Empty,
                    onRenderCenterChanged = onRenderCenterChanged,
                    simMatches = state.simMatches,
                    panelKey = overlayPanelKey,
                    cameraTargets = cameraTargets,
                    savedViewportSnapshot = viewportSnapshots[overlayPanelKey],
                    pendingUserPointDraft = pendingUserPointDraft,
                    movingUserPoint = movingUserPoint,
                    onAddMeasurementPoint = onAddMeasurementPoint,
                    onAddUserPolygonPoint = onAddUserPolygonPoint,
                    onPlacePendingUserPoint = { longitude, latitude ->
                        val draft = pendingUserPointDraft ?: return@MapCanvas
                        onAddUserPoint(
                            draft.layerId,
                            draft.name,
                            draft.iconPreset,
                            longitude,
                            latitude,
                            draft.remark,
                        )
                        pendingUserPointDraft = null
                    },
                    onFinishMeasurement = onFinishMeasurement,
                    onUpdateUserRecord = onUpdateUserRecord,
                    onDeleteUserRecord = onDeleteUserRecord,
                    onEditUserPolygon = onEditUserPolygon,
                    onStartMoveUserPoint = { candidate ->
                        movingUserPoint = candidate
                    },
                    onMoveUserPoint = { longitude, latitude ->
                        val candidate = movingUserPoint ?: return@MapCanvas
                        onMoveUserPoint(candidate.record.layerId, candidate.record.id, longitude, latitude)
                        movingUserPoint = null
                    },
                    onViewportSnapshotChanged = { snapshot ->
                        onViewportSnapshotChanged(overlayPanelKey, snapshot)
                    },
                )
            }
            if ((overlayPanel?.title?.isNotBlank() == true) && state.simMatches.size > 1) {
                PanelBadge(
                    title = overlayPanel.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp),
                )
            }
        }

        // 顶部悬浮搜索 + 筛选区域（带毛玻璃效果）
        val createCenterViewport = viewportSnapshots["overlay"]
            ?: panels.firstNotNullOfOrNull { panel -> viewportSnapshots[panel.id] }
        MapOverlayLayout(
            panelsExpanded = panelExpansionState.run {
                searchExpanded || filterExpanded || testRecordExpanded || toolExpanded || createExpanded
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .imePadding()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = paddingValues.calculateBottomPadding() + 12.dp),
            toolbar = {
                MapToolbar(
                    panelExpansionState = panelExpansionState,
                    isRecording = state.isTestRecording,
                    isMeasuring = state.measurementState.isActive,
                    onSearchExpandedChange = onSearchExpandedChange,
                    onFilterExpandedChange = onFilterExpandedChange,
                    onTestRecordExpandedChange = onTestRecordExpandedChange,
                    onToolExpandedChange = onToolExpandedChange,
                    onCreateExpandedChange = onCreateExpandedChange,
                )
            },
            panels = {
                PanelEnterAnimation(visible = panelExpansionState.searchExpanded) {
                    SearchCard(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onSubmitSearchQuery = onSubmitSearchQuery,
                        onJumpToCoordinate = onJumpToCoordinate,
                        onRequestPermissions = onRequestPermissions,
                        onCollapse = { onSearchExpandedChange(false) },
                    )
                }
                PanelEnterAnimation(visible = panelExpansionState.filterExpanded) {
                    FilterCard(
                        state = state,
                        onToggleTechnology = onToggleTechnology,
                        onToggleBand = onToggleBand,
                        onToggleShowSectorFill = onToggleShowSectorFill,
                        onToggleShowSitePoints = onToggleShowSitePoints,
                        onToggleShowSiteLabels = onToggleShowSiteLabels,
                        onToggleShowServingLine = onToggleShowServingLine,
                        onToggleShowNeighborLines = onToggleShowNeighborLines,
                        onToggleSimServingLine = onToggleSimServingLine,
                        onToggleSimNeighborLines = onToggleSimNeighborLines,
                        onToggleMapDisplayMode = onToggleMapDisplayMode,
                        onExpandedChange = onFilterExpandedChange,
                    )
                }
                PanelEnterAnimation(visible = panelExpansionState.testRecordExpanded) {
                    TestRecordCard(
                        state = state,
                        onExpandedChange = onTestRecordExpandedChange,
                        onStart = onStartTestRecording,
                        onStop = onStopTestRecording,
                        onToggleMetricMode = onToggleTestMetricMode,
                        onToggleSimTestPoints = onToggleSimTestPoints,
                    )
                }
                PanelEnterAnimation(visible = panelExpansionState.toolExpanded) {
                    ToolCard(
                        state = state,
                        onExpandedChange = onToolExpandedChange,
                        onStartDistanceMeasurement = onStartDistanceMeasurement,
                        onStartAreaMeasurement = onStartAreaMeasurement,
                        onFinishMeasurement = onFinishMeasurement,
                        onClearMeasurement = onClearMeasurement,
                    )
                }
                PanelEnterAnimation(visible = panelExpansionState.createExpanded) {
                    CreateLayerCard(
                        state = state,
                        onExpandedChange = onCreateExpandedChange,
                        onCreateLayer = onCreateUserLayer,
                        onAddPoint = onAddUserPoint,
                        centerViewport = createCenterViewport,
                        pendingPointDraft = pendingUserPointDraft,
                        onStartPointPlacement = { draft -> pendingUserPointDraft = draft },
                        onStartLine = onStartUserLine,
                        onStartPolygon = onStartUserPolygon,
                        onUndoPolygonPoint = onUndoUserPolygonPoint,
                        onFinishPolygon = onFinishUserPolygon,
                        onCancelPolygon = onCancelUserPolygon,
                    )
                }
                PanelEnterAnimation(
                    visible = panelExpansionState.searchExpanded &&
                        state.searchResults.isNotEmpty() &&
                        state.searchQuery.trim() == state.submittedSearchQuery,
                ) {
                    SearchResultCard(results = state.searchResults, onFocusSector = onFocusSector)
                }
            },
            signalHud = if (simSignalBadges.isNotEmpty()) {
                { MapSimSignalColumn(badges = simSignalBadges, deviceSensorSummary = deviceSensorSummary) }
            } else null,
            compactSignalHud = if (simSignalBadges.isNotEmpty()) {
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        simSignalBadges.forEach { badge ->
                            Box(Modifier.weight(1f)) { MapSimSignalChip(badge) }
                        }
                    }
                }
            } else null,
        )
        if (pendingUserPointDraft != null) {
            PanelBadge(
                title = "点选位置：点击地图创建点",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
        if (movingUserPoint != null) {
            PanelBadge(
                title = "移动点：长按拖到新位置后松手",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }

        // 右下角定位按钮
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp),
            onClick = onCenterLocation,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
            ),
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = "定位到当前位置",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun MapToolbar(
    panelExpansionState: MapPanelExpansionState,
    isRecording: Boolean,
    isMeasuring: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onFilterExpandedChange: (Boolean) -> Unit,
    onTestRecordExpandedChange: (Boolean) -> Unit,
    onToolExpandedChange: (Boolean) -> Unit,
    onCreateExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollapsedPanelIconButton(
            icon = Icons.Filled.Search,
            contentDescription = "\u641C\u7D22",
            expanded = panelExpansionState.searchExpanded,
            onClick = { onSearchExpandedChange(!panelExpansionState.searchExpanded) },
        )
        CollapsedPanelIconButton(
            icon = Icons.Filled.Layers,
            contentDescription = MapFilterText.title,
            expanded = panelExpansionState.filterExpanded,
            onClick = { onFilterExpandedChange(!panelExpansionState.filterExpanded) },
        )
        CollapsedPanelIconButton(
            icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = MapTestRecordText.title,
            expanded = panelExpansionState.testRecordExpanded,
            accentColor = if (isRecording) MaterialTheme.colorScheme.error else null,
            showPulse = isRecording,
            onClick = { onTestRecordExpandedChange(!panelExpansionState.testRecordExpanded) },
        )
        CollapsedPanelIconButton(
            icon = Icons.Filled.Build,
            contentDescription = MapToolText.title,
            expanded = panelExpansionState.toolExpanded,
            accentColor = if (isMeasuring) MaterialTheme.colorScheme.primary else null,
            onClick = { onToolExpandedChange(!panelExpansionState.toolExpanded) },
        )
        CollapsedPanelIconButton(
            icon = Icons.Filled.Add,
            contentDescription = "新建",
            expanded = panelExpansionState.createExpanded,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = { onCreateExpandedChange(!panelExpansionState.createExpanded) },
        )
    }
}

@Composable
private fun ColumnScope.SplitMapPanel(
    state: MainUiState,
    preparedVectors: PreparedMapVectors,
    onRenderCenterChanged: (String, GeoPoint?) -> Unit,
    panelId: String,
    title: String,
    simMatches: List<SimCellMatch>,
    cameraTargets: Flow<MapCameraTarget>,
    savedViewportSnapshot: MapViewportSnapshot?,
    pendingUserPointDraft: PendingUserPointDraft?,
    movingUserPoint: MapSelectionCandidate?,
    onAddMeasurementPoint: (Double, Double) -> Unit,
    onAddUserPolygonPoint: (Double, Double) -> Unit,
    onPlacePendingUserPoint: (Double, Double) -> Unit,
    onFinishMeasurement: () -> Unit,
    onUpdateUserRecord: (String, String, String, Double?, Double?, String, LayerPointIconPreset?) -> Unit,
    onDeleteUserRecord: (String, String) -> Unit,
    onEditUserPolygon: (String, String) -> Unit,
    onStartMoveUserPoint: (MapSelectionCandidate) -> Unit,
    onMoveUserPoint: (Double, Double) -> Unit,
    onViewportSnapshotChanged: (MapViewportSnapshot) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        key(panelId) {
            MapCanvas(
                modifier = Modifier.fillMaxSize(),
                state = state,
                preparedVectors = preparedVectors,
                onRenderCenterChanged = onRenderCenterChanged,
                simMatches = simMatches,
                panelKey = panelId,
                cameraTargets = cameraTargets,
                savedViewportSnapshot = savedViewportSnapshot,
                pendingUserPointDraft = pendingUserPointDraft,
                movingUserPoint = movingUserPoint,
                onAddMeasurementPoint = onAddMeasurementPoint,
                onAddUserPolygonPoint = onAddUserPolygonPoint,
                onPlacePendingUserPoint = onPlacePendingUserPoint,
                onFinishMeasurement = onFinishMeasurement,
                onUpdateUserRecord = onUpdateUserRecord,
                onDeleteUserRecord = onDeleteUserRecord,
                onEditUserPolygon = onEditUserPolygon,
                onStartMoveUserPoint = onStartMoveUserPoint,
                onMoveUserPoint = onMoveUserPoint,
                onViewportSnapshotChanged = onViewportSnapshotChanged,
            )
        }
        PanelBadge(
            title = title,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )
    }
}

@Composable
private fun PanelEnterAnimation(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) +
            expandVertically(
                animationSpec = androidx.compose.animation.core.tween(220),
                expandFrom = Alignment.Top,
            ),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(140)) +
            shrinkVertically(
                animationSpec = androidx.compose.animation.core.tween(200),
                shrinkTowards = Alignment.Top,
            ),
    ) {
        content()
    }
}

@Composable
private fun CollapsedPanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    expanded: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null,
    showPulse: Boolean = false,
) {
    val activeColor = accentColor ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (expanded) activeColor else MaterialTheme.xywy.glass,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (expanded) Color.Transparent else MaterialTheme.xywy.glassBorder,
        ),
        shadowElevation = if (expanded) 6.dp else 4.dp,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (expanded) {
                    contentColorFor(activeColor)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (showPulse && !expanded) {
                PulsingDot(
                    color = accentColor ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp),
                    size = 8.dp,
                )
            }
        }
    }
}

@Composable
private fun CreateLayerCard(
    state: MainUiState,
    onExpandedChange: (Boolean) -> Unit,
    onCreateLayer: (String) -> Unit,
    onAddPoint: (String, String, LayerPointIconPreset, Double?, Double?, String) -> Unit,
    centerViewport: MapViewportSnapshot?,
    pendingPointDraft: PendingUserPointDraft?,
    onStartPointPlacement: (PendingUserPointDraft) -> Unit,
    onStartLine: (String, String) -> Unit,
    onStartPolygon: (String, String) -> Unit,
    onUndoPolygonPoint: () -> Unit,
    onFinishPolygon: () -> Unit,
    onCancelPolygon: () -> Unit,
) {
    var newLayerName by remember { mutableStateOf("手动图层") }
    var objectName by remember { mutableStateOf("新建点") }
    var pointRemark by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf("") }
    var iconPreset by remember { mutableStateOf(LayerPointIconPreset.RED_PIN) }
    val editableLayers = state.layers.filter { it.userEditable }
    LaunchedEffect(editableLayers.map { it.id }) {
        if (selectedLayerId.isBlank() || editableLayers.none { it.id == selectedLayerId }) {
            selectedLayerId = editableLayers.firstOrNull()?.id.orEmpty()
        }
    }
    val polygonDraft = state.userPolygonDraft

    GlassPanelCard(contentPadding = 12.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PanelHeader(
                title = "新建",
                icon = Icons.Filled.Add,
                accent = MaterialTheme.colorScheme.tertiary,
                onCollapse = { onExpandedChange(false) },
                collapseDescription = "收起",
            )
            Text(
                "已有手动图层可直接选择；只有需要新图层时再点“新建图层”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            centerViewport?.let { viewport ->
                Text(
                    text = String.format(
                        Locale.US,
                        "屏幕中心：%.6f, %.6f",
                        viewport.centerLongitude,
                        viewport.centerLatitude,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = newLayerName,
                onValueChange = { newLayerName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("新图层名称") },
                singleLine = true,
            )

            if (editableLayers.isNotEmpty()) {
                Text(
                    "选择已有图层",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(editableLayers, key = { it.id }) { layer ->
                        Surface(
                            onClick = { selectedLayerId = layer.id },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selectedLayerId == layer.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                        ) {
                            Text(
                                layer.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedLayerId == layer.id) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val layerName = newLayerName.trim().ifBlank { "手动图层" }
                    onCreateLayer(layerName)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("新建图层")
            }

            if (editableLayers.isNotEmpty()) {
                OutlinedTextField(
                    value = objectName,
                    onValueChange = { objectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = pointRemark,
                    onValueChange = { pointRemark = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    minLines = 1,
                    maxLines = 2,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LayerPointIconPreset.entries, key = { it.name }) { preset ->
                        Surface(
                            onClick = { iconPreset = preset },
                            shape = RoundedCornerShape(999.dp),
                            color = if (iconPreset == preset) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                        ) {
                            Text(
                                preset.title,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(
                        onClick = {
                            onStartPointPlacement(
                                PendingUserPointDraft(
                                    layerId = selectedLayerId,
                                    name = objectName.trim().ifBlank { "新建点" },
                                    remark = pointRemark,
                                    iconPreset = iconPreset,
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("地图点选")
                    }
                    ElevatedButton(
                        onClick = {
                            onStartLine(
                                selectedLayerId,
                                objectName.trim().ifBlank { "新建线" },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("画线")
                    }
                    ElevatedButton(
                        onClick = {
                            onStartPolygon(
                                selectedLayerId,
                                objectName.trim().ifBlank { "新建多边形" },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("画多边形")
                    }
                }
                if (pendingPointDraft != null) {
                    Text(
                        "正在点选：请在地图上点击要创建的位置",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (polygonDraft != null) {
                    val shapeName = if (polygonDraft.shapeType == UserShapeType.LINE) "线" else "多边形"
                    val minPointCount = if (polygonDraft.shapeType == UserShapeType.LINE) 2 else 3
                    Text(
                        if (polygonDraft.recordId == null) {
                            "正在绘制$shapeName：点击地图加点，当前 ${polygonDraft.points.size} 个点"
                        } else {
                            "正在编辑$shapeName：点击地图追加顶点，当前 ${polygonDraft.points.size} 个点"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onUndoPolygonPoint,
                            enabled = polygonDraft.points.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("撤销点")
                        }
                        Button(
                            onClick = onFinishPolygon,
                            enabled = polygonDraft.points.size >= minPointCount,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (polygonDraft.recordId == null) "完成" else "保存")
                        }
                        TextButton(
                            onClick = onCancelPolygon,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("取消")
                        }
                    }
                }
            } else {
                Text(
                    "请先新建一个手动图层。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    state: MainUiState,
    onExpandedChange: (Boolean) -> Unit,
    onStartDistanceMeasurement: () -> Unit,
    onStartAreaMeasurement: () -> Unit,
    onFinishMeasurement: () -> Unit,
    onClearMeasurement: () -> Unit,
) {
    val measurementState = state.measurementState
    GlassPanelCard(contentPadding = 12.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PanelHeader(
                title = MapToolText.title,
                icon = Icons.Filled.Build,
                onCollapse = { onExpandedChange(false) },
                collapseDescription = MapToolText.expandDescription(true),
                badge = measurementModeBadge(measurementState.mode),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    ToggleChip(
                        label = MapToolText.distance,
                        active = measurementState.mode == MapMeasurementMode.DISTANCE,
                        onClick = onStartDistanceMeasurement,
                    )
                }
                item {
                    ToggleChip(
                        label = MapToolText.area,
                        active = measurementState.mode == MapMeasurementMode.AREA,
                        onClick = onStartAreaMeasurement,
                    )
                }
                if (measurementState.canFinish) {
                    item {
                        ToggleChip(
                            label = MapToolText.finish,
                            active = false,
                            onClick = onFinishMeasurement,
                        )
                    }
                }
                item {
                    ToggleChip(
                        label = MapToolText.clear,
                        active = false,
                        onClick = onClearMeasurement,
                    )
                }
            }

            Text(
                text = measurementSummaryText(measurementState),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun measurementModeBadge(mode: MapMeasurementMode): String? = when (mode) {
    MapMeasurementMode.NONE -> null
    MapMeasurementMode.DISTANCE -> MapToolText.distance
    MapMeasurementMode.AREA -> MapToolText.area
}

private fun measurementSummaryText(
    state: com.xywy.signalmap.domain.map.MapMeasurementState,
): String {
    if (!state.isActive) return MapToolText.idleHint
    return when (state.mode) {
        MapMeasurementMode.DISTANCE -> {
            val distance = state.distanceMeters?.let(::formatDistanceText) ?: "--"
            "\u5DF2\u52A0 ${state.points.size} \u4E2A\u70B9 \u00B7 \u603B\u8DDD\u79BB $distance"
        }

        MapMeasurementMode.AREA -> {
            val distance = state.distanceMeters?.let(::formatDistanceText) ?: "--"
            val area = state.areaSquareMeters?.let(::formatAreaText) ?: "\u5F85\u5F62\u6210\u591A\u8FB9\u5F62"
            "\u5DF2\u52A0 ${state.points.size} \u4E2A\u70B9 \u00B7 \u5468\u957F $distance \u00B7 \u9762\u79EF $area"
        }

        MapMeasurementMode.NONE -> MapToolText.idleHint
    }
}

private fun formatDistanceText(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.CHINA, "%.2f km", value / 1000.0)
    else -> String.format(Locale.CHINA, "%.0f m", value)
}

private fun formatAreaText(value: Double): String = when {
    value >= 1_000_000.0 -> String.format(Locale.CHINA, "%.2f km²", value / 1_000_000.0)
    else -> String.format(Locale.CHINA, "%.0f ㎡", value)
}

@Composable
internal fun SearchCard(
    modifier: Modifier = Modifier,
    state: MainUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSubmitSearchQuery: () -> Unit,
    onJumpToCoordinate: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onCollapse: () -> Unit,
) {
    val servingMatches = state.simMatches.take(2)

    // 半透明毛玻璃风格卡片
    GlassPanelCard(modifier = modifier, contentPadding = 14.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PanelHeader(
                title = "${state.tilePreset.title} · ${state.layers.count { it.isVisible }} 图层",
                icon = Icons.Outlined.CellTower,
                onCollapse = onCollapse,
                collapseDescription = "收起搜索",
            )

            if (!state.locationPermissionGranted || !state.phonePermissionGranted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "缺少定位 / 电话权限",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(
                            onClick = onRequestPermissions,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        ) {
                            Text("授权", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (servingMatches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    servingMatches.forEach { match ->
                        ServingSummaryBadge(match = match)
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                label = { Text("搜索站点 / 小区 / 经纬度", fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitSearchQuery() }),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                ),
            )

            if (state.searchQuery.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSubmitSearchQuery,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("\u67E5\u8BE2", fontSize = 13.sp)
                    }
                    FilledTonalButton(
                        onClick = { onJumpToCoordinate(state.searchQuery) },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("跳转坐标", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TestRecordCard(
    state: MainUiState,
    onExpandedChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggleMetricMode: () -> Unit,
    onToggleSimTestPoints: (Int) -> Unit,
) {
    GlassPanelCard(contentPadding = 12.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.xywy.signalmap.ui.components.IconTile(
                    icon = Icons.Filled.SignalCellularAlt,
                    tint = if (state.isTestRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    tileSize = 30.dp,
                    iconSize = 16.dp,
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = MapTestRecordText.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                PanelCollapseButton(
                    onClick = { onExpandedChange(false) },
                    contentDescription = MapTestRecordText.expandDescription(true),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onToggleMetricMode,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = MapTestRecordText.metricPrefix + state.testMetricMode.name,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    onClick = if (state.isTestRecording) onStop else onStart,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (state.isTestRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.isTestRecording) {
                            PulsingDot(
                                color = MaterialTheme.colorScheme.onError,
                                size = 9.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isTestRecording) {
                                MapTestRecordText.stopAction
                            } else {
                                MapTestRecordText.startAction
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isTestRecording) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val summary = if (state.isTestRecording) {
                    MapTestRecordText.recordingSummary(state.activeTestSampleCount)
                } else {
                    MapTestRecordText.loadedSummary(state.testLogs.count { it.isVisible })
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = MapTestRecordText.visibleSimTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.simPreferenceStates, key = { "test-${it.slotIndex}" }) { sim ->
                        ToggleChip(
                            label = "SIM${sim.slotIndex + 1}",
                            active = sim.showTestPoints,
                            onClick = { onToggleSimTestPoints(sim.slotIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServingSummaryBadge(match: SimCellMatch) {
    val servingName = match.servingSector?.cellName?.takeIf(String::isNotBlank)
        ?: match.servingSector?.siteName?.takeIf(String::isNotBlank)
        ?: match.servingCell?.cellId
        ?: "未锁定"
    val techColor = SignalColors.forTechnology(match.servingCell?.radioTechnology ?: RadioTechnology.UNKNOWN)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = techColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, techColor.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = techColor)
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = match.label.ifBlank { "SIM ${match.slotIndex + 1}" },
                    style = MaterialTheme.typography.labelMedium,
                    color = techColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "占用小区：$servingName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SignalBars(
                rsrp = match.servingCell?.takeUnless { it.isStale }?.rsrp,
                tint = techColor,
            )
        }
    }
}

@Composable
private fun CollapsedSearchChip(
    modifier: Modifier = Modifier,
    query: String,
    resultCount: Int,
    onExpand: () -> Unit,
) {
    val label = when {
        query.isNotBlank() && resultCount > 0 -> "搜索：${query.take(10)} · ${resultCount}条"
        query.isNotBlank() -> "搜索：${query.take(12)}"
        else -> "搜索 / 跳转"
    }
    Surface(
        modifier = modifier,
        onClick = onExpand,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterCard(
    state: MainUiState,
    onToggleTechnology: (RadioTechnology) -> Unit,
    onToggleBand: (String) -> Unit,
    onToggleShowSectorFill: () -> Unit,
    onToggleShowSitePoints: () -> Unit,
    onToggleShowSiteLabels: () -> Unit,
    onToggleShowServingLine: () -> Unit,
    onToggleShowNeighborLines: () -> Unit,
    onToggleSimServingLine: (Int) -> Unit,
    onToggleSimNeighborLines: (Int) -> Unit,
    onToggleMapDisplayMode: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    GlassPanelCard(contentPadding = 12.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelHeader(
                title = MapFilterText.title,
                icon = Icons.Filled.Layers,
                onCollapse = { onExpandedChange(false) },
                collapseDescription = MapFilterText.expandDescription(true),
            )

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(RadioTechnology.NR, RadioTechnology.LTE)) { technology ->
                            val isSelected = technology in state.selectedTechs
                            val techColor = SignalColors.forTechnology(technology)
                            Surface(
                                onClick = { onToggleTechnology(technology) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) techColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.dp, techColor.copy(alpha = 0.55f))
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.7f))
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    StatusDot(
                                        color = if (isSelected) techColor else Color.Gray.copy(alpha = 0.4f),
                                        size = 8.dp,
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = technology.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) techColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    if (state.availableBands.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.availableBands) { band ->
                                val isSelected = band in state.selectedBands
                                Surface(
                                    onClick = { onToggleBand(band) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    } else {
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.7f))
                                    },
                                ) {
                                    Text(
                                        text = band,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            ToggleChip(
                                label = MapFilterText.sectorFill,
                                active = state.showSectorFill,
                                onClick = onToggleShowSectorFill,
                            )
                        }
                        item {
                            ToggleChip(
                                label = MapFilterText.sitePoints,
                                active = state.showSitePoints,
                                onClick = onToggleShowSitePoints,
                            )
                        }
                        item {
                            ToggleChip(
                                label = MapFilterText.siteLabels,
                                active = state.showSiteLabels,
                                onClick = onToggleShowSiteLabels,
                            )
                        }
                        item {
                            ToggleChip(
                                label = MapFilterText.servingLine,
                                active = state.showServingLine,
                                onClick = onToggleShowServingLine,
                            )
                        }
                        item {
                            ToggleChip(
                                label = MapFilterText.neighborLine,
                                active = state.showNeighborLines,
                                onClick = onToggleShowNeighborLines,
                            )
                        }
                        item {
                            ToggleChip(
                                label = if (state.mapDisplayMode == MapDisplayMode.Overlay) MapFilterText.overlayMode else MapFilterText.splitMode,
                                active = state.mapDisplayMode == MapDisplayMode.Split,
                                onClick = onToggleMapDisplayMode,
                            )
                        }
                    }

                    if (state.simPreferenceStates.isNotEmpty()) {
                        Text(
                            text = MapFilterText.servingLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.simPreferenceStates, key = { it.slotIndex }) { sim ->
                                ToggleChip(
                                    label = "SIM${sim.slotIndex + 1}",
                                    active = sim.showServingLine,
                                    onClick = { onToggleSimServingLine(sim.slotIndex) },
                                )
                            }
                        }

                        Text(
                            text = MapFilterText.neighborLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.simPreferenceStates, key = { "neighbor-${it.slotIndex}" }) { sim ->
                                ToggleChip(
                                    label = "SIM${sim.slotIndex + 1}",
                                    active = sim.showNeighborLines,
                                    onClick = { onToggleSimNeighborLines(sim.slotIndex) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun MapSimSignalColumn(
    badges: List<MapSimSignalBadgeState>,
    deviceSensorSummary: MapDeviceSensorSummaryState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        badges.forEach { badge ->
            MapSimSignalChip(badge = badge)
        }
        MapDeviceSensorChip(summary = deviceSensorSummary)
    }
}

@Composable
internal fun MapSimSignalChip(badge: MapSimSignalBadgeState) {
    val technology = RadioTechnology.fromRaw(badge.technologyLabel)
    val techColor = if (badge.active) {
        SignalColors.forTechnology(technology)
    } else {
        MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.xywy.glass,
        border = androidx.compose.foundation.BorderStroke(1.dp, techColor.copy(alpha = 0.45f)),
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(color = techColor, size = 7.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = techColor,
                        maxLines = 1,
                    )
                }
                Text(
                    text = badge.technologyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricPill(
                    modifier = Modifier.fillMaxWidth(),
                    label = "RSRP",
                    value = badge.rsrpText,
                    color = techColor,
                    strong = true,
                )
                MetricPill(
                    modifier = Modifier.fillMaxWidth(),
                    label = "SINR",
                    value = badge.sinrText,
                    color = MaterialTheme.colorScheme.secondary,
                    strong = false,
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    strong: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = if (strong) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MapDeviceSensorChip(summary: MapDeviceSensorSummaryState) {
    val borderColor = if (summary.available) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.xywy.glass,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.45f)),
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = borderColor, size = 7.dp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "手机姿态",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor,
                    maxLines = 1,
                )
            }
            SensorInlineText(text = summary.verticalAngleText, strong = true)
            SensorInlineText(text = summary.azimuthText, strong = false)
            SensorInlineText(text = summary.altitudeText, strong = false)
            SensorInlineText(text = summary.pressureText, strong = false)
        }
    }
}

@Composable
private fun SensorInlineText(
    text: String,
    strong: Boolean,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun SearchResultCard(
    results: List<SectorRecord>,
    onFocusSector: (SectorRecord) -> Unit,
) {
    GlassPanelCard(contentPadding = 6.dp) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(
                items = results,
                key = { it.id },
            ) { record ->
                Surface(
                    onClick = { onFocusSector(record) },
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CellTower,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            record.cellName ?: record.siteName ?: record.id,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        record.band?.let { band ->
                            TagChip(text = band, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelBadge(
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.xywy.glass,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.xywy.glassBorder),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = MaterialTheme.colorScheme.primary, size = 7.dp)
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MapCanvas(
    modifier: Modifier,
    state: MainUiState,
    preparedVectors: PreparedMapVectors,
    onRenderCenterChanged: (String, GeoPoint?) -> Unit,
    simMatches: List<SimCellMatch>,
    panelKey: String,
    cameraTargets: Flow<MapCameraTarget>,
    savedViewportSnapshot: MapViewportSnapshot?,
    pendingUserPointDraft: PendingUserPointDraft?,
    movingUserPoint: MapSelectionCandidate?,
    onAddMeasurementPoint: (Double, Double) -> Unit,
    onAddUserPolygonPoint: (Double, Double) -> Unit,
    onPlacePendingUserPoint: (Double, Double) -> Unit,
    onFinishMeasurement: () -> Unit,
    onUpdateUserRecord: (String, String, String, Double?, Double?, String, LayerPointIconPreset?) -> Unit,
    onDeleteUserRecord: (String, String) -> Unit,
    onEditUserPolygon: (String, String) -> Unit,
    onStartMoveUserPoint: (MapSelectionCandidate) -> Unit,
    onMoveUserPoint: (Double, Double) -> Unit,
    onViewportSnapshotChanged: (MapViewportSnapshot) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tileProxyConfig = remember(context.applicationContext) {
        (context.applicationContext as? SignalMapApplication)?.container?.tileProxyConfig
    }
    val mapView = remember(panelKey) {
        MapView(context, MapLibreMapOptions.createFromAttributes(context).localIdeographFontFamily("sans-serif"))
            .apply { onCreate(null) }
    }
    var pendingTarget by remember(panelKey) { mutableStateOf<MapCameraTarget?>(null) }
    val cameraIdleListenerRef = remember(panelKey) { MapRuntimeValue<MapLibreMap.OnCameraIdleListener?>(null) }
    val cameraMoveStartedListenerRef = remember(panelKey) { MapRuntimeValue<MapLibreMap.OnCameraMoveStartedListener?>(null) }
    val cameraMoveCanceledListenerRef = remember(panelKey) { MapRuntimeValue<MapLibreMap.OnCameraMoveCanceledListener?>(null) }
    val gestureSettingsAppliedRef = remember(panelKey) { MapRuntimeValue(false) }
    var isGestureInteracting by remember(panelKey) { mutableStateOf(false) }
    val mapClickListenerRef = remember(panelKey) { MapRuntimeValue<MapLibreMap.OnMapClickListener?>(null) }
    val longClickListenerRef = remember(panelKey) { MapRuntimeValue<MapLibreMap.OnMapLongClickListener?>(null) }
    val currentTilePresetRef = remember(panelKey) { MapRuntimeValue(state.tilePreset) }
    var selectionCandidates by remember(panelKey) { mutableStateOf<List<MapSelectionCandidate>>(emptyList()) }
    var detailCandidate by remember(panelKey) { mutableStateOf<MapSelectionCandidate?>(null) }
    val lastVectorSourceStateRef = remember(panelKey) { MapRuntimeValue<PreparedMapVectors?>(null) }
    var sourcesReady by remember(panelKey) { mutableStateOf(false) }
    val mapDisposed = remember(panelKey) { MapRuntimeValue(false) }
    val lastRasterSourceStateRef = remember(panelKey) { MapRuntimeValue<MapRasterSourceState?>(null) }
    val lastLocationSourceStateRef = remember(panelKey) { MapRuntimeValue<MapLocationSourceState?>(null) }
    val lastLineSourceStateRef = remember(panelKey) { MapRuntimeValue<MapLineSourceState?>(null) }
    val lastTestLogSourceStateRef = remember(panelKey) { MapRuntimeValue<MapTestLogSourceState?>(null) }
    val lastMeasurementSourceStateRef = remember(panelKey) { MapRuntimeValue<MapMeasurementSourceState?>(null) }
    val lastUserDraftSourceStateRef = remember(panelKey) { MapRuntimeValue<MapUserDraftSourceState?>(null) }
    val moveTouchStateRef = remember(panelKey) { MapRuntimeValue<PointMoveTouchState?>(null) }
    var autoCenteredOnFirstLocation by remember(panelKey) { mutableStateOf(false) }
    var userGestureMovedMap by remember(panelKey) { mutableStateOf(false) }
    val activeRasterLayerIds = remember(panelKey) { linkedSetOf<String>() }
    val rasterImageCache = remember(panelKey) { RasterLayerImageCache<android.graphics.Bitmap> { file -> BitmapFactory.decodeFile(file.absolutePath) } }
    val latestViewportChangeHandler by rememberUpdatedState(onViewportSnapshotChanged)

    currentTilePresetRef.value = state.tilePreset

    val activeViewport = remember(savedViewportSnapshot, state.renderAnchorLocation, state.currentLocation, state.visibleSectors, simMatches) {
        resolveMapViewportSnapshot(
            retainedSnapshot = savedViewportSnapshot,
            currentLocation = state.renderAnchorLocation ?: state.currentLocation,
            simMatches = simMatches,
            visibleSectors = state.visibleSectors,
        )
    }
    val latestRenderCenterHandler by rememberUpdatedState(onRenderCenterChanged)
    LaunchedEffect(panelKey, activeViewport.centerLongitude, activeViewport.centerLatitude) {
        latestRenderCenterHandler(panelKey, GeoPoint(activeViewport.centerLongitude, activeViewport.centerLatitude))
    }
    DisposableEffect(panelKey) {
        onDispose { latestRenderCenterHandler(panelKey, null) }
    }
    val hasPointOnlyRecords = remember(state.visibleSectors, state.layers) {
        val visibleLayerIds = state.layers.filter { it.isVisible }.mapTo(hashSetOf()) { it.id }
        state.visibleSectors.any { it.isPointOnly && it.layerId in visibleLayerIds }
    }
    val overlayRenderPolicy = remember(
        state.showSectorFill,
        state.showSitePoints,
        state.showSiteLabels,
        state.showServingLine,
        state.showNeighborLines,
        hasPointOnlyRecords,
    ) {
        MapOverlayRenderPolicy.resolve(
            zoom = activeViewport.zoom,
            wantSectorFill = state.showSectorFill,
            wantSitePoints = state.showSitePoints,
            wantSiteLabels = state.showSiteLabels,
            wantServingLine = state.showServingLine,
            wantNeighborLines = state.showNeighborLines,
            wantPointIcons = hasPointOnlyRecords,
        )
    }
    val latestOverlayRenderPolicy by rememberUpdatedState(overlayRenderPolicy)
    val compatibleVectors = preparedVectors.forTilePreset(state.tilePreset)
    val rawRenderedById = compatibleVectors.rawRecordsById
    val displayRenderedById = compatibleVectors.displayRecordsById
    val layersById = remember(state.layers) { state.layers.associateBy { it.id } }
    val visibleLayerFilter = remember(state.layers) {
        Expression.`in`(Expression.get("layerId"), Expression.literal(state.layers.filter { it.isVisible }.map { it.id }.toTypedArray()))
    }
    val visibleLabelFilter = remember(state.layers) {
        Expression.`in`(Expression.get("layerId"), Expression.literal(state.layers.filter { it.isVisible && it.showLabels }.map { it.id }.toTypedArray()))
    }
    val lastLayerFilterRef = remember(panelKey) { MapRuntimeValue<Any?>(null) }
    val visibleRasterLayers = remember(state.layers) {
        state.layers.filter { it.isVisible && it.isRaster }
    }
    val displayLocation = remember(state.currentLocation, state.tilePreset) {
        MapCoordinateProjector.toDisplayLocation(state.currentLocation, state.tilePreset)
    }
    val displaySimMatches = remember(
        simMatches,
        state.tilePreset,
        overlayRenderPolicy.showServingLine,
        overlayRenderPolicy.showNeighborLines,
    ) {
        if (overlayRenderPolicy.showServingLine || overlayRenderPolicy.showNeighborLines) {
            MapCoordinateProjector.toDisplaySimMatches(simMatches, state.tilePreset)
        } else {
            emptyList()
        }
    }
    val displayMeasurementState = remember(state.measurementState, state.tilePreset) {
        state.measurementState.copy(
            points = state.measurementState.points.map { point ->
                MapCoordinateProjector.toDisplayPoint(
                    longitude = point.longitude,
                    latitude = point.latitude,
                    tilePreset = state.tilePreset,
                )
            },
        )
    }
    val displayUserPolygonDraft = remember(state.userPolygonDraft, state.tilePreset) {
        state.userPolygonDraft?.let { draft ->
            draft.copy(
                points = draft.points.map { point ->
                    MapCoordinateProjector.toDisplayPoint(
                        longitude = point.longitude,
                        latitude = point.latitude,
                        tilePreset = state.tilePreset,
                    )
                },
            )
        }
    }
    val servingLineSlots = remember(state.simPreferenceStates) {
        state.simPreferenceStates.filter { it.showServingLine }.mapTo(linkedSetOf()) { it.slotIndex }
    }
    val neighborLineSlots = remember(state.simPreferenceStates) {
        state.simPreferenceStates.filter { it.showNeighborLines }.mapTo(linkedSetOf()) { it.slotIndex }
    }

    LaunchedEffect(panelKey, state.tilePreset) {
        selectionCandidates = emptyList()
        detailCandidate = null
    }

    LaunchedEffect(panelKey, state.currentLocation) {
        val fix = state.currentLocation
        if (MapInitialAutoCenterPolicy.shouldAutoCenter(
                currentLocation = fix,
                alreadyAutoCentered = autoCenteredOnFirstLocation,
                userGestureMovedMap = userGestureMovedMap,
            )
        ) {
            pendingTarget = MapCameraTarget(
                longitude = requireNotNull(fix).longitude,
                latitude = fix.latitude,
                zoom = MapInitialAutoCenterPolicy.ZOOM,
            )
            autoCenteredOnFirstLocation = true
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            mapDisposed.value = true
            lifecycle.removeObserver(observer)
            mapView.getMapAsync { map ->
                mapClickListenerRef.value?.let(map::removeOnMapClickListener)
                longClickListenerRef.value?.let(map::removeOnMapLongClickListener)
                cameraIdleListenerRef.value?.let(map::removeOnCameraIdleListener)
                cameraMoveStartedListenerRef.value?.let(map::removeOnCameraMoveStartedListener)
                cameraMoveCanceledListenerRef.value?.let(map::removeOnCameraMoveCancelListener)
            }
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(cameraTargets, panelKey) {
        cameraTargets.collect { target ->
            pendingTarget = target
            // An explicit import/search target must not be replaced by the first delayed GPS fix.
            autoCenteredOnFirstLocation = true
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            val tilePreset = state.tilePreset
            val styleSignature = Triple(panelKey, tilePreset, tileProxyConfig?.port ?: 0)
            // Read Compose state here, not only in MapLibre's potentially deferred callback.
            val readyForSources = sourcesReady
            val targetToApply = pendingTarget
            val currentVectorPayload = compatibleVectors
            view.getMapAsync { map ->
                if (mapDisposed.value || currentTilePresetRef.value != tilePreset) return@getMapAsync
                if (!gestureSettingsAppliedRef.value) {
                    configureMapGesturesForSignalMap(map)
                    gestureSettingsAppliedRef.value = true
                }
                if (cameraIdleListenerRef.value == null) {
                    val listener = MapLibreMap.OnCameraIdleListener {
                        isGestureInteracting = false
                        latestViewportChangeHandler(captureViewport(map, currentTilePresetRef.value))
                    }
                    map.addOnCameraIdleListener(listener)
                    cameraIdleListenerRef.value = listener
                }
                if (cameraMoveStartedListenerRef.value == null) {
                    val listener = MapLibreMap.OnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            isGestureInteracting = true
                            userGestureMovedMap = true
                            map.style?.let { style ->
                                applyMapGestureLayerVisibility(
                                    style = style,
                                    layerVisibility = MapGestureRenderPolicy.resolve(
                                        isGestureInteracting = true,
                                        overlayPolicy = latestOverlayRenderPolicy,
                                    ),
                                )
                            }
                        }
                    }
                    map.addOnCameraMoveStartedListener(listener)
                    cameraMoveStartedListenerRef.value = listener
                }
                if (cameraMoveCanceledListenerRef.value == null) {
                    val listener = MapLibreMap.OnCameraMoveCanceledListener {
                        isGestureInteracting = false
                    }
                    map.addOnCameraMoveCancelListener(listener)
                    cameraMoveCanceledListenerRef.value = listener
                }

                val queryLayerIds = buildList {
                    if (overlayRenderPolicy.showSectorFill) add("sector-fill-layer")
                    if (overlayRenderPolicy.showPointIcons) add("point-icon-layer")
                    if (overlayRenderPolicy.showSitePoints) add("site-circle-layer")
                    if (overlayRenderPolicy.showSiteLabels) add("site-label-layer")
                }
                mapClickListenerRef.value?.let(map::removeOnMapClickListener)
                mapClickListenerRef.value = MapLibreMap.OnMapClickListener { latLng ->
                    if (pendingUserPointDraft != null) {
                        val rawPoint = MapCoordinateProjector.toRawPoint(
                            longitude = latLng.longitude,
                            latitude = latLng.latitude,
                            tilePreset = currentTilePresetRef.value,
                        )
                        onPlacePendingUserPoint(rawPoint.longitude, rawPoint.latitude)
                        return@OnMapClickListener true
                    }
                    if (state.userPolygonDraft != null) {
                        val rawPoint = MapCoordinateProjector.toRawPoint(
                            longitude = latLng.longitude,
                            latitude = latLng.latitude,
                            tilePreset = currentTilePresetRef.value,
                        )
                        onAddUserPolygonPoint(rawPoint.longitude, rawPoint.latitude)
                        return@OnMapClickListener true
                    }
                    if (!state.measurementState.isActive || state.measurementState.isFinished) {
                        return@OnMapClickListener false
                    }
                    if (state.measurementState.canFinish) {
                        val touchPoint = map.projection.toScreenLocation(latLng)
                        val radiusPx = MapFeatureSelectionResolver.selectionRadiusPx(
                            zoom = map.cameraPosition.zoom,
                            density = density.density,
                        )
                        val finishHits = runCatching {
                            map.queryRenderedFeatures(
                                RectF(
                                    touchPoint.x - radiusPx,
                                    touchPoint.y - radiusPx,
                                    touchPoint.x + radiusPx,
                                    touchPoint.y + radiusPx,
                                ),
                                "measurement-end-layer",
                            )
                        }.getOrElse { emptyList() }
                        if (finishHits.isNotEmpty()) {
                            onFinishMeasurement()
                            return@OnMapClickListener true
                        }
                    }
                    val rawPoint = MapCoordinateProjector.toRawPoint(
                        longitude = latLng.longitude,
                        latitude = latLng.latitude,
                        tilePreset = currentTilePresetRef.value,
                    )
                    onAddMeasurementPoint(rawPoint.longitude, rawPoint.latitude)
                    true
                }
                mapClickListenerRef.value?.let(map::addOnMapClickListener)
                longClickListenerRef.value?.let(map::removeOnMapLongClickListener)
                longClickListenerRef.value = MapLibreMap.OnMapLongClickListener { latLng ->
                    if (queryLayerIds.isEmpty()) return@OnMapLongClickListener false
                    val touchPoint = map.projection.toScreenLocation(latLng)
                    val radiusPx = MapFeatureSelectionResolver.selectionRadiusPx(
                        zoom = map.cameraPosition.zoom,
                        density = density.density,
                    )
                    val hits = runCatching {
                        map.queryRenderedFeatures(
                            RectF(
                                touchPoint.x - radiusPx,
                                touchPoint.y - radiusPx,
                                touchPoint.x + radiusPx,
                                touchPoint.y + radiusPx,
                            ),
                            *queryLayerIds.toTypedArray(),
                        )
                    }.getOrElse { emptyList() }
                    val pressPoint = ScreenPoint(touchPoint.x, touchPoint.y)
                    val projector = { longitude: Double, latitude: Double ->
                        val projected = map.projection.toScreenLocation(LatLng(latitude, longitude))
                        ScreenPoint(projected.x, projected.y)
                    }
                    val candidates = MapFeatureSelectionResolver.resolveCandidates(
                        pressPoint = pressPoint,
                        hits = hits,
                        rawRecordsById = rawRenderedById,
                        displayRecordsById = displayRenderedById,
                        projector = projector,
                        isSelectable = { record ->
                            layersById[record.layerId]?.isVisible == true &&
                                MapRecordFilter.matches(record, state.selectedTechs, state.selectedBands)
                        },
                    )
                    when {
                        candidates.isEmpty() -> false
                        candidates.size == 1 -> {
                            selectionCandidates = emptyList()
                            detailCandidate = candidates.first()
                            true
                        }
                        else -> {
                            detailCandidate = null
                            selectionCandidates = candidates
                            true
                        }
                    }
                }
                longClickListenerRef.value?.let(map::addOnMapLongClickListener)
                view.setOnTouchListener { touchedView, event ->
                    if (movingUserPoint == null) {
                        moveTouchStateRef.value = null
                        return@setOnTouchListener false
                    }
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            moveTouchStateRef.value = PointMoveTouchState(
                                downTimeMillis = event.eventTime,
                                dragging = false,
                            )
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val state = moveTouchStateRef.value
                            if (state != null && event.eventTime - state.downTimeMillis >= POINT_MOVE_LONG_PRESS_MILLIS) {
                                moveTouchStateRef.value = state.copy(dragging = true)
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            val state = moveTouchStateRef.value
                            if (state?.dragging == true) {
                                val displayLatLng = map.projection.fromScreenLocation(PointF(event.x, event.y))
                                val rawPoint = MapCoordinateProjector.toRawPoint(
                                    longitude = displayLatLng.longitude,
                                    latitude = displayLatLng.latitude,
                                    tilePreset = currentTilePresetRef.value,
                                )
                                onMoveUserPoint(rawPoint.longitude, rawPoint.latitude)
                            } else {
                                touchedView.performClick()
                            }
                            moveTouchStateRef.value = null
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            moveTouchStateRef.value = null
                            true
                        }

                        else -> true
                    }
                }

                fun updateSources() {
                    if (!readyForSources || !sourcesReady || view.tag != styleSignature) return
                    val style = map.style ?: return
                    if (lastLayerFilterRef.value !== state.layers) {
                        (style.getLayer("sector-fill-layer") as? FillLayer)?.setFilter(visibleLayerFilter)
                        (style.getLayer("sector-line-layer") as? LineLayer)?.setFilter(visibleLayerFilter)
                        (style.getLayer("site-circle-layer") as? CircleLayer)?.setFilter(
                            Expression.all(visibleLayerFilter, Expression.eq(Expression.get("showCircle"), Expression.literal(true))),
                        )
                        (style.getLayer("point-icon-layer") as? SymbolLayer)?.setFilter(
                            Expression.all(visibleLayerFilter, Expression.eq(Expression.get("showIcon"), Expression.literal(true))),
                        )
                        (style.getLayer("site-label-layer") as? SymbolLayer)?.setFilter(
                            Expression.all(visibleLabelFilter, Expression.eq(Expression.get("showLabel"), Expression.literal(true))),
                        )
                        lastLayerFilterRef.value = state.layers
                    }
                    val layerVisibility = MapGestureRenderPolicy.resolve(
                        isGestureInteracting = isGestureInteracting,
                        overlayPolicy = overlayRenderPolicy,
                    )
                    applyMapGestureLayerVisibility(style, layerVisibility)
                    if (currentVectorPayload !== lastVectorSourceStateRef.value) {
                        val sectorSource = style.getSource("sector-source") as? GeoJsonSource ?: return
                        val siteSource = style.getSource("site-source") as? GeoJsonSource ?: return
                        // Strings were serialized off-thread; MapLibre parses them on its worker.
                        sectorSource.setGeoJson(currentVectorPayload.sectorsJson)
                        siteSource.setGeoJson(currentVectorPayload.sitesJson)
                        lastVectorSourceStateRef.value = currentVectorPayload
                    }

                    val rasterState = MapRasterSourceState(
                        signature = visibleRasterLayers.mapNotNull { layer ->
                            layer.rasterInfo?.let { raster ->
                                listOf(
                                    layer.id,
                                    layer.isVisible.toString(),
                                    raster.colorHex,
                                    raster.width.toString(),
                                    raster.height.toString(),
                                    raster.opacity.toString(),
                                    raster.topLeft.longitude.toString(),
                                    raster.topLeft.latitude.toString(),
                                    raster.topRight.longitude.toString(),
                                    raster.topRight.latitude.toString(),
                                    raster.bottomRight.longitude.toString(),
                                    raster.bottomRight.latitude.toString(),
                                    raster.bottomLeft.longitude.toString(),
                                    raster.bottomLeft.latitude.toString(),
                                    currentTilePresetRef.value.key,
                                ).joinToString("|")
                            }
                        },
                    )
                    if (rasterState != lastRasterSourceStateRef.value) {
                        syncRasterLayers(
                            style = style,
                            context = context,
                            tilePreset = currentTilePresetRef.value,
                            rasterLayers = visibleRasterLayers,
                            activeIds = activeRasterLayerIds,
                            imageCache = rasterImageCache,
                        )
                        lastRasterSourceStateRef.value = rasterState
                    }

                    val locationState = MapLocationSourceState(
                        locationKey = displayLocation?.let { "${it.longitude},${it.latitude}" },
                    )
                    if (locationState != lastLocationSourceStateRef.value) {
                        (style.getSource("location-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.locationFeatures(displayLocation),
                        )
                        lastLocationSourceStateRef.value = locationState
                    }

                    val testLogState = MapTestLogSourceState(
                        signature = state.testLogRenderPayload.signature,
                    )
                    if (testLogState != lastTestLogSourceStateRef.value) {
                        (style.getSource("test-log-source") as? GeoJsonSource)?.setGeoJson(
                            state.testLogRenderPayload.geoJson,
                        )
                        lastTestLogSourceStateRef.value = testLogState
                    }

                    val lineState = MapLineSourceState(
                        showServingLine = overlayRenderPolicy.showServingLine,
                        showNeighborLines = overlayRenderPolicy.showNeighborLines,
                        servingVisibleSlots = servingLineSlots.toList(),
                        neighborVisibleSlots = neighborLineSlots.toList(),
                        locationKey = displayLocation?.let { "${it.longitude},${it.latitude}" },
                        servingIds = displaySimMatches.map { it.servingSector?.id },
                        neighborIds = displaySimMatches.flatMap { match -> match.neighborSectors.map { it.id } },
                    )
                    if (lineState != lastLineSourceStateRef.value) {
                        (style.getSource("serving-line-source") as? GeoJsonSource)?.setGeoJson(
                            if (overlayRenderPolicy.showServingLine) {
                                GeoJsonFeatureFactory.servingLineFeatures(
                                    locationFix = displayLocation,
                                    simMatches = displaySimMatches,
                                    visibleSlotIndexes = servingLineSlots,
                                )
                            } else {
                                GeoJsonFeatureFactory.servingLineFeatures(null, emptyList())
                            },
                        )
                        (style.getSource("neighbor-line-source") as? GeoJsonSource)?.setGeoJson(
                            if (overlayRenderPolicy.showNeighborLines) {
                                GeoJsonFeatureFactory.neighborLineFeatures(
                                    locationFix = displayLocation,
                                    simMatches = displaySimMatches,
                                    visibleSlotIndexes = neighborLineSlots,
                                )
                            } else {
                                GeoJsonFeatureFactory.neighborLineFeatures(null, emptyList())
                            },
                        )
                        lastLineSourceStateRef.value = lineState
                    }

                    val measurementState = MapMeasurementSourceState(
                        measurementKey = buildString {
                            append(displayMeasurementState.mode.name)
                            append('|')
                            append(displayMeasurementState.isFinished)
                            append('|')
                            append(displayMeasurementState.distanceMeters?.roundToInt())
                            append('|')
                            append(displayMeasurementState.areaSquareMeters?.roundToInt())
                            append('|')
                            append(
                                displayMeasurementState.points.joinToString(";") { point ->
                                    "${point.longitude},${point.latitude}"
                                },
                            )
                        },
                    )
                    if (measurementState != lastMeasurementSourceStateRef.value) {
                        (style.getSource("measurement-line-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.measurementLineFeatures(displayMeasurementState),
                        )
                        (style.getSource("measurement-fill-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.measurementFillFeatures(displayMeasurementState),
                        )
                        (style.getSource("measurement-point-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.measurementPointFeatures(displayMeasurementState),
                        )
                        (style.getSource("measurement-label-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.measurementLabelFeatures(displayMeasurementState),
                        )
                        (style.getSource("measurement-end-source") as? GeoJsonSource)?.setGeoJson(
                            GeoJsonFeatureFactory.measurementEndHandleFeatures(displayMeasurementState),
                        )
                        lastMeasurementSourceStateRef.value = measurementState
                    }

                    val userDraftState = MapUserDraftSourceState(
                        draftKey = displayUserPolygonDraft?.let { draft ->
                            buildString {
                                append(draft.layerId)
                                append('|')
                                append(draft.name)
                                append('|')
                                append(draft.shapeType.name)
                                append('|')
                                append(
                                    draft.points.joinToString(";") { point ->
                                        "${point.longitude},${point.latitude}"
                                    },
                                )
                            }
                        }.orEmpty(),
                    )
                    if (userDraftState != lastUserDraftSourceStateRef.value) {
                        val draftFeatures = displayUserPolygonDraft?.let { draft ->
                            GeoJsonFeatureFactory.userPolygonDraftFeatures(
                                points = draft.points,
                                name = draft.name,
                                closeShape = draft.shapeType != UserShapeType.LINE,
                            )
                        }
                        (style.getSource("user-draft-fill-source") as? GeoJsonSource)?.setGeoJson(
                            draftFeatures?.fillFeatures ?: FeatureCollection.fromFeatures(emptyList()),
                        )
                        (style.getSource("user-draft-line-source") as? GeoJsonSource)?.setGeoJson(
                            draftFeatures?.lineFeatures ?: FeatureCollection.fromFeatures(emptyList()),
                        )
                        (style.getSource("user-draft-point-source") as? GeoJsonSource)?.setGeoJson(
                            draftFeatures?.pointFeatures ?: FeatureCollection.fromFeatures(emptyList()),
                        )
                        (style.getSource("user-draft-label-source") as? GeoJsonSource)?.setGeoJson(
                            draftFeatures?.labelFeatures ?: FeatureCollection.fromFeatures(emptyList()),
                        )
                        lastUserDraftSourceStateRef.value = userDraftState
                    }
                }

                if (view.tag != styleSignature) {
                    view.tag = styleSignature
                    sourcesReady = false
                    lastLayerFilterRef.value = null
                    activeRasterLayerIds.clear()
                    rasterImageCache.clear()
                    lastVectorSourceStateRef.value = null
                    lastRasterSourceStateRef.value = null
                    lastLocationSourceStateRef.value = null
                    lastLineSourceStateRef.value = null
                    lastTestLogSourceStateRef.value = null
                    lastMeasurementSourceStateRef.value = null
                    lastUserDraftSourceStateRef.value = null
                    map.setStyle(
                        Style.Builder().fromJson(
                            MapStyleFactory.buildStyleJson(
                                tilePreset = tilePreset,
                                proxyConfig = tileProxyConfig,
                            ),
                        ),
                    ) { style ->
                        if (mapDisposed.value || view.tag != styleSignature) return@setStyle
                        SitePointBitmapFactory.createStyleImages(context).forEach { (id, bitmap) ->
                            style.addImage(id, bitmap)
                        }
                        if (style.getSource("sector-source") == null) {
                            style.addSource(GeoJsonSource("sector-source", PreparedMapVectors.EMPTY_GEOJSON, GeoJsonOptions().withSynchronousUpdate(false)))
                            style.addLayer(
                                FillLayer("sector-fill-layer", "sector-source").withProperties(
                                    fillColor(Expression.get("color")),
                                    fillOpacity(0.28f),
                                ),
                            )
                            style.addLayer(
                                LineLayer("sector-line-layer", "sector-source").withProperties(
                                    lineColor(Expression.get("color")),
                                    lineWidth(1.1f),
                                    lineOpacity(0.8f),
                                ),
                            )
                        }
                        if (style.getSource("site-source") == null) {
                            style.addSource(GeoJsonSource("site-source", PreparedMapVectors.EMPTY_GEOJSON, GeoJsonOptions().withSynchronousUpdate(false)))
                            style.addLayer(
                                CircleLayer("site-circle-layer", "site-source").withProperties(
                                    circleColor(Expression.get("color")),
                                    circleRadius(4.2f),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(1.2f),
                                ).withFilter(Expression.eq(Expression.get("showCircle"), Expression.literal(true))),
                            )
                        }
                        if (style.getLayer("point-icon-layer") == null) {
                            style.addLayer(
                                SymbolLayer("point-icon-layer", "site-source").withProperties(
                                    iconImage(Expression.get("iconId")),
                                    iconAnchor(SiteSymbolLayout.POINT_ANCHOR),
                                    iconOffset(arrayOf(0f, 0f)),
                                    iconAllowOverlap(true),
                                    iconIgnorePlacement(true),
                                ).withFilter(Expression.eq(Expression.get("showIcon"), Expression.literal(true))),
                            )
                        }
                        if (style.getLayer("site-label-layer") == null) {
                            style.addLayerBelow(
                                SymbolLayer("site-label-layer", "site-source").withProperties(
                                    textField(Expression.get("title")),
                                    textFont(arrayOf("Noto Sans Regular")),
                                    textSize(11.5f),
                                    textAnchor(SiteSymbolLayout.LABEL_ANCHOR),
                                    textOffset(arrayOf(0f, SiteSymbolLayout.LABEL_OFFSET_Y_DP / 11.5f)),
                                    textColor(Expression.get("labelColor")),
                                    textHaloColor(Expression.get("labelHaloColor")),
                                    textHaloWidth(1.2f),
                                    textMaxWidth(0f),
                                    textAllowOverlap(true),
                                    textIgnorePlacement(true),
                                ).withFilter(Expression.eq(Expression.get("showLabel"), Expression.literal(true))),
                                "point-icon-layer",
                            )
                        }
                        if (style.getSource("test-log-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "test-log-source",
                                    GeoJsonFeatureFactory.testLogPointFeatures(emptyList(), state.testMetricMode),
                                ),
                            )
                            style.addLayer(
                                CircleLayer("test-log-layer-sim0", "test-log-source").withProperties(
                                    circleColor(Expression.get("color")),
                                    circleRadius(3.6f),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(0.8f),
                                ).apply {
                                    setFilter(Expression.eq(Expression.get("slotIndex"), Expression.literal(0)))
                                },
                            )
                            style.addLayer(
                                CircleLayer("test-log-layer-sim1", "test-log-source").withProperties(
                                    circleColor(Expression.get("color")),
                                    circleRadius(3.6f),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(0.8f),
                                    circleTranslate(arrayOf(7f, -7f)),
                                ).apply {
                                    setFilter(Expression.eq(Expression.get("slotIndex"), Expression.literal(1)))
                                },
                            )
                            style.addLayer(
                                CircleLayer("test-log-layer-default", "test-log-source").withProperties(
                                    circleColor(Expression.get("color")),
                                    circleRadius(3.6f),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(0.8f),
                                ).apply {
                                    setFilter(Expression.eq(Expression.get("slotIndex"), Expression.literal(-1)))
                                },
                            )
                        }
                        if (style.getSource("location-source") == null) {
                            style.addSource(GeoJsonSource("location-source", GeoJsonFeatureFactory.locationFeatures(null)))
                            style.addLayer(
                                CircleLayer("location-circle-layer", "location-source").withProperties(
                                    circleColor(Expression.get("color")),
                                    circleRadius(6f),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(2f),
                                ),
                            )
                        }
                        if (style.getSource("serving-line-source") == null) {
                            style.addSource(GeoJsonSource("serving-line-source", GeoJsonFeatureFactory.servingLineFeatures(null, emptyList())))
                            style.addLayer(
                                LineLayer("serving-line-layer", "serving-line-source").withProperties(
                                    lineColor(Expression.get("color")),
                                    lineWidth(3f),
                                    lineOpacity(0.9f),
                                ),
                            )
                        }
                        if (style.getSource("neighbor-line-source") == null) {
                            style.addSource(GeoJsonSource("neighbor-line-source", GeoJsonFeatureFactory.neighborLineFeatures(null, emptyList())))
                            style.addLayer(
                                LineLayer("neighbor-line-layer", "neighbor-line-source").withProperties(
                                    lineColor(Expression.get("color")),
                                    lineWidth(2.2f),
                                    lineOpacity(0.85f),
                                ),
                            )
                        }
                        if (style.getSource("measurement-fill-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "measurement-fill-source",
                                    GeoJsonFeatureFactory.measurementFillFeatures(displayMeasurementState),
                                ),
                            )
                            style.addLayer(
                                FillLayer("measurement-fill-layer", "measurement-fill-source").withProperties(
                                    fillColor("#1D9BF0"),
                                    fillOpacity(0.16f),
                                ),
                            )
                        }
                        if (style.getSource("measurement-line-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "measurement-line-source",
                                    GeoJsonFeatureFactory.measurementLineFeatures(displayMeasurementState),
                                ),
                            )
                            style.addLayer(
                                LineLayer("measurement-line-layer", "measurement-line-source").withProperties(
                                    lineColor("#1D9BF0"),
                                    lineWidth(2.6f),
                                    lineOpacity(0.92f),
                                ),
                            )
                        }
                        if (style.getSource("measurement-point-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "measurement-point-source",
                                    GeoJsonFeatureFactory.measurementPointFeatures(displayMeasurementState),
                                ),
                            )
                            style.addLayer(
                                CircleLayer("measurement-point-layer", "measurement-point-source").withProperties(
                                    circleColor("#FFFFFF"),
                                    circleRadius(4.4f),
                                    circleStrokeColor("#1D9BF0"),
                                    circleStrokeWidth(2f),
                                ),
                            )
                        }
                        if (style.getSource("measurement-label-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "measurement-label-source",
                                    GeoJsonFeatureFactory.measurementLabelFeatures(displayMeasurementState),
                                ),
                            )
                            style.addLayer(
                                SymbolLayer("measurement-label-layer", "measurement-label-source").withProperties(
                                    textField(Expression.get("title")),
                                    textFont(arrayOf("Noto Sans Regular")),
                                    textSize(12f),
                                    textColor("#FFFFFF"),
                                    textHaloColor("#111111"),
                                    textHaloWidth(1.2f),
                                    textAllowOverlap(true),
                                    textIgnorePlacement(true),
                                    textOffset(arrayOf(0f, -1.1f)),
                                ),
                            )
                        }
                        if (style.getSource("measurement-end-source") == null) {
                            style.addSource(
                                GeoJsonSource(
                                    "measurement-end-source",
                                    GeoJsonFeatureFactory.measurementEndHandleFeatures(displayMeasurementState),
                                ),
                            )
                            style.addLayer(
                                SymbolLayer("measurement-end-layer", "measurement-end-source").withProperties(
                                    textField(Expression.get("title")),
                                    textFont(arrayOf("Noto Sans Regular")),
                                    textSize(12f),
                                    textColor("#FFF7E6"),
                                    textHaloColor("#7C2D12"),
                                    textHaloWidth(1.1f),
                                    textAllowOverlap(true),
                                    textIgnorePlacement(true),
                                    textOffset(arrayOf(1.1f, -0.2f)),
                                ),
                            )
                        }
                        if (style.getSource("user-draft-fill-source") == null) {
                            style.addSource(GeoJsonSource("user-draft-fill-source", FeatureCollection.fromFeatures(emptyList())))
                            style.addLayer(
                                FillLayer("user-draft-fill-layer", "user-draft-fill-source").withProperties(
                                    fillColor("#FF7A00"),
                                    fillOpacity(0.14f),
                                ),
                            )
                        }
                        if (style.getSource("user-draft-line-source") == null) {
                            style.addSource(GeoJsonSource("user-draft-line-source", FeatureCollection.fromFeatures(emptyList())))
                            style.addLayer(
                                LineLayer("user-draft-line-layer", "user-draft-line-source").withProperties(
                                    lineColor("#FF7A00"),
                                    lineWidth(2.8f),
                                    lineOpacity(0.95f),
                                ),
                            )
                        }
                        if (style.getSource("user-draft-point-source") == null) {
                            style.addSource(GeoJsonSource("user-draft-point-source", FeatureCollection.fromFeatures(emptyList())))
                            style.addLayer(
                                CircleLayer("user-draft-point-layer", "user-draft-point-source").withProperties(
                                    circleColor("#FFFFFF"),
                                    circleRadius(4.8f),
                                    circleStrokeColor("#FF7A00"),
                                    circleStrokeWidth(2f),
                                ),
                            )
                        }
                        if (style.getSource("user-draft-label-source") == null) {
                            style.addSource(GeoJsonSource("user-draft-label-source", FeatureCollection.fromFeatures(emptyList())))
                            style.addLayer(
                                SymbolLayer("user-draft-label-layer", "user-draft-label-source").withProperties(
                                    textField(Expression.get("title")),
                                    textFont(arrayOf("Noto Sans Regular")),
                                    textSize(12f),
                                    textColor("#FFF7E6"),
                                    textHaloColor("#7C2D12"),
                                    textHaloWidth(1.2f),
                                    textAllowOverlap(true),
                                    textIgnorePlacement(true),
                                    textOffset(arrayOf(0f, -1.1f)),
                                ),
                            )
                        }
                        sourcesReady = true
                        // Trigger AndroidView's latest update; this callback may predate an import.
                        if (pendingTarget == null) {
                            val initialPoint = MapCoordinateProjector.toDisplayPoint(
                                longitude = activeViewport.centerLongitude,
                                latitude = activeViewport.centerLatitude,
                                tilePreset = currentTilePresetRef.value,
                            )
                            val initialTarget = LatLng(initialPoint.latitude, initialPoint.longitude)
                            map.cameraPosition = CameraPosition.Builder()
                                .target(initialTarget)
                                .zoom(activeViewport.zoom)
                                .build()
                            latestViewportChangeHandler(activeViewport)
                        }
                    }
                } else {
                    updateSources()
                }

                targetToApply?.takeIf { readyForSources && sourcesReady && it == pendingTarget }?.let { target ->
                    val displayTarget = MapCoordinateProjector.toDisplayPoint(
                        longitude = target.longitude,
                        latitude = target.latitude,
                        tilePreset = currentTilePresetRef.value,
                    )
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(displayTarget.latitude, displayTarget.longitude),
                            target.zoom,
                        ),
                    )
                    if (pendingTarget == target) pendingTarget = null
                }
            }
        },
    )

    if (selectionCandidates.isNotEmpty()) {
        MapSelectionCandidatesSheet(
            candidates = selectionCandidates,
            layersById = layersById,
            onDismiss = { selectionCandidates = emptyList() },
            onSelect = { candidate ->
                selectionCandidates = emptyList()
                detailCandidate = candidate
            },
        )
    }

    detailCandidate?.let { candidate ->
        val layer = layersById[candidate.record.layerId] ?: fallbackLayerMetadata(candidate.record)
        MapFeatureDetailsSheet(
            title = candidateTitle(candidate.record),
            subtitle = candidateSubtitle(candidate, layersById),
            rows = MapFeatureDetailsFormatter.build(candidate.record, layer),
            record = candidate.record,
            layer = layer,
            onUpdateUserRecord = onUpdateUserRecord,
            onDeleteUserRecord = onDeleteUserRecord,
            onEditUserPolygon = onEditUserPolygon,
            onStartMoveUserPoint = { onStartMoveUserPoint(candidate) },
            onDismiss = { detailCandidate = null },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MapSelectionCandidatesSheet(
    candidates: List<MapSelectionCandidate>,
    layersById: Map<String, LayerMetadata>,
    onDismiss: () -> Unit,
    onSelect: (MapSelectionCandidate) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 标题区域
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CellTower,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "附近对象",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "按最近命中排序，点一下查看详情",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(candidates, key = { it.record.id }) { candidate ->
                    val techColor = SignalColors.forTechnology(candidate.record.radioTechnology)
                    Surface(
                        onClick = { onSelect(candidate) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.xywy.cardBorder.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 左侧制式色点
                            StatusDot(color = techColor, size = 10.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = candidateTitle(candidate.record),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = candidateSubtitle(candidate, layersById),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // 右侧距离
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "${candidate.distancePx.roundToInt()}px",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MapFeatureDetailsSheet(
    title: String,
    subtitle: String,
    rows: List<MapFeatureDetailRow>,
    record: SectorRecord,
    layer: LayerMetadata,
    onUpdateUserRecord: (String, String, String, Double?, Double?, String, LayerPointIconPreset?) -> Unit,
    onDeleteUserRecord: (String, String) -> Unit,
    onEditUserPolygon: (String, String) -> Unit,
    onStartMoveUserPoint: () -> Unit,
    onDismiss: () -> Unit,
) {
    var editName by remember(record.id) { mutableStateOf(candidateTitle(record)) }
    var editIconPreset by remember(record.id) { mutableStateOf(recordIconPreset(record)) }
    var editLongitudeText by remember(record.id) { mutableStateOf(String.format(Locale.US, "%.6f", record.longitude)) }
    var editLatitudeText by remember(record.id) { mutableStateOf(String.format(Locale.US, "%.6f", record.latitude)) }
    var editRemark by remember(record.id) { mutableStateOf(record.rawFields["备注"] ?: record.rawFields["remark"].orEmpty()) }
    var showEditPanel by remember(record.id) { mutableStateOf(false) }
    val canEdit = layer.userEditable
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 标题区域
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CellTower,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 38.dp),
                        )
                    }
                    if (canEdit) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 38.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(onClick = { showEditPanel = !showEditPanel }) {
                                Text(if (showEditPanel) "收起编辑" else "编辑")
                            }
                            TextButton(
                                onClick = {
                                    onDeleteUserRecord(record.layerId, record.id)
                                    onDismiss()
                                },
                            ) {
                                Text("删除")
                            }
                            if (!record.isPointOnly && record.polygonCoordinates.size >= 2) {
                                TextButton(
                                    onClick = {
                                        onEditUserPolygon(record.layerId, record.id)
                                        onDismiss()
                                    },
                                ) {
                                    Text(if (record.isLineOnly) "编辑线" else "编辑多边形")
                                }
                            }
                            if (record.isPointOnly) {
                                TextButton(
                                    onClick = {
                                        onStartMoveUserPoint()
                                        onDismiss()
                                    },
                                ) {
                                    Text("移动点")
                                }
                            }
                        }
                    }
                }
            }

            if (canEdit && showEditPanel) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("名称") },
                            singleLine = true,
                        )
                        if (record.isPointOnly) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editLongitudeText,
                                    onValueChange = { editLongitudeText = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("经度") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = editLatitudeText,
                                    onValueChange = { editLatitudeText = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("纬度") },
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = editRemark,
                                onValueChange = { editRemark = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("备注") },
                                minLines = 1,
                                maxLines = 3,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(LayerPointIconPreset.entries, key = { it.name }) { preset ->
                                    Surface(
                                        onClick = { editIconPreset = preset },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (editIconPreset == preset) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        },
                                    ) {
                                        Text(
                                            preset.title,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                onUpdateUserRecord(
                                    record.layerId,
                                    record.id,
                                    editName,
                                    editLongitudeText.toDoubleOrNull(),
                                    editLatitudeText.toDoubleOrNull(),
                                    editRemark,
                                    editIconPreset.takeIf { record.isPointOnly },
                                )
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存修改")
                        }
                    }
                }
            }

            // 详情行 - 每两行一组用网格布局
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(rows.chunked(2)) { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowPair.forEach { row ->
                        DetailRowCard(
                            row = row,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DetailRowCard(
    row: MapFeatureDetailRow,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun candidateTitle(record: SectorRecord): String =
    record.cellName ?: record.siteName ?: record.cellId ?: record.siteId ?: record.id

private fun candidateSubtitle(
    candidate: MapSelectionCandidate,
    layersById: Map<String, LayerMetadata>,
): String {
    val record = candidate.record
    return listOfNotNull(
        layersById[record.layerId]?.name,
        record.radioTechnology.label.takeIf { record.radioTechnology != RadioTechnology.UNKNOWN },
        record.band?.takeIf { it.isNotBlank() },
        if (record.isPointOnly) "点位" else "扇区",
    ).joinToString(" · ")
}

private fun fallbackLayerMetadata(record: SectorRecord): LayerMetadata = LayerMetadata(
    id = record.layerId,
    name = "已导入图层",
    sourceFileName = "",
    importedAtMillis = 0L,
    isVisible = true,
    recordCount = 1,
)

private data class MapLocationSourceState(
    val locationKey: String?,
)

private data class MapRasterSourceState(
    val signature: List<String>,
)

private data class MapLineSourceState(
    val showServingLine: Boolean,
    val showNeighborLines: Boolean,
    val servingVisibleSlots: List<Int>,
    val neighborVisibleSlots: List<Int>,
    val locationKey: String?,
    val servingIds: List<String?>,
    val neighborIds: List<String>,
)

private data class MapTestLogSourceState(
    val signature: String,
)

private data class MapMeasurementSourceState(
    val measurementKey: String,
)

private data class MapUserDraftSourceState(
    val draftKey: String,
)

private data class PointMoveTouchState(
    val downTimeMillis: Long,
    val dragging: Boolean,
)

private data class PendingUserPointDraft(
    val layerId: String,
    val name: String,
    val remark: String,
    val iconPreset: LayerPointIconPreset,
)

private const val POINT_MOVE_LONG_PRESS_MILLIS = 350L

private fun configureMapGesturesForSignalMap(map: MapLibreMap) {
    map.uiSettings.apply {
        setRotateGesturesEnabled(false)
        setTiltGesturesEnabled(false)
        setDisableRotateWhenScaling(true)
        setIncreaseScaleThresholdWhenRotating(true)
    }
}

private fun applyMapGestureLayerVisibility(
    style: Style,
    layerVisibility: MapGestureLayerVisibility,
) {
    style.setLayerVisibility(layerVisibility.sector, "sector-fill-layer", "sector-line-layer")
    style.setLayerVisibility(layerVisibility.sitePoints, "site-circle-layer")
    style.setLayerVisibility(layerVisibility.pointIcons, "point-icon-layer")
    style.setLayerVisibility(layerVisibility.siteLabels, "site-label-layer")
    style.setLayerVisibility(
        layerVisibility.testLog,
        "test-log-layer-sim0",
        "test-log-layer-sim1",
        "test-log-layer-default",
    )
    style.setLayerVisibility(layerVisibility.servingLine, "serving-line-layer")
    style.setLayerVisibility(layerVisibility.neighborLine, "neighbor-line-layer")
    style.setLayerVisibility(
        layerVisibility.measurement,
        "measurement-fill-layer",
        "measurement-line-layer",
        "measurement-point-layer",
        "measurement-label-layer",
        "measurement-end-layer",
    )
    style.setLayerVisibility(
        layerVisibility.userDraft,
        "user-draft-fill-layer",
        "user-draft-line-layer",
        "user-draft-point-layer",
        "user-draft-label-layer",
    )
}

private fun Style.setLayerVisibility(
    visible: Boolean,
    vararg layerIds: String,
) {
    val value = if (visible) VISIBLE else NONE
    layerIds.forEach { layerId ->
        getLayer(layerId)?.setProperties(visibility(value))
    }
}

private fun captureViewport(
    map: org.maplibre.android.maps.MapLibreMap,
    tilePreset: com.xywy.signalmap.domain.model.TilePreset,
): MapViewportSnapshot {
    val target = map.cameraPosition.target ?: return MapViewportSnapshot(
        centerLongitude = 0.0,
        centerLatitude = 0.0,
        zoom = map.cameraPosition.zoom,
        bounds = null,
    )
    val bounds = runCatching { map.projection.visibleRegion.latLngBounds }.getOrNull()
    val rawTarget = MapCoordinateProjector.toRawPoint(
        longitude = target.longitude,
        latitude = target.latitude,
        tilePreset = tilePreset,
    )
    return MapViewportSnapshot(
        centerLongitude = rawTarget.longitude,
        centerLatitude = rawTarget.latitude,
        zoom = map.cameraPosition.zoom,
        bounds = bounds?.let {
            val southWest = MapCoordinateProjector.toRawPoint(
                longitude = it.longitudeWest,
                latitude = it.latitudeSouth,
                tilePreset = tilePreset,
            )
            val northEast = MapCoordinateProjector.toRawPoint(
                longitude = it.longitudeEast,
                latitude = it.latitudeNorth,
                tilePreset = tilePreset,
            )
            GeoBounds(
                minLongitude = minOf(southWest.longitude, northEast.longitude),
                maxLongitude = maxOf(southWest.longitude, northEast.longitude),
                minLatitude = minOf(southWest.latitude, northEast.latitude),
                maxLatitude = maxOf(southWest.latitude, northEast.latitude),
            )
        },
    )
}

private fun syncRasterLayers(
    style: Style,
    context: android.content.Context,
    tilePreset: com.xywy.signalmap.domain.model.TilePreset,
    rasterLayers: List<LayerMetadata>,
    activeIds: MutableSet<String>,
    imageCache: RasterLayerImageCache<android.graphics.Bitmap>,
) {
    val layersRoot = File(context.filesDir, "layers")
    val nextIds = linkedSetOf<String>()
    rasterLayers.forEach { layer ->
        val raster = layer.rasterInfo ?: return@forEach
        val renderFile = RasterLayerAssetPaths.renderFile(layersRoot, layer.id)
        if (!renderFile.exists()) return@forEach
        val bitmap = imageCache.get(renderFile, contentRevision = raster) ?: return@forEach
        val quad = LatLngQuad(
            raster.topLeft.toDisplayLatLng(tilePreset),
            raster.topRight.toDisplayLatLng(tilePreset),
            raster.bottomRight.toDisplayLatLng(tilePreset),
            raster.bottomLeft.toDisplayLatLng(tilePreset),
        )
        val sourceId = rasterSourceId(layer.id)
        val layerId = rasterLayerId(layer.id)
        nextIds += layer.id
        val existingSource = style.getSource(sourceId) as? ImageSource
        if (existingSource == null) {
            style.addSource(ImageSource(sourceId, quad, bitmap))
            style.addLayerBelow(
                RasterLayer(layerId, sourceId).withProperties(
                    rasterOpacity(1f),
                    rasterResampling(RASTER_RESAMPLING_NEAREST),
                ),
                "sector-fill-layer",
            )
        } else {
            existingSource.setCoordinates(quad)
            existingSource.setImage(bitmap)
            style.getLayer(layerId)?.setProperties(
                rasterOpacity(1f),
                rasterResampling(RASTER_RESAMPLING_NEAREST),
            )
        }
    }
    (activeIds - nextIds).forEach { staleId ->
        if (style.getLayer(rasterLayerId(staleId)) != null) {
            style.removeLayer(rasterLayerId(staleId))
        }
        if (style.getSource(rasterSourceId(staleId)) != null) {
            style.removeSource(rasterSourceId(staleId))
        }
    }
    activeIds.clear()
    activeIds += nextIds
}

private fun GeoPoint.toDisplayLatLng(tilePreset: com.xywy.signalmap.domain.model.TilePreset): LatLng {
    val point = MapCoordinateProjector.toDisplayPoint(
        longitude = longitude,
        latitude = latitude,
        tilePreset = tilePreset,
    )
    return LatLng(point.latitude, point.longitude)
}

private fun rasterSourceId(layerId: String): String = "raster-source-$layerId"

private fun rasterLayerId(layerId: String): String = "raster-layer-$layerId"

private fun recordIconPreset(record: SectorRecord): LayerPointIconPreset =
    record.rawFields["icon"]
        ?.let { icon ->
            LayerPointIconPreset.entries.firstOrNull { preset ->
                preset.name.equals(icon, ignoreCase = true) ||
                    preset.iconId.equals(icon, ignoreCase = true) ||
                    preset.title == icon
            }
        }
        ?: LayerPointIconPreset.RED_PIN
