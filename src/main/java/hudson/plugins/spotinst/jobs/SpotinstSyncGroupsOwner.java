package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.common.GroupLockingManager;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by itayshklar on 25/01/2023.
 */
@Extension
public class SpotinstSyncGroupsOwner extends AsyncPeriodicWork {
    //region constants
    private static final Integer LOCK_TTL_TO_SYNC_GROUP_RATIO = 3;
    private static final  Integer JOB_INTERVAL_IN_SECONDS      =
            GroupLockingManager.LOCK_TIME_TO_LIVE_IN_SECONDS / LOCK_TTL_TO_SYNC_GROUP_RATIO;
    final static         long    RECURRENCE_PERIOD            = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    //endregion

    //region Members
    private static final Logger                   LOGGER                    =
            LoggerFactory.getLogger(SpotinstSyncGroupsOwner.class);
    private static       Set<GroupLockingManager> groupsManagersFromLastRun = new HashSet<>();
    //endregion

    //region Constructor
    public SpotinstSyncGroupsOwner() {
        super("Sync Groups Owner");
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (this) {
            Set<GroupLockingManager> activeGroupManagers = getActiveGroupLockingManagers();
            handleRemovedGroupsSinceLastRun(activeGroupManagers);
            activeGroupManagers.forEach(GroupLockingManager::syncGroupController);
        }
    }

    private static void handleRemovedGroupsSinceLastRun(Set<GroupLockingManager> activeGroupManagers) {
        Set<GroupLockingManager> removedGroupsManagers = groupsManagersFromLastRun.stream()
                                                                                  .filter(groupManagerFromLastRun -> activeGroupManagers.stream()
                                                                                                                                        .anyMatch(
                                                                                                                                                activeGroupManager -> activeGroupManager.hasSameLock(
                                                                                                                                                        groupManagerFromLastRun)))
                                                                                  .collect(Collectors.toSet());
        boolean hasGroupsToUnlock = removedGroupsManagers.isEmpty() == false;

        if (hasGroupsToUnlock) {
            List<String> groupIds = removedGroupsManagers.stream().map(GroupLockingManager::getGroupId)
                                                         .collect(Collectors.toList());
            LOGGER.info("the groups {} are not in use anymore by any active cloud. unlocking them.", groupIds);
            removedGroupsManagers.forEach(GroupLockingManager::deleteGroupControllerLock);
        }

        groupsManagersFromLastRun = activeGroupManagers;
    }

    public void deallocateAll() {
        List<Cloud> cloudList = Jenkins.getInstance().clouds;

        if (CollectionUtils.isNotEmpty(cloudList)) {
            Set<GroupLockingManager> groupManagers =
                    cloudList.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                             .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupLockingManager())
                             .collect(Collectors.toSet());

            if (groupManagers.isEmpty() == false) {
                LOGGER.info(String.format("unlocking %s Spotinst clouds", groupManagers.size()));
                groupManagers.forEach(GroupLockingManager::deleteGroupControllerLock);
            }
        }

        groupsManagersFromLastRun = new HashSet<>();
    }
    //endregion

    //region private methods
    private Set<GroupLockingManager> getActiveGroupLockingManagers() {
        List<Cloud>              clouds = Jenkins.getInstance().clouds;
        Set<GroupLockingManager> retVal;

        if (clouds != null) {
            retVal = clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                           .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupLockingManager())
                           .filter(GroupLockingManager::isActive).collect(Collectors.toSet());
        }
        else {
            retVal = new HashSet<>();
        }

        return retVal;
    }
    //endregion

    //region getters & setters
    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }
    //endregion
}
