package com.xywy.signalmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UiTextCatalogTest {
    @Test
    fun `map filter labels are readable chinese`() {
        assertEquals("\u56FE\u5C42\u7B5B\u9009", MapFilterText.title)
        assertEquals("\u6536\u8D77\u7B5B\u9009", MapFilterText.expandDescription(expanded = true))
        assertEquals("\u5C55\u5F00\u7B5B\u9009", MapFilterText.expandDescription(expanded = false))
        assertEquals("\u6247\u533A\u9762", MapFilterText.sectorFill)
        assertEquals("工参站点", MapFilterText.sitePoints)
        assertEquals("\u7AD9\u540D", MapFilterText.siteLabels)
        assertEquals("\u4E3B\u670D\u52A1\u7EBF", MapFilterText.servingLine)
        assertEquals("\u90BB\u533A\u7EBF", MapFilterText.neighborLine)
        assertEquals("\u5206\u5C4F", MapFilterText.splitMode)
    }

    @Test
    fun `map test record text stays readable chinese`() {
        assertEquals("\u6D4B\u8BD5\u8BB0\u5F55", MapTestRecordText.title)
        assertEquals("\u989C\u8272\uFF1A", MapTestRecordText.metricPrefix)
        assertEquals("\u5F00\u59CB", MapTestRecordText.startAction)
        assertEquals("\u505C\u6B62", MapTestRecordText.stopAction)
        assertEquals("\u663E\u793A\u5361\u69FD", MapTestRecordText.visibleSimTitle)
        assertEquals("\u6536\u8D77\u6D4B\u8BD5\u8BB0\u5F55", MapTestRecordText.expandDescription(expanded = true))
        assertEquals("\u5C55\u5F00\u6D4B\u8BD5\u8BB0\u5F55", MapTestRecordText.expandDescription(expanded = false))
        assertEquals("\u5F55\u5236\u4E2D \u00B7 \u5F53\u524D\u5DF2\u8BB0\u5F55 12 \u4E2A\u70B9", MapTestRecordText.recordingSummary(12))
        assertEquals("\u5DF2\u8F7D\u5165 3 \u6761\u53EF\u89C1\u65E5\u5FD7", MapTestRecordText.loadedSummary(3))
    }

    @Test
    fun `import text reflects loading state and file name`() {
        assertEquals("\u5BFC\u5165\u6587\u4EF6", ImportUiText.actionLabel(isImporting = false))
        assertEquals("\u5BFC\u5165\u4E2D...", ImportUiText.actionLabel(isImporting = true))
        assertEquals("\u652F\u6301 xlsx / kml / xywy \u5BFC\u5165", ImportUiText.idleHint)

        val loadingMessage = ImportUiText.loadingHint(fileName = "sample.xlsx")
        assertTrue(loadingMessage.contains("sample.xlsx"))
        assertTrue(loadingMessage.contains("5\u4E07\u884C"))
    }

    @Test
    fun `profile text stays readable chinese`() {
        assertEquals("\u6743\u9650\u4E0E\u5BFC\u5165", ProfileUiText.permissionImportTitle)
        assertEquals("\u5B9A\u4F4D\u6743\u9650", ProfileUiText.locationPermissionLabel)
        assertEquals("\u7535\u8BDD\u6743\u9650", ProfileUiText.phonePermissionLabel)
        assertEquals("\u7533\u8BF7\u6743\u9650", ProfileUiText.requestPermissionAction)
        assertEquals("\u5BFC\u5165\u65F6\u6309 GCJ-02 \u8F6C WGS-84", ProfileUiText.importCoordinateToggleTitle)
        assertEquals(
            "\u9AD8\u5FB7/\u5FAE\u4FE1\u5E38\u89C1\u5DE5\u53C2\u5750\u6807\u53EF\u5F00\u542F\uFF1B\u5FAE\u4FE1\u5916\u90E8\u6253\u5F00\u540C\u6837\u4F7F\u7528\u8FD9\u4E2A\u9009\u9879",
            ProfileUiText.importCoordinateToggleSummary,
        )
        assertEquals("\u5DF2\u5BFC\u5165\u56FE\u5C42", ProfileUiText.importedLayersTitle)
        assertEquals("\u53CC\u5361\u5DE5\u53C2\u5339\u914D", ProfileUiText.simBindingTitle)
        assertEquals(
            "\u53EF\u6309 SIM1 / SIM2 \u5206\u522B\u6307\u5B9A NR \u548C LTE \u5339\u914D\u7684\u5DE5\u53C2\u56FE\u5C42\uFF0C\u6216\u8005\u5173\u95ED\u5DE5\u53C2\u5339\u914D",
            ProfileUiText.simBindingHint,
        )
        assertEquals("\u81EA\u52A8\u5339\u914D\u5168\u90E8\u5DE5\u53C2", ProfileUiText.autoLayerBindingLabel)
        assertEquals("\u4E0D\u4F7F\u7528\u5DE5\u53C2", ProfileUiText.disabledLayerBindingLabel)
        assertEquals("NR \u5DE5\u53C2", ProfileUiText.nrBindingLabel)
        assertEquals("LTE \u5DE5\u53C2", ProfileUiText.lteBindingLabel)
        assertEquals("\u8FD8\u6CA1\u6709\u53EF\u7528\u7684 4G/5G \u5DE5\u53C2\u56FE\u5C42", ProfileUiText.noEngineeringLayerHint)
        assertEquals("\u5E95\u56FE\u5207\u6362", ProfileUiText.tilePresetTitle)
        assertEquals("\u9009\u62E9\u5E95\u56FE", ProfileUiText.tilePresetFieldLabel)
        assertEquals("\u4FE1\u53F7\u6D4B\u8BD5 \u00B7 \u57FA\u7AD9\u5730\u56FE \u00B7 \u5DE5\u53C2\u7BA1\u7406", ProfileUiText.appSubtitle)
        assertEquals("\u6761\u8BB0\u5F55 \u00B7 ", ProfileUiText.layerRecordSummarySeparator)
    }

    @Test
    fun `cell neighbor text stays readable chinese`() {
        assertEquals("\u90BB\u533A", CellNeighborText.sectionTitle)
        assertEquals("\u5F53\u524D\u672A\u8BFB\u53D6\u5230 NR/LTE \u90BB\u533A\u4FE1\u606F", CellNeighborText.emptyMessage)
        assertEquals("NR \u90BB\u533A (3)", CellNeighborText.groupTitle("NR", 3))
        assertEquals("\u90BB\u533A\u540D\u79F0", CellNeighborText.nameTitle)
        assertEquals("\u70B9\u51FB\u5361\u7247\u53EF\u67E5\u770B\u5E76\u590D\u5236", CellNeighborText.cardHint)
        assertEquals("\u9891\u70B9", CellNeighborText.frequencyLabel)
        assertEquals("\u90BB\u533A\u8BE6\u60C5", CellNeighborText.detailTitle)
        assertEquals("\u590D\u5236", CellNeighborText.copyAction)
        assertEquals("\u5173\u95ED", CellNeighborText.closeAction)
        assertEquals("\u90BB\u533A\u540D\u79F0\uFF1A\u793A\u4F8B\u5730\u533A\u793A\u4F8B\u533A-1", CellNeighborText.detailNameLine("\u793A\u4F8B\u5730\u533A\u793A\u4F8B\u533A-1"))
    }
}
