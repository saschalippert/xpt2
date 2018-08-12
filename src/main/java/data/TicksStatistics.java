package data;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TicksStatistics {
    private SummaryStatistics descriptiveStatistics = new SummaryStatistics();

    private Duration duration;
    private List<Pair<Double, LocalDateTime>> dataList = new LinkedList<>();

    public TicksStatistics(Duration duration) {
        this.duration = duration;
    }

    public void add(double value, LocalDateTime localDateTime) {
        descriptiveStatistics.clear();

        dataList.add(new Pair<>(value, localDateTime));

        LocalDateTime oldest = localDateTime.minus(duration);

        for (Iterator<Pair<Double, LocalDateTime>> iterator = dataList.iterator(); iterator.hasNext(); ) {
            Pair<Double, LocalDateTime> pair = iterator.next();

            if (pair.getValue().isBefore(oldest)) {
                iterator.remove();
            } else {
                descriptiveStatistics.addValue(pair.getKey());
            }
        }
    }

    public double getMean() {
        return descriptiveStatistics.getMean();
    }

    public double getMin() {
        return descriptiveStatistics.getMin();
    }

    public double getMax() {
        return descriptiveStatistics.getMax();
    }

    public double getStandardDeviation() {
        return descriptiveStatistics.getStandardDeviation();
    }

    public void clear() {
        descriptiveStatistics.clear();
    }
}
