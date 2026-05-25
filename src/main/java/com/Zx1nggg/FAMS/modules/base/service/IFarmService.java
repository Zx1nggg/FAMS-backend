package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.FarmDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.vo.FarmVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IFarmService extends IService<Farm> {

    Page<FarmVO> pageQuery(Integer pageNum, Integer pageSize, String farmName);

    FarmVO queryById(Long id);

    FarmVO create(FarmDTO dto, Long currentUserId);

    FarmVO update(Long id, FarmDTO dto);

    void batchDelete(List<Long> ids);

    void restore(List<Long> ids);
}
