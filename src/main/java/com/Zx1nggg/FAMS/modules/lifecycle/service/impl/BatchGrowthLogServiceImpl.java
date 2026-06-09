package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.entity.Stocking;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.StockingMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.BatchGrowthLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IBatchGrowthLogService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.BatchGrowthLogVO;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.GrowthChartVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BatchGrowthLogServiceImpl extends ServiceImpl<BatchGrowthLogMapper, BatchGrowthLog> implements IBatchGrowthLogService {

    @Resource
    private PondMapper pondMapper;

    @Resource
    private StockingMapper stockingMapper;

    @Resource
    private PurchaseBatchMapper purchaseBatchMapper;

    @Resource
    private SeedlingDictMapper seedlingDictMapper;

    @Override
    public Page<BatchGrowthLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                            String batchNo, Long pondId, Long farmId,
                                            Long patrolLogId,
                                            LocalDate startDate, LocalDate endDate) {
        // 🌟 数据隔离：FARMER 只能查看本农场的生长记录
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        LambdaQueryWrapper<BatchGrowthLog> wrapper = new LambdaQueryWrapper<>();

        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<BatchGrowthLogVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(BatchGrowthLog::getPondId, pondIds);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq(BatchGrowthLog::getBatchNo, batchNo);
        }
        if (pondId != null) {
            wrapper.eq(BatchGrowthLog::getPondId, pondId);
        }
        if (patrolLogId != null) {
            wrapper.eq(BatchGrowthLog::getPatrolLogId, patrolLogId);
        }
        if (startDate != null) {
            wrapper.ge(BatchGrowthLog::getLogDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(BatchGrowthLog::getLogDate, endDate);
        }
        wrapper.orderByDesc(BatchGrowthLog::getLogDate);
        Page<BatchGrowthLog> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public BatchGrowthLogVO queryById(Long id) {
        BatchGrowthLog log = getById(id);
        if (log == null) return null;
        // 🌟 数据隔离
        checkFarmAccessByPondId(log.getPondId());
        return toVO(log);
    }

    @Override
    public BatchGrowthLogVO create(BatchGrowthLogDTO dto) {
        // 🌟 数据隔离：FARMER 只能在本农场池塘创建记录
        checkFarmAccessByPondId(dto.getPondId());
        BatchGrowthLog log = new BatchGrowthLog();
        BeanUtils.copyProperties(dto, log);
        save(log);
        return toVO(log);
    }

    @Override
    public BatchGrowthLogVO update(Long id, BatchGrowthLogDTO dto) {
        BatchGrowthLog log = getById(id);
        if (log == null) return null;
        // 🌟 数据隔离
        checkFarmAccessByPondId(log.getPondId());
        checkFarmAccessByPondId(dto.getPondId());
        BeanUtils.copyProperties(dto, log);
        log.setId(id);
        updateById(log);
        return toVO(log);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        // 🌟 数据隔离：FARMER 只能删除本农场记录
        if (SecurityUtils.isFarmer()) {
            List<BatchGrowthLog> logs = listByIds(ids);
            for (BatchGrowthLog log : logs) {
                checkFarmAccessByPondId(log.getPondId());
            }
        }
        removeByIds(ids);
    }

    @Override
    public GrowthChartVO getGrowthChart(String batchNo, Long pondId) {
        // 1. 查批次信息
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo));
        if (batch == null) {
            return null;
        }

        // 2. 查品种名称
        String seedlingName = null;
        if (batch.getSeedlingId() != null) {
            SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
            if (seedling != null) {
                seedlingName = seedling.getCategoryName();
            }
        }

        // 3. 查投放记录（获取初始尾数和投放日期）
        Stocking stocking = stockingMapper.selectOne(
                new LambdaQueryWrapper<Stocking>()
                        .eq(Stocking::getBatchId, batch.getId())
                        .eq(Stocking::getPondId, pondId));
        Integer initialQty = (stocking != null && stocking.getStockedQty() != null)
                ? stocking.getStockedQty() : 0;
        LocalDate stockingDate = (stocking != null) ? stocking.getStockingDate() : null;

        // 4. 查该批次+池塘所有生长记录，按日期升序
        List<BatchGrowthLog> logs = list(new LambdaQueryWrapper<BatchGrowthLog>()
                .eq(BatchGrowthLog::getBatchNo, batchNo)
                .eq(BatchGrowthLog::getPondId, pondId)
                .orderByAsc(BatchGrowthLog::getLogDate));

        // 5. 查池塘名称
        String pondName = null;
        Pond pond = pondMapper.selectById(pondId);
        if (pond != null) {
            pondName = pond.getPondName();
        }

        // 6. 构建 VO
        GrowthChartVO vo = new GrowthChartVO();
        vo.setBatchNo(batchNo);
        vo.setPondName(pondName);
        vo.setSeedlingName(seedlingName);
        vo.setStockingDate(stockingDate);
        vo.setInitialQuantity(initialQty);

        // 7. 按周聚合
        if (stockingDate == null || logs.isEmpty()) {
            vo.setDataPoints(new ArrayList<>());
            return vo;
        }

        // 按周分组 (weekNumber -> 该周的记录列表)
        Map<Integer, List<BatchGrowthLog>> weekMap = new LinkedHashMap<>();
        for (BatchGrowthLog log : logs) {
            int weekNum = (int) (ChronoUnit.DAYS.between(stockingDate, log.getLogDate()) / 7) + 1;
            weekMap.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(log);
        }

        int cumulativeDeaths = 0;
        List<GrowthChartVO.WeekDataPoint> dataPoints = new ArrayList<>();

        for (Map.Entry<Integer, List<BatchGrowthLog>> entry : weekMap.entrySet()) {
            Integer weekNum = entry.getKey();
            List<BatchGrowthLog> weekLogs = entry.getValue();

            GrowthChartVO.WeekDataPoint dp = new GrowthChartVO.WeekDataPoint();
            dp.setWeekNumber(weekNum);
            dp.setWeekLabel("第" + weekNum + "周");

            // 本周平均体重和体长（取多条记录的平均值）
            BigDecimal avgWeight = BigDecimal.ZERO;
            BigDecimal avgLength = BigDecimal.ZERO;
            int weightCount = 0, lengthCount = 0;
            int weeklyDeaths = 0;

            for (BatchGrowthLog log : weekLogs) {
                if (log.getAvgWeight() != null) {
                    avgWeight = avgWeight.add(log.getAvgWeight());
                    weightCount++;
                }
                if (log.getAvgLength() != null) {
                    avgLength = avgLength.add(log.getAvgLength());
                    lengthCount++;
                }
                if (log.getRoutineDeathCount() != null) {
                    weeklyDeaths += log.getRoutineDeathCount();
                }
                if (log.getAbnormalDeathCount() != null) {
                    weeklyDeaths += log.getAbnormalDeathCount();
                }
            }

            dp.setAvgWeight(weightCount > 0
                    ? avgWeight.divide(BigDecimal.valueOf(weightCount), 1, RoundingMode.HALF_UP)
                    : null);
            dp.setAvgLength(lengthCount > 0
                    ? avgLength.divide(BigDecimal.valueOf(lengthCount), 1, RoundingMode.HALF_UP)
                    : null);
            dp.setWeeklyDeaths(weeklyDeaths);

            cumulativeDeaths += weeklyDeaths;
            dp.setCumulativeDeaths(cumulativeDeaths);

            // 存活率
            if (initialQty > 0) {
                BigDecimal rate = BigDecimal.valueOf(100.0 * (initialQty - cumulativeDeaths) / initialQty);
                dp.setSurvivalRate(rate.setScale(1, RoundingMode.HALF_UP));
            } else {
                dp.setSurvivalRate(BigDecimal.ZERO);
            }

            dataPoints.add(dp);
        }

        vo.setDataPoints(dataPoints);
        return vo;
    }

    // ==================== private helpers ====================

    /**
     * 🌟 数据隔离：校验 FARMER 是否有权操作该池塘所属农场
     */
    private void checkFarmAccessByPondId(Long pondId) {
        if (pondId == null) return;
        if (SecurityUtils.isFarmer()) {
            Pond pond = pondMapper.selectById(pondId);
            if (pond == null || !Objects.equals(pond.getFarmId(), SecurityUtils.getCurrentFarmId())) {
                throw new BusinessException(403, "无权操作其他养殖场的数据");
            }
        }
    }

    private BatchGrowthLogVO toVO(BatchGrowthLog log) {
        BatchGrowthLogVO vo = new BatchGrowthLogVO();
        BeanUtils.copyProperties(log, vo);

        if (log.getPondId() != null) {
            Pond pond = pondMapper.selectById(log.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        return vo;
    }

    private Page<BatchGrowthLogVO> toVOPage(Page<BatchGrowthLog> page) {
        Page<BatchGrowthLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
