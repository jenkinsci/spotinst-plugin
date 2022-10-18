package hudson.plugins.spotinst.slave;

import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ohadmuchnik on 27/05/2016.
 */
public class SpotinstComputer extends SlaveComputer {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstComputer.class);
    private              long   launchTime;
    //endregion

    //region overrides
    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);
        SpotinstSlave spotinstNode = this.getNode();

        if (spotinstNode != null) {
            BaseSpotinstCloud spotinstCloud = spotinstNode.getSpotinstCloud();

            if (spotinstCloud != null) {
                if (spotinstCloud.getIsSingleTaskNodesEnabled()) {
                    String msg = String.format(
                            "Node %s has accepted a job and 'Single Task Nodes' setting on Cloud %s is on. Node will not accept any more jobs.",
                            spotinstNode.getNodeName(), spotinstCloud.getDisplayName());
                    LOGGER.info(msg);
                    this.setAcceptingTasks(false);
                    SpotinstNonLocalizable spotinstNonLocalizable = new SpotinstNonLocalizable(msg);
                    SpotinstSingleTaskOfflineCause spotinstSingleTaskOfflineCause = new SpotinstSingleTaskOfflineCause(spotinstNonLocalizable);
                    this.setTemporarilyOffline(true,spotinstSingleTaskOfflineCause);
                }
            }
            else {
                LOGGER.error(String.format(
                        "Node %s has accepted a job but can't determine 'Single Task Nodes' setting because SpotinstNode's SpotinstCloud appears to be null.",
                        spotinstNode.getNodeName()));
            }
        } else {
            LOGGER.error(String.format(
                    "Executor of Node %s has accepted a job but can't determine 'Single Task Nodes' setting because SpotinstNode is null.", executor.getOwner().getName()));
        }
    }

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
    @RequirePOST
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);

        try {
            if (getNode() != null) {
                getNode().forceTerminate();
            }

            return new HttpRedirect("..");
        } catch (NullPointerException ex) {
            return HttpResponses.error(500, ex);
        }
    }


    /**
     * For SSH-connecting clouds, we initiate a {@link SpotinstComputer} before we
     * have a valid SSH-launcher and Jenkins doesn't auto-sync the computer's launcher
     * (JNLP by default) when its corresponding {@link SpotinstSlave} launcher is updated.
     * {@link hudson.model.Computer#setNode(Node)} calls {@link SlaveComputer#grabLauncher(Node)}
     * which will perform the required update.
     */
    public void resyncNode() {
        SpotinstSlave node = this.getNode();

        if (node != null) {
            this.setNode(node);
        }
    }

    //endregion
}
