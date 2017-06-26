package hudson.plugins.spotinst.model.elastigroup.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Created by ohadmuchnik on 21/06/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureDetachNodeRequest {
    private List<String> nodesToDetach;
    private boolean shouldDecrementTargetCapacity;

    public List<String> getNodesToDetach() {
        return nodesToDetach;
    }

    public void setNodesToDetach(List<String> nodesToDetach) {
        this.nodesToDetach = nodesToDetach;
    }

    public boolean isShouldDecrementTargetCapacity() {
        return shouldDecrementTargetCapacity;
    }

    public void setShouldDecrementTargetCapacity(boolean shouldDecrementTargetCapacity) {
        this.shouldDecrementTargetCapacity = shouldDecrementTargetCapacity;
    }
}
