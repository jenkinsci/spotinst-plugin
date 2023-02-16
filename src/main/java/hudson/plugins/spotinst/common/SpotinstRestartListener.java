package hudson.plugins.spotinst.common;

import hudson.Extension;
import hudson.model.RestartListener;
import hudson.plugins.spotinst.jobs.SpotinstSyncGroupsController;
import jenkins.model.Jenkins;

@Extension
public class SpotinstRestartListener extends RestartListener {
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
    public void onRestart() {
        SpotinstSyncGroupsController.deallocateAll();
        super.onRestart();
    }
}
