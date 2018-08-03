package util;

public class PriceUtil {

    public static double getOpenPrice(boolean buy, double ask, double bid) {

        if (buy) {
            return ask;
        } else {
            return bid;
        }
    }

    public static double getClosePrice(boolean buy, double ask, double bid) {

        if (buy) {
            return bid;
        } else {
            return ask;
        }
    }
}
