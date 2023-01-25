package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.cloud.helpers.GroupLockHelper;
import hudson.plugins.spotinst.cloud.helpers.TimeHelper;
import hudson.plugins.spotinst.common.GroupLockKey;
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
    public static final  Integer JOB_INTERVAL_IN_SECONDS      =
            TimeHelper.getRedisTimeToLeaveInSeconds() / LOCK_TTL_TO_SYNC_GROUP_RATIO;
    final static         long    RECURRENCE_PERIOD            = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    //endregion

    //region Members
    private static final Logger            LOGGER            = LoggerFactory.getLogger(SpotinstSyncGroupsOwner.class);
    private static       Set<GroupLockKey> groupsFromLastRun = new HashSet<>();
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
            List<BaseSpotinstCloud> activeClouds = getActiveClouds();
            HandleCloudsChangeFromLastRun(activeClouds);

            activeClouds.forEach(BaseSpotinstCloud::syncGroupOwner);
        }
    }

    public void deallocateAll() {
        List<Cloud>       cloudList = Jenkins.getInstance().clouds;
        Set<GroupLockKey> groupLockAcquiringSet;

        if (CollectionUtils.isNotEmpty(cloudList)) {
            groupLockAcquiringSet = cloudList.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                                             .map(cloud -> (BaseSpotinstCloud) cloud)
                                             .map(spotinstCloud -> new GroupLockKey(spotinstCloud.getGroupId(),
                                                                                    spotinstCloud.getAccountId()))
                                             .collect(Collectors.toSet());
        }
        else {
            groupLockAcquiringSet = new HashSet<>();
        }

        LOGGER.info(String.format("unlocking %s Spotinst clouds", groupLockAcquiringSet.size()));
        GroupLockHelper.UnlockGroups(groupLockAcquiringSet);
        groupsFromLastRun = null;
    }
    //endregion

    //region private methods
    private List<BaseSpotinstCloud> getActiveClouds() {
        List<Cloud>             clouds = Jenkins.getInstance().clouds;
        List<BaseSpotinstCloud> retVal;

        if (clouds != null) {
            retVal = clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                           .map(cloud -> (BaseSpotinstCloud) cloud).filter(BaseSpotinstCloud::isActive)
                           .collect(Collectors.toList());
        }
        else {
            retVal = new ArrayList<>();
        }

        return retVal;
    }

    private void HandleCloudsChangeFromLastRun(List<BaseSpotinstCloud> activeClouds) {
        Set<GroupLockKey> currentActiveGroups = getGroupLockKeys(activeClouds);
        Set<GroupLockKey> groupsToUnlock =
                groupsFromLastRun.stream().filter(groupLockKey -> currentActiveGroups.contains(groupLockKey) == false)
                                 .collect(Collectors.toSet());

        LOGGER.info("the groups {} are not in use anymore by any active cloud, unlocking them.", groupsToUnlock);
        GroupLockHelper.UnlockGroups(groupsToUnlock);
        groupsFromLastRun = currentActiveGroups;
    }

    private static Set<GroupLockKey> getGroupLockKeys(List<BaseSpotinstCloud> clouds) {
        Set<GroupLockKey> retVal =
                clouds.stream().map(baseCloud -> new GroupLockKey(baseCloud.getGroupId(), baseCloud.getAccountId()))
                      .collect(Collectors.toSet());
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
