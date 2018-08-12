package util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class DateUtil {

    public static boolean isTradingTime(LocalDateTime time) {
        return isTradingHours(time) && !isWeekEnd(time);
    }

    public static boolean isWeekEnd(LocalDateTime time) {
        List<DayOfWeek> weekend = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        return weekend.contains(time.getDayOfWeek());
    }

    public static boolean isTradingHours(LocalDateTime time) {
        return (time.getHour() >= 9 && time.getHour() <= 17);
    }

    public static LocalDateTime getDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

}
