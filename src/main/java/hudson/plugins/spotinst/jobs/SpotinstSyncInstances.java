package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AsyncPeriodicWork;
import hudson.model.PeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstSyncInstances extends AsyncPeriodicWork {

    //region Members
    private static final Logger  LOGGER                  = LoggerFactory.getLogger(SpotinstSyncInstances.class);
    public static final  Integer JOB_INTERVAL_IN_MINUTES = 1;
    final long recurrencePeriod;
    boolean isJobExecuted;
    //endregion

    //region Constructor
    public SpotinstSyncInstances() {
        super("Sync Instances");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(JOB_INTERVAL_IN_MINUTES);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        List<Cloud> cloudList = Jenkins.getInstance().clouds;

        if (cloudList != null && cloudList.size() > 0) {
            for (Cloud cloud : cloudList) {
                if (cloud instanceof BaseSpotinstCloud) {
                    BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;

                    if(this.isJobExecuted == false) {
                        ExtensionList<PeriodicWork> spotinstGroupsOwnerMonitorPeriodicWork = SpotinstSyncGroupsController.all();

                        if (CollectionUtils.isNotEmpty(spotinstGroupsOwnerMonitorPeriodicWork)) {
                            spotinstGroupsOwnerMonitorPeriodicWork.get(0).run();
                            this.isJobExecuted = true;
                        }
                    }

                    try {
                        spotinstCloud.syncGroupInstances();
                    }
                    catch (Exception e) {
                        LOGGER.error(String.format("Failed to sync group: %s instances", spotinstCloud.getGroupId()),
                                     e);
                    }
                }
            }
        }
        else {
            LOGGER.info("There are no groups to handle");
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
