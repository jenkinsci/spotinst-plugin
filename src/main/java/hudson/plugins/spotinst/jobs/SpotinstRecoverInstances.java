package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
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
public class SpotinstRecoverInstances extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRecoverInstances.class);
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstRecoverInstances() {
        super("Recover Instances");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(5);
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
                    try {
                        spotinstCloud.recoverInstances();
                    }
                    catch (Exception e) {
                        LOGGER.error(String.format("Failed to handle group: %s recover", spotinstCloud.getGroupId()),
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
