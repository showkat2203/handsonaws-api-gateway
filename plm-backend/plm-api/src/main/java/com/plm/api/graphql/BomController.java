package com.plm.api.graphql;

import com.plm.service.api.BomService;
import com.plm.service.domain.BomNode;
import com.plm.service.domain.Part;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Thin GraphQL transport wrapper over {@link BomService}. Contains no business logic — every
 * method delegates straight into the internal service layer that the chatbot also calls.
 */
@Controller
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @QueryMapping
    public BomNode bom(@Argument String partId) {
        return bomService.getBom(partId);
    }

    @QueryMapping
    public List<Part> whereUsed(@Argument String partId) {
        return bomService.whereUsed(partId);
    }

    @MutationMapping
    public BomNode addBomComponent(@Argument String parentId, @Argument String childId, @Argument int quantity) {
        return bomService.addBomComponent(parentId, childId, quantity);
    }

    @MutationMapping
    public Boolean removeBomComponent(@Argument String parentId, @Argument String childId) {
        bomService.removeBomComponent(parentId, childId);
        return true;
    }
}
