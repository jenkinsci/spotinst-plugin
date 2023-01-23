package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.api.infra.ApiError;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.cloud.helpers.TimeHelper;
import hudson.plugins.spotinst.common.GroupLockKey;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

    //region Members
    private static final Logger            LOGGER                  =
            LoggerFactory.getLogger(SpotinstSyncGroupsOwner.class);
    private static       Set<GroupLockKey> groupsFromLastRun;
    private static final Integer           lockTTLToSyncGroupRatio = 3;
    public static final  Integer           JOB_INTERVAL_IN_SECONDS =
            TimeHelper.getRedisTimeToLeaveInSeconds() / lockTTLToSyncGroupRatio;
    final static         long              recurrencePeriod        = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
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

        LOGGER.info(String.format("deallocating %s Spotinst clouds", groupLockAcquiringSet.size()));
        deallocateGroups(groupLockAcquiringSet);
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
        Set<GroupLockKey> previousActiveGroups = getGroupsFromLastRun();
        Set<GroupLockKey> currentActiveGroups  = getGroups(activeClouds);

        previousActiveGroups.removeAll(currentActiveGroups);

        deallocateGroups(previousActiveGroups);
        groupsFromLastRun = currentActiveGroups;
    }

    private static Set<GroupLockKey> getGroups(List<BaseSpotinstCloud> clouds) {
        Set<GroupLockKey> retVal =
                clouds.stream().map(baseCloud -> new GroupLockKey(baseCloud.getGroupId(), baseCloud.getAccountId()))
                      .collect(Collectors.toSet());
        return retVal;
    }

    private void deallocateGroups(Set<GroupLockKey> groupsNoLongerExist) {
        for (GroupLockKey groupKeyNoLongerExists : groupsNoLongerExist) {
            String  groupId       = groupKeyNoLongerExists.getGroupId(), accountId =
                    groupKeyNoLongerExists.getAccountId();
            boolean isActiveCloud = StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId);

            if (isActiveCloud) {
                ILockRepo groupControllerLockRepo = RepoManager.getInstance().getLockRepo();
                ApiResponse<String> lockGroupControllerResponse =
                        groupControllerLockRepo.getGroupControllerLock(groupId, accountId);

                if (lockGroupControllerResponse.isRequestSucceed()) {
                    String  lockGroupControllerValue  = lockGroupControllerResponse.getValue();
                    String  controllerIdentifier      = SpotinstContext.getInstance().getControllerIdentifier();
                    boolean isGroupBelongToController = controllerIdentifier.equals(lockGroupControllerValue);

                    if (isGroupBelongToController) {
                        deallocateGroup(groupKeyNoLongerExists, groupControllerLockRepo);
                    }
                    else {
                        LOGGER.warn("Controller {} could not unlock group {} - already locked by Controller {}",
                                    controllerIdentifier, groupId, lockGroupControllerValue);
                    }
                }
                else {
                    LOGGER.error("group unlocking service failed to get lock for groupId {}, accountId {}. Errors: {}",
                                 groupId, accountId, lockGroupControllerResponse.getErrors());
                }
            }
        }
    }

    private void deallocateGroup(GroupLockKey groupNoLongerExists, ILockRepo groupControllerLockRepo) {
        String groupId = groupNoLongerExists.getGroupId(), accountId = groupNoLongerExists.getAccountId();
        ApiResponse<Integer> groupControllerValueResponse =
                groupControllerLockRepo.unlockGroupController(groupId, accountId);

        if (groupControllerValueResponse.isRequestSucceed()) {
            LOGGER.info("Successfully unlocked group {}", groupId);
        }
        else {
            List<ApiError> errors =
                    groupControllerValueResponse.getErrors() != null ? groupControllerValueResponse.getErrors() :
                    new LinkedList<>();
            LOGGER.error("Failed to unlock group {}. Errors: {}", groupId, errors);
        }
    }
    //endregion

    //region getters & setters
    public static Set<GroupLockKey> getGroupsFromLastRun() {
        if (groupsFromLastRun == null) {
            groupsFromLastRun = new HashSet<>();
        }

        return groupsFromLastRun;
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
