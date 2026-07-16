package com.plm.service.impl;

import com.plm.service.domain.ChangeOrder;
import com.plm.service.domain.ChangeOrderStatus;
import com.plm.service.domain.NewChangeOrder;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.ChangeOrderRepository;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.security.PlmPrincipal;
import com.plm.service.security.PlmPrincipalContext;
import com.plm.service.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeOrderServiceImplTest {

    @Mock
    private ChangeOrderRepository changeOrderRepository;
    @Mock
    private PartAttributeRepository partAttributeRepository;

    private ChangeOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChangeOrderServiceImpl(changeOrderRepository, partAttributeRepository);
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
    }

    @AfterEach
    void clearPrincipal() {
        PlmPrincipalContext.clear();
    }

    private static ChangeOrder eco(String id, ChangeOrderStatus status) {
        return new ChangeOrder(id, "Swap PSU", "desc", status, List.of("PART-1"), "engineer1",
                Instant.now(), Instant.now(), null);
    }

    @Test
    void createChangeOrder_startsInDraft() {
        when(partAttributeRepository.existsById("PART-1")).thenReturn(true);

        ChangeOrder created = service.createChangeOrder(new NewChangeOrder("Swap PSU", "desc", List.of("PART-1")), "engineer1");

        assertThat(created.getStatus()).isEqualTo(ChangeOrderStatus.DRAFT);
        assertThat(created.getId()).startsWith("ECO-");
    }

    @Test
    void fullWorkflow_draftToImplemented() {
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.DRAFT)));
        ChangeOrder submitted = service.submitChangeOrder("ECO-1");
        assertThat(submitted.getStatus()).isEqualTo(ChangeOrderStatus.SUBMITTED);

        PlmPrincipalContext.set(new PlmPrincipal("admin1", Set.of(Role.ADMIN)));
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.SUBMITTED)));
        ChangeOrder approved = service.approveChangeOrder("ECO-1");
        assertThat(approved.getStatus()).isEqualTo(ChangeOrderStatus.APPROVED);

        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.APPROVED)));
        ChangeOrder implemented = service.implementChangeOrder("ECO-1");
        assertThat(implemented.getStatus()).isEqualTo(ChangeOrderStatus.IMPLEMENTED);
        assertThat(implemented.getImplementedAt()).isNotNull();
    }

    @Test
    void submitChangeOrder_rejectsWhenNotDraft() {
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.APPROVED)));

        assertThatThrownBy(() -> service.submitChangeOrder("ECO-1")).isInstanceOf(ValidationException.class);
    }

    @Test
    void deleteChangeOrder_rejectsWhenApproved() {
        PlmPrincipalContext.set(new PlmPrincipal("admin1", Set.of(Role.ADMIN)));
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.APPROVED)));

        assertThatThrownBy(() -> service.deleteChangeOrder("ECO-1")).isInstanceOf(ValidationException.class);
    }

    @Test
    void updateChangeOrder_rejectsWhenNotDraft() {
        when(changeOrderRepository.findById("ECO-1")).thenReturn(Optional.of(eco("ECO-1", ChangeOrderStatus.SUBMITTED)));

        assertThatThrownBy(() -> service.updateChangeOrder("ECO-1", new NewChangeOrder("t", "d", List.of())))
                .isInstanceOf(ValidationException.class);
    }
}
