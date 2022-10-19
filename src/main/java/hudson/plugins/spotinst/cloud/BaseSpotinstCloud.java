package hudson.plugins.spotinst.cloud;

import hudson.DescriptorExtensionList;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.repos.IRedisRepo;
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
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public abstract class BaseSpotinstCloud extends Cloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpotinstCloud.class);

    protected static final int                               NO_OVERRIDED_NUM_OF_EXECUTORS = -1;
    private static final   Integer                             REDIS_ENTRY_TIME_TO_LIVE_IN_SECONDS = 60 * 3;
    public static final    String                               REDIS_OK_STATUS = "OK";
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

        boolean isGroupManagedByOtherController = SpotinstContext.getInstance().getGroupsInUse().containsKey(this.groupId);

        if (isGroupManagedByOtherController) {
            try {
                handleGroupDosNotManageByThisController(this.groupId);
            }
            catch (Exception e){
                LOGGER.error(e.getMessage());
            }
        }
        else {
            SpotinstContext.getInstance().getGroupsInUse().put(groupId,accountId);
        }

    }
    //endregion

    //region Overridden Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        ProvisionRequest request = new ProvisionRequest(label, excessWorkload);

        LOGGER.info(String.format("Got provision slave request: %s", JsonMapper.toJson(request)));
        boolean isGroupManagedByThisController = SpotinstContext.getInstance().getGroupsInUse().containsKey(this.groupId);

        if (isGroupManagedByThisController) {

            setNumOfNeededExecutors(request);

            if (request.getExecutors() > 0) {
                LOGGER.info(String.format("Need to scale up %s units", request.getExecutors()));


                List<SpotinstSlave> slaves = provisionSlaves(request);

                if (slaves.size() > 0) {
                    for (final SpotinstSlave slave : slaves) {

                        try {
                            Jenkins.getInstance().addNode(slave);
                        } catch (IOException e) {
                            LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
                        }
                    }
                }
            } 
            else {
                LOGGER.info("No need to scale up new slaves, there are some that are initiating");
            }
        } 
        else {
            try {
                handleGroupDosNotManageByThisController(groupId);
            }catch (Exception e){
                LOGGER.warn(e.getMessage());
            }        }

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
                            TimeUtils.isTimePassed(pendingInstance.getCreatedAt(), pendingThreshold);

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
            Boolean isOverOfflineThreshold = TimeUtils.isTimePassed(slaveCreatedAt, offlineThreshold);

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
    //endregion

    //region Private Methods
    private synchronized List<SpotinstSlave> provisionSlaves(ProvisionRequest request) {
        LOGGER.info(String.format("Scale up group: %s with %s workload units", groupId, request.getExecutors()));

        List<SpotinstSlave> slaves = scaleUp(request);
        return slaves;
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

    private void removeFromSuspendedGroupFetching(String groupId) {
        boolean isGroupExistInSuspendedGroupFetching = SpotinstContext.getInstance().getSuspendedGroupFetching().containsKey(groupId);

        if(isGroupExistInSuspendedGroupFetching){
            SpotinstContext.getInstance().getSuspendedGroupFetching().remove(groupId);
        }
    }

    private void addGroupToRedis(String groupId, String accountId, String controllerIdentifier) {
        IRedisRepo redisRepo = RepoManager.getInstance().getRedisRepo();
        ApiResponse<String> redisSetKeyResponse = redisRepo.setKey(groupId, accountId, controllerIdentifier,REDIS_ENTRY_TIME_TO_LIVE_IN_SECONDS);

        if (redisSetKeyResponse.isRequestSucceed()) {
            String redisResponseSetKeyValue = redisSetKeyResponse.getValue();

            if(redisResponseSetKeyValue.equals(REDIS_OK_STATUS)){
                LOGGER.info(String.format("Successfully added group %s to redis memory", groupId));
            }
            else{
                LOGGER.error(String.format("Failed adding group %s to redis memory", groupId));
            }
        }
        else {
            LOGGER.error("redis request failed");
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
            int overridedNumOfExecutors = getOverridedNumberOfExecutors(instanceType);
            boolean isNumOfExecutorsOverrided = overridedNumOfExecutors != NO_OVERRIDED_NUM_OF_EXECUTORS;

            if(isNumOfExecutorsOverrided){
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
        return NO_OVERRIDED_NUM_OF_EXECUTORS;
    }

    protected Integer getPendingThreshold() {
        return Constants.PENDING_INSTANCE_TIMEOUT_IN_MINUTES;
    }

    protected Integer getSlaveOfflineThreshold() {
        return Constants.SLAVE_OFFLINE_THRESHOLD_IN_MINUTES;
    }

    public void handleGroupDosNotManageByThisController(String groupId) {
        boolean isGroupExistInSuspendedGroupFetching = SpotinstContext.getInstance().getSuspendedGroupFetching().containsKey(groupId);
        String message;
        if(isGroupExistInSuspendedGroupFetching){
            message = String.format("Group %s is might be in use by other Jenkins controller, please make sure it belong to this Jenkins controller and try again after 3 minutes", groupId);
            LOGGER.warn(message);
        }
        else{
            //TODO Liron - pop up message in jenkins UI (ONLY after fetching groupId retries pass the ttl for key in redis is expired)
            message = String.format("Group %s is in use by other Jenkins controller", groupId);
            LOGGER.error(message);

//            StaplerRequest req = new RequestImpl();
//            StaplerResponse rsp = new RequestImpl();
//            sendError(message);
        }
    }

    public void syncGroupsOwner(String groupId, String accountId) {
        LOGGER.info(String.format("try fetching controller identifier for group %s from redis", groupId));

        boolean isGroupExistInLocalCache = SpotinstContext.getInstance().getGroupsInUse().containsKey(groupId);
        IRedisRepo redisRepo = RepoManager.getInstance().getRedisRepo();
        ApiResponse<Object> redisGetValueResponse = redisRepo.getValue(groupId, accountId);
        String controllerIdentifier = SpotinstContext.getInstance().getControllerIdentifier();

        if (redisGetValueResponse.isRequestSucceed()) {
            //redis response might return in different types
            if (redisGetValueResponse.getValue() instanceof String) {
                String redisResponseValue = (String) redisGetValueResponse.getValue();

                if (redisResponseValue != null) {
                    boolean isGroupBelongToController = redisResponseValue.equals(controllerIdentifier);

                    if (isGroupBelongToController) {
                        handleGroupManagedByThisController(groupId, accountId, isGroupExistInLocalCache, controllerIdentifier);
                    }
                    else {
                        LOGGER.info(String.format("group %s does not belong to controller with identifier %s", groupId, controllerIdentifier));

                        SpotinstContext.getInstance().getSuspendedGroupFetching().put(groupId, accountId);

                        if (isGroupExistInLocalCache) {
                            SpotinstContext.getInstance().getGroupsInUse().remove(groupId);
                        }
                    }
                }
                else {
                    LOGGER.warn("redis response value return null");
                }
            }
            //there is no controller for the given group in redis, should take ownership
            else {
                handleGroupManagedByThisController(groupId, accountId, isGroupExistInLocalCache, controllerIdentifier);
            }
        }
    }

    private void handleGroupManagedByThisController(String groupId, String accountId, boolean isGroupExistInLocalCache, String controllerIdentifier) {
        LOGGER.info(String.format("group %s belong to controller with identifier %s", groupId, controllerIdentifier));

        if (isGroupExistInLocalCache == false) {
            SpotinstContext.getInstance().getGroupsInUse().put(groupId, accountId);
        }

        removeFromSuspendedGroupFetching(groupId);
        //expand TTL of the current controller in redis
        addGroupToRedis(groupId, accountId, controllerIdentifier);
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

    @DataBoundSetter
    public void setIsSingleTaskNodesEnabled(Boolean isSingleTaskNodesEnabled) {
        this.isSingleTaskNodesEnabled = isSingleTaskNodesEnabled;

        // if enabled, enable and override GlobalExecutorOverride to 1
        // better clarity to user, avoid race conditions
        boolean shouldDisableGlobalExecutors = isSingleTaskNodesEnabled != null && isSingleTaskNodesEnabled && this.globalExecutorOverride != null;
        if (shouldDisableGlobalExecutors) {
            this.globalExecutorOverride.setIsEnabled(false);
        }
    }

    //endregion

    //region Abstract Methods
    abstract List<SpotinstSlave> scaleUp(ProvisionRequest request);

    public abstract Boolean detachInstance(String instanceId);

    public abstract String getCloudUrl();

    public abstract void syncGroupInstances();

    public abstract Map<String, String> getInstanceIpsById();

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