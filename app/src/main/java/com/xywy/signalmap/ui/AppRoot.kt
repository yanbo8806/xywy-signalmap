package com.xywy.signalmap.ui

import android.content.Intent
import com.xywy.signalmap.data.importer.ImportFileMetadata
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xywy.signalmap.MainViewModel
import com.xywy.signalmap.XYWY_LOG_MIME
import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import com.xywy.signalmap.ui.cell.CellScreen
import com.xywy.signalmap.ui.map.MapPanelExpansionState
import com.xywy.signalmap.ui.map.MapScreen
import com.xywy.signalmap.ui.map.MapViewportSnapshot
import com.xywy.signalmap.ui.profile.ImportConfigDialog
import com.xywy.signalmap.ui.profile.ProfileScreen
import com.xywy.signalmap.ui.profile.LogReplayDialog
import com.xywy.signalmap.ui.profile.ReplayMapControls
import com.xywy.signalmap.ui.theme.xywy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf

internal data class AppTabHostPlan(
    val keepMapMounted: Boolean,
    val overlayTab: AppTab?,
    val collectMapSensorHud: Boolean,
) {
    companion object {
        fun from(currentTab: AppTab): AppTabHostPlan = when (currentTab) {
            AppTab.Map -> AppTabHostPlan(
                keepMapMounted = true,
                overlayTab = null,
                collectMapSensorHud = true,
            )
            AppTab.Cells -> AppTabHostPlan(
                keepMapMounted = true,
                overlayTab = AppTab.Cells,
                collectMapSensorHud = false,
            )
            AppTab.Profile -> AppTabHostPlan(
                keepMapMounted = true,
                overlayTab = AppTab.Profile,
                collectMapSensorHud = false,
            )
        }
    }
}

enum class AppTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    Map("基站地图", Icons.Filled.Map, Icons.Outlined.Map),
    Cells("占用小区", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Profile("我的", Icons.Filled.Person, Icons.Outlined.Person),
}

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preparedVectors by viewModel.preparedMapVectors.collectAsStateWithLifecycle()
    val liveEvents by viewModel.liveNetworkEvents.collectAsStateWithLifecycle()
    val replay by viewModel.logReplay.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(AppTab.Map) }
    val tabHostPlan = remember(currentTab) { AppTabHostPlan.from(currentTab) }
    val emptyDeviceSensorFlow = remember { flowOf(DeviceSensorSnapshot.Empty) }
    var mapPanelExpansionState by rememberSaveable(stateSaver = MapPanelExpansionState.Saver) {
        mutableStateOf(MapPanelExpansionState())
    }
    val mapViewportSnapshots = remember { mutableStateMapOf<String, MapViewportSnapshot>() }

    BackHandler(enabled = currentTab != AppTab.Map) { currentTab = AppTab.Map }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPermissionState()
    }
    val requestedPermissions = remember {
        arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE,
        )
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val displayName = ImportFileMetadata.displayName(context.contentResolver, uri)
        viewModel.prepareImportFile(
            context.contentResolver,
            uri,
            displayName,
            ImportFileMetadata.mimeType(context.contentResolver, uri),
        )
    }

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeStatusMessage()
    }

    LaunchedEffect(viewModel) {
        viewModel.shareFileRequests.collectLatest { request ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = request.mimeType
                putExtra(Intent.EXTRA_STREAM, request.uri)
                putExtra(Intent.EXTRA_SUBJECT, request.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享测试记录"))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // 悬浮式导航坞：圆角胶囊 + 描边 + 大投影，地图从底部透出
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.xywy.cardBorder.copy(alpha = 0.6f),
                    ),
                    shadowElevation = 18.dp,
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(23.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                ),
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (tabHostPlan.keepMapMounted) {
                MapScreen(
                    state = state,
                    preparedVectors = preparedVectors,
                    onRenderCenterChanged = viewModel::updateMapRenderCenter,
                    deviceSensorSnapshotFlow = if (tabHostPlan.collectMapSensorHud) {
                        viewModel.deviceSensorSnapshotFlow
                    } else {
                        emptyDeviceSensorFlow
                    },
                    cameraTargets = viewModel.cameraTargets,
                    viewportSnapshots = mapViewportSnapshots,
                    paddingValues = paddingValues,
                    onSearchQueryChanged = viewModel::updateSearchQuery,
                    onSubmitSearchQuery = viewModel::submitSearchQuery,
                    onJumpToCoordinate = viewModel::jumpToCoordinate,
                    onFocusSector = viewModel::focusSector,
                    onCenterLocation = viewModel::centerOnCurrentLocation,
                    onToggleTechnology = viewModel::toggleTechnology,
                    onToggleBand = viewModel::toggleBand,
                    onToggleShowSectorFill = viewModel::toggleShowSectorFill,
                    onToggleShowSitePoints = viewModel::toggleShowSitePoints,
                    onToggleShowSiteLabels = viewModel::toggleShowSiteLabels,
                    onToggleShowServingLine = viewModel::toggleShowServingLine,
                    onToggleShowNeighborLines = viewModel::toggleShowNeighborLines,
                    onToggleSimServingLine = viewModel::toggleSimServingLine,
                    onToggleSimNeighborLines = viewModel::toggleSimNeighborLines,
                    onToggleMapDisplayMode = viewModel::toggleMapDisplayMode,
                    panelExpansionState = mapPanelExpansionState,
                    onSearchExpandedChange = { expanded ->
                        mapPanelExpansionState = mapPanelExpansionState.withSearchExpanded(expanded)
                    },
                    onFilterExpandedChange = { expanded ->
                        mapPanelExpansionState = mapPanelExpansionState.withFilterExpanded(expanded)
                    },
                    onTestRecordExpandedChange = { expanded ->
                        mapPanelExpansionState = mapPanelExpansionState.withTestRecordExpanded(expanded)
                    },
                    onToolExpandedChange = { expanded ->
                        mapPanelExpansionState = mapPanelExpansionState.withToolExpanded(expanded)
                    },
                    onCreateExpandedChange = { expanded ->
                        mapPanelExpansionState = mapPanelExpansionState.withCreateExpanded(expanded)
                    },
                    onCreateUserLayer = viewModel::createUserLayer,
                    onAddUserPoint = viewModel::addUserPointAtMapCenter,
                    onStartUserLine = viewModel::startUserLine,
                    onStartUserPolygon = viewModel::startUserPolygon,
                    onAddUserPolygonPoint = viewModel::addUserPolygonPoint,
                    onUndoUserPolygonPoint = viewModel::undoUserPolygonPoint,
                    onFinishUserPolygon = viewModel::finishUserPolygon,
                    onCancelUserPolygon = viewModel::cancelUserPolygon,
                    onUpdateUserRecord = viewModel::updateUserRecord,
                    onDeleteUserRecord = viewModel::deleteUserRecord,
                    onEditUserPolygon = viewModel::startEditUserPolygon,
                    onMoveUserPoint = viewModel::moveUserPoint,
                    onStartTestRecording = viewModel::startTestRecording,
                    onStopTestRecording = viewModel::stopTestRecording,
                    onToggleTestMetricMode = viewModel::toggleTestMetricMode,
                    onToggleSimTestPoints = viewModel::toggleSimTestPoints,
                    onStartDistanceMeasurement = viewModel::startDistanceMeasurement,
                    onStartAreaMeasurement = viewModel::startAreaMeasurement,
                    onAddMeasurementPoint = viewModel::addMeasurementPoint,
                    onFinishMeasurement = viewModel::finishMeasurement,
                    onClearMeasurement = viewModel::clearMeasurement,
                    onViewportSnapshotChanged = { panelKey, snapshot ->
                        if (mapViewportSnapshots[panelKey] != snapshot) {
                            mapViewportSnapshots[panelKey] = snapshot
                        }
                    },
                    onRequestPermissions = {
                        permissionLauncher.launch(requestedPermissions)
                    },
                )
            }

            AppTabOverlay(tab = tabHostPlan.overlayTab) { overlayTab ->
                when (overlayTab) {
                    AppTab.Cells -> {
                        CellScreen(
                            state = state,
                            diagnostics = liveEvents,
                            paddingValues = paddingValues,
                            onRequestPermissions = {
                                permissionLauncher.launch(requestedPermissions)
                            },
                        )
                    }

                    AppTab.Profile -> {
                        ProfileScreen(
                            state = state,
                            paddingValues = paddingValues,
                            onRequestPermissions = {
                                permissionLauncher.launch(requestedPermissions)
                            },
                            importGcj02ToWgs84 = state.importGcj02ToWgs84,
                            isImporting = state.isImporting,
                            importingFileName = state.importingFileName,
                            onImportGcj02ToWgs84Changed = viewModel::setImportGcj02ToWgs84,
                            onImportClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.google-earth.kml+xml",
                                        "image/tiff",
                                        "image/x-tiff",
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        XYWY_LOG_MIME,
                                        "application/vnd.xywy.log",
                                        "application/xml",
                                        "text/xml",
                                    ),
                                )
                            },
                            onToggleLayerVisibility = viewModel::toggleLayerVisibility,
                            onSetLayerAlwaysRenderAllRecords = viewModel::setLayerAlwaysRenderAllRecords,
                            onNearbyRecordLimitChanged = viewModel::setNearbyRecordLimit,
                            onDeleteLayer = viewModel::deleteLayer,
                            onUpdateLayerStyle = viewModel::updateLayerStyle,
                            onExportLayerCsv = viewModel::exportLayerCsv,
                            onExportLayerKml = viewModel::exportLayerKml,
                            onUpdateSimEngineeringBinding = viewModel::updateSimEngineeringBinding,
                            onToggleTestLogVisibility = viewModel::toggleTestLogVisibility,
                            onDeleteTestLog = viewModel::deleteTestLog,
                            onShareTestLog = viewModel::shareTestLog,
                            onReplayTestLog = viewModel::openLogReplay,
                            onTilePresetSelected = viewModel::setTilePreset,
                            onSaveCustomTileSource = viewModel::saveCustomTileSource,
                            onDeleteCustomTileSource = viewModel::deleteCustomTileSource,
                        )
                    }

                    else -> Unit
                }
            }
            replay?.takeIf { it.mapMode && currentTab == AppTab.Map }?.let { playback ->
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = paddingValues.calculateBottomPadding() + 8.dp, start = 12.dp, end = 12.dp)) {
                    ReplayMapControls(playback, viewModel::seekLogReplay, viewModel::toggleLogReplay, viewModel::showReplayAnalysis, viewModel::closeLogReplay)
                }
            }
        }

        replay?.takeIf { !it.mapMode }?.let { playback ->
            LogReplayDialog(playback, viewModel::closeLogReplay, viewModel::seekLogReplay, viewModel::toggleLogReplay) {
                currentTab = AppTab.Map
                viewModel.locateReplaySample()
            }
        }
        state.importDialog?.let { dialog ->
            ImportConfigDialog(
                state = dialog,
                onDismiss = viewModel::dismissImportDialog,
                onConfirm = { viewModel.confirmImportDialog(context.contentResolver) },
                onSheetSelected = viewModel::selectImportSheet,
                onRenderModeSelected = viewModel::updateImportRenderMode,
                onFieldSelected = viewModel::updateImportField,
                onToggleDetailHeader = viewModel::toggleImportDetailHeader,
            )
        }
    }
}
