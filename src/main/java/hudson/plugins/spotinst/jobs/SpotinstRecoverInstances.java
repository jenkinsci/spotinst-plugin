package hudson.plugins.spotinst.jobs;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.spotinst.SpotinstCloud;
import hudson.plugins.spotinst.SpotinstSlave;
import hudson.plugins.spotinst.common.CloudProviderEnum;
import hudson.plugins.spotinst.common.ContextInstance;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import hudson.plugins.spotinst.elastigroup.AwsElastigroupInstance;
import hudson.plugins.spotinst.elastigroup.GcpElastigroupInstance;
import hudson.plugins.spotinst.rest.JsonMapper;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
@Extension
public class SpotinstRecoverInstances extends AsyncPeriodicWork {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstRecoverInstances.class);
    final long recurrencePeriod;
    private Map<String, SpotinstCloud> clouds;
    private Map<String, List<SpotinstSlave>> slavesForGroups;
    //endregion

    //region Constructor
    public SpotinstRecoverInstances() {
        super("Recover Instances");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(5);
    }
    //endregion

    //region Private Methods

    //region AWS
    private void handleGroup(String groupId) {
        List<AwsElastigroupInstance> elastigroupInstances = SpotinstGateway.getAwsElastigroupInstances(groupId);
        if (elastigroupInstances != null) {
            LOGGER.info("There are {} instances in group {}", elastigroupInstances.size(), groupId);
            addNewSlaveInstances(groupId, elastigroupInstances);
            removeOldSlaveInstances(groupId, elastigroupInstances);
        } else {
            LOGGER.error("can't recover group {}", groupId);
        }
    }

    private void addNewSlaveInstances(String groupId, List<AwsElastigroupInstance> elastigroupInstances) {
        if (elastigroupInstances.size() > 0) {
            for (AwsElastigroupInstance instance : elastigroupInstances) {
                boolean isSlaveExist = isSlaveExistForInstance(instance);
                if (isSlaveExist == false) {
                    handleNewInstance(groupId, instance);
                }
            }
        } else {
            LOGGER.info("There are no new instances to add for group: {}", groupId);
        }
    }

    private void handleNewInstance(String groupId, AwsElastigroupInstance instance) {
        LOGGER.info("Instance: {} of group: {} doesn't have slave , adding new one", JsonMapper.toJson(instance), groupId);
        SpotinstSlave slave = buildSpotinstSlave(instance, groupId);
        if (slave != null) {
            LOGGER.info("Adding slave to group: {}", groupId);
            try {
                Jenkins.getInstance().addNode(slave);
                LOGGER.info("Slave added successfully to group: {}", groupId);
            } catch (IOException e) {
                LOGGER.error("Failed to add slave to group: {}", groupId, e);
            }
        }
    }

    private void removeOldSlaveInstances(String groupId, List<AwsElastigroupInstance> elastigroupInstances) {

        List<SpotinstSlave> allGroupsSlaves = slavesForGroups.get(groupId);
        if (allGroupsSlaves != null) {
            LOGGER.info("There are {} slaves for group: {}", allGroupsSlaves.size(), groupId);
            List<String> groupInstanceAndSpotRequestIds = getGroupInstanceAndSpotIds(elastigroupInstances);
            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();

                if (slaveInstanceId != null &&
                        groupInstanceAndSpotRequestIds.contains(slaveInstanceId) == false) {
                    LOGGER.info("Slave for instance: {} is no longer running in group: {}, removing it", slaveInstanceId, groupId);
                    try {
                        Jenkins.getInstance().removeNode(slave);
                        LOGGER.info("Slave: {} removed successfully", slaveInstanceId);
                    } catch (IOException e) {
                        LOGGER.error("Failed to remove slave from group: {}", groupId, e);
                    }
                }
            }
        } else {
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

        Node node = Jenkins.getInstance().getNode(instance.getInstanceId());
        if (node != null) {
            retVal = true;
        } else {
            node = Jenkins.getInstance().getNode(instance.getSpotInstanceRequestId());
            if (node != null) {
                retVal = true;
            }
        }
        return retVal;
    }

    private SpotinstSlave buildSpotinstSlave(AwsElastigroupInstance instance,
                                             String groupId) {
        SpotinstSlave slave = null;
        SpotinstCloud cloud = clouds.get(groupId);
        if (cloud != null) {
            if (instance.getInstanceId() != null) {
                slave = cloud.buildInstanceSlave(instance.getInstanceType(), instance.getInstanceId());
            } else if (instance.getSpotInstanceRequestId() != null) {
                slave = cloud.buildSpotSlave(instance.getInstanceType(), instance.getSpotInstanceRequestId());
            }
        }

        return slave;
    }

    //endregion

    //region GCP
    private void handleGcpGroup(String groupId) {
        List<GcpElastigroupInstance> gcpElastigroupInstances = SpotinstGateway.getGcpElastigroupInstances(groupId);
        if (gcpElastigroupInstances != null) {
            LOGGER.info("There are {} instances in group {}", gcpElastigroupInstances.size(), groupId);
            addNewGcpSlaveInstances(groupId, gcpElastigroupInstances);
            removeOldGcpSlaveInstances(groupId, gcpElastigroupInstances);
        } else {
            LOGGER.error("can't recover group {}", groupId);
        }
    }

    private void addNewGcpSlaveInstances(String groupId, List<GcpElastigroupInstance> gcpElastigroupInstances) {
        if (gcpElastigroupInstances.size() > 0) {
            for (GcpElastigroupInstance instance : gcpElastigroupInstances) {
                boolean isSlaveExist = isSlaveExistForGcpInstance(instance);
                if (isSlaveExist == false) {
                    handleNewGcpInstance(groupId, instance);
                }
            }
        } else {
            LOGGER.info("There are no new instances to add for group: {}", groupId);
        }
    }

    private void handleNewGcpInstance(String groupId, GcpElastigroupInstance instance) {
        LOGGER.info("Instance: {} of group: {} doesn't have slave , adding new one", JsonMapper.toJson(instance), groupId);
        SpotinstSlave slave = buildSpotinstGcpSlave(instance, groupId);
        if (slave != null) {
            LOGGER.info("Adding slave to group: {}", groupId);
            try {
                Jenkins.getInstance().addNode(slave);
                LOGGER.info("Slave added successfully to group: {}", groupId);
            } catch (IOException e) {
                LOGGER.error("Failed to add slave to group: {}", groupId, e);
            }
        }
    }

    private void removeOldGcpSlaveInstances(String groupId, List<GcpElastigroupInstance> gcpElastigroupInstances) {

        List<SpotinstSlave> allGroupsSlaves = slavesForGroups.get(groupId);
        if (allGroupsSlaves != null) {
            LOGGER.info("There are {} slaves for group: {}", allGroupsSlaves.size(), groupId);
            List<String> instanceNames = getGcpInstanceNames(gcpElastigroupInstances);
            for (SpotinstSlave slave : allGroupsSlaves) {
                String slaveInstanceId = slave.getInstanceId();
                boolean isInstanceInitiating = isGcpInstanceInitiating(groupId, slaveInstanceId);

                if (slaveInstanceId != null &&
                        instanceNames.contains(slaveInstanceId) == false &&
                        isInstanceInitiating == false) {
                    LOGGER.info("Slave for instance: {} is no longer running in group: {}, removing it", slaveInstanceId, groupId);
                    try {
                        Jenkins.getInstance().removeNode(slave);
                        LOGGER.info("Slave: {} removed successfully", slaveInstanceId);
                    } catch (IOException e) {
                        LOGGER.error("Failed to remove slave from group: {}", groupId, e);
                    }
                }
            }
        } else {
            LOGGER.info("There are no slaves for group: {}", groupId);
        }
    }

    private boolean isGcpInstanceInitiating(String groupId, String instanceName) {
        boolean retVal = false;
        Map<String, ContextInstance> initiating = SpotinstContext.getInstance().getSpotRequestWaiting().get(groupId);
        if (initiating != null &&
                initiating.containsKey(instanceName)) {
            retVal = true;
        }
        return retVal;
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

    private SpotinstSlave buildSpotinstGcpSlave(GcpElastigroupInstance instance,
                                                String groupId) {
        SpotinstSlave slave = null;
        SpotinstCloud cloud = clouds.get(groupId);
        if (cloud != null) {
            if (instance.getInstanceName() != null) {
                slave = cloud.buildGcpInstanceSlave(instance.getMachineType(), instance.getInstanceName());
            }
        }

        return slave;
    }

    //endregion

    private void loadClouds() {
        this.clouds = new HashMap<>();
        List<Cloud> cloudList = Jenkins.getInstance().clouds;
        if (cloudList != null &&
                cloudList.size() > 0) {
            for (Cloud cloud : cloudList) {
                if (cloud instanceof SpotinstCloud) {
                    SpotinstCloud spotinstCloud = (SpotinstCloud) cloud;
                    clouds.put(spotinstCloud.getGroupId(), spotinstCloud);
                }
            }
        }
    }

    private void loadSlaves() {
        this.slavesForGroups = new HashMap<>();
        List<Node> allNodes = Jenkins.getInstance().getNodes();
        if (allNodes != null) {
            for (Node node : allNodes) {
                if (node instanceof SpotinstSlave) {
                    SpotinstSlave slave = (SpotinstSlave) node;
                    if (slave.getElastigroupId() != null) {
                        String groupId = slave.getElastigroupId();
                        if (slavesForGroups.containsKey(groupId)) {
                            slavesForGroups.get(groupId).add(slave);
                        } else {
                            List<SpotinstSlave> slaves = new LinkedList<>();
                            slaves.add(slave);
                            slavesForGroups.put(groupId, slaves);
                        }
                    }
                }
            }
        }
    }
    //endregion

    //region Public Methods
    @Override
    protected void execute(TaskListener taskListener) {
        loadClouds();
        loadSlaves();
        if (clouds.keySet().size() > 0) {
            for (String groupId : clouds.keySet()) {
                if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
                    handleGcpGroup(groupId);
                } else {
                    handleGroup(groupId);
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
