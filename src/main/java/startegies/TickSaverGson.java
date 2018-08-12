package startegies;/*
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.TickData;
import util.FileUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TickSaverGson implements IStrategy {
    private IEngine engine = null;
    private IConsole console;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    LocalDateTime currentDate;

    List<TickData> dataList = new ArrayList<>();

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        this.console = context.getConsole();

        console.getOut().println("Started");
    }

    public void onStop() throws JFException {
        console.getOut().println("Stopped");
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        TickData tickData = new TickData();
        tickData.setAskPrice(tick.getAsk());
        tickData.setAskVolume(tick.getAskVolume());
        tickData.setBidPrice(tick.getBid());
        tickData.setBidVolume(tick.getBidVolume());

        tickData.setTime(tick.getTime());
        tickData.setInstrument(instrument.name());

        try {
            rollWriter(tickData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {

    }

    private void rollWriter(TickData priceData) throws IOException {

        LocalDateTime newDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(priceData.getTime()), ZoneId.systemDefault()).truncatedTo(ChronoUnit.HOURS);

        if (currentDate == null) {
            currentDate = newDate;
        }

        if (newDate.isAfter(currentDate)) {

            console.getOut().println(currentDate.getDayOfMonth() + " -> " + newDate.getDayOfMonth());

            Path path = FileUtil.getFile(newDate, priceData.getInstrument());

            if (path.toFile().exists()) {
                throw new RuntimeException("file exists");
            }

            Files.createDirectories(path.getParent());

            try (Writer writer = new FileWriter(path.toFile())) {
                gson.toJson(dataList.toArray(), writer);
            }

            dataList.clear();
            currentDate = newDate;
        }

        dataList.add(priceData);
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }
}