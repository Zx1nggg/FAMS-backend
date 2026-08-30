package com.Zx1nggg.FAMS.modules.regulator.service;

import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.Zx1nggg.FAMS.modules.regulator.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.List;

public interface IRegulatorService {
    DashboardStatsVO getDashboardStats();

    List<DashboardAlertVO> getDashboardAlerts(Integer limit);

    List<DashboardWatchlistVO> getDashboardWatchlist(Integer limit);

    List<FarmGeoVO> getFarmsGeo();

    TraceChainVO quickTrace(String keyword);

    TraceChainVO getTraceDetail(String batchNo);

    Page<TraceBatchVO> listTraceBatches(Integer pageNum, Integer pageSize, Long farmId, Byte batchStatus, String keyword);

    AlertStatsVO getAlertStats();

    Page<AlarmRecordVO> listAlerts(Integer pageNum, Integer pageSize, Long farmId, Byte severity,
                                   Byte status, String sourceType, LocalDate startDate, LocalDate endDate);

    List<AlertTrendVO> getAlertTrend(Integer days);

    List<IotRealtimeAlertVO> getIotRealtimeAlerts();

    void handleAlert(Long id, Byte status, String remark);
}
