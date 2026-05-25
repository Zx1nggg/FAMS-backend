package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.SeedlingDictDTO;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.vo.SeedlingDictVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ISeedlingDictService extends IService<SeedlingDict> {

    Page<SeedlingDictVO> pageQuery(Integer pageNum, Integer pageSize, String categoryName);

    List<SeedlingDictVO> listAll();

    SeedlingDictVO queryById(Long id);

    SeedlingDictVO create(SeedlingDictDTO dto);

    SeedlingDictVO update(Long id, SeedlingDictDTO dto);

    void batchDelete(List<Long> ids);
}
