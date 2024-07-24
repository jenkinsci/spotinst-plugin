package hudson.plugins.spotinst.slave;

import hudson.model.*;
import hudson.model.queue.SubTask;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by Shibel Karmi Mansour on 31/12/2020.
 * <p>
 * A helper class for handling common callbacks (like afterDisconnect) for different types of
 * ComputerLaunchers.
 */
class SpotLauncherHelper {
    //region Members
    private static final Logger LOGGER                               =
            LoggerFactory.getLogger(SpotLauncherHelper.class);
    private static final String PREVIOUS_EXECUTION_ACTION_CLASS_NAME =
            "org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction";
    //endregion

    //region Methods
    static void handleDisconnect(final SlaveComputer computer, Boolean shouldRetriggerBuilds) {
        LOGGER.info("Start handling disconnect event.");

        shouldRetriggerBuilds = resolveShouldRetriggerBuilds(shouldRetriggerBuilds);

        // according to jenkins docs could be null in edge cases, check ComputerLauncher.afterDisconnect
        if (computer != null && computer.isOffline() && computer instanceof SpotinstComputer) {
            SpotinstComputer spotinstComputer = (SpotinstComputer) computer;
            SpotinstSlave    slave            = spotinstComputer.getNode();


            if (shouldRetriggerBuilds && (slave == null || BooleanUtils.isFalse(slave.isSlavePending()))) {
                LOGGER.info(String.format("Start retriggering executors for %s", spotinstComputer.getDisplayName()));

                final List<Executor> executors = spotinstComputer.getExecutors();

                for (Executor executor : executors) {
                    final Queue.Executable executable = executor.getCurrentExecutable();

                    if (executable != null) {
                        executor.interrupt(Result.ABORTED);

                        retriggerJob(executable);
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
    }

    private static void retriggerJob(Queue.Executable executable) {
        final SubTask    subTask = executable.getParent();
        final Queue.Task task    = subTask.getOwnerTask();
        List<Action> actions = null;
        Queue.Executable currExecutable = executable;

        while (currExecutable != null && currExecutable instanceof Actionable == false) {
            currExecutable = currExecutable.getParentExecutable();
        }

        if (currExecutable != null) {
            LOGGER.info("Getting actions from executable.");

            actions = ((Actionable) currExecutable).getActions();

            if (actions != null) {
                actions = removePreviousRunningActions(actions);
            }
        }

        if (actions == null) {
            LOGGER.info("No actions found on executable.");
            actions = new ArrayList<>();
        }

        LOGGER.info(String.format("RETRIGGERING: %s - WITH ACTIONS: %s", task, actions));
        Queue.getInstance().schedule2(task, 10, actions);
    }

    private static List<Action> removePreviousRunningActions(List<Action> actions) {
        List<Action> retVal;
        retVal = actions.stream()
                        .filter(action -> action.getClass().getName().equals(PREVIOUS_EXECUTION_ACTION_CLASS_NAME) ==
                                          false).collect(Collectors.toList());
        return retVal;
    }

    /**
     * shouldRetriggerBuilds was introduced after some clouds have already been set-up, so until those are saved
     * again (only then Jenkins calls their constructor), we need to set the default behaviour for them.
     */
    private static Boolean resolveShouldRetriggerBuilds(Boolean shouldRetriggerBuilds) {
        if (shouldRetriggerBuilds == null) {
            shouldRetriggerBuilds = true;
        }

        return shouldRetriggerBuilds;
    }
    //endregion
}
