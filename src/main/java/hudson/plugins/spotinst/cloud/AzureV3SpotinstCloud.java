package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.model.azure.AzureV3GroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleResultNewVm;
import hudson.plugins.spotinst.model.azure.AzureV3VmSizeEnum;
import hudson.plugins.spotinst.repos.IAzureV3GroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveInstanceDetails;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Shibel Karmi Mansour on 08/12/2020.
 */
public class AzureV3SpotinstCloud extends BaseSpotinstCloud {
    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureV3SpotinstCloud.class);
    //endregion


    //region Constructor
    @DataBoundConstructor
    public AzureV3SpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                                SlaveUsageEnum usage, String tunnel, Boolean shouldUseWebsocket,
                                Boolean shouldRetriggerBuilds, String vmargs,
                                EnvironmentVariablesNodeProperty environmentVariables,
                                ToolLocationNodeProperty toolLocations, String accountId) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId);
    }
    //endregion

    // region Override Methods
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        IAzureV3GroupRepo azureV3GroupRepo = RepoManager.getInstance().getAzureV3GroupRepo();
        ApiResponse<List<AzureScaleResultNewVm>> scaleUpResponse =
                azureV3GroupRepo.scaleUp(groupId, request.getExecutors(), this.accountId);

        if (scaleUpResponse.isRequestSucceed()) {
            List<AzureScaleResultNewVm> newVms = scaleUpResponse.getValue();

            Boolean isNewVmsPresent = newVms != null;

            if (isNewVmsPresent) {
                LOGGER.info(String.format("Scale up group %s succeeded", groupId));
                List<SpotinstSlave> newInstanceSlaves = handleNewVms(newVms, request.getLabel());
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
        IAzureV3GroupRepo    azureV3GroupRepo = RepoManager.getInstance().getAzureV3GroupRepo();
        ApiResponse<Boolean> detachVmResponse = azureV3GroupRepo.detachVM(groupId, instanceId, this.accountId);

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
    public void syncGroupInstances() {
        IAzureV3GroupRepo                 azureV3GroupRepo  = RepoManager.getInstance().getAzureV3GroupRepo();
        ApiResponse<List<AzureV3GroupVm>> instancesResponse = azureV3GroupRepo.getGroupVms(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<AzureV3GroupVm> vms = instancesResponse.getValue();

            LOGGER.info(String.format("There are %s instances in group %s", vms.size(), groupId));

            addNewSlaveInstances(vms);
            removeOldSlaveInstances(vms);

            Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId = new HashMap<>();

            for (AzureV3GroupVm vm : vms) {
                SlaveInstanceDetails instanceDetails = SlaveInstanceDetails.build(vm);
                slaveInstancesDetailsByInstanceId.put(instanceDetails.getInstanceId(), instanceDetails);
            }

            this.slaveInstancesDetailsByInstanceId = new HashMap<>(slaveInstancesDetailsByInstanceId);
        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }
    }
    //endregion

    //region Private Methods
    private List<SpotinstSlave> handleNewVms(List<AzureScaleResultNewVm> newVms, String label) {
        List<SpotinstSlave> retVal = new LinkedList<>();

        LOGGER.info(String.format("%s new instances launched", newVms.size()));


        for (AzureScaleResultNewVm vm : newVms) {
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

    private void removeOldSlaveInstances(List<AzureV3GroupVm> azureV3GroupVms) {
        List<SpotinstSlave> allGroupsSlaves = getAllSpotinstSlaves();

        if (allGroupsSlaves.size() > 0) {
            List<String> elastigroupVmIds =
                    azureV3GroupVms.stream().filter(x -> x.getVmName() != null).map(AzureV3GroupVm::getVmName)
                                   .collect(Collectors.toList());

            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                Boolean slaveIdNotNull       = slaveInstanceId != null;
                Boolean slaveExistsInEg      = elastigroupVmIds.contains(slaveInstanceId);

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
            }
        }
        else {
            LOGGER.info(String.format("There are no slaves for group: %s", groupId));
        }
    }

    private void addNewSlaveInstances(List<AzureV3GroupVm> azureV3GroupVms) {

        if (azureV3GroupVms.size() > 0) {

            for (AzureV3GroupVm vm : azureV3GroupVms) {
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

    private void addSpotinstSlave(AzureV3GroupVm vm) {
        SpotinstSlave slave = null;

        if (vm.getVmName() != null) {
            Integer executors = getNumOfExecutors(vm.getVmSize());
            slave = buildSpotinstSlave(vm.getVmName(), vm.getVmSize(), String.valueOf(executors));
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

    private Integer getNumOfExecutors(String vmSize) {
        LOGGER.info(String.format("Determining # of executors for instance type: %s", vmSize));

        Integer           retVal;
        Integer           defaultExecutors = 1;
        AzureV3VmSizeEnum vmSizeEnum       = AzureV3VmSizeEnum.fromValue(vmSize);

        if (vmSizeEnum != null) {
            retVal = vmSizeEnum.getExecutors();
        }
        else {
            retVal = defaultExecutors;
            LOGGER.info(String.format(
                    "Failed to determine # of executors for instance type %s, defaulting to %s executor(s) ", vmSize,
                    defaultExecutors));
        }

        return retVal;
    }

    private Boolean isSlaveExistForInstance(AzureV3GroupVm vm) {
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

        @Override
        public String getDisplayName() {
            return "Spotinst Azure Elastigroup";
        }
    }
    //endregion

}
