package com.Zx1nggg.FAMS.modules.iot.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService;
import com.Zx1nggg.FAMS.modules.iot.vo.IotSensorDataVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Tag(name = "IoT 传感器数据")
@RestController
@RequestMapping("/iot/sensor-data")
public class IotSensorDataController {

    @Autowired
    private IIotSensorDataService iotSensorDataService;

    @Resource
    private PondMapper pondMapper;

    @Operation(summary = "获取指定池塘最新传感器读数")
    @GetMapping("/latest")
    public Result<IotSensorDataVO> latest(@RequestParam Long pondId) {
        // 🌟 数据隔离：FARMER 只能查看本农场池塘的传感器数据
        checkPondFarmAccess(pondId);
        IotSensorDataVO vo = iotSensorDataService.getLatestByPondId(pondId);
        if (vo == null) return Result.error(404, "该池塘暂无传感器数据");
        return Result.success(vo);
    }

    @Operation(summary = "获取当前养殖场所有池塘的最新传感器读数")
    @GetMapping("/latest/all")
    public Result<List<IotSensorDataVO>> latestAll(
            @RequestParam(required = false) Long farmId) {
        // 🌟 数据隔离：FARMER 强制只能看本农场数据
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        } else if (farmId == null) {
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
        // 🌟 数据隔离：FARMER 只能查看本农场池塘的传感器数据
        checkPondFarmAccess(pondId);
        return Result.success(iotSensorDataService.getHistory(pondId, hours));
    }

    /**
     * 🌟 数据隔离：校验 FARMER 是否有权访问该池塘
     */
    private void checkPondFarmAccess(Long pondId) {
        if (!SecurityUtils.isFarmer()) return;
        Pond pond = pondMapper.selectById(pondId);
        if (pond == null || !Objects.equals(pond.getFarmId(), SecurityUtils.getCurrentFarmId())) {
            throw new BusinessException(403, "无权查看其他养殖场的传感器数据");
        }
    }
}
