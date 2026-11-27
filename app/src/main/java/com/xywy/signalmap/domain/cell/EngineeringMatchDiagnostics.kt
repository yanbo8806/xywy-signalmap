package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*

object EngineeringMatchDiagnostics {
    fun describe(
        signal: CellSignal?,
        slot: Int,
        bindings: SimEngineeringBindings,
        index: SectorMatchIndex,
        currentLocation: LocationFix? = null,
    ): MatchDiagnostic {
        if (signal == null) return MatchDiagnostic("无测量", message = "当前没有服务小区信息，不能判断工参是否匹配")
        val binding = bindings.forSignal(slot, signal.radioTechnology)
        if (binding.mode == EngineeringLayerBindingMode.DISABLED)
            return MatchDiagnostic("已禁用", message = "此卡此制式的工参匹配已关闭")
        val scope = index.scope(slot, signal.radioTechnology, bindings)
        val scopeName = if (binding.mode == EngineeringLayerBindingMode.SPECIFIC_LAYER) "指定图层" else "自动匹配范围"
        if (scope.all.isEmpty()) return MatchDiagnostic("无可用工参", message = "$scopeName 暂无此制式记录；请检查加载、图层显示和绑定设置")
        if (!EngineeringMatchDistance.hasValidLocation(currentLocation))
            return MatchDiagnostic("等待定位", message = "暂无有效定位，无法确认工参是否在 5 km 内，暂不匹配名称和拉线")
        val exactCandidates = scope.byCellId(signal.cellId)
        val exact = exactCandidates.filter { EngineeringMatchDistance.isWithinRange(currentLocation, it) }
        if (exact.isNotEmpty()) return MatchDiagnostic("ECI/NCI 精确匹配", exact.size,
            if (exact.size == 1) "$scopeName 中找到唯一小区标识（含 CGI/站号换算）" else "小区标识命中 ${exact.size} 条，已按频段、TAC、PCI和位置择优；请核查重号工参")
        val pciCandidates = scope.byPci(signal.pci)
        val pci = pciCandidates.filter { EngineeringMatchDistance.isWithinRange(currentLocation, it) }
        if (pci.isNotEmpty()) return MatchDiagnostic("PCI 推测", pci.size,
            "5 km 内 ECI/NCI 未命中，仅按 PCI 从 ${pci.size} 条候选择优；PCI 可复用，结果需核实")
        if (exactCandidates.isNotEmpty() || pciCandidates.isNotEmpty())
            return MatchDiagnostic("超出匹配范围", message = "同标识或 PCI 的工参均不在当前位置 5 km 内或坐标无效，暂不匹配名称和拉线")
        return MatchDiagnostic("未匹配", message = "$scopeName 未找到对应 ECI/NCI 或 PCI；请核查工参缺失、制式和字段映射")
    }
}
