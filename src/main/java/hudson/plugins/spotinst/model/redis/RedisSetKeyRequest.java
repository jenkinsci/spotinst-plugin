package hudson.plugins.spotinst.model.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisSetKeyRequest {

    //region Memebers
    String groupId;
    String controllerIdentifier;
    Integer ttl;
    //endregion

    //region Getters & Setters
    public String getGroupId() {
        return groupId;
    }

    public String getControllerIdentifier() {
        return controllerIdentifier;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setControllerIdentifier(String controllerIdentifier) {
        this.controllerIdentifier = controllerIdentifier;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }
    //endregion
}
