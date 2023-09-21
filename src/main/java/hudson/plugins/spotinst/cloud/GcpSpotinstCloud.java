package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.ConnectionMethodEnum;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;
import hudson.plugins.spotinst.model.gcp.GcpMachineType;
import hudson.plugins.spotinst.model.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.repos.IGcpGroupRepo;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ohadmuchnik on 20/03/2017.
 */
public class GcpSpotinstCloud extends BaseSpotinstCloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpSpotinstCloud.class);
    //endregion

    //region Constructors
    @DataBoundConstructor
    public GcpSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            SlaveUsageEnum usage, String tunnel, Boolean shouldUseWebsocket,
                            Boolean shouldRetriggerBuilds, String vmargs,
                            EnvironmentVariablesNodeProperty environmentVariables,
                            ToolLocationNodeProperty toolLocations, String accountId,
                            ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                            Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride,
                            Integer pendingThreshold) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId, connectionMethod,
              computerConnector, shouldUsePrivateIp, globalExecutorOverride, pendingThreshold);
    }
    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal       = new LinkedList<>();
        IGcpGroupRepo       gcpGroupRepo = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<GcpScaleUpResult> scaleUpResponse =
                gcpGroupRepo.scaleUp(groupId, request.getExecutors(), this.accountId);

        if (scaleUpResponse.isRequestSucceed()) {
            GcpScaleUpResult scaleUpResult = scaleUpResponse.getValue();

            if (scaleUpResult != null) {
                LOGGER.info(String.format("Scale up group %s succeeded", groupId));

                if (scaleUpResult.getNewInstances() != null) {
                    for (GcpResultNewInstance newInstance : scaleUpResult.getNewInstances()) {
                        SpotinstSlave newInstanceSlave =
                                handleNewGcpInstance(newInstance.getInstanceName(), newInstance.getMachineType(),
                                                     request.getLabel());
                        retVal.add(newInstanceSlave);
                    }
                }

                if (scaleUpResult.getNewPreemptibles() != null) {
                    for (GcpResultNewInstance newInstance : scaleUpResult.getNewPreemptibles()) {
                        SpotinstSlave newPreemptibleSlave =
                                handleNewGcpInstance(newInstance.getInstanceName(), newInstance.getMachineType(),
                                                     request.getLabel());
                        retVal.add(newPreemptibleSlave);
                    }
                }
            }
            else {
                LOGGER.error(String.format("Failed to scale up group: %s", groupId));
            }
        }
        else {
            LOGGER.error(
                    String.format("Failed to scale up group %s. Errors: %s ", groupId, scaleUpResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    protected BlResponse<Boolean> checkIsStatefulGroup() {
        return new BlResponse<>(false);
    }

    @Override
    protected String getSsiId(String instanceId) {
        return null;//TODO: implement
    }

    @Override
    protected Boolean deallocateInstance(String statefulInstanceId) {
        return false;//TODO: implement
    }

    @Override
    protected Boolean detachInstance(String instanceId) {
        Boolean              retVal                 = false;
        IGcpGroupRepo        gcpGroupRepo           = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<Boolean> detachInstanceResponse = gcpGroupRepo.detachInstance(groupId, instanceId, this.accountId);

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
    protected void syncGroupInstances() {
        IGcpGroupRepo                       gcpGroupRepo      = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<List<GcpGroupInstance>> instancesResponse = gcpGroupRepo.getGroupInstances(groupId, this.accountId);

        if (instancesResponse.isRequestSucceed()) {

            List<GcpGroupInstance> instances = instancesResponse.getValue();

            LOGGER.info(String.format("There are %s instances in group %s", instances.size(), groupId));

            Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId = new HashMap<>();

            for (GcpGroupInstance instance : instances) {
                SlaveInstanceDetails instanceDetails = SlaveInstanceDetails.build(instance);
                slaveInstancesDetailsByInstanceId.put(instanceDetails.getInstanceId(), instanceDetails);
            }

            this.slaveInstancesDetailsByInstanceId = new HashMap<>(slaveInstancesDetailsByInstanceId);

            removeOldGcpSlaveInstances(instances);
            addNewGcpSlaveInstances(instances);
        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }
    }

    @Override
    public Map<String, String> getInstanceIpsById() {
        Map<String, String> retVal = new HashMap<>();

        IGcpGroupRepo                       awsGroupRepo      = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<List<GcpGroupInstance>> instancesResponse = awsGroupRepo.getGroupInstances(groupId, accountId);

        if (instancesResponse.isRequestSucceed()) {
            List<GcpGroupInstance> instances = instancesResponse.getValue();

            for (GcpGroupInstance instance : instances) {
                if (this.getShouldUsePrivateIp()) {
                    retVal.put(instance.getInstanceName(), instance.getPrivateIpAddress());
                }
                else {
                    retVal.put(instance.getInstanceName(), instance.getPublicIpAddress());
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
        return "gcp/gce";
    }

    @Override
    protected Integer getDefaultExecutorsNumber(String instanceType) {
        Integer retVal;
        LOGGER.info(String.format("Getting the # of default executors for instance type: %s", instanceType));
        GcpMachineType enumMember = GcpMachineType.fromValue(instanceType);

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
    private SpotinstSlave handleNewGcpInstance(String instanceName, String machineType, String label) {
        LOGGER.info(String.format("Setting the # of executors for instance type: %s", machineType));
        Integer executors = getNumOfExecutors(machineType);
        addToPending(instanceName, executors, PendingInstance.StatusEnum.INSTANCE_INITIATING, label);
        SpotinstSlave retVal = buildSpotinstSlave(instanceName, machineType, String.valueOf(executors));

        return retVal;
    }

    private void addNewGcpSlaveInstances(List<GcpGroupInstance> gcpGroupInstances) {
        if (gcpGroupInstances.size() > 0) {

            for (GcpGroupInstance instance : gcpGroupInstances) {
                Boolean isSlaveExist = isSlaveExistForGcpInstance(instance);

                if (isSlaveExist == false) {
                    LOGGER.info(String.format("Instance: %s of group: %s doesn't have slave , adding new one",
                                              JsonMapper.toJson(instance), groupId));
                    addSpotinstGcpSlave(instance);
                }
            }
        }
        else {
            LOGGER.info(String.format("There are no new instances to add for group: %s", groupId));
        }
    }

    private void removeOldGcpSlaveInstances(List<GcpGroupInstance> gcpGroupInstances) {
        List<SpotinstSlave> allGroupsSlaves = getAllSpotinstSlaves();

        LOGGER.info(String.format("There are %s slaves for group: %s", allGroupsSlaves.size(), groupId));

        List<String> instanceNames = getGcpInstanceNames(gcpGroupInstances);

        for (SpotinstSlave slave : allGroupsSlaves) {
            String  slaveInstanceId      = slave.getInstanceId();
            Boolean isInstanceInitiating = isInstancePending(slaveInstanceId);

            if (slaveInstanceId != null && instanceNames.contains(slaveInstanceId) == false &&
                isInstanceInitiating == false) {

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

    private List<String> getGcpInstanceNames(List<GcpGroupInstance> gcpGroupInstances) {
        List<String> retVal = new LinkedList<>();

        for (GcpGroupInstance gcpGroupInstance : gcpGroupInstances) {
            retVal.add(gcpGroupInstance.getInstanceName());
        }

        return retVal;
    }

    private Boolean isSlaveExistForGcpInstance(GcpGroupInstance instance) {
        Boolean retVal = false;

        Node node = Jenkins.getInstance().getNode(instance.getInstanceName());

        if (node != null) {
            retVal = true;
        }

        return retVal;
    }

    private void addSpotinstGcpSlave(GcpGroupInstance instance) {
        if (instance.getInstanceName() != null) {
            SpotinstSlave slave = handleNewGcpInstance(instance.getInstanceName(), instance.getMachineType(), null);

            try {
                Jenkins.getInstance().addNode(slave);
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
            }
        }
    }
    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Spot GCP Elastigroup";
        }
    }
    //endregion
}
