package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.model.gcp.GcpMachineType;
import hudson.plugins.spotinst.common.TimeUtils;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;
import hudson.plugins.spotinst.model.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.repos.IGcpGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ohadmuchnik on 20/03/2017.
 */
public class GcpSpotinstCloud extends BaseSpotinstCloud {

    //region Members
    private static final Logger LOGGER    = LoggerFactory.getLogger(GcpSpotinstCloud.class);
    //endregion

    //region Constructors
    @DataBoundConstructor
    public GcpSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            SlaveUsageEnum usage, String tunnel, String vmargs) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, vmargs);
    }
    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave>           retVal          = new LinkedList<>();
        IGcpGroupRepo                 gcpGroupRepo    = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<GcpScaleUpResult> scaleUpResponse = gcpGroupRepo.scaleUp(groupId, request.getExecutors());

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
    public Boolean detachInstance(String instanceId) {
        Boolean              retVal                 = false;
        IGcpGroupRepo        gcpGroupRepo           = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<Boolean> detachInstanceResponse = gcpGroupRepo.detachInstance(groupId, instanceId);

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
        IGcpGroupRepo                       gcpGroupRepo      = RepoManager.getInstance().getGcpGroupRepo();
        ApiResponse<List<GcpGroupInstance>> instancesResponse = gcpGroupRepo.getGroupInstances(groupId);

        if (instancesResponse.isRequestSucceed()) {

            List<GcpGroupInstance> instances = instancesResponse.getValue();

            LOGGER.info(String.format("There are %s instances in group %s", instances.size(), groupId));

            addNewGcpSlaveInstances(instances);
            removeOldGcpSlaveInstances(instances);

        }
        else {
            LOGGER.error(String.format("Failed to get group %s instances. Errors: %s", groupId,
                                       instancesResponse.getErrors()));
        }
    }

    @Override
    public String getCloudUrl() {
        return "gcp/gce";
    }

    //endregion

    //region Private Methods
    private SpotinstSlave handleNewGcpInstance(String instanceName, String machineType, String label) {
        Integer executors = GcpMachineType.fromValue(machineType).getExecutors();
        addToPending(instanceName, executors, PendingInstance.StatusEnum.INSTANCE_INITIATING, label);
        return buildSpotinstSlave(instanceName, machineType, String.valueOf(executors));
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

        @Override
        public String getDisplayName() {
            return "Spotinst GCP Elastigroup";
        }
    }
    //endregion
}
