package com.xywy.signalmap.data.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderAliasTest {
    @Test
    fun `old dex header aliases map to canonical names`() {
        assertEquals(HeaderAliases.RADIO_TECH, HeaderAliases.canonical("NetType"))
        assertEquals(HeaderAliases.SITE_NAME, HeaderAliases.canonical("BTSName"))
        assertEquals(HeaderAliases.AZIMUTH, HeaderAliases.canonical("Azimuth"))
        assertEquals(HeaderAliases.LONGITUDE, HeaderAliases.canonical("BLon"))
        assertEquals(HeaderAliases.LATITUDE, HeaderAliases.canonical("BLat"))
    }

    @Test
    fun `45g workbook headers map to canonical names`() {
        assertEquals(HeaderAliases.CITY, HeaderAliases.canonical("地市"))
        assertEquals(HeaderAliases.SITE_NAME, HeaderAliases.canonical("网元名称"))
        assertEquals(HeaderAliases.SITE_ID, HeaderAliases.canonical("基站编号"))
        assertEquals(HeaderAliases.CELL_ID, HeaderAliases.canonical("ECI（唯一标识列）"))
        assertEquals(HeaderAliases.CELL_NAME, HeaderAliases.canonical("小区名称"))
        assertEquals(HeaderAliases.LONGITUDE, HeaderAliases.canonical("经度"))
        assertEquals(HeaderAliases.LATITUDE, HeaderAliases.canonical("纬度"))
        assertEquals(HeaderAliases.AZIMUTH, HeaderAliases.canonical("方位角（度）"))
        assertEquals(HeaderAliases.TAC, HeaderAliases.canonical("TAC(十进制)"))
        assertEquals(HeaderAliases.BAND, HeaderAliases.canonical("频带指示"))
        assertEquals(HeaderAliases.BANDWIDTH, HeaderAliases.canonical("小区带宽"))
        assertEquals(HeaderAliases.VENDOR, HeaderAliases.canonical("设备厂家"))
        assertEquals(HeaderAliases.ANTENNA_HEIGHT, HeaderAliases.canonical("天线挂高（m）"))
    }
}
