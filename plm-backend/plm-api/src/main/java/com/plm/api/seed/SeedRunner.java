package com.plm.api.seed;

import com.plm.service.api.BomService;
import com.plm.service.api.ChangeOrderService;
import com.plm.service.api.PartService;
import com.plm.service.api.SupplierService;
import com.plm.service.domain.LifecycleState;
import com.plm.service.domain.NewChangeOrder;
import com.plm.service.domain.NewPart;
import com.plm.service.domain.NewSupplier;
import com.plm.service.domain.PartType;
import com.plm.service.security.PlmPrincipal;
import com.plm.service.security.PlmPrincipalContext;
import com.plm.service.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads a realistic sample dataset (racks, servers, PSUs, NICs, cables, suppliers, BOM
 * hierarchy, and change orders) into both the graph and DynamoDB stores on startup, so the API,
 * chatbot, and frontend all have something meaningful to query out of the box. Runs after
 * {@link com.plm.service.repository.dynamo.DynamoTableInitializer} and is skipped if suppliers
 * already exist (idempotent across restarts) or if {@code plm.seed.enabled=false}.
 */
@Component
@Order(1)
public class SeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final PartService partService;
    private final SupplierService supplierService;
    private final BomService bomService;
    private final ChangeOrderService changeOrderService;

    @Value("${plm.seed.enabled:true}")
    private boolean enabled;

    public SeedRunner(PartService partService, SupplierService supplierService,
                       BomService bomService, ChangeOrderService changeOrderService) {
        this.partService = partService;
        this.supplierService = supplierService;
        this.bomService = bomService;
        this.changeOrderService = changeOrderService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Seed data disabled (plm.seed.enabled=false)");
            return;
        }
        if (supplierService.listSuppliers(0, 1).totalCount() > 0) {
            log.info("Seed data already present, skipping");
            return;
        }

        PlmPrincipalContext.set(new PlmPrincipal("seed-runner", Set.of(Role.ADMIN)));
        try {
            seed();
            log.info("Seed data loaded successfully");
        } finally {
            PlmPrincipalContext.clear();
        }
    }

    private void seed() {
        supplierService.createSupplier(new NewSupplier("Acme Components", "sales@acme.example", "1 Acme Way, Fremont, CA", true));
        supplierService.createSupplier(new NewSupplier("Delta Power Systems", "orders@delta-power.example", "88 Volt Ave, Austin, TX", true));
        supplierService.createSupplier(new NewSupplier("NetX Networking", "sales@netx.example", "500 Fiber Blvd, Reno, NV", true));

        String acmeId = supplierIdByName("Acme Components");
        String deltaId = supplierIdByName("Delta Power Systems");
        String netxId = supplierIdByName("NetX Networking");

        createPart("R-14", "Rack R-14", "42U data center rack, row 3", PartType.RACK, LifecycleState.ACTIVE, "B", null, Map.of("rackUnits", "42", "location", "DC1-Row3"));
        createPart("R-15", "Rack R-15", "42U data center rack, next-gen layout (in design)", PartType.RACK, LifecycleState.DESIGN, "A", null, Map.of("rackUnits", "42"));

        createPart("SRV-A1", "Server A1 1U", "1U dual-socket compute server", PartType.SERVER, LifecycleState.ACTIVE, "C", acmeId, Map.of("formFactor", "1U", "sockets", "2"));
        createPart("SRV-A2", "Server A2 1U", "1U storage-optimized server", PartType.SERVER, LifecycleState.ACTIVE, "B", acmeId, Map.of("formFactor", "1U", "driveBays", "12"));

        createPart("PSU-2200", "PSU 2200W Redundant", "2200W 80+ Platinum redundant power supply", PartType.PSU, LifecycleState.ACTIVE, "D", deltaId, Map.of("wattage", "2200", "efficiency", "80+ Platinum"));
        createPart("PSU-1100", "PSU 1100W", "1100W 80+ Gold power supply (end of life)", PartType.PSU, LifecycleState.EOL, "B", acmeId, Map.of("wattage", "1100", "efficiency", "80+ Gold"));

        createPart("NIC-10G", "10GbE Dual-Port NIC", "Dual-port 10GbE SFP+ network interface card", PartType.NIC, LifecycleState.ACTIVE, "A", netxId, Map.of("ports", "2", "speed", "10GbE"));

        createPart("CABLE-CAT6", "Cat6 Patch Cable 2m", "2 meter shielded Cat6 patch cable", PartType.CABLE, LifecycleState.ACTIVE, "A", netxId, Map.of("length", "2m"));
        createPart("CABLE-DAC", "DAC 10G Cable 3m", "3 meter passive 10G DAC cable", PartType.CABLE, LifecycleState.ACTIVE, "A", netxId, Map.of("length", "3m"));

        bomService.addBomComponent("R-14", "SRV-A1", 4);
        bomService.addBomComponent("R-14", "SRV-A2", 2);
        bomService.addBomComponent("R-15", "SRV-A1", 2);

        bomService.addBomComponent("SRV-A1", "PSU-2200", 2);
        bomService.addBomComponent("SRV-A1", "NIC-10G", 1);
        bomService.addBomComponent("SRV-A1", "CABLE-CAT6", 2);

        bomService.addBomComponent("SRV-A2", "PSU-1100", 2);
        bomService.addBomComponent("SRV-A2", "NIC-10G", 1);
        bomService.addBomComponent("SRV-A2", "CABLE-DAC", 1);

        var eco1 = changeOrderService.createChangeOrder(
                new NewChangeOrder("Replace EOL PSU-1100 with PSU-2200 in Server A2",
                        "PSU-1100 has reached end of life; requalify Server A2 with PSU-2200.",
                        List.of("PSU-1100", "PSU-2200", "SRV-A2")),
                "admin");
        changeOrderService.submitChangeOrder(eco1.getId());
        changeOrderService.approveChangeOrder(eco1.getId());

        changeOrderService.createChangeOrder(
                new NewChangeOrder("Qualify new NIC firmware", "Validate NIC-10G firmware v3.2 across the fleet.",
                        List.of("NIC-10G")),
                "engineer");

        var eco3 = changeOrderService.createChangeOrder(
                new NewChangeOrder("Retire Rack R-15 design in favor of R-16", "Superseded by next layout revision.",
                        List.of("R-15")),
                "engineer");
        changeOrderService.submitChangeOrder(eco3.getId());
    }

    private void createPart(String id, String name, String description, PartType type, LifecycleState state,
                             String revision, String supplierId, Map<String, String> attributes) {
        partService.createPartWithId(id, new NewPart(name, description, type, revision, supplierId, attributes), state);
    }

    private String supplierIdByName(String name) {
        return supplierService.listSuppliers(0, 50).items().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
