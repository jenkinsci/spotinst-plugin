package hudson.plugins.spotinst.model.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Ran on 12/7/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsDetachInstancesRequest {

    //region Memebers
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
}
