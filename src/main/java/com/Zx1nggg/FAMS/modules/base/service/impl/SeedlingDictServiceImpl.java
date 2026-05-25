package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.modules.base.dto.SeedlingDictDTO;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.service.ISeedlingDictService;
import com.Zx1nggg.FAMS.modules.base.vo.SeedlingDictVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeedlingDictServiceImpl extends ServiceImpl<SeedlingDictMapper, SeedlingDict> implements ISeedlingDictService {

    @Override
    public Page<SeedlingDictVO> pageQuery(Integer pageNum, Integer pageSize, String categoryName) {
        LambdaQueryWrapper<SeedlingDict> wrapper = new LambdaQueryWrapper<>();
        if (categoryName != null && !categoryName.isEmpty()) {
            wrapper.like(SeedlingDict::getCategoryName, categoryName);
        }
        wrapper.orderByDesc(SeedlingDict::getId);
        Page<SeedlingDict> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<SeedlingDictVO> listAll() {
        List<SeedlingDict> list = list(
                new LambdaQueryWrapper<SeedlingDict>().orderByAsc(SeedlingDict::getCategoryName));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public SeedlingDictVO queryById(Long id) {
        SeedlingDict dict = getById(id);
        return dict == null ? null : toVO(dict);
    }

    @Override
    public SeedlingDictVO create(SeedlingDictDTO dto) {
        SeedlingDict dict = new SeedlingDict();
        BeanUtils.copyProperties(dto, dict);
        save(dict);
        return toVO(dict);
    }

    @Override
    public SeedlingDictVO update(Long id, SeedlingDictDTO dto) {
        SeedlingDict dict = getById(id);
        if (dict == null) {
            return null;
        }
        BeanUtils.copyProperties(dto, dict);
        dict.setId(id);
        updateById(dict);
        return toVO(dict);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        removeByIds(ids);
    }

    private SeedlingDictVO toVO(SeedlingDict dict) {
        SeedlingDictVO vo = new SeedlingDictVO();
        BeanUtils.copyProperties(dict, vo);
        return vo;
    }

    private Page<SeedlingDictVO> toVOPage(Page<SeedlingDict> page) {
        Page<SeedlingDictVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SeedlingDictVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
