package hudson.plugins.spotinst.api.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseServiceItemsResponse<T> {

    public BaseServiceItemsResponse() {
    }

    @JsonProperty
    public ItemsInnerResponse<T> getResponse() {
        return response;
    }

    @JsonProperty
    public void setResponse(ItemsInnerResponse<T> response) {
        this.response = response;
    }

    @JsonProperty
    private ItemsInnerResponse<T> response;
}

