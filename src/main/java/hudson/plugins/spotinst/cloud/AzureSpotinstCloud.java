package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.ConnectionMethodEnum;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;
import hudson.plugins.spotinst.model.azure.AzureScaleSetSizeEnum;
import hudson.plugins.spotinst.repos.IAzureGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveInstanceDetails;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.ComputerConnector;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

/**
 * Created by ohadmuchnik on 19/06/2017.
 */
public class AzureSpotinstCloud extends BaseSpotinstCloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSpotinstCloud.class);
    //endregion

    //region Constructor
    @DataBoundConstructor
    public AzureSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                              SlaveUsageEnum usage, String tunnel, Boolean shouldUseWebsocket,
                              Boolean shouldRetriggerBuilds, String vmargs,
                              EnvironmentVariablesNodeProperty environmentVariables,
                              ToolLocationNodeProperty toolLocations, String accountId,
                              ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                              Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId, connectionMethod,
              computerConnector, shouldUsePrivateIp, globalExecutorOverride);
    }
    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        IAzureGroupRepo      azureGroupRepo  = RepoManager.getInstance().getAzureGroupRepo();
        ApiResponse<Boolean> scaleUpResponse = azureGroupRepo.scaleUp(groupId, request.getExecutors(), this.accountId);

        if (scaleUpResponse.isRequestSucceed()) {
            LOGGER.info(String.format("Scale up group %s succeeded", groupId));
            addToGroupPending(request);
        }
        else {
            LOGGER.error(
                    String.format("Failed to scale up group: %s. Errors: %s", groupId, scaleUpResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    public Boolean detachInstance(String instanceId) {
        Boolean         retVal         = false;
        IAzureGroupRepo azureGroupRepo = RepoManager.getInstance().getAzureGroupRepo();
        ApiResponse<Boolean> detachInstanceResponse = azureGroupRepo.detachInstance(groupId, instanceId, accountId);

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

    }

    @Override
    protected void internalSyncGroupInstances() {

    }

    @Override
    public Map<String, String> getInstanceIpsById() {
        Map<String, String> retVal = new HashMap<>();

        IAzureGroupRepo awsGroupRepo = RepoManager.getInstance().getAzureGroupRepo();
        ApiResponse<List<AzureGroupInstance>> instancesResponse =
                awsGroupRepo.getGroupInstances(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AzureGroupInstance> instances = instancesResponse.getValue();

            for (AzureGroupInstance instance : instances) {
                if (getShouldUsePrivateIp()) {
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
    public void monitorInstances() {
        if(isCloudReadyForGroupCommunication()) {
            IAzureGroupRepo azureGroupRepo = RepoManager.getInstance().getAzureGroupRepo();
            ApiResponse<List<AzureGroupInstance>> instancesResponse = azureGroupRepo.getGroupInstances(groupId, this.accountId);

            if (instancesResponse.isRequestSucceed()) {
                List<AzureGroupInstance> instances = instancesResponse.getValue();

                LOGGER.info(String.format("There are %s instances in group %s", instances.size(), groupId));


                Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId = new HashMap<>();

                for (AzureGroupInstance instance : instances) {
                    SlaveInstanceDetails instanceDetails = SlaveInstanceDetails.build(instance);
                    slaveInstancesDetailsByInstanceId.put(instanceDetails.getInstanceId(), instanceDetails);
                }

                this.slaveInstancesDetailsByInstanceId = new HashMap<>(slaveInstancesDetailsByInstanceId);

                removeOldSlaveInstances(instances);

                addNewSlaveInstances(instances);

            }
            else {
                LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId, instancesResponse.getErrors()));
            }
        }
        else{
            LOGGER.error(SKIPPED_METHOD_GROUP_IS_NIT_READY_ERROR_LOGGER_FORMAT, "monitorInstances", groupId);
        }

        super.monitorInstances();
    }

    @Override
    protected Integer getPendingThreshold() {
        return Constants.AZURE_PENDING_INSTANCE_TIMEOUT_IN_MINUTES;
    }

    @Override
    public Boolean onInstanceReady(String instanceId) {
        Boolean retVal = isCloudReadyForGroupCommunication();

        if (retVal) {
            removeFromPending();
        }
        else {
            LOGGER.error(SKIPPED_METHOD_GROUP_IS_NIT_READY_ERROR_LOGGER_FORMAT, "onInstanceReady", groupId);
        }

        return retVal;
    }

    @Override
    protected PendingExecutorsCounts getPendingExecutors(ProvisionRequest request) {
        PendingExecutorsCounts retVal              = new PendingExecutorsCounts();
        Integer                pendingExecutors    = 0;
        Integer                initiatingExecutors = 0;

        if (pendingInstances.size() > 0) {
            pendingExecutors = pendingInstances.size();
        }

        retVal.setPendingExecutors(pendingExecutors);
        retVal.setInitiatingExecutors(initiatingExecutors);

        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return "azure/compute";
    }

    @Override
    protected Integer getDefaultExecutorsNumber(String instanceType) {
        Integer retVal;
        LOGGER.info(String.format("Getting the # of default executors for instance type: %s", instanceType));
        AzureScaleSetSizeEnum enumMember = AzureScaleSetSizeEnum.fromValue(instanceType);

        if (enumMember != null) {
            retVal = enumMember.getExecutors();
        }
        else {
            retVal = null;
        }

        return retVal;
    }

    //endregion

    //region Private Methods
    private void addNewSlaveInstances(List<AzureGroupInstance> azureGroupInstances) {
        if (azureGroupInstances.size() > 0) {
            for (AzureGroupInstance instance : azureGroupInstances) {
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

    private void removeOldSlaveInstances(List<AzureGroupInstance> azureGroupInstances) {
        List<SpotinstSlave> allGroupsSlaves = getAllSpotinstSlaves();

        if (allGroupsSlaves.size() > 0) {
            List<String> groupInstanceIds = getGroupInstanceIds(azureGroupInstances);

            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                if (slaveInstanceId != null && groupInstanceIds.contains(slaveInstanceId) == false) {
                    LOGGER.info(String.format("Slave for instance: %s is no longer running in group: %s, removing it",
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
                    terminateOfflineSlaves(slave, slaveInstanceId);
                }
            }
        }
        else {
            LOGGER.info(String.format("There are no slaves for group: %s", groupId));
        }

    }

    private List<String> getGroupInstanceIds(List<AzureGroupInstance> azureGroupInstances) {
        List<String> retVal = new LinkedList<>();

        for (AzureGroupInstance instance : azureGroupInstances) {
            if (instance.getInstanceId() != null) {
                retVal.add(instance.getInstanceId());
            }
        }

        return retVal;
    }

    private Boolean isSlaveExistForInstance(AzureGroupInstance instance) {
        Boolean retVal = false;

        Node node = Jenkins.getInstance().getNode(instance.getInstanceId());

        if (node != null) {
            retVal = true;
        }

        return retVal;
    }

    private void addSpotinstSlave(AzureGroupInstance instance) {
        SpotinstSlave slave = null;

        if (instance.getInstanceId() != null) {
            String vmSize = instance.getVmSize();
            LOGGER.info(String.format("Setting the # of executors for instance type: %s", vmSize));
            Integer executors = getNumOfExecutors(vmSize);
            slave = buildSpotinstSlave(instance.getInstanceId(), vmSize, String.valueOf(executors));
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

    private synchronized void removeFromPending() {

        if (pendingInstances.size() > 0) {
            String key = pendingInstances.entrySet().iterator().next().getKey();
            pendingInstances.remove(key);
        }
    }

    private void addToGroupPending(ProvisionRequest request) {
        for (int i = 0; i < request.getExecutors(); i++) {
            String          key             = UUID.randomUUID().toString();
            PendingInstance pendingInstance = new PendingInstance();
            pendingInstance.setCreatedAt(new Date());
            pendingInstance.setNumOfExecutors(1);
            pendingInstance.setRequestedLabel(request.getLabel());
            pendingInstance.setStatus(PendingInstance.StatusEnum.PENDING);

            pendingInstances.put(key, pendingInstance);
        }

    }
    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Spot Azure LPVM (old)";
        }
    }
    //endregion
}
