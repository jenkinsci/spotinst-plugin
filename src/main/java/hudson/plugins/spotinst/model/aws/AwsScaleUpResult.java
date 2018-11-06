package hudson.plugins.spotinst.model.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsScaleUpResult {

    //region Members
    private List<AwsScaleResultNewSpot>     newSpotRequests;
    private List<AwsScaleResultNewInstance> newInstances;
    //endregion

    //region Public Methods
    public List<AwsScaleResultNewSpot> getNewSpotRequests() {
        return newSpotRequests;
    }

    public void setNewSpotRequests(List<AwsScaleResultNewSpot> newSpotRequests) {
        this.newSpotRequests = newSpotRequests;
    }

    public List<AwsScaleResultNewInstance> getNewInstances() {
        return newInstances;
    }

    public void setNewInstances(List<AwsScaleResultNewInstance> newInstances) {
        this.newInstances = newInstances;
    }
    //endregion
}
