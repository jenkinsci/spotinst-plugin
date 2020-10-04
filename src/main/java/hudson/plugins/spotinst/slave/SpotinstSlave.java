package hudson.plugins.spotinst.slave;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by ohadmuchnik on 23/05/2016.
 */
public class SpotinstSlave extends Slave {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstSlave.class);

    private String            instanceId;
    private String            instanceType;
    private String            elastigroupId;
    private String            workspaceDir;
    private String            groupUrl;
    private SlaveUsageEnum    usage;
    private String            privateIp;
    private String            publicIp;
    private Date              createdAt;
    private BaseSpotinstCloud spotinstCloud;
    //endregion

    //region Constructor
    public SpotinstSlave(BaseSpotinstCloud spotinstCloud, String name, String elastigroupId, String instanceId,
                         String instanceType, String privateIp, String publicIp, String label, String idleTerminationMinutes, String workspaceDir,
                         String numOfExecutors, Mode mode, String tunnel, String vmargs,
                         List<NodeProperty<?>> nodeProperties) throws Descriptor.FormException, IOException {

        super(name, "Elastigroup Id: " + elastigroupId, workspaceDir, numOfExecutors, mode, label,
              new SpotinstComputerLauncher(tunnel, vmargs), new SpotinstRetentionStrategy(idleTerminationMinutes),
              nodeProperties);

        this.elastigroupId = elastigroupId;
        this.instanceType = instanceType;
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.workspaceDir = workspaceDir;
        this.usage = SlaveUsageEnum.fromMode(mode);
        this.createdAt = new Date();

        this.spotinstCloud = spotinstCloud;
        groupUrl = spotinstCloud.getCloudUrl();
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
        return createdAt;
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

    public BaseSpotinstCloud getSpotinstCloud() {
        return spotinstCloud;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }
    //endregion

    //region Setters
    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
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
        String         usageStr  = form.getString("usage");
        SlaveUsageEnum usageEnum = SlaveUsageEnum.fromName(usageStr);
        this.usage = usageEnum;
        this.setMode(this.usage.toMode());

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SpotinstSlave that = (SpotinstSlave) o;
        return Objects.equals(instanceId, that.instanceId) && Objects.equals(instanceType, that.instanceType) &&
               Objects.equals(elastigroupId, that.elastigroupId) && Objects.equals(workspaceDir, that.workspaceDir) &&
               Objects.equals(groupUrl, that.groupUrl) && usage == that.usage &&
               Objects.equals(privateIp, that.privateIp) && Objects.equals(publicIp, that.publicIp) &&
               Objects.equals(createdAt, that.createdAt) && Objects.equals(spotinstCloud, that.spotinstCloud);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), instanceId, instanceType, elastigroupId, workspaceDir, groupUrl, usage,
                            privateIp, publicIp, createdAt, spotinstCloud);
    }
    //endregion

    //region Public Methods
    public void terminate() {
        Boolean isTerminated = spotinstCloud.detachInstance(instanceId);

        if (isTerminated) {
            LOGGER.info(String.format("Instance: %s terminated successfully", getInstanceId()));
            try {
                Jenkins.getInstance().removeNode(this);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            LOGGER.error(String.format("Failed to terminate instance: %s", getInstanceId()));
        }
    }

    public Boolean forceTerminate() {
        Boolean isTerminated = spotinstCloud.detachInstance(instanceId);

        if (isTerminated) {
            LOGGER.info(String.format("Instance: %s terminated successfully", getInstanceId()));
        }
        else {
            LOGGER.error(String.format("Failed to terminate instance: %s", getInstanceId()));
        }

        try {
            Jenkins.getInstance().removeNode(this);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return isTerminated;
    }

    public boolean isSlavePending() {
        boolean retVal = this.spotinstCloud.isInstancePending(getNodeName());
        return retVal;
    }

    public void onSlaveConnected() {
        this.spotinstCloud.onInstanceReady(getNodeName());
    }
    //endregion

    //region Extension class
    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "Spotinst Slave";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
    //endregion
}
