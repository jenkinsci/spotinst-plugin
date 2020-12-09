package hudson.plugins.spotinst.slave;

import hudson.plugins.spotinst.model.aws.AwsGroupInstance;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;
import hudson.plugins.spotinst.model.azure.AzureV3GroupVm;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;

/**
 * @author Caduri Katzav
 */
public class SlaveInstanceDetails {
    //region Members
    private final String instanceId;
    private final String privateIp;
    private final String publicIp;
    //endregion

    //region Constructor
    public SlaveInstanceDetails(String instanceId, String privateIp, String publicIp) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
    }
    //endregion

    //region Static Methods
    public static SlaveInstanceDetails build(AwsGroupInstance instance) {
        SlaveInstanceDetails retVal =
                new SlaveInstanceDetails(instance.getInstanceId(), instance.getPrivateIp(), instance.getPublicIp());
        return retVal;
    }

    public static SlaveInstanceDetails build(AzureGroupInstance instance) {
        SlaveInstanceDetails retVal =
                new SlaveInstanceDetails(instance.getInstanceId(), instance.getPrivateIp(), instance.getPublicIp());
        return retVal;
    }

    public static SlaveInstanceDetails build(GcpGroupInstance instance) {
        SlaveInstanceDetails retVal =
                new SlaveInstanceDetails(instance.getInstanceName(), instance.getPrivateIpAddress(),
                                         instance.getPublicIpAddress());
        return retVal;
    }

    public static SlaveInstanceDetails build(AzureV3GroupVm instance) {
        SlaveInstanceDetails retVal =
                new SlaveInstanceDetails(instance.getVmName(), instance.getPrivateIp(),
                                         instance.getPublicIp());
        return retVal;
    }

    public static SlaveInstanceDetails build(SpotinstSlave slave) {
        SlaveInstanceDetails retVal =
                new SlaveInstanceDetails(slave.getInstanceId(), slave.getPrivateIp(), slave.getPublicIp());
        return retVal;
    }
    //endregion

    //region Getters & Setters
    public String getInstanceId() {
        return instanceId;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }
    //endregion
}
