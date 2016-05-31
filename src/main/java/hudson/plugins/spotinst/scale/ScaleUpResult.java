package hudson.plugins.spotinst.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScaleUpResult {

    //region Members
    private List<ScaleResultNewSpot> newSpotRequests;
    private List<ScaleResultNewInstance> newInstances;
    //endregion

    //region Public Methods
    public List<ScaleResultNewSpot> getNewSpotRequests() {
        return newSpotRequests;
    }

    public void setNewSpotRequests(List<ScaleResultNewSpot> newSpotRequests) {
        this.newSpotRequests = newSpotRequests;
    }

    public List<ScaleResultNewInstance> getNewInstances() {
        return newInstances;
    }

    public void setNewInstances(List<ScaleResultNewInstance> newInstances) {
        this.newInstances = newInstances;
    }
    //endregion
}
