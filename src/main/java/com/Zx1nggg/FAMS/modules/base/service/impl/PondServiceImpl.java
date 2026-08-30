package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.PondDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.service.IPondService;
import com.Zx1nggg.FAMS.modules.base.vo.PondVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PondServiceImpl extends ServiceImpl<PondMapper, Pond> implements IPondService {

    private static final String REDIS_LATEST_PREFIX = "iot:latest:";
    private static final String REDIS_LATEST_FARM_PREFIX = "iot:latest:farm:";
    private static final String REDIS_HISTORY_PREFIX = "iot:history:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Page<PondVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, String pondName) {
        LambdaQueryWrapper<Pond> wrapper = new LambdaQueryWrapper<>();
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(Pond::getFarmId, SecurityUtils.getCurrentFarmId());
        } else if (farmId != null) {
            wrapper.eq(Pond::getFarmId, farmId);
        }
        if (pondName != null && !pondName.isEmpty()) {
            wrapper.like(Pond::getPondName, pondName);
        }
        wrapper.orderByDesc(Pond::getId);
        Page<Pond> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public PondVO queryById(Long id) {
        Pond pond = getById(id);
        if (pond == null) {
            return null;
        }
        checkFarmAccess(pond);
        return toVO(pond);
    }

    @Override
    public PondVO create(PondDTO dto) {
        Pond pond = new Pond();
        BeanUtils.copyProperties(dto, pond);
        pond.setFarmId(resolveFarmId(dto.getFarmId()));
        save(pond);
        return toVO(pond);
    }

    @Override
    public PondVO update(Long id, PondDTO dto) {
        Pond pond = getById(id);
        if (pond == null) {
            return null;
        }
        checkFarmAccess(pond);
        BeanUtils.copyProperties(dto, pond);
        pond.setId(id);
        pond.setFarmId(resolveFarmId(dto.getFarmId()));
        updateById(pond);
        return toVO(pond);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (SecurityUtils.isFarmer()) {
            List<Pond> ponds = listByIds(ids);
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            for (Pond pond : ponds) {
                if (!userFarmId.equals(pond.getFarmId())) {
                    throw new BusinessException(403, "无权删除不属于本养殖场的池塘");
                }
            }
        }
        List<Pond> ponds = listByIds(ids);
        removeByIds(ids);
        cleanupIotCache(ponds);
    }

    @Override
    public void batchDeleteByFarmIds(List<Long> farmIds) {
        LambdaQueryWrapper<Pond> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Pond::getFarmId, farmIds);
        List<Pond> ponds = list(wrapper);
        if (!ponds.isEmpty()) {
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            removeByIds(pondIds);
            cleanupIotCache(ponds);
        }
    }

    @Override
    public void restoreByFarmIds(List<Long> farmIds) {
        LambdaUpdateWrapper<Pond> uw = new LambdaUpdateWrapper<>();
        uw.in(Pond::getFarmId, farmIds).set(Pond::getIsDeleted, 0);
        baseMapper.update(null, uw);
    }

    private PondVO toVO(Pond pond) {
        PondVO vo = new PondVO();
        BeanUtils.copyProperties(pond, vo);
        return vo;
    }

    private Page<PondVO> toVOPage(Page<Pond> page) {
        Page<PondVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PondVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    private Long resolveFarmId(Long dtoFarmId) {
        if (SecurityUtils.isFarmer()) {
            Long farmId = SecurityUtils.getCurrentFarmId();
            if (farmId == null) {
                throw new BusinessException(401, "当前用户养殖场信息缺失，请重新登录");
            }
            return farmId;
        }
        return dtoFarmId;
    }

    private void checkFarmAccess(Pond pond) {
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            if (!userFarmId.equals(pond.getFarmId())) {
                throw new BusinessException(403, "无权访问其他养殖场的数据");
            }
        }
    }

    private void cleanupIotCache(List<Pond> ponds) {
        if (ponds == null || ponds.isEmpty()) return;
        for (Pond pond : ponds) {
            if (pond.getId() != null) {
                redisTemplate.delete(REDIS_LATEST_PREFIX + pond.getId());
                redisTemplate.delete(REDIS_HISTORY_PREFIX + pond.getId());
            }
            if (pond.getFarmId() != null) {
                redisTemplate.delete(REDIS_LATEST_FARM_PREFIX + pond.getFarmId());
            }
        }
        ponds.stream()
                .map(Pond::getFarmId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(farmId -> redisTemplate.delete(REDIS_LATEST_FARM_PREFIX + farmId));
    }
}
