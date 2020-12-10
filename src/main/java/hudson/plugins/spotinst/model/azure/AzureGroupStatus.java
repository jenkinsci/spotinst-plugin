package hudson.plugins.spotinst.model.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureGroupStatus {
    //region members
    private String             status;
    private List<AzureGroupVm> vms;
    private String             description;
    //endregion

    //region getters & setters
    public AzureGroupStatus() {
        this.vms = new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AzureGroupVm> getVms() {
        return vms;
    }

    public void setVms(List<AzureGroupVm> vms) {
        this.vms = vms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    //endregion
}
