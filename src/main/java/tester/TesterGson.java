package tester;

import com.dukascopy.api.Instrument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.Order;
import data.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import startegies.CustomStrategy;
import util.DateUtil;
import util.FileUtil;
import util.UtilPrice;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class TesterGson {

    private static Logger logger = LoggerFactory.getLogger(TesterGson.class);
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private LocalDateTime start = LocalDateTime.of(2018, 01, 07, 0, 0);
    private LocalDateTime stop = LocalDateTime.of(2018, 03, 01, 0, 0);

    private Duration preLoadDuration = Duration.ofDays(5);

    private String instrument = Instrument.EURUSD.name();
    private CustomStrategy customStrategy;

    private Order order;
    private double pipsTotal;

    private int counterSell;
    private int counterBuy;

    public static void main(String[] args) {
        TesterGson testerGson = new TesterGson();
        while (true) {
            testerGson.start();
        }
    }

    public TesterGson() {
        customStrategy = new CustomStrategy(this);
    }

    public void start() {
        LocalDateTime current = start.minusDays(preLoadDuration.toDays());

        while (current.isBefore(start)) {
            Arrays.stream(loadData(current)).forEach(customStrategy::preLoad);
            current = current.plusHours(1);
        }

        pipsTotal = 0;
        counterBuy = 0;
        counterSell = 0;

        while (current.isBefore(stop)) {
            Arrays.stream(loadData(current)).forEach(this::process);
            current = current.plusHours(1);
        }

        customStrategy.stop();

        logger.info("total pips " + pipsTotal);
        logger.info("buy# " + counterBuy);
        logger.info("sell# " + counterSell);
    }

    private TickData[] loadData(LocalDateTime date) {
        Path path = FileUtil.getFile(date, instrument);

        if (path.toFile().exists()) {
            try (Reader reader = new FileReader(path.toFile())) {
                return gson.fromJson(reader, TickData[].class);

            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        } else {
            //logger.warn("no file " + path);
        }

        return new TickData[0];
    }

    public void process(TickData tickData) {
        customStrategy.process(tickData);

        if (order != null) {
            checkClose(order, tickData);
        }
    }

    private void checkClose(Order order, TickData tickData) {
        Instrument instrument = Instrument.valueOf(tickData.getInstrument());

        boolean buy = order.isBuy();
        double price = UtilPrice.getClosePrice(buy, tickData.getAskPrice(), tickData.getBidPrice());
        double pips = UtilPrice.getProfitLossPips(buy, order.getEntryPrice(), price, instrument.getPipValue());

        if (order.isCloseable()) {
            close(pips);
            return;
        }

        if (buy) {
            if (price >= order.getTakeProfit()) {
                close(pips);
            } else if (price <= order.getStopLoss()) {
                close(pips);
            }
        } else {
            if (price <= order.getTakeProfit()) {
                close(pips);
            } else if (price >= order.getStopLoss()) {
                close(pips);
            }
        }
    }


    public void close(double pips) {
        pipsTotal = pipsTotal + pips;
        //logger.info("pips " + pips);

        customStrategy.close(order, pips);

        order = null;
    }

    public void openOrder(Order order) {
        this.order = order;

        if (order.isBuy()) {
            counterBuy++;
        } else {
            counterSell++;
        }
    }

    public Order getOrder() {
        return this.order;
    }
}
