package hudson.plugins.spotinst.model.gcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by ohadmuchnik on 28/08/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpScaleUpResult {

    List<GcpResultNewInstance> newPreemptibles;
    List<GcpResultNewInstance> newInstances;

    public List<GcpResultNewInstance> getNewPreemptibles() {
        return newPreemptibles;
    }

    public void setNewPreemptibles(List<GcpResultNewInstance> newPreemptibles) {
        this.newPreemptibles = newPreemptibles;
    }

    public List<GcpResultNewInstance> getNewInstances() {
        return newInstances;
    }

    public void setNewInstances(List<GcpResultNewInstance> newInstances) {
        this.newInstances = newInstances;
    }
}
