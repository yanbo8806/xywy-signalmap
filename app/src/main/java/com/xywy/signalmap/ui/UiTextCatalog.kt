package com.xywy.signalmap.ui

object MapFilterText {
    const val title = "\u56FE\u5C42\u7B5B\u9009"
    const val sectorFill = "\u6247\u533A\u9762"
    const val sitePoints = "工参站点"
    const val siteLabels = "\u7AD9\u540D"
    const val servingLine = "\u4E3B\u670D\u52A1\u7EBF"
    const val neighborLine = "\u90BB\u533A\u7EBF"
    const val overlayMode = "\u53E0\u52A0"
    const val splitMode = "\u5206\u5C4F"

    fun expandDescription(expanded: Boolean): String = if (expanded) "\u6536\u8D77\u7B5B\u9009" else "\u5C55\u5F00\u7B5B\u9009"
}

object MapTestRecordText {
    const val title = "\u6D4B\u8BD5\u8BB0\u5F55"
    const val metricPrefix = "\u989C\u8272\uFF1A"
    const val startAction = "\u5F00\u59CB"
    const val stopAction = "\u505C\u6B62"
    const val visibleSimTitle = "\u663E\u793A\u5361\u69FD"

    fun expandDescription(expanded: Boolean): String =
        if (expanded) "\u6536\u8D77\u6D4B\u8BD5\u8BB0\u5F55" else "\u5C55\u5F00\u6D4B\u8BD5\u8BB0\u5F55"

    fun recordingSummary(sampleCount: Int): String =
        "\u5F55\u5236\u4E2D \u00B7 \u5F53\u524D\u5DF2\u8BB0\u5F55 $sampleCount \u4E2A\u70B9"

    fun loadedSummary(visibleLogCount: Int): String =
        "\u5DF2\u8F7D\u5165 $visibleLogCount \u6761\u53EF\u89C1\u65E5\u5FD7"
}

object MapToolText {
    const val title = "\u5DE5\u5177"
    const val distance = "\u8DDD\u79BB"
    const val area = "\u9762\u79EF"
    const val finish = "\u7ED3\u675F"
    const val clear = "\u6E05\u9664"
    const val idleHint = "\u542F\u52A8\u6D4B\u91CF\u540E\uFF0C\u5355\u51FB\u5730\u56FE\u52A0\u70B9\uFF0C\u957F\u6309\u4ECD\u53EF\u67E5\u770B\u57FA\u7AD9\u8BE6\u60C5"

    fun expandDescription(expanded: Boolean): String =
        if (expanded) "\u6536\u8D77\u5DE5\u5177" else "\u5C55\u5F00\u5DE5\u5177"
}

object ImportUiText {
    const val idleHint = "\u652F\u6301 xlsx / kml / xywy \u5BFC\u5165"

    fun actionLabel(isImporting: Boolean): String = if (isImporting) "\u5BFC\u5165\u4E2D..." else "\u5BFC\u5165\u6587\u4EF6"

    fun loadingHint(fileName: String?): String {
        val target = fileName?.takeIf { it.isNotBlank() } ?: "\u5F53\u524D\u6587\u4EF6"
        return "\u6B63\u5728\u5BFC\u5165 " + target + "\uFF0C\u8BF7\u7A0D\u5019 (5\u4E07\u884C\u53EF\u80FD\u8F83\u6162)"
    }
}

object CellNeighborText {
    const val sectionTitle = "\u90BB\u533A"
    const val emptyMessage = "\u5F53\u524D\u672A\u8BFB\u53D6\u5230 NR/LTE \u90BB\u533A\u4FE1\u606F"
    const val nameTitle = "\u90BB\u533A\u540D\u79F0"
    const val cardHint = "\u70B9\u51FB\u5361\u7247\u53EF\u67E5\u770B\u5E76\u590D\u5236"
    const val frequencyLabel = "\u9891\u70B9"
    const val detailTitle = "\u90BB\u533A\u8BE6\u60C5"
    const val copyAction = "\u590D\u5236"
    const val closeAction = "\u5173\u95ED"

    fun groupTitle(technologyLabel: String, count: Int): String =
        "$technologyLabel \u90BB\u533A ($count)"

    fun detailNameLine(name: String): String =
        "\u90BB\u533A\u540D\u79F0\uFF1A$name"
}

object ProfileUiText {
    const val appSubtitle = "\u4FE1\u53F7\u6D4B\u8BD5 \u00B7 \u57FA\u7AD9\u5730\u56FE \u00B7 \u5DE5\u53C2\u7BA1\u7406"
    const val permissionImportTitle = "\u6743\u9650\u4E0E\u5BFC\u5165"
    const val locationPermissionLabel = "\u5B9A\u4F4D\u6743\u9650"
    const val phonePermissionLabel = "\u7535\u8BDD\u6743\u9650"
    const val requestPermissionAction = "\u7533\u8BF7\u6743\u9650"
    const val importCoordinateToggleTitle = "\u5BFC\u5165\u65F6\u6309 GCJ-02 \u8F6C WGS-84"
    const val importCoordinateToggleSummary = "\u9AD8\u5FB7/\u5FAE\u4FE1\u5E38\u89C1\u5DE5\u53C2\u5750\u6807\u53EF\u5F00\u542F\uFF1B\u5FAE\u4FE1\u5916\u90E8\u6253\u5F00\u540C\u6837\u4F7F\u7528\u8FD9\u4E2A\u9009\u9879"
    const val importedLayersTitle = "\u5DF2\u5BFC\u5165\u56FE\u5C42"
    const val simBindingTitle = "\u53CC\u5361\u5DE5\u53C2\u5339\u914D"
    const val simBindingHint = "\u53EF\u6309 SIM1 / SIM2 \u5206\u522B\u6307\u5B9A NR \u548C LTE \u5339\u914D\u7684\u5DE5\u53C2\u56FE\u5C42\uFF0C\u6216\u8005\u5173\u95ED\u5DE5\u53C2\u5339\u914D"
    const val autoLayerBindingLabel = "\u81EA\u52A8\u5339\u914D\u5168\u90E8\u5DE5\u53C2"
    const val disabledLayerBindingLabel = "\u4E0D\u4F7F\u7528\u5DE5\u53C2"
    const val nrBindingLabel = "NR \u5DE5\u53C2"
    const val lteBindingLabel = "LTE \u5DE5\u53C2"
    const val noEngineeringLayerHint = "\u8FD8\u6CA1\u6709\u53EF\u7528\u7684 4G/5G \u5DE5\u53C2\u56FE\u5C42"
    const val layerRecordSummarySeparator = "\u6761\u8BB0\u5F55 \u00B7 "
    const val tilePresetTitle = "\u5E95\u56FE\u5207\u6362"
    const val tilePresetFieldLabel = "\u9009\u62E9\u5E95\u56FE"
}
