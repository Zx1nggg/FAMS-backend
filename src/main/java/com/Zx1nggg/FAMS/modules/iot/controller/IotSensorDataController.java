package com.Zx1nggg.FAMS.modules.iot.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService;
import com.Zx1nggg.FAMS.modules.iot.vo.IotSensorDataVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "IoT 传感器数据")
@RestController
@RequestMapping("/iot/sensor-data")
public class IotSensorDataController {

    @Autowired
    private IIotSensorDataService iotSensorDataService;

    @Operation(summary = "获取指定池塘最新传感器读数")
    @GetMapping("/latest")
    public Result<IotSensorDataVO> latest(@RequestParam Long pondId) {
        IotSensorDataVO vo = iotSensorDataService.getLatestByPondId(pondId);
        if (vo == null) return Result.error(404, "该池塘暂无传感器数据");
        return Result.success(vo);
    }

    @Operation(summary = "获取当前养殖场所有池塘的最新传感器读数")
    @GetMapping("/latest/all")
    public Result<List<IotSensorDataVO>> latestAll(
            @RequestParam(required = false) Long farmId) {
        // FARMER 角色自动使用自己的 farmId，ADMIN/REGULATOR 可指定
        if (farmId == null) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        if (farmId == null) {
            return Result.error(400, "请指定养殖场ID");
        }
        return Result.success(iotSensorDataService.getLatestByFarmId(farmId));
    }

    @Operation(summary = "获取指定池塘历史传感器数据（≤24h分钟级 / >24h小时均值）")
    @GetMapping("/history")
    public Result<List<IotSensorDataVO>> history(
            @RequestParam Long pondId,
            @RequestParam(defaultValue = "24") int hours) {
        if (hours < 1 || hours > 720) {
            return Result.error(400, "hours 参数须在 1-720 之间");
        }
        return Result.success(iotSensorDataService.getHistory(pondId, hours));
    }
}
