package hudson.plugins.spotinst.api.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by aharontwizer on 7/18/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorsInnerResponse  extends BaseInnerResponse {
    public ErrorsInnerResponse() {

    }

    @JsonProperty
    private List<ApiError> errors;

    public List<ApiError> getErrors() {
        return errors;
    }

    public void setErrors(List<ApiError> errors) {
        this.errors = errors;
    }
}
