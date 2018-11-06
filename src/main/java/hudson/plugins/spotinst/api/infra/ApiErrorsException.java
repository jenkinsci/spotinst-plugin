package hudson.plugins.spotinst.api.infra;

import java.util.List;

/**
 * Created by aharontwizer on 8/24/15.
 */
public class ApiErrorsException extends ApiException {

    private List<ApiError> errors;

    public List<ApiError> getErrors() {
        return errors;
    }

    public ApiErrorsException(String message, List<ApiError> errors) {
        super(message);
        this.errors = errors;
    }

    public ApiErrorsException(String message, Throwable cause, List<ApiError> errors) {
        super(message, cause);
        this.errors = errors;
    }
}