package hudson.plugins.spotinst.slave;

import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

/**
 * Created by ohadmuchnik on 27/05/2016.
 */
public class SpotinstComputer extends SlaveComputer {

    //region Members
    private long launchTime;
    //endregion

    //region Constructor
    public SpotinstComputer(SpotinstSlave slave) {
        super(slave);
    }
    //endregion

    //region Public Methods
    public long getLaunchTime() {
        return launchTime;
    }

    public long getUptime() {
        return System.currentTimeMillis() - getLaunchTime();
    }


    public void setLaunchTime(long launchTime) {
        this.launchTime = launchTime;
    }

    @Override
    public SpotinstSlave getNode() {
        return (SpotinstSlave) super.getNode();
    }

    @Override
    public HttpResponse doDoDelete() {
        checkPermission(DELETE);
        if (getNode() != null) {
            getNode().forceTerminate();
        }
        return new HttpRedirect("..");
    }
    //endregion
}
