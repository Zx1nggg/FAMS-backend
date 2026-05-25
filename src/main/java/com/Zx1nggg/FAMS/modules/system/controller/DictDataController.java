package com.Zx1nggg.FAMS.modules.system.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.entity.DictData;
import com.Zx1nggg.FAMS.modules.system.service.IDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "数据字典")
@RestController
@RequestMapping("/system/dict-data")
public class DictDataController {

    @Autowired
    private IDictDataService dictDataService;

    @Operation(summary = "根据字典类型查询字典项列表（通用接口）")
    @GetMapping("/type/{dictType}")
    public Result<List<Map<String, Object>>> getByDictType(@PathVariable String dictType) {
        List<DictData> list = dictDataService.listByDictType(dictType);
        List<Map<String, Object>> result = list.stream().map(item -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("label", item.getDictLabel());
            map.put("value", item.getDictValue());
            map.put("cssClass", item.getCssClass());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "查询所有字典类型")
    @GetMapping("/types")
    public Result<List<String>> getDictTypes() {
        List<DictData> list = dictDataService.list();
        List<String> types = list.stream()
                .map(DictData::getDictType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(types);
    }
}
