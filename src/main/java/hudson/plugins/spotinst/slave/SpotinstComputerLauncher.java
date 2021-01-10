package hudson.plugins.spotinst.slave;

import hudson.model.*;
import hudson.model.queue.SubTask;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public class SpotinstComputerLauncher extends JNLPLauncher {
    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstComputerLauncher.class);

    private Boolean shouldRetriggerBuilds;
    //endregion

    //region Constructor
    public SpotinstComputerLauncher(String tunnel, String vmargs, Boolean shouldUseWebsocket,
                                    Boolean shouldRetriggerBuilds) {
        super(tunnel, vmargs);

        this.shouldRetriggerBuilds = shouldRetriggerBuilds;

        if (shouldUseWebsocket != null) {
            setWebSocket(shouldUseWebsocket);
        }
    }
    //endregion

    //region Override Public Methods
    @Override
    public void afterDisconnect(final SlaveComputer computer, final TaskListener listener) {
        SpotLauncherHelper.handleDisconnect(computer, this.shouldRetriggerBuilds);
        // call parent
        super.afterDisconnect(computer, listener);
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        super.launch(computer, listener);
    }
    //endregion
}
