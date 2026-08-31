package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.modules.regulator.vo.ProductionRankingVO;
import com.Zx1nggg.FAMS.modules.regulator.vo.ProductionStatsVO;
import com.Zx1nggg.FAMS.modules.regulator.vo.SurvivalRateVO;
import com.Zx1nggg.FAMS.modules.regulator.vo.SurvivalTrendVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "农户经营统计分析")
@RestController
@RequestMapping("/farmer/analysis")
public class FarmerAnalysisController {

    @Autowired
    private IRegulatorService regulatorService;

    @Operation(summary = "农户成活率统计")
    @GetMapping("/survival-rate")
    public Result<List<SurvivalRateVO>> getSurvivalRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long seedlingId,
            @RequestParam(required = false, defaultValue = "batch") String groupBy) {
        return Result.success(regulatorService.getSurvivalRate(startDate, endDate, currentFarmId(), seedlingId, groupBy));
    }

    @Operation(summary = "农户成活率趋势")
    @GetMapping("/survival-trend")
    public Result<List<SurvivalTrendVO>> getSurvivalTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long seedlingId) {
        return Result.success(regulatorService.getSurvivalTrend(startDate, endDate, currentFarmId(), seedlingId));
    }

    @Operation(summary = "农户产销经营统计")
    @GetMapping("/production-stats")
    public Result<ProductionStatsVO> getProductionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long seedlingId) {
        return Result.success(regulatorService.getProductionStats(startDate, endDate, currentFarmId(), seedlingId));
    }

    @Operation(summary = "农户批次/场区产量排名")
    @GetMapping("/production-ranking")
    public Result<List<ProductionRankingVO>> getProductionRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long seedlingId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(regulatorService.getProductionRanking(startDate, endDate, currentFarmId(), seedlingId, limit));
    }

    @Log(title = "农户经营统计报表导出", businessType = 4)
    @Operation(summary = "导出农户经营统计报表")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAnalysis(
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long seedlingId) {
        byte[] bytes = regulatorService.exportAnalysis(type, startDate, endDate, currentFarmId(), seedlingId);
        String filename = "FAMS-farmer-analysis-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private Long currentFarmId() {
        Long farmId = SecurityUtils.getCurrentFarmId();
        if (farmId == null) {
            throw new BusinessException(401, "当前养殖场信息缺失，请先选择养殖场");
        }
        return farmId;
    }
}
