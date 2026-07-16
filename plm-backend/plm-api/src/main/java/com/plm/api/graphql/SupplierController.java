package com.plm.api.graphql;

import com.plm.api.graphql.input.NewSupplierInput;
import com.plm.service.api.SupplierService;
import com.plm.service.domain.NewSupplier;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.Supplier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Thin GraphQL transport wrapper over {@link SupplierService}. Contains no business logic —
 * every method delegates straight into the internal service layer that the chatbot also calls.
 */
@Controller
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @QueryMapping
    public Supplier supplier(@Argument String id) {
        return supplierService.getSupplier(id);
    }

    @QueryMapping
    public PageResult<Supplier> suppliers(@Argument Integer page, @Argument Integer size) {
        return supplierService.listSuppliers(page == null ? 0 : page, size == null ? 20 : size);
    }

    @QueryMapping
    public List<Part> supplierParts(@Argument String supplierId) {
        return supplierService.supplierParts(supplierId);
    }

    @MutationMapping
    public Supplier createSupplier(@Argument("input") NewSupplierInput input) {
        return supplierService.createSupplier(
                new NewSupplier(input.name(), input.contactEmail(), input.address(),
                        input.active() == null || input.active()));
    }

    @MutationMapping
    public Supplier updateSupplier(@Argument String id, @Argument("input") NewSupplierInput input) {
        return supplierService.updateSupplier(id,
                new NewSupplier(input.name(), input.contactEmail(), input.address(),
                        input.active() == null || input.active()));
    }

    @MutationMapping
    public Boolean deleteSupplier(@Argument String id) {
        supplierService.deleteSupplier(id);
        return true;
    }

    /** Batches Supplier -&gt; parts resolution across a suppliers page instead of one graph call per row. */
    @BatchMapping(typeName = "Supplier", field = "parts")
    public Map<Supplier, List<Part>> parts(List<Supplier> suppliers) {
        Map<Supplier, List<Part>> result = new java.util.HashMap<>();
        for (Supplier s : suppliers) {
            result.put(s, supplierService.supplierParts(s.getId()));
        }
        return result;
    }
}
