package data;

import java.time.LocalDateTime;


public class Order {

	private LocalDateTime dateTime;
	private double entryPrice;
	private double stopLoss;
	private double takeProfit;
	private boolean buy;
	private boolean closeable;

	public LocalDateTime getDateTime() {

		return dateTime;
	}

	public double getEntryPrice() {

		return entryPrice;
	}

	public double getStopLoss() {

		return stopLoss;
	}

	public double getTakeProfit() {

		return takeProfit;
	}

	public boolean isBuy() {

		return buy;
	}

	public boolean isCloseable() {

		return closeable;
	}

	public void setBuy(boolean buy) {

		this.buy = buy;
	}

	public void setCloseable(boolean closeable) {

		this.closeable = closeable;
	}

	public void setDateTime(LocalDateTime dateTime) {

		this.dateTime = dateTime;
	}

	public void setEntryPrice(double entryPrice) {

		this.entryPrice = entryPrice;
	}

	public void setStopLoss(double stopLoss) {

		this.stopLoss = stopLoss;
	}

	public void setTakeProfit(double takeProfit) {

		this.takeProfit = takeProfit;
	}
}
