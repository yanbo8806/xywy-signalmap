package com.xywy.signalmap.data.local

import android.content.Context
import com.xywy.signalmap.data.importer.ParsedRasterLayer
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RasterLayerInfo
import com.xywy.signalmap.domain.model.SectorRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class LayerStore(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val diskStore = LayerDiskStore(
        rootDir = File(context.filesDir, "layers"),
        json = json,
    )
    private val rasterAssetStore = RasterLayerAssetStore(
        rootDir = File(context.filesDir, "layers"),
    )
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val documentsState = MutableStateFlow<List<LayerDocument>>(emptyList())
    private val mutations = Mutex()

    val documents: StateFlow<List<LayerDocument>> = documentsState.asStateFlow()

    private val initialization = storeScope.launch {
        mutations.withLock {
            documentsState.value = loadMetadataSkeleton()
            preloadVisibleLayers()
            refreshRasterAssetsIfNeeded()
        }
    }

    suspend fun addLayer(
        displayName: String,
        sourceFileName: String,
        sectors: List<SectorRecord>,
        sheetName: String? = null,
        renderMode: ImportedLayerRenderMode = ImportedLayerRenderMode.AUTO,
        detailFieldHeaders: List<String> = emptyList(),
        userEditable: Boolean = false,
        rasterInfo: RasterLayerInfo? = null,
        layerId: String = UUID.randomUUID().toString(),
    ): LayerDocument = onDisk {
        val metadata = LayerMetadata(
            id = layerId,
            name = displayName,
            sourceFileName = sourceFileName,
            importedAtMillis = System.currentTimeMillis(),
            isVisible = true,
            recordCount = rasterInfo?.validPixelCount ?: sectors.size,
            sheetName = sheetName,
            renderMode = renderMode,
            detailFieldHeaders = detailFieldHeaders,
            userEditable = userEditable,
            rasterInfo = rasterInfo,
        )
        val normalizedSectors = if (sectors.all { it.layerId == layerId }) {
            sectors
        } else {
            sectors.map { it.copy(layerId = metadata.id) }
        }
        val document = LayerDocument(
            meta = metadata,
            sectors = normalizedSectors,
        )
        saveFullDocument(document)
        document
    }

    suspend fun addRasterLayer(
        displayName: String,
        sourceFileName: String,
        raster: ParsedRasterLayer,
        layerId: String = UUID.randomUUID().toString(),
    ): LayerDocument = withContext(Dispatchers.IO) {
        val rasterInfo = RasterLayerInfo(
            width = raster.width,
            height = raster.height,
            validPixelCount = raster.validPixelCount,
            colorHex = RasterLayerInfo.DEFAULT_RASTER_COLOR_HEX,
            opacity = RasterLayerInfo.DEFAULT_RASTER_OPACITY,
            topLeft = raster.bounds.topLeft,
            topRight = raster.bounds.topRight,
            bottomRight = raster.bounds.bottomRight,
            bottomLeft = raster.bounds.bottomLeft,
        )
        rasterAssetStore.write(
            layerId = layerId,
            width = raster.width,
            height = raster.height,
            alphaMask = raster.alphaMask,
            colorHex = rasterInfo.colorHex,
            opacity = rasterInfo.opacity,
        )
        addLayer(
            displayName = displayName,
            sourceFileName = sourceFileName,
            sectors = emptyList(),
            renderMode = ImportedLayerRenderMode.AUTO,
            rasterInfo = rasterInfo,
            layerId = layerId,
        )
    }

    suspend fun createUserLayer(
        name: String,
        layerId: String = UUID.randomUUID().toString(),
    ): LayerDocument = addLayer(
        displayName = name,
        sourceFileName = "手动新建",
        sectors = emptyList(),
        renderMode = ImportedLayerRenderMode.USER,
        userEditable = true,
        layerId = layerId,
    )

    suspend fun upsertUserRecord(
        layerId: String,
        record: SectorRecord,
    ): LayerDocument? = onDisk {
        val document = loadFullDocument(layerId) ?: return@onDisk null
        val normalizedRecord = record.copy(layerId = layerId)
        val sectors = document.sectors.filterNot { it.id == normalizedRecord.id } + normalizedRecord
        val updated = document.copy(
            meta = document.meta.copy(recordCount = sectors.size, userEditable = true, renderMode = ImportedLayerRenderMode.USER),
            sectors = sectors,
        )
        saveFullDocument(updated)
        updated
    }

    suspend fun renameRecord(
        layerId: String,
        recordId: String,
        name: String,
    ): LayerDocument? = onDisk {
        updateRecord(layerId, recordId) { record ->
            record.copy(
                siteName = name,
                cellName = name,
                rawFields = record.rawFields + ("名称" to name),
            )
        }
    }

    suspend fun updateUserRecord(
        layerId: String,
        recordId: String,
        name: String,
        pointIconPreset: LayerPointIconPreset? = null,
        longitude: Double? = null,
        latitude: Double? = null,
        remark: String? = null,
    ): LayerDocument? = onDisk {
        updateRecord(layerId, recordId) { record ->
            val nextRawFields = buildMap {
                putAll(record.rawFields)
                put("名称", name)
                remark?.let { put("备注", it) }
                if (pointIconPreset != null) {
                    put("icon", pointIconPreset.name)
                    put("图标", pointIconPreset.title)
                }
            }
            record.copy(
                siteName = name,
                cellName = name,
                longitude = longitude ?: record.longitude,
                latitude = latitude ?: record.latitude,
                rawFields = nextRawFields,
            )
        }
    }

    suspend fun moveUserRecord(
        layerId: String,
        recordId: String,
        longitude: Double,
        latitude: Double,
    ): LayerDocument? = onDisk {
        updateRecord(layerId, recordId) { record ->
            record.copy(
                longitude = longitude,
                latitude = latitude,
                rawFields = record.rawFields + mapOf(
                    "经度" to longitude.toString(),
                    "纬度" to latitude.toString(),
                ),
            )
        }
    }

    suspend fun deleteRecord(
        layerId: String,
        recordId: String,
    ): LayerDocument? = onDisk {
        val document = loadFullDocument(layerId) ?: return@onDisk null
        if (document.sectors.none { it.id == recordId }) return@onDisk null
        val sectors = document.sectors.filterNot { it.id == recordId }
        val updated = document.copy(meta = document.meta.copy(recordCount = sectors.size), sectors = sectors)
        saveFullDocument(updated)
        updated
    }

    suspend fun setVisibility(layerId: String, visible: Boolean) = onDisk {
        val current = documentsState.value.firstOrNull { it.meta.id == layerId } ?: return@onDisk
        val sectors = if (visible) loadFullDocument(layerId)?.sectors ?: return@onDisk else emptyList()
        val updated = current.copy(meta = current.meta.copy(isVisible = visible), sectors = sectors)
        publishDocuments(documentsState.value.map { if (it.meta.id == layerId) updated else it })
    }

    suspend fun deleteLayer(layerId: String) = onDisk {
        diskStore.deleteLayer(layerId)
        publishDocuments(documentsState.value.filterNot { it.meta.id == layerId })
        rasterAssetStore.delete(layerId)
    }

    suspend fun updateStyle(
        layerId: String,
        showLabels: Boolean,
        labelTextColor: String?,
        labelHaloColor: String?,
        pointIconPreset: LayerPointIconPreset,
        rasterColorHex: String? = null,
        rasterOpacity: Float? = null,
    ) = onDisk {
        val document = documentsState.value.firstOrNull { it.meta.id == layerId } ?: return@onDisk
        val nextRaster = document.meta.rasterInfo?.let { current ->
            current.copy(
                colorHex = rasterColorHex?.takeIf(String::isNotBlank) ?: current.colorHex,
                opacity = rasterOpacity?.coerceIn(0.05f, 1f) ?: current.opacity,
            )
        }
        nextRaster?.takeIf { it != document.meta.rasterInfo }?.let { rasterInfo ->
            rasterAssetStore.recolor(
                layerId = layerId,
                width = rasterInfo.width,
                height = rasterInfo.height,
                colorHex = rasterInfo.colorHex,
                opacity = rasterInfo.opacity,
            )
        }
        // Publish only after recoloring finishes, so the map cannot cache the previous PNG as the new style.
        val updated = document.copy(meta = document.meta.copy(
            showLabels = showLabels, labelTextColor = labelTextColor, labelHaloColor = labelHaloColor,
            pointIconPreset = pointIconPreset, rasterInfo = nextRaster,
        ))
        publishDocuments(documentsState.value.map { if (it.meta.id == layerId) updated else it })
    }

    suspend fun setAlwaysRenderAllRecords(
        layerId: String,
        enabled: Boolean,
    ) = onDisk {
        publishDocuments(documentsState.value.map { document ->
            if (document.meta.id == layerId) {
                document.copy(meta = document.meta.copy(alwaysRenderAllRecords = enabled))
            } else document
        })
    }

    suspend fun findLayer(layerId: String): LayerDocument? = onDisk {
        loadFullDocument(layerId)
    }

    private fun loadMetadataSkeleton(): List<LayerDocument> =
        sortedDocuments(
            diskStore.loadMetadata().map { metadata ->
                LayerDocument(
                    meta = metadata,
                    sectors = emptyList(),
                )
            },
        )

    private fun preloadVisibleLayers() {
        // Publish one initial catalog. Publishing each file rebuilt/queued the whole city repeatedly.
        val loaded = documentsState.value
            .filter { it.meta.isVisible && it.sectors.isEmpty() }
            .mapNotNull { document -> diskStore.loadLayer(document.meta)?.let { document.meta.id to it } }
            .toMap()
        documentsState.update { documents ->
            documents.map { current ->
                val document = loaded[current.meta.id]
                if (document != null && current.meta.isVisible && current.sectors.isEmpty()) {
                    document.copy(meta = current.meta)
                } else current
            }
        }
    }

    private fun refreshRasterAssetsIfNeeded() {
        documentsState.value.forEach { document ->
            val rasterInfo = document.meta.rasterInfo ?: return@forEach
            runCatching {
                if (!rasterAssetStore.needsRefresh(document.meta.id)) return@runCatching
                rasterAssetStore.recolor(
                    layerId = document.meta.id,
                    width = rasterInfo.width,
                    height = rasterInfo.height,
                    colorHex = rasterInfo.colorHex,
                    opacity = rasterInfo.opacity,
                )
            }
        }
    }

    private fun updateRecord(
        layerId: String,
        recordId: String,
        transform: (SectorRecord) -> SectorRecord,
    ): LayerDocument? {
        val document = loadFullDocument(layerId) ?: return null
        if (document.sectors.none { it.id == recordId }) return null
        val updated = document.copy(sectors = document.sectors.map { if (it.id == recordId) transform(it) else it })
        saveFullDocument(updated)
        return updated
    }

    private fun loadFullDocument(layerId: String): LayerDocument? {
        val current = documentsState.value.firstOrNull { it.meta.id == layerId } ?: return null
        return if (current.sectors.isNotEmpty() || current.meta.recordCount == 0) {
            current
        } else {
            diskStore.loadLayer(current.meta)
        }
    }

    private fun saveFullDocument(document: LayerDocument) {
        diskStore.saveLayer(document)
        val loaded = if (document.meta.isVisible) document else document.copy(sectors = emptyList())
        publishDocuments(documentsState.value.filterNot { it.meta.id == document.meta.id } + loaded)
    }

    private fun publishDocuments(documents: List<LayerDocument>) {
        val sorted = sortedDocuments(documents)
        diskStore.saveIndex(sorted.map { it.meta })
        documentsState.value = sorted
    }

    private suspend fun <T> onDisk(block: () -> T): T = withContext(Dispatchers.IO) {
        initialization.join()
        mutations.withLock { block() }
    }

    private fun sortedDocuments(documents: List<LayerDocument>): List<LayerDocument> =
        documents.sortedByDescending { it.meta.importedAtMillis }
}
