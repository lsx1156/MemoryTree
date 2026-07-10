package com.memorytree.enums;

public enum SeverityLevel {
    MINOR(0.3),
    MAJOR(0.6),
    CRITICAL(1.0);

    private final double weight;

    SeverityLevel(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
