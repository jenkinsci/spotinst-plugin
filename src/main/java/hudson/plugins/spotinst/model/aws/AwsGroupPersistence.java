package hudson.plugins.spotinst.model.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by ItayShklar on 07/08/2023.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsGroupPersistence {
    //region members
    private Boolean shouldPersistPrivateIp;
    private Boolean shouldPersistBlockDevices;
    private Boolean shouldPersistRootDevice;
    private String  blockDevicesMode;
    //endregion

    //region getters & setters

    public Boolean getShouldPersistPrivateIp() {
        return shouldPersistPrivateIp;
    }

    public void setShouldPersistPrivateIp(Boolean shouldPersistPrivateIp) {
        this.shouldPersistPrivateIp = shouldPersistPrivateIp;
    }

    public Boolean getShouldPersistBlockDevices() {
        return shouldPersistBlockDevices;
    }

    public void setShouldPersistBlockDevices(Boolean shouldPersistBlockDevices) {
        this.shouldPersistBlockDevices = shouldPersistBlockDevices;
    }

    public Boolean getShouldPersistRootDevice() {
        return shouldPersistRootDevice;
    }

    public void setShouldPersistRootDevice(Boolean shouldPersistRootDevice) {
        this.shouldPersistRootDevice = shouldPersistRootDevice;
    }

    public String getBlockDevicesMode() {
        return blockDevicesMode;
    }

    public void setBlockDevicesMode(String blockDevicesMode) {
        this.blockDevicesMode = blockDevicesMode;
    }
    //endregion
}
