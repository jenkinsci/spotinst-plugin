package hudson.plugins.spotinst.cloud.helpers;

import java.util.Calendar;
import java.util.Date;

public class TimeHelper {
    //region members
    private static final Integer redisTimeToLeaveInSeconds                       = 60 * 3;
    private static final Integer miliToSeconds                                   = 1000;
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
        return redisTimeToLeaveInSeconds;
    }
    //endregion

    //region private methods
    private static Integer generateSuspendedGroupFetchingTime() {
        Integer retVal = miliToSeconds * redisTimeToLeaveInSeconds + 10;

        return retVal;
    }
    //endregion
}
