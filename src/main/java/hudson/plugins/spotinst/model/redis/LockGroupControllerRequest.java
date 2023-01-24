package hudson.plugins.spotinst.model.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LockGroupControllerRequest {

    //region Memebers
    private final String  controllerIdentifier;
    private final Integer ttl;
    //endregion

    //region Constructor
    public LockGroupControllerRequest(String controllerIdentifier, Integer ttl) {
        this.controllerIdentifier = controllerIdentifier;
        this.ttl = ttl;
    }
    //endregion

    //region Getters & Setters
    public String getControllerIdentifier() {
        return controllerIdentifier;
    }

    public Integer getTtl() {
        return ttl;
    }
    //endregion
}
