package com.plm.service.api;

import com.plm.service.domain.NewSupplier;
import com.plm.service.domain.PageResult;
import com.plm.service.domain.Part;
import com.plm.service.domain.Supplier;

import java.util.List;

/**
 * CRUD on suppliers and supplier -&gt; part relationship queries. This is the single source of
 * truth for supplier business logic — both the GraphQL resolvers and the chatbot's tools call
 * these same methods.
 */
public interface SupplierService {

    Supplier getSupplier(String id);

    PageResult<Supplier> listSuppliers(int page, int size);

    Supplier createSupplier(NewSupplier newSupplier);

    Supplier updateSupplier(String id, NewSupplier update);

    void deleteSupplier(String id);

    /** All parts supplied by this supplier (e.g. "which parts from Acme are EOL?" filters this client-side). */
    List<Part> supplierParts(String supplierId);
}
