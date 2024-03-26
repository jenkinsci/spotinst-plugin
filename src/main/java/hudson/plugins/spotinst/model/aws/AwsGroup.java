package hudson.plugins.spotinst.model.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by ItayShklar on 07/08/2023.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsGroup {
    //region members
    private String                        id;
    private String           name;
    private String           description;
    private AwsGroupStrategy strategy;
    //endregion

    //region getters & setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AwsGroupStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AwsGroupStrategy strategy) {
        this.strategy = strategy;
    }

    //endregion
}
