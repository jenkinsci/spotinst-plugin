package hudson.plugins.spotinst.common;

import hudson.model.RestartListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.jobs.SpotinstSyncGroupsOwner;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO Liron - this part not working yet
public class SpotinstRestartListener extends RestartListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRestartListener.class);

    //TODO Liron - check if necessary
    public static SpotinstRestartListener getInstance() {
        return Jenkins.get()
                .getExtensionList(RestartListener.class)
                .get(SpotinstRestartListener.class);
    }

    //TODO Liron - check if necessary
    public SpotinstRestartListener(){}

    @Override
    public boolean isReadyToRestart() throws IOException, InterruptedException {
        return true;
    }

    @Override
    public void onRestart() {
        SpotinstSyncGroupsOwner groupsOwnerJob = new SpotinstSyncGroupsOwner();
        List<Cloud> cloudList = Jenkins.getInstance().clouds;
        Set<BaseSpotinstCloud> cloudSet = new HashSet<>();

        if (cloudList != null && cloudList.size() > 0) {
            for (Cloud cloud : cloudList) {
                if (cloud instanceof BaseSpotinstCloud) {
                   cloudSet.add((BaseSpotinstCloud) cloud);
                }
            }
        }

        LOGGER.info(String.format("deallocating %s Spotinst clouds", cloudSet.size()));
        groupsOwnerJob.deallocateCloudsNoLongerInUse(cloudSet);//TODO: execute instead?
    }
}
