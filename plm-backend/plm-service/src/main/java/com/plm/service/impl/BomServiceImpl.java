package com.plm.service.impl;

import com.plm.service.api.BomService;
import com.plm.service.domain.BomNode;
import com.plm.service.domain.Part;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.repository.graph.BomEdge;
import com.plm.service.repository.graph.GraphRepository;
import com.plm.service.security.Authz;
import com.plm.service.security.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BomServiceImpl implements BomService {

    private final GraphRepository graphRepository;
    private final PartAttributeRepository partAttributeRepository;

    public BomServiceImpl(GraphRepository graphRepository, PartAttributeRepository partAttributeRepository) {
        this.graphRepository = graphRepository;
        this.partAttributeRepository = partAttributeRepository;
    }

    @Override
    public BomNode getBom(String partId) {
        if (!graphRepository.partVertexExists(partId)) {
            throw NotFoundException.forEntity("Part", partId);
        }
        return buildNode(partId, 1, new HashSet<>());
    }

    private BomNode buildNode(String partId, int quantity, Set<String> visiting) {
        if (!visiting.add(partId)) {
            throw new ValidationException("Cycle detected in BOM graph at part " + partId);
        }
        Part part = partAttributeRepository.findById(partId)
                .orElseThrow(() -> NotFoundException.forEntity("Part", partId));

        List<BomEdge> children = graphRepository.getDirectChildren(partId);
        List<BomNode> childNodes = new ArrayList<>(children.size());
        for (BomEdge edge : children) {
            childNodes.add(buildNode(edge.partId(), edge.quantity(), visiting));
        }
        visiting.remove(partId);
        return new BomNode(part, quantity, childNodes);
    }

    @Override
    public List<Part> whereUsed(String partId) {
        if (!graphRepository.partVertexExists(partId)) {
            throw NotFoundException.forEntity("Part", partId);
        }
        return graphRepository.getWhereUsed(partId).stream()
                .map(partAttributeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    @Override
    public BomNode addBomComponent(String parentId, String childId, int quantity) {
        Authz.require(Role.ENGINEER);
        if (parentId.equals(childId)) {
            throw new ValidationException("A part cannot contain itself");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
        if (!partAttributeRepository.existsById(parentId)) {
            throw NotFoundException.forEntity("Part", parentId);
        }
        if (!partAttributeRepository.existsById(childId)) {
            throw NotFoundException.forEntity("Part", childId);
        }
        if (graphRepository.getWhereUsed(parentId).contains(childId)) {
            throw new ValidationException("Adding %s under %s would create a cycle".formatted(childId, parentId));
        }

        graphRepository.addBomEdge(parentId, childId, quantity);
        return buildNode(childId, quantity, new HashSet<>());
    }

    @Override
    public void removeBomComponent(String parentId, String childId) {
        Authz.require(Role.ENGINEER);
        graphRepository.removeBomEdge(parentId, childId);
    }
}
