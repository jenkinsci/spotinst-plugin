package hudson.plugins.spotinst.common;

import hudson.model.RestartListener;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.jobs.SpotinstSyncGroupsOwner;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO Liron - this part not working yet
public class SpotinstRestartListener extends RestartListener {
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

        groupsOwnerJob.deallocateGroupsNoLongerInUse(cloudSet);
    }
}
