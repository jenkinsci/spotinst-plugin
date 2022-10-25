package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.common.SpotinstCloudCommunicationState;
import hudson.plugins.spotinst.common.SpotinstContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Extension
public class SpotinstCloudsCommunicationMonitor extends AdministrativeMonitor {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpotinstCloud.class);
    List<String> spotinstCloudsCommunicationFailures;
    List<String> spotinstCloudsCommunicationInitializing;
    List<String> spotinstCloudsCommunicationReady;
    //endregion

    //region Overridden Public Methods
    @Override
    public boolean isActivated() {
        return isSpotinstCloudsCommunicationFailuresExist() || isSpotinstCloudsCommunicationInitializingExist()
                || isSpotinstCloudsCommunicationReadyExist();
    }

    @Override
    public String getDisplayName() {
        return "Spotinst Clouds Communication Monitor";
    }
    //endregion

    //region getters & setters
    public boolean isSpotinstCloudsCommunicationFailuresExist() {
        boolean isCloudsWithFailureStateExist = SpotinstContext.getInstance().getCloudsInitializationState().containsValue(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_FAILED);
        boolean isCloudsWithGroupIdExist = CollectionUtils.isNotEmpty(getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_FAILED));
        return isCloudsWithFailureStateExist && isCloudsWithGroupIdExist;
    }

    public boolean isSpotinstCloudsCommunicationInitializingExist() {
        return SpotinstContext.getInstance().getCloudsInitializationState().containsValue(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
    }

    public boolean isSpotinstCloudsCommunicationReadyExist() {
        return SpotinstContext.getInstance().getCloudsInitializationState().containsValue(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_READY);
    }

    public String getSpotinstCloudsCommunicationFailures() {
        String retVal;

        spotinstCloudsCommunicationFailures = getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_FAILED);
        retVal = spotinstCloudsCommunicationFailures.stream().collect(Collectors.joining(", "));

        return retVal;
    }

    public String getSpotinstCloudsCommunicationInitializing() {
        String retVal;

        spotinstCloudsCommunicationInitializing = getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
        retVal = spotinstCloudsCommunicationInitializing.stream().collect(Collectors.joining(", "));

        return retVal;
    }

    public String getSpotinstCloudsCommunicationReady() {
        String retVal;

        spotinstCloudsCommunicationReady = getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_READY);
        retVal = spotinstCloudsCommunicationReady.stream().collect(Collectors.joining(", "));

        return retVal;
    }

    private List<String> getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState state) {
        List<String> retVal = new ArrayList<>();

        for (Map.Entry<BaseSpotinstCloud, SpotinstCloudCommunicationState> cloudsInitializationStateEntry : SpotinstContext.getInstance().getCloudsInitializationState().entrySet()) {
            if (cloudsInitializationStateEntry.getValue().equals(state)) {
                BaseSpotinstCloud cloud = cloudsInitializationStateEntry.getKey();

                if (StringUtils.isNotEmpty(cloud.getGroupId())) {
                    retVal.add(cloud.getGroupId());
                }
            }
        }

        return retVal;
    }
    //endregion
}
