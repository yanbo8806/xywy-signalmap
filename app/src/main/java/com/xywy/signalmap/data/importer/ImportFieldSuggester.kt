package com.xywy.signalmap.data.importer

object ImportFieldSuggester {
    fun suggest(headers: List<String>): ImportFieldSuggestion {
        val mapping = ImportFieldMapping(
            longitudeHeader = findCanonical(headers, HeaderAliases.LONGITUDE),
            latitudeHeader = findCanonical(headers, HeaderAliases.LATITUDE),
            azimuthHeader = findCanonical(headers, HeaderAliases.AZIMUTH),
            nameHeader = findNameHeader(headers),
            siteIdHeader = findCanonical(headers, HeaderAliases.SITE_ID),
            cellIdHeader = findCellIdentityHeader(headers),
            pciHeader = findCanonical(headers, HeaderAliases.PCI),
            antennaHeightHeader = findCanonical(headers, HeaderAliases.ANTENNA_HEIGHT),
        )
        val detailHeaders = listOfNotNull(
            mapping.pciHeader,
            mapping.antennaHeightHeader,
            mapping.siteIdHeader,
            mapping.cellIdHeader,
        ).distinct()
        return ImportFieldSuggestion(
            mapping = mapping,
            defaultDetailHeaders = detailHeaders,
        )
    }

    private fun findNameHeader(headers: List<String>): String? {
        return headers.firstOrNull {
            HeaderAliases.canonical(it) == HeaderAliases.CELL_NAME
        } ?: headers.firstOrNull {
            HeaderAliases.canonical(it) == HeaderAliases.SITE_NAME
        }
    }

    private fun findCanonical(headers: List<String>, canonicalKey: String): String? {
        return headers.firstOrNull { HeaderAliases.canonical(it) == canonicalKey }
            ?: headers.firstOrNull { matchesFallback(it, canonicalKey) }
    }

    private fun findCellIdentityHeader(headers: List<String>): String? {
        return headers
            .filter { HeaderAliases.canonical(it) == HeaderAliases.CELL_ID }
            .maxByOrNull(HeaderAliases::cellIdentityPriority)
            ?: findCanonical(headers, HeaderAliases.CELL_ID)
    }

    private fun matchesFallback(header: String, canonicalKey: String): Boolean {
        val normalized = HeaderAliases.normalize(header)
        return when (canonicalKey) {
            HeaderAliases.SITE_ID -> listOf("SITEID", "基站ID", "站点ID", "ENODEBID", "GNODEBID")
                .map(HeaderAliases::normalize)
                .any { it == normalized }

            HeaderAliases.ANTENNA_HEIGHT -> listOf("站高", "挂高", "天线挂高")
                .map(HeaderAliases::normalize)
                .any { it == normalized }

            else -> false
        }
    }
}
