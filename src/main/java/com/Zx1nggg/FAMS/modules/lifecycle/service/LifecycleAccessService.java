package com.Zx1nggg.FAMS.modules.lifecycle.service;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.entity.Stocking;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.StockingMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.PatrolLogMapper;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** 校验台账的关联链，不能仅验证请求中的池塘而信任批次和巡塘 ID。 */
@Service
public class LifecycleAccessService {
    @Autowired private PondMapper pondMapper;
    @Autowired private PurchaseBatchMapper batchMapper;
    @Autowired private StockingMapper stockingMapper;
    @Autowired private PatrolLogMapper patrolMapper;
    @Autowired private com.Zx1nggg.FAMS.modules.lifecycle.mapper.HarvestRecordMapper harvestMapper;

    public Pond requirePond(Long pondId) {
        if (pondId == null) throw new BusinessException(400, "池塘不能为空");
        Pond pond = pondMapper.selectById(pondId);
        if (pond == null) throw new BusinessException(404, "池塘不存在或已删除");
        if (SecurityUtils.isFarmer() && !Objects.equals(pond.getFarmId(), SecurityUtils.getCurrentFarmId())) {
            throw new BusinessException(403, "无权操作其他养殖场的数据");
        }
        return pond;
    }

    public PurchaseBatch requireBatchForPond(String batchNo, Long pondId, boolean editable) {
        Pond pond = requirePond(pondId);
        if (batchNo == null || batchNo.isBlank()) throw new BusinessException(400, "批次号不能为空");
        PurchaseBatch batch = editable ? batchMapper.selectByBatchNoForUpdate(batchNo)
                : batchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo));
        if (batch == null) throw new BusinessException(404, "批次不存在");
        if (!Objects.equals(batch.getFarmId(), pond.getFarmId())) {
            throw new BusinessException(403, "批次与池塘不属于同一养殖场");
        }
        if (editable && !Byte.valueOf((byte) 2).equals(batch.getBatchStatus())) {
            throw new BusinessException(400, "仅养殖中的批次可编辑台账");
        }
        if (editable && harvestMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord>()
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord::getBatchNo, batchNo)
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord::getPondId, pondId)) > 0) {
            throw new BusinessException(400, "该批次在所选池塘已出塘，历史台账不可编辑");
        }
        if (stockingMapper.selectCount(new LambdaQueryWrapper<Stocking>()
                .eq(Stocking::getBatchId, batch.getId()).eq(Stocking::getPondId, pondId)) == 0) {
            throw new BusinessException(400, "该批次未投放在所选池塘");
        }
        return batch;
    }

    public void requirePatrol(Long patrolId, Long pondId, String batchNo) {
        requirePond(pondId);
        if (patrolId == null) return;
        PatrolLog patrol = patrolMapper.selectById(patrolId);
        if (patrol == null || !Objects.equals(patrol.getPondId(), pondId)
                || (batchNo != null && !Objects.equals(patrol.getBatchNo(), batchNo))) {
            throw new BusinessException(400, "巡塘记录与当前池塘或批次不一致");
        }
        if (patrol.getBatchNo() != null && !patrol.getBatchNo().isBlank()) {
            requireBatchForPond(patrol.getBatchNo(), pondId, true);
        }
    }
}
