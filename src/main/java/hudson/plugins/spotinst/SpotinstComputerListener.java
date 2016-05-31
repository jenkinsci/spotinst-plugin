package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstComputerListener extends ComputerListener {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstComputerListener.class);
    //endregion

    //region Public Methods
    @Override
    public void onOnline(Computer computer, TaskListener listener) {

        if (computer instanceof SpotinstComputer) {
            SpotinstComputer spotinstComputer = (SpotinstComputer) computer;

            String nodeName = spotinstComputer.getNode().getNodeName();
            String label = spotinstComputer.getNode().getLabelString();

            Map<String, Integer> spotRequestInitiating = SpotinstContext.getInstance().getSpotRequestInitiating().get(label);

            if (spotRequestInitiating != null) {
                if (spotRequestInitiating.containsKey(nodeName)) {
                    SpotinstContext.getInstance().removeSpotRequestFromInitiating(label, nodeName);
                }
            }
        }
    }

    @Override
    public void onOffline(@Nonnull Computer computer, @CheckForNull OfflineCause cause) {

        if (computer instanceof SpotinstComputer) {
            LOGGER.info("Computer " + computer.getName() + " is offline");

            SpotinstComputer spotinstComputer = (SpotinstComputer) computer;
            SpotinstSlave spotinstSlave = spotinstComputer.getNode();

            if (spotinstSlave.getInstanceId() != null) {
                SpotinstContext.getInstance().addToOfflineComputers(spotinstSlave.getElastigroupId(), spotinstSlave.getInstanceId());
            }
        }
    }
    //endregion
}
