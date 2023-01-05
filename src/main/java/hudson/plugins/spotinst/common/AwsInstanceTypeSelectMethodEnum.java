package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AwsInstanceTypeSelectMethodEnum {
    PICK("PICK"),
    SEARCH("SEARCH");

    private final String name;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceTypeSelectMethodEnum.class);

    AwsInstanceTypeSelectMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AwsInstanceTypeSelectMethodEnum fromName(String name) {
        AwsInstanceTypeSelectMethodEnum retVal = null;
        for (AwsInstanceTypeSelectMethodEnum selectMethodEnum : AwsInstanceTypeSelectMethodEnum.values()) {
            if (selectMethodEnum.name.equals(name)) {
                retVal = selectMethodEnum;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.error("Tried to create select method type enum for: " + name + ", but we don't support such type ");
        }

        return retVal;
    }
}
