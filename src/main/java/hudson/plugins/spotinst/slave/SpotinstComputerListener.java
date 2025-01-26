package hudson.plugins.spotinst.slave;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.UnexpectedException;

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
    public void onOnline(Computer computer, TaskListener listener) throws IOException, InterruptedException {

        if (computer instanceof SpotinstComputer) {
            SpotinstComputer spotinstComputer = (SpotinstComputer) computer;
            SpotinstSlave    slave            = spotinstComputer.getNode();

            if (slave == null) {
                throw new UnexpectedException("Slave cannot be null");
            }

            slave.onSlaveConnected();
            LOGGER.info(String.format("Slave: %s is connected to master", slave.getNodeName()));
        }

        super.onOnline(computer, listener);
    }

    @Override
    public void onOffline(@NonNull Computer c, OfflineCause cause) {
        LOGGER.info(String.format("Computer went offline, Cause: %s.", cause));
        //  TODO shibel: investigate if we are inadvertently triggering afterDisconnect twice.
        //      See SlaveComputer#setChannel, which itself calls afterDisconnect in the callback it sets,
        //      but then SlaveComputer#disconnect does that as well.
        c.disconnect(cause);
        super.onOffline(c, cause);
    }
    //endregion
}
