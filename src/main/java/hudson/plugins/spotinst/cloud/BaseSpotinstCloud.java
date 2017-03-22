package hudson.plugins.spotinst.cloud;

import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import hudson.plugins.spotinst.slave.SpotinstSlave;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public abstract class BaseSpotinstCloud extends Cloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpotinstCloud.class);
    protected String                       groupId;
    protected Map<String, PendingInstance> pendingInstances;
    private   String                       labelString;
    private   String                       idleTerminationMinutes;
    private   String                       workspaceDir;
    private   Set<LabelAtom>               labelSet;
    private   SlaveUsageEnum               usage;
    private   String                       tunnel;
    private   String                       vmargs;

    //endregion

    //region Constructor
    public BaseSpotinstCloud(String groupId, String labelString, String idleTerminationMinutes, String workspaceDir,
                             SlaveUsageEnum usage, String tunnel, String vmargs) {
        super(groupId);
        this.groupId = groupId;
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
        this.tunnel = tunnel;
        this.vmargs = vmargs;
    }

    //endregion

    //region Private Methods
    private synchronized List<SpotinstSlave> provisionSlaves(ProvisionRequest request) {
        LOGGER.info("Scale up Elastigroup: " + groupId + " with " + request.getExecutors() + " workload units");
        List<SpotinstSlave> slaves = scaleUp(request);
        return slaves;
    }

    private void setNumOfNeededExecutors(ProvisionRequest request) {
        int spotPendingExecutors        = 0;
        int instanceInitiatingExecutors = 0;
        for (PendingInstance pendingInstance : pendingInstances.values()) {
            if (request.getLabel() == null || (pendingInstance.getRequestedLabel() != null &&
                                               pendingInstance.getRequestedLabel().equals(request.getLabel()))) {
                switch (pendingInstance.getStatus()) {
                    case SPOT_PENDING: {
                        spotPendingExecutors += pendingInstance.getNumOfExecutors();
                    }
                    break;
                    case INSTANCE_INITIATING: {
                        instanceInitiatingExecutors += pendingInstance.getNumOfExecutors();
                    }
                    break;
                }
            }
        }

        int currentPendingExecutors = spotPendingExecutors + instanceInitiatingExecutors;

        LOGGER.info(String.format("Pending spot executors: %s, Initiating instance executors: %s, for request: %s",
                                  spotPendingExecutors, instanceInitiatingExecutors, JsonMapper.toJson(request)));

        if (request.getExecutors() > currentPendingExecutors) {
            request.setExecutors(request.getExecutors() - currentPendingExecutors);
        }
        else {
            request.setExecutors(0);
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
        try {
            slave = new SpotinstSlave(this, id, groupId, id, instanceType, labelString, idleTerminationMinutes,
                                      workspaceDir, numOfExecutors, mode, this.tunnel, this.vmargs);

        }
        catch (Descriptor.FormException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return slave;
    }

    protected List<SpotinstSlave> loadSlaves() {
        List<SpotinstSlave> retVal   = new LinkedList<>();
        List<Node>          allNodes = Jenkins.getInstance().getNodes();
        if (allNodes != null) {
            for (Node node : allNodes) {
                if (node instanceof SpotinstSlave) {
                    SpotinstSlave slave = (SpotinstSlave) node;
                    if (slave.getElastigroupId().equals(groupId)) {
                        retVal.add(slave);
                    }
                }
            }
        }

        return retVal;
    }

    //endregion

    //region Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        ProvisionRequest request = new ProvisionRequest(label, excessWorkload);

        LOGGER.info(String.format("Got provision slave request: %s", JsonMapper.toJson(request)));

        setNumOfNeededExecutors(request);

        if (request.getExecutors() > 0) {
            LOGGER.info(String.format("Need to scale up %s units", request.getExecutors()));

            List<SpotinstSlave> slaves = provisionSlaves(request);
            if (slaves.size() > 0) {
                for (SpotinstSlave slave : slaves) {
                    try {
                        Jenkins.getInstance().addNode(slave);
                    }
                    catch (IOException e) {
                        LOGGER.error(String.format("Failed to create node for slave: %s", slave.getInstanceId()), e);
                    }
                }
            }
        }
        else {
            LOGGER.info("No need to scale up new slaves, there are some that are initiating");
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

    public boolean isInstancePending(String id) {
        boolean retVal = pendingInstances.containsKey(id);
        return retVal;
    }

    public void onInstanceReady(String id) {
        pendingInstances.remove(id);
    }
    //endregion

    //region Classes
    public static abstract class DescriptorImpl extends Descriptor<Cloud> {
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

    public String getLabelString() {
        return labelString;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public void setPendingInstances(Map<String, PendingInstance> pendingInstances) {
        this.pendingInstances = pendingInstances;
    }
    //endregion

    //region Abstract Methods
    abstract List<SpotinstSlave> scaleUp(ProvisionRequest request);

    public abstract boolean detachInstance(String instanceId);

    public abstract String getCloudUrl();

    public abstract void recoverInstances();

    public abstract void monitorInstances();
    //endregion
}
