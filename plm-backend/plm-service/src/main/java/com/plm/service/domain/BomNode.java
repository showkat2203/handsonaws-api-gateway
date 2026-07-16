package com.plm.service.domain;

import java.util.List;

/** A node in a Bill-of-Materials tree: a part, the quantity used by its parent, and its own children. */
public record BomNode(Part part, int quantity, List<BomNode> children) {

    public BomNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
