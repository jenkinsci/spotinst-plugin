package hudson.plugins.spotinst.common;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by ohadmuchnik on 21/03/2017.
 */
public class TimeUtils {
    public static Boolean isTimePassedInMinutes(Date from, Integer minutes) {
        Boolean  retVal   = false;
        Date     now      = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.MINUTE, minutes);
        Date timeToPass = calendar.getTime();

        if (now.after(timeToPass)) {
            retVal = true;
        }

        return retVal;
    }

    public static long getDiffInMinutes(Date currDate, Date previousDate) {
        long retVal;

        long diffInMs = currDate.getTime() - previousDate.getTime();
        retVal = TimeUnit.MILLISECONDS.toMinutes(diffInMs);

        return retVal;
    }
}
