package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SpotinstCloudCommunicationState {
    SPOTINST_CLOUD_COMMUNICATION_INITIALIZING("SPOTINST CLOUD COMMUNICATION INITIALIZING"),
    SPOTINST_CLOUD_COMMUNICATION_FAILED("SPOTINST CLOUD COMMUNICATION FAILED"),
    SPOTINST_CLOUD_COMMUNICATION_READY("SPOTINST CLOUD COMMUNICATION READY");

    // region members
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsMethodEnum.class);

    private final String name;
    // endregion

    SpotinstCloudCommunicationState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SpotinstCloudCommunicationState fromName(String name){
        SpotinstCloudCommunicationState retVal = null;

        for (SpotinstCloudCommunicationState enumValues : SpotinstCloudCommunicationState.values()) {
            if (enumValues.name.equals(name)) {
                retVal = enumValues;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.warn(String.format(
                    "Tried to create CredentialsMethodEnum for name: %s, but we don't support such type ", name));
        }
        return retVal;
    }
}
