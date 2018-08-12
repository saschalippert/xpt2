package startegies;

import com.dukascopy.api.Instrument;
import data.Order;
import data.TickData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tester.TesterGson;
import util.DateUtil;
import util.UtilMath;
import util.UtilPrice;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class CustomStrategy {

    TesterGson tester;
    private MultiLayerNetwork model;

    private double high = 1.5146;
    private double low = 1.0342;

    int nIn = 6;
    int nOut = 2;

    private double buyChance;

    private static Logger logger = LoggerFactory.getLogger(CustomStrategy.class);

    private DescriptiveStatistics statisticsAskPrice = new DescriptiveStatistics(1000);
    private DescriptiveStatistics statisticsBidPrice = new DescriptiveStatistics(1000);

    private DescriptiveStatistics statisticsAskVolume = new DescriptiveStatistics(1000);
    private DescriptiveStatistics statisticsBidVolume = new DescriptiveStatistics(1000);

    public CustomStrategy(TesterGson tester) {
        this.tester = tester;

        File file = new File(getClass().getSimpleName() + ".zip");

        if (file.exists()) {
            try {
                logger.info("model loaded");
                model = ModelSerializer.restoreMultiLayerNetwork(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            int seed = 12345;
            int nHidden = 32;

            Nd4j.getRandom().setSeed(seed);
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .seed(seed)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam())
                    .list()
                    .layer(0, new DenseLayer.Builder().nIn(nIn)
                            .nOut(32)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(1, new DenseLayer.Builder().nIn(32)
                            .nOut(16)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(2, new DenseLayer.Builder().nIn(16)
                            .nOut(8)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(3, new DenseLayer.Builder().nIn(8)
                            .nOut(4)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(4, new DenseLayer.Builder().nIn(4)
                            .nOut(nOut)
                            .activation(Activation.SOFTMAX)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .backprop(true)
                    .build();

            model = new MultiLayerNetwork(conf);
            model.init();
        }
    }

    public void preLoad(TickData tickData) {
        LocalDateTime dateTime = DateUtil.getDateTime(tickData.getTime());

        statisticsAskPrice.addValue(tickData.getAskPrice());
        statisticsBidPrice.addValue(tickData.getBidPrice());

        statisticsAskVolume.addValue(tickData.getAskVolume());
        statisticsBidVolume.addValue(tickData.getBidVolume());
    }

    public void process(TickData tickData) {
        Instrument instrument = Instrument.valueOf(tickData.getInstrument());

        Order order = tester.getOrder();

        LocalDateTime dateTime = DateUtil.getDateTime(tickData.getTime());

        statisticsAskPrice.addValue(tickData.getAskPrice());
        statisticsBidPrice.addValue(tickData.getBidPrice());

        statisticsAskVolume.addValue(tickData.getAskVolume());
        statisticsBidVolume.addValue(tickData.getBidVolume());

        if (!DateUtil.isTradingTime(dateTime)) {
            if (order != null) {
                order.setCloseable(true);
            }

            return;
        }

        if (order == null) {
            order = new Order();


            INDArray input = Nd4j.zeros(1, nIn);
            input.putScalar(0, UtilMath.scale(low, high, 0, 1, tickData.getAskPrice()));
            input.putScalar(1, UtilMath.scale(low, high, 0, 1, tickData.getBidPrice()));

            input.putScalar(2, UtilMath.scale(statisticsAskPrice.getMin(), statisticsAskPrice.getMax(), 0, 1, tickData.getAskPrice()));
            input.putScalar(3, UtilMath.scale(statisticsBidPrice.getMin(), statisticsBidPrice.getMax(), 0, 1, tickData.getBidPrice()));

            input.putScalar(4, UtilMath.scale(statisticsAskVolume.getMin(), statisticsAskVolume.getMax(), 0, 1, tickData.getAskVolume()));
            input.putScalar(5, UtilMath.scale(statisticsBidVolume.getMin(), statisticsBidVolume.getMax(), 0, 1, tickData.getBidVolume()));

            //input.putScalar(2, UtilMath.scale(low, high, 0, 1, statistics.getMin()));
            //input.putScalar(3, UtilMath.scale(low, high, 0, 1, statistics.getMax()));

            //input.putScalar(4, UtilMath.scale(low, high, 0, 1, statisticsBidPrice.getMin()));
            //input.putScalar(5, UtilMath.scale(low, high, 0, 1, statisticsBidPrice.getMax()));


            //input.putScalar(2, UtilMath.scale(bidPriceOneMinuteStatistics.getMin(), bidPriceOneMinuteStatistics.getMax(), -1, 1, tickData.getAskPrice()));

            model.setInput(input);
            List<INDArray> out = model.feedForward(true, false);

            double rand = Math.random();
            buyChance = out.get(3).getDouble(0);
            boolean buy = rand <= buyChance;

            double entry = UtilPrice.getOpenPrice(buy, tickData.getAskPrice(), tickData.getBidPrice());
            double close = UtilPrice.getClosePrice(buy, tickData.getAskPrice(), tickData.getBidPrice());

            order.setBuy(buy);
            order.setDateTime(dateTime);
            order.setEntryPrice(entry);


            order.setStopLoss(UtilPrice.getStopLossPrice(buy, close, 10, instrument.getPipValue()));
            order.setTakeProfit(UtilPrice.getTakeProfitPrice(buy, close, 10, instrument.getPipValue()));

            tester.openOrder(order);
        }
    }

    public void close(Order order, double pips) {

        boolean buy = order.isBuy();

        INDArray error = Nd4j.zeros(1, nOut);

        if (buy) {
            error.putScalar(0, buyChance * pips * -1);
        } else {
            error.putScalar(1, (1 - buyChance) * pips * -1);
        }

        backprop(error);
    }

    public void stop() {
        statisticsAskPrice.clear();
        statisticsBidPrice.clear();

        statisticsAskVolume.clear();
        statisticsBidVolume.clear();

        try {
            ModelSerializer.writeModel(model, new File(getClass().getSimpleName() + ".zip"), true);
            logger.info("model saved");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
}
