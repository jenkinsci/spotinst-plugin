package hudson.plugins.spotinst.model.aws.stateful;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by sitay on 30/08/23.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsStatefulDeallocationConfig {
    //region members
    private Boolean shouldDeleteImages;
    private Boolean shouldDeleteNetworkInterfaces;
    private Boolean shouldDeleteVolumes;
    private Boolean shouldDeleteSnapshots;
    //endregion

    //region getters & setters
    public Boolean getShouldDeleteImages() {
        return shouldDeleteImages;
    }

    public void setShouldDeleteImages(Boolean shouldDeleteImages) {
        this.shouldDeleteImages = shouldDeleteImages;
    }

    public Boolean getShouldDeleteNetworkInterfaces() {
        return shouldDeleteNetworkInterfaces;
    }

    public void setShouldDeleteNetworkInterfaces(Boolean shouldDeleteNetworkInterfaces) {
        this.shouldDeleteNetworkInterfaces = shouldDeleteNetworkInterfaces;
    }

    public Boolean getShouldDeleteVolumes() {
        return shouldDeleteVolumes;
    }

    public void setShouldDeleteVolumes(Boolean shouldDeleteVolumes) {
        this.shouldDeleteVolumes = shouldDeleteVolumes;
    }

    public Boolean getShouldDeleteSnapshots() {
        return shouldDeleteSnapshots;
    }

    public void setShouldDeleteSnapshots(Boolean shouldDeleteSnapshots) {
        this.shouldDeleteSnapshots = shouldDeleteSnapshots;
    }
    //endregion
}
