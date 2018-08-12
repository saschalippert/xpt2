package util;

import org.apache.commons.math.util.MathUtils;

public class UtilMath {

    public static boolean isEqual(double a, double b, int precision) {

        double ar = round(a, precision);
        double br = round(b, precision);

        return isEqual(ar, br);
    }

    public static boolean isEqual(double a, double b) {

        return compare(a, b) == 0;
    }

    public static boolean isZero(double a) {

        return MathUtils.compareTo(a, 0.0, MathUtils.EPSILON) == 0;
    }

    public static boolean isZero(double a, int precision) {

        double ar = round(a, precision);

        return isZero(ar);
    }

    public static double round(double a, int precision) {

        return MathUtils.round(a, precision);
    }

    public static int compare(double a, double b) {

        return MathUtils.compareTo(a, b, MathUtils.EPSILON);
    }

    public static int compare(double a, double b, int precision) {

        double ar = round(a, precision);
        double br = round(b, precision);

        return compare(ar, br);
    }

    public static double scale(double min1, double max1, double min2, double max2, double value) {
        double range1 = max1 - min1;
        double range2 = max2 - min2;
        return (((value - min1) * range2) / range1) + min2;
    }

    public static double scale(double min1, double max1, double value) {
        return (value - min1) / (max1 - min1);
    }

    public static double sign(double value) {
        if (value < 0) {
            return -1;
        }

        return 1;
    }
}
