package hudson.plugins.spotinst.model.gcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpDetachInstancesRequest {

    //region Members
    private List<String> instancesToDetach;
    private Boolean      shouldTerminateInstances;
    private Boolean      shouldDecrementTargetCapacity;
    //endregion

    //region Properties

    public List<String> getInstancesToDetach() {
        return instancesToDetach;
    }

    public void setInstancesToDetach(List<String> instancesToDetach) {
        this.instancesToDetach = instancesToDetach;
    }

    public Boolean getShouldTerminateInstances() {
        return shouldTerminateInstances;
    }

    public void setShouldTerminateInstances(Boolean shouldTerminateInstances) {
        this.shouldTerminateInstances = shouldTerminateInstances;
    }

    public Boolean getShouldDecrementTargetCapacity() {
        return shouldDecrementTargetCapacity;
    }

    public void setShouldDecrementTargetCapacity(Boolean shouldDecrementTargetCapacity) {
        this.shouldDecrementTargetCapacity = shouldDecrementTargetCapacity;
    }

    //endregion

    //region Methods
    //endregion
}
