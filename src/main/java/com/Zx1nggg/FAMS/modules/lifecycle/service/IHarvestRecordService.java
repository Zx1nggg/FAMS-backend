package com.Zx1nggg.FAMS.modules.lifecycle.service;

import com.Zx1nggg.FAMS.modules.lifecycle.dto.HarvestRecordDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.HarvestRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 出塘结算与消费者防伪溯源凭证表 服务类
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface IHarvestRecordService extends IService<HarvestRecord> {

    /**
     * 分页查询出塘结算记录（农场数据隔离）
     */
    Page<HarvestRecordVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, String batchNo, Long pondId);

    /**
     * 根据ID查询详情
     */
    HarvestRecordVO queryById(Long id);

    /**
     * 创建出塘结算记录（自动计算金额 + 批次状态 2→3）
     */
    HarvestRecordVO create(HarvestRecordDTO dto);

    /**
     * 更新出塘结算记录（重算金额）
     */
    HarvestRecordVO update(Long id, HarvestRecordDTO dto);

    /**
     * 批量软删除（恢复批次状态 3→2）
     */
    void batchDelete(List<Long> ids);

    /**
     * 结算预览：拉取该批次的养殖汇总数据
     */
    Map<String, Object> preview(Long batchId);
}
