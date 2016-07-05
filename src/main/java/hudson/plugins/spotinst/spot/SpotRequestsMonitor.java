package hudson.plugins.spotinst.spot;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.SpotinstSlave;
import hudson.plugins.spotinst.common.ContextInstanceData;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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
    private void handleGroup(Map<String, Map<String, ContextInstanceData>> spotRequestWaiting, String groupId) throws IOException {

        Map<String, ContextInstanceData> spotRequests = spotRequestWaiting.get(groupId);
        Set<String> spotRequestsIds = spotRequests.keySet();
        if (spotRequestsIds.size() > 0) {
            for (String spotRequestId : spotRequestsIds) {
                handleSpotRequest(spotRequests.get(spotRequestId), spotRequestId, groupId);
            }
        } else {
            LOGGER.info("There are no spot requests to handle for group: " + groupId);
        }
    }

    private void handleSpotRequest(ContextInstanceData contextInstanceData, String spotRequestId, String groupId) throws IOException {

        boolean isSpotStuck = isTimePassed(contextInstanceData.getCreatedAt(), 15);

        if (isSpotStuck) {
            LOGGER.info("Spot request: " + spotRequestId + " is in waiting state for over than 15 minutes, ignoring this Spot request");
            SpotinstContext.getInstance().removeSpotRequestFromWaiting(groupId, spotRequestId);
        } else {
            SpotRequest spotRequest =
                    SpotinstGateway.getSpotRequest(spotRequestId);

            if (spotRequest != null &&
                    spotRequest.getInstanceId() != null) {

                String instanceId = spotRequest.getInstanceId();
                SpotinstSlave node = (SpotinstSlave) Jenkins.getInstance().getNode(spotRequestId);

                if (node != null) {
                    updateNodeName(contextInstanceData.getNumOfExecutors(), spotRequestId, instanceId, node);
                }
            }
        }
    }

    private void updateNodeName(Integer numOfExecutors, String spotRequestId, String instanceId, SpotinstSlave node) throws IOException {

        LOGGER.info("Spot request: " + spotRequestId + " is ready, setting the node name to instanceId: " + instanceId);

        Jenkins.getInstance().removeNode(node);
        node.setNodeName(instanceId);
        node.setInstanceId(instanceId);
        Jenkins.getInstance().addNode(node);
        String elastigroupId = node.getElastigroupId();
        SpotinstContext.getInstance().addSpotRequestToInitiating(elastigroupId, instanceId, numOfExecutors);
        SpotinstContext.getInstance().removeSpotRequestFromWaiting(elastigroupId, spotRequestId);
    }

    private boolean isTimePassed(Date from, Integer minutes) {
        boolean retVal = false;
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.MINUTE, minutes);
        Date timeToPass = calendar.getTime();

        if (now.after(timeToPass)) {
            retVal = true;
        }

        return retVal;
    }

    private void removeStuckInitiatingInstances() {
        Map<String, Map<String, ContextInstanceData>> spotRequestInitiating = SpotinstContext.getInstance().getSpotRequestInitiating();
        if (spotRequestInitiating.size() > 0) {
            Set<String> groupIds = spotRequestInitiating.keySet();
            for (String groupId : groupIds) {
                handleInitiatingForGroup(spotRequestInitiating, groupId);
            }
        }
    }

    private void handleInitiatingForGroup(Map<String, Map<String, ContextInstanceData>> spotRequestInitiating, String groupId) {
        Map<String, ContextInstanceData> spotInitiating = spotRequestInitiating.get(groupId);
        Set<String> instanceIds = spotInitiating.keySet();
        for (String instanceId : instanceIds) {
            handleInitiatingInstance(groupId, spotInitiating, instanceId);
        }
    }

    private void handleInitiatingInstance(String groupId, Map<String, ContextInstanceData> spotInitiating, String instanceId) {
        ContextInstanceData contextInstanceData = spotInitiating.get(instanceId);
        boolean isInstanceStuck = isTimePassed(contextInstanceData.getCreatedAt(), 10);
        if (isInstanceStuck) {
            LOGGER.info("Instance: " + instanceId + " is in initiating state for over than 10 minutes, ignoring this instance");
            SpotinstContext.getInstance().removeSpotRequestFromInitiating(groupId, instanceId);
        }
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {

        Map<String, Map<String, ContextInstanceData>> spotRequestWaiting = SpotinstContext.getInstance().getSpotRequestWaiting();

        if (spotRequestWaiting.size() > 0) {
            Set<String> groupIds = spotRequestWaiting.keySet();

            for (String groupId : groupIds) {
                handleGroup(spotRequestWaiting, groupId);
            }

        } else {
            LOGGER.info("There are no spot requests to handle");
        }

        removeStuckInitiatingInstances();
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
