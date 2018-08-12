package data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = true)
public class Order {
    private LocalDateTime dateTime;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private boolean buy;
    private boolean closeable;
}
