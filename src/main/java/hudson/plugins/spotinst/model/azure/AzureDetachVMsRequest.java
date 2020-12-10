package hudson.plugins.spotinst.model.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureDetachVMsRequest {
    //region members
    private List<String> vmsToDetach;
    private Boolean      shouldTerminateVms;
    private Boolean      shouldDecrementTargetCapacity;
    //endregion

    //region getters & setters
    public List<String> getVmsToDetach() {
        return vmsToDetach;
    }

    public void setVmsToDetach(List<String> vmsToDetach) {
        this.vmsToDetach = vmsToDetach;
    }

    public Boolean getShouldTerminateVms() {
        return shouldTerminateVms;
    }

    public void setShouldTerminateVms(Boolean shouldTerminateVms) {
        this.shouldTerminateVms = shouldTerminateVms;
    }

    public Boolean getShouldDecrementTargetCapacity() {
        return shouldDecrementTargetCapacity;
    }

    public void setShouldDecrementTargetCapacity(Boolean shouldDecrementTargetCapacity) {
        this.shouldDecrementTargetCapacity = shouldDecrementTargetCapacity;
    }
    //endregion
}
