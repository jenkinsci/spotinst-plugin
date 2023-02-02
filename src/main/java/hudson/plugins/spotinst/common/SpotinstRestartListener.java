package hudson.plugins.spotinst.common;

import hudson.Extension;
import hudson.model.RestartListener;
import hudson.plugins.spotinst.jobs.SpotinstSyncGroupsController;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO Liron - this part not working yet
@Extension
public class SpotinstRestartListener extends RestartListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRestartListener.class);

    public static SpotinstRestartListener getInstance() {
        return Jenkins.get().getExtensionList(RestartListener.class).get(SpotinstRestartListener.class);
    }

    public SpotinstRestartListener() {
    }

    @Override
    public boolean isReadyToRestart() {
        return true;
    }

    @Override
    public void onRestart() {//TODO: check
        SpotinstSyncGroupsController groupsOwnerJob = new SpotinstSyncGroupsController();
        groupsOwnerJob.deallocateAll();
        super.onRestart();
    }
}
