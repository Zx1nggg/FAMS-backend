package com.Zx1nggg.FAMS.modules.system.service;

import com.Zx1nggg.FAMS.modules.system.entity.DictData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IDictDataService extends IService<DictData> {

    /** 根据字典类型查询所有启用的字典项 */
    List<DictData> listByDictType(String dictType);
}
