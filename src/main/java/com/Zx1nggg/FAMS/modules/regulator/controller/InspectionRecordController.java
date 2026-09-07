package com.Zx1nggg.FAMS.modules.regulator.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.modules.regulator.dto.*;
import com.Zx1nggg.FAMS.modules.regulator.vo.InspectionRecordVO;
import com.Zx1nggg.FAMS.modules.regulator.service.IInspectionRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/regulator/inspections")
public class InspectionRecordController {
    @Autowired private IInspectionRecordService service;
    @GetMapping("/list")
    public Result<Page<InspectionRecordVO>> list(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long farmId, @RequestParam(required = false) String result, @RequestParam(required = false) String rectifyStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.pageQuery(pageNum, pageSize, farmId, result, rectifyStatus, startDate, endDate));
    }
    @GetMapping("/{id}") public Result<InspectionRecordVO> detail(@PathVariable Long id) { return Result.success(service.detail(id)); }
    @Log(title = "抽检档案", businessType = 1)
    @PostMapping public Result<InspectionRecordVO> create(@Valid @RequestBody InspectionRecordDTO dto) { return Result.success(service.create(dto)); }
    @Log(title = "抽检档案", businessType = 2)
    @PutMapping("/{id}") public Result<InspectionRecordVO> update(@PathVariable Long id, @Valid @RequestBody InspectionRecordDTO dto) { return Result.success(service.updateRecord(id, dto)); }
    @Log(title = "抽检档案", businessType = 3)
    @DeleteMapping("/{ids}") public Result<Void> delete(@PathVariable List<Long> ids) { service.deleteRecords(ids); return Result.success(); }
    @Log(title = "抽检整改", businessType = 2)
    @PutMapping("/{id}/rectify") public Result<Void> rectify(@PathVariable Long id, @Valid @RequestBody InspectionRectifyDTO dto) { service.rectify(id, dto); return Result.success(); }
    @GetMapping("/stats") public Result<Map<String, Object>> stats() { return Result.success(service.stats()); }
}
