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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstGroupsOwnerMonitor extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstGroupsOwnerMonitor.class);
    public static final Integer JOB_INTERVAL_IN_SECONDS = 60;
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstGroupsOwnerMonitor() {
        super("Groups Monitor");
        recurrencePeriod = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        synchronized (this) {
            List<Cloud> cloudList = Jenkins.getInstance().clouds;

            if (cloudList != null && cloudList.size() > 0) {
                for (Cloud cloud : cloudList) {
                    Map<String, String> groupsNoLongerInUse = new HashMap<>(SpotinstContext.getInstance().getGroupsInUse());

                    if (cloud instanceof BaseSpotinstCloud) {
                        BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
                        String groupId = spotinstCloud.getGroupId();
                        String accountId = spotinstCloud.getAccountId();
                        groupsNoLongerInUse.remove(groupId);

                        if (groupId != null && accountId != null) {
                            spotinstCloud.syncGroupsOwner(groupId, accountId);
                        }
                    }

                    deallocateGroupsNoLongerInUse(groupsNoLongerInUse);
                }
            }
        }
    }

    private void deallocateGroupsNoLongerInUse(Map<String, String> groupsNoLongerInUse) {
        for (Map.Entry<String,String> entry : groupsNoLongerInUse.entrySet()){
            String groupId = entry.getKey();
            String accountId = entry.getValue();

            SpotinstContext.getInstance().getGroupsInUse().remove(groupId, accountId);
            IRedisRepo redisRepo = RepoManager.getInstance().getRedisRepo();
            ApiResponse<Integer> redisGetValueResponse = redisRepo.deleteKey(groupId, accountId);

            if (redisGetValueResponse.isRequestSucceed()) {
                LOGGER.info(String.format("Successfully removed group %s from redis", groupId));
            }
            else {
                LOGGER.error(String.format("Failed to remove group %s from redis", groupId));
            }
        }

    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
