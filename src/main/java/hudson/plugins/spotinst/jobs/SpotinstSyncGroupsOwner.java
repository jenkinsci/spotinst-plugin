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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstSyncGroupsOwner extends AsyncPeriodicWork {

    //region Members
    private static final Logger  LOGGER          = LoggerFactory.getLogger(SpotinstSyncGroupsOwner.class);
    private static final Integer redisToJobRatio = 3;
    public final         Integer JOB_INTERVAL_IN_SECONDS;

    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstSyncGroupsOwner() {
        super("Sync Groups Owner");
        JOB_INTERVAL_IN_SECONDS = TimeHelper.getRedisTimeToLeaveInSeconds() / redisToJobRatio;
        recurrencePeriod = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (this) {
            List<Cloud> cloudList = Jenkins.getInstance().clouds;
            Set<GroupLockKey>       currentGroupKeys    = new HashSet<>();
            Set<GroupLockKey>       groupsFromContext   = SpotinstContext.getInstance().getCachedProcessedGroupIds();
            Set<GroupLockKey>       groupsNoLongerExist = new HashSet<>(groupsFromContext);
            List<BaseSpotinstCloud> activeClouds        = new ArrayList<>();

            if (cloudList != null && cloudList.size() > 0) {
                for (Cloud cloud : cloudList) {

                    if (cloud instanceof BaseSpotinstCloud) {
                        BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
                        String            groupId       = spotinstCloud.getGroupId();
                        String            accountId     = spotinstCloud.getAccountId();

                        GroupLockKey groupLockKey = new GroupLockKey(groupId, accountId);
                        currentGroupKeys.add(groupLockKey);
                        groupsNoLongerExist.remove(groupLockKey);

                        boolean isActiveCloud = StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId);

                        if (isActiveCloud) {
                            activeClouds.add(spotinstCloud);
                        }
                    }
                }
            }

            deallocateGroupsNoLongerInUse(groupsNoLongerExist);

            SpotinstContext.getInstance().setCachedProcessedGroupIds(currentGroupKeys);
            activeClouds.forEach(BaseSpotinstCloud::syncGroupOwner);
        }
    }
    
    public void deallocateGroupsNoLongerInUse(Set<GroupLockKey> groupsNoLongerExist) {
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
                        deallocateGroupNoLongerInUse(groupKeyNoLongerExists, groupControllerLockRepo);
                    }
                    else {
                        LOGGER.warn("Could not unlock group {} - already locked by Controller {}", groupId,
                                    lockGroupControllerValue);
                    }
                }
                else {
                    LOGGER.error("group unlocking service failed to get lock for groupId {}, accountId {}. Errors: {}",
                                 groupId, accountId, lockGroupControllerResponse.getErrors());
                }
            }
        }
    }

    private void deallocateGroupNoLongerInUse(GroupLockKey groupNoLongerExists, ILockRepo groupControllerLockRepo) {
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

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
