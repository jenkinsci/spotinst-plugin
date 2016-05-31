package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstComputersMonitor extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstComputersMonitor.class);
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstComputersMonitor() {
        super("Offline computers monitor");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(5);
    }
    //endregion

    //region Private Methods
    private void replaceNodeName(String elastigroupId, List<String> newInstancesIds, String instanceId) throws IOException {
        if (newInstancesIds.get(0) != null) {
            String newInstanceId = newInstancesIds.get(0);
            SpotinstSlave node = (SpotinstSlave) Jenkins.getInstance().getNode(instanceId);
            Jenkins.getInstance().removeNode(node);
            node.setNodeName(newInstanceId);
            node.setInstanceId(newInstanceId);
            Jenkins.getInstance().addNode(node);
            newInstancesIds.remove(newInstanceId);
            SpotinstContext.getInstance().removeFromOfflineComputers(elastigroupId, instanceId);
        }
    }

    private List<String> getNewInstances(String elastigroupId) {
        List<String> newInstancesIds = new LinkedList<String>();
        List<String> instances = SpotinstGateway.getElastigroupInstances(elastigroupId);

        LOGGER.info("Got " + instances.size() + " instances from group " + elastigroupId + ", " + instances.toString());

        for (String instanceId : instances) {
            List<Node> nodes = Jenkins.getInstance().getNodes();

            boolean isExists = false;
            for (Node node : nodes) {
                if (node.getNodeName().equals(instanceId)) {
                    isExists = true;
                    break;
                }
            }

            LOGGER.info("instanceId : " + instanceId + " isExists = " + isExists);

            if (isExists == false) {
                newInstancesIds.add(instanceId);
            }
        }

        return newInstancesIds;
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        Map<String, List<String>> offlineComputers = SpotinstContext.getInstance().getOfflineComputers();

        if (offlineComputers.size() > 0) {
            Set<String> elastigroupsIds = offlineComputers.keySet();

            for (String elastigroupId : elastigroupsIds) {

                List<String> newInstancesIds = getNewInstances(elastigroupId);
                List<String> offlineInstances = offlineComputers.get(elastigroupId);

                for (String instanceId : offlineInstances) {
                    if (newInstancesIds.size() > 0) {
                        replaceNodeName(elastigroupId, newInstancesIds, instanceId);
                    }
                }
            }
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
