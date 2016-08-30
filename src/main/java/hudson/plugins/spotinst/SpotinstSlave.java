package hudson.plugins.spotinst;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.plugins.spotinst.common.AwsInstanceType;
import hudson.plugins.spotinst.common.CloudProviderEnum;
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
    private String instanceId;
    private String instanceType;
    private String elastigroupId;
    private String workspaceDir;
    private String groupUrl;
    //endregion

    //region Constructor
    public SpotinstSlave(String name,
                         String elastigroupId,
                         String instanceId,
                         String instanceType,
                         String label,
                         String idleTerminationMinutes,
                         String workspaceDir,
                         String numOfExecutors) throws Descriptor.FormException, IOException {
        super(name,
                "Elastigroup Id: " + elastigroupId,
                workspaceDir,
                numOfExecutors,
                null,
                label,
                new SpotinstComputerLauncher(),
                new SpotinstRetentionStrategy(idleTerminationMinutes),
                new LinkedList<NodeProperty<?>>());

        this.elastigroupId = elastigroupId;
        this.instanceType = instanceType;
        this.instanceId = instanceId;
        this.workspaceDir = workspaceDir;


        if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
            groupUrl = "gcp/gce";
        } else {
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
        } else {
            isTerminated = SpotinstGateway.awsDetachInstance(getInstanceId());
        }

        if (isTerminated) {
            LOGGER.info("Instance: " + getInstanceId() + " terminated successfully");
            try {
                Jenkins.getInstance().removeNode(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.error("Failed to terminate instance: " + getInstanceId());
        }
    }

    public String getGroupUrl() {
        return groupUrl;
    }

    public static int executorsForInstanceType(AwsInstanceType awsInstanceType) {
        switch (awsInstanceType) {
            case T1Micro:
                return 1;
            case M1Small:
                return 1;
            case M1Medium:
                return 2;
            case M3Medium:
                return 2;
            case M1Large:
                return 4;
            case M3Large:
                return 4;
            case M4Large:
                return 4;
            case C1Medium:
                return 5;
            case M2Xlarge:
                return 6;
            case C3Large:
                return 7;
            case C4Large:
                return 7;
            case M1Xlarge:
                return 8;
            case M22xlarge:
                return 13;
            case M3Xlarge:
                return 13;
            case M4Xlarge:
                return 13;
            case C3Xlarge:
                return 14;
            case C4Xlarge:
                return 14;
            case C1Xlarge:
                return 20;
            case M24xlarge:
                return 26;
            case M32xlarge:
                return 26;
            case M42xlarge:
                return 26;
            case G22xlarge:
                return 26;
            case C32xlarge:
                return 28;
            case C42xlarge:
                return 28;
            case Cc14xlarge:
                return 33;
            case Cg14xlarge:
                return 33;
            case Hi14xlarge:
                return 35;
            case Hs18xlarge:
                return 35;
            case C34xlarge:
                return 55;
            case C44xlarge:
                return 55;
            case M44xlarge:
                return 55;
            case Cc28xlarge:
                return 88;
            case Cr18xlarge:
                return 88;
            case C38xlarge:
                return 108;
            case C48xlarge:
                return 108;
            case M410xlarge:
                return 120;
            default:
                return 1;
        }
    }

    @Override
    public Computer createComputer() {
        return new SpotinstComputer(this);
    }

    @Override
    public Node reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        SpotinstSlave retVal = (SpotinstSlave) super.reconfigure(req, form);
        return retVal;
    }
    //endregion
}
