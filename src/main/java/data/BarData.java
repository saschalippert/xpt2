package data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper=true, includeFieldNames=true)
public class BarData extends PriceData {

    private PeriodData periodData;

    private double askOpen;
    private double askClose;
    private double askLow;
    private double askHigh;
    private double askVolume;

    private double bidOpen;
    private double bidClose;
    private double bidLow;
    private double bidHigh;
    private double bidVolume;
}
