package util;

import java.util.Currency;

import com.dukascopy.api.IContext;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;

public class UtilPrice {

	public final static double	DEFAULT_LOT_SIZE	= 1000000;

	private UtilPrice() {
	}

	public static double convertAmount(Currency from, Currency to, double amount, IContext context) {
		if (from.equals(to)) {
			return amount;
		} else {
			Instrument i = Instrument.fromString(to.getCurrencyCode() + "/" + from.getCurrencyCode());
			try {
				if (i != null) {
					return amount / context.getHistory().getLastTick(i).getBid();
				} else {
					i = Instrument.fromInvertedString(to.getCurrencyCode() + "/" + from.getCurrencyCode());
					return amount * context.getHistory().getLastTick(i).getBid();
				}
			} catch (JFException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static double getLotSize() {

		return DEFAULT_LOT_SIZE;
	}

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

	public static double getProfitLossPips(boolean buy, double openPrice, double price, double pipValue) {

		if (buy) {
			return UtilMath.round((price - openPrice) / pipValue, 1);
		} else {
			return UtilMath.round((openPrice - price) / pipValue, 1);
		}
	}

	public static double getProfitLoss(double profitLossPips, double pipValue, double amount, double commission) {

		return profitLossPips * pipValue * amount * UtilPrice.getLotSize() - commission;
	}

	public static double getStopLossPrice(boolean buy, double price, long stopLossPips, double pipValue) {

		if (buy) {
			return price - stopLossPips * pipValue;
		} else {
			return price + stopLossPips * pipValue;
		}
	}

	public static double getStopLossPips(boolean buy, double price, double stopLossPrice, double pipValue) {

		if (buy) {
			return (price - stopLossPrice) / pipValue;
		} else {
			return (stopLossPrice - price) / pipValue;
		}
	}

	public static double getTakeProfitPrice(boolean buy, double price, long takeProfitPips, double pipValue) {

		if (buy) {
			return price + takeProfitPips * pipValue;
		} else {
			return price - takeProfitPips * pipValue;
		}
	}

	public static double getTakeProfitPips(boolean buy, double price, long takeProfitPrice, double pipValue) {

		if (buy) {
			return (takeProfitPrice - price) / pipValue;
		} else {
			return (price - takeProfitPrice) / pipValue;
		}
	}

	public static double getSharpestStopLoss(boolean buy, int pipScale, double... stopLosses) {

		double best = stopLosses[0];

		if (buy) {
			for (double d : stopLosses) {
				if (d > best) {
					best = d;
				}
			}
		} else {
			for (double d : stopLosses) {
				if ((d < best && !UtilMath.isZero(d, pipScale)) || UtilMath.isZero(best, pipScale)) {
					best = d;
				}
			}
		}

		return best;
	}

	public static double getSharpestTakeProfit(boolean buy, int pipScale, double... takeProfits) {

		double best = takeProfits[0];

		if (buy) {
			for (double d : takeProfits) {
				if ((d < best && !UtilMath.isZero(d, pipScale)) || UtilMath.isZero(best, pipScale)) {
					best = d;
				}
			}
		} else {
			for (double d : takeProfits) {
				if (d > best) {
					best = d;
				}
			}
		}

		return best;
	}

	public static boolean isStopLoss(boolean buy, double price, double stopLossPrice, int pipScale) {

		if (UtilMath.isZero(stopLossPrice, pipScale)) {
			return false;
		} else {
			boolean longSl = buy && price < stopLossPrice;
			boolean shortSl = !buy && price > stopLossPrice;

			return longSl || shortSl;
		}
	}

	public static boolean isTakeProfit(boolean buy, double price, double takeProfitPrice, int pipScale) {

		if (UtilMath.isZero(takeProfitPrice, pipScale)) {
			return false;
		} else {
			boolean longSl = buy && price > takeProfitPrice;
			boolean shortSl = !buy && price < takeProfitPrice;

			return longSl || shortSl;
		}
	}

	public static double getCommission(double deposit, double equity, double amount) {
		double deposits[] = { 0, 5000, 10000, 25000, 50000, 250000, 500000, 1000000, 5000000, 10000000 };
		double commissions[] = { 48, 38, 32, 25, 18, 16, 14, 12, 9, 5 };
		int depositCom = 0;
		int equityCom = 0;

		for (int i = 0; i < 10; i++) {
			if (deposit >= deposits[i]) {
				depositCom = i;
			} else {
				break;
			}
		}

		for (int i = 0; i < 10; i++) {
			if (equity >= deposits[i]) {
				equityCom = i;
			} else {
				break;
			}
		}

		int min = Math.min(depositCom, equityCom);

		return UtilMath.round(commissions[min] * amount, 1);
	}
}
