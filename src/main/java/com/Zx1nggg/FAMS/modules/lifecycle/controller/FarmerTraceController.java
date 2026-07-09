package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IFarmerTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "农户全链路溯源")
@RestController
@RequestMapping("/lifecycle/trace")
public class FarmerTraceController {

    @Autowired
    private IFarmerTraceService farmerTraceService;

    @Operation(summary = "农户端按批次号查询本场全链路溯源")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam String batchNo) {
        return Result.success(farmerTraceService.getTraceDetail(batchNo));
    }
}
