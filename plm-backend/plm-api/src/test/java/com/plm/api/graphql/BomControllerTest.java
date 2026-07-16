package com.plm.api.graphql;

import com.plm.service.api.BomService;
import com.plm.service.domain.BomNode;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BomControllerTest {

    @Mock
    private BomService bomService;

    @Test
    void bom_delegatesToService() {
        BomController controller = new BomController(bomService);
        Part rack = new Part("R-14", "Rack", null, PartType.RACK, LifecycleState.ACTIVE, "A", null, Map.of(), Instant.now(), Instant.now());
        BomNode node = new BomNode(rack, 1, List.of());
        when(bomService.getBom("R-14")).thenReturn(node);

        assertThat(controller.bom("R-14")).isEqualTo(node);
    }

    @Test
    void addBomComponent_delegatesWithArgs() {
        BomController controller = new BomController(bomService);
        controller.addBomComponent("R-14", "SRV-A1", 4);
        verify(bomService).addBomComponent("R-14", "SRV-A1", 4);
    }

    @Test
    void removeBomComponent_returnsTrueAndDelegates() {
        BomController controller = new BomController(bomService);
        Boolean result = controller.removeBomComponent("R-14", "SRV-A1");
        assertThat(result).isTrue();
        verify(bomService).removeBomComponent("R-14", "SRV-A1");
    }
}
