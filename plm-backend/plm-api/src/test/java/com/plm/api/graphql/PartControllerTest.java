package com.plm.api.graphql;

import com.plm.api.graphql.input.NewPartInput;
import com.plm.service.api.PartService;
import com.plm.service.api.SupplierService;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartType;
import com.plm.service.domain.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the GraphQL layer is a thin, correct pass-through onto {@link PartService} — no
 * business logic lives here.
 */
@ExtendWith(MockitoExtension.class)
class PartControllerTest {

    @Mock
    private PartService partService;
    @Mock
    private SupplierService supplierService;

    @Test
    void part_delegatesToService() {
        PartController controller = new PartController(partService, supplierService);
        Part part = new Part("PART-1", "PSU", null, PartType.PSU, LifecycleState.ACTIVE, "A", null, Map.of(), Instant.now(), Instant.now());
        when(partService.getPart("PART-1")).thenReturn(part);

        assertThat(controller.part("PART-1")).isEqualTo(part);
    }

    @Test
    void createPart_mapsInputAndAttributesIntoServiceCall() {
        PartController controller = new PartController(partService, supplierService);
        NewPartInput input = new NewPartInput("New Part", "desc", PartType.NIC, "A", "SUP-1",
                List.of(new com.plm.api.graphql.input.KeyValueInput("k", "v")));

        controller.createPart(input);

        ArgumentCaptor<NewPart> captor = ArgumentCaptor.forClass(NewPart.class);
        verify(partService).createPart(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("New Part");
        assertThat(captor.getValue().attributes()).containsEntry("k", "v");
    }

    @Test
    void supplier_batchMapping_deduplicatesLookupsAcrossParts() {
        PartController controller = new PartController(partService, supplierService);
        Part p1 = new Part("PART-1", "A", null, PartType.NIC, LifecycleState.ACTIVE, "A", "SUP-1", Map.of(), Instant.now(), Instant.now());
        Part p2 = new Part("PART-2", "B", null, PartType.NIC, LifecycleState.ACTIVE, "A", "SUP-1", Map.of(), Instant.now(), Instant.now());
        Supplier supplier = new Supplier("SUP-1", "Acme", null, null, true);
        when(supplierService.getSupplier("SUP-1")).thenReturn(supplier);

        Map<Part, Supplier> result = controller.supplier(List.of(p1, p2));

        assertThat(result).containsEntry(p1, supplier).containsEntry(p2, supplier);
        verify(supplierService, org.mockito.Mockito.times(1)).getSupplier("SUP-1");
    }
}
