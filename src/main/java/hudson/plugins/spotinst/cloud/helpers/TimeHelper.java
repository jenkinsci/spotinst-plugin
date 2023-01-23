package hudson.plugins.spotinst.cloud.helpers;

import java.util.Calendar;
import java.util.Date;

public class TimeHelper {
    //region constants
    private static final Integer REDIS_TTL_IN_SECONDS                            = 60 * 3;
    private static final Integer MILI_TO_SECONDS                                 = 1000;
    public static final  Integer SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS = generateSuspendedGroupFetchingTime();
    //endregion

    //region methods
    public static Boolean isTimePassedInSeconds(Date from) {
        Boolean  retVal   = false;
        Date     now      = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.SECOND, SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS / MILI_TO_SECONDS);
        Date timeToPass = calendar.getTime();

        if (now.after(timeToPass)) {
            retVal = true;
        }

        return retVal;
    }

    public static Integer getRedisTimeToLeaveInSeconds() {
        return REDIS_TTL_IN_SECONDS;
    }
    //endregion

    //region private methods
    private static Integer generateSuspendedGroupFetchingTime() {
        Integer retVal = MILI_TO_SECONDS * REDIS_TTL_IN_SECONDS + 10;

        return retVal;
    }
    //endregion
}
