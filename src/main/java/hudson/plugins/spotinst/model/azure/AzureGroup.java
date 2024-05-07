package hudson.plugins.spotinst.model.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by ItayShklar on 15/04/2024.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureGroup {
    //region members
    private String id;
    private String name;
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
    //endregion
}
