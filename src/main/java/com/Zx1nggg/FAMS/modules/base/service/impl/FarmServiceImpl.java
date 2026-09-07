package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.FarmDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.base.service.IFarmService;
import com.Zx1nggg.FAMS.modules.base.service.IPondService;
import com.Zx1nggg.FAMS.modules.base.vo.FarmVO;
import com.Zx1nggg.FAMS.security.service.UserFarmCacheService;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
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
    @Autowired private com.Zx1nggg.FAMS.modules.system.mapper.UserMapper userMapper;

    @Override
    public Page<FarmVO> pageQuery(Integer pageNum, Integer pageSize, String farmName) {
        LambdaQueryWrapper<Farm> wrapper = new LambdaQueryWrapper<>();
        if (farmName != null && !farmName.isEmpty()) {
            wrapper.like(Farm::getFarmName, farmName);
        }
        // 数据隔离：FARMER 只能看到自己名下的养殖场
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(Farm::getUserId, SecurityUtils.getCurrentUserId());
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
        // 🌟 数据隔离：FARMER 不能查看别人的养殖场
        checkFarmOwnership(farm);
        return toVO(farm);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FarmVO create(FarmDTO dto, Long currentUserId) {
        Farm farm = new Farm();
        BeanUtils.copyProperties(dto, farm);
        // 🌟 数据隔离：FARMER 强制绑定到当前用户
        if (SecurityUtils.isFarmer()) {
            farm.setUserId(SecurityUtils.getCurrentUserId());
        } else if (farm.getUserId() == null) {
            farm.setUserId(currentUserId);
        }
        requireOwner(farm.getUserId());
        save(farm);
        if (farm.getUserId() != null) {
            userFarmCacheService.evictUserFarms(farm.getUserId());
        }
        return toVO(farm);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FarmVO update(Long id, FarmDTO dto) {
        Farm farm = getById(id);
        if (farm == null) {
            return null;
        }
        // 🌟 数据隔离：FARMER 不能修改别人的养殖场
        checkFarmOwnership(farm);
        Set<Long> affectedUsers = new HashSet<>();
        if (farm.getUserId() != null) {
            affectedUsers.add(farm.getUserId());
        }
        if (dto.getUserId() != null) {
            affectedUsers.add(dto.getUserId());
        }
        Long previousOwner = farm.getUserId();
        BeanUtils.copyProperties(dto, farm);
        if (farm.getUserId() == null) farm.setUserId(previousOwner);
        farm.setId(id);
        // 🌟 数据隔离：FARMER 不能将养殖场转让给其他用户
        if (SecurityUtils.isFarmer()) {
            farm.setUserId(SecurityUtils.getCurrentUserId());
        }
        requireOwner(farm.getUserId());
        updateById(farm);
        affectedUsers.forEach(userFarmCacheService::evictUserFarms);
        return toVO(farm);
    }

    private void requireOwner(Long userId) {
        if (userId == null || userMapper.selectForUpdate(userId) == null) {
            throw new BusinessException(404, "养殖场所属账号不存在");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择养殖场");
        List<Farm> farms = ids.stream().distinct().sorted().map(baseMapper::selectIncludingDeletedForUpdate)
                .filter(Objects::nonNull).toList();
        // 🌟 数据隔离：FARMER 只能删除自己的养殖场
        if (SecurityUtils.isFarmer()) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            for (Farm farm : farms) {
                if (!Objects.equals(farm.getUserId(), currentUserId)) {
                    throw new BusinessException(403, "无权删除养殖场 ID=" + farm.getId());
                }
            }
        }
        Set<Long> affectedUsers = farms.stream()
                .map(Farm::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 仅对本次仍有效且已验证归属的农场级联，重复删除不能改写恢复标记。
        List<Long> activeIds = farms.stream().filter(f -> !Integer.valueOf(1).equals(f.getIsDeleted()))
                .map(Farm::getId).toList();
        if (activeIds.isEmpty()) return;
        String deleteBatch = java.util.UUID.randomUUID().toString();
        baseMapper.update(null, new LambdaUpdateWrapper<Farm>().in(Farm::getId, activeIds).set(Farm::getDeleteBatch, deleteBatch));
        pondService.batchDeleteByFarmIds(activeIds, deleteBatch);
        // 软删除养殖场（@TableLogic 自动将 DELETE 转为 UPDATE is_deleted=1）
        removeByIds(activeIds);
        affectedUsers.forEach(userFarmCacheService::evictUserFarms);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void restore(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择养殖场");
        List<Farm> farms = ids.stream().distinct().sorted().map(baseMapper::selectIncludingDeletedForUpdate).toList();
        for (Farm farm : farms) {
            if (farm == null) throw new BusinessException(404, "养殖场不存在");
            checkFarmOwnership(farm);
        }
        for (Farm farm : farms) {
            if (!Integer.valueOf(1).equals(farm.getIsDeleted())) continue;
            baseMapper.restoreDeleted(farm.getId());
            pondService.restoreByFarmIds(List.of(farm.getId()), farm.getDeleteBatch());
            if (farm.getUserId() != null) userFarmCacheService.evictUserFarms(farm.getUserId());
        }
    }

    /**
     *  数据隔离：校验 FARMER 是否拥有该养殖场的操作权限
     */
    private void checkFarmOwnership(Farm farm) {
        if (SecurityUtils.isFarmer()
                && !Objects.equals(farm.getUserId(), SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(403, "无权操作该养殖场");
        }
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
