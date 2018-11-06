package hudson.plugins.spotinst.api.infra;

/**
 * Created by aharontwizer on 8/24/15.
 */
public class ApiException extends Exception {

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiException(String message) {
        super(message);
    }
}