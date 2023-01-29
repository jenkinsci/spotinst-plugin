package hudson.plugins.spotinst.common;

/**
 * Created by ohadmuchnik on 21/03/2017.
 */
public class Constants {
    public static final Integer PENDING_INSTANCE_TIMEOUT_IN_MINUTES               = 10;
    public static final Integer AZURE_PENDING_INSTANCE_TIMEOUT_IN_MINUTES         = 15;
    public static final Integer SLAVE_OFFLINE_THRESHOLD_IN_MINUTES                = 15;
    public static final Integer REST_CLIENT_CONNECT_TIMEOUT_IN_SECONDS            = 120;
    public static final Integer REST_CLIENT_CONNECTION_REQUEST_TIMEOUT_IN_SECONDS = 120;
    public static final Integer REST_CLIENT_SOCKET_TIMEOUT_IN_SECONDS             = 120;
    public static final String  LOCK_OK_STATUS                                    = "OK";
    public static final Integer LOCK_TIME_TO_LIVE_IN_SECONDS                    = 60 * 3;
    public static final Integer MILI_TO_SECONDS                                 = 1000;
    public static final Integer SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS =
            generateSuspendedGroupInitializingTimeInMilli();


    //region private methods
    private static Integer generateSuspendedGroupInitializingTimeInMilli() {
        Integer retVal = MILI_TO_SECONDS * LOCK_TIME_TO_LIVE_IN_SECONDS + 10;

        return retVal;
    }
    //endregion
}
