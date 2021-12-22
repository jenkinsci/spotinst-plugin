package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.ConnectionMethodEnum;
import hudson.plugins.spotinst.common.SpotAwsInstanceTypesHelper;
import hudson.plugins.spotinst.model.aws.*;
import hudson.plugins.spotinst.repos.IAwsGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.*;
import hudson.slaves.ComputerConnector;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ohadmuchnik on 20/03/2017.
 */
public class AwsSpotinstCloud extends BaseSpotinstCloud {

    //region Members
    private static final Logger                                 LOGGER    =
            LoggerFactory.getLogger(AwsSpotinstCloud.class);
    private static final String                                 CLOUD_URL = "aws/ec2";
    protected            Map<String, Integer>                   executorsByInstanceType;
    private              List<? extends SpotinstInstanceWeight> executorsForTypes;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public AwsSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            List<? extends SpotinstInstanceWeight> executorsForTypes, SlaveUsageEnum usage,
                            String tunnel, Boolean shouldUseWebsocket, Boolean shouldRetriggerBuilds, String vmargs,
                            EnvironmentVariablesNodeProperty environmentVariables,
                            ToolLocationNodeProperty toolLocations, String accountId,
                            ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                            Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride) {

        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId, connectionMethod,
              computerConnector, shouldUsePrivateIp, globalExecutorOverride);

        this.executorsForTypes = new LinkedList<>();

        if (executorsForTypes != null) {
            this.executorsForTypes = executorsForTypes;
        }

        initExecutorsByInstanceType();
    }

    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        IAwsGroupRepo awsGroupRepo = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<AwsScaleUpResult> scaleUpResponse =
                awsGroupRepo.scaleUp(groupId, request.getExecutors(), this.accountId);

        if (scaleUpResponse.isRequestSucceed()) {
            AwsScaleUpResult scaleUpResult = scaleUpResponse.getValue();

            if (scaleUpResult != null) {
                LOGGER.info(String.format("Scale up group %s succeeded", groupId));

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
                LOGGER.error(String.format("Failed to scale up group: %s", groupId));
            }
        }
        else {
            LOGGER.error(
                    String.format("Failed to scale up group: %s. Errors: %s", groupId, scaleUpResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    public Boolean detachInstance(String instanceId) {
        Boolean retVal = false;

        IAwsGroupRepo        awsGroupRepo           = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<Boolean> detachInstanceResponse = awsGroupRepo.detachInstance(instanceId, this.accountId);

        if (detachInstanceResponse.isRequestSucceed()) {
            LOGGER.info(String.format("Instance %s detached", instanceId));
            retVal = true;
        }
        else {
            LOGGER.error(String.format("Failed to detach instance %s. Errors: %s", instanceId,
                                       detachInstanceResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    public void syncGroupInstances() {
        IAwsGroupRepo                       awsGroupRepo      = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<List<AwsGroupInstance>> instancesResponse = awsGroupRepo.getGroupInstances(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AwsGroupInstance> instances = instancesResponse.getValue();
            LOGGER.info(String.format("There are %s instances in group %s", instances.size(), groupId));

            Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId = new HashMap<>();

            for (AwsGroupInstance instance : instances) {
                SlaveInstanceDetails instanceDetails = SlaveInstanceDetails.build(instance);
                slaveInstancesDetailsByInstanceId.put(instanceDetails.getInstanceId(), instanceDetails);
            }

            this.slaveInstancesDetailsByInstanceId = new HashMap<>(slaveInstancesDetailsByInstanceId);

            addNewSlaveInstances(instances);
            removeOldSlaveInstances(instances);


        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }
    }

    @Override
    public Map<String, String> getInstanceIpsById() {
        Map<String, String> retVal = new HashMap<>();

        IAwsGroupRepo                       awsGroupRepo      = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<List<AwsGroupInstance>> instancesResponse = awsGroupRepo.getGroupInstances(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AwsGroupInstance> instances = instancesResponse.getValue();

            for (AwsGroupInstance instance : instances) {
                if (this.getShouldUsePrivateIp()) {
                    retVal.put(instance.getInstanceId(), instance.getPrivateIp());
                }
                else {
                    retVal.put(instance.getInstanceId(), instance.getPublicIp());
                }
            }
        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return CLOUD_URL;
    }

    @Override
    protected Integer getDefaultExecutorsNumber(String instanceType) {
        Integer retVal = null;
        LOGGER.info(String.format("Getting the # of default executors for instance type: %s", instanceType));
        Optional<AwsInstanceType> awsInstanceType = SpotAwsInstanceTypesHelper.getAllInstanceTypes().stream()
                                                                              .filter(i -> i.getInstanceType()
                                                                                            .equals(instanceType))
                                                                              .findFirst();

        if (awsInstanceType.isPresent()) {
            retVal = awsInstanceType.get().getVCPU();
        }

        return retVal;
    }
    //endregion

    //region Private Methods
    @Override
    protected Integer getNumOfExecutors(String instanceType) {
        Integer retVal;

        if (executorsByInstanceType == null) {
            initExecutorsByInstanceType();
        }

        if (executorsByInstanceType.containsKey(instanceType)) {
            retVal = executorsByInstanceType.get(instanceType);
            LOGGER.info(String.format("We have a weight definition for this type of %s", retVal));
        }
        else {
            retVal = super.getNumOfExecutors(instanceType);
        }

        return retVal;
    }

    private List<SpotinstSlave> handleNewAwsSpots(AwsScaleUpResult scaleUpResult, String label) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        LOGGER.info(String.format("%s new spot requests created", scaleUpResult.getNewSpotRequests().size()));

        for (AwsScaleResultNewSpot spot : scaleUpResult.getNewSpotRequests()) {

            SpotinstSlave slave = handleNewAwsInstance(spot.getInstanceId(), spot.getInstanceType(), label);

            retVal.add(slave);
        }

        return retVal;
    }

    private List<SpotinstSlave> handleNewAwsInstances(AwsScaleUpResult scaleUpResult, String label) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        LOGGER.info(String.format("%s new instances launched", scaleUpResult.getNewInstances().size()));

        for (AwsScaleResultNewInstance instance : scaleUpResult.getNewInstances()) {
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

    private void addNewSlaveInstances(List<AwsGroupInstance> elastigroupInstances) {
        if (elastigroupInstances.size() > 0) {
            for (AwsGroupInstance instance : elastigroupInstances) {
                Boolean isSlaveExist = isSlaveExistForInstance(instance);

                if (isSlaveExist == false) {
                    LOGGER.info(String.format("Instance: %s of group: %s doesn't have slave , adding new one",
                                              JsonMapper.toJson(instance), groupId));
                    addSpotinstSlave(instance);
                }
            }
        }
        else {
            LOGGER.info(String.format("There are no new instances to add for group: %s", groupId));
        }
    }

    private void removeOldSlaveInstances(List<AwsGroupInstance> elastigroupInstances) {
        List<SpotinstSlave> allGroupsSlaves = getAllSpotinstSlaves();

        if (allGroupsSlaves.size() > 0) {

            LOGGER.info(String.format("Found %s existing nodes for group %s", allGroupsSlaves.size(), groupId));

            List<String> groupInstanceAndSpotRequestIds = getGroupInstanceAndSpotIds(elastigroupInstances);
            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                if (slaveInstanceId != null) {
                    if (groupInstanceAndSpotRequestIds.contains(slaveInstanceId) == false) {
                        LOGGER.info(
                                String.format("Slave for instance: %s is no longer running in group: %s, removing it",
                                              slaveInstanceId, groupId));
                        try {
                            Jenkins.getInstance().removeNode(slave);
                            LOGGER.info(String.format("Slave: %s removed successfully", slaveInstanceId));
                        }
                        catch (IOException e) {
                            LOGGER.error(String.format("Failed to remove slave from group: %s", groupId), e);
                        }
                    }
                    else {
                        // looking for zombie slave instances which meet the following conditions and clearing them from the EG when 'SLAVE_OFFLINE_THRESHOLD_IN_MINUTES' is meet
                        // 1.is a group slave for this slave id; - trivial here
                        // 2.slave is offline
                        // 3.slave is not connecting
                        // 4.zombie threshold passed

                        terminateOfflineSlaves(slave, slaveInstanceId);
                    }
                }
            }
        }
        else {
            LOGGER.info(String.format("There are no slaves for group: %s", groupId));
        }
    }


    private List<String> getGroupInstanceAndSpotIds(List<AwsGroupInstance> elastigroupInstances) {
        List<String> retVal = new LinkedList<>();

        for (AwsGroupInstance instance : elastigroupInstances) {
            if (instance.getInstanceId() != null) {
                retVal.add(instance.getInstanceId());
            }

            if (instance.getSpotInstanceRequestId() != null) {
                retVal.add(instance.getSpotInstanceRequestId());
            }
        }

        return retVal;
    }

    private Boolean isSlaveExistForInstance(AwsGroupInstance instance) {
        Boolean retVal = false;
        Node    node;

        String instanceId = instance.getInstanceId();
        if (instanceId != null) {
            LOGGER.info(String.format("Checking if slave exist for instance id: %s", instanceId));

            node = Jenkins.getInstance().getNode(instanceId);

            if (node != null) {
                LOGGER.info(String.format("Found slave for instance id: %s", instanceId));
                retVal = true;
            }
        }

        String spotRequestId = instance.getSpotInstanceRequestId();
        if (retVal == false && spotRequestId != null) {
            LOGGER.info(String.format("Checking if slave exist for spot request id: %s", spotRequestId));

            node = Jenkins.getInstance().getNode(spotRequestId);

            if (node != null) {
                LOGGER.info(String.format("Found slave for spot request id: %s", spotRequestId));
                retVal = true;
            }
        }

        return retVal;
    }

    private void addSpotinstSlave(AwsGroupInstance instance) {
        SpotinstSlave slave = handleNewAwsInstance(instance.getInstanceId(), instance.getInstanceType(), null);

        if (slave != null) {
            try {
                Jenkins.getInstance().addNode(slave);
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
            }
        }
    }

    private void initExecutorsByInstanceType() {
        this.executorsByInstanceType = new HashMap<>();

        if (this.executorsForTypes != null) {
            for (SpotinstInstanceWeight instance : this.executorsForTypes) {
                if (instance.getExecutors() != null) {
                    Integer executors = instance.getExecutors();
                    String  type      = instance.getAwsInstanceTypeFromAPI();
                    this.executorsByInstanceType.put(type, executors);
                }
            }
        }
    }
    //endregion

    //region Getters
    public List<? extends SpotinstInstanceWeight> getExecutorsForTypes() {
        return executorsForTypes;
    }
    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Spot AWS Elastigroup";
        }


    }
    //endregion
}
