/*
 * Copyright (c) 2017 Dukascopy (Suisse) SA. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Dukascopy (Suisse) SA or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. DUKASCOPY (SUISSE) SA ("DUKASCOPY")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL DUKASCOPY OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF DUKASCOPY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

import com.dukascopy.api.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.BarData;
import data.PeriodData;
import data.TickData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class TickSaver implements IStrategy {
    private IEngine engine = null;
    private IConsole console;
    private Output output;
    private Kryo kryo;
    private BiMap<Period, PeriodData> periodMap = HashBiMap.create();

    public void onStart(IContext context) throws JFException {

        periodMap.put(Period.ONE_SEC, PeriodData.ONE_SEC);
        periodMap.put(Period.TWO_SECS, PeriodData.TWO_SECS);
        periodMap.put(Period.TEN_SECS, PeriodData.TEN_SECS);
        periodMap.put(Period.TWENTY_SECS, PeriodData.TWENTY_SECS);
        periodMap.put(Period.THIRTY_SECS, PeriodData.THIRTY_SECS);

        periodMap.put(Period.ONE_MIN, PeriodData.ONE_MIN);
        periodMap.put(Period.FIVE_MINS, PeriodData.FIVE_MINS);
        periodMap.put(Period.TEN_MINS, PeriodData.TEN_MINS);
        periodMap.put(Period.FIFTEEN_MINS, PeriodData.FIFTEEN_MINS);
        periodMap.put(Period.TWENTY_MINS, PeriodData.TWENTY_MINS);
        periodMap.put(Period.THIRTY_MINS, PeriodData.THIRTY_MINS);

        periodMap.put(Period.ONE_HOUR, PeriodData.ONE_HOUR);
        periodMap.put(Period.FOUR_HOURS, PeriodData.FOUR_HOURS);

        periodMap.put(Period.DAILY, PeriodData.DAILY);

        kryo = new Kryo();

        try {
            output = new Output(new FileOutputStream("C:\\temp\\file.bin"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        engine = context.getEngine();
        this.console = context.getConsole();
        console.getOut().println("Started");
    }

    public void onStop() throws JFException {
        console.getOut().println("Stopped");
        output.close();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        TickData tickData = new TickData();
        tickData.setAskPrice(tick.getAsk());
        tickData.setAskVolume(tick.getAskVolume());
        tickData.setBidPrice(tick.getBid());
        tickData.setBidVolume(tick.getBidVolume());
        tickData.setTime(tick.getTime());
        tickData.setInstrument(instrument.getName());

        kryo.writeClassAndObject(output, tickData);
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        BarData barData = new BarData();

        barData.setAskClose(askBar.getClose());
        barData.setAskHigh(askBar.getHigh());
        barData.setAskLow(askBar.getLow());
        barData.setAskOpen(askBar.getOpen());
        barData.setAskVolume(askBar.getClose());

        barData.setBidClose(bidBar.getClose());
        barData.setBidHigh(bidBar.getHigh());
        barData.setBidLow(bidBar.getLow());
        barData.setBidOpen(bidBar.getOpen());
        barData.setBidVolume(bidBar.getVolume());


        barData.setPeriodData(periodMap.get(period));
        barData.setInstrument(instrument.getName());
        barData.setTime(askBar.getTime());

        kryo.writeClassAndObject(output, barData);
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }
}