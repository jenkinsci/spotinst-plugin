package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.cloud.helpers.TimeHelper;
import hudson.plugins.spotinst.common.GroupStateTracker;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.repos.IRedisRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    //    @Override
    //    protected void execute(TaskListener taskListener) {
    //        synchronized (this) {
    //            List<Cloud>            cloudList           = Jenkins.getInstance().clouds;
    //            Set<BaseSpotinstCloud> cloudsFromContext   =
    //                    SpotinstContext.getInstance().getCloudsInitializationState().keySet();
    //            Set<BaseSpotinstCloud> cloudsNoLongerExist = new HashSet<>(cloudsFromContext);
    //
    //            if (cloudList != null && cloudList.size() > 0) {
    //                for (Cloud cloud : cloudList) {
    //
    //                    if (cloud instanceof BaseSpotinstCloud) {
    //                        BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
    //                        String            groupId       = spotinstCloud.getGroupId();
    //                        String            accountId     = spotinstCloud.getAccountId();
    //                        cloudsNoLongerExist.remove(spotinstCloud);
    //
    //                        if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
    //                            spotinstCloud.syncGroupsOwner(spotinstCloud);
    //                        }
    //                    }
    //                }
    //            }
    //
    //            deallocateGroupsNoLongerInUse(cloudsNoLongerExist);
    //        }
    //    }

    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (this) {
            List<Cloud> cloudList           = Jenkins.getInstance().clouds;
            Set<String> groupsFromContext   = SpotinstContext.getInstance().getConnectionStateByGroupId().keySet();
            Set<String> groupsNoLongerExist = new HashSet<>(groupsFromContext);

            if (cloudList != null && cloudList.size() > 0) {
                for (Cloud cloud : cloudList) {

                    if (cloud instanceof BaseSpotinstCloud) {
                        BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
                        String            groupId       = spotinstCloud.getGroupId();
                        String            accountId     = spotinstCloud.getAccountId();
                        groupsNoLongerExist.remove(groupId);

                        if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
                            spotinstCloud.syncGroupsOwner(spotinstCloud);
                        }
                    }
                }
            }

            deallocateGroupsNoLongerInUse(groupsNoLongerExist);
        }
    }

    //    public void deallocateGroupsNoLongerInUse(Set<BaseSpotinstCloud> cloudsNoLongerExist) {
    //        for (BaseSpotinstCloud cloud : cloudsNoLongerExist) {
    //            String groupId   = cloud.getGroupId();
    //            String accountId = cloud.getAccountId();
    //            SpotinstContext.getInstance().getCloudsInitializationState().remove(cloud);
    //
    //            if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
    //                IRedisRepo           redisRepo             = RepoManager.getInstance().getRedisRepo();
    //                ApiResponse<Integer> redisGetValueResponse = redisRepo.deleteKey(groupId, accountId);
    //
    //                if (redisGetValueResponse.isRequestSucceed()) {
    //                    LOGGER.info(String.format("Successfully removed group %s from redis", groupId));
    //                }
    //                else {
    //                    LOGGER.error(String.format("Failed to remove group %s from redis", groupId));
    //                }
    //            }
    //        }
    //    }

    public void deallocateCloudsNoLongerInUse(Set<BaseSpotinstCloud> cloudsNoLongerExist) {
        Set<String> groupsNoLongerExist =
                cloudsNoLongerExist.stream().map(BaseSpotinstCloud::getGroupId).collect(Collectors.toSet());
        deallocateGroupsNoLongerInUse(groupsNoLongerExist);
    }

    private void deallocateGroupsNoLongerInUse(Set<String> groupsNoLongerExist) {
        for (String groupId : groupsNoLongerExist) {
            GroupStateTracker groupDetails = SpotinstContext.getInstance().getConnectionStateByGroupId().get(groupId);
            String            accountId    = groupDetails.getAccountId();
            SpotinstContext.getInstance().getConnectionStateByGroupId().remove(groupId);

            if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
                IRedisRepo           redisRepo             = RepoManager.getInstance().getRedisRepo();
                ApiResponse<Integer> redisGetValueResponse = redisRepo.deleteKey(groupId, accountId);

                if (redisGetValueResponse.isRequestSucceed()) {
                    LOGGER.info(String.format("Successfully removed group %s from redis", groupId));
                }
                else {
                    LOGGER.error(String.format("Failed to remove group %s from redis", groupId));
                }
            }
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
