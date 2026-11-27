package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord

internal fun SectorRecord.cellIdentityAliases(): Set<String> {
    val aliases = linkedSetOf<String>()
    val rawCellId = cellId.normalizedIdentityOrNull()
    rawCellId?.let(aliases::add)

    val localFromCellId = rawCellId?.toLongOrNull()
    val cgiParts = if (localFromCellId == null) rawCellId?.extractCgiSiteAndLocal() else null
    if (cgiParts != null) {
        aliases += cgiParts.localId.toString()
        deriveGlobalCellId(
            technology = radioTechnology,
            siteId = cgiParts.siteId,
            localCellId = cgiParts.localId,
        )?.let(aliases::add)
    }

    val siteFromSiteId = siteId.normalizedIdentityOrNull()?.toLongOrNull()
    if (localFromCellId != null && siteFromSiteId != null) {
        deriveGlobalCellId(
            technology = radioTechnology,
            siteId = siteFromSiteId,
            localCellId = localFromCellId,
        )?.let(aliases::add)
    }

    return aliases
}

internal fun String?.normalizedIdentityOrNull(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.uppercase()

private fun String.extractCgiSiteAndLocal(): CgiParts? {
    val groups = CGI_NUMBER_GROUPS
        .findAll(this)
        .mapNotNull { it.value.toLongOrNull() }
        .toList()
    if (groups.size < 4) return null
    return CgiParts(
        siteId = groups[groups.size - 2],
        localId = groups.last(),
    )
}

private fun deriveGlobalCellId(
    technology: RadioTechnology,
    siteId: Long,
    localCellId: Long,
): String? {
    val multiplier = when (technology) {
        RadioTechnology.LTE -> 256L
        RadioTechnology.NR -> 4096L
        else -> return null
    }
    if (localCellId !in 0 until multiplier) return null
    return (siteId * multiplier + localCellId).toString()
}

private data class CgiParts(
    val siteId: Long,
    val localId: Long,
)

private val CGI_NUMBER_GROUPS = Regex("""\d+""")
