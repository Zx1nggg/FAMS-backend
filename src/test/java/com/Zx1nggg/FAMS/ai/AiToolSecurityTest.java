package com.Zx1nggg.FAMS.ai;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.ai.tool.AiTool;
import com.Zx1nggg.FAMS.modules.ai.tool.AiToolContext;
import com.Zx1nggg.FAMS.modules.ai.tool.AiToolRegistry;
import com.Zx1nggg.FAMS.modules.ai.tool.AiToolSchemas;
import com.Zx1nggg.FAMS.modules.ai.tool.LatestWaterQualityTool;
import com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiToolSecurityTest {

    @Test
    void farmerCannotOverrideAuthenticatedFarmId() throws Exception {
        IIotSensorDataService sensorService = mock(IIotSensorDataService.class);
        when(sensorService.getLatestByFarmId(7L)).thenReturn(List.of());
        LatestWaterQualityTool tool = new LatestWaterQualityTool(sensorService);

        Object result = tool.execute(new AiToolContext(10L, "FARMER", 7L),
                new ObjectMapper().readTree("{\"farm_id\":999}"));

        verify(sensorService).getLatestByFarmId(7L);
        assertThat(result).isEqualTo(Map.of("farmId", 7L, "readings", List.of()));
    }

    @Test
    void registryFiltersDefinitionsAndRechecksRoleAtExecution() {
        AiTool farmerOnly = new AiTool() {
            @Override public String name() { return "farmer_only"; }
            @Override public String description() { return "test"; }
            @Override public Set<String> allowedRoles() { return Set.of("FARMER"); }
            @Override public Map<String, Object> parameters() { return AiToolSchemas.noArguments(); }
            @Override public Object execute(AiToolContext context, JsonNode arguments) { return Map.of("ok", true); }
        };
        AiToolRegistry registry = new AiToolRegistry(List.of(farmerOnly), new ObjectMapper());

        assertThat(registry.definitionsFor("REGULATOR")).isEmpty();
        assertThat(registry.definitionsFor("FARMER")).hasSize(1);
        assertThatThrownBy(() -> registry.execute("farmer_only", "{}",
                new AiToolContext(1L, "REGULATOR", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(403);
    }
}
