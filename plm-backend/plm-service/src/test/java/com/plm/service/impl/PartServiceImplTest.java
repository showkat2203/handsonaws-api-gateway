package com.plm.service.impl;

import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartFilter;
import com.plm.service.domain.PartType;
import com.plm.service.domain.PartUpdate;
import com.plm.service.exception.AuthorizationException;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.repository.dynamo.SupplierRepository;
import com.plm.service.repository.graph.GraphRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceImplTest {

    @Mock
    private PartAttributeRepository partAttributeRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private GraphRepository graphRepository;

    private PartServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PartServiceImpl(partAttributeRepository, supplierRepository, graphRepository);
    }

    @AfterEach
    void clearPrincipal() {
        PlmPrincipalContext.clear();
    }

    private static Part samplePart(String id, LifecycleState state) {
        return new Part(id, "PSU 2200W", "Redundant PSU", PartType.PSU, state, "A", "SUP-1",
                Map.of(), Instant.now(), Instant.now());
    }

    @Test
    void getPart_returnsWhenPresent() {
        when(partAttributeRepository.findById("PART-1")).thenReturn(Optional.of(samplePart("PART-1", LifecycleState.ACTIVE)));

        Part result = service.getPart("PART-1");

        assertThat(result.getId()).isEqualTo("PART-1");
    }

    @Test
    void getPart_throwsNotFoundWhenMissing() {
        when(partAttributeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPart("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchParts_filtersByNameTypeLifecycleAndSupplier() {
        when(partAttributeRepository.findAll()).thenReturn(List.of(
                samplePart("PART-1", LifecycleState.ACTIVE),
                new Part("PART-2", "Rack R-14", null, PartType.RACK, LifecycleState.ACTIVE, "A", "SUP-2", Map.of(), Instant.now(), Instant.now()),
                new Part("PART-3", "PSU 1100W", null, PartType.PSU, LifecycleState.EOL, "A", "SUP-1", Map.of(), Instant.now(), Instant.now())
        ));

        var result = service.searchParts(new PartFilter("psu", PartType.PSU, null, "SUP-1", 0, 20));

        assertThat(result.items()).extracting(Part::getId).containsExactlyInAnyOrder("PART-1", "PART-3");
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void searchParts_paginates() {
        when(partAttributeRepository.findAll()).thenReturn(List.of(
                samplePart("PART-1", LifecycleState.ACTIVE),
                samplePart("PART-2", LifecycleState.ACTIVE),
                samplePart("PART-3", LifecycleState.ACTIVE)
        ));

        var page0 = service.searchParts(new PartFilter(null, null, null, null, 0, 2));
        var page1 = service.searchParts(new PartFilter(null, null, null, null, 1, 2));

        assertThat(page0.items()).hasSize(2);
        assertThat(page1.items()).hasSize(1);
        assertThat(page0.totalCount()).isEqualTo(3);
    }

    @Test
    void createPart_requiresEngineerRole() {
        NewPart newPart = new NewPart("New Part", "desc", PartType.NIC, "A", null, Map.of());

        assertThatThrownBy(() -> service.createPart(newPart)).isInstanceOf(AuthorizationException.class);
        verify(partAttributeRepository, never()).save(any());
    }

    @Test
    void createPart_persistsToBothStoresWhenAuthorized() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        NewPart newPart = new NewPart("New Part", "desc", PartType.NIC, "A", null, Map.of());

        Part created = service.createPart(newPart);

        assertThat(created.getLifecycleState()).isEqualTo(LifecycleState.DESIGN);
        assertThat(created.getId()).startsWith("PART-");
        verify(graphRepository).upsertPartVertex(created.getId());
        verify(partAttributeRepository).save(created);
    }

    @Test
    void createPart_rejectsUnknownSupplier() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(supplierRepository.existsById("SUP-X")).thenReturn(false);
        NewPart newPart = new NewPart("New Part", "desc", PartType.NIC, "A", "SUP-X", Map.of());

        assertThatThrownBy(() -> service.createPart(newPart)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateLifecycle_rejectsBackwardTransition() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(partAttributeRepository.findById("PART-1")).thenReturn(Optional.of(samplePart("PART-1", LifecycleState.ACTIVE)));

        assertThatThrownBy(() -> service.updateLifecycle("PART-1", LifecycleState.DESIGN))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateLifecycle_allowsForwardTransition() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(partAttributeRepository.findById("PART-1")).thenReturn(Optional.of(samplePart("PART-1", LifecycleState.ACTIVE)));

        Part updated = service.updateLifecycle("PART-1", LifecycleState.EOL);

        assertThat(updated.getLifecycleState()).isEqualTo(LifecycleState.EOL);
        verify(partAttributeRepository, times(1)).save(updated);
    }

    @Test
    void deletePart_requiresAdminRole() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));

        assertThatThrownBy(() -> service.deletePart("PART-1")).isInstanceOf(AuthorizationException.class);
    }

    @Test
    void deletePart_removesFromBothStoresWhenAuthorized() {
        PlmPrincipalContext.set(new PlmPrincipal("admin1", Set.of(Role.ADMIN)));
        when(partAttributeRepository.existsById("PART-1")).thenReturn(true);

        service.deletePart("PART-1");

        verify(graphRepository).deletePartVertex("PART-1");
        verify(partAttributeRepository).deleteById("PART-1");
    }

    @Test
    void createPartWithId_requiresAdminRole() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        NewPart newPart = new NewPart("Rack R-14", "desc", PartType.RACK, "A", null, Map.of());

        assertThatThrownBy(() -> service.createPartWithId("R-14", newPart, LifecycleState.ACTIVE))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void createPartWithId_persistsWithGivenIdAndState() {
        PlmPrincipalContext.set(new PlmPrincipal("admin1", Set.of(Role.ADMIN)));
        NewPart newPart = new NewPart("Rack R-14", "desc", PartType.RACK, "A", null, Map.of());

        Part created = service.createPartWithId("R-14", newPart, LifecycleState.ACTIVE);

        assertThat(created.getId()).isEqualTo("R-14");
        assertThat(created.getLifecycleState()).isEqualTo(LifecycleState.ACTIVE);
    }

    @Test
    void updatePart_appliesPartialUpdate() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(partAttributeRepository.findById("PART-1")).thenReturn(Optional.of(samplePart("PART-1", LifecycleState.ACTIVE)));

        Part updated = service.updatePart("PART-1", new PartUpdate("Renamed", null, null, null, null));

        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getDescription()).isEqualTo("Redundant PSU");
    }
}
