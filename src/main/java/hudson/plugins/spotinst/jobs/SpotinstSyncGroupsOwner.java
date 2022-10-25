package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
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

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstSyncGroupsOwner extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstSyncGroupsOwner.class);
    public static final Integer JOB_INTERVAL_IN_SECONDS = 60;
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstSyncGroupsOwner() {
        super("Sync Groups Owner");
        recurrencePeriod = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (this) {
            List<Cloud> cloudList = Jenkins.getInstance().clouds;
            Set<BaseSpotinstCloud> cloudsFromContext = SpotinstContext.getInstance().getCloudsInitializationState().keySet();
            Set<BaseSpotinstCloud> cloudsNoLongerExist = new HashSet<>(cloudsFromContext);

            if (cloudList != null && cloudList.size() > 0) {
                for (Cloud cloud : cloudList) {

                    if (cloud instanceof BaseSpotinstCloud) {
                        BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
                        String groupId = spotinstCloud.getGroupId();
                        String accountId = spotinstCloud.getAccountId();
                        cloudsNoLongerExist.remove(spotinstCloud);

                        if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
                            spotinstCloud.syncGroupsOwner(spotinstCloud);
                        }
                    }
                }
            }
            
            deallocateGroupsNoLongerInUse(cloudsNoLongerExist);
        }
    }

    public void deallocateGroupsNoLongerInUse(Set<BaseSpotinstCloud> cloudsNoLongerExist) {
        for (BaseSpotinstCloud cloud : cloudsNoLongerExist){
            String groupId = cloud.getGroupId();
            String accountId = cloud.getAccountId();
            SpotinstContext.getInstance().getCloudsInitializationState().remove(cloud);

            if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId)) {
                IRedisRepo redisRepo = RepoManager.getInstance().getRedisRepo();
                ApiResponse<Integer> redisGetValueResponse = redisRepo.deleteKey(groupId, accountId);

                if (redisGetValueResponse.isRequestSucceed()) {
                    LOGGER.info(String.format("Successfully removed group %s from redis", groupId));
                } else {
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
