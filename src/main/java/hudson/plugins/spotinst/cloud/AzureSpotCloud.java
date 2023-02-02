package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.ConnectionMethodEnum;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.model.azure.AzureGroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;
import hudson.plugins.spotinst.model.azure.AzureVmSizeEnum;
import hudson.plugins.spotinst.repos.IAzureVmGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveInstanceDetails;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.ComputerConnector;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Shibel Karmi Mansour on 08/12/2020.
 */
public class AzureSpotCloud extends BaseSpotinstCloud {
    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSpotCloud.class);
    //endregion

    //region Constructor
    @DataBoundConstructor
    public AzureSpotCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                          SlaveUsageEnum usage, String tunnel, Boolean shouldUseWebsocket,
                          Boolean shouldRetriggerBuilds, String vmargs,
                          EnvironmentVariablesNodeProperty environmentVariables, ToolLocationNodeProperty toolLocations,
                          String accountId, ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                          Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId, connectionMethod,
              computerConnector, shouldUsePrivateIp, globalExecutorOverride);
    }
    //endregion

    // region Override Methods
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal           = new LinkedList<>();
        IAzureVmGroupRepo   azureVmGroupRepo = RepoManager.getInstance().getAzureVmGroupRepo();
        ApiResponse<List<AzureScaleUpResultNewVm>> scaleUpResponse =
                azureVmGroupRepo.scaleUp(groupId, request.getExecutors(), this.accountId);

        if (scaleUpResponse.isRequestSucceed()) {
            List<AzureScaleUpResultNewVm> newVms = scaleUpResponse.getValue();

            if (newVms != null) {
                LOGGER.info(String.format("Scale up group %s succeeded", groupId));
                List<SpotinstSlave> newInstanceSlaves = handleNewVms(newVms, request.getLabel(), groupId);
                retVal.addAll(newInstanceSlaves);
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
        Boolean              retVal           = false;
        IAzureVmGroupRepo    azureVmGroupRepo = RepoManager.getInstance().getAzureVmGroupRepo();
        ApiResponse<Boolean> detachVmResponse = azureVmGroupRepo.detachVM(groupId, instanceId, this.accountId);

        if (detachVmResponse.isRequestSucceed()) {
            LOGGER.info(String.format("Instance %s detached", instanceId));
            retVal = true;
        }
        else {
            LOGGER.error(String.format("Failed to detach instance %s. Errors: %s", instanceId,
                                       detachVmResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return "azure/compute";
    }

    @Override
    protected Integer getPendingThreshold() {
        return Constants.AZURE_PENDING_INSTANCE_TIMEOUT_IN_MINUTES;
    }

    @Override
    protected void internalSyncGroupInstances() {
        IAzureVmGroupRepo               azureVmGroupRepo  = RepoManager.getInstance().getAzureVmGroupRepo();
        ApiResponse<List<AzureGroupVm>> instancesResponse = azureVmGroupRepo.getGroupVms(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AzureGroupVm> vms = instancesResponse.getValue();

            LOGGER.info(String.format("There are %s instances in group %s", vms.size(), groupId));

            Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId = new HashMap<>();

            for (AzureGroupVm vm : vms) {
                SlaveInstanceDetails instanceDetails = SlaveInstanceDetails.build(vm);
                slaveInstancesDetailsByInstanceId.put(instanceDetails.getInstanceId(), instanceDetails);
            }

            this.slaveInstancesDetailsByInstanceId = new HashMap<>(slaveInstancesDetailsByInstanceId);

            removeOldSlaveInstances(vms);
            addNewSlaveInstances(vms);
        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }
    }

    @Override
    public Map<String, String> getInstanceIpsById() {
        Map<String, String> retVal = new HashMap<>();

        IAzureVmGroupRepo               awsGroupRepo      = RepoManager.getInstance().getAzureVmGroupRepo();
        ApiResponse<List<AzureGroupVm>> instancesResponse = awsGroupRepo.getGroupVms(groupId, accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AzureGroupVm> instances = instancesResponse.getValue();

            for (AzureGroupVm instance : instances) {
                if (this.getShouldUsePrivateIp()) {
                    retVal.put(instance.getVmName(), instance.getPrivateIp());
                }
                else {
                    retVal.put(instance.getVmName(), instance.getPublicIp());
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
    protected Integer getDefaultExecutorsNumber(String instanceType) {
        Integer retVal;
        LOGGER.info(String.format("Getting the # of default executors for instance type: %s", instanceType));
        AzureVmSizeEnum enumMember = AzureVmSizeEnum.fromValue(instanceType);

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
    private List<SpotinstSlave> handleNewVms(List<AzureScaleUpResultNewVm> newVms, String label, String groupId) {
        List<SpotinstSlave> retVal = new LinkedList<>();
        LOGGER.info(String.format("%s new instances launched in group %s", newVms.size(), groupId));

        for (AzureScaleUpResultNewVm vm : newVms) {
            SpotinstSlave slave = handleNewVm(vm.getVmName(), vm.getVmSize(), label);
            retVal.add(slave);
        }

        return retVal;
    }

    private SpotinstSlave handleNewVm(String vmName, String vmSize, String label) {
        Integer executors = getNumOfExecutors(vmSize);
        addToPending(vmName, executors, PendingInstance.StatusEnum.PENDING, label);
        SpotinstSlave retVal = buildSpotinstSlave(vmName, vmSize, String.valueOf(executors));
        return retVal;
    }

    private void removeOldSlaveInstances(List<AzureGroupVm> azureGroupVms) {
        List<SpotinstSlave> allGroupsSlaves = getAllSpotinstSlaves();

        if (allGroupsSlaves.size() > 0) {
            List<String> elastigroupVmIds =
                    azureGroupVms.stream().filter(x -> x.getVmName() != null).map(AzureGroupVm::getVmName)
                                 .collect(Collectors.toList());

            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                Boolean slaveIdNotNull  = slaveInstanceId != null;
                Boolean slaveExistsInEg = elastigroupVmIds.contains(slaveInstanceId);

                if (slaveIdNotNull && BooleanUtils.isFalse(slaveExistsInEg)) {
                    LOGGER.info(String.format("Slave for instance: %s is no longer running in group: %s, removing it",
                                              slaveInstanceId, groupId));
                    try {
                        Jenkins.get().removeNode(slave);
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

    private void addNewSlaveInstances(List<AzureGroupVm> azureGroupVms) {

        if (azureGroupVms.size() > 0) {

            for (AzureGroupVm vm : azureGroupVms) {
                Boolean doesSlaveNotExist = BooleanUtils.isFalse(isSlaveExistForInstance(vm));

                if (doesSlaveNotExist) {
                    LOGGER.info(String.format("Instance: %s of group: %s doesn't have slave , adding new one",
                                              JsonMapper.toJson(vm), groupId));
                    addSpotinstSlave(vm);
                }

            }

        }
        else {
            LOGGER.info(String.format("There are no new instances to add for group: %s", groupId));
        }

    }

    private void addSpotinstSlave(AzureGroupVm vm) {
        SpotinstSlave slave = null;

        if (vm.getVmName() != null) {
            String  vmSize    = vm.getVmSize();
            Integer executors = getNumOfExecutors(vmSize);
            slave = buildSpotinstSlave(vm.getVmName(), vmSize, String.valueOf(executors));
        }

        if (slave != null) {
            try {
                Jenkins.get().addNode(slave);
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
            }
        }
    }


    private Boolean isSlaveExistForInstance(AzureGroupVm vm) {
        Boolean retVal = false;
        Node    node   = Jenkins.get().getNode(vm.getVmName());

        if (node != null) {
            retVal = true;
        }

        return retVal;
    }
    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Spot Azure Elastigroup";
        }
    }
    //endregion

}
