package com.xywy.signalmap

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xywy.signalmap.data.importer.EngineeringParamImporter
import com.xywy.signalmap.data.importer.ImportFieldMapping
import com.xywy.signalmap.data.importer.ImportFieldSuggester
import com.xywy.signalmap.data.importer.ImportFileFormat
import com.xywy.signalmap.data.importer.ImportFileFormatResolver
import com.xywy.signalmap.data.importer.ImportOptions
import com.xywy.signalmap.data.importer.ImportSheetConfig
import com.xywy.signalmap.data.importer.ParsedSheetPreview
import com.xywy.signalmap.data.exporter.UserLayerExportCodec
import com.xywy.signalmap.data.local.LayerStore
import com.xywy.signalmap.data.local.TestLogStore
import com.xywy.signalmap.data.location.LocationRepository
import com.xywy.signalmap.data.sensor.DeviceSensorRepository
import com.xywy.signalmap.data.settings.SettingsStore
import com.xywy.signalmap.data.telephony.TelephonyRepository
import com.xywy.signalmap.domain.cell.MapDisplayPlanner
import com.xywy.signalmap.domain.cell.MapPanelPlan
import com.xywy.signalmap.domain.cell.CellSnapshotContentComparator
import com.xywy.signalmap.domain.cell.SectorMatchIndex
import com.xywy.signalmap.domain.cell.EngineeringCatalogSummary
import com.xywy.signalmap.domain.cell.sameSectorLists
import com.xywy.signalmap.domain.cell.SimSignalHistory
import com.xywy.signalmap.domain.cell.SimSignalHistoryTracker
import com.xywy.signalmap.domain.cell.SimCellMatcher
import com.xywy.signalmap.domain.cell.TestLogSampleFactory
import com.xywy.signalmap.domain.cell.NetworkEventDetector
import com.xywy.signalmap.domain.cell.LiveNetworkEvents
import com.xywy.signalmap.domain.cell.LogReplayState
import com.xywy.signalmap.domain.cell.TestLogAnalysis
import com.xywy.signalmap.domain.geo.CoordinateParser
import com.xywy.signalmap.domain.model.CellSnapshot
import com.xywy.signalmap.domain.model.CustomTileSourceConfig
import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import com.xywy.signalmap.domain.model.EngineeringLayerBinding
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.MapControlPreferences
import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.RasterLayerInfo
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimOverlayPreferenceState
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.SimEngineeringBindings
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import com.xywy.signalmap.data.tile.TileSourceCatalog
import com.xywy.signalmap.domain.model.defaultMapControlPreferences
import com.xywy.signalmap.domain.map.MapLocationRefreshPolicy
import com.xywy.signalmap.domain.map.MapImportFocusPlanner
import com.xywy.signalmap.domain.map.MapRecordFilter
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.map.MapMeasurementController
import com.xywy.signalmap.domain.map.MapMeasurementState
import com.xywy.signalmap.domain.map.SectorSpatialIndex
import com.xywy.signalmap.domain.search.SearchResultMatcher
import com.xywy.signalmap.map.TestLogMapRenderPayload
import com.xywy.signalmap.map.TestLogMapRenderPayloadFactory
import com.xywy.signalmap.map.PanelMapVectorPreparer
import com.xywy.signalmap.map.MapVectorRenderRequest
import com.xywy.signalmap.map.PreparedMapVectors
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val appContext: Context,
    private val settingsStore: SettingsStore,
    private val layerStore: LayerStore,
    private val testLogStore: TestLogStore,
    private val engineeringParamImporter: EngineeringParamImporter,
    private val locationRepository: LocationRepository,
    private val telephonyRepository: TelephonyRepository,
    private val deviceSensorRepository: DeviceSensorRepository,
) : ViewModel() {
    private val initialMapControls = defaultMapControlPreferences()
    private val selectedTechs = MutableStateFlow(
        initialMapControls.selectedTechs,
    )
    private val selectedBands = MutableStateFlow(initialMapControls.selectedBands)
    private val searchDraftQuery = MutableStateFlow("")
    private val submittedSearchQuery = MutableStateFlow("")
    private val showSectorFill = MutableStateFlow(initialMapControls.showSectorFill)
    private val showSitePoints = MutableStateFlow(initialMapControls.showSitePoints)
    private val showSiteLabels = MutableStateFlow(initialMapControls.showSiteLabels)
    private val nearbyRecordLimit = MutableStateFlow(initialMapControls.nearbyRecordLimit)
    private val mapRenderCenters = MutableStateFlow<Map<String, GeoPoint>>(emptyMap())
    private val panelVectorPreparer = PanelMapVectorPreparer()
    private val showServingLine = MutableStateFlow(true)
    private val showNeighborLines = MutableStateFlow(true)
    private val simOverlayPreferences = MutableStateFlow(initialMapControls.simOverlayPreferences.toRuntimeOverlayPreferences())
    private val mapDisplayMode = MutableStateFlow(initialMapControls.mapDisplayMode)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val locationPermissionGranted = MutableStateFlow(false)
    private val phonePermissionGranted = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val importingFileName = MutableStateFlow<String?>(null)
    private val importDialogState = MutableStateFlow<ImportDialogUiState?>(null)
    private val testMetricMode = MutableStateFlow(initialMapControls.testMetricMode)
    private val isTestRecording = MutableStateFlow(false)
    private val activeTestSamples = MutableStateFlow<List<TestLogSample>>(emptyList())
    private val measurementState = MutableStateFlow(MapMeasurementState())
    private val userPolygonDraft = MutableStateFlow<UserPolygonDraftUiState?>(null)
    private var pendingImportRequest: PendingImportRequest? = null
    private var testRecordingJob: Job? = null
    private val replayState = MutableStateFlow<LogReplayState?>(null)
    val logReplay: StateFlow<LogReplayState?> = replayState
    private var replayLoadJob: Job? = null
    private var replayPlayJob: Job? = null

    val cameraTargets = MutableSharedFlow<MapCameraTarget>(extraBufferCapacity = 8)
    val shareFileRequests = MutableSharedFlow<ShareFileRequest>(extraBufferCapacity = 4)

    private val layersFlow = layerStore.documents
    private val testLogsFlow = testLogStore.documents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val customTileSourcesFlow = settingsStore.customTileSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val availableTilePresetsFlow = customTileSourcesFlow
        .map { customSources ->
            TilePresets.mergeCustom(customSources).also(TileSourceCatalog::replaceCustomPresets)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TilePresets.selectable)
    private val tilePresetFlow = combine(
        settingsStore.tilePresetKeyFlow,
        availableTilePresetsFlow,
    ) { selectedKey, presets ->
        presets.firstOrNull { it.key == selectedKey }
            ?: TilePresets.legacyKeyToPreset(selectedKey)
            ?: presets.firstOrNull()
            ?: TilePresets.byId(TilePresetId.AMAP_ROAD)
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TilePresets.byId(TilePresetId.AMAP_ROAD))
    private val importGcj02Flow = settingsStore.importGcj02ToWgs84Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val simEngineeringBindingsFlow = settingsStore.simEngineeringBindingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SimEngineeringBindings())
    val deviceSensorSnapshotFlow: StateFlow<DeviceSensorSnapshot> = deviceSensorRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceSensorSnapshot.Empty)
    private val rawLocationFlow = locationPermissionGranted
        .flatMapLatest { granted -> if (granted) locationRepository.observe() else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val displayLocationFlow = rawLocationFlow
        .scan(null as LocationFix?) { previous, candidate ->
            MapLocationRefreshPolicy.resolveDisplayLocation(previous = previous, candidate = candidate)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val renderAnchorLocationFlow = rawLocationFlow
        .scan(null as LocationFix?) { previous, candidate ->
            MapLocationRefreshPolicy.resolve(previous = previous, candidate = candidate)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val allSectorsFlow = layersFlow
        .map { documents -> documents.filter { it.meta.isVisible }.map { it.sectors } }
        .distinctUntilChanged(::sameSectorLists)
        .mapLatest { sectorLists ->
            withContext(Dispatchers.Default) {
                sectorLists.flatten()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val sectorIndexFlow = allSectorsFlow
        .mapLatest { sectors ->
            withContext(Dispatchers.Default) {
                SectorMatchIndex.build(sectors)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SectorMatchIndex.Empty)
    private val engineeringUiSummaryFlow = layersFlow.mapLatest { documents ->
        withContext(Dispatchers.Default) {
            val summary = EngineeringCatalogSummary.create(documents)
            EngineeringUiSummaryPayload(
                layers = documents.map { it.meta },
                bands = summary.bands,
                options = documents.mapNotNull { document ->
                    val technologies = summary.technologiesByLayer[document.meta.id].orEmpty()
                    val supportsNr = RadioTechnology.NR in technologies
                    val supportsLte = RadioTechnology.LTE in technologies
                    if (!supportsNr && !supportsLte) return@mapNotNull null
                    EngineeringLayerOptionUiState(document.meta.id, document.meta.name, supportsNr, supportsLte)
                }.sortedBy { it.name },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EngineeringUiSummaryPayload())
    private val observedCellSnapshotFlow = combine(locationPermissionGranted, phonePermissionGranted) { locationGranted, phoneGranted ->
        locationGranted && phoneGranted
    }.flatMapLatest { granted ->
        if (granted) telephonyRepository.observe() else flowOf(CellSnapshot(permissionMissing = true))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CellSnapshot.Empty)
    private val cellSnapshotFlow = observedCellSnapshotFlow
        .distinctUntilChanged { previous, candidate ->
            CellSnapshotContentComparator.areEquivalent(previous, candidate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CellSnapshot.Empty)
    val liveNetworkEvents = observedCellSnapshotFlow.scan(LiveNetworkEvents()) { history, snapshot ->
        val matches = snapshot.simCards.map { sim ->
            SimCellMatch(sim.subscriptionId, sim.slotIndex, sim.label, servingCell = sim.servingCell, neighbors = sim.neighbors)
        }
        history.append(TestLogSampleFactory.create(snapshot.collectedAtMillis, rawLocationFlow.value, matches, snapshot, android.os.SystemClock.elapsedRealtime()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveNetworkEvents())
    private val signalHistoriesFlow = cellSnapshotFlow
        .scan(emptyList<SimSignalHistory>()) { histories, snapshot ->
            SimSignalHistoryTracker.update(
                histories = histories,
                simSnapshots = snapshot.simCards,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val cellDataFlow = combine(cellSnapshotFlow, signalHistoriesFlow) { cellSnapshot, signalHistories ->
        cellSnapshot to signalHistories
    }
    private val filteredSectorsFlow = combine(allSectorsFlow, selectedTechs, selectedBands) { sectors, technologies, bands ->
        Triple(sectors, technologies, bands)
    }.mapLatest { (sectors, technologies, bands) ->
        withContext(Dispatchers.Default) {
            sectors.filter { sector ->
                MapRecordFilter.matches(sector, technologies, bands)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    // Bounded nearest selection needs no second city-wide index at startup.
    private val visibleSectorSpatialIndexFlow = flowOf(SectorSpatialIndex.Empty)
    private val mapVectorRequestFlow = combine(
        filteredSectorsFlow,
        engineeringUiSummaryFlow,
        tilePresetFlow,
        combine(showSectorFill, showSitePoints, showSiteLabels) { sectors, sites, labels -> Triple(sectors, sites, labels) },
        nearbyRecordLimit,
    ) { records, summary, tilePreset, switches, limit ->
        MapVectorRenderRequest(records, summary.layers, tilePreset, switches.first, switches.second, switches.third, limit)
    }.distinctUntilChanged { previous, next -> previous.sameRenderingAs(next) }

    val preparedMapVectors: StateFlow<Map<String, PreparedMapVectors>> =
        combine(mapVectorRequestFlow, mapRenderCenters) { request, centers -> request to centers }
        .mapLatest { (request, centers) ->
            try {
                // Cancel obsolete work immediately; coalesce new inputs inside the cancellable block.
                delay(200)
                panelVectorPreparer.prepare(request, centers)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "图层绘制失败：${error.message ?: "请关闭部分图层后重试"}"
                null
            }
        }
        .mapNotNull { it }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val searchResultsFlow = combine(filteredSectorsFlow, submittedSearchQuery) { sectors, query ->
        sectors to query
    }.mapLatest { (sectors, query) ->
        withContext(Dispatchers.Default) {
            SearchResultMatcher.search(
                records = sectors,
                query = query,
                limit = 20,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val visibleTestSamplesFlow = combine(testLogsFlow, activeTestSamples) { documents, activeSamples ->
        documents
            .filter { it.meta.isVisible }
            .flatMap { it.samples } + activeSamples
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val testUiFlow = combine(
        testLogsFlow,
        testMetricMode,
        isTestRecording,
        activeTestSamples,
    ) { testLogs, metricMode, recording, activeSamples ->
        TestUiPayload(
            logs = testLogs.map { it.meta },
            metricMode = metricMode,
            isRecording = recording,
            activeSampleCount = activeSamples.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TestUiPayload())
    private val testLogRenderPayloadFlow = combine(
        visibleTestSamplesFlow,
        testMetricMode,
        simOverlayPreferences,
        tilePresetFlow,
    ) { samples, metricMode, overlayPreferences, tilePreset ->
        Quadruple(samples, metricMode, overlayPreferences, tilePreset)
    }.mapLatest { (samples, metricMode, overlayPreferences, tilePreset) ->
        withContext(Dispatchers.Default) {
            val visibleSlotIndexes = overlayPreferences
                .filterValues { it.showTestPoints }
                .keys
            TestLogMapRenderPayloadFactory.create(
                samples = samples,
                metricMode = metricMode,
                visibleSlotIndexes = visibleSlotIndexes,
                tilePreset = tilePreset,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TestLogMapRenderPayload.Empty)

    private val baseUiSourceFlow = combine(
        combine(engineeringUiSummaryFlow, allSectorsFlow, sectorIndexFlow, tilePresetFlow, availableTilePresetsFlow) { summary, allSectors, sectorIndex, tilePreset, tilePresets ->
            BaseUiStaticSourcePayload(
                summary = summary,
                allSectors = allSectors,
                sectorIndex = sectorIndex,
                tilePreset = tilePreset,
                tilePresets = tilePresets,
            )
        },
        combine(renderAnchorLocationFlow, cellDataFlow, filteredSectorsFlow, visibleSectorSpatialIndexFlow, simEngineeringBindingsFlow) { renderAnchorLocation, cellData, visibleSectors, visibleSectorSpatialIndex, simEngineeringBindings ->
            BaseUiDynamicSourcePayload(
                currentLocation = renderAnchorLocation,
                renderAnchorLocation = renderAnchorLocation,
                cellData = cellData,
                visibleSectors = visibleSectors,
                visibleSectorSpatialIndex = visibleSectorSpatialIndex,
                simEngineeringBindings = simEngineeringBindings,
            )
        },
        rawLocationFlow,
    ) { static, dynamic, currentLocation ->
        BaseUiSourcePayload(
            summary = static.summary,
            allSectors = static.allSectors,
            sectorIndex = static.sectorIndex,
            tilePreset = static.tilePreset,
            tilePresets = static.tilePresets,
            currentLocation = currentLocation,
            renderAnchorLocation = dynamic.renderAnchorLocation,
            cellData = dynamic.cellData,
            visibleSectors = dynamic.visibleSectors,
            visibleSectorSpatialIndex = dynamic.visibleSectorSpatialIndex,
            simEngineeringBindings = dynamic.simEngineeringBindings,
        )
    }

    private val baseUiFlow = baseUiSourceFlow
        .mapLatest { source ->
            withContext(Dispatchers.Default) {
                val (cellSnapshot, signalHistories) = source.cellData
                BaseUiPayload(
                    layers = source.summary.layers,
                    allBands = source.summary.bands,
                    engineeringLayerOptions = source.summary.options,
                    tilePreset = source.tilePreset,
                    tilePresets = source.tilePresets,
                    currentLocation = source.currentLocation,
                    cellSnapshot = cellSnapshot,
                    signalHistories = signalHistories,
                    visibleSectors = source.visibleSectors,
                    visibleSectorSpatialIndex = source.visibleSectorSpatialIndex,
                    simMatches = SimCellMatcher.match(
                        simSnapshots = cellSnapshot.simCards,
                        sectors = source.allSectors,
                        currentLocation = source.currentLocation,
                        layerBindings = source.simEngineeringBindings,
                        sectorIndex = source.sectorIndex,
                    ),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BaseUiPayload())

    private val controlsFlow = combine(
        combine(searchDraftQuery, showSectorFill, showSitePoints, showSiteLabels, simOverlayPreferences) {
                query,
                showFill,
                showSites,
                showLabels,
                overlayPreferences,
            ->
            UiControls(
                searchQuery = query,
                showSectorFill = showFill,
                showSitePoints = showSites,
                showSiteLabels = showLabels,
                showServingLine = overlayPreferences.values.any { it.showServingLine },
                showNeighborLines = overlayPreferences.values.any { it.showNeighborLines },
                mapDisplayMode = MapDisplayMode.Overlay,
            )
        },
        mapDisplayMode,
    ) { controls, displayMode ->
        controls.copy(
            mapDisplayMode = displayMode,
        )
    }
    private val searchUiFlow = combine(
        submittedSearchQuery,
        searchResultsFlow,
    ) { submittedQuery, results ->
        SearchUiPayload(
            submittedQuery = submittedQuery,
            results = results,
        )
    }

    private val flagsFlow = combine(
        combine(
            combine(selectedTechs, selectedBands, statusMessage, locationPermissionGranted, phonePermissionGranted) {
                    techs,
                    bands,
                    message,
                    hasLocation,
                    hasPhone,
                ->
                UiFlags(
                    selectedTechs = techs,
                    selectedBands = bands,
                    statusMessage = message,
                    locationPermissionGranted = hasLocation,
                    phonePermissionGranted = hasPhone,
                    importGcj02ToWgs84 = false,
                    isImporting = false,
                    importingFileName = null,
                )
            },
            combine(isImporting, importingFileName) { importing, importingName ->
                importing to importingName
            },
        ) { flags, importState ->
            flags.copy(
                isImporting = importState.first,
                importingFileName = importState.second,
            )
        },
        importGcj02Flow,
    ) { flags, importGcj02 ->
        flags.copy(importGcj02ToWgs84 = importGcj02)
    }

    private val uiSettingsFlow = combine(
        simEngineeringBindingsFlow,
        simOverlayPreferences,
    ) { simEngineeringBindings, overlayPreferences ->
        simEngineeringBindings to overlayPreferences
    }
    private val measurementUiFlow = combine(
        testUiFlow,
        measurementState,
        testLogRenderPayloadFlow,
        userPolygonDraft,
    ) { testUi, mapMeasurement, testLogRenderPayload, polygonDraft ->
        MeasurementUiPayload(
            testUi = testUi,
            mapMeasurement = mapMeasurement,
            testLogRenderPayload = testLogRenderPayload,
            userPolygonDraft = polygonDraft,
        )
    }

    private val uiCoreFlow = combine(
        baseUiFlow,
        controlsFlow,
        flagsFlow,
        importDialogState,
        measurementUiFlow,
    ) { base, controls, flags, importDialog, measurementUi ->
        val testUi = measurementUi.testUi
        val mapMeasurement = measurementUi.mapMeasurement
        val testLogRenderPayload = measurementUi.testLogRenderPayload
        UiCorePayload(
            base = base,
            controls = controls,
            flags = flags,
            importDialog = importDialog,
            testUi = testUi,
            mapMeasurement = mapMeasurement,
            testLogRenderPayload = testLogRenderPayload,
            userPolygonDraft = measurementUi.userPolygonDraft,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        uiCoreFlow,
        uiSettingsFlow,
        searchUiFlow,
        displayLocationFlow,
        nearbyRecordLimit,
    ) { core, settings, searchUi, displayLocation, nearbyLimit ->
        val (simEngineeringBindings, overlayPreferences) = settings
        val base = core.base
        val controls = core.controls
        val flags = core.flags
        val importDialog = core.importDialog
        val testUi = core.testUi
        val mapMeasurement = core.mapMeasurement
        val testLogRenderPayload = core.testLogRenderPayload
        val simPreferenceStates = buildSimPreferenceUiStates(
            snapshot = base.cellSnapshot,
            bindings = simEngineeringBindings,
            overlayPreferences = overlayPreferences,
        )
        MainUiState(
            nearbyRecordLimit = nearbyLimit,
            layers = base.layers,
            engineeringLayerOptions = base.engineeringLayerOptions,
            tilePreset = base.tilePreset,
            tilePresets = base.tilePresets,
            currentLocation = displayLocation,
            renderAnchorLocation = base.renderAnchorLocation,
            cellSnapshot = base.cellSnapshot,
            signalHistories = base.signalHistories,
            visibleSectors = base.visibleSectors,
            visibleSectorSpatialIndex = base.visibleSectorSpatialIndex,
            simMatches = base.simMatches,
            simPreferenceStates = simPreferenceStates,
            mapPanels = MapDisplayPlanner.build(controls.mapDisplayMode, base.simMatches),
            mapDisplayMode = controls.mapDisplayMode,
            searchQuery = controls.searchQuery,
            submittedSearchQuery = searchUi.submittedQuery,
            searchResults = searchUi.results,
            availableBands = base.allBands,
            selectedTechs = flags.selectedTechs,
            selectedBands = flags.selectedBands,
            showSectorFill = controls.showSectorFill,
            showSitePoints = controls.showSitePoints,
            showSiteLabels = controls.showSiteLabels,
            showServingLine = controls.showServingLine,
            showNeighborLines = controls.showNeighborLines,
            statusMessage = flags.statusMessage,
            locationPermissionGranted = flags.locationPermissionGranted,
            phonePermissionGranted = flags.phonePermissionGranted,
            importGcj02ToWgs84 = flags.importGcj02ToWgs84,
            isImporting = flags.isImporting,
            importingFileName = flags.importingFileName,
            importDialog = importDialog,
            testLogs = testUi.logs,
            testMetricMode = testUi.metricMode,
            isTestRecording = testUi.isRecording,
            activeTestSampleCount = testUi.activeSampleCount,
            measurementState = mapMeasurement,
            testLogRenderPayload = testLogRenderPayload,
            userPolygonDraft = core.userPolygonDraft,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        refreshPermissionState()
        viewModelScope.launch {
            settingsStore.mapControlPreferencesFlow
                .distinctUntilChanged()
                .collect(::applyMapControlPreferences)
        }
    }

    fun refreshPermissionState() {
        locationPermissionGranted.value = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        phonePermissionGranted.value = hasPermission(Manifest.permission.READ_PHONE_STATE)
    }

    private fun applyMapControlPreferences(preferences: MapControlPreferences) {
        selectedTechs.value = preferences.selectedTechs
        selectedBands.value = preferences.selectedBands
        showSectorFill.value = preferences.showSectorFill
        showSitePoints.value = preferences.showSitePoints
        showSiteLabels.value = preferences.showSiteLabels
        nearbyRecordLimit.value = preferences.nearbyRecordLimit
        mapDisplayMode.value = preferences.mapDisplayMode
        testMetricMode.value = preferences.testMetricMode
        simOverlayPreferences.value = preferences.simOverlayPreferences.toRuntimeOverlayPreferences()
    }

    fun setTilePreset(tilePresetKey: String) {
        viewModelScope.launch { settingsStore.setTilePresetKey(tilePresetKey) }
    }

    fun saveCustomTileSource(
        existingKey: String?,
        title: String,
        urlTemplate: String,
        tileSize: Int,
        coordinateSystem: TileCoordinateSystem,
    ) {
        viewModelScope.launch {
            runCatching {
                CustomTileSourceConfig.create(
                    existingKey = existingKey,
                    title = title,
                    urlTemplate = urlTemplate,
                    tileSize = tileSize,
                    coordinateSystem = coordinateSystem,
                    keySeed = UUID.randomUUID().toString(),
                )
            }.onSuccess { source ->
                settingsStore.upsertCustomTileSource(source)
                statusMessage.value = "已保存自定义底图：${source.title}"
            }.onFailure { error ->
                statusMessage.value = error.message ?: "自定义底图配置不正确"
            }
        }
    }

    fun deleteCustomTileSource(tilePresetKey: String) {
        viewModelScope.launch {
            settingsStore.deleteCustomTileSource(tilePresetKey)
            statusMessage.value = "已删除自定义底图"
        }
    }

    fun toggleTechnology(radioTechnology: RadioTechnology) {
        selectedTechs.update { current ->
            if (radioTechnology in current) current - radioTechnology else current + radioTechnology
        }
        persistMapControlPreferences()
    }

    fun toggleBand(band: String) {
        selectedBands.update { current ->
            if (band in current) current - band else current + band
        }
        persistMapControlPreferences()
    }

    fun toggleShowSectorFill() {
        showSectorFill.update { !it }
        persistMapControlPreferences()
    }

    fun toggleShowSitePoints() {
        showSitePoints.update { !it }
        persistMapControlPreferences()
    }

    fun toggleShowSiteLabels() {
        showSiteLabels.update { !it }
        persistMapControlPreferences()
    }

    fun toggleShowServingLine() {
        val next = !uiState.value.simPreferenceStates.all { it.showServingLine }
        showServingLine.value = next
        simOverlayPreferences.update { current ->
            current.mapValues { (_, preference) -> preference.copy(showServingLine = next) }
        }
        persistMapControlPreferences()
    }

    fun toggleShowNeighborLines() {
        val next = !uiState.value.simPreferenceStates.all { it.showNeighborLines }
        showNeighborLines.value = next
        simOverlayPreferences.update { current ->
            current.mapValues { (_, preference) -> preference.copy(showNeighborLines = next) }
        }
        persistMapControlPreferences()
    }

    fun toggleSimServingLine(slotIndex: Int) {
        simOverlayPreferences.update { current ->
            current + (slotIndex to current.preferenceFor(slotIndex).copy(
                showServingLine = !current.preferenceFor(slotIndex).showServingLine,
            ))
        }
        persistMapControlPreferences()
    }

    fun toggleSimNeighborLines(slotIndex: Int) {
        simOverlayPreferences.update { current ->
            current + (slotIndex to current.preferenceFor(slotIndex).copy(
                showNeighborLines = !current.preferenceFor(slotIndex).showNeighborLines,
            ))
        }
        persistMapControlPreferences()
    }

    fun toggleSimTestPoints(slotIndex: Int) {
        simOverlayPreferences.update { current ->
            current + (slotIndex to current.preferenceFor(slotIndex).copy(
                showTestPoints = !current.preferenceFor(slotIndex).showTestPoints,
            ))
        }
        persistMapControlPreferences()
    }

    fun updateSimEngineeringBinding(
        slotIndex: Int,
        technology: RadioTechnology,
        binding: EngineeringLayerBinding,
    ) {
        if (technology != RadioTechnology.NR && technology != RadioTechnology.LTE) return
        viewModelScope.launch {
            settingsStore.setSimEngineeringBinding(
                slotIndex = slotIndex,
                technology = technology,
                binding = binding,
            )
            statusMessage.value = "已更新 SIM${slotIndex + 1} ${technology.label} 工参匹配"
        }
    }

    fun toggleMapDisplayMode() {
        mapDisplayMode.update { current ->
            if (current == MapDisplayMode.Overlay) MapDisplayMode.Split else MapDisplayMode.Overlay
        }
        persistMapControlPreferences()
    }

    fun startTestRecording() {
        if (testRecordingJob != null) return
        if (!locationPermissionGranted.value || !phonePermissionGranted.value) {
            statusMessage.value = "请先授予定位和电话权限后再开始测试"
            return
        }
        val startedAt = System.currentTimeMillis()
        activeTestSamples.value = emptyList()
        isTestRecording.value = true
        statusMessage.value = "已开始测试，每个采样点自动保存"
        testRecordingJob = viewModelScope.launch {
            var logId: String? = null
            var recordingFailed = false
            var previousSample: TestLogSample? = null
            try {
                // Cancellation must not lose the ID of a journal already created on disk.
                withContext(NonCancellable) {
                    logId = testLogStore.beginRecording(buildTestLogName(startedAt), startedAt, testMetricMode.value)
                }
                while (isActive && isTestRecording.value) {
                    val timestamp = System.currentTimeMillis()
                    val locationFix = rawLocationFlow.value
                    val snapshot = telephonyRepository.readSnapshotNow()
                    val simMatches = SimCellMatcher.match(
                        simSnapshots = snapshot.simCards,
                        sectors = allSectorsFlow.value,
                        currentLocation = locationFix,
                        layerBindings = simEngineeringBindingsFlow.value,
                        sectorIndex = sectorIndexFlow.value,
                    )
                    val captured = TestLogSampleFactory.create(
                        timestampMillis = timestamp,
                        locationFix = locationFix,
                        simMatches = simMatches,
                        snapshot = snapshot,
                        elapsedRealtimeMillis = android.os.SystemClock.elapsedRealtime(),
                    )
                    val sample = captured.copy(events = NetworkEventDetector.detect(previousSample, captured))
                    withContext(NonCancellable) {
                        testLogStore.appendSample(requireNotNull(logId), sample)
                        activeTestSamples.update { current -> current + sample }
                        previousSample = sample
                    }
                    delay(1000)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                recordingFailed = true
                statusMessage.value = "测试已停止：${error.message ?: "采集或自动保存失败"}，已写入的采样会保留"
            } finally {
                withContext(NonCancellable) {
                    isTestRecording.value = false
                    try {
                        logId?.let { id ->
                            val document = testLogStore.finishRecording(id)
                            activeTestSamples.value = emptyList()
                            if (!recordingFailed) {
                                focusTestSamples(document.samples)
                                statusMessage.value = "测试记录已保存：${document.meta.sampleCount} 个点"
                            }
                        }
                    } catch (error: Exception) {
                        statusMessage.value = "记录整理失败，已自动保存的采样将在下次启动恢复：${error.message.orEmpty()}"
                    } finally {
                        testRecordingJob = null
                    }
                }
            }
        }
    }

    fun stopTestRecording() {
        if (!isTestRecording.value) return
        isTestRecording.value = false
        testRecordingJob?.cancel()
        statusMessage.value = "正在结束测试并整理已保存记录"
    }

    fun toggleTestMetricMode() {
        testMetricMode.update { current ->
            if (current == TestMetricMode.RSRP) TestMetricMode.SINR else TestMetricMode.RSRP
        }
        persistMapControlPreferences()
    }

    fun startDistanceMeasurement() {
        userPolygonDraft.value = null
        measurementState.value = MapMeasurementController.startDistance()
        statusMessage.value = "\u5DF2\u5F00\u542F\u8DDD\u79BB\u6D4B\u91CF\uFF0C\u5355\u51FB\u5730\u56FE\u5F00\u59CB\u52A0\u70B9"
    }

    fun startAreaMeasurement() {
        userPolygonDraft.value = null
        measurementState.value = MapMeasurementController.startArea()
        statusMessage.value = "\u5DF2\u5F00\u542F\u9762\u79EF\u6D4B\u91CF\uFF0C\u5355\u51FB\u5730\u56FE\u5F00\u59CB\u52A0\u70B9"
    }

    fun addMeasurementPoint(
        longitude: Double,
        latitude: Double,
    ) {
        measurementState.update { state ->
            MapMeasurementController.addPoint(
                state = state,
                longitude = longitude,
                latitude = latitude,
            )
        }
    }

    fun finishMeasurement() {
        measurementState.update(MapMeasurementController::finish)
    }

    fun clearMeasurement() {
        measurementState.value = MapMeasurementController.clear()
        statusMessage.value = "\u5DF2\u6E05\u9664\u6D4B\u91CF\u7ED3\u679C"
    }

    fun updateSearchQuery(query: String) {
        searchDraftQuery.value = query
        if (query.isBlank()) {
            submittedSearchQuery.value = ""
        }
    }

    fun submitSearchQuery() {
        submittedSearchQuery.value = searchDraftQuery.value.trim()
    }

    fun focusSector(record: SectorRecord) {
        cameraTargets.tryEmit(MapCameraTarget(record.longitude, record.latitude, 16.0))
    }

    fun jumpToCoordinate(input: String) {
        val coordinate = CoordinateParser.parse(input)
        if (coordinate == null) {
            statusMessage.value = "坐标格式不正确，请输入如 120.0 30.0 或 120.0,30.0"
            return
        }
        cameraTargets.tryEmit(MapCameraTarget(coordinate.longitude, coordinate.latitude, 15.5))
    }

    fun centerOnCurrentLocation() {
        uiState.value.currentLocation?.let { fix ->
            cameraTargets.tryEmit(MapCameraTarget(fix.longitude, fix.latitude, 16.5))
        } ?: run {
            statusMessage.value = "暂未获取到定位，请先开启 GPS 并授予权限"
        }
    }

    fun toggleLayerVisibility(layerId: String, visible: Boolean) {
        launchStorageOperation("图层显隐保存失败") { layerStore.setVisibility(layerId, visible) }
    }

    fun setLayerAlwaysRenderAllRecords(layerId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                layerStore.setAlwaysRenderAllRecords(layerId, enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "图层显示设置保存失败：${error.message}"
            }
        }
    }

    fun setNearbyRecordLimit(limit: Int) {
        if (limit <= 0) return
        nearbyRecordLimit.value = limit
        persistMapControlPreferences()
    }

    fun updateMapRenderCenter(panelId: String, center: GeoPoint?) {
        mapRenderCenters.value = if (center == null) mapRenderCenters.value - panelId
            else mapRenderCenters.value + (panelId to center)
    }

    fun deleteLayer(layerId: String) {
        launchStorageOperation("删除图层失败") {
            layerStore.deleteLayer(layerId)
            statusMessage.value = "图层已删除"
        }
    }

    fun toggleTestLogVisibility(logId: String, visible: Boolean) {
        launchStorageOperation("测试记录显隐保存失败") {
            testLogStore.setVisibility(logId, visible)
        }
    }

    fun deleteTestLog(logId: String) {
        if (replayState.value?.document?.meta?.id == logId) closeLogReplay()
        launchStorageOperation("删除测试记录失败") {
            testLogStore.deleteLog(logId)
            statusMessage.value = "测试记录已删除"
        }
    }

    fun openLogReplay(logId: String) {
        closeLogReplay()
        statusMessage.value = "正在分析测试记录"
        replayLoadJob = viewModelScope.launch {
            try {
                val document = testLogStore.findLog(logId) ?: error("测试记录不存在")
                val analysis = withContext(Dispatchers.Default) { TestLogAnalysis.create(document.samples) }
                replayState.value = LogReplayState(document, analysis)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { statusMessage.value = "无法打开记录：${error.message}" }
        }
    }

    fun closeLogReplay() {
        replayLoadJob?.cancel()
        replayPlayJob?.cancel()
        replayState.value = null
    }

    fun seekLogReplay(index: Int) {
        replayPlayJob?.cancel()
        replayState.update { it?.seek(index)?.copy(playing = false) }
        focusReplayIfOnMap()
    }

    fun toggleLogReplay() {
        val replay = replayState.value ?: return
        replayPlayJob?.cancel()
        if (replay.playing || replay.document.samples.isEmpty()) {
            replayState.value = replay.copy(playing = false)
            return
        }
        replayState.value = replay.copy(index = if (replay.index == replay.document.samples.lastIndex) 0 else replay.index, playing = true)
        focusReplayIfOnMap()
        replayPlayJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = replayState.value ?: break
                if (!current.playing) break
                if (current.index >= current.document.samples.lastIndex) {
                    replayState.value = current.copy(playing = false)
                    break
                }
                replayState.value = current.seek(current.index + 1)
                focusReplayIfOnMap()
            }
        }
    }

    fun locateReplaySample() {
        val replay = replayState.value ?: return
        val sample = replay.sample ?: return
        if (sample.point == null) return
        replayPlayJob?.cancel()
        replayState.value = replay.copy(mapMode = true, playing = false)
        viewModelScope.launch {
            try {
                testLogStore.setVisibility(replay.document.meta.id, true)
                if (replayState.value?.document?.meta?.id == replay.document.meta.id) focusReplayIfOnMap()
            } catch (error: Exception) { statusMessage.value = "地图定位失败：${error.message}" }
        }
    }

    private fun focusReplayIfOnMap() {
        val replay = replayState.value?.takeIf { it.mapMode } ?: return
        replay.sample?.takeIf { it.point != null }?.let { focusTestSamples(listOf(it)) }
    }

    fun showReplayAnalysis() {
        replayPlayJob?.cancel()
        replayState.update { it?.copy(mapMode = false, playing = false) }
    }

    fun shareTestLog(logId: String) {
        viewModelScope.launch {
            runCatching {
                val log = testLogStore.findLog(logId) ?: error("测试记录不存在")
                val shareDir = File(appContext.cacheDir, "shared-logs").apply { mkdirs() }
                val archive = File(shareDir, buildArchiveFileName(log.meta.name))
                testLogStore.exportArchive(logId, archive)
                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    archive,
                )
                shareFileRequests.tryEmit(
                    ShareFileRequest(
                        uri = uri,
                        mimeType = XYWY_LOG_MIME,
                        title = archive.name,
                    ),
                )
            }.onFailure { error ->
                statusMessage.value = "分享测试记录失败：${error.message ?: "请稍后重试"}"
            }
        }
    }

    fun updateLayerStyle(
        layerId: String,
        showLabels: Boolean,
        labelTextColor: String?,
        labelHaloColor: String?,
        pointIconPreset: LayerPointIconPreset,
        rasterColorHex: String? = null,
        rasterOpacity: Float? = null,
    ) {
        launchStorageOperation("图层样式保存失败") {
            layerStore.updateStyle(
                layerId = layerId,
                showLabels = showLabels,
                labelTextColor = labelTextColor,
                labelHaloColor = labelHaloColor,
                pointIconPreset = pointIconPreset,
                rasterColorHex = rasterColorHex,
                rasterOpacity = rasterOpacity,
            )
            statusMessage.value = "图层样式已更新"
        }
    }

    fun createUserLayer(name: String) {
        launchStorageOperation("新建图层失败") {
            val layerName = name.trim().ifBlank { "手动图层" }
            layerStore.createUserLayer(layerName)
            statusMessage.value = "已新建图层：$layerName"
        }
    }

    fun addUserPointAtMapCenter(
        layerId: String,
        name: String,
        iconPreset: LayerPointIconPreset,
        longitude: Double? = null,
        latitude: Double? = null,
        remark: String = "",
    ) {
        val viewport = GeoPoint(
            longitude = longitude ?: currentMapViewportSnapshot().longitude,
            latitude = latitude ?: currentMapViewportSnapshot().latitude,
        )
        if (layerId.isBlank()) {
            statusMessage.value = "请先选择手动图层"
            return
        }
        launchStorageOperation("添加点失败") {
            val objectName = name.trim().ifBlank { "新建点" }
            layerStore.upsertUserRecord(
                layerId = layerId,
                record = SectorRecord(
                    id = UUID.randomUUID().toString(),
                    layerId = layerId,
                    radioTechnology = RadioTechnology.UNKNOWN,
                    siteName = objectName,
                    cellName = objectName,
                    longitude = viewport.longitude,
                    latitude = viewport.latitude,
                    rawFields = mapOf(
                        "名称" to objectName,
                        "类型" to "点",
                        "经度" to viewport.longitude.toString(),
                        "纬度" to viewport.latitude.toString(),
                        "备注" to remark,
                        "icon" to iconPreset.name,
                        "图标" to iconPreset.title,
                    ),
                    isPointOnly = true,
                ),
            )
            layerStore.updateStyle(
                layerId = layerId,
                showLabels = true,
                labelTextColor = null,
                labelHaloColor = null,
                pointIconPreset = iconPreset,
            )
            statusMessage.value = "已添加点：$objectName"
        }
    }

    fun startUserPolygon(
        layerId: String,
        name: String,
    ) {
        startUserShape(layerId = layerId, name = name, shapeType = UserShapeType.POLYGON)
    }

    fun startUserLine(
        layerId: String,
        name: String,
    ) {
        startUserShape(layerId = layerId, name = name, shapeType = UserShapeType.LINE)
    }

    private fun startUserShape(
        layerId: String,
        name: String,
        shapeType: UserShapeType,
    ) {
        if (layerId.isBlank()) {
            statusMessage.value = "请先选择手动图层"
            return
        }
        val fallbackName = if (shapeType == UserShapeType.LINE) "新建线" else "新建多边形"
        val actionName = if (shapeType == UserShapeType.LINE) "线" else "多边形"
        userPolygonDraft.value = UserPolygonDraftUiState(
            layerId = layerId,
            name = name.trim().ifBlank { fallbackName },
            shapeType = shapeType,
        )
        measurementState.value = MapMeasurementController.clear()
        statusMessage.value = "点击地图添加${actionName}顶点"
    }

    fun addUserPolygonPoint(
        longitude: Double,
        latitude: Double,
    ) {
        userPolygonDraft.update { draft ->
            draft?.copy(points = draft.points + GeoPoint(longitude = longitude, latitude = latitude))
        }
    }

    fun undoUserPolygonPoint() {
        userPolygonDraft.update { draft ->
            draft?.copy(points = draft.points.dropLast(1))
        }
    }

    fun finishUserPolygon() {
        val draft = userPolygonDraft.value ?: return
        val minPointCount = if (draft.shapeType == UserShapeType.LINE) 2 else 3
        val shapeName = if (draft.shapeType == UserShapeType.LINE) "线" else "多边形"
        if (draft.points.size < minPointCount) {
            statusMessage.value = "${shapeName}至少需要 $minPointCount 个点"
            return
        }
        launchStorageOperation("保存图形失败") {
            val centerLongitude = draft.points.map { it.longitude }.average()
            val centerLatitude = draft.points.map { it.latitude }.average()
            layerStore.upsertUserRecord(
                layerId = draft.layerId,
                record = SectorRecord(
                    id = draft.recordId ?: UUID.randomUUID().toString(),
                    layerId = draft.layerId,
                    radioTechnology = RadioTechnology.UNKNOWN,
                    siteName = draft.name,
                    cellName = draft.name,
                    longitude = centerLongitude,
                    latitude = centerLatitude,
                    polygonCoordinates = draft.points,
                    rawFields = mapOf(
                        "名称" to draft.name,
                        "类型" to shapeName,
                        "type" to if (draft.shapeType == UserShapeType.LINE) "line" else "polygon",
                        "顶点数" to draft.points.size.toString(),
                    ),
                    isPointOnly = false,
                ),
            )
            userPolygonDraft.value = null
            statusMessage.value = "已保存${shapeName}：${draft.name}"
        }
    }

    fun startEditUserPolygon(
        layerId: String,
        recordId: String,
    ) {
        launchStorageOperation("读取图形失败") {
            val record = layerStore.findLayer(layerId)
                ?.sectors
                ?.firstOrNull { it.id == recordId }
            val shapeType = if (record?.isLineOnly == true) UserShapeType.LINE else UserShapeType.POLYGON
            val minPointCount = if (shapeType == UserShapeType.LINE) 2 else 3
            val shapeName = if (shapeType == UserShapeType.LINE) "线" else "多边形"
            if (record == null || record.polygonCoordinates.size < minPointCount) {
                statusMessage.value = "未找到可编辑的${shapeName}"
                return@launchStorageOperation
            }
            userPolygonDraft.value = UserPolygonDraftUiState(
                layerId = layerId,
                recordId = recordId,
                name = record.cellName ?: record.siteName ?: shapeName,
                points = record.polygonCoordinates,
                shapeType = shapeType,
            )
            measurementState.value = MapMeasurementController.clear()
            statusMessage.value = "已进入${shapeName}编辑：可继续点地图加点，完成后保存覆盖"
        }
    }

    fun cancelUserPolygon() {
        userPolygonDraft.value = null
        statusMessage.value = "已取消绘制"
    }

    fun updateUserRecord(
        layerId: String,
        recordId: String,
        name: String,
        longitude: Double?,
        latitude: Double?,
        remark: String,
        iconPreset: LayerPointIconPreset?,
    ) {
        val nextName = name.trim().ifBlank { "未命名对象" }
        launchStorageOperation("编辑对象失败") {
            val updated = layerStore.updateUserRecord(
                layerId = layerId,
                recordId = recordId,
                name = nextName,
                longitude = longitude,
                latitude = latitude,
                remark = remark,
                pointIconPreset = iconPreset,
            )
            statusMessage.value = if (updated == null) {
                "未找到要编辑的对象"
            } else {
                "对象已更新：$nextName"
            }
        }
    }

    fun moveUserPoint(
        layerId: String,
        recordId: String,
        longitude: Double,
        latitude: Double,
    ) {
        launchStorageOperation("移动点位失败") {
            val updated = layerStore.moveUserRecord(
                layerId = layerId,
                recordId = recordId,
                longitude = longitude,
                latitude = latitude,
            )
            statusMessage.value = if (updated == null) {
                "未找到要移动的点"
            } else {
                "点位已移动"
            }
        }
    }

    fun deleteUserRecord(
        layerId: String,
        recordId: String,
    ) {
        launchStorageOperation("删除对象失败") {
            val updated = layerStore.deleteRecord(layerId, recordId)
            statusMessage.value = if (updated == null) {
                "未找到要删除的对象"
            } else {
                "对象已删除"
            }
        }
    }

    fun exportLayerCsv(layerId: String) {
        exportUserLayer(layerId = layerId, extension = "csv", mimeType = "text/csv") { document ->
            UserLayerExportCodec.toCsv(document)
        }
    }

    fun exportLayerKml(layerId: String) {
        exportUserLayer(layerId = layerId, extension = "kml", mimeType = "application/vnd.google-earth.kml+xml") { document ->
            UserLayerExportCodec.toKml(document)
        }
    }

    fun setImportGcj02ToWgs84(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setImportGcj02ToWgs84(enabled)
        }
    }

    fun prepareImportFile(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
        mimeType: String? = null,
    ) {
        if (isImporting.value) {
            statusMessage.value = "\u6B63\u5728\u5BFC\u5165\u4E2D\uFF0C\u8BF7\u7A0D\u5019"
            return
        }
        val format = ImportFileFormatResolver.resolve(displayName = displayName, mimeType = mimeType)
        if (format == ImportFileFormat.XYWY) {
            executeTestLogImport(resolver, uri, displayName)
            return
        }
        if (format == ImportFileFormat.KML || format == ImportFileFormat.TIFF || format == ImportFileFormat.TIFF_ZIP) {
            executeImport(resolver, uri, displayName, mimeType, sheetConfig = null)
            return
        }
        isImporting.value = true
        importingFileName.value = displayName
        viewModelScope.launch {
            val sheets = try {
                engineeringParamImporter.previewSheets(resolver, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "读取导入文件失败：${error.message ?: "请检查文件格式"}"
                return@launch
            } finally {
                isImporting.value = false
                importingFileName.value = null
            }
            if (sheets.isNotEmpty()) {
                pendingImportRequest = PendingImportRequest(
                    uri = uri,
                    displayName = displayName,
                    mimeType = mimeType,
                )
                importDialogState.value = buildImportDialogState(
                    displayName = displayName,
                    sheets = sheets,
                    selectedSheetName = sheets.first().name,
                    renderMode = ImportedLayerRenderMode.AUTO,
                )
            } else {
                executeImport(resolver, uri, displayName, mimeType, sheetConfig = null)
            }
        }
    }

    fun dismissImportDialog() {
        pendingImportRequest = null
        importDialogState.value = null
    }

    fun selectImportSheet(sheetName: String) {
        val current = importDialogState.value ?: return
        importDialogState.value = buildImportDialogState(
            displayName = current.displayName,
            sheets = current.sheets,
            selectedSheetName = sheetName,
            renderMode = current.renderMode,
        )
    }

    fun updateImportRenderMode(renderMode: ImportedLayerRenderMode) {
        importDialogState.update { current -> current?.copy(renderMode = renderMode) }
    }

    fun updateImportField(field: ImportFieldKey, header: String?) {
        importDialogState.update { current ->
            current?.copy(
                fieldMapping = when (field) {
                    ImportFieldKey.Longitude -> current.fieldMapping.copy(longitudeHeader = header)
                    ImportFieldKey.Latitude -> current.fieldMapping.copy(latitudeHeader = header)
                    ImportFieldKey.Azimuth -> current.fieldMapping.copy(azimuthHeader = header)
                    ImportFieldKey.Name -> current.fieldMapping.copy(nameHeader = header)
                    ImportFieldKey.SiteId -> current.fieldMapping.copy(siteIdHeader = header)
                    ImportFieldKey.CellId -> current.fieldMapping.copy(cellIdHeader = header)
                    ImportFieldKey.Pci -> current.fieldMapping.copy(pciHeader = header)
                    ImportFieldKey.AntennaHeight -> current.fieldMapping.copy(antennaHeightHeader = header)
                },
            )
        }
    }

    fun toggleImportDetailHeader(header: String) {
        importDialogState.update { current ->
            current?.copy(
                detailHeaders = if (header in current.detailHeaders) {
                    current.detailHeaders - header
                } else {
                    current.detailHeaders + header
                },
            )
        }
    }

    fun confirmImportDialog(resolver: ContentResolver) {
        val request = pendingImportRequest ?: return
        val dialog = importDialogState.value ?: return
        pendingImportRequest = null
        importDialogState.value = null
        executeImport(
            resolver = resolver,
            uri = request.uri,
            displayName = request.displayName,
            mimeType = request.mimeType,
            sheetConfig = ImportSheetConfig(
                selectedSheetName = dialog.selectedSheetName,
                renderMode = dialog.renderMode,
                fieldMapping = dialog.fieldMapping,
                detailHeaders = dialog.detailHeaders,
            ),
        )
    }

    fun consumeStatusMessage() {
        statusMessage.value = null
    }

    private fun launchStorageOperation(failureMessage: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "$failureMessage：${error.message ?: "请检查可用存储空间后重试"}"
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun executeImport(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
        mimeType: String?,
        sheetConfig: ImportSheetConfig?,
    ) {
        if (isImporting.value) return
        isImporting.value = true
        importingFileName.value = displayName
        viewModelScope.launch {
            try {
                val previewLayerId = java.util.UUID.randomUUID().toString()
                val parsed = engineeringParamImporter.parse(
                    resolver = resolver,
                    uri = uri,
                    displayName = displayName,
                    mimeType = mimeType,
                    layerId = previewLayerId,
                    options = ImportOptions(
                        convertGcj02ToWgs84 = settingsStore.importGcj02ToWgs84Flow.first(),
                        sheetConfig = sheetConfig,
                    ),
                )
                if (parsed.records.isEmpty() && parsed.rasterLayer == null) {
                    statusMessage.value = "\u5BFC\u5165\u5931\u8D25\uFF0C\u672A\u89E3\u6790\u5230\u6709\u6548\u5DE5\u53C2\u3001\u70B9\u4F4D\u6216 KML \u8981\u7D20"
                    return@launch
                }
                val baseName = displayName.substringBeforeLast('.').ifBlank { "\u65B0\u56FE\u5C42" }
                val layerName = sheetConfig?.selectedSheetName?.let { "$baseName [$it]" } ?: baseName
                val layer = if (parsed.rasterLayer != null) {
                    layerStore.addRasterLayer(
                        displayName = layerName,
                        sourceFileName = displayName,
                        raster = parsed.rasterLayer,
                        layerId = previewLayerId,
                    )
                } else {
                    layerStore.addLayer(
                        displayName = layerName,
                        sourceFileName = displayName,
                        sectors = parsed.records,
                        sheetName = sheetConfig?.selectedSheetName,
                        renderMode = sheetConfig?.renderMode ?: ImportedLayerRenderMode.AUTO,
                        detailFieldHeaders = sheetConfig?.detailHeaders.orEmpty(),
                        layerId = previewLayerId,
                    )
                }
                if (parsed.rasterLayer != null) {
                    focusImportedRaster(layer.meta.rasterInfo)
                } else {
                    focusImportedRecords(layer.sectors)
                }
                statusMessage.value = if (parsed.rasterLayer != null) {
                    "导入完成：${layer.meta.recordCount} 个有效格点"
                } else {
                    "\u5BFC\u5165\u5B8C\u6210\uFF1A${layer.meta.recordCount} \u6761\uFF0C\u8DF3\u8FC7 ${parsed.skippedCount} \u6761"
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "\u5BFC\u5165\u5931\u8D25\uFF1A${error.message ?: "\u8BF7\u68C0\u67E5\u6587\u4EF6\u683C\u5F0F"}"
            } finally {
                isImporting.value = false
                importingFileName.value = null
            }
        }
    }

    private fun executeTestLogImport(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
    ) {
        if (isImporting.value) return
        isImporting.value = true
        importingFileName.value = displayName
        viewModelScope.launch {
            try {
                val document = testLogStore.importArchive(
                    resolver = resolver,
                    uri = uri,
                    displayName = displayName,
                )
                focusTestSamples(document.samples)
                statusMessage.value = "测试记录导入完成：${document.meta.sampleCount} 个点"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage.value = "测试记录导入失败：${error.message ?: "请检查文件格式"}"
            } finally {
                isImporting.value = false
                importingFileName.value = null
            }
        }
    }

    private fun buildImportDialogState(
        displayName: String,
        sheets: List<ParsedSheetPreview>,
        selectedSheetName: String,
        renderMode: ImportedLayerRenderMode,
    ): ImportDialogUiState {
        val selectedSheet = sheets.firstOrNull { it.name == selectedSheetName } ?: sheets.first()
        val suggestion = ImportFieldSuggester.suggest(selectedSheet.headers)
        return ImportDialogUiState(
            displayName = displayName,
            sheets = sheets,
            selectedSheetName = selectedSheet.name,
            availableHeaders = selectedSheet.headers,
            renderMode = renderMode,
            fieldMapping = suggestion.mapping,
            detailHeaders = suggestion.defaultDetailHeaders,
        )
    }

    private fun focusImportedRecords(records: List<SectorRecord>) {
        val focus = MapImportFocusPlanner.resolve(records) ?: return
        cameraTargets.tryEmit(MapCameraTarget(focus.longitude, focus.latitude, focus.zoom))
    }

    private fun focusImportedRaster(rasterInfo: RasterLayerInfo?) {
        rasterInfo ?: return
        val longitudes = listOf(
            rasterInfo.topLeft.longitude,
            rasterInfo.topRight.longitude,
            rasterInfo.bottomRight.longitude,
            rasterInfo.bottomLeft.longitude,
        )
        val latitudes = listOf(
            rasterInfo.topLeft.latitude,
            rasterInfo.topRight.latitude,
            rasterInfo.bottomRight.latitude,
            rasterInfo.bottomLeft.latitude,
        )
        val centerLongitude = longitudes.average()
        val centerLatitude = latitudes.average()
        val span = maxOf(
            (longitudes.maxOrNull() ?: centerLongitude) - (longitudes.minOrNull() ?: centerLongitude),
            (latitudes.maxOrNull() ?: centerLatitude) - (latitudes.minOrNull() ?: centerLatitude),
        )
        val zoom = when {
            span <= 0.02 -> 15.0
            span <= 0.08 -> 13.5
            span <= 0.3 -> 12.0
            span <= 1.0 -> 10.0
            span <= 3.0 -> 8.0
            else -> 6.2
        }
        cameraTargets.tryEmit(MapCameraTarget(centerLongitude, centerLatitude, zoom))
    }

    private fun focusTestSamples(samples: List<TestLogSample>) {
        val points = samples.mapNotNull { it.point }
        if (points.isEmpty()) return
        val longitudes = points.map { it.longitude }
        val latitudes = points.map { it.latitude }
        val centerLongitude = longitudes.average()
        val centerLatitude = latitudes.average()
        val span = maxOf(
            (longitudes.maxOrNull() ?: centerLongitude) - (longitudes.minOrNull() ?: centerLongitude),
            (latitudes.maxOrNull() ?: centerLatitude) - (latitudes.minOrNull() ?: centerLatitude),
        )
        val zoom = when {
            span <= 0.01 -> 16.0
            span <= 0.05 -> 14.5
            span <= 0.15 -> 13.0
            span <= 0.5 -> 11.5
            span <= 1.5 -> 9.5
            else -> 7.2
        }
        cameraTargets.tryEmit(MapCameraTarget(centerLongitude, centerLatitude, zoom))
    }

    private fun buildTestLogName(startedAtMillis: Long): String =
        "测试记录 ${TEST_LOG_NAME_FORMAT.format(Date(startedAtMillis))}"

    private fun buildArchiveFileName(name: String): String =
        name.ifBlank { "测试记录" }
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .trim()
            .ifBlank { "测试记录" } + ".xywy"

    private fun currentMapViewportSnapshot(): GeoPoint {
        val location = uiState.value.currentLocation
        return GeoPoint(
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
        )
    }

    private fun persistMapControlPreferences() {
        val preferences = MapControlPreferences(
            selectedTechs = selectedTechs.value,
            selectedBands = selectedBands.value,
            showSectorFill = showSectorFill.value,
            showSitePoints = showSitePoints.value,
            showSiteLabels = showSiteLabels.value,
            nearbyRecordLimit = nearbyRecordLimit.value,
            mapDisplayMode = mapDisplayMode.value,
            testMetricMode = testMetricMode.value,
            simOverlayPreferences = simOverlayPreferences.value.toPersistedOverlayPreferences(),
        )
        viewModelScope.launch {
            settingsStore.setMapControlPreferences(preferences)
        }
    }

    private fun exportUserLayer(
        layerId: String,
        extension: String,
        mimeType: String,
        encode: (com.xywy.signalmap.domain.model.LayerDocument) -> String,
    ) {
        viewModelScope.launch {
            runCatching {
                val document = layerStore.findLayer(layerId) ?: error("图层不存在")
                val shareDir = File(appContext.cacheDir, "shared-layers").apply { mkdirs() }
                val file = File(shareDir, buildLayerExportFileName(document.meta.name, extension))
                file.writeText(encode(document), Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file,
                )
                shareFileRequests.tryEmit(
                    ShareFileRequest(
                        uri = uri,
                        mimeType = mimeType,
                        title = file.name,
                    ),
                )
            }.onFailure { error ->
                statusMessage.value = "导出图层失败：${error.message ?: "请稍后重试"}"
            }
        }
    }

    private fun buildLayerExportFileName(
        name: String,
        extension: String,
    ): String =
        name.ifBlank { "手动图层" }
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .trim()
            .ifBlank { "手动图层" } + ".$extension"

    companion object {
        fun factory(application: SignalMapApplication): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = application.container
                return MainViewModel(
                    appContext = application.applicationContext,
                    settingsStore = container.settingsStore,
                    layerStore = container.layerStore,
                    testLogStore = container.testLogStore,
                    engineeringParamImporter = container.engineeringParamImporter,
                    locationRepository = container.locationRepository,
                    telephonyRepository = container.telephonyRepository,
                    deviceSensorRepository = container.deviceSensorRepository,
                ) as T
            }
        }
    }
}

private data class BaseUiPayload(
    val layers: List<LayerMetadata> = emptyList(),
    val allBands: List<String> = emptyList(),
    val engineeringLayerOptions: List<EngineeringLayerOptionUiState> = emptyList(),
    val tilePreset: TilePreset = TilePresets.byId(TilePresetId.AMAP_ROAD),
    val tilePresets: List<TilePreset> = TilePresets.selectable,
    val currentLocation: LocationFix? = null,
    val renderAnchorLocation: LocationFix? = null,
    val cellSnapshot: CellSnapshot = CellSnapshot.Empty,
    val signalHistories: List<SimSignalHistory> = emptyList(),
    val visibleSectors: List<SectorRecord> = emptyList(),
    val visibleSectorSpatialIndex: SectorSpatialIndex = SectorSpatialIndex.Empty,
    val simMatches: List<SimCellMatch> = emptyList(),
)

private data class BaseUiSourcePayload(
    val summary: EngineeringUiSummaryPayload,
    val allSectors: List<SectorRecord>,
    val sectorIndex: SectorMatchIndex,
    val tilePreset: TilePreset,
    val tilePresets: List<TilePreset>,
    val currentLocation: LocationFix?,
    val renderAnchorLocation: LocationFix?,
    val cellData: Pair<CellSnapshot, List<SimSignalHistory>>,
    val visibleSectors: List<SectorRecord>,
    val visibleSectorSpatialIndex: SectorSpatialIndex,
    val simEngineeringBindings: SimEngineeringBindings,
)

private data class BaseUiStaticSourcePayload(
    val summary: EngineeringUiSummaryPayload,
    val allSectors: List<SectorRecord>,
    val sectorIndex: SectorMatchIndex,
    val tilePreset: TilePreset,
    val tilePresets: List<TilePreset>,
)

private data class BaseUiDynamicSourcePayload(
    val currentLocation: LocationFix?,
    val renderAnchorLocation: LocationFix?,
    val cellData: Pair<CellSnapshot, List<SimSignalHistory>>,
    val visibleSectors: List<SectorRecord>,
    val visibleSectorSpatialIndex: SectorSpatialIndex,
    val simEngineeringBindings: SimEngineeringBindings,
)

private data class TestUiPayload(
    val logs: List<TestLogMetadata> = emptyList(),
    val metricMode: TestMetricMode = TestMetricMode.RSRP,
    val isRecording: Boolean = false,
    val activeSampleCount: Int = 0,
)

private data class SearchUiPayload(
    val submittedQuery: String = "",
    val results: List<SectorRecord> = emptyList(),
)

private data class UiCorePayload(
    val base: BaseUiPayload,
    val controls: UiControls,
    val flags: UiFlags,
    val importDialog: ImportDialogUiState?,
    val testUi: TestUiPayload,
    val mapMeasurement: MapMeasurementState,
    val testLogRenderPayload: TestLogMapRenderPayload,
    val userPolygonDraft: UserPolygonDraftUiState?,
)

private data class MeasurementUiPayload(
    val testUi: TestUiPayload,
    val mapMeasurement: MapMeasurementState,
    val testLogRenderPayload: TestLogMapRenderPayload,
    val userPolygonDraft: UserPolygonDraftUiState?,
)

private data class UiControls(
    val searchQuery: String,
    val showSectorFill: Boolean,
    val showSitePoints: Boolean,
    val showSiteLabels: Boolean,
    val showServingLine: Boolean,
    val showNeighborLines: Boolean,
    val mapDisplayMode: MapDisplayMode,
)

private data class UiFlags(
    val selectedTechs: Set<RadioTechnology>,
    val selectedBands: Set<String>,
    val statusMessage: String?,
    val locationPermissionGranted: Boolean,
    val phonePermissionGranted: Boolean,
    val importGcj02ToWgs84: Boolean,
    val isImporting: Boolean,
    val importingFileName: String?,
)

data class MainUiState(
    val nearbyRecordLimit: Int = 200,
    val layers: List<LayerMetadata> = emptyList(),
    val engineeringLayerOptions: List<EngineeringLayerOptionUiState> = emptyList(),
    val tilePreset: TilePreset = TilePresets.byId(TilePresetId.AMAP_ROAD),
    val tilePresets: List<TilePreset> = TilePresets.selectable,
    val currentLocation: LocationFix? = null,
    val renderAnchorLocation: LocationFix? = null,
    val cellSnapshot: CellSnapshot = CellSnapshot.Empty,
    val signalHistories: List<SimSignalHistory> = emptyList(),
    val visibleSectors: List<SectorRecord> = emptyList(),
    val visibleSectorSpatialIndex: SectorSpatialIndex = SectorSpatialIndex.Empty,
    val simMatches: List<SimCellMatch> = emptyList(),
    val simPreferenceStates: List<SimPreferenceUiState> = defaultSimPreferenceUiStates(),
    val mapPanels: List<MapPanelPlan> = emptyList(),
    val mapDisplayMode: MapDisplayMode = MapDisplayMode.Overlay,
    val searchQuery: String = "",
    val submittedSearchQuery: String = "",
    val searchResults: List<SectorRecord> = emptyList(),
    val availableBands: List<String> = emptyList(),
    val selectedTechs: Set<RadioTechnology> = emptySet(),
    val selectedBands: Set<String> = emptySet(),
    val showSectorFill: Boolean = true,
    val showSitePoints: Boolean = true,
    val showSiteLabels: Boolean = true,
    val showServingLine: Boolean = true,
    val showNeighborLines: Boolean = true,
    val statusMessage: String? = null,
    val locationPermissionGranted: Boolean = false,
    val phonePermissionGranted: Boolean = false,
    val importGcj02ToWgs84: Boolean = false,
    val isImporting: Boolean = false,
    val importingFileName: String? = null,
    val importDialog: ImportDialogUiState? = null,
    val testLogs: List<TestLogMetadata> = emptyList(),
    val testMetricMode: TestMetricMode = TestMetricMode.RSRP,
    val isTestRecording: Boolean = false,
    val activeTestSampleCount: Int = 0,
    val measurementState: MapMeasurementState = MapMeasurementState(),
    val testLogRenderPayload: TestLogMapRenderPayload = TestLogMapRenderPayload.Empty,
    val userPolygonDraft: UserPolygonDraftUiState? = null,
)

private data class EngineeringUiSummaryPayload(
    val layers: List<LayerMetadata> = emptyList(),
    val bands: List<String> = emptyList(),
    val options: List<EngineeringLayerOptionUiState> = emptyList(),
)

data class UserPolygonDraftUiState(
    val layerId: String,
    val recordId: String? = null,
    val name: String,
    val points: List<GeoPoint> = emptyList(),
    val shapeType: UserShapeType = UserShapeType.POLYGON,
)

enum class UserShapeType {
    LINE,
    POLYGON,
}

data class EngineeringLayerOptionUiState(
    val layerId: String,
    val name: String,
    val supportsNr: Boolean,
    val supportsLte: Boolean,
)

data class SimPreferenceUiState(
    val slotIndex: Int,
    val label: String,
    val nrBinding: EngineeringLayerBinding = EngineeringLayerBinding.Auto,
    val lteBinding: EngineeringLayerBinding = EngineeringLayerBinding.Auto,
    val showServingLine: Boolean = true,
    val showNeighborLines: Boolean = true,
    val showTestPoints: Boolean = true,
)

private data class SimOverlayPreference(
    val showServingLine: Boolean = true,
    val showNeighborLines: Boolean = true,
    val showTestPoints: Boolean = true,
)

private fun Map<Int, SimOverlayPreferenceState>.toRuntimeOverlayPreferences(): Map<Int, SimOverlayPreference> =
    defaultSimOverlayPreferences() + mapValues { (_, preference) ->
        SimOverlayPreference(
            showServingLine = preference.showServingLine,
            showNeighborLines = preference.showNeighborLines,
            showTestPoints = preference.showTestPoints,
        )
    }

private fun Map<Int, SimOverlayPreference>.toPersistedOverlayPreferences(): Map<Int, SimOverlayPreferenceState> =
    mapValues { (_, preference) ->
        SimOverlayPreferenceState(
            showServingLine = preference.showServingLine,
            showNeighborLines = preference.showNeighborLines,
            showTestPoints = preference.showTestPoints,
        )
    }

data class MapCameraTarget(
    val longitude: Double,
    val latitude: Double,
    val zoom: Double,
)

enum class ImportFieldKey {
    Longitude,
    Latitude,
    Azimuth,
    Name,
    SiteId,
    CellId,
    Pci,
    AntennaHeight,
}

data class ImportDialogUiState(
    val displayName: String,
    val sheets: List<ParsedSheetPreview>,
    val selectedSheetName: String,
    val availableHeaders: List<String>,
    val renderMode: ImportedLayerRenderMode,
    val fieldMapping: ImportFieldMapping,
    val detailHeaders: List<String>,
) {
    val canConfirm: Boolean
        get() = !fieldMapping.longitudeHeader.isNullOrBlank() && !fieldMapping.latitudeHeader.isNullOrBlank()
}

private data class PendingImportRequest(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
)

private fun buildSimPreferenceUiStates(
    snapshot: CellSnapshot,
    bindings: SimEngineeringBindings,
    overlayPreferences: Map<Int, SimOverlayPreference>,
): List<SimPreferenceUiState> = defaultSimPreferenceUiStates().map { fallback ->
    val simCard = snapshot.simCards.firstOrNull { it.slotIndex == fallback.slotIndex }
    val overlay = overlayPreferences.preferenceFor(fallback.slotIndex)
    SimPreferenceUiState(
        slotIndex = fallback.slotIndex,
        label = simCard?.label?.takeIf(String::isNotBlank) ?: fallback.label,
        nrBinding = bindings.forSlot(fallback.slotIndex).nr,
        lteBinding = bindings.forSlot(fallback.slotIndex).lte,
        showServingLine = overlay.showServingLine,
        showNeighborLines = overlay.showNeighborLines,
        showTestPoints = overlay.showTestPoints,
    )
}

private fun defaultSimPreferenceUiStates(): List<SimPreferenceUiState> =
    listOf(0, 1).map { slotIndex ->
        SimPreferenceUiState(
            slotIndex = slotIndex,
            label = "SIM${slotIndex + 1}",
        )
    }

private fun defaultSimOverlayPreferences(): Map<Int, SimOverlayPreference> =
    listOf(0, 1).associateWith { SimOverlayPreference() }

private fun Map<Int, SimOverlayPreference>.preferenceFor(slotIndex: Int): SimOverlayPreference =
    this[slotIndex] ?: SimOverlayPreference()

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

data class ShareFileRequest(
    val uri: Uri,
    val mimeType: String,
    val title: String,
)

private val TEST_LOG_NAME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINA)
const val XYWY_LOG_MIME: String = "application/x-xywy-log"
