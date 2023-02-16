package hudson.plugins.spotinst.common;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 21/03/2017.
 */
public class TimeUtils {
    //region methods
    public static Boolean isTimePassedInSeconds(Date from, Integer amount){
        Boolean retVal = isTimePassed(from, amount, Calendar.SECOND);
        return retVal;
    }

    public static Boolean isTimePassedInMinutes(Date from, Integer amount){
        Boolean retVal = isTimePassed(from, amount, Calendar.MINUTE);
        return retVal;
    }

    public static long getDiffInMinutes(Date currDate, Date previousDate) {
        long retVal;

        long diffInMs = currDate.getTime() - previousDate.getTime();
        retVal = TimeUnit.MILLISECONDS.toMinutes(diffInMs);

        return retVal;
    }
    //endregion

    //region private methods
    private static Boolean isTimePassed(Date from, Integer amount, Integer timeUnit) {
        Boolean  retVal   = false;
        Date     now      = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(timeUnit, amount);
        Date timeToPass = calendar.getTime();

        if (now.after(timeToPass)) {
            retVal = true;
        }

        return retVal;
    }
    //endregion
}
