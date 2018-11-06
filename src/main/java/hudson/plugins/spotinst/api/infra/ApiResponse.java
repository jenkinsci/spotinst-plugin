package hudson.plugins.spotinst.api.infra;

import java.util.List;

/**
 * Created by aharontwizer on 8/24/15.
 */
public class ApiResponse<T> {

    private boolean        requestSucceed;
    private T              value;
    private List<ApiError> errors;

    public ApiResponse(T value) {
        this.value = value;
        this.requestSucceed = true;
    }

    public ApiResponse(boolean requestSucceed) {
        this.requestSucceed = requestSucceed;
    }

    public ApiResponse(List<ApiError> errors) {
        this.requestSucceed = false;
        this.errors = errors;
    }

    public boolean isRequestSucceed() {
        return requestSucceed;
    }

    public T getValue() {
        return value;
    }

    public List<ApiError> getErrors() {
        return errors;
    }
}
