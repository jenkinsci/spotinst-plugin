package hudson.plugins.spotinst.model.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisSetKeyRequest {

    //region Memebers
    String groupId;
    String orchestratorIdentifier;
    Integer ttl;
    //endregion

    //region Getters & Setters
    public String getGroupId() {
        return groupId;
    }

    public String getOrchestratorIdentifier() {
        return orchestratorIdentifier;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setOrchestratorIdentifier(String orchestratorIdentifier) {
        this.orchestratorIdentifier = orchestratorIdentifier;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }
    //endregion
}
