package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.common.GroupAcquiringDetails;
import hudson.plugins.spotinst.common.SpotinstCloudCommunicationState;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static hudson.plugins.spotinst.common.SpotinstCloudCommunicationState.*;

@Extension
public class SpotinstCloudsCommunicationMonitor extends AdministrativeMonitor {

    //region Members
    List<String> spotinstCloudsCommunicationFailures;
    List<String> spotinstCloudsCommunicationInitializing;
    //endregion

    //region Overridden Public Methods
    @Override
    public boolean isActivated() {
        return isSpotinstCloudsCommunicationFailuresExist() || isSpotinstCloudsCommunicationInitializingExist();
    }

    @Override
    public String getDisplayName() {
        return "Spotinst Clouds Communication Monitor";
    }
    //endregion

    //region getters & setters
    public boolean isSpotinstCloudsCommunicationFailuresExist() {
        return isSpotinstCloudsCommunicationStateExist(SPOTINST_CLOUD_COMMUNICATION_FAILED);
    }

    public boolean isSpotinstCloudsCommunicationInitializingExist() {
        return isSpotinstCloudsCommunicationStateExist(SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
    }

    private boolean isSpotinstCloudsCommunicationStateExist(SpotinstCloudCommunicationState state) {
        Stream<GroupAcquiringDetails> groupsDetails =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                            .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupAcquiringDetails())
                                            .filter(Objects::nonNull);

        boolean isCloudsWithReadyStateExist =
                groupsDetails.anyMatch(groupDetails -> state.equals(groupDetails.getState()));

        return isCloudsWithReadyStateExist;
    }

    public String getSpotinstCloudsCommunicationFailures() {
        String retVal;

        spotinstCloudsCommunicationFailures =
                getGroupsIdByCloudInitializationState(SPOTINST_CLOUD_COMMUNICATION_FAILED);
        retVal = String.join(", ", spotinstCloudsCommunicationFailures);

        return retVal;
    }

    public String getSpotinstCloudsCommunicationInitializing() {
        String retVal;

        spotinstCloudsCommunicationInitializing =
                getGroupsIdByCloudInitializationState(SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
        retVal = String.join(", ", spotinstCloudsCommunicationInitializing);

        return retVal;
    }

    private List<String> getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState state) {
        List<String> retVal = new ArrayList<>();

        Stream<GroupAcquiringDetails> groupsDetails =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                            .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupAcquiringDetails())
                                            .filter(Objects::nonNull);

        groupsDetails.forEach(group -> {
            if (state.equals(group.getState()) && StringUtils.isNotEmpty(group.getGroupId())) {
                retVal.add(group.getGroupId());
            }
        });

        return retVal;
    }
    //endregion
}
