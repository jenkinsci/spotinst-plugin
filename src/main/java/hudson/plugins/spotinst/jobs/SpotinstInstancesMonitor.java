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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstInstancesMonitor extends AsyncPeriodicWork {

    //region Members
    public static final Integer JOB_INTERVAL_IN_SECONDS = 30;
    final               long    recurrencePeriod;
    private             boolean isJobExecuted;
    //endregion

    //region Constructor
    public SpotinstInstancesMonitor() {
        super("Instances monitor");
        recurrencePeriod = TimeUnit.SECONDS.toMillis(JOB_INTERVAL_IN_SECONDS);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        List<Cloud> cloudList = Jenkins.getInstance().clouds;

        if (cloudList != null && cloudList.size() > 0) {
            for (Cloud cloud : cloudList) {
                if (cloud instanceof BaseSpotinstCloud) {
                    if (this.isJobExecuted == false) {
                        ExtensionList<PeriodicWork> spotinstGroupsOwnerMonitorPeriodicWork =
                                SpotinstSyncGroupsOwner.all();

                        if (CollectionUtils.isNotEmpty(spotinstGroupsOwnerMonitorPeriodicWork)) {
                            spotinstGroupsOwnerMonitorPeriodicWork.get(0).run();
                            this.isJobExecuted = true;
                        }
                    }

                    BaseSpotinstCloud spotinstCloud = (BaseSpotinstCloud) cloud;
                    spotinstCloud.monitorInstances();
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
