package com.plm.api.graphql;

import com.plm.api.graphql.input.ChangeOrderFilterInput;
import com.plm.api.graphql.input.NewChangeOrderInput;
import com.plm.service.api.ChangeOrderService;
import com.plm.service.api.PartService;
import com.plm.service.domain.ChangeOrder;
import com.plm.service.domain.ChangeOrderFilter;
import com.plm.service.domain.NewChangeOrder;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.exception.NotFoundException;
import com.plm.service.security.PlmPrincipalContext;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Thin GraphQL transport wrapper over {@link ChangeOrderService}. Contains no business logic —
 * every method delegates straight into the internal service layer that the chatbot also calls.
 */
@Controller
public class ChangeOrderController {

    private final ChangeOrderService changeOrderService;
    private final PartService partService;

    public ChangeOrderController(ChangeOrderService changeOrderService, PartService partService) {
        this.changeOrderService = changeOrderService;
        this.partService = partService;
    }

    @QueryMapping
    public ChangeOrder changeOrder(@Argument String id) {
        return changeOrderService.getChangeOrder(id);
    }

    @QueryMapping
    public PageResult<ChangeOrder> changeOrders(@Argument ChangeOrderFilterInput filter) {
        ChangeOrderFilter f = filter == null
                ? ChangeOrderFilter.empty()
                : new ChangeOrderFilter(filter.status(), filter.partId(),
                        filter.page() == null ? 0 : filter.page(),
                        filter.size() == null ? 20 : filter.size());
        return changeOrderService.changeOrders(f);
    }

    @MutationMapping
    public ChangeOrder createChangeOrder(@Argument("input") NewChangeOrderInput input) {
        String createdBy = PlmPrincipalContext.get().getUsername();
        List<String> affected = input.affectedPartIds() == null ? List.of() : input.affectedPartIds();
        return changeOrderService.createChangeOrder(new NewChangeOrder(input.title(), input.description(), affected), createdBy);
    }

    @MutationMapping
    public ChangeOrder updateChangeOrder(@Argument String id, @Argument("input") NewChangeOrderInput input) {
        List<String> affected = input.affectedPartIds() == null ? List.of() : input.affectedPartIds();
        return changeOrderService.updateChangeOrder(id, new NewChangeOrder(input.title(), input.description(), affected));
    }

    @MutationMapping
    public ChangeOrder submitChangeOrder(@Argument String id) {
        return changeOrderService.submitChangeOrder(id);
    }

    @MutationMapping
    public ChangeOrder approveChangeOrder(@Argument String id) {
        return changeOrderService.approveChangeOrder(id);
    }

    @MutationMapping
    public ChangeOrder rejectChangeOrder(@Argument String id) {
        return changeOrderService.rejectChangeOrder(id);
    }

    @MutationMapping
    public ChangeOrder implementChangeOrder(@Argument String id) {
        return changeOrderService.implementChangeOrder(id);
    }

    @MutationMapping
    public Boolean deleteChangeOrder(@Argument String id) {
        changeOrderService.deleteChangeOrder(id);
        return true;
    }

    /** Batches ChangeOrder -&gt; affectedParts resolution across a page instead of one lookup per row. */
    @BatchMapping(typeName = "ChangeOrder", field = "affectedParts")
    public Map<ChangeOrder, List<Part>> affectedParts(List<ChangeOrder> changeOrders) {
        Map<String, Part> cache = new java.util.HashMap<>();
        Map<ChangeOrder, List<Part>> result = new java.util.HashMap<>();
        for (ChangeOrder co : changeOrders) {
            List<Part> parts = co.getAffectedPartIds().stream()
                    .map(id -> cache.computeIfAbsent(id, i -> {
                        try {
                            return partService.getPart(i);
                        } catch (NotFoundException e) {
                            return null;
                        }
                    }))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            result.put(co, parts);
        }
        return result;
    }
}
