package co.singularit.az104simulator.domain;

public enum Domain {
    IDENTITY_GOVERNANCE("Identity & Governance"),
    STORAGE("Storage"),
    COMPUTE("Compute"),
    NETWORKING("Networking"),
    MONITOR_MAINTAIN("Monitor & Maintain");

    private final String displayName;

    Domain(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
