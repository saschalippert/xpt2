package data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper=true, includeFieldNames=true)
public class TickData extends PriceData {
    private double askPrice;
    private double bidPrice;
    private double askVolume;
    private double bidVolume;
}
