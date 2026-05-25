package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.SopTemplateDTO;
import com.Zx1nggg.FAMS.modules.base.entity.SopTemplate;
import com.Zx1nggg.FAMS.modules.base.vo.SopTemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ISopTemplateService extends IService<SopTemplate> {

    Page<SopTemplateVO> pageQuery(Integer pageNum, Integer pageSize, Long categoryId, String stageName, String taskType);

    SopTemplateVO queryById(Long id);

    SopTemplateVO create(SopTemplateDTO dto);

    SopTemplateVO update(Long id, SopTemplateDTO dto);

    void batchDelete(List<Long> ids);
}
