package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.common.GroupLockingManager;
import hudson.plugins.spotinst.jobs.jobSynchronizer.JobSynchronizer;
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
public class SpotinstSyncGroupsController extends AsyncPeriodicWork {
    //region constants
    private static final Integer LOCK_TTL_TO_SYNC_GROUPS_JOB_RATIO = 3;
    private static final Integer JOB_INTERVAL_IN_SECONDS           =
            GroupLockingManager.LOCK_TIME_TO_LIVE_IN_SECONDS / LOCK_TTL_TO_SYNC_GROUPS_JOB_RATIO;
    final static         long    RECURRENCE_PERIOD                 = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    //endregion

    //region Members
    private static final Logger                   LOGGER                    =
            LoggerFactory.getLogger(SpotinstSyncGroupsController.class);
    private static       Set<GroupLockingManager> groupsManagersFromLastRun = new HashSet<>();
    private static final Object                   monitor                   = new Object();
    //endregion

    //region Constructor
    public SpotinstSyncGroupsController() {
        super("Sync Groups Controller");
    }
    //endregion

    //region Public Methods
    @Initializer(after = InitMilestone.JOB_CONFIG_ADAPTED)
    public static void init() {
        initFailedGroupLockingManagers();
        new SpotinstSyncGroupsController().execute(null);

        JobSynchronizer.getInstance().release();
        LOGGER.info("Finished initializing groups' controllers, Ready to run Jobs");
    }

    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (monitor) {
            Set<GroupLockingManager> activeGroupManagers = getActiveGroupLockingManagers();
            handleRemovedGroupsSinceLastRun(activeGroupManagers);
            activeGroupManagers.forEach(GroupLockingManager::syncGroupController);
        }
    }

    public static void deallocateAll() {
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
    private static void initFailedGroupLockingManagers() {
        List<Cloud> clouds = Jenkins.getInstance().clouds;

        if (clouds != null) {
            List<GroupLockingManager> groupsNotInReadyState =
                    clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                          .map(baseCloud -> ((BaseSpotinstCloud) baseCloud).getGroupLockingManager())
                          .filter(groupLockingManager -> groupLockingManager.isCloudReadyForGroupCommunication() ==
                                                         false).collect(Collectors.toList());

            for (GroupLockingManager group : groupsNotInReadyState) {
                if (group.isActive()) {
                    group.setInitializingState();
                }
                else {
                    group.setFailedState("Found a cloud with uninitialized Group ID. please check configuration");
                }
            }

        }
    }

    private static Set<GroupLockingManager> getActiveGroupLockingManagers() {
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

    private static void handleRemovedGroupsSinceLastRun(Set<GroupLockingManager> activeGroupManagers) {
        Set<GroupLockingManager> removedGroupsManagers =
                groupsManagersFromLastRun.stream().filter(groupManagerFromLastRun -> {
                    boolean isStillActive = activeGroupManagers.stream().anyMatch(
                            activeGroupManager -> activeGroupManager.hasSameLock(groupManagerFromLastRun));
                    return isStillActive == false;
                }).collect(Collectors.toSet());
        boolean hasGroupsToUnlock = CollectionUtils.isNotEmpty(removedGroupsManagers);

        if (hasGroupsToUnlock) {
            List<String> groupIds =
                    removedGroupsManagers.stream().map(GroupLockingManager::getGroupId).collect(Collectors.toList());
            LOGGER.info("the groups {} are not in use anymore by any active cloud. unlocking them.", groupIds);
            removedGroupsManagers.forEach(GroupLockingManager::deleteGroupControllerLock);
        }

        groupsManagersFromLastRun = activeGroupManagers;
    }
    //endregion

    //region getters & setters
    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }
    //endregion
}
