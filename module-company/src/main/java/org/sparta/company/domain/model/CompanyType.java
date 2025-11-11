package org.sparta.company.domain.model;

public enum CompanyType {
    SUPPLIER("공급사"),
    RECEIVER("수취사");

    private final String description;

    CompanyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
