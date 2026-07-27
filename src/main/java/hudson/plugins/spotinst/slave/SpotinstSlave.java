package hudson.plugins.spotinst.slave;

import hudson.Extension;
import hudson.model.*;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.slaves.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by ohadmuchnik on 23/05/2016.
 */
public class SpotinstSlave extends Slave implements EphemeralNode {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstSlave.class);

    private String            instanceId;
    private String            instanceType;
    private String            elastigroupId;
    private String            workspaceDir;
    private String            groupUrl;
    private SlaveUsageEnum    usage;
    private Date              createdAt;
    private BaseSpotinstCloud lastCloud;
    private volatile boolean           isTerminated = false;
    private transient volatile boolean terminationInProgress = false;
    //endregion

    //region Constructor
    public SpotinstSlave(String name, String elastigroupId, String instanceId, String instanceType, String label,
                         String idleTerminationMinutes, String workspaceDir, String numOfExecutors, Mode mode,
                         ComputerLauncher launcher,
                         List<NodeProperty<?>> nodeProperties) throws Descriptor.FormException, IOException {


        super(name, "Elastigroup Id: " + elastigroupId, workspaceDir, numOfExecutors, mode, label, launcher,
              new SpotinstRetentionStrategy(idleTerminationMinutes), nodeProperties);

        this.elastigroupId = elastigroupId;
        this.instanceType = instanceType;
        this.instanceId = instanceId;
        this.workspaceDir = workspaceDir;
        this.usage = SlaveUsageEnum.fromMode(mode);
        this.createdAt = new Date();
        groupUrl = getSpotinstCloud().getCloudUrl();
    }
    //endregion

    //region Getters
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public Date getCreatedAt() {
        return (Date) createdAt.clone();
    }

    public String getElastigroupId() {
        return elastigroupId;
    }

    public SlaveUsageEnum getUsage() {
        return usage;
    }

    public String getGroupUrl() {
        return groupUrl;
    }


    /**
     * In some edge-cases (e.g.: user has deleted the cloud before removing its nodes) {@link Jenkins#getCloud} will
     * return null, therefore we keep a possibly-stale (yet usable) instance of the cloud as a member to remedy those
     * scenarios (for example, {@link SpotinstSlave#terminate()} which calls this method).
     */
    public BaseSpotinstCloud getSpotinstCloud() {
        BaseSpotinstCloud retVal;
        Cloud             cloud = Jenkins.get().getCloud(this.elastigroupId);

        if (cloud != null) {
            retVal = (BaseSpotinstCloud) cloud;
            lastCloud = retVal;
        }
        else {
            LOGGER.warn(String.format(
                    "could not get Cloud %s from Jenkins for SpotinstSlave %s - returning the last known cloud instance",
                    this.elastigroupId, this.instanceId));
            retVal = lastCloud;
        }

        return retVal;
    }

    public String getPrivateIp() {
        String               retVal          = null;
        String               instanceId      = getInstanceId();
        SlaveInstanceDetails instanceDetails = getSpotinstCloud().getSlaveDetails(instanceId);

        if (instanceDetails != null) {
            retVal = instanceDetails.getPrivateIp();
        }

        return retVal;
    }

    public String getPublicIp() {
        String               retVal          = null;
        String               instanceId      = getInstanceId();
        SlaveInstanceDetails instanceDetails = getSpotinstCloud().getSlaveDetails(instanceId);

        if (instanceDetails != null) {
            retVal = instanceDetails.getPublicIp();
        }

        return retVal;
    }
    //endregion

    //region Public Override Methods
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public Computer createComputer() {
        return new SpotinstComputer(this);
    }

    @Override
    public Node reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        if (form != null) {
            String         usageStr  = form.getString("usage");
            SlaveUsageEnum usageEnum = SlaveUsageEnum.fromName(usageStr);

            if (usageEnum != null) {
                this.usage = usageEnum;
            }

            this.setMode(this.usage.toMode());

            boolean      shouldUseWebsocket = form.getBoolean("shouldUseWebsocket");
            JNLPLauncher launcher           = (JNLPLauncher) getLauncher();
            launcher.setWebSocket(shouldUseWebsocket);
        }

        return this;
    }

    @Override
    public Node asNode() {
        return this;
    }
    //endregion

    //region Public Methods
    public void terminate() {
        synchronized (this) {
            if (isTerminated || terminationInProgress) {
                LOGGER.info(String.format(
                        "Instance: %s is already terminated or termination is in progress. Ignoring.",
                        getInstanceId()));
                return;
            }
            terminationInProgress = true;
        }

        boolean completed = false;

        try {
            boolean isGroupManagedByThisController = getSpotinstCloud().isCloudReadyForGroupCommunication();

            if (!isGroupManagedByThisController) {
                LOGGER.error(
                        "Skipped terminating slave instance {} - slave's group {} is not ready for communication.",
                        getInstanceId(), getSpotinstCloud().getGroupId());
                return;
            }

            Boolean isInstanceRemoved = getSpotinstCloud().removeInstance(instanceId);

            if (!Boolean.TRUE.equals(isInstanceRemoved)) {
                LOGGER.error(String.format("Failed to terminate instance: %s", getInstanceId()));
                return;
            }

            removeIfInPending();

            try {
                // Do not hold the SpotinstSlave monitor while Jenkins acquires its queue lock.
                Jenkins.get().removeNode(this);
                completed = true;
                LOGGER.info(String.format("Instance: %s terminated successfully", getInstanceId()));
            }
            catch (IOException e) {
                LOGGER.error(String.format("Failed to remove node %s", getInstanceId()), e);
            }
        }
        finally {
            synchronized (this) {
                if (completed) {
                    isTerminated = true;
                }
                else {
                    terminationInProgress = false;
                }
            }
        }
    }

    public Boolean forceTerminate() {
        synchronized (this) {
            if (isTerminated || terminationInProgress) {
                LOGGER.info(String.format(
                        "Instance: %s is already terminated or termination is in progress. Ignoring force-terminate.",
                        getInstanceId()));
                return false;
            }
            terminationInProgress = true;
        }

        boolean instanceRemoved = false;
        boolean completed = false;

        try {
            boolean isGroupManagedByThisController = getSpotinstCloud().isCloudReadyForGroupCommunication();

            if (!isGroupManagedByThisController) {
                LOGGER.error(
                        "Skipped force terminating slave instance {} - slave's group {} is not ready for communication.",
                        getInstanceId(), getSpotinstCloud().getGroupId());
                return false;
            }

            instanceRemoved = Boolean.TRUE.equals(getSpotinstCloud().removeInstance(instanceId));

            if (instanceRemoved) {
                LOGGER.info(String.format("Instance: %s terminated successfully", getInstanceId()));
                removeIfInPending();
            }
            else {
                LOGGER.error(String.format("Failed to terminate instance: %s", getInstanceId()));
            }

            try {
                // Do not hold the SpotinstSlave monitor while Jenkins acquires its queue lock.
                Jenkins.get().removeNode(this);
                completed = instanceRemoved;
            }
            catch (IOException e) {
                LOGGER.error(String.format(
                        "Failed to remove node %s during force termination", getInstanceId()), e);
            }

            return instanceRemoved;
        }
        finally {
            synchronized (this) {
                if (completed) {
                    isTerminated = true;
                }
                else {
                    terminationInProgress = false;
                }
            }
        }
    }

    private void removeIfInPending() {
        String            instanceId        = getInstanceId();
        BaseSpotinstCloud cloud             = this.getSpotinstCloud();
        Boolean           isInstancePending = cloud.isInstancePending(instanceId);

        if (isInstancePending) {
            LOGGER.info(
                    "Removing the instance {} from pending list.",
                    instanceId);
            cloud.removeInstanceFromPending(instanceId);
        }
        else {
            LOGGER.info(
                    "The instance {} is NOT pending, ignored removing from pending list.",
                    instanceId);
        }
    }

    public boolean isSlavePending() {
        boolean retVal = getSpotinstCloud().isInstancePending(getNodeName());
        return retVal;
    }

    public Boolean onSlaveConnected() {
        Boolean retVal = getSpotinstCloud().onInstanceReady(getNodeName());
        return retVal;
    }
    //endregion

    //region Extension class
    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Spot Node";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
    //endregion
}
