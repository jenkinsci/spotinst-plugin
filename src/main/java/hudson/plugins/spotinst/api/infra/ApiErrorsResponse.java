package hudson.plugins.spotinst.api.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by aharontwizer on 7/18/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiErrorsResponse {

    public ApiErrorsResponse()
    {

    }

    @JsonProperty
    private ErrorsInnerResponse response;

    public ErrorsInnerResponse getResponse() {
        return response;
    }

    public void setResponse(ErrorsInnerResponse response) {
        this.response = response;
    }
}

