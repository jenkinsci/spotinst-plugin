package hudson.plugins.spotinst.common;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by ohadmuchnik on 21/03/2017.
 */
public class TimeUtils {
    public static boolean isTimePassed(Date from, Integer minutes) {
        boolean  retVal   = false;
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
}
