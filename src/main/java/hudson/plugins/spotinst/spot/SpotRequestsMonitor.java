package hudson.plugins.spotinst.spot;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.common.SpotinstGateway;
import hudson.plugins.spotinst.SpotinstSlave;
import hudson.plugins.spotinst.common.SpotinstContext;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotRequestsMonitor extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotRequestsMonitor.class);
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotRequestsMonitor() {
        super("Spot requests monitor");
        recurrencePeriod = TimeUnit.SECONDS.toMillis(30);
    }
    //endregion

    //region Private Methods
    private void handleLabel(Map<String, Map<String, Integer>> spotRequestWaiting, String label) throws IOException {

        Map<String, Integer> spotRequests = spotRequestWaiting.get(label);
        Set<String> spotRequestsIds = spotRequests.keySet();

        for (String spotRequestId : spotRequestsIds) {
            handleSpotRequest(label, spotRequests, spotRequestId);
        }
    }

    private void handleSpotRequest(String label, Map<String, Integer> spotRequests, String spotRequestId) throws IOException {

        SpotRequest spotRequest =
                SpotinstGateway.getSpotRequest(spotRequestId);

        if (spotRequest != null &&
                spotRequest.getInstanceId() != null) {

            String instanceId = spotRequest.getInstanceId();
            SpotinstSlave node = (SpotinstSlave) Jenkins.getInstance().getNode(spotRequestId);

            if (node != null) {
                updateNodeName(label, spotRequests, spotRequestId, instanceId, node);
            }
        }
    }

    private void updateNodeName(String label, Map<String, Integer> spotRequests, String spotRequestId, String instanceId, SpotinstSlave node) throws IOException {

        LOGGER.info("Spot request: " + spotRequestId + " is ready, setting the node name to instanceId: " + instanceId);

        Jenkins.getInstance().removeNode(node);
        node.setNodeName(instanceId);
        node.setInstanceId(instanceId);
        Jenkins.getInstance().addNode(node);
        SpotinstContext.getInstance().addSpotRequestToInitiating(label, instanceId, spotRequests.get(spotRequestId));
        SpotinstContext.getInstance().removeSpotRequestFromWaiting(label, spotRequestId);
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {

        Map<String, Map<String, Integer>> spotRequestWaiting = SpotinstContext.getInstance().getSpotRequestWaiting();

        if (spotRequestWaiting.size() > 0) {
            Set<String> labels = spotRequestWaiting.keySet();

            for (String label : labels) {
                handleLabel(spotRequestWaiting, label);
            }

        } else {
            LOGGER.info("There are no spot requests to handle");
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
