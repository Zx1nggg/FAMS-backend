package com.Zx1nggg.FAMS.modules.iot.service;

import com.Zx1nggg.FAMS.modules.iot.entity.IotSensorData;
import com.Zx1nggg.FAMS.modules.iot.vo.IotSensorDataVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * IoT 传感器数据服务接口
 */
public interface IIotSensorDataService extends IService<IotSensorData> {

    /**
     * 获取指定池塘的最新一条传感器数据（优先 Redis，miss 回 MySQL）
     */
    IotSensorDataVO getLatestByPondId(Long pondId);

    /**
     * 获取指定养殖场下所有池塘的最新数据（优先 Redis，miss 回 MySQL 重建）
     */
    List<IotSensorDataVO> getLatestByFarmId(Long farmId);

    /**
     * 获取指定池塘的历史传感器数据
     * hours ≤ 24：走 Redis ZSET（分钟级精度）
     * hours > 24：走 MySQL（小时均值）
     */
    List<IotSensorDataVO> getHistory(Long pondId, int hours);
}
