package com.plm.chatbot.tools;

import com.plm.chatbot.llm.ToolSpec;
import com.plm.service.api.BomService;
import com.plm.service.api.ChangeOrderService;
import com.plm.service.api.PartService;
import com.plm.service.api.SupplierService;
import com.plm.service.domain.ChangeOrderFilter;
import com.plm.service.domain.ChangeOrderStatus;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.PartFilter;
import com.plm.service.domain.PartType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps each read-only internal-service-layer method as an LLM tool with a JSON-schema
 * description. These are the only tools exposed to the chatbot (read-only first, per the
 * project's authz posture) and every execute() call is a direct, in-process Java method call
 * into plm-service — never GraphQL, never a raw DB query.
 */
@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public ToolRegistry(PartService partService, BomService bomService,
                         SupplierService supplierService, ChangeOrderService changeOrderService) {
        register(getPart(partService));
        register(searchParts(partService));
        register(getBom(bomService));
        register(whereUsed(bomService));
        register(getSupplier(supplierService));
        register(listSuppliers(supplierService));
        register(supplierParts(supplierService));
        register(getChangeOrder(changeOrderService));
        register(listChangeOrders(changeOrderService));
    }

    private void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public List<ToolSpec> specs() {
        return tools.values().stream().map(ToolDefinition::spec).toList();
    }

    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    // -- part / BOM tools -----------------------------------------------------------------

    private ToolDefinition getPart(PartService partService) {
        Map<String, Object> schema = objectSchema(
                prop("id", "string", "The part id, e.g. PSU-2200 or R-14"),
                List.of("id"));
        return new ToolDefinition(
                new ToolSpec("get_part", "Get full details (attributes, lifecycle state, revision, supplier) for a single part by its id.", schema),
                args -> partService.getPart(ToolArgs.requireString(args, "id")));
    }

    private ToolDefinition searchParts(PartService partService) {
        Map<String, Object> schema = objectSchema(
                merge(
                        prop("nameContains", "string", "Case-insensitive substring to match against the part name"),
                        prop("type", "string", "Filter by part type: RACK, SERVER, PSU, NIC, CABLE, OTHER"),
                        prop("lifecycleState", "string", "Filter by lifecycle state: DESIGN, ACTIVE, EOL"),
                        prop("supplierId", "string", "Filter by supplier id"),
                        prop("page", "integer", "Zero-based page number, default 0"),
                        prop("size", "integer", "Page size, default 20")),
                List.of());
        return new ToolDefinition(
                new ToolSpec("search_parts", "Search/filter parts by name, type, lifecycle state, and/or supplier. Use this to answer questions like 'which parts are EOL?' or 'find PSUs from supplier X'.", schema),
                args -> partService.searchParts(new PartFilter(
                        ToolArgs.getString(args, "nameContains"),
                        ToolArgs.getEnum(args, "type", PartType.class),
                        ToolArgs.getEnum(args, "lifecycleState", LifecycleState.class),
                        ToolArgs.getString(args, "supplierId"),
                        ToolArgs.getInt(args, "page", 0),
                        ToolArgs.getInt(args, "size", 20))));
    }

    private ToolDefinition getBom(BomService bomService) {
        Map<String, Object> schema = objectSchema(
                prop("partId", "string", "The id of the assembly/part to get the Bill of Materials tree for, e.g. R-14"),
                List.of("partId"));
        return new ToolDefinition(
                new ToolSpec("get_bom", "Get the full Bill-of-Materials tree for a part (e.g. what a rack contains). Use for questions like 'show the BOM for rack R-14'.", schema),
                args -> bomService.getBom(ToolArgs.requireString(args, "partId")));
    }

    private ToolDefinition whereUsed(BomService bomService) {
        Map<String, Object> schema = objectSchema(
                prop("partId", "string", "The id of the part to find containing assemblies for, e.g. PSU-2200"),
                List.of("partId"));
        return new ToolDefinition(
                new ToolSpec("where_used", "Find every assembly that directly or indirectly contains a given part. Use for questions like 'what assemblies use PSU-2200?'.", schema),
                args -> bomService.whereUsed(ToolArgs.requireString(args, "partId")));
    }

    // -- supplier tools ---------------------------------------------------------------------

    private ToolDefinition getSupplier(SupplierService supplierService) {
        Map<String, Object> schema = objectSchema(
                prop("id", "string", "The supplier id"),
                List.of("id"));
        return new ToolDefinition(
                new ToolSpec("get_supplier", "Get details for a single supplier by id.", schema),
                args -> supplierService.getSupplier(ToolArgs.requireString(args, "id")));
    }

    private ToolDefinition listSuppliers(SupplierService supplierService) {
        Map<String, Object> schema = objectSchema(
                merge(prop("page", "integer", "Zero-based page number, default 0"),
                        prop("size", "integer", "Page size, default 20")),
                List.of());
        return new ToolDefinition(
                new ToolSpec("list_suppliers", "List all suppliers, paginated. Use to find a supplier's id from its name.", schema),
                args -> supplierService.listSuppliers(ToolArgs.getInt(args, "page", 0), ToolArgs.getInt(args, "size", 20)));
    }

    private ToolDefinition supplierParts(SupplierService supplierService) {
        Map<String, Object> schema = objectSchema(
                prop("supplierId", "string", "The supplier id"),
                List.of("supplierId"));
        return new ToolDefinition(
                new ToolSpec("supplier_parts", "List all parts supplied by a given supplier. Use for questions like 'which parts from supplier Acme are EOL?' (combine with the part's lifecycleState).", schema),
                args -> supplierService.supplierParts(ToolArgs.requireString(args, "supplierId")));
    }

    // -- change order tools -------------------------------------------------------------------

    private ToolDefinition getChangeOrder(ChangeOrderService changeOrderService) {
        Map<String, Object> schema = objectSchema(
                prop("id", "string", "The change order (ECO) id"),
                List.of("id"));
        return new ToolDefinition(
                new ToolSpec("get_change_order", "Get details for a single Engineering Change Order (ECO) by id.", schema),
                args -> changeOrderService.getChangeOrder(ToolArgs.requireString(args, "id")));
    }

    private ToolDefinition listChangeOrders(ChangeOrderService changeOrderService) {
        Map<String, Object> schema = objectSchema(
                merge(prop("status", "string", "Filter by status: DRAFT, SUBMITTED, APPROVED, REJECTED, IMPLEMENTED"),
                        prop("partId", "string", "Filter to change orders affecting this part id"),
                        prop("page", "integer", "Zero-based page number, default 0"),
                        prop("size", "integer", "Page size, default 20")),
                List.of());
        return new ToolDefinition(
                new ToolSpec("list_change_orders", "Search/list Engineering Change Orders by status and/or affected part.", schema),
                args -> changeOrderService.changeOrders(new ChangeOrderFilter(
                        ToolArgs.getEnum(args, "status", ChangeOrderStatus.class),
                        ToolArgs.getString(args, "partId"),
                        ToolArgs.getInt(args, "page", 0),
                        ToolArgs.getInt(args, "size", 20))));
    }

    // -- schema helpers ---------------------------------------------------------------------

    private static Map<String, Object> prop(String name, String type, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(name, Map.of("type", type, "description", description));
        return m;
    }

    @SafeVarargs
    private static Map<String, Object> merge(Map<String, Object>... maps) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Map<String, Object> m : maps) {
            merged.putAll(m);
        }
        return merged;
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
}
