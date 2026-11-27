package com.xywy.signalmap.data.importer

object HeaderAliases {
    const val RADIO_TECH = "radioTech"
    const val SITE_ID = "siteId"
    const val SITE_NAME = "siteName"
    const val CELL_ID = "cellId"
    const val CELL_NAME = "cellName"
    const val PCI = "pci"
    const val TAC = "tac"
    const val CHANNEL_NUMBER = "channelNumber"
    const val BAND = "band"
    const val BANDWIDTH = "bandwidth"
    const val AZIMUTH = "azimuth"
    const val LONGITUDE = "longitude"
    const val LATITUDE = "latitude"
    const val OPERATOR = "operator"
    const val VENDOR = "vendor"
    const val CITY = "city"
    const val PROVINCE = "province"
    const val ANTENNA_HEIGHT = "antennaHeight"

    private val aliasToCanonical = listOf(
        "NetType" to RADIO_TECH,
        "网络制式" to RADIO_TECH,
        "制式" to RADIO_TECH,
        "网络" to RADIO_TECH,
        "组网方式" to RADIO_TECH,
        "组网方式（制式）" to RADIO_TECH,
        "RAT" to RADIO_TECH,

        "BTSID" to SITE_ID,
        "基站ID" to SITE_ID,
        "站号" to SITE_ID,
        "站点ID" to SITE_ID,
        "基站编号" to SITE_ID,
        "eNODEB ID" to SITE_ID,
        "gNODEB ID" to SITE_ID,
        "ENODEBID" to SITE_ID,
        "GNODEBID" to SITE_ID,
        "NODEBID" to SITE_ID,

        "BTSName" to SITE_NAME,
        "基站名" to SITE_NAME,
        "站点名称" to SITE_NAME,
        "基站名称" to SITE_NAME,
        "站名" to SITE_NAME,
        "网元名称" to SITE_NAME,

        "CELLID" to CELL_ID,
        "CI" to CELL_ID,
        "ECI" to CELL_ID,
        "NCI" to CELL_ID,
        "小区CI" to CELL_ID,
        "CIDCI" to CELL_ID,
        "NCI" to CELL_ID,
        "ECI（唯一标识列）" to CELL_ID,
        "ECI(唯一标识列)" to CELL_ID,
        "CGI(小区唯一标识)" to CELL_ID,
        "小区ID" to CELL_ID,
        "扇区ID" to CELL_ID,
        "NR DU小区ID" to CELL_ID,
        "NRDU小区ID" to CELL_ID,
        "LocalID" to CELL_ID,

        "CELLNAME" to CELL_NAME,
        "SECTORNAME" to CELL_NAME,
        "小区名称" to CELL_NAME,
        "扇区名称" to CELL_NAME,
        "小区名" to CELL_NAME,

        "PCI" to PCI,
        "PSC" to PCI,
        "BSIC" to PCI,
        "PN" to PCI,

        "NRARFCN" to CHANNEL_NUMBER,
        "EARFCN" to CHANNEL_NUMBER,
        "UARFCN" to CHANNEL_NUMBER,
        "ARFCN" to CHANNEL_NUMBER,
        "DLARFCN" to CHANNEL_NUMBER,
        "\u9891\u70B9" to CHANNEL_NUMBER,
        "\u4E0B\u884C\u9891\u70B9" to CHANNEL_NUMBER,
        "\u4E2D\u5FC3\u9891\u70B9" to CHANNEL_NUMBER,

        "TAC" to TAC,
        "LAC" to TAC,
        "TRACKINGAREA" to TAC,
        "TAC(十进制)" to TAC,

        "BAND" to BAND,
        "频带指示" to BAND,
        "频段" to BAND,

        "BANDWIDTH" to BANDWIDTH,
        "带宽" to BANDWIDTH,
        "小区带宽" to BANDWIDTH,

        "AZIMUTH" to AZIMUTH,
        "方位角" to AZIMUTH,
        "方位角（度）" to AZIMUTH,

        "LONGITUDE" to LONGITUDE,
        "LON" to LONGITUDE,
        "经度" to LONGITUDE,
        "BLon" to LONGITUDE,

        "LATITUDE" to LATITUDE,
        "LAT" to LATITUDE,
        "纬度" to LATITUDE,
        "BLat" to LATITUDE,

        "CITY" to CITY,
        "地市" to CITY,
        "城市" to CITY,

        "PROVINCE" to PROVINCE,
        "省份" to PROVINCE,
        "省" to PROVINCE,

        "OPERATOR" to OPERATOR,
        "运营商" to OPERATOR,

        "FACTORY" to VENDOR,
        "厂家" to VENDOR,
        "设备厂家" to VENDOR,
        "VENDOR" to VENDOR,

        "ANTHEIGHT" to ANTENNA_HEIGHT,
        "站高" to ANTENNA_HEIGHT,
        "天线挂高" to ANTENNA_HEIGHT,
        "天线挂高（m）" to ANTENNA_HEIGHT,
        "挂高" to ANTENNA_HEIGHT,
    ).associate { normalize(it.first) to it.second }

    fun normalize(header: String): String = header
        .trim()
        .replace("（", "")
        .replace("）", "")
        .replace("(", "")
        .replace(")", "")
        .replace("[", "")
        .replace("]", "")
        .replace("_", "")
        .replace("-", "")
        .replace("/", "")
        .replace(" ", "")
        .uppercase()

    private val simplePointNameAliases = setOf(
        normalize("NAME"),
        normalize("\u540d\u79f0"),
        normalize("\u70b9\u4f4d\u540d\u79f0"),
        normalize("\u70b9\u540d"),
    )

    fun canonical(header: String): String? {
        val normalized = normalize(header)
        return aliasToCanonical[normalized]
            ?: SITE_NAME.takeIf { normalized in simplePointNameAliases }
    }

    fun cellIdentityPriority(header: String): Int {
        val normalized = normalize(header)
        return when {
            normalized.contains("ECI") -> 100
            normalized.contains("NCI") -> 100
            normalized == normalize("小区CI") -> 95
            normalized == normalize("CI") -> 90
            normalized.contains("CGI") -> 20
            normalized.contains("LOCAL") -> 10
            normalized.contains("DU小区ID") -> 10
            normalized.contains("小区标识") -> 10
            else -> 50
        }
    }
}
