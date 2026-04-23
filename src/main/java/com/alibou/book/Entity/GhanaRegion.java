package com.alibou.book.Entity;



public enum GhanaRegion {
    ASHANTI("Ashanti"),
    BRONG_AHAFO("Brong Ahafo"),
    CENTRAL("Central"),
    EASTERN("Eastern"),
    GREATER_ACCRA("Greater Accra"),
    NORTHERN("Northern"),
    UPPER_EAST("Upper East"),
    UPPER_WEST("Upper West"),
    VOLTA("Volta"),
    WESTERN("Western");
//    WESTERN_NORTH("Western North");

    private final String displayName;

    GhanaRegion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

//    @JsonValue
//    public String toValue() {
//        return this.displayName;
//    }


//    @JsonCreator
//    public static GhanaRegion fromDisplayName(String displayName) {
//        for (GhanaRegion region : values()) {
//            if (region.displayName.equalsIgnoreCase(displayName)) {
//                return region;
//            }
//        }
//        throw new IllegalArgumentException("Unknown region: " + displayName);
//    }
}