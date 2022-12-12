package hudson.plugins.spotinst.common;

public enum AwsInstanceTypeSearchMethodEnum {
    SELECT("SELECT"),
    SEARCH("SEARCH");

    private final String name;

    AwsInstanceTypeSearchMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
