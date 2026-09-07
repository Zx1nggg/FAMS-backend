package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.modules.base.dto.SopTemplateDTO;
import com.Zx1nggg.FAMS.modules.base.entity.SopTemplate;
import com.Zx1nggg.FAMS.modules.base.mapper.SopTemplateMapper;
import com.Zx1nggg.FAMS.modules.base.service.ISopTemplateService;
import com.Zx1nggg.FAMS.modules.base.vo.SopTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SopTemplateServiceImpl extends ServiceImpl<SopTemplateMapper, SopTemplate> implements ISopTemplateService {
    @org.springframework.beans.factory.annotation.Autowired
    private com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper seedlingMapper;

    private void validate(SopTemplateDTO dto) {
        if (dto.getCategoryId() == null || seedlingMapper.selectForUpdate(dto.getCategoryId()) == null) {
            throw new com.Zx1nggg.FAMS.common.exception.BusinessException(404, "适用苗种不存在");
        }
    }

    @Override
    public Page<SopTemplateVO> pageQuery(Integer pageNum, Integer pageSize, Long categoryId, String stageName, String taskType) {
        LambdaQueryWrapper<SopTemplate> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(SopTemplate::getCategoryId, categoryId);
        }
        if (stageName != null && !stageName.isEmpty()) {
            wrapper.eq(SopTemplate::getStageName, stageName);
        }
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.eq(SopTemplate::getTaskType, taskType);
        }
        wrapper.orderByAsc(SopTemplate::getCategoryId, SopTemplate::getDayOffset);
        Page<SopTemplate> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public SopTemplateVO queryById(Long id) {
        SopTemplate template = getById(id);
        return template == null ? null : toVO(template);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SopTemplateVO create(SopTemplateDTO dto) {
        validate(dto);
        SopTemplate template = new SopTemplate();
        BeanUtils.copyProperties(dto, template);
        save(template);
        return toVO(template);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SopTemplateVO update(Long id, SopTemplateDTO dto) {
        validate(dto);
        SopTemplate template = getById(id);
        if (template == null) {
            return null;
        }
        BeanUtils.copyProperties(dto, template);
        template.setId(id);
        updateById(template);
        return toVO(template);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        removeByIds(ids);
    }

    private SopTemplateVO toVO(SopTemplate template) {
        SopTemplateVO vo = new SopTemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private Page<SopTemplateVO> toVOPage(Page<SopTemplate> page) {
        Page<SopTemplateVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SopTemplateVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
