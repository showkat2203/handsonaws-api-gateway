package com.plm.service.impl;

import com.plm.service.api.ChangeOrderService;
import com.plm.service.domain.ChangeOrder;
import com.plm.service.domain.ChangeOrderFilter;
import com.plm.service.domain.ChangeOrderStatus;
import com.plm.service.domain.NewChangeOrder;
import com.plm.service.domain.PageResult;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import com.plm.service.repository.dynamo.ChangeOrderRepository;
import com.plm.service.repository.dynamo.PartAttributeRepository;
import com.plm.service.security.Authz;
import com.plm.service.security.Role;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeOrderServiceImpl implements ChangeOrderService {

    private final ChangeOrderRepository changeOrderRepository;
    private final PartAttributeRepository partAttributeRepository;

    public ChangeOrderServiceImpl(ChangeOrderRepository changeOrderRepository,
                                   PartAttributeRepository partAttributeRepository) {
        this.changeOrderRepository = changeOrderRepository;
        this.partAttributeRepository = partAttributeRepository;
    }

    @Override
    public ChangeOrder getChangeOrder(String id) {
        return changeOrderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("ChangeOrder", id));
    }

    @Override
    public PageResult<ChangeOrder> changeOrders(ChangeOrderFilter filter) {
        ChangeOrderFilter f = filter == null ? ChangeOrderFilter.empty() : filter;
        List<ChangeOrder> filtered = changeOrderRepository.findAll().stream()
                .filter(co -> f.status() == null || co.getStatus() == f.status())
                .filter(co -> f.partId() == null || co.getAffectedPartIds().contains(f.partId()))
                .sorted(Comparator.comparing(ChangeOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = Math.min(f.page() * f.size(), filtered.size());
        int to = Math.min(from + f.size(), filtered.size());
        return PageResult.of(filtered.subList(from, to), filtered.size(), f.page(), f.size());
    }

    @Override
    public ChangeOrder createChangeOrder(NewChangeOrder newChangeOrder, String createdBy) {
        Authz.require(Role.ENGINEER);
        if (newChangeOrder.title() == null || newChangeOrder.title().isBlank()) {
            throw new ValidationException("Change order title is required");
        }
        for (String partId : newChangeOrder.affectedPartIds()) {
            if (!partAttributeRepository.existsById(partId)) {
                throw NotFoundException.forEntity("Part", partId);
            }
        }
        String id = "ECO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = Instant.now();
        ChangeOrder co = new ChangeOrder(id, newChangeOrder.title(), newChangeOrder.description(),
                ChangeOrderStatus.DRAFT, newChangeOrder.affectedPartIds(), createdBy, now, now, null);
        changeOrderRepository.save(co);
        return co;
    }

    @Override
    public ChangeOrder updateChangeOrder(String id, NewChangeOrder update) {
        Authz.require(Role.ENGINEER);
        ChangeOrder existing = getChangeOrder(id);
        if (existing.getStatus() != ChangeOrderStatus.DRAFT) {
            throw new ValidationException("Only DRAFT change orders can be edited: " + id);
        }
        for (String partId : update.affectedPartIds()) {
            if (!partAttributeRepository.existsById(partId)) {
                throw NotFoundException.forEntity("Part", partId);
            }
        }
        ChangeOrder updated = new ChangeOrder(id, update.title(), update.description(), existing.getStatus(),
                update.affectedPartIds(), existing.getCreatedBy(), existing.getCreatedAt(), Instant.now(), null);
        changeOrderRepository.save(updated);
        return updated;
    }

    @Override
    public ChangeOrder submitChangeOrder(String id) {
        Authz.require(Role.ENGINEER);
        return transition(id, ChangeOrderStatus.DRAFT, ChangeOrderStatus.SUBMITTED);
    }

    @Override
    public ChangeOrder approveChangeOrder(String id) {
        Authz.require(Role.ADMIN);
        return transition(id, ChangeOrderStatus.SUBMITTED, ChangeOrderStatus.APPROVED);
    }

    @Override
    public ChangeOrder rejectChangeOrder(String id) {
        Authz.require(Role.ADMIN);
        return transition(id, ChangeOrderStatus.SUBMITTED, ChangeOrderStatus.REJECTED);
    }

    @Override
    public ChangeOrder implementChangeOrder(String id) {
        Authz.require(Role.ENGINEER);
        ChangeOrder existing = getChangeOrder(id);
        requireStatus(existing, ChangeOrderStatus.APPROVED);
        Instant now = Instant.now();
        ChangeOrder updated = existing.withStatus(ChangeOrderStatus.IMPLEMENTED, now, now);
        changeOrderRepository.save(updated);
        return updated;
    }

    @Override
    public void deleteChangeOrder(String id) {
        Authz.require(Role.ADMIN);
        ChangeOrder existing = getChangeOrder(id);
        if (existing.getStatus() != ChangeOrderStatus.DRAFT && existing.getStatus() != ChangeOrderStatus.REJECTED) {
            throw new ValidationException("Only DRAFT or REJECTED change orders can be deleted: " + id);
        }
        changeOrderRepository.deleteById(id);
    }

    private ChangeOrder transition(String id, ChangeOrderStatus expected, ChangeOrderStatus next) {
        ChangeOrder existing = getChangeOrder(id);
        requireStatus(existing, expected);
        ChangeOrder updated = existing.withStatus(next, Instant.now(), existing.getImplementedAt());
        changeOrderRepository.save(updated);
        return updated;
    }

    private void requireStatus(ChangeOrder co, ChangeOrderStatus expected) {
        if (co.getStatus() != expected) {
            throw new ValidationException("Change order %s must be %s but is %s"
                    .formatted(co.getId(), expected, co.getStatus()));
        }
    }
}
