package hudson.plugins.spotinst.slave;

import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
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
    public static final boolean DISABLED = Boolean.getBoolean(SpotinstRetentionStrategy.class.getName() + ".disabled");

    private static final Logger LOGGER                     = LoggerFactory.getLogger(SpotinstRetentionStrategy.class);
    private static final int    STARTUP_TIME_DEFAULT_VALUE = 30;

    public final int idleTerminationMinutes;

    private transient ReentrantLock checkLock;
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
                LOGGER.info(String.format("Malformed default idleTermination value: %s", idleTerminationMinutes));
            }

            this.idleTerminationMinutes = value;
        }
    }
    //endregion

    //region Public Methods
    @Override
    public void start(SpotinstComputer c) {
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
    //endregion

    //region Protected Methods
    protected Object readResolve() {
        checkLock = new ReentrantLock(false);
        return this;
    }
    //endregion

    //region Private Methods
    private long CheckComputer(SpotinstComputer computer) {
        SpotinstSlave slave = computer.getNode();

        if (idleTerminationMinutes == 0 || slave == null) {
            return 1;
        }

        if (slave.isSlavePending()) {
            LOGGER.info("The slave {} is pending, skipping idle check", slave.getInstanceId());
            return 1;
        }

        if (computer.isIdle() && !DISABLED) {

            final long idleMilliseconds = System.currentTimeMillis() - computer.getIdleStartMilliseconds();

            if (idleTerminationMinutes > 0) {
                if (idleMilliseconds > TimeUnit.MINUTES.toMillis(idleTerminationMinutes)) {

                    LOGGER.info(String.format("%s is idle for %s minutes, terminating..", computer.getName(),
                                              TimeUnit.MILLISECONDS.toMinutes(idleMilliseconds)));
                    slave.terminate();
                }
            }
            else {
                long upTime = System.currentTimeMillis() - slave.getCreatedAt().getTime();
                final int freeSecondsLeft =
                        (60 * 60) - (int) (TimeUnit.SECONDS.convert(upTime, TimeUnit.MILLISECONDS) % (60 * 60));

                if (freeSecondsLeft <= TimeUnit.MINUTES.toSeconds(Math.abs(idleTerminationMinutes))) {
                    LOGGER.info(String.format(
                            "Idle timeout of %s after %s idle minutes, with %s minutes remaining in billing period",
                            computer.getName(), TimeUnit.MILLISECONDS.toMinutes(idleMilliseconds),
                            TimeUnit.SECONDS.toMinutes(freeSecondsLeft)));
                    slave.terminate();
                }
            }
        }

        return 1;
    }
    //endregion

    //region Descriptor class
    public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Spotinst";
        }
    }
    //endregion
}
