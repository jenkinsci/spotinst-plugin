package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.AzureVmSizeEnum;
import hudson.plugins.spotinst.model.elastigroup.azure.AzureElastigroupInstance;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
                              SlaveUsageEnum usage, String tunnel, String vmargs) {
        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, vmargs);
    }
    //endregion

    //region Private Methods
    private void addNewSlaveInstances(List<AzureElastigroupInstance> azureElastigroupInstances) {
        if (azureElastigroupInstances.size() > 0) {
            for (AzureElastigroupInstance instance : azureElastigroupInstances) {
                boolean isSlaveExist = isSlaveExistForInstance(instance);
                if (isSlaveExist == false) {
                    LOGGER.info("Instance: {} of group: {} doesn't have slave , adding new one",
                                JsonMapper.toJson(instance), groupId);
                    addSpotinstSlave(instance);
                }
            }
        }
        else {
            LOGGER.info("There are no new instances to add for group: {}", groupId);
        }
    }

    private void removeOldSlaveInstances(List<AzureElastigroupInstance> azureElastigroupInstances) {
        List<SpotinstSlave> allGroupsSlaves = loadSlaves();
        if (allGroupsSlaves != null) {
            List<String> groupInstanceIds = getGroupInstanceIds(azureElastigroupInstances);
            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                if (slaveInstanceId != null && groupInstanceIds.contains(slaveInstanceId) == false) {
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

    private List<String> getGroupInstanceIds(List<AzureElastigroupInstance> azureElastigroupInstances) {
        List<String> groupInstanceAndSpotRequestIds = new LinkedList<>();
        for (AzureElastigroupInstance instance : azureElastigroupInstances) {
            if (instance.getId() != null) {
                groupInstanceAndSpotRequestIds.add(instance.getId());
            }
        }
        return groupInstanceAndSpotRequestIds;
    }

    private boolean isSlaveExistForInstance(AzureElastigroupInstance instance) {
        boolean retVal = false;

        Node node = Jenkins.getInstance().getNode(instance.getId());
        if (node != null) {
            retVal = true;
        }
        return retVal;
    }

    private void addSpotinstSlave(AzureElastigroupInstance instance) {
        SpotinstSlave slave = null;
        if (instance.getId() != null) {
            slave = handleNewAzureInstance(instance.getId(), instance.getVmSize(), null);
        }

        if (slave != null) {
            try {
                Jenkins.getInstance().addNode(slave);

                PendingInstance pendingInstance = pendingInstances.get(groupId);
                if (pendingInstance != null) {
                    Integer currentExecutors = pendingInstance.getNumOfExecutors();
                    pendingInstance.setNumOfExecutors(currentExecutors - 1);
                    pendingInstances.put(groupId, pendingInstance);
                }
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
            }
        }
    }

    private SpotinstSlave handleNewAzureInstance(String instanceId, String vmSize, String label) {
        Integer executors = getNumOfExecutors(vmSize);
        addToPending(instanceId, executors, PendingInstance.StatusEnum.INSTANCE_INITIATING, label);
        SpotinstSlave retVal = buildSpotinstSlave(instanceId, vmSize, String.valueOf(executors));

        return retVal;
    }

    private Integer getNumOfExecutors(String vmSize) {
        LOGGER.info("Determining the # of executors for instance type: " + vmSize);
        Integer         retVal     = 1;
        AzureVmSizeEnum vmSizeEnum = AzureVmSizeEnum.fromValue(vmSize);
        if (vmSizeEnum != null) {
            retVal = vmSizeEnum.getExecutors();
        }
        return retVal;
    }

    private void addToPending(ProvisionRequest request) {

        String          key             = groupId;
        PendingInstance pendingInstance = pendingInstances.get(key);
        if (pendingInstance == null) {
            pendingInstance = new PendingInstance();
            pendingInstance.setId(key);
            pendingInstance.setCreatedAt(new Date());
            pendingInstance.setNumOfExecutors(request.getExecutors());
            pendingInstance.setRequestedLabel(request.getLabel());
            pendingInstance.setStatus(PendingInstance.StatusEnum.SPOT_PENDING);
        }
        else {
            Integer currentExecutors = pendingInstance.getNumOfExecutors();
            pendingInstance.setNumOfExecutors(currentExecutors + request.getExecutors());
            pendingInstance.setCreatedAt(new Date());
        }
        pendingInstances.put(key, pendingInstance);
    }
    //endregion

    //region Overrides
    @Override
    List<SpotinstSlave> scaleUp(ProvisionRequest request) {
        List<SpotinstSlave> retVal    = new LinkedList<>();
        boolean             isSucceed = SpotinstApi.getInstance().azureScaleUp(groupId, request.getExecutors());
        if (isSucceed) {
            LOGGER.info("Scale succeed");
            addToPending(request);
        }
        return retVal;
    }

    @Override
    public boolean detachInstance(String instanceId) {
        boolean retVal = SpotinstApi.getInstance().azureDetachInstance(groupId, instanceId);
        return retVal;
    }

    @Override
    public String getCloudUrl() {
        return "/azure/compute";
    }

    @Override
    public void recoverInstances() {

    }

    @Override
    public void monitorInstances() {
        List<AzureElastigroupInstance> azureElastigroupInstances =
                SpotinstApi.getInstance().getAzureElastigroupInstances(groupId);
        if (azureElastigroupInstances != null) {
            LOGGER.info("There are {} instances in group {}", azureElastigroupInstances.size(), groupId);
            addNewSlaveInstances(azureElastigroupInstances);
            removeOldSlaveInstances(azureElastigroupInstances);
        }
        else {
            LOGGER.error("can't recover group {}", groupId);
        }
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
