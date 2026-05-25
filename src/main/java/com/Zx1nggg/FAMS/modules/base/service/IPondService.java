package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.PondDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.vo.PondVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPondService extends IService<Pond> {

    Page<PondVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, String pondName);

    PondVO queryById(Long id);

    PondVO create(PondDTO dto);

    PondVO update(Long id, PondDTO dto);

    void batchDelete(List<Long> ids);

    void batchDeleteByFarmIds(List<Long> farmIds);

    void restoreByFarmIds(List<Long> farmIds);
}
