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

    //region Getters & Setters
    /**
     * shouldRetriggerBuilds was introduced after some clouds have already been set-up, so until those are saved
     * again (only then Jenkins calls their constructor), we need to set the default behaviour for them.
     */
    public Boolean getShouldRetriggerBuilds() {
        if (shouldRetriggerBuilds == null) {
            shouldRetriggerBuilds = true;
        }

        return shouldRetriggerBuilds;
    }
    //endregion

    //region Override Public Methods
    @Override
    public void afterDisconnect(final SlaveComputer computer, final TaskListener listener) {
        // according to jenkins docs could be null in edge cases, check ComputerLauncher.afterDisconnect
        if (computer != null && computer.isOffline() && computer instanceof SpotinstComputer) {
            SpotinstComputer spotinstComputer = (SpotinstComputer) computer;
            SpotinstSlave    slave            = spotinstComputer.getNode();

            Boolean shouldRetriggerBuilds = getShouldRetriggerBuilds();

            if (shouldRetriggerBuilds && (slave == null || BooleanUtils.isFalse(slave.isSlavePending()))) {
                LOGGER.info(String.format("Start retriggering executors for %s", spotinstComputer.getDisplayName()));

                final List<Executor> executors = spotinstComputer.getExecutors();

                for (Executor executor : executors) {
                    final Queue.Executable executable = executor.getCurrentExecutable();

                    if (executable != null) {
                        executor.interrupt(Result.ABORTED);

                        final SubTask    subTask = executable.getParent();
                        final Queue.Task task    = subTask.getOwnerTask();

                        List<Action> actions = new LinkedList<>();

                        if (executable instanceof Actionable) {
                            actions = ((Actionable) executable).getActions();
                        }

                        LOGGER.info(String.format("RETRIGGERING: %s - WITH ACTIONS: %s", task, actions));

                        Queue.getInstance().schedule2(task, 10, actions);
                    }
                }

                LOGGER.info(String.format("Finished retriggering executors for %s", spotinstComputer.getDisplayName()));
            }
            else if (BooleanUtils.isFalse(shouldRetriggerBuilds)) {
                LOGGER.info(String.format("Retrigger Build disabled for %s, not retriggering executors",
                                          spotinstComputer.getDisplayName()));
            }
        }
        else if (computer != null) {
            LOGGER.info(
                    String.format("Skipping executable resubmission for %s - offline: %s", computer.getDisplayName(),
                                  computer.isOffline()));
        }
        else {
            LOGGER.info("Skipping executable resubmission, computer is null");
        }

        // call parent
        super.afterDisconnect(computer, listener);
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        super.launch(computer, listener);
    }
    //endregion
}
