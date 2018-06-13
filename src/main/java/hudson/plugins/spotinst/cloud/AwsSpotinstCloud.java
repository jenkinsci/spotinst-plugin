package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.AwsInstanceTypeEnum;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.common.TimeUtils;
import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstance;
import hudson.plugins.spotinst.model.scale.aws.ScaleResultNewInstance;
import hudson.plugins.spotinst.model.scale.aws.ScaleResultNewSpot;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.model.spot.SpotRequest;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ohadmuchnik on 20/03/2017.
 */
public class AwsSpotinstCloud extends BaseSpotinstCloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSpotinstCloud.class);
    private Map<AwsInstanceTypeEnum, Integer>      executorsForInstanceType;
    private List<? extends SpotinstInstanceWeight> executorsForTypes;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public AwsSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            List<? extends SpotinstInstanceWeight> executorsForTypes, SlaveUsageEnum usage,
                            String tunnel, String vmargs) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, vmargs);
        this.executorsForTypes = new LinkedList<>();
        executorsForInstanceType = new HashMap<>();
        if (executorsForTypes != null) {
            this.executorsForTypes = executorsForTypes;
            for (SpotinstInstanceWeight executors : executorsForTypes) {
                if (executors.getExecutors() != null) {
                    executorsForInstanceType.put(executors.getAwsInstanceType(), executors.getExecutors());
                }
            }
        }
    }

    //endregion

    //region Private Methods
    private Integer getNumOfExecutors(String instanceType) {
        LOGGER.info("Determining the # of executors for instance type: " + instanceType);
        Integer             retVal = 1;
        AwsInstanceTypeEnum type   = AwsInstanceTypeEnum.fromValue(instanceType);
        if (type != null) {
            if (executorsForInstanceType.containsKey(type)) {
                retVal = executorsForInstanceType.get(type);
                LOGGER.info("We have a weight definition for this type of " + retVal);
            }
            else {
                retVal = type.getExecutors();
                LOGGER.info("Using the default value of " + retVal);
            }
        }
        return retVal;
    }

    private List<SpotinstSlave> handleNewAwsSpots(ScaleUpResult scaleUpResult, String label) {
        List<SpotinstSlave> retVal = new LinkedList<>();
        LOGGER.info(scaleUpResult.getNewSpotRequests().size() + " new spot requests created");
        for (ScaleResultNewSpot spot : scaleUpResult.getNewSpotRequests()) {
            SpotinstSlave slave = handleNewAwsSpot(spot.getSpotInstanceRequestId(), spot.getInstanceType(), label);
            retVal.add(slave);
        }

        return retVal;
    }

    private SpotinstSlave handleNewAwsSpot(String spotRequestId, String instanceType, String label) {
        Integer executors = getNumOfExecutors(instanceType);
        addToPending(spotRequestId, executors, PendingInstance.StatusEnum.SPOT_PENDING, label);
        SpotinstSlave retVal = buildSpotinstSlave(spotRequestId, instanceType, String.valueOf(executors));

        return retVal;
    }

    private List<SpotinstSlave> handleNewAwsInstances(ScaleUpResult scaleUpResult, String label) {
        List<SpotinstSlave> retVal = new LinkedList<>();
        LOGGER.info(scaleUpResult.getNewInstances().size() + " new instances launched");
        for (ScaleResultNewInstance instance : scaleUpResult.getNewInstances()) {
            SpotinstSlave slave = handleNewAwsInstance(instance.getInstanceId(), instance.getInstanceType(), label);
            retVal.add(slave);
        }

        return retVal;
    }

    private SpotinstSlave handleNewAwsInstance(String instanceId, String instanceType, String label) {
        Integer executors = getNumOfExecutors(instanceType);
        addToPending(instanceId, executors, PendingInstance.StatusEnum.INSTANCE_INITIATING, label);
        SpotinstSlave retVal = buildSpotinstSlave(instanceId, instanceType, String.valueOf(executors));

        return retVal;
    }

    //region Recover
    private void addNewSlaveInstances(List<AwsElastigroupInstance> elastigroupInstances) {
        if (elastigroupInstances.size() > 0) {
            for (AwsElastigroupInstance instance : elastigroupInstances) {
                boolean isSlaveExist = isSlaveExistForInstance(instance);
                if (isSlaveExist == false) {
                    LOGGER.info(String.format("Instance: %s of group: %s doesn't have slave , adding new one",
                                              JsonMapper.toJson(instance), groupId));
                    addSpotinstSlave(instance);
                }
            }
        }
        else {
            LOGGER.info("There are no new instances to add for group: {}", groupId);
        }
    }

    private void removeOldSlaveInstances(List<AwsElastigroupInstance> elastigroupInstances) {
        List<SpotinstSlave> allGroupsSlaves = loadSlaves();
        if (allGroupsSlaves != null && allGroupsSlaves.size() > 0) {

            LOGGER.info(String.format("Found %s existing nodes for group %s", allGroupsSlaves.size(), groupId));

            List<String> groupInstanceAndSpotRequestIds = getGroupInstanceAndSpotIds(elastigroupInstances);
            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                if (slaveInstanceId != null && groupInstanceAndSpotRequestIds.contains(slaveInstanceId) == false) {
                    LOGGER.info("Slave for instance: {} is no longer running in group: {}, removing it",
                                slaveInstanceId, groupId);
                    try {
                        Jenkins.getInstance().removeNode(slave);
                        LOGGER.info("Slave: {} removed successfully", slaveInstanceId);
                    }
                    catch (IOException e) {
                        LOGGER.error("Failed to remove slave from group: {}", groupId, e);
                    }
                }
            }
        }
        else {
            LOGGER.info("There are no slaves for group: {}", groupId);
        }
    }

    private List<String> getGroupInstanceAndSpotIds(List<AwsElastigroupInstance> elastigroupInstances) {
        List<String> groupInstanceAndSpotRequestIds = new LinkedList<>();
        for (AwsElastigroupInstance instance : elastigroupInstances) {
            if (instance.getInstanceId() != null) {
                groupInstanceAndSpotRequestIds.add(instance.getInstanceId());
            }
            if (instance.getSpotInstanceRequestId() != null) {
                groupInstanceAndSpotRequestIds.add(instance.getSpotInstanceRequestId());
            }
        }
        return groupInstanceAndSpotRequestIds;
    }

    private boolean isSlaveExistForInstance(AwsElastigroupInstance instance) {
        boolean retVal = false;
        Node    node;
        if (instance.getInstanceId() != null) {
            LOGGER.info(String.format("Checking if slave exist for instance id"));
            node = Jenkins.getInstance().getNode(instance.getInstanceId());
            if (node != null) {
                LOGGER.info(String.format("Found slave for instance id"));
                retVal = true;
            }
        }

        if (retVal == false && instance.getSpotInstanceRequestId() != null) {
            LOGGER.info(String.format("Checking if slave exist for spot request id"));
            node = Jenkins.getInstance().getNode(instance.getSpotInstanceRequestId());
            if (node != null) {
                LOGGER.info(String.format("Found slave for spot request id"));
                retVal = true;
            }
        }
        return retVal;
    }

    private void addSpotinstSlave(AwsElastigroupInstance instance) {
        SpotinstSlave slave = null;
        if (instance.getInstanceId() != null) {
            slave = handleNewAwsInstance(instance.getInstanceId(), instance.getInstanceType(), null);
        }
        else if (instance.getSpotInstanceRequestId() != null) {
            slave = handleNewAwsSpot(instance.getSpotInstanceRequestId(), instance.getInstanceType(), null);
        }

        if (slave != null) {
            try {
                Jenkins.getInstance().addNode(slave);
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
            }
        }
    }
    //endregion

    //region Monitor
    private void handlePendingSpot(PendingInstance pendingSpot) throws IOException {
        boolean isStuck =
                TimeUtils.isTimePassed(pendingSpot.getCreatedAt(), Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES);

        if (isStuck) {
            LOGGER.info(
                    String.format("spot %s is in waiting state for over than %s minutes, ignoring this Spot request",
                                  pendingSpot.getId(), Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES));
            pendingInstances.remove(pendingSpot.getId());
        }
        else {
            SpotRequest spotRequest = SpotinstApi.getInstance().getSpotRequest(pendingSpot.getId());

            if (spotRequest != null && spotRequest.getInstanceId() != null) {

                String instanceId = spotRequest.getInstanceId();

                LOGGER.info(
                        "Spot request: " + pendingSpot.getId() + " is ready, setting the node name to instanceId: " +
                        instanceId);

                SpotinstSlave node = (SpotinstSlave) Jenkins.getInstance().getNode(pendingSpot.getId());

                if (node != null) {
                    updateNodeName(instanceId, node);
                    updatePendingInstanceStatus(pendingSpot, instanceId);
                }
            }
        }
    }

    private void updateNodeName(String instanceId, SpotinstSlave node) throws IOException {
        Jenkins.getInstance().removeNode(node);
        node.setNodeName(instanceId);
        node.setInstanceId(instanceId);
        Jenkins.getInstance().addNode(node);
    }

    private void updatePendingInstanceStatus(PendingInstance pendingSpot, String instanceId) {
        pendingInstances.remove(pendingSpot.getId());
        addToPending(instanceId, pendingSpot.getNumOfExecutors(), PendingInstance.StatusEnum.INSTANCE_INITIATING,
                     pendingSpot.getRequestedLabel());
    }

    private void handleInitiatingInstance(PendingInstance pendingInstance) {
        boolean isInstanceStuck =
                TimeUtils.isTimePassed(pendingInstance.getCreatedAt(), Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES);
        if (isInstanceStuck) {
            LOGGER.info(
                    String.format("Instance %s is in initiating state for over than %s minutes, ignoring this instance",
                                  pendingInstance.getId(), Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES));
            pendingInstances.remove(pendingInstance.getId());
        }
    }
    //endregion
    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal        = new LinkedList<>();
        ScaleUpResult       scaleUpResult = SpotinstApi.getInstance().awsScaleUp(groupId, request.getExecutors());

        if (scaleUpResult != null) {
            if (scaleUpResult.getNewInstances() != null) {
                List<SpotinstSlave> newInstanceSlaves = handleNewAwsInstances(scaleUpResult, request.getLabel());
                retVal.addAll(newInstanceSlaves);
            }
            if (scaleUpResult.getNewSpotRequests() != null) {
                List<SpotinstSlave> newSpotSlaves = handleNewAwsSpots(scaleUpResult, request.getLabel());
                retVal.addAll(newSpotSlaves);
            }
        }
        else {
            LOGGER.error("Failed to scale up Elastigroup: " + groupId);
        }

        return retVal;
    }

    @Override
    public boolean detachInstance(String instanceId) {
        boolean retVal = SpotinstApi.getInstance().awsDetachInstance(instanceId);
        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return "aws/ec2";
    }

    @Override
    public void recoverInstances() {

        LOGGER.info(String.format("Handling group %s recover", groupId));

        List<AwsElastigroupInstance> elastigroupInstances =
                SpotinstApi.getInstance().getAwsElastigroupInstances(groupId);

        if (elastigroupInstances != null) {

            LOGGER.info(String.format("There are %s instances in group %s", elastigroupInstances.size(), groupId));

            addNewSlaveInstances(elastigroupInstances);
            removeOldSlaveInstances(elastigroupInstances);
        }
        else {
            LOGGER.error(String.format("can't recover group %s", groupId));
        }
    }

    @Override
    public void monitorInstances() {
        LOGGER.info(String.format("Monitoring group %s instances", groupId));
        if (pendingInstances.size() > 0) {

            List<String> pendingInstanceIds = new LinkedList<>(pendingInstances.keySet());
            for (String id : pendingInstanceIds) {
                try {
                    PendingInstance pendingInstance = pendingInstances.get(id);
                    if (pendingInstance != null) {
                        switch (pendingInstance.getStatus()) {
                            case SPOT_PENDING: {
                                handlePendingSpot(pendingInstance);
                            }
                            break;
                            case INSTANCE_INITIATING: {
                                handleInitiatingInstance(pendingInstance);
                            }
                            break;
                        }
                    }
                }
                catch (IOException e) {
                    LOGGER.warn(String.format("Failed to handle pending instance %s, will be handled in next iteration",
                                              id), e);
                }
            }
        }
        else {
            LOGGER.info(String.format("There are no instances to handle for group %s", groupId));
        }
    }

    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Spotinst AWS Elastigroup";
        }
    }
    //endregion

    //region Getters
    public List<? extends SpotinstInstanceWeight> getExecutorsForTypes() {
        return executorsForTypes;
    }
    //endregion
}
