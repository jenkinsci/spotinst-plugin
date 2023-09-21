package hudson.plugins.spotinst.model.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by ItayShklar on 07/08/2023.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsGroupStrategy {
    //region members
    private AwsGroupPersistence        persistence;
    //endregion

    //region getters & setters
    public AwsGroupPersistence getPersistence() {
        return persistence;
    }

    public void setPersistence(AwsGroupPersistence persistence) {
        this.persistence = persistence;
    }
    //endregion
}
