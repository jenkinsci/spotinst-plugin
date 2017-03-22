package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.Constants;
import hudson.plugins.spotinst.common.GcpMachineType;
import hudson.plugins.spotinst.common.TimeUtils;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstance;
import hudson.plugins.spotinst.model.scale.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResult;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpSpotinstCloud.class);
    //endregion

    //region Constructors
    @DataBoundConstructor
    public GcpSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            SlaveUsageEnum usage, String tunnel, String vmargs) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, vmargs);
    }
    //endregion

    //region Private Methods
    private SpotinstSlave handleNewGcpInstance(String instanceName, String machineType, String label) {
        Integer executors = GcpMachineType.fromValue(machineType).getExecutors();
        addToPending(instanceName, executors, PendingInstance.StatusEnum.INSTANCE_INITIATING, label);
        SpotinstSlave retVal = buildSpotinstSlave(instanceName, machineType, String.valueOf(executors));
        return retVal;
    }

    //region Recover
    private void addNewGcpSlaveInstances(List<GcpElastigroupInstance> gcpElastigroupInstances) {
        if (gcpElastigroupInstances.size() > 0) {
            for (GcpElastigroupInstance instance : gcpElastigroupInstances) {
                boolean isSlaveExist = isSlaveExistForGcpInstance(instance);
                if (isSlaveExist == false) {
                    LOGGER.info("Instance: {} of group: {} doesn't have slave , adding new one",
                                JsonMapper.toJson(instance), groupId);
                    addSpotinstGcpSlave(instance);
                }
            }
        }
        else {
            LOGGER.info("There are no new instances to add for group: {}", groupId);
        }
    }

    private void removeOldGcpSlaveInstances(List<GcpElastigroupInstance> gcpElastigroupInstances) {

        List<SpotinstSlave> allGroupsSlaves = loadSlaves();
        LOGGER.info("There are {} slaves for group: {}", allGroupsSlaves.size(), groupId);
        List<String> instanceNames = getGcpInstanceNames(gcpElastigroupInstances);
        for (SpotinstSlave slave : allGroupsSlaves) {
            String  slaveInstanceId      = slave.getInstanceId();
            boolean isInstanceInitiating = isInstancePending(slaveInstanceId);

            if (slaveInstanceId != null &&
                instanceNames.contains(slaveInstanceId) == false &&
                isInstanceInitiating == false) {
                LOGGER.info("Slave for instance: {} is no longer running in group: {}, removing it", slaveInstanceId,
                            groupId);
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

    private List<String> getGcpInstanceNames(List<GcpElastigroupInstance> gcpElastigroupInstances) {
        List<String> retVal = new LinkedList<>();
        for (GcpElastigroupInstance gcpElastigroupInstance : gcpElastigroupInstances) {
            retVal.add(gcpElastigroupInstance.getInstanceName());
        }
        return retVal;
    }

    private boolean isSlaveExistForGcpInstance(GcpElastigroupInstance instance) {
        boolean retVal = false;

        Node node = Jenkins.getInstance().getNode(instance.getInstanceName());
        if (node != null) {
            retVal = true;
        }
        return retVal;
    }

    private void addSpotinstGcpSlave(GcpElastigroupInstance instance) {
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

    //region Monitor
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
        GcpScaleUpResult    scaleUpResult = SpotinstApi.getInstance().gcpScaleUp(groupId, request.getExecutors());

        if (scaleUpResult != null) {
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
            LOGGER.error("Failed to scale up Elastigroup: " + groupId);
        }

        return retVal;
    }

    @Override
    public boolean detachInstance(String instanceId) {
        boolean retVal = SpotinstApi.getInstance().gcpDetachInstance(groupId, instanceId);
        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return "gcp/gce";
    }

    @Override
    public void recoverInstances() {
        List<GcpElastigroupInstance> gcpElastigroupInstances =
                SpotinstApi.getInstance().getGcpElastigroupInstances(groupId);
        if (gcpElastigroupInstances != null) {
            LOGGER.info("There are {} instances in group {}", gcpElastigroupInstances.size(), groupId);
            addNewGcpSlaveInstances(gcpElastigroupInstances);
            removeOldGcpSlaveInstances(gcpElastigroupInstances);
        }
        else {
            LOGGER.error("can't recover group {}", groupId);
        }

    }

    @Override
    public void monitorInstances() {
        LOGGER.info(String.format("Monitoring group %s instances", groupId));
        if (pendingInstances.size() > 0) {
            for (String id : pendingInstances.keySet()) {
                PendingInstance pendingInstance = pendingInstances.get(id);
                handleInitiatingInstance(pendingInstance);
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
            return "Spotinst GCP Elastigroup";
        }
    }
    //endregion
}
