package hudson.plugins.spotinst.common;

/**
 * Created by ohadmuchnik on 29/08/2016.
 */
public enum CloudProviderEnum {
    AWS("aws"),
    GCP("gcp");

    private String name;

    CloudProviderEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static CloudProviderEnum fromValue(String value) {
        if (value != null) {
            if ("AWS".equalsIgnoreCase(value)) {
                return  AWS;
            } else if ("GCP".equalsIgnoreCase(value)) {
                return GCP;
            } else {
                throw new IllegalArgumentException("Cannot create enum from " + value + " name!");
            }
        } else {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }
    }
}
