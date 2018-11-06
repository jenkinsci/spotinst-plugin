package hudson.plugins.spotinst.cloud;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public class PendingExecutorsCounts {
    private Integer pendingExecutors;
    private Integer initiatingExecutors;

    public Integer getPendingExecutors() {
        return pendingExecutors;
    }

    public void setPendingExecutors(Integer pendingExecutors) {
        this.pendingExecutors = pendingExecutors;
    }

    public Integer getInitiatingExecutors() {
        return initiatingExecutors;
    }

    public void setInitiatingExecutors(Integer initiatingExecutors) {
        this.initiatingExecutors = initiatingExecutors;
    }
}
