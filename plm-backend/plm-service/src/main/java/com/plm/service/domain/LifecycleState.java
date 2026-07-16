package com.plm.service.domain;

/** Lifecycle state of a {@link Part}, following a strict forward progression: design -> active -> eol. */
public enum LifecycleState {
    DESIGN,
    ACTIVE,
    EOL;

    /** Whether transitioning from this state to {@code target} is a legal forward move. */
    public boolean canTransitionTo(LifecycleState target) {
        if (target == null) {
            return false;
        }
        return target.ordinal() >= this.ordinal();
    }
}
