package com.xywy.signalmap.data.tile

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class MbtilesCacheStore(
    rootDir: File,
    private val maxTileCount: Int = DEFAULT_MAX_TILE_COUNT,
    private val trimCheckIntervalWrites: Int = DEFAULT_TRIM_CHECK_INTERVAL_WRITES,
) : TileCacheStore, AutoCloseable {
    private val databaseFile = File(rootDir, "xywy_tiles.mbtiles")
    private val databaseLock = Any()
    @Volatile
    private var database: SQLiteDatabase? = null
    private val writesSinceLastTrimCheck = AtomicInteger(0)

    init {
        rootDir.mkdirs()
    }

    override fun read(key: TileCacheKey): TilePayload? = database().query(
        "tiles",
        arrayOf("tile_data", "content_type"),
        "source = ? AND zoom_level = ? AND tile_column = ? AND tile_row = ?",
        arrayOf(key.sourceId, key.z.toString(), key.x.toString(), key.y.toString()),
        null,
        null,
        null,
        "1",
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        TilePayload(
            bytes = cursor.getBlob(0),
            contentType = cursor.getString(1),
        )
    }

    override fun write(key: TileCacheKey, payload: TilePayload, fetchedAtMillis: Long) {
        val db = database()
        db.insertWithOnConflict(
            "tiles",
            null,
            ContentValues().apply {
                put("source", key.sourceId)
                put("zoom_level", key.z)
                put("tile_column", key.x)
                put("tile_row", key.y)
                put("tile_data", payload.bytes)
                put("content_type", payload.contentType)
                put("fetched_at", fetchedAtMillis)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        maybeTrimCache(db)
    }

    override fun close() {
        synchronized(databaseLock) {
            database?.close()
            database = null
        }
    }

    private fun database(): SQLiteDatabase {
        database?.takeIf(SQLiteDatabase::isOpen)?.let { return it }
        synchronized(databaseLock) {
            database?.takeIf(SQLiteDatabase::isOpen)?.let { return it }
            return SQLiteDatabase.openOrCreateDatabase(databaseFile, null).also { opened ->
                try {
                    opened.enableWriteAheadLogging()
                    initializeSchema(opened)
                    database = opened
                } catch (error: Exception) {
                    opened.close()
                    throw error
                }
            }
        }
    }

    private fun maybeTrimCache(database: SQLiteDatabase) {
        if (maxTileCount <= 0) return
        if (writesSinceLastTrimCheck.incrementAndGet() < trimCheckIntervalWrites) return
        writesSinceLastTrimCheck.set(0)
        trimCache(database, maxTileCount)
    }

    private fun trimCache(database: SQLiteDatabase, maxTileCount: Int) {
        database.execSQL(
            """
            DELETE FROM tiles
            WHERE rowid IN (
                SELECT rowid FROM tiles
                ORDER BY fetched_at ASC
                LIMIT max((SELECT COUNT(*) FROM tiles) - ?, 0)
            )
            """.trimIndent(),
            arrayOf(maxTileCount),
        )
    }

    private fun initializeSchema(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS metadata (
                name TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tiles (
                source TEXT NOT NULL,
                zoom_level INTEGER NOT NULL,
                tile_column INTEGER NOT NULL,
                tile_row INTEGER NOT NULL,
                tile_data BLOB NOT NULL,
                content_type TEXT NOT NULL,
                fetched_at INTEGER NOT NULL,
                PRIMARY KEY(source, zoom_level, tile_column, tile_row)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tiles_source_zoom ON tiles(source, zoom_level)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tiles_fetched_at ON tiles(fetched_at)",
        )
        database.insertWithOnConflict(
            "metadata",
            null,
            ContentValues().apply {
                put("name", "name")
                put("value", "XYWY tile cache")
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        database.insertWithOnConflict(
            "metadata",
            null,
            ContentValues().apply {
                put("name", "format")
                put("value", "mixed")
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
    }
    companion object {
        private const val DEFAULT_MAX_TILE_COUNT = 80_000
        private const val DEFAULT_TRIM_CHECK_INTERVAL_WRITES = 256
    }
}
