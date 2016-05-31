package hudson.plugins.spotinst;

import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public class SpotinstComputerLauncher extends ComputerLauncher {

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
    }
}
