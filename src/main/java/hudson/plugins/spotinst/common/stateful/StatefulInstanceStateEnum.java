package hudson.plugins.spotinst.common.stateful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sitay on 30/08/23.
 */
public enum StatefulInstanceStateEnum {
    //region Enums
    ACTIVE("ACTIVE"),
    PAUSE("PAUSE"),
    PAUSING("PAUSING"),
    PAUSED("PAUSED"),
    RESUME("RESUME"),
    RESUMING("RESUMING"),
    RECYCLE("RECYCLE"),
    RECYCLING("RECYCLING"),
    DEALLOCATE("DEALLOCATE"),
    DEALLOCATING("DEALLOCATING"),
    DEALLOCATED("DEALLOCATED"),
    ERROR("ERROR");
    //endregion

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(StatefulInstanceStateEnum.class);
    private final        String name;
    //endregion

    //region Constructors
    StatefulInstanceStateEnum(String name) {
        this.name = name;
    }
    //endregion

    //region Getters & Setters
    public String getName() {
        return name;
    }
    //endregion

    //region Methods
    public static StatefulInstanceStateEnum fromName(String name) {
        StatefulInstanceStateEnum retVal = null;

        for (StatefulInstanceStateEnum deploymentInstanceStatus : StatefulInstanceStateEnum.values()) {
            if (name.equalsIgnoreCase(deploymentInstanceStatus.name)) {
                retVal = deploymentInstanceStatus;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.error(
                    "Tried to create stateful instance state Enum for: " + name + ", but we don't support such type ");
        }

        return retVal;
    }
    //endregion
}
