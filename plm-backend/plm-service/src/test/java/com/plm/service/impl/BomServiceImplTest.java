package com.plm.service.impl;

import com.plm.service.domain.BomNode;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartType;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.repository.graph.BomEdge;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BomServiceImplTest {

    @Mock
    private GraphRepository graphRepository;
    @Mock
    private PartAttributeRepository partAttributeRepository;

    private BomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BomServiceImpl(graphRepository, partAttributeRepository);
    }

    @AfterEach
    void clearPrincipal() {
        PlmPrincipalContext.clear();
    }

    private static Part part(String id, String name) {
        return new Part(id, name, null, PartType.OTHER, LifecycleState.ACTIVE, "A", null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void getBom_buildsNestedTree() {
        when(graphRepository.partVertexExists("RACK-1")).thenReturn(true);
        when(graphRepository.getDirectChildren("RACK-1")).thenReturn(List.of(new BomEdge("SERVER-1", 2)));
        when(graphRepository.getDirectChildren("SERVER-1")).thenReturn(List.of(new BomEdge("PSU-1", 1)));
        when(graphRepository.getDirectChildren("PSU-1")).thenReturn(List.of());
        when(partAttributeRepository.findById("RACK-1")).thenReturn(Optional.of(part("RACK-1", "Rack R-14")));
        when(partAttributeRepository.findById("SERVER-1")).thenReturn(Optional.of(part("SERVER-1", "Server")));
        when(partAttributeRepository.findById("PSU-1")).thenReturn(Optional.of(part("PSU-1", "PSU-2200")));

        BomNode root = service.getBom("RACK-1");

        assertThat(root.part().getId()).isEqualTo("RACK-1");
        assertThat(root.children()).hasSize(1);
        BomNode server = root.children().get(0);
        assertThat(server.part().getId()).isEqualTo("SERVER-1");
        assertThat(server.quantity()).isEqualTo(2);
        assertThat(server.children()).hasSize(1);
        assertThat(server.children().get(0).part().getId()).isEqualTo("PSU-1");
    }

    @Test
    void getBom_throwsNotFoundForMissingRoot() {
        when(graphRepository.partVertexExists("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.getBom("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void whereUsed_hydratesAncestorParts() {
        when(graphRepository.partVertexExists("PSU-1")).thenReturn(true);
        when(graphRepository.getWhereUsed("PSU-1")).thenReturn(List.of("SERVER-1", "RACK-1"));
        when(partAttributeRepository.findById("SERVER-1")).thenReturn(Optional.of(part("SERVER-1", "Server")));
        when(partAttributeRepository.findById("RACK-1")).thenReturn(Optional.of(part("RACK-1", "Rack R-14")));

        List<Part> result = service.whereUsed("PSU-1");

        assertThat(result).extracting(Part::getId).containsExactlyInAnyOrder("SERVER-1", "RACK-1");
    }

    @Test
    void addBomComponent_rejectsCycle() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(partAttributeRepository.existsById("A")).thenReturn(true);
        when(partAttributeRepository.existsById("B")).thenReturn(true);
        when(graphRepository.getWhereUsed("A")).thenReturn(List.of("B"));

        assertThatThrownBy(() -> service.addBomComponent("A", "B", 1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void addBomComponent_rejectsSelfReference() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));

        assertThatThrownBy(() -> service.addBomComponent("A", "A", 1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void addBomComponent_addsEdgeWhenValid() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));
        when(partAttributeRepository.existsById("A")).thenReturn(true);
        when(partAttributeRepository.existsById("B")).thenReturn(true);
        when(graphRepository.getWhereUsed("A")).thenReturn(List.of());
        when(graphRepository.getDirectChildren("B")).thenReturn(List.of());
        when(partAttributeRepository.findById("B")).thenReturn(Optional.of(part("B", "Child")));

        BomNode node = service.addBomComponent("A", "B", 3);

        verify(graphRepository).addBomEdge("A", "B", 3);
        assertThat(node.quantity()).isEqualTo(3);
    }
}
