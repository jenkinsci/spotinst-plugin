package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.plugins.spotinst.common.CloudProviderEnum;
import hudson.plugins.spotinst.common.SlaveUsageEnum;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by ohadmuchnik on 23/05/2016.
 */
public class SpotinstSlave extends Slave {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstSlave.class);
    private String         instanceId;
    private String         instanceType;
    private String         elastigroupId;
    private String         workspaceDir;
    private String         groupUrl;
    private SlaveUsageEnum usage;
    //endregion

    //region Constructor
    public SpotinstSlave(String name, String elastigroupId, String instanceId, String instanceType, String label,
                         String idleTerminationMinutes, String workspaceDir, String numOfExecutors,
                         Mode mode, String tunnel, String vmargs) throws Descriptor.FormException, IOException {
        super(name, "Elastigroup Id: " + elastigroupId, workspaceDir, numOfExecutors, mode, label,
              new SpotinstComputerLauncher(tunnel, vmargs), new SpotinstRetentionStrategy(idleTerminationMinutes),
              new LinkedList<NodeProperty<?>>());

        this.elastigroupId = elastigroupId;
        this.instanceType = instanceType;
        this.instanceId = instanceId;
        this.workspaceDir = workspaceDir;
        this.usage = SlaveUsageEnum.fromMode(mode);

        if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
            groupUrl = "gcp/gce";
        }
        else {
            groupUrl = "aws/ec2";
        }
    }

    //endregion

    //region Public Methods
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

    public String getElastigroupId() {
        return elastigroupId;
    }

    public void terminate() {
        boolean isTerminated;
        if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
            isTerminated = SpotinstGateway.gcpDetachInstance(elastigroupId, instanceId);
        }
        else {
            isTerminated = SpotinstGateway.awsDetachInstance(getInstanceId());
        }

        if (isTerminated) {
            LOGGER.info("Instance: " + getInstanceId() + " terminated successfully");
            try {
                Jenkins.getInstance().removeNode(this);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            LOGGER.error("Failed to terminate instance: " + getInstanceId());
        }
    }

    public SlaveUsageEnum getUsage() {
        return usage;
    }

    public boolean forceTerminate() {
        boolean isTerminated;
        if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
            isTerminated = SpotinstGateway.gcpDetachInstance(elastigroupId, instanceId);
        }
        else {
            isTerminated = SpotinstGateway.awsDetachInstance(getInstanceId());
        }

        if (isTerminated) {
            LOGGER.info("Instance: " + getInstanceId() + " terminated successfully");
        }
        else {
            LOGGER.error("Failed to terminate instance: " + getInstanceId());
        }

        try {
            Jenkins.getInstance().removeNode(this);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return isTerminated;
    }

    public String getGroupUrl() {
        return groupUrl;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

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
    //endregion
}
