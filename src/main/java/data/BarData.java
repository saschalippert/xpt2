package data;

public class BarData {

	private Long time;
	private String instrument;
	private Double askPrice;
	private Double bidPrice;
	private Double askVolume;
	private Double bidVolume;

	public Double getAskPrice() {

		return askPrice;
	}

	public Double getAskVolume() {

		return askVolume;
	}

	public Double getBidPrice() {

		return bidPrice;
	}

	public Double getBidVolume() {

		return bidVolume;
	}

	public String getInstrument() {

		return instrument;
	}

	public Long getTime() {

		return time;
	}

	public void setAskPrice(Double askPrice) {

		this.askPrice = askPrice;
	}

	public void setAskVolume(Double askVolume) {

		this.askVolume = askVolume;
	}

	public void setBidPrice(Double bidPrice) {

		this.bidPrice = bidPrice;
	}

	public void setBidVolume(Double bidVolume) {

		this.bidVolume = bidVolume;
	}

	public void setInstrument(String instrument) {

		this.instrument = instrument;
	}

	public void setTime(Long time) {

		this.time = time;
	}
}
