package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CurrentFarmOverviewTool implements AiTool {
    private final FarmMapper farmMapper;
    private final PondMapper pondMapper;
    private final PurchaseBatchMapper purchaseBatchMapper;

    public CurrentFarmOverviewTool(FarmMapper farmMapper, PondMapper pondMapper,
                                   PurchaseBatchMapper purchaseBatchMapper) {
        this.farmMapper = farmMapper;
        this.pondMapper = pondMapper;
        this.purchaseBatchMapper = purchaseBatchMapper;
    }

    @Override public String name() { return "get_current_farm_overview"; }

    @Override
    public String description() {
        return "读取当前农户已选择养殖场的基本信息、池塘数量、面积和养殖批次概况。仅用于回答当前场区总体情况。";
    }

    @Override public Set<String> allowedRoles() { return Set.of("FARMER"); }

    @Override public Map<String, Object> parameters() { return AiToolSchemas.noArguments(); }

    @Override
    public Object execute(AiToolContext context, JsonNode arguments) {
        Long farmId = context.farmId();
        if (farmId == null) throw new BusinessException(400, "请先选择养殖场");
        Farm farm = farmMapper.selectById(farmId);
        if (farm == null) throw new BusinessException(404, "当前养殖场不存在");
        List<Pond> ponds = pondMapper.selectList(new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
        BigDecimal totalAreaMu = ponds.stream().map(Pond::getAreaMu)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("farmId", farm.getId());
        result.put("farmName", farm.getFarmName());
        result.put("address", farm.getAddress());
        result.put("pondCount", ponds.size());
        result.put("totalAreaMu", totalAreaMu);
        result.put("ponds", ponds.stream().map(pond -> Map.of(
                "pondId", pond.getId(),
                "pondName", pond.getPondName(),
                "areaMu", pond.getAreaMu() == null ? BigDecimal.ZERO : pond.getAreaMu())).toList());
        result.put("activeBatchCount", purchaseBatchMapper.selectCount(new LambdaQueryWrapper<PurchaseBatch>()
                .eq(PurchaseBatch::getFarmId, farmId).in(PurchaseBatch::getBatchStatus, 0, 1, 2)));
        return result;
    }
}
