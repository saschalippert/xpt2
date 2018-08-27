package tester;/*
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;

import startegies.TickSaverGson;

public class TesterMain {

    private static final Logger logger = LoggerFactory.getLogger(TesterMain.class);

    private static final Calendar fromDate = new GregorianCalendar(2018, 0, 1);
    private static final Calendar toDate = new GregorianCalendar(2018, 5, 1);
    private static final IStrategy strategy = new TickSaverGson();

    private static String jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp";
    private static String userName = "DEMO2ZggWH";
    private static String password = "ZggWH";
    private static ITesterClient client;

    static {
	fromDate.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
	toDate.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
    }

    public static void main(String[] args) throws Exception {
        client = TesterFactory.getDefaultInstance();

        setSystemListener();
        tryToConnect();
        subscribeToInstruments();
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 50000);
        loadData();


	logger.info("Starting strategy");

	client.startStrategy(strategy, getLoadingProgressListener());
    }

    private static void setSystemListener() {
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
		logger.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId) {
		logger.info("Strategy stopped: " + processId);
            }

            @Override
            public void onConnect() {
		logger.info("Connected");
            }

            @Override
            public void onDisconnect() {
		logger.info("Disconnected");
            }
        });
    }

    private static void tryToConnect() throws Exception {
	logger.info("Connecting...");

        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
	    logger.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }
    }

    private static void subscribeToInstruments() {
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
	logger.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
    }

    private static void loadData() throws InterruptedException, java.util.concurrent.ExecutionException {

        client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, fromDate.getTimeInMillis(), toDate.getTimeInMillis());
	logger.info("Downloading data");
	Future<?> future = client.downloadData(getLoadingProgressListener());
        future.get();
    }

    private static LoadingProgressListener getLoadingProgressListener() {
        return new LoadingProgressListener() {
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
		logger.info(information);
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
		logger.info("Loading finished, " + (allDataLoaded ? "" : "(not all data loaded)"));
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        };
    }
}
