package util;

import com.dukascopy.api.Period;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.PeriodData;

public class PeriodUtil {

    private static BiMap<Period, PeriodData> periodMap = HashBiMap.create();

    static {
        periodMap.put(Period.ONE_SEC, PeriodData.ONE_SEC);
        periodMap.put(Period.TWO_SECS, PeriodData.TWO_SECS);
        periodMap.put(Period.TEN_SECS, PeriodData.TEN_SECS);
        periodMap.put(Period.TWENTY_SECS, PeriodData.TWENTY_SECS);
        periodMap.put(Period.THIRTY_SECS, PeriodData.THIRTY_SECS);

        periodMap.put(Period.ONE_MIN, PeriodData.ONE_MIN);
        periodMap.put(Period.FIVE_MINS, PeriodData.FIVE_MINS);
        periodMap.put(Period.TEN_MINS, PeriodData.TEN_MINS);
        periodMap.put(Period.FIFTEEN_MINS, PeriodData.FIFTEEN_MINS);
        periodMap.put(Period.TWENTY_MINS, PeriodData.TWENTY_MINS);
        periodMap.put(Period.THIRTY_MINS, PeriodData.THIRTY_MINS);

        periodMap.put(Period.ONE_HOUR, PeriodData.ONE_HOUR);
        periodMap.put(Period.FOUR_HOURS, PeriodData.FOUR_HOURS);

        periodMap.put(Period.DAILY, PeriodData.DAILY);
    }

    public static Period map(PeriodData data) {
        return periodMap.inverse().get(data);
    }

    public static PeriodData map(Period period) {
        return periodMap.get(period);
    }
}
