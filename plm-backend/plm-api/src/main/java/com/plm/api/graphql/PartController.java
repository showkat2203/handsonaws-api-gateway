package com.plm.api.graphql;

import com.plm.api.graphql.input.NewPartInput;
import com.plm.api.graphql.input.PartFilterInput;
import com.plm.api.graphql.input.PartUpdateInput;
import com.plm.service.api.PartService;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.PartFilter;
import com.plm.service.domain.PartUpdate;
import com.plm.service.domain.Supplier;
import com.plm.service.api.SupplierService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Thin GraphQL transport wrapper over {@link PartService}. Contains no business logic — every
 * method delegates straight into the internal service layer that the chatbot also calls.
 */
@Controller
public class PartController {

    private final PartService partService;
    private final SupplierService supplierService;

    public PartController(PartService partService, SupplierService supplierService) {
        this.partService = partService;
        this.supplierService = supplierService;
    }

    @QueryMapping
    public Part part(@Argument String id) {
        return partService.getPart(id);
    }

    @QueryMapping
    public PageResult<Part> searchParts(@Argument PartFilterInput filter) {
        PartFilter f = filter == null
                ? PartFilter.empty()
                : new PartFilter(filter.nameContains(), filter.type(), filter.lifecycleState(),
                        filter.supplierId(),
                        filter.page() == null ? 0 : filter.page(),
                        filter.size() == null ? 20 : filter.size());
        return partService.searchParts(f);
    }

    @MutationMapping
    public Part createPart(@Argument("input") NewPartInput input) {
        NewPart newPart = new NewPart(input.name(), input.description(), input.type(), input.revision(),
                input.supplierId(), GraphQLMappers.toAttributeMap(input.attributes()));
        return partService.createPart(newPart);
    }

    @MutationMapping
    public Part updatePart(@Argument String id, @Argument("input") PartUpdateInput input) {
        PartUpdate update = new PartUpdate(input.name(), input.description(), input.revision(),
                input.supplierId(), GraphQLMappers.toAttributeMap(input.attributes()));
        return partService.updatePart(id, update);
    }

    @MutationMapping
    public Boolean deletePart(@Argument String id) {
        partService.deletePart(id);
        return true;
    }

    @MutationMapping
    public Part updateLifecycle(@Argument String id, @Argument LifecycleState state) {
        return partService.updateLifecycle(id, state);
    }

    @SchemaMapping(typeName = "Part", field = "attributes")
    public Iterable<GraphQLMappers.KeyValue> attributes(Part part) {
        return GraphQLMappers.toKeyValues(part.getAttributes());
    }

    /**
     * Batches Part -&gt; Supplier resolution for an entire result page into one deduplicated pass
     * (Spring for GraphQL runs this through a DataLoader internally), instead of one supplier
     * lookup per part row.
     */
    @BatchMapping(typeName = "Part", field = "supplier")
    public Map<Part, Supplier> supplier(List<Part> parts) {
        Map<Part, Supplier> result = new java.util.HashMap<>();
        Map<String, Supplier> cache = new java.util.HashMap<>();
        for (Part p : parts) {
            String supplierId = p.getSupplierId();
            if (supplierId == null) {
                continue;
            }
            Supplier supplier = cache.computeIfAbsent(supplierId, id -> {
                try {
                    return supplierService.getSupplier(id);
                } catch (com.plm.service.exception.NotFoundException e) {
                    return null;
                }
            });
            if (supplier != null) {
                result.put(p, supplier);
            }
        }
        return result;
    }
}
