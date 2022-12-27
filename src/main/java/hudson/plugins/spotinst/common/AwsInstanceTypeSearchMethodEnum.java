package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AwsInstanceTypeSearchMethodEnum {
    SELECT("SELECT"),
    SEARCH("SEARCH");

    private final String name;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceTypeSearchMethodEnum.class);

    AwsInstanceTypeSearchMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AwsInstanceTypeSearchMethodEnum fromValue(String value) {
        AwsInstanceTypeSearchMethodEnum retVal = null;
        for (AwsInstanceTypeSearchMethodEnum searchMethodEnum : AwsInstanceTypeSearchMethodEnum.values()) {
            if (searchMethodEnum.name.equals(value)) {
                retVal = searchMethodEnum;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.error("Tried to create search method type enum for: " + value + ", but we don't support such type ");
        }

        return retVal;
    }
}
