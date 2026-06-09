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
import java.util.Objects;

@Service
public class SeedlingDictServiceImpl extends ServiceImpl<SeedlingDictMapper, SeedlingDict> implements ISeedlingDictService {

    @Override
    public Page<SeedlingDictVO> pageQuery(Integer pageNum, Integer pageSize, String categoryName) {
        LambdaQueryWrapper<SeedlingDict> wrapper = new LambdaQueryWrapper<>();
        if (categoryName != null && !categoryName.isEmpty()) {
            wrapper.like(SeedlingDict::getCategoryName, categoryName);
        }
        // 🌟 数据隔离：FARMER 只能看到本农场的苗种字典
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(SeedlingDict::getUserId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByDesc(SeedlingDict::getId);
        Page<SeedlingDict> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<SeedlingDictVO> listAll() {
        LambdaQueryWrapper<SeedlingDict> wrapper = new LambdaQueryWrapper<>();
        // 🌟 数据隔离：FARMER 只能看到本农场的苗种字典
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(SeedlingDict::getUserId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByAsc(SeedlingDict::getCategoryName);
        List<SeedlingDict> list = list(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public SeedlingDictVO queryById(Long id) {
        SeedlingDict dict = getById(id);
        if (dict == null) return null;
        // 🌟 数据隔离
        checkSeedlingAccess(dict);
        return toVO(dict);
    }

    @Override
    public SeedlingDictVO create(SeedlingDictDTO dto) {
        SeedlingDict dict = new SeedlingDict();
        BeanUtils.copyProperties(dto, dict);
        // 🌟 数据隔离：FARMER 创建时自动绑定本农场
        if (SecurityUtils.isFarmer()) {
            dict.setUserId(SecurityUtils.getCurrentUserId());
        }
        save(dict);
        return toVO(dict);
    }

    @Override
    public SeedlingDictVO update(Long id, SeedlingDictDTO dto) {
        SeedlingDict dict = getById(id);
        if (dict == null) return null;
        // 🌟 数据隔离
        checkSeedlingAccess(dict);
        BeanUtils.copyProperties(dto, dict);
        dict.setId(id);
        // 🌟 数据隔离：FARMER 不能将苗种转给其他农场
        if (SecurityUtils.isFarmer()) {
            dict.setUserId(SecurityUtils.getCurrentUserId());
        }
        updateById(dict);
        return toVO(dict);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        // 🌟 数据隔离：FARMER 只能删除自己的苗种字典
        if (SecurityUtils.isFarmer()) {
            List<SeedlingDict> dicts = listByIds(ids);
            Long currentUserId = SecurityUtils.getCurrentUserId();
            for (SeedlingDict d : dicts) {
                if (!Objects.equals(d.getUserId(), currentUserId)) {
                    throw new BusinessException(403, "无权删除苗种字典 ID=" + d.getId());
                }
            }
        }
        removeByIds(ids);
    }

    /**
     * 🌟 数据隔离：校验 FARMER 是否拥有该苗种的操作权限
     */
    private void checkSeedlingAccess(SeedlingDict dict) {
        if (SecurityUtils.isFarmer()
                && !Objects.equals(dict.getUserId(), SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(403, "无权操作该苗种字典");
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
