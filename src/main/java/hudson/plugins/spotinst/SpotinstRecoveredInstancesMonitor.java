package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.common.ContextInstance;
import hudson.plugins.spotinst.common.InstanceType;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import hudson.plugins.spotinst.elastigroup.ElastigroupInstance;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstRecoveredInstancesMonitor extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRecoveredInstancesMonitor.class);
    final long recurrencePeriod;
    //endregion

    //region Constructor
    public SpotinstRecoveredInstancesMonitor() {
        super("Recovered Instances monitor");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(10);
    }
    //endregion

    //region Private Methods
    private void handleGroup(String groupId) throws IOException {

        // add new running instances to slave
        List<ElastigroupInstance> currentRunningInstances = SpotinstGateway.getElastigroupInstancesDetailed(groupId);
        LOGGER.info("There are {} running insatnces at group {}", currentRunningInstances.size(), groupId);
        addNewRecoveredInstancesToGroup(groupId, currentRunningInstances);
        removeOldInstancesThatHaveTerminatedFromGroup(groupId, currentRunningInstances);
    }

    private void addNewRecoveredInstancesToGroup(String groupId, List<ElastigroupInstance> currentRunningInstances) throws IOException {
        List<SpotinstSlave> slaves = new ArrayList<>();
        if (currentRunningInstances.size() > 0) {
            for (ElastigroupInstance instance : currentRunningInstances) {
                boolean isSlaveExist = doSlaveExist(instance, groupId);
                if (isSlaveExist == false) {
                    SpotinstSlave slave = buildSpotinstSlave(instance, groupId);
                    if (slaves != null) {
                        slaves.add(slave);
                    }
                }
            }

            if (slaves.size() > 0) {
                LOGGER.info("Adding {} new slaves to group {} ", slaves.size(),  groupId);
                for (SpotinstSlave slave : slaves) {
                    LOGGER.info("Adding slave {} to group {}", slave.getInstanceId(), groupId);
                    Jenkins.getInstance().addNode(slave);
                }
            } else {
                LOGGER.info("There are no slaves to add for group: " + groupId);
            }
        } else {
            LOGGER.info("There are no spot requests to handle for group: " + groupId);
        }
    }

    private void removeOldInstancesThatHaveTerminatedFromGroup(String groupId, List<ElastigroupInstance> currentRunningInstances) throws IOException {
        List<String> runningInstancesIds = new ArrayList<>();
        List<Node> allNodes = Jenkins.getInstance().getNodes();
        List<SpotinstSlave> allGroupsSlaves = new ArrayList<>();

        for (Node node : allNodes) {
            if (node instanceof SpotinstSlave) {
                SpotinstSlave slave = (SpotinstSlave)node;
                if (slave.getElastigroupId() != null &&
                        slave.getElastigroupId().equals(groupId)) {
                    allGroupsSlaves.add(slave);
                }
            }
        }

        for (ElastigroupInstance instance : currentRunningInstances) {
            if (instance.getInstanceId() != null) {
                runningInstancesIds.add(instance.getInstanceId());
            }
            if (instance.getSpotInstanceRequestId() != null) {
                runningInstancesIds.add(instance.getSpotInstanceRequestId());
            }
        }

        for (SpotinstSlave slave : allGroupsSlaves) {
            String slaveInstanceId = slave.getInstanceId();

            if (runningInstancesIds.contains(slaveInstanceId) == false) {
                LOGGER.info("Removing slave {} not in a running state." ,slaveInstanceId);
                Jenkins.getInstance().removeNode(slave);
            }
        }

    }

    private boolean doSlaveExist(ElastigroupInstance instance, String groupId) {
        boolean retVal = false;
        SpotinstSlave node = (SpotinstSlave) Jenkins.getInstance().getNode(instance.getInstanceId());
        if (node != null) {
           retVal = true;
        } else {
            Map<String, ContextInstance> groupSpotInitiating = SpotinstContext.getInstance().getSpotRequestInitiating().get(groupId);
            Map<String, ContextInstance> groupSpotWaiting = SpotinstContext.getInstance().getSpotRequestWaiting().get(groupId);

            if (groupSpotWaiting != null &&
                    groupSpotWaiting.containsKey(instance.getSpotInstanceRequestId())) {
                retVal = true;
            }

            if (groupSpotInitiating != null &&
                    groupSpotInitiating.containsKey(instance.getInstanceId())) {
                retVal = true;
            }
        }
        return retVal;
    }

    private Integer getNumOfExecutors(String instanceType, Map<InstanceType, Integer> executorsForInstanceType) {
        LOGGER.info("Determining the # of executors for instance type: " + instanceType);
        Integer retVal;
        InstanceType type = InstanceType.fromValue(instanceType);
        if (executorsForInstanceType.containsKey(type)) {
            retVal = executorsForInstanceType.get(type);
            LOGGER.info("We have a weight definition for this type of " + retVal);
        } else {
            retVal = SpotinstSlave.executorsForInstanceType(InstanceType.fromValue(instanceType));
            LOGGER.info("Using the default value of " + retVal);
        }
        return retVal;

    }

    private SpotinstSlave buildSpotinstSlave(ElastigroupInstance newInstance,
                                             String elastigroupId) {

        SpotinstSlave slave = null;
        SpotinstCloud spotinstCloud = (SpotinstCloud) Jenkins.getInstance().getCloud(elastigroupId);
        if (spotinstCloud != null) {
            Map<InstanceType, Integer> executorsForInstanceType = spotinstCloud.getExecutorsForInstanceType();
            Integer numOfExecutors = getNumOfExecutors(newInstance.getInstanceType(), executorsForInstanceType);

            try {
                slave = new SpotinstSlave(
                        newInstance.getInstanceId(),
                        elastigroupId,
                        newInstance.getInstanceId(),
                        newInstance.getInstanceType(),
                        spotinstCloud.getLabelString(),
                        spotinstCloud.getIdleTerminationMinutes(),
                        spotinstCloud.getWorkspaceDir(),
                        String.valueOf(numOfExecutors));

            } catch (Descriptor.FormException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return slave;
    }

    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        Set<String> groupIds = new HashSet<>();

        for (Cloud cloud : Jenkins.getInstance().clouds) {
            if (cloud instanceof SpotinstCloud) {
                SpotinstCloud spotinstCloud = (SpotinstCloud) cloud;
                groupIds.add(spotinstCloud.getGroupId());
            }
        }

        if (groupIds.size() > 0) {

            for (String groupId : groupIds) {
                try {
                    handleGroup(groupId);
                } catch (Exception e) {
                    LOGGER.info("Waiting list is modified right now, will be handle in next iteration");
                }
            }

        } else {
            LOGGER.info("There are no groups to handle");
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }
    //endregion
}
