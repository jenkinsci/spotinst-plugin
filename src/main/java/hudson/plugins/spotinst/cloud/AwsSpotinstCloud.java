package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.ConnectionMethodEnum;
import hudson.plugins.spotinst.common.SpotAwsInstanceTypesHelper;
import hudson.plugins.spotinst.common.stateful.StatefulInstanceStateEnum;
import hudson.plugins.spotinst.model.aws.*;
import hudson.plugins.spotinst.model.aws.stateful.AwsStatefulInstance;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.repos.IAwsGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveInstanceDetails;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.ComputerConnector;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private              List<String>                           invalidInstanceTypes;
    private              Map<String, AwsStatefulInstance>       ssiByInstanceId;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public AwsSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                            List<? extends SpotinstInstanceWeight> executorsForTypes, SlaveUsageEnum usage,
                            String tunnel, Boolean shouldUseWebsocket, Boolean shouldRetriggerBuilds, String vmargs,
                            EnvironmentVariablesNodeProperty environmentVariables,
                            ToolLocationNodeProperty toolLocations, String accountId,
                            ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                            Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride,
                            Integer pendingThreshold) {

        super(groupId, labelString, idleTerminationMinutes, workspaceDir, usage, tunnel, shouldUseWebsocket,
              shouldRetriggerBuilds, vmargs, environmentVariables, toolLocations, accountId, connectionMethod,
              computerConnector, shouldUsePrivateIp, globalExecutorOverride, pendingThreshold);

        this.executorsForTypes = new LinkedList<>();

        if (executorsForTypes != null) {
            this.executorsForTypes = executorsForTypes;
        }

        initExecutorsByInstanceType();
    }

    //endregion

    //region Overrides
    @Override
    public String getElastigroupName(){
        return getElastigroupName(groupId, accountId);
    }

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
    protected String getSsiId(String instanceId) {
        String              retVal           = null;
        AwsStatefulInstance statefulInstance = getStatefulInstance(instanceId);

        if (statefulInstance != null) {
            retVal = statefulInstance.getId();
        }

        return retVal;
    }

    private AwsStatefulInstance getStatefulInstance(String instanceId) {
        AwsStatefulInstance retVal             = null;
        boolean             isInstanceStateful = ssiByInstanceId != null && ssiByInstanceId.containsKey(instanceId);

        if (isInstanceStateful) {
            retVal = ssiByInstanceId.get(instanceId);
        }

        return retVal;
    }

    @Override
    public Boolean deallocateInstance(String statefulInstanceId) {
        boolean retVal = false;

        IAwsGroupRepo awsGroupRepo = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<Boolean> detachInstanceResponse =
                awsGroupRepo.deallocateInstance(groupId, statefulInstanceId, accountId);

        if (detachInstanceResponse.isRequestSucceed()) {
            LOGGER.info(String.format("Stateful Instance %s deallocated", statefulInstanceId));
            retVal = true;
        }
        else {
            LOGGER.error(String.format("Failed to deallocate instance %s. Errors: %s", statefulInstanceId,
                                       detachInstanceResponse.getErrors()));
        }

        return retVal;
    }

    @Override
    protected Boolean detachInstance(String instanceId) {
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
    protected void syncGroupInstances() {
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

            if (isStatefulGroup()) {
                syncGroupStatefulInstances();
            }

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
        Map<String, String>                 retVal            = new HashMap<>();
        IAwsGroupRepo                       awsGroupRepo      = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<List<AwsGroupInstance>> instancesResponse = awsGroupRepo.getGroupInstances(groupId, accountId);

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
    private static String getElastigroupName(String groupId, String accountId){
        String retVal = null;

        if(StringUtils.isNotEmpty(groupId)) {
            IAwsGroupRepo         awsGroupRepo  = RepoManager.getInstance().getAwsGroupRepo();
            ApiResponse<AwsGroup> groupResponse = awsGroupRepo.getGroup(groupId, accountId);

            if (groupResponse.isRequestSucceed() && groupResponse.getValue() != null) {
                AwsGroup group = groupResponse.getValue();
                retVal = group.getName();
            }
            else {
                LOGGER.error("Failed to get group {}. Errors: {}", groupId, groupResponse.getErrors());
            }
        }

        return retVal;
    }

    @Override
    protected int getOverriddenNumberOfExecutors(String instanceType) {
        Integer retVal;

        if (executorsByInstanceType == null) {
            initExecutorsByInstanceType();
        }

        if (executorsByInstanceType.containsKey(instanceType)) {
            retVal = executorsByInstanceType.get(instanceType);
            LOGGER.info(String.format("We have a weight definition for this type of %s", retVal));
        }
        else {
            retVal = NO_OVERRIDDEN_NUM_OF_EXECUTORS;
        }

        return retVal;
    }

    @Override
    protected BlResponse<Boolean> checkIsStatefulGroup() {
        BlResponse<Boolean>   retVal;
        IAwsGroupRepo         awsGroupRepo  = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<AwsGroup> groupResponse = awsGroupRepo.getGroup(groupId, this.accountId);

        if (groupResponse.isRequestSucceed()) {
            Boolean          result        = false;
            AwsGroup         awsGroup      = groupResponse.getValue();
            AwsGroupStrategy groupStrategy = awsGroup.getStrategy();

            if (groupStrategy != null) {
                AwsGroupPersistence groupPersistence = groupStrategy.getPersistence();

                if (groupPersistence != null) {
                    result = BooleanUtils.isTrue(groupPersistence.getShouldPersistPrivateIp()) ||
                             BooleanUtils.isTrue(groupPersistence.getShouldPersistBlockDevices()) ||
                             BooleanUtils.isTrue(groupPersistence.getShouldPersistRootDevice());
                }
            }

            retVal = new BlResponse<>(result);
        }
        else {
            LOGGER.error(String.format("Failed to get group %s. Errors: %s", groupId, groupResponse.getErrors()));
            retVal = new BlResponse<>(false);
        }

        return retVal;
    }

    private void syncGroupStatefulInstances() {
        IAwsGroupRepo awsGroupRepo = RepoManager.getInstance().getAwsGroupRepo();
        ApiResponse<List<AwsStatefulInstance>> statefulInstancesResponse =
                awsGroupRepo.getStatefulInstances(groupId, this.accountId);

        if (statefulInstancesResponse.isRequestSucceed()) {
            List<AwsStatefulInstance> statefulInstances = statefulInstancesResponse.getValue();
            this.ssiByInstanceId = statefulInstances.stream().filter(statefulInstance -> StringUtils.isNotEmpty(
                    statefulInstance.getInstanceId())).collect(
                    Collectors.toMap(AwsStatefulInstance::getInstanceId, statefulInstance -> statefulInstance));
            LOGGER.info("found {} running stateful instances for group {}", ssiByInstanceId.size(), groupId);
        }
        else {
            LOGGER.error(String.format("Failed to get group %s stateful instances. Errors: %s", groupId,
                                       statefulInstancesResponse.getErrors()));
        }
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
                boolean shouldAddSlaveInstance = shouldAddNewSlaveInstance(instance);

                if (shouldAddSlaveInstance) {
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

    private Boolean shouldAddNewSlaveInstance(AwsGroupInstance instance) {
        boolean retVal;
        boolean isSlaveExist = isSlaveExistForInstance(instance);

        if (isSlaveExist) {
            retVal = false;
        }
        else {
            if (isStatefulGroup()) {
                AwsStatefulInstance statefulInstance = getStatefulInstance(instance.getInstanceId());
                boolean isStatefulInstanceReadyForUse = statefulInstance != null &&
                                                        Objects.equals(statefulInstance.getState(),
                                                                       StatefulInstanceStateEnum.ACTIVE);
                retVal = isStatefulInstanceReadyForUse;
            }
            else {
                retVal = true;
            }
        }

        return retVal;
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
        boolean retVal = false;
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
        this.invalidInstanceTypes = new LinkedList<>();

        if (this.executorsForTypes != null) {
            for (SpotinstInstanceWeight instance : this.executorsForTypes) {
                if (instance.getExecutors() != null) {
                    Integer executors = instance.getExecutors();
                    String  type      = instance.getAwsInstanceTypeFromAPIInput();
                    this.executorsByInstanceType.put(type, executors);

                    if (instance.getIsValid() == false) {
                        LOGGER.error(String.format("Invalid type \'%s\' in group \'%s\'", type, this.getGroupId()));
                        invalidInstanceTypes.add(type);
                    }
                }
            }
        }
    }
    //endregion

    //region Getters
    public List<? extends SpotinstInstanceWeight> getExecutorsForTypes() {
        return executorsForTypes;
    }

    public List<String> getInvalidInstanceTypes() {
        return this.invalidInstanceTypes;
    }
    //endregion

    //region Classes
    @Extension
    public static class DescriptorImpl extends BaseSpotinstCloud.DescriptorImpl {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Spot AWS Elastigroup";
        }


    }
    //endregion
}
