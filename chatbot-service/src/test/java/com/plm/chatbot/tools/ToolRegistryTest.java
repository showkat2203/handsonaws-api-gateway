package com.plm.chatbot.tools;

import com.plm.service.api.BomService;
import com.plm.service.api.ChangeOrderService;
import com.plm.service.api.PartService;
import com.plm.service.api.SupplierService;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {

    @Mock
    private PartService partService;
    @Mock
    private BomService bomService;
    @Mock
    private SupplierService supplierService;
    @Mock
    private ChangeOrderService changeOrderService;

    @Test
    void registersAllNineReadOnlyTools() {
        ToolRegistry registry = new ToolRegistry(partService, bomService, supplierService, changeOrderService);

        assertThat(registry.specs()).extracting(spec -> spec.name()).containsExactlyInAnyOrder(
                "get_part", "search_parts", "get_bom", "where_used",
                "get_supplier", "list_suppliers", "supplier_parts",
                "get_change_order", "list_change_orders");
    }

    @Test
    void getPartTool_invokesPartServiceWithIdArgument() {
        ToolRegistry registry = new ToolRegistry(partService, bomService, supplierService, changeOrderService);
        Part part = new Part("PSU-2200", "PSU", null, PartType.PSU, LifecycleState.ACTIVE, "A", null, Map.of(), Instant.now(), Instant.now());
        when(partService.getPart("PSU-2200")).thenReturn(part);

        Object result = registry.get("get_part").execute().apply(Map.of("id", "PSU-2200"));

        assertThat(result).isEqualTo(part);
    }

    @Test
    void searchPartsTool_mapsFilterArguments() {
        ToolRegistry registry = new ToolRegistry(partService, bomService, supplierService, changeOrderService);

        registry.get("search_parts").execute().apply(Map.of("lifecycleState", "EOL", "type", "PSU"));

        ArgumentCaptor<com.plm.service.domain.PartFilter> captor = ArgumentCaptor.forClass(com.plm.service.domain.PartFilter.class);
        verify(partService).searchParts(captor.capture());
        assertThat(captor.getValue().lifecycleState()).isEqualTo(LifecycleState.EOL);
        assertThat(captor.getValue().type()).isEqualTo(PartType.PSU);
    }
}
