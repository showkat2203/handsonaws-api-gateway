package com.plm.service.impl;

import com.plm.service.domain.NewSupplier;
import com.plm.service.domain.Supplier;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private GraphRepository graphRepository;
    @Mock
    private PartAttributeRepository partAttributeRepository;

    private SupplierServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupplierServiceImpl(supplierRepository, graphRepository, partAttributeRepository);
    }

    @AfterEach
    void clearPrincipal() {
        PlmPrincipalContext.clear();
    }

    @Test
    void deleteSupplier_rejectsWhenPartsStillLinked() {
        PlmPrincipalContext.set(new PlmPrincipal("admin1", Set.of(Role.ADMIN)));
        when(supplierRepository.findById("SUP-1")).thenReturn(Optional.of(new Supplier("SUP-1", "Acme", null, null, true)));
        when(graphRepository.getPartsBySupplier("SUP-1")).thenReturn(List.of("PART-1"));

        assertThatThrownBy(() -> service.deleteSupplier("SUP-1")).isInstanceOf(ValidationException.class);
    }

    @Test
    void createSupplier_persistsAndCreatesVertex() {
        PlmPrincipalContext.set(new PlmPrincipal("engineer1", Set.of(Role.ENGINEER)));

        Supplier created = service.createSupplier(new NewSupplier("Acme", "sales@acme.com", "1 Main St", true));

        assertThat(created.getId()).startsWith("SUP-");
        assertThat(created.getName()).isEqualTo("Acme");
    }

    @Test
    void listSuppliers_paginatesSortedByName() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                new Supplier("SUP-2", "Zeta", null, null, true),
                new Supplier("SUP-1", "Acme", null, null, true)
        ));

        var page = service.listSuppliers(0, 20);

        assertThat(page.items()).extracting(Supplier::getName).containsExactly("Acme", "Zeta");
    }
}
