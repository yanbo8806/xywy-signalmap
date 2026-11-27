package com.xywy.signalmap.data.local

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Replace only after the complete new contents have reached storage. */
internal fun File.writeDurably(text: String) {
    parentFile?.mkdirs()
    val pending = File(parentFile, "$name.tmp")
    FileOutputStream(pending).use { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
        output.fd.sync()
    }
    Files.move(pending.toPath(), toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
}
