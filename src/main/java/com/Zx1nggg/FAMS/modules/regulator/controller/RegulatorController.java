package com.Zx1nggg.FAMS.modules.regulator.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.Zx1nggg.FAMS.modules.regulator.dto.AlertHandleDTO;
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.modules.regulator.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Regulator")
@RestController
@RequestMapping("/regulator")
public class RegulatorController {

    @Autowired
    private IRegulatorService regulatorService;

    @Operation(summary = "Dashboard stats")
    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsVO> getDashboardStats() {
        return Result.success(regulatorService.getDashboardStats());
    }

    @Operation(summary = "Dashboard active alerts")
    @GetMapping("/dashboard/alerts")
    public Result<List<DashboardAlertVO>> getDashboardAlerts(
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return Result.success(regulatorService.getDashboardAlerts(limit));
    }

    @Operation(summary = "Dashboard watchlist")
    @GetMapping("/dashboard/watchlist")
    public Result<List<DashboardWatchlistVO>> getDashboardWatchlist(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(regulatorService.getDashboardWatchlist(limit));
    }

    @Operation(summary = "Farm GIS data")
    @GetMapping("/farms/geo")
    public Result<List<FarmGeoVO>> getFarmsGeo() {
        return Result.success(regulatorService.getFarmsGeo());
    }

    @Operation(summary = "Alert stats")
    @GetMapping("/alerts/stats")
    public Result<AlertStatsVO> getAlertStats() {
        return Result.success(regulatorService.getAlertStats());
    }

    @Operation(summary = "Alert list")
    @GetMapping("/alerts/list")
    public Result<Page<AlarmRecordVO>> listAlerts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Byte severity,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(regulatorService.listAlerts(pageNum, pageSize, farmId, severity, status, sourceType, startDate, endDate));
    }

    @Operation(summary = "Alert trend")
    @GetMapping("/alerts/trend")
    public Result<List<AlertTrendVO>> getAlertTrend(@RequestParam(required = false, defaultValue = "30") Integer days) {
        return Result.success(regulatorService.getAlertTrend(days));
    }

    @Operation(summary = "Realtime IoT alerts")
    @GetMapping("/iot/realtime-alerts")
    public Result<List<IotRealtimeAlertVO>> getIotRealtimeAlerts() {
        return Result.success(regulatorService.getIotRealtimeAlerts());
    }

    @Log(title = "Regulator alert handling", businessType = 2)
    @Operation(summary = "Handle alert")
    @PutMapping("/alerts/{id}/handle")
    public Result<String> handleAlert(@PathVariable Long id, @Valid @RequestBody AlertHandleDTO dto) {
        regulatorService.handleAlert(id, dto.getStatus(), dto.getRemark());
        return Result.success("Alert status updated");
    }

    @Log(title = "Regulator trace query", businessType = 0)
    @Operation(summary = "Quick trace")
    @GetMapping("/trace/quick")
    public Result<TraceChainVO> quickTrace(@RequestParam String keyword) {
        return Result.success(regulatorService.quickTrace(keyword));
    }

    @Operation(summary = "Trace detail")
    @GetMapping("/trace/detail")
    public Result<TraceChainVO> getTraceDetail(@RequestParam String batchNo) {
        return Result.success(regulatorService.getTraceDetail(batchNo));
    }
}