package com.Zx1nggg.FAMS.modules.regulator.service;

import com.Zx1nggg.FAMS.modules.regulator.vo.*;

import java.util.List;

/**
 * 监管方聚合业务接口
 * 跨模块联合查询 (Farm / Stocking / HarvestRecord / AlarmRecord / IoT / Supplier)
 */
public interface IRegulatorService {

    /** 监管大屏 — 宏观统计 */
    DashboardStatsVO getDashboardStats();

    /** 监管大屏 — 未处理告警列表 (Top N) */
    List<DashboardAlertVO> getDashboardAlerts(Integer limit);

    /** 监管大屏 — 督办名单 (按风险排序, Top N) */
    List<DashboardWatchlistVO> getDashboardWatchlist(Integer limit);

    /** GIS — 所有养殖场地理分布 + 告警状态 */
    List<FarmGeoVO> getFarmsGeo();

    /** 溯源码快速查询 (根据批次号或溯源码关键字) */
    TraceChainVO quickTrace(String keyword);

    /** 全链路追溯详情 (按批次号) */
    TraceChainVO getTraceDetail(String batchNo);
}
