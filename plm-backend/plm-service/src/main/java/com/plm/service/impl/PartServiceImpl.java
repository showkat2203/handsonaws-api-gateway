package com.plm.service.impl;

import com.plm.service.api.PartService;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartFilter;
import com.plm.service.domain.PartUpdate;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.repository.dynamo.SupplierRepository;
import com.plm.service.repository.graph.GraphRepository;
import com.plm.service.security.Authz;
import com.plm.service.security.Role;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PartServiceImpl implements PartService {

    private final PartAttributeRepository partAttributeRepository;
    private final SupplierRepository supplierRepository;
    private final GraphRepository graphRepository;

    public PartServiceImpl(PartAttributeRepository partAttributeRepository,
                            SupplierRepository supplierRepository,
                            GraphRepository graphRepository) {
        this.partAttributeRepository = partAttributeRepository;
        this.supplierRepository = supplierRepository;
        this.graphRepository = graphRepository;
    }

    @Override
    public Part getPart(String id) {
        return partAttributeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Part", id));
    }

    @Override
    public PageResult<Part> searchParts(PartFilter filter) {
        PartFilter f = filter == null ? PartFilter.empty() : filter;
        List<Part> filtered = partAttributeRepository.findAll().stream()
                .filter(p -> f.nameContains() == null || f.nameContains().isBlank()
                        || p.getName().toLowerCase().contains(f.nameContains().toLowerCase()))
                .filter(p -> f.type() == null || p.getType() == f.type())
                .filter(p -> f.lifecycleState() == null || p.getLifecycleState() == f.lifecycleState())
                .filter(p -> f.supplierId() == null || f.supplierId().equals(p.getSupplierId()))
                .sorted(Comparator.comparing(Part::getName))
                .toList();

        int from = Math.min(f.page() * f.size(), filtered.size());
        int to = Math.min(from + f.size(), filtered.size());
        return PageResult.of(filtered.subList(from, to), filtered.size(), f.page(), f.size());
    }

    @Override
    public Part createPart(NewPart newPart) {
        Authz.require(Role.ENGINEER);
        String id = "PART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return persistNewPart(id, newPart, LifecycleState.DESIGN);
    }

    @Override
    public Part createPartWithId(String id, NewPart newPart, LifecycleState initialState) {
        Authz.require(Role.ADMIN);
        return persistNewPart(id, newPart, initialState);
    }

    private Part persistNewPart(String id, NewPart newPart, LifecycleState initialState) {
        if (newPart.name() == null || newPart.name().isBlank()) {
            throw new ValidationException("Part name is required");
        }
        if (newPart.supplierId() != null && !supplierRepository.existsById(newPart.supplierId())) {
            throw NotFoundException.forEntity("Supplier", newPart.supplierId());
        }

        Instant now = Instant.now();
        Part part = new Part(id, newPart.name(), newPart.description(), newPart.type(),
                initialState, newPart.revision(), newPart.supplierId(), newPart.attributes(), now, now);

        graphRepository.upsertPartVertex(id);
        if (newPart.supplierId() != null) {
            graphRepository.linkSupplierToPart(newPart.supplierId(), id);
        }
        partAttributeRepository.save(part);
        return part;
    }

    @Override
    public Part updatePart(String id, PartUpdate update) {
        Authz.require(Role.ENGINEER);
        Part existing = getPart(id);

        String supplierId = update.supplierId() != null ? update.supplierId() : existing.getSupplierId();
        if (update.supplierId() != null && !supplierRepository.existsById(update.supplierId())) {
            throw NotFoundException.forEntity("Supplier", update.supplierId());
        }

        Part updated = new Part(
                id,
                update.name() != null ? update.name() : existing.getName(),
                update.description() != null ? update.description() : existing.getDescription(),
                existing.getType(),
                existing.getLifecycleState(),
                update.revision() != null ? update.revision() : existing.getRevision(),
                supplierId,
                update.attributes() != null ? update.attributes() : existing.getAttributes(),
                existing.getCreatedAt(),
                Instant.now());

        if (update.supplierId() != null && !update.supplierId().equals(existing.getSupplierId())) {
            graphRepository.linkSupplierToPart(update.supplierId(), id);
        }

        partAttributeRepository.save(updated);
        return updated;
    }

    @Override
    public void deletePart(String id) {
        Authz.require(Role.ADMIN);
        if (!partAttributeRepository.existsById(id)) {
            throw NotFoundException.forEntity("Part", id);
        }
        graphRepository.deletePartVertex(id);
        partAttributeRepository.deleteById(id);
    }

    @Override
    public Part updateLifecycle(String id, LifecycleState newState) {
        Authz.require(Role.ENGINEER);
        Part existing = getPart(id);
        if (!existing.getLifecycleState().canTransitionTo(newState)) {
            throw new ValidationException("Cannot move part %s from %s back to %s"
                    .formatted(id, existing.getLifecycleState(), newState));
        }
        Part updated = existing.withLifecycleState(newState, Instant.now());
        partAttributeRepository.save(updated);
        return updated;
    }
}
