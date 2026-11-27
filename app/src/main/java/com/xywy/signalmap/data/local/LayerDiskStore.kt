package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.LayerMetadata
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class LayerDiskStore(
    private val rootDir: File,
    private val json: Json,
) {
    private val indexFile = File(rootDir, "index.json")

    init {
        rootDir.mkdirs()
    }

    fun loadMetadata(): List<LayerMetadata> {
        val indexed = runCatching {
            json.decodeFromString(ListSerializer(LayerMetadata.serializer()), indexFile.readText(Charsets.UTF_8))
        }.getOrDefault(emptyList()).filter { layerFile(it.id).isFile }
        val indexedIds = indexed.map { it.id }.toSet()
        // Recover completed files after an interrupted index write; ignore unfinished .tmp files.
        val orphaned = rootDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "json" && it != indexFile && it.nameWithoutExtension !in indexedIds }
            .mapNotNull { file ->
                runCatching {
                    json.decodeFromString(LayerDocument.serializer(), file.readText(Charsets.UTF_8)).meta
                        .takeIf { it.id == file.nameWithoutExtension }
                }.getOrNull()
            }
        return indexed + orphaned
    }

    fun saveIndex(metadata: List<LayerMetadata>) {
        indexFile.writeDurably(
            json.encodeToString(ListSerializer(LayerMetadata.serializer()), metadata),
        )
    }

    fun loadLayer(metadata: LayerMetadata): LayerDocument? {
        val file = layerFile(metadata.id)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(LayerDocument.serializer(), file.readText(Charsets.UTF_8)).copy(meta = metadata)
        }.getOrNull()
    }

    fun saveLayer(document: LayerDocument) {
        layerFile(document.meta.id).writeDurably(
            json.encodeToString(LayerDocument.serializer(), document),
        )
    }

    fun deleteLayer(layerId: String) {
        val file = layerFile(layerId)
        check(!file.exists() || file.delete()) { "无法删除图层" }
    }

    private fun layerFile(layerId: String): File = File(rootDir, "$layerId.json")
}
