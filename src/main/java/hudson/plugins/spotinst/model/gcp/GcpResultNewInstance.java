package hudson.plugins.spotinst.model.gcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by ohadmuchnik on 28/08/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpResultNewInstance {

    private String instanceName;
    private String machineType;
    private String zone;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
