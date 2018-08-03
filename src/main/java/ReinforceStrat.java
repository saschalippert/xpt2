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
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ReinforceStrat implements IStrategy {
    private IEngine engine = null;
    private IConsole console;
    private MultiLayerNetwork model;
    private IIndicators indicators;
    private Period periodEntry = Period.ONE_MIN;
    private int tagCounter;
    private IOrder order;
    private double buyChance;

    public void onStart(IContext context) throws JFException {

        File file = new File(getClass().getSimpleName() + ".zip");

        if (false) {
            try {
                model = ModelSerializer.restoreMultiLayerNetwork(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            int seed = 12345;
            int nIn = 8;
            int nHidden = 3;
            int nOut = 2;

            Nd4j.getRandom().setSeed(seed);
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .seed(seed)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(1e-4))
                    .list()
                    .layer(0, new DenseLayer.Builder().nIn(nIn)
                            .nOut(nHidden)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(1, new DenseLayer.Builder().nIn(nHidden)
                            .nOut(nHidden)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(2, new DenseLayer.Builder().nIn(nHidden)
                            .nOut(nOut)
                            .activation(Activation.SOFTMAX)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .backprop(true)
                    .build();

            model = new MultiLayerNetwork(conf);
            model.init();
        }

        engine = context.getEngine();
        indicators = context.getIndicators();
        this.console = context.getConsole();
        console.getOut().println("Started");
    }

    public void onStop() throws JFException {
        console.getOut().println("Stopped");

        try {
            ModelSerializer.writeModel(model, new File(getClass().getSimpleName() + ".zip"), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {

    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

        if (order != null) {
            return;
        }

        if (!period.equals(periodEntry)) {
            return;
        }

        LocalDateTime triggerTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(askBar.getTime()), TimeZone.getDefault().toZoneId());

        List<DayOfWeek> weekend = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        if (weekend.contains(triggerTime.getDayOfWeek())) {
            return;
        }

        if (triggerTime.getHour() < 9 || triggerTime.getHour() > 17) {
            return;
        }

        double min = indicators.min(instrument, Period.ONE_HOUR, OfferSide.ASK, IIndicators.AppliedPrice.LOW, 24, Filter.WEEKENDS, 2,
                askBar.getTime(), 0)[0];
        double max = indicators.max(instrument, Period.ONE_HOUR, OfferSide.ASK, IIndicators.AppliedPrice.HIGH, 24, Filter.WEEKENDS, 2,
                askBar.getTime(), 0)[0];

        double minWeek = indicators.min(instrument, Period.DAILY, OfferSide.ASK, IIndicators.AppliedPrice.LOW, 7, Filter.WEEKENDS, 2,
                askBar.getTime(), 0)[0];
        double maxWeek = indicators.max(instrument, Period.DAILY, OfferSide.ASK, IIndicators.AppliedPrice.HIGH, 7, Filter.WEEKENDS, 2,
                askBar.getTime(), 0)[0];

        INDArray input = Nd4j.zeros(1, 8);

        input.putScalar(0, min / 2.0);
        input.putScalar(1, max / 2.0);
        input.putScalar(2, askBar.getClose() / 2.0);
        input.putScalar(3, askBar.getOpen() / 2.0);
        input.putScalar(4, askBar.getHigh() / 2.0);
        input.putScalar(5, askBar.getLow() / 2.0);
        input.putScalar(6, minWeek / 2.0);
        input.putScalar(7, maxWeek / 2.0);

        model.setInput(input);
        List<INDArray> out = model.feedForward(true, false);

        double rand = Math.random();
        buyChance = out.get(3).getDouble(0);
        boolean buy = rand <= buyChance;


        if (!buy) {
            order = engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELL, 0.001, 0, 0, bidBar.getClose()
                    + instrument.getPipValue() * 10, bidBar.getClose() - instrument.getPipValue() * 10);
        } else {
            order = engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUY, 0.001, 0, 0, askBar.getClose()
                    - instrument.getPipValue() * 10, askBar.getClose() + instrument.getPipValue() * 10);
        }

        console.getOut().println("bar");
    }

    public void onMessage(IMessage message) throws JFException {
        if (IMessage.Type.ORDER_FILL_OK.equals(message.getType())) {
            console.getOut().println("open");
        } else if (IMessage.Type.ORDER_CLOSE_OK.equals(message.getType())) {
            console.getOut().println("close");

            double mul = 1;

            if (!order.isLong()) {
                mul = -1;
            }

            double sellChance = 1 - buyChance;

            double score = (order.getClosePrice() - order.getOpenPrice()) * mul;

            INDArray error = Nd4j.zeros(1, 2);

            if (score > 0) {
                if (order.isLong()) {
                    error.putScalar(0, -1);
                } else {
                    error.putScalar(1, -1);
                }
            } else {
                if (order.isLong()) {
                    error.putScalar(0, 1);
                } else {
                    error.putScalar(1, 1);
                }
            }

            backprop(error);

            order = null;
        } else if (IMessage.Type.ORDER_CLOSE_REJECTED.equals(message.getType())) {
            console.getOut().println("ORDER_CLOSE_REJECTED");
        } else if (IMessage.Type.ORDER_FILL_REJECTED.equals(message.getType())) {
            console.getOut().println("ORDER_FILL_REJECTED");
        } else if (IMessage.Type.ORDER_SUBMIT_REJECTED.equals(message.getType())) {
            console.getOut().println("ORDER_SUBMIT_REJECTED");
        }
    }

    public void onAccount(IAccount account) throws JFException {
    }

    private void backprop(INDArray error) {
        Pair<Gradient, INDArray> p = model.backpropGradient(error, null);
        Gradient gradient = p.getFirst();
        int iteration = 0;
        int epoch = 0;
        model.getUpdater().update(model, gradient, iteration, epoch, 1, LayerWorkspaceMgr.noWorkspaces());
        INDArray updateVector = gradient.gradient();
        model.params().subi(updateVector);
    }

    protected String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = label.substring(0, 2) + label.substring(3, 5);
        label = label + (tagCounter++);
        label = label.toLowerCase();
        return label;
    }
}