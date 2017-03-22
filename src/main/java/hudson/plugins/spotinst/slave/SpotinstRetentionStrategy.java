package hudson.plugins.spotinst.slave;

import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
import hudson.util.TimeUnit2;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public class SpotinstRetentionStrategy extends RetentionStrategy<SpotinstComputer> {

    //region Members
    private static final Logger  LOGGER   = LoggerFactory.getLogger(SpotinstRetentionStrategy.class);
    public static final  boolean DISABLED = Boolean.getBoolean(SpotinstRetentionStrategy.class.getName() + ".disabled");
    public final      int           idleTerminationMinutes;
    private transient ReentrantLock checkLock;
    private static final int STARTUP_TIME_DEFAULT_VALUE = 30;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public SpotinstRetentionStrategy(String idleTerminationMinutes) {
        readResolve();
        if (idleTerminationMinutes == null || idleTerminationMinutes.trim().isEmpty()) {
            this.idleTerminationMinutes = 0;
        }
        else {
            int value = STARTUP_TIME_DEFAULT_VALUE;
            try {
                value = Integer.parseInt(idleTerminationMinutes);
            }
            catch (NumberFormatException nfe) {
                LOGGER.info("Malformed default idleTermination value: " + idleTerminationMinutes);
            }

            this.idleTerminationMinutes = value;
        }
    }
    //endregion

    //region Private Methods
    private long CheckComputer(SpotinstComputer computer) {

        if (idleTerminationMinutes == 0 || computer.getNode() == null) {
            return 1;
        }

        SpotinstSlave slave = computer.getNode();
        if (slave.isSlavePending()) {
            return 1;
        }

        if (computer.isIdle() && !DISABLED) {

            final long idleMilliseconds = System.currentTimeMillis() - computer.getIdleStartMilliseconds();

            if (idleTerminationMinutes > 0) {
                if (idleMilliseconds > TimeUnit2.MINUTES.toMillis(idleTerminationMinutes)) {

                    LOGGER.info(computer.getName() +
                                " is idle for " +
                                TimeUnit2.MILLISECONDS.toMinutes(idleMilliseconds) +
                                " minutes, terminating..");
                    slave.terminate();
                }
            }
            else {
                long upTime = System.currentTimeMillis() - slave.getCreatedAt().getTime();
                final int freeSecondsLeft =
                        (60 * 60) - (int) (TimeUnit2.SECONDS.convert(upTime, TimeUnit2.MILLISECONDS) % (60 * 60));
                if (freeSecondsLeft <= TimeUnit.MINUTES.toSeconds(Math.abs(idleTerminationMinutes))) {
                    LOGGER.info("Idle timeout of " + computer.getName() + " after " +
                                TimeUnit2.MILLISECONDS.toMinutes(idleMilliseconds) + " idle minutes, with " +
                                TimeUnit2.SECONDS.toMinutes(freeSecondsLeft) +
                                " minutes remaining in billing period");
                    slave.terminate();
                }
            }
        }

        return 1;
    }
    //endregion

    //region Public Methods
    @Override
    public void start(SpotinstComputer c) {
        LOGGER.info("Start requested for " + c.getName());
        c.connect(false);
    }

    @Override
    public long check(SpotinstComputer computer) {
        if (!checkLock.tryLock()) {
            return 1;
        }
        else {
            try {
                return CheckComputer(computer);
            }
            finally {
                checkLock.unlock();
            }
        }
    }

    public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Spotinst";
        }
    }

    protected Object readResolve() {
        checkLock = new ReentrantLock(false);
        return this;
    }
    //endregion
}
