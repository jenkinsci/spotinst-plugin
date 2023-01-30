package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SpotinstCloudCommunicationState {
    INITIALIZING("INITIALIZING"),
    FAILED("FAILED"),
    READY("READY");

    // region members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstCloudCommunicationState.class);

    private final String name;
    // endregion

    SpotinstCloudCommunicationState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SpotinstCloudCommunicationState fromName(String name) {
        SpotinstCloudCommunicationState retVal = null;

        for (SpotinstCloudCommunicationState enumValues : SpotinstCloudCommunicationState.values()) {
            if (enumValues.name.equals(name)) {
                retVal = enumValues;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.warn(String.format(
                    "Tried to create SpotinstCloudCommunicationState for name: %s, but we don't support such type ",
                    name));
        }
        return retVal;
    }
}
