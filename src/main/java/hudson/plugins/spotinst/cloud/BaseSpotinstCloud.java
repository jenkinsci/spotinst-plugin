package hudson.plugins.spotinst.cloud;

import hudson.DescriptorExtensionList;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.cloud.helpers.GroupLockHelper;
import hudson.plugins.spotinst.cloud.helpers.TimeHelper;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.model.common.BlResponse;
import hudson.plugins.spotinst.repos.ILockRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.*;
import hudson.plugins.sshslaves.SSHConnector;
import hudson.slaves.*;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolLocationNodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static hudson.plugins.spotinst.common.SpotinstCloudCommunicationState.*;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public abstract class BaseSpotinstCloud extends Cloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpotinstCloud.class);

    protected static final int                               NO_OVERRIDDEN_NUM_OF_EXECUTORS = -1;
    protected              String                            accountId;
    protected              String                            groupId;
    protected              Map<String, PendingInstance>      pendingInstances;
    protected              Map<String, SlaveInstanceDetails> slaveInstancesDetailsByInstanceId;
    private                String                            labelString;
    private                String                            idleTerminationMinutes;
    private                String                            workspaceDir;
    private                Set<LabelAtom>                    labelSet;
    private                SlaveUsageEnum                    usage;
    private                String                            tunnel;
    private                String                            vmargs;
    private                EnvironmentVariablesNodeProperty  environmentVariables;
    private                ToolLocationNodeProperty          toolLocations;
    private                Boolean                           shouldUseWebsocket;
    private                Boolean                           shouldRetriggerBuilds;
    private                Boolean                           isSingleTaskNodesEnabled;
    private                ComputerConnector                 computerConnector;
    private                ConnectionMethodEnum              connectionMethod;
    private                Boolean                           shouldUsePrivateIp;
    private                SpotGlobalExecutorOverride        globalExecutorOverride;
    private                GroupAcquiringDetails             groupAcquiringDetails          = null;
    //endregion

    //region Constructor
    public BaseSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                             SlaveUsageEnum usage, String tunnel, Boolean shouldUseWebsocket,
                             Boolean shouldRetriggerBuilds, String vmargs,
                             EnvironmentVariablesNodeProperty environmentVariables,
                             ToolLocationNodeProperty toolLocations, String accountId,
                             ConnectionMethodEnum connectionMethod, ComputerConnector computerConnector,
                             Boolean shouldUsePrivateIp, SpotGlobalExecutorOverride globalExecutorOverride) {

        super(groupId);
        this.groupId = groupId;
        this.accountId = accountId;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.workspaceDir = workspaceDir;
        this.pendingInstances = new HashMap<>();
        labelSet = Label.parse(labelString);

        if (usage != null) {
            this.usage = usage;
        }
        else {
            this.usage = SlaveUsageEnum.NORMAL;
        }

        this.shouldRetriggerBuilds = shouldRetriggerBuilds == null || BooleanUtils.isTrue(shouldRetriggerBuilds);
        this.tunnel = tunnel;
        this.shouldUseWebsocket = shouldUseWebsocket;
        this.vmargs = vmargs;
        this.environmentVariables = environmentVariables;
        this.toolLocations = toolLocations;
        this.slaveInstancesDetailsByInstanceId = new HashMap<>();

        if (connectionMethod != null) {
            this.connectionMethod = connectionMethod;
        }
        else {
            this.connectionMethod = ConnectionMethodEnum.JNLP;
        }

        if (shouldUsePrivateIp != null) {
            this.shouldUsePrivateIp = shouldUsePrivateIp;
        }
        else {
            this.shouldUsePrivateIp = false;
        }

        this.computerConnector = computerConnector;

        if (globalExecutorOverride != null) {
            this.globalExecutorOverride = globalExecutorOverride;
        }
        else {
            this.globalExecutorOverride = new SpotGlobalExecutorOverride(false, 1);
        }

        boolean isActiveCloud = isActive();

        if (isActiveCloud) {
            groupAcquiringDetails = new GroupAcquiringDetails(groupId, accountId);
            syncGroupOwner();
        }
    }
    //endregion

    //region Overridden Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        ProvisionRequest request = new ProvisionRequest(label, excessWorkload);

        LOGGER.info(String.format("Got provision slave request: %s", JsonMapper.toJson(request)));
        boolean isGroupManagedByThisController = isCloudReadyForGroupCommunication();

        if (isGroupManagedByThisController) {
            setNumOfNeededExecutors(request);

            if (request.getExecutors() > 0) {
                LOGGER.info(String.format("Need to scale up %s units", request.getExecutors()));
                List<SpotinstSlave> slaves = provisionSlaves(request);

                if (slaves.size() > 0) {
                    for (final SpotinstSlave slave : slaves) {
                        try {
                            Jenkins.getInstance().addNode(slave);
                        }
                        catch (IOException e) {
                            LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()),
                                         e);
                        }
                    }
                }
            }
            else {
                LOGGER.info("No need to scale up new slaves, there are some that are initiating");
            }
        }
        else {
            handleGroupManagedByOtherController();
        }

        return Collections.emptyList();
    }

    @Override
    public boolean canProvision(Label label) {
        boolean canProvision = true;

        if (label != null) {
            canProvision = label.matches(labelSet);
        }
        else {
            if (this.usage.equals(SlaveUsageEnum.EXCLUSIVE) && labelSet.size() > 0) {
                canProvision = false;
            }
        }

        return canProvision;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    public Boolean isInstancePending(String id) {
        Boolean retVal = pendingInstances.containsKey(id);
        return retVal;
    }
    //endregion

    //region Public Methods
    public void onInstanceReady(String instanceId) {
        removeInstanceFromPending(instanceId);
    }

    public void removeInstanceFromPending(String instanceId) {
        pendingInstances.remove(instanceId);
    }

    public void monitorInstances() {
        if (pendingInstances.size() > 0) {
            List<String> keys = new LinkedList<>(pendingInstances.keySet());

            for (String key : keys) {
                PendingInstance pendingInstance = pendingInstances.get(key);

                if (pendingInstance != null) {

                    Integer pendingThreshold = getPendingThreshold();
                    Boolean isPendingOverThreshold =
                            TimeUtils.isTimePassedInMinutes(pendingInstance.getCreatedAt(), pendingThreshold);

                    if (isPendingOverThreshold) {
                        LOGGER.info(String.format(
                                "Instance %s is in initiating state for over than %s minutes, ignoring this instance",
                                pendingInstance.getId(), pendingThreshold));
                        removeInstanceFromPending(key);
                    }
                }
            }

            connectOfflineSshAgents();
        }
    }

    private void connectOfflineSshAgents() {
        List<SpotinstSlave> offlineAgents = getOfflineSshAgents();

        if (offlineAgents.size() > 0) {
            LOGGER.info(String.format("%s offline SSH agent(s) currently waiting to connect", offlineAgents.size()));

            Map<String, String> instanceIpById = getInstanceIpsById();

            for (SpotinstSlave offlineAgent : offlineAgents) {
                String agentName  = offlineAgent.getNodeName();
                String ipForAgent = instanceIpById.get(agentName);

                if (ipForAgent != null) {
                    String preFormat = "IP for agent %s is available at %s, trying to attach SSHLauncher and launch";
                    LOGGER.info(String.format(preFormat, agentName, ipForAgent));
                    connectAgent(offlineAgent, ipForAgent);
                }
                else {
                    String preFormat = "IP for agent %s is not available yet, not attaching SSH launcher";
                    LOGGER.info(String.format(preFormat, agentName));
                }
            }
        }
    }

    public void connectAgent(SpotinstSlave offlineAgent, String ipForAgent) {
        SpotinstComputer computerForAgent = (SpotinstComputer) offlineAgent.toComputer();

        if (computerForAgent != null) {
            ComputerConnector connector        = getComputerConnector();
            ComputerLauncher  computerLauncher = computerForAgent.getLauncher();

            if (computerLauncher == null || computerLauncher.getClass() != SpotinstComputerLauncher.class) {
                try {
                    SpotSSHComputerLauncher launcher =
                            new SpotSSHComputerLauncher(connector.launch(ipForAgent, computerForAgent.getListener()),
                                                        this.getShouldRetriggerBuilds());
                    offlineAgent.setLauncher(launcher);
                    // save this information to disk - so Launcher survives Jenkins restarts
                    offlineAgent.save();
                    // tell computer its node has been updated
                    computerForAgent.resyncNode();
                    computerForAgent.connect(false);

                }
                catch (IOException | InterruptedException e) {
                    String preFormatted = "Error while launching agent %s with a newly assigned IP %s";
                    LOGGER.error(String.format(preFormatted, offlineAgent.getNodeName(), ipForAgent));
                    e.printStackTrace();
                }
            }
        }
        else {
            String preFormatted = "Agent %s does not have a computer";
            LOGGER.warn(String.format(preFormatted, offlineAgent.getNodeName()));
        }
    }

    private List<SpotinstSlave> getOfflineSshAgents() {
        List<SpotinstSlave> retVal = new LinkedList<>();

        if (pendingInstances.size() > 0) {
            List<String> keys = new LinkedList<>(pendingInstances.keySet());

            for (String key : keys) {
                PendingInstance pendingInstance = pendingInstances.get(key);
                String          instanceId      = pendingInstance.getId();
                SpotinstSlave   agent           = (SpotinstSlave) Jenkins.get().getNode(instanceId);

                if (agent != null) {
                    SpotinstComputer computerForAgent = (SpotinstComputer) agent.getComputer();

                    if (computerForAgent != null) {
                        if (computerForAgent.isOnline()) {
                            LOGGER.info(String.format("Agent %s is already online, no need to handle", instanceId));
                        }
                        else {
                            if (computerForAgent.getLauncher() == null ||
                                computerForAgent.getLauncher().getClass() != SpotinstComputerLauncher.class) {
                                retVal.add(agent);
                            }
                        }
                    }
                    else {
                        String msg =
                                "Agent %s does not have a computer - agent isn't necessarily an SSH agent - SpotinstCloud connection type is: %s";
                        LOGGER.warn(String.format(msg, instanceId, this.getConnectionMethod()));
                    }

                }
                else {
                    LOGGER.warn(String.format("Pending instance %s does not have a SpotinstSlave", instanceId));
                }
            }
        }

        return retVal;
    }

    protected void terminateOfflineSlaves(SpotinstSlave slave, String slaveInstanceId) {
        SlaveComputer computer = slave.getComputer();
        if (computer != null) {
            Integer offlineThreshold  = getSlaveOfflineThreshold();
            Boolean isSlaveConnecting = computer.isConnecting();
            Boolean isSlaveOffline    = computer.isOffline();
            // the computer is actively marked as temporary offline
            Boolean temporarilyOffline = computer.isTemporarilyOffline();
            long    idleStartMillis    = computer.getIdleStartMilliseconds();

            long idleInMillis  = System.currentTimeMillis() - idleStartMillis;
            long idleInMinutes = TimeUnit.MILLISECONDS.toMinutes(idleInMillis);
            // the computer has been idle for more than threshold - rule out temporary disconnection
            Boolean isOverIdleThreshold = idleInMinutes > offlineThreshold;


            Date    slaveCreatedAt         = slave.getCreatedAt();
            Boolean isOverOfflineThreshold = TimeUtils.isTimePassedInMinutes(slaveCreatedAt, offlineThreshold);

            if (isSlaveOffline && isSlaveConnecting == false && isOverOfflineThreshold && temporarilyOffline == false &&
                isOverIdleThreshold) {
                LOGGER.info(String.format(
                        "Agent for instance: %s running in group: %s is offline and created more than %d minutes ago (agent creation time: %s), terminating",
                        slaveInstanceId, groupId, offlineThreshold, slaveCreatedAt));

                slave.terminate();
            }
        }
    }

    public SlaveInstanceDetails getSlaveDetails(String instanceId) {
        SlaveInstanceDetails retVal = slaveInstancesDetailsByInstanceId.get(instanceId);

        return retVal;
    }

    public boolean isCloudReadyForGroupCommunication() {
        boolean retVal  = false;
        boolean isValid = StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId);

        if (isValid) {
            retVal = groupAcquiringDetails.getState().equals(SPOTINST_CLOUD_COMMUNICATION_READY);
        }

        return retVal;
    }

    public Boolean isActive() {
        Boolean retVal = StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(accountId);
        return retVal;
    }
    //endregion

    //region Private Methods
    private synchronized List<SpotinstSlave> provisionSlaves(ProvisionRequest request) {
        LOGGER.info(String.format("Scale up group: %s with %s workload units", groupId, request.getExecutors()));

        List<SpotinstSlave> retVal = scaleUp(request);
        return retVal;
    }

    private void setNumOfNeededExecutors(ProvisionRequest request) {
        PendingExecutorsCounts pendingExecutorsCounts = getPendingExecutors(request);

        Integer pendingExecutors    = pendingExecutorsCounts.getPendingExecutors();
        Integer initiatingExecutors = pendingExecutorsCounts.getInitiatingExecutors();

        Integer currentPendingExecutors = pendingExecutors + initiatingExecutors;

        LOGGER.info(
                String.format("Pending instances executors: %s, Initiating instances executors: %s, for request: %s",
                              pendingExecutors, initiatingExecutors, JsonMapper.toJson(request)));

        if (request.getExecutors() > currentPendingExecutors) {
            Integer neededExecutors = request.getExecutors() - currentPendingExecutors;
            request.setExecutors(neededExecutors);
        }
        else {
            request.setExecutors(0);
        }
    }

    private List<NodeProperty<?>> buildNodeProperties() {
        List<NodeProperty<?>> retVal = new LinkedList<>();

        if (this.environmentVariables != null && this.environmentVariables.getEnvVars() != null) {
            retVal.add(this.environmentVariables);
        }

        if (this.toolLocations != null && this.toolLocations.getLocations() != null) {
            retVal.add(this.toolLocations);
        }

        return retVal;
    }

    private Integer getExecutorsFromGlobalOverride() {
        Integer retVal = null;

        boolean isOverrideEnabled = this.getGlobalExecutorOverride().getIsEnabled();

        if (isOverrideEnabled) {
            LOGGER.debug("Global Executor Override is enabled");
            boolean isOverrideValid = isGlobalExecutorOverrideValid();

            if (isOverrideValid) {
                retVal = this.getGlobalExecutorOverride().getExecutors();
            }
            else {
                LOGGER.warn(
                        "Global Executor Override is enabled but invalid, reverting to the default instance type executors number");
            }
        }

        return retVal;
    }

    private boolean isGlobalExecutorOverrideValid() {
        boolean retVal            = false;
        Integer overrideExecutors = this.getGlobalExecutorOverride().getExecutors();

        String warningMsg = String.format("Global Executor Override executors attribute has an invalid value %s",
                                          overrideExecutors);

        if (overrideExecutors != null) {
            if (overrideExecutors > 0) {
                retVal = true;
            }
            else {
                LOGGER.warn(warningMsg);
            }
        }
        else {
            LOGGER.warn(warningMsg);
        }

        return retVal;
    }

    private void AcquireGroupLock(String controllerIdentifier) {
        LOGGER.info(
                String.format("group %s doesn't belong to any controller. controller with identifier %s is trying to lock it",
                              groupId, controllerIdentifier));

        BlResponse<Boolean> hasLockResponse =
                GroupLockHelper.AcquireLockGroupController(groupId, accountId, controllerIdentifier);

        if (hasLockResponse.isSucceed()) {
            Boolean hasLock = hasLockResponse.getResult();

            if (hasLock) {
                groupAcquiringDetails.setState(SPOTINST_CLOUD_COMMUNICATION_READY);
            }
            else {
                handleGroupManagedByOtherController();
            }
        }
        else {
            handleInitializingExpired();
        }
    }

    private void ExpandGroupLock(String controllerIdentifier) {
        LOGGER.info("group {} already belongs the controller {}, reviving the lock duration.", groupId,
                    controllerIdentifier);

        BlResponse<Boolean> lockResponse =
                GroupLockHelper.SetGroupControllerLockExpiry(groupId, accountId, controllerIdentifier);

        if (lockResponse.isSucceed()) {
            if (lockResponse.getResult()) {
                groupAcquiringDetails.setState(SPOTINST_CLOUD_COMMUNICATION_READY);
            }
            else {
                handleGroupManagedByOtherController();
            }
        }
    }

    private void handleInitializingExpired() {
        if (groupAcquiringDetails.getState().equals(SPOTINST_CLOUD_COMMUNICATION_INITIALIZING)) {
            boolean shouldFail = TimeHelper.isTimePassedInSeconds(groupAcquiringDetails.getTimeStamp());

            if (shouldFail) {
                groupAcquiringDetails.setState(SPOTINST_CLOUD_COMMUNICATION_FAILED);
            }
        }
    }
    //endregion

    //region Protected Methods
    protected void addToPending(String id, Integer numOfExecutors, PendingInstance.StatusEnum status, String label) {
        PendingInstance pendingInstance = new PendingInstance();
        pendingInstance.setId(id);
        pendingInstance.setNumOfExecutors(numOfExecutors);
        pendingInstance.setStatus(status);
        pendingInstance.setCreatedAt(new Date());
        pendingInstance.setRequestedLabel(label);
        pendingInstances.put(id, pendingInstance);
    }

    protected SpotinstSlave buildSpotinstSlave(String id, String instanceType, String numOfExecutors) {
        SpotinstSlave slave = null;
        Node.Mode     mode  = Node.Mode.NORMAL;

        if (this.usage != null && this.usage.equals(SlaveUsageEnum.EXCLUSIVE)) {
            mode = Node.Mode.EXCLUSIVE;
        }

        List<NodeProperty<?>> nodeProperties = buildNodeProperties();

        try {
            ComputerLauncher launcher = buildLauncherForAgent(id);
            slave = new SpotinstSlave(id, groupId, id, instanceType, labelString, idleTerminationMinutes, workspaceDir,
                                      numOfExecutors, mode, launcher, nodeProperties);

        }
        catch (Descriptor.FormException | IOException e) {
            LOGGER.error(String.format("Failed to build Spotinst slave for: %s", id));
            e.printStackTrace();
        }

        return slave;
    }

    private ComputerLauncher buildLauncherForAgent(String instanceId) throws IOException {
        ComputerLauncher     retVal;
        Boolean              isSshCloud          = this.getConnectionMethod().equals(ConnectionMethodEnum.SSH);
        SlaveInstanceDetails instanceDetailsById = getSlaveDetails(instanceId);

        if (isSshCloud) {
            retVal = HandleSSHLauncher(instanceDetailsById);
        }
        else {
            retVal = handleJNLPLauncher();
        }

        return retVal;
    }

    private ComputerLauncher handleJNLPLauncher() {
        return new SpotinstComputerLauncher(this.getTunnel(), this.getVmargs(), this.getShouldUseWebsocket(),
                                            this.getShouldRetriggerBuilds());
    }

    private ComputerLauncher HandleSSHLauncher(SlaveInstanceDetails instanceDetailsById) throws IOException {
        ComputerLauncher retVal     = null;
        String           instanceId = this.name;
        String           ipAddress;

        if (instanceDetailsById != null) {
            if (this.getShouldUsePrivateIp()) {
                ipAddress = instanceDetailsById.getPrivateIp();
            }
            else {
                ipAddress = instanceDetailsById.getPublicIp();
            }

            if (ipAddress != null) {
                try {
                    Boolean shouldRetriggerBuilds = this.getShouldRetriggerBuilds();
                    retVal = new SpotSSHComputerLauncher(
                            this.getComputerConnector().launch(instanceDetailsById.getPublicIp(), TaskListener.NULL),
                            shouldRetriggerBuilds);
                }
                catch (InterruptedException e) {
                    String preformatted =
                            "Creating SSHComputerLauncher for SpotinstSlave (instance %s) was interrupted";
                    LOGGER.error(String.format(preformatted, instanceId));
                    e.printStackTrace();
                }
            }
            else {
                String preformatted = "SSH-cloud instance %s does not have an IP yet, not setting launcher to for now.";
                LOGGER.info(String.format(preformatted, instanceId));
            }
        }
        else {
            LOGGER.info(String.format(
                    "no details about instance %s in instanceDetailsById map, not initializing launcher yet.",
                    this.name));
        }

        return retVal;
    }

    protected List<SpotinstSlave> getAllSpotinstSlaves() {
        LOGGER.info(String.format("Getting all existing slaves for group: %s", groupId));

        List<SpotinstSlave> retVal   = new LinkedList<>();
        List<Node>          allNodes = Jenkins.getInstance().getNodes();

        LOGGER.info(String.format("Found total %s nodes in Jenkins, filtering the group nodes", allNodes.size()));

        for (Node node : allNodes) {

            if (node instanceof SpotinstSlave) {
                SpotinstSlave slave = (SpotinstSlave) node;

                if (slave.getElastigroupId().equals(groupId)) {
                    retVal.add(slave);
                }
            }
        }

        return retVal;
    }

    protected PendingExecutorsCounts getPendingExecutors(ProvisionRequest request) {
        PendingExecutorsCounts retVal              = new PendingExecutorsCounts();
        Integer                pendingExecutors    = 0;
        Integer                initiatingExecutors = 0;

        for (PendingInstance pendingInstance : pendingInstances.values()) {
            if (request.getLabel() == null || (pendingInstance.getRequestedLabel() != null &&
                                               pendingInstance.getRequestedLabel().equals(request.getLabel()))) {
                switch (pendingInstance.getStatus()) {
                    case PENDING: {
                        pendingExecutors += pendingInstance.getNumOfExecutors();
                    }
                    break;
                    case INSTANCE_INITIATING: {
                        initiatingExecutors += pendingInstance.getNumOfExecutors();
                    }
                    break;
                    default: {
                        LOGGER.warn(String.format("Pending instance %s has unknown status %s", pendingInstance.getId(),
                                                  pendingInstance.getStatus().getName()));
                    }
                    break;
                }
            }
        }

        retVal.setPendingExecutors(pendingExecutors);
        retVal.setInitiatingExecutors(initiatingExecutors);

        return retVal;
    }

    protected Integer getNumOfExecutors(String instanceType) {
        Integer retVal;
        boolean isSingleTaskNodesEnabled = getIsSingleTaskNodesEnabled();

        if (isSingleTaskNodesEnabled) {
            retVal = 1;
        }
        else {
            int     overridedNumOfExecutors   = getOverridedNumberOfExecutors(instanceType);
            boolean isNumOfExecutorsOverrided = overridedNumOfExecutors != NO_OVERRIDDEN_NUM_OF_EXECUTORS;

            if (isNumOfExecutorsOverrided) {
                retVal = overridedNumOfExecutors;
            }
            else {
                Integer globalOverrideExecutorsNumber = getExecutorsFromGlobalOverride();

                if (globalOverrideExecutorsNumber != null) {
                    LOGGER.debug(String.format("Overriding executors for instance type %s to be %s", instanceType,
                                               globalOverrideExecutorsNumber));
                    retVal = globalOverrideExecutorsNumber;
                }
                else {
                    Integer defaultNumberOfExecutors = getDefaultExecutorsNumber(instanceType);

                    if (defaultNumberOfExecutors != null) {
                        retVal = defaultNumberOfExecutors;
                    }
                    else {
                        retVal = 1;
                        String warningMsg = String.format(
                                "Failed to determine # of executors for instance type %s, defaulting to %s executor(s). Group ID: %s",
                                instanceType, retVal, this.getGroupId());
                        LOGGER.warn(warningMsg);

                    }
                }
            }

        }

        LOGGER.debug(String.format("instance type executors number was set to %s", retVal));

        return retVal;
    }

    protected int getOverridedNumberOfExecutors(String instanceType) {
        return NO_OVERRIDDEN_NUM_OF_EXECUTORS;
    }

    protected Integer getPendingThreshold() {
        return Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES;
    }

    protected Integer getSlaveOfflineThreshold() {
        return Constants.SLAVE_OFFLINE_THRESHOLD_IN_MINUTES;
    }

    public void handleGroupManagedByOtherController() {
        handleInitializingExpired();

        if (groupAcquiringDetails.getState().equals(SPOTINST_CLOUD_COMMUNICATION_READY)) {
            groupAcquiringDetails = new GroupAcquiringDetails(groupId, accountId);
        }
    }

    public void syncGroupOwner() {
        ILockRepo           lockRepo                    = RepoManager.getInstance().getLockRepo();
        ApiResponse<String> lockGroupControllerResponse = lockRepo.getGroupControllerLockValue(groupId, accountId);

        if (lockGroupControllerResponse.isRequestSucceed()) {
            String  lockGroupControllerValue       = lockGroupControllerResponse.getValue();
            String  currentControllerIdentifier    = SpotinstContext.getInstance().getControllerIdentifier();
            boolean isGroupAlreadyHasAnyController = lockGroupControllerValue != null;

            if (isGroupAlreadyHasAnyController) {
                boolean isGroupBelongToCurrentController = currentControllerIdentifier.equals(lockGroupControllerValue);

                if (isGroupBelongToCurrentController) {
                    ExpandGroupLock(currentControllerIdentifier);
                }
                else {
                    LOGGER.warn(
                            "group {} does not belong to controller with identifier {}, make sure that there is no duplicated Jenkins controllers configured to the same group",
                            groupId, currentControllerIdentifier);
                    handleGroupManagedByOtherController();
                }
            }
            else {
                AcquireGroupLock(currentControllerIdentifier);
            }
        }
        else {
            LOGGER.error(
                    "group locking service failed to get lock for groupId {}, accountId {}. staying in current status {}. Errors: {}",
                    groupId, accountId, groupAcquiringDetails.getState().getName(),
                    lockGroupControllerResponse.getErrors());
        }
    }
    //endregion

    //region Getters / Setters
    public String getGroupId() {
        return groupId;
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public SlaveUsageEnum getUsage() {
        return usage;
    }

    public String getTunnel() {
        return tunnel;
    }

    public String getVmargs() {
        return vmargs;
    }

    public String getAccountId() {
        return accountId;
    }

    public EnvironmentVariablesNodeProperty getEnvironmentVariables() {
        return environmentVariables;
    }

    public ToolLocationNodeProperty getToolLocations() {
        return toolLocations;
    }

    public String getLabelString() {
        return labelString;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public void setPendingInstances(Map<String, PendingInstance> pendingInstances) {
        this.pendingInstances = pendingInstances;
    }

    public Boolean getShouldUseWebsocket() {
        return shouldUseWebsocket;
    }

    public void setShouldUseWebsocket(Boolean shouldUseWebsocket) {
        this.shouldUseWebsocket = shouldUseWebsocket;
    }

    public Boolean getShouldRetriggerBuilds() {
        return shouldRetriggerBuilds;
    }

    public void setShouldRetriggerBuilds(Boolean shouldRetriggerBuilds) {
        this.shouldRetriggerBuilds = shouldRetriggerBuilds;
    }

    public ConnectionMethodEnum getConnectionMethod() {
        if (this.connectionMethod == null) {
            return ConnectionMethodEnum.JNLP;
        }

        return connectionMethod;
    }

    public void setConnectionMethod(ConnectionMethodEnum connectionMethod) {
        this.connectionMethod = connectionMethod;
    }


    public ComputerConnector getComputerConnector() {
        return computerConnector;
    }

    public void setComputerConnector(ComputerConnector computerConnector) {
        this.computerConnector = computerConnector;
    }

    public boolean getShouldUsePrivateIp() {
        // default for clouds that were configured before introducing this field
        if (shouldUsePrivateIp == null) {
            return false;
        }
        return shouldUsePrivateIp;
    }

    public void setShouldUsePrivateIp(Boolean shouldUsePrivateIp) {
        this.shouldUsePrivateIp = shouldUsePrivateIp;
    }

    public SpotGlobalExecutorOverride getGlobalExecutorOverride() {
        SpotGlobalExecutorOverride retVal;
        // default for clouds that were configured before introducing this field
        if (this.globalExecutorOverride == null) {
            retVal = new SpotGlobalExecutorOverride(false, 1);
            this.globalExecutorOverride = retVal;
        }
        else {
            retVal = this.globalExecutorOverride;
        }

        return retVal;
    }

    public void setGlobalExecutorOverride(SpotGlobalExecutorOverride globalExecutorOverride) {
        this.globalExecutorOverride = globalExecutorOverride;
    }


    public Boolean getIsSingleTaskNodesEnabled() {
        if (this.isSingleTaskNodesEnabled == null) {
            return false;
        }
        else {
            return isSingleTaskNodesEnabled;
        }
    }

    public GroupAcquiringDetails getGroupAcquiringDetails() {
        return groupAcquiringDetails;
    }

    @DataBoundSetter
    public void setIsSingleTaskNodesEnabled(Boolean isSingleTaskNodesEnabled) {
        this.isSingleTaskNodesEnabled = isSingleTaskNodesEnabled;

        // if enabled, enable and override GlobalExecutorOverride to 1
        // better clarity to user, avoid race conditions
        boolean shouldDisableGlobalExecutors =
                isSingleTaskNodesEnabled != null && isSingleTaskNodesEnabled && this.globalExecutorOverride != null;
        if (shouldDisableGlobalExecutors) {
            this.globalExecutorOverride.setIsEnabled(false);
        }
    }

    //endregion

    //region Abstract Methods
    abstract List<SpotinstSlave> scaleUp(ProvisionRequest request);

    public abstract Boolean detachInstance(String instanceId);

    public abstract String getCloudUrl();

    public void syncGroupInstances() {
        boolean isGroupManagedByThisController = isCloudReadyForGroupCommunication();

        if (isGroupManagedByThisController) {
            handleSyncGroupInstances();
        }
        else {
            handleGroupManagedByOtherController();
        }
    }

    protected abstract void handleSyncGroupInstances();

    public Map<String, String> getInstanceIpsById() {
        Map<String, String> retVal;
        boolean             isGroupManagedByThisController = isCloudReadyForGroupCommunication();

        if (isGroupManagedByThisController) {
            retVal = handleGetInstanceIpsById();
        }
        else {
            handleGroupManagedByOtherController();
            retVal = new HashMap<>();
        }

        return retVal;
    }

    protected abstract Map<String, String> handleGetInstanceIpsById();

    protected abstract Integer getDefaultExecutorsNumber(String instanceType);
    //endregion

    //region Abstract Class
    @SuppressWarnings("unused")
    public static abstract class DescriptorImpl extends Descriptor<Cloud> {
        public DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> getToolDescriptors() {
            return ToolInstallation.all();
        }

        public String getKey(ToolInstallation installation) {
            return installation.getDescriptor().getClass().getName() + "@" + installation.getName();
        }

        public List getComputerConnectorDescriptors() {
            return Jenkins.get().getDescriptorList(ComputerConnector.class).stream()
                          .filter(x -> x.isSubTypeOf(SSHConnector.class)).collect(Collectors.toList());
        }
    }
    //endregion
}