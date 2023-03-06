package hudson.plugins.spotinst.jobs.jobSynchronizer;

public class JobSynchronizer {
    //region members
    private              boolean         isReady;
    private static final JobSynchronizer instance = new JobSynchronizer();
    //endregion

    //region constructor
    private JobSynchronizer() {
        isReady = false;
    }
    //endregion

    //region methods
    public static JobSynchronizer getInstance() {
        return instance;
    }

    public synchronized void await() {
        while (isReady == false) {
            try {
                wait();
            }
            catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized void release() {
        isReady = true;
        notifyAll();
    }
    //endregion
}
