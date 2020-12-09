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
public class AzureV3GroupStatus {
    //region members
    private String               status;
    private List<AzureV3GroupVm> vms;
    private String               description;
    //endregion

    //region getters & setters
    public AzureV3GroupStatus() {
        this.vms = new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AzureV3GroupVm> getVms() {
        return vms;
    }

    public void setVms(List<AzureV3GroupVm> vms) {
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
