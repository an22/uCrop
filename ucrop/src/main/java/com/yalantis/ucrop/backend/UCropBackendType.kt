package com.yalantis.ucrop.backend;

public enum UCropBackendType {
    NATIVE("NATIVE"),
    DEFAULT("NON_NATIVE");

    public final String label;

    private UCropBackendType(String label) {
        this.label = label;
    }
}
