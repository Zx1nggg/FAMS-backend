package com.Zx1nggg.FAMS.modules.regulator.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.modules.regulator.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 监管方 Dashboard + 追溯 API
 */
@Tag(name = "监管方工作台")
@RestController
@RequestMapping("/regulator")
public class RegulatorController {

    @Autowired
    private IRegulatorService regulatorService;

    // ==================== Dashboard ====================

    @Operation(summary = "监管大屏 — 宏观统计")
    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsVO> getDashboardStats() {
        return Result.success(regulatorService.getDashboardStats());
    }

    @Operation(summary = "监管大屏 — 未处理告警列表")
    @GetMapping("/dashboard/alerts")
    public Result<List<DashboardAlertVO>> getDashboardAlerts(
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return Result.success(regulatorService.getDashboardAlerts(limit));
    }

    @Operation(summary = "监管大屏 — 重点督办名单")
    @GetMapping("/dashboard/watchlist")
    public Result<List<DashboardWatchlistVO>> getDashboardWatchlist(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(regulatorService.getDashboardWatchlist(limit));
    }

    // ==================== GIS ====================

    @Operation(summary = "GIS 养殖场地理分布")
    @GetMapping("/farms/geo")
    public Result<List<FarmGeoVO>> getFarmsGeo() {
        return Result.success(regulatorService.getFarmsGeo());
    }

    // ==================== Trace ====================

    @Log(title = "监管追溯查询", businessType = 0)
    @Operation(summary = "溯源码快速查询 (按批次号/溯源码关键字)")
    @GetMapping("/trace/quick")
    public Result<TraceChainVO> quickTrace(@RequestParam String keyword) {
        return Result.success(regulatorService.quickTrace(keyword));
    }

    @Operation(summary = "全链路追溯详情 (按批次号)")
    @GetMapping("/trace/detail")
    public Result<TraceChainVO> getTraceDetail(@RequestParam String batchNo) {
        return Result.success(regulatorService.getTraceDetail(batchNo));
    }
}
