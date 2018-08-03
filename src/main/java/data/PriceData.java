package data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = true)
public class PriceData {
    private Long time;
    private String instrument;
    private PriceType priceType;

    private Double tickAskPrice;
    private Double tickBidPrice;
    private Double tickAskVolume;
    private Double tickBidVolume;

    private PeriodData barPeriod;

    private Double barAskOpen;
    private Double barAskClose;
    private Double barAskLow;
    private Double barAskHigh;
    private Double barAskVolume;

    private Double barBidOpen;
    private Double barBidClose;
    private Double barBidLow;
    private Double barBidHigh;
    private Double barBidVolume;
}
