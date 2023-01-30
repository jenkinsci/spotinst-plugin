package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.common.GroupAcquiringDetails;
import hudson.plugins.spotinst.common.SpotinstCloudCommunicationState;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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

    //region methods
    public boolean isSpotinstCloudsCommunicationFailuresExist() {
        return isSpotinstCloudsCommunicationStateExist(FAILED);
    }

    public boolean isSpotinstCloudsCommunicationInitializingExist() {
        return isSpotinstCloudsCommunicationStateExist(INITIALIZING);
    }

    private boolean isSpotinstCloudsCommunicationStateExist(SpotinstCloudCommunicationState state) {
        Stream<GroupAcquiringDetails> groupsDetails =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                            .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupAcquiringDetails())
                                            .filter(Objects::nonNull);

        boolean isCloudsWithReadyStateExist = groupsDetails.anyMatch(groupDetails -> state == groupDetails.getState());

        return isCloudsWithReadyStateExist;
    }
    //endregion

    //region getters & setters
    public String getSpotinstCloudsCommunicationFailures() {
        String retVal;
        Stream<GroupAcquiringDetails> groupsDetails =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                            .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupAcquiringDetails())
                                            .filter(Objects::nonNull);
        spotinstCloudsCommunicationFailures =
                groupsDetails.filter(group -> group.getState() == FAILED)
                             .map(GroupAcquiringDetails::getDescription).collect(Collectors.toList());

        retVal = String.join("; ", spotinstCloudsCommunicationFailures);

        return retVal;
    }

    public String getSpotinstCloudsCommunicationInitializing() {
        String retVal;

        spotinstCloudsCommunicationInitializing = new ArrayList<>();

        Stream<GroupAcquiringDetails> groupsDetails =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                            .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupAcquiringDetails())
                                            .filter(Objects::nonNull);

        groupsDetails.forEach(group -> {
            if (group.getState() == INITIALIZING) {
                spotinstCloudsCommunicationInitializing.add(group.getGroupId());
            }
        });

        retVal = String.join(", ", spotinstCloudsCommunicationInitializing);

        return retVal;
    }
    //endregion
}
