package hudson.plugins.spotinst.api.infra;


/**
 * Created by aharontwizer on 8/26/15.
 */
public class ExceptionHelper {

    public static <T> ApiResponse<T> handleDalException(ApiException exc) {
        ApiResponse<T> retVal;

        if (exc instanceof ApiErrorsException) {
            ApiErrorsException apiErrorsException = (ApiErrorsException) exc;
            retVal = new ApiResponse<>(apiErrorsException.getErrors());
        }
        else {
            retVal = new ApiResponse<>(false);
        }

        return retVal;
    }
}
