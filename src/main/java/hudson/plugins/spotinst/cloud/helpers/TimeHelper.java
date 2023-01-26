package hudson.plugins.spotinst.cloud.helpers;

import hudson.plugins.spotinst.common.Constants;

import java.util.Calendar;
import java.util.Date;

public class TimeHelper {
    //region constants
    private static final Integer MILI_TO_SECONDS                                 = 1000;
    public static final  Integer SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS =
            generateSuspendedGroupInitializingTimeInMilli();
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

    public static Integer getLockTimeToLeaveInSeconds() {
        return Constants.LOCK_TIME_TO_LIVE_IN_SECONDS;
    }
    //endregion

    //region private methods
    private static Integer generateSuspendedGroupInitializingTimeInMilli() {
        Integer retVal = MILI_TO_SECONDS * getLockTimeToLeaveInSeconds() + 10;

        return retVal;
    }
    //endregion
}
