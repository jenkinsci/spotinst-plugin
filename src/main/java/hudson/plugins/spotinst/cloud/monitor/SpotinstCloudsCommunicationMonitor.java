package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.common.GroupStateTracker;
import hudson.plugins.spotinst.common.SpotinstCloudCommunicationState;
import hudson.plugins.spotinst.common.SpotinstContext;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
//    public boolean isSpotinstCloudsCommunicationFailuresExist() {
//        boolean isCloudsWithFailureStateExist = SpotinstContext.getInstance().getCloudsInitializationState()
//                                                               .containsValue(
//                                                                       SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_FAILED);
//        boolean isCloudsWithGroupIdExist = CollectionUtils.isNotEmpty(getGroupsIdByCloudInitializationState(
//                SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_FAILED));
//        return isCloudsWithFailureStateExist && isCloudsWithGroupIdExist;
//    }

    public boolean isSpotinstCloudsCommunicationFailuresExist() {
        return isSpotinstCloudsCommunicationStateExist(SPOTINST_CLOUD_COMMUNICATION_FAILED);
    }

//    public boolean isSpotinstCloudsCommunicationInitializingExist() {
//        return SpotinstContext.getInstance().getCloudsInitializationState()
//                              .containsValue(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
//    }

    public boolean isSpotinstCloudsCommunicationInitializingExist() {
        return isSpotinstCloudsCommunicationStateExist(SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
    }

//    public boolean isSpotinstCloudsCommunicationReadyExist() {
//        return SpotinstContext.getInstance().getCloudsInitializationState()
//                              .containsValue(SpotinstCloudCommunicationState.SPOTINST_CLOUD_COMMUNICATION_READY);
//    }

    private boolean isSpotinstCloudsCommunicationStateExist(SpotinstCloudCommunicationState state){
        Collection<GroupStateTracker> allCurrentGroups =
                SpotinstContext.getInstance().getConnectionStateByGroupId().values();

        boolean isCloudsWithReadyStateExist = allCurrentGroups.stream().anyMatch(
                groupDetails -> state.equals(
                        groupDetails.getState()));

        return isCloudsWithReadyStateExist;
    }

    public String getSpotinstCloudsCommunicationFailures() {
        String retVal;

        spotinstCloudsCommunicationFailures = getGroupsIdByCloudInitializationState(
                SPOTINST_CLOUD_COMMUNICATION_FAILED);
        retVal = String.join(", ", spotinstCloudsCommunicationFailures);

        return retVal;
    }

    public String getSpotinstCloudsCommunicationInitializing() {
        String retVal;

        spotinstCloudsCommunicationInitializing = getGroupsIdByCloudInitializationState(
                SPOTINST_CLOUD_COMMUNICATION_INITIALIZING);
        retVal = String.join(", ", spotinstCloudsCommunicationInitializing);

        return retVal;
    }

//    private List<String> getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState state) {
//        List<String> retVal = new ArrayList<>();
//
//        for (Map.Entry<BaseSpotinstCloud, SpotinstCloudCommunicationState> cloudsInitializationStateEntry : SpotinstContext.getInstance()
//                                                                                                                           .getCloudsInitializationState()
//                                                                                                                           .entrySet()) {
//            if (cloudsInitializationStateEntry.getValue().equals(state)) {
//                BaseSpotinstCloud cloud = cloudsInitializationStateEntry.getKey();
//
//                if (StringUtils.isNotEmpty(cloud.getGroupId())) {
//                    retVal.add(cloud.getGroupId());
//                }
//            }
//        }
//
//        return retVal;
//    }

    private List<String> getGroupsIdByCloudInitializationState(SpotinstCloudCommunicationState state) {
        List<String> retVal = new ArrayList<>();

        Collection<GroupStateTracker> allCurrentGroups =
                SpotinstContext.getInstance().getConnectionStateByGroupId().values();
        allCurrentGroups.forEach(group -> {
            if(state.equals(group.getState()) && StringUtils.isNotEmpty(group.getGroupId())){
                retVal.add(group.getGroupId());
            }
        });

        return retVal;
    }
    //endregion
}
