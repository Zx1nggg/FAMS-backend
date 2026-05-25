package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.modules.base.dto.FarmDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.base.service.IFarmService;
import com.Zx1nggg.FAMS.modules.base.service.IPondService;
import com.Zx1nggg.FAMS.modules.base.vo.FarmVO;
import com.Zx1nggg.FAMS.security.service.UserFarmCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FarmServiceImpl extends ServiceImpl<FarmMapper, Farm> implements IFarmService {

    @Autowired
    private UserFarmCacheService userFarmCacheService;

    @Autowired
    private IPondService pondService;

    @Override
    public Page<FarmVO> pageQuery(Integer pageNum, Integer pageSize, String farmName) {
        LambdaQueryWrapper<Farm> wrapper = new LambdaQueryWrapper<>();
        if (farmName != null && !farmName.isEmpty()) {
            wrapper.like(Farm::getFarmName, farmName);
        }
        wrapper.orderByDesc(Farm::getId);
        Page<Farm> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public FarmVO queryById(Long id) {
        Farm farm = getById(id);
        if (farm == null) {
            return null;
        }
        return toVO(farm);
    }

    @Override
    public FarmVO create(FarmDTO dto, Long currentUserId) {
        Farm farm = new Farm();
        BeanUtils.copyProperties(dto, farm);
        if (farm.getUserId() == null) {
            farm.setUserId(currentUserId);
        }
        save(farm);
        if (farm.getUserId() != null) {
            userFarmCacheService.evictUserFarms(farm.getUserId());
        }
        return toVO(farm);
    }

    @Override
    public FarmVO update(Long id, FarmDTO dto) {
        Farm farm = getById(id);
        if (farm == null) {
            return null;
        }
        Set<Long> affectedUsers = new HashSet<>();
        if (farm.getUserId() != null) {
            affectedUsers.add(farm.getUserId());
        }
        if (dto.getUserId() != null) {
            affectedUsers.add(dto.getUserId());
        }
        BeanUtils.copyProperties(dto, farm);
        farm.setId(id);
        updateById(farm);
        affectedUsers.forEach(userFarmCacheService::evictUserFarms);
        return toVO(farm);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        List<Farm> farms = listByIds(ids);
        Set<Long> affectedUsers = farms.stream()
                .map(Farm::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 级联软删除关联的池塘
        pondService.batchDeleteByFarmIds(ids);
        // 软删除养殖场（@TableLogic 自动将 DELETE 转为 UPDATE is_deleted=1）
        removeByIds(ids);
        affectedUsers.forEach(userFarmCacheService::evictUserFarms);
    }

    @Override
    public void restore(List<Long> ids) {
        // 恢复养殖场：直接用 LambdaUpdateWrapper 更新 is_deleted=0
        LambdaUpdateWrapper<Farm> farmUw = new LambdaUpdateWrapper<>();
        farmUw.in(Farm::getId, ids).set(Farm::getIsDeleted, 0);
        baseMapper.update(null, farmUw);

        // 级联恢复关联的池塘
        pondService.restoreByFarmIds(ids);

        // 恢复后刷新相关用户的缓存
        List<Farm> farms = listByIds(ids);
        farms.stream()
                .map(Farm::getUserId)
                .filter(Objects::nonNull)
                .forEach(userFarmCacheService::evictUserFarms);
    }

    private FarmVO toVO(Farm farm) {
        FarmVO vo = new FarmVO();
        BeanUtils.copyProperties(farm, vo);
        return vo;
    }

    private Page<FarmVO> toVOPage(Page<Farm> page) {
        Page<FarmVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<FarmVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
