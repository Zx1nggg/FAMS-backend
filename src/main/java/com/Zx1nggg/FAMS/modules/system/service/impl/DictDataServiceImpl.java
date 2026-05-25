package com.Zx1nggg.FAMS.modules.system.service.impl;

import com.Zx1nggg.FAMS.modules.system.entity.DictData;
import com.Zx1nggg.FAMS.modules.system.mapper.DictDataMapper;
import com.Zx1nggg.FAMS.modules.system.service.IDictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictData> implements IDictDataService {

    @Override
    public List<DictData> listByDictType(String dictType) {
        return list(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getStatus, (byte) 1)
                .orderByAsc(DictData::getId));
    }
}
