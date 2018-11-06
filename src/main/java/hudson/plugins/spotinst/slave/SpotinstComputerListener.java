package hudson.plugins.spotinst.slave;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            SpotinstSlave slave = spotinstComputer.getNode();
            slave.onSlaveConnected();

            LOGGER.info(String.format("Slave: %s is connected to master", slave.getNodeName()));
        }
    }

    //TODO - handle onOffline computer
    //endregion
}
