package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.common.GroupLockingManager;
import hudson.plugins.spotinst.common.SpotinstCloudCommunicationState;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Extension
public class SpotinstCloudsCommunicationMonitor extends AdministrativeMonitor {
    /*
    Referenced from jelly file SpotinstCloudsCommunicationMonitor/message.jelly
    that file needed for jenkins alert page (bell and errors/warnings), for example alerts of groups that failed to
    communicate with Jenkins
     */

    //region Members
    private List<GroupLockingManager> groupLockingManagers;
    private List<String>              groupCommunicationFailureDescriptions;
    private List<String>              initializingGroupIds;
    //endregion

    //region Overridden Public Methods
    @Override
    public boolean isActivated() {
        //called by jenkins first, before jelly calls the getters.
        initMonitor();
        return isGroupCommunicationFailureDescriptionsExist() || isInitializingGroupIdsExist();
    }

    @Override
    public String getDisplayName() {
        return "Spotinst Clouds Communication Monitor";
    }
    //endregion

    //region methods
    public boolean isGroupCommunicationFailureDescriptionsExist() {
        return CollectionUtils.isNotEmpty(groupCommunicationFailureDescriptions);
    }

    public boolean isInitializingGroupIdsExist() {
        return CollectionUtils.isNotEmpty(initializingGroupIds);
    }
    //endregion

    //region private methods
    private void initMonitor() {
        groupLockingManagers = getGroupLockingManagers();
        initInitializingGroupIds();
        initGroupCommunicationFailureDescriptions();
    }

    private void initGroupCommunicationFailureDescriptions() {
        if (groupLockingManagers != null) {
            groupCommunicationFailureDescriptions = groupLockingManagers.stream().filter(group ->
                                                                                                 group.getCloudCommunicationState() ==
                                                                                                 SpotinstCloudCommunicationState.FAILED)
                                                                        .map(GroupLockingManager::getErrorDescription)
                                                                        .collect(Collectors.toList());
        }
        else {
            groupCommunicationFailureDescriptions = Collections.emptyList();
        }
    }

    private void initInitializingGroupIds() {
        if (groupLockingManagers != null) {
            initializingGroupIds = groupLockingManagers.stream().filter(group -> group.getCloudCommunicationState() ==
                                                                                 SpotinstCloudCommunicationState.INITIALIZING)
                                                       .map(GroupLockingManager::getGroupId)
                                                       .collect(Collectors.toList());
        }
        else {
            initializingGroupIds = Collections.emptyList();
        }
    }

    private List<GroupLockingManager> getGroupLockingManagers() {
        List<GroupLockingManager> retVal  = null;
        Jenkins                   jenkins = Jenkins.getInstanceOrNull();

        if (jenkins != null) {
            retVal = jenkins.clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                   .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupLockingManager())
                                   .filter(Objects::nonNull).collect(Collectors.toList());
        }

        return retVal;
    }
    //endregion

    //region getters & setters
    public List<String> getGroupCommunicationFailureDescriptions() {
        return groupCommunicationFailureDescriptions;
    }

    public String getInitializingGroupIds() {
        return String.join(", ", initializingGroupIds);
    }
    //endregion
}
