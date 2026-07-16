package com.plm.service.impl;

import com.plm.service.api.SupplierService;
import com.plm.service.domain.NewSupplier;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.Supplier;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.repository.dynamo.SupplierRepository;
import com.plm.service.repository.graph.GraphRepository;
import com.plm.service.security.Authz;
import com.plm.service.security.Role;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final GraphRepository graphRepository;
    private final PartAttributeRepository partAttributeRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository, GraphRepository graphRepository,
                                PartAttributeRepository partAttributeRepository) {
        this.supplierRepository = supplierRepository;
        this.graphRepository = graphRepository;
        this.partAttributeRepository = partAttributeRepository;
    }

    @Override
    public Supplier getSupplier(String id) {
        return supplierRepository.findById(id).orElseThrow(() -> NotFoundException.forEntity("Supplier", id));
    }

    @Override
    public PageResult<Supplier> listSuppliers(int page, int size) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? 20 : Math.min(size, 200);
        List<Supplier> all = supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName))
                .toList();
        int from = Math.min(p * s, all.size());
        int to = Math.min(from + s, all.size());
        return PageResult.of(all.subList(from, to), all.size(), p, s);
    }

    @Override
    public Supplier createSupplier(NewSupplier newSupplier) {
        Authz.require(Role.ENGINEER);
        if (newSupplier.name() == null || newSupplier.name().isBlank()) {
            throw new ValidationException("Supplier name is required");
        }
        String id = "SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Supplier supplier = new Supplier(id, newSupplier.name(), newSupplier.contactEmail(),
                newSupplier.address(), newSupplier.active());
        graphRepository.upsertSupplierVertex(id);
        supplierRepository.save(supplier);
        return supplier;
    }

    @Override
    public Supplier updateSupplier(String id, NewSupplier update) {
        Authz.require(Role.ENGINEER);
        getSupplier(id); // 404 if missing
        Supplier updated = new Supplier(id, update.name(), update.contactEmail(), update.address(), update.active());
        supplierRepository.save(updated);
        return updated;
    }

    @Override
    public void deleteSupplier(String id) {
        Authz.require(Role.ADMIN);
        getSupplier(id); // 404 if missing
        List<String> linkedParts = graphRepository.getPartsBySupplier(id);
        if (!linkedParts.isEmpty()) {
            throw new ValidationException(
                    "Cannot delete supplier %s: still linked to %d part(s)".formatted(id, linkedParts.size()));
        }
        supplierRepository.deleteById(id);
    }

    @Override
    public List<Part> supplierParts(String supplierId) {
        getSupplier(supplierId); // 404 if missing
        return graphRepository.getPartsBySupplier(supplierId).stream()
                .map(partAttributeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
