package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.SeedlingDictDTO;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.service.ISeedlingDictService;
import com.Zx1nggg.FAMS.modules.base.vo.SeedlingDictVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
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
        // 苗种是全局公共目录；农户只能查阅，由监管方/管理员维护。
        wrapper.orderByDesc(SeedlingDict::getId);
        Page<SeedlingDict> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<SeedlingDictVO> listAll() {
        LambdaQueryWrapper<SeedlingDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SeedlingDict::getCategoryName);
        List<SeedlingDict> list = list(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public SeedlingDictVO queryById(Long id) {
        SeedlingDict dict = getById(id);
        if (dict == null) return null;
        return toVO(dict);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SeedlingDictVO create(SeedlingDictDTO dto) {
        assertCanManage();
        SeedlingDict dict = new SeedlingDict();
        BeanUtils.copyProperties(dto, dict);
        save(dict);
        return toVO(dict);
    }

    @Override
    public SeedlingDictVO update(Long id, SeedlingDictDTO dto) {
        assertCanManage();
        SeedlingDict dict = getById(id);
        if (dict == null) return null;
        BeanUtils.copyProperties(dto, dict);
        dict.setId(id);
        updateById(dict);
        return toVO(dict);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        assertCanManage();
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择苗种");
        ids.stream().distinct().sorted().forEach(baseMapper::selectForUpdate);
        for (Long id : ids) {
            if (baseMapper.countReferences(id) > 0) throw new BusinessException(400, "苗种被采购或 SOP 引用，不能删除");
        }
        removeByIds(ids);
    }

    private void assertCanManage() {
        if (!SecurityUtils.isRegulator() && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "苗种公共目录仅允许监管方或管理员维护");
        }
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
