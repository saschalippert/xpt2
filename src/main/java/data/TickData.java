package data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = true)
public class TickData {
    private Long time;
    private String instrument;

    private Double askPrice;
    private Double bidPrice;
    private Double askVolume;
    private Double bidVolume;
}
