package hudson.plugins.spotinst.common;

import hudson.model.RestartListener;
import hudson.plugins.spotinst.jobs.SpotinstSyncGroupsOwner;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

//TODO Liron - this part not working yet
public class SpotinstRestartListener extends RestartListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRestartListener.class);

    //TODO Liron - check if necessary
    public static SpotinstRestartListener getInstance() {
        return Jenkins.get().getExtensionList(RestartListener.class).get(SpotinstRestartListener.class);
    }

    //TODO Liron - check if necessary
    public SpotinstRestartListener() {
    }

    @Override
    public boolean isReadyToRestart() throws IOException, InterruptedException {
        return true;
    }

    @Override
    public void onRestart() {
        SpotinstSyncGroupsOwner groupsOwnerJob = new SpotinstSyncGroupsOwner();
        groupsOwnerJob.deallocateAll();
    }
}
