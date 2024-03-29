package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.jobs.jobSynchronizer.JobSynchronizer;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstSyncGroups extends AsyncPeriodicWork {

    //region Members
    private static final Logger  LOGGER                  = LoggerFactory.getLogger(SpotinstSyncGroups.class);
    public static final  Integer JOB_INTERVAL_IN_MINUTES = 1;
    final                long    recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstSyncGroups() {
        super("Sync Instances");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(JOB_INTERVAL_IN_MINUTES);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        JobSynchronizer.getInstance().await();
        List<Cloud> cloudList = Jenkins.getInstance().clouds;

        if (cloudList != null && cloudList.size() > 0) {
            for (Cloud cloud : cloudList) {
                if (cloud instanceof BaseSpotinstCloud) {
                    BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;

                    try {
                        spotinstCloud.syncGroup();
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
