package hudson.plugins.spotinst.model.aws.stateful;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by sitay on 30/08/23.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsDeallocateStatefulInstanceRequest {
    //region members
    private AwsStatefulDeallocationConfig statefulDeallocation;
    //endregion

    //region getters & setters
    public AwsStatefulDeallocationConfig getStatefulDeallocation() {
        return statefulDeallocation;
    }

    public void setStatefulDeallocation(AwsStatefulDeallocationConfig statefulDeallocation) {
        this.statefulDeallocation = statefulDeallocation;
    }
    //endregion
}