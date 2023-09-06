package hudson.plugins.spotinst.model.aws.stateful;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.plugins.spotinst.common.stateful.StatefulInstanceStateEnum;

/**
 * Created by ItayShklar on 07/08/2023.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsStatefulInstance {
    //region members
    private String                    id;
    private String                    instanceId;
    private StatefulInstanceStateEnum state;
    //endregion

    //region getters & setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public StatefulInstanceStateEnum getState() {
        return state;
    }

    public void setState(StatefulInstanceStateEnum state) {
        this.state = state;
    }
    //endregion
}
