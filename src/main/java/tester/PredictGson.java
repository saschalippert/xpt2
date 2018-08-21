package tester;

import com.dukascopy.api.Instrument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.TickData;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtil;
import util.PlotUtil;
import util.UtilMath;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PredictGson {

    private static Logger logger = LoggerFactory.getLogger(PredictGson.class);
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private LocalDateTime startTrain = LocalDateTime.of(2018, 01, 03, 9, 0);
    private LocalDateTime stopTrain = LocalDateTime.of(2018, 01, 03, 12, 0);
    private Duration durationTest = Duration.ofDays(1);

    private String instrument = Instrument.EURUSD.name();

    private int nIn = 4;
    private int nOut = 2;
    private int seed = 12345;

    private static final int lstmLayer1Size = 256;
    private static final int lstmLayer2Size = 256;
    private static final int denseLayerSize = 32;
    private static final double dropoutRatio = 0.2;
    private static final int truncatedBPTTLength = 22;

    private MultiLayerNetwork model;

    private int windowSize = truncatedBPTTLength;

    private int minMaxValueSize = 4;
    private double[] minValues = new double[minMaxValueSize];
    private double[] maxValues = new double[minMaxValueSize];
    private int counterFiles = 0;

    public static void main(String[] args) {
        PredictGson testerGson = new PredictGson();
        testerGson.init();
        testerGson.loadMinMax();
        testerGson.train();
        testerGson.test();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Plotting");
                testerGson.test();
            }
        });
    }


    public void init() {
        File file = new File(getClass().getSimpleName() + ".zip");

        if (file.exists()) {
            try {
                logger.info("model loaded");
                model = ModelSerializer.restoreMultiLayerNetwork(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Nd4j.getRandom().setSeed(seed);
/*            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .seed(seed)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new RmsProp(1e4))
                    .list()
                    .layer(0, new DenseLayer.Builder()
                            .nIn(nIn)
                            .nOut(nHidden)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(1, new DenseLayer.Builder()
                            .nIn(nHidden)
                            .nOut(nHidden)
                            .activation(Activation.RELU)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(nHidden)
                            .nOut(nOut)
                            .activation(Activation.IDENTITY)
                            .weightInit(WeightInit.XAVIER)
                            .build())
                    .backprop(true)
                    .build();*/

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(seed)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new RmsProp())
                    .l2(1e-4)
                    .list()
                    .layer(0, new LSTM.Builder()
                            .nIn(nIn)
                            .nOut(lstmLayer1Size)
                            .activation(Activation.TANH)
                            .gateActivationFunction(Activation.HARDSIGMOID)
                            .dropOut(dropoutRatio)
                            .build())
                    .layer(1, new LSTM.Builder()
                            .nIn(lstmLayer1Size)
                            .nOut(lstmLayer2Size)
                            .activation(Activation.TANH)
                            .gateActivationFunction(Activation.HARDSIGMOID)
                            .dropOut(dropoutRatio)
                            .build())
                    .layer(2, new DenseLayer.Builder()
                            .nIn(lstmLayer2Size)
                            .nOut(denseLayerSize)
                            .activation(Activation.RELU)
                            .build())
                    .layer(3, new RnnOutputLayer.Builder()
                            .nIn(denseLayerSize)
                            .nOut(nOut)
                            .activation(Activation.IDENTITY)
                            .lossFunction(LossFunctions.LossFunction.MSE)
                            .build())
                    .backpropType(BackpropType.TruncatedBPTT)
                    .tBPTTForwardLength(truncatedBPTTLength)
                    .tBPTTBackwardLength(truncatedBPTTLength)
                    .pretrain(false)
                    .backprop(true)
                    .build();

            model = new MultiLayerNetwork(conf);
            model.init();
        }

        model.setListeners(new ScoreIterationListener(1));
    }

    public void loadMinMax() {
        for (int i = 0; i < minMaxValueSize; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
        }

        LocalDateTime current = startTrain;

        while (current.isBefore(stopTrain)) {
            TickData[] tickData = loadData(current);

            if (tickData.length > 0) {
                counterFiles = counterFiles + 1;
            }

            Stream.of(tickData).forEach(x -> {
                minValues[0] = Math.min(minValues[0], x.getAskPrice());
                minValues[1] = Math.min(minValues[1], x.getBidPrice());
                minValues[2] = Math.min(minValues[2], x.getAskVolume());
                minValues[3] = Math.min(minValues[3], x.getBidVolume());

                maxValues[0] = Math.max(maxValues[0], x.getAskPrice());
                maxValues[1] = Math.max(maxValues[1], x.getBidPrice());
                maxValues[2] = Math.max(maxValues[2], x.getAskVolume());
                maxValues[3] = Math.max(maxValues[3], x.getBidVolume());
            });

            logger.info("minmax loaded " + current);

            current = current.plusHours(1);
        }

    }

    public void train() {

        for (int i = 0; i < 100; i++) {

            LocalDateTime current = startTrain;
            int counter = 0;

            while (current.isBefore(stopTrain)) {
                logger.info("training date " + current);

                TickData[] tickData = loadData(current);

                if (tickData.length > 0) {
                    counter = counter + 1;
                    DataSetIterator dataSet = getTrainingData(tickData, counter);
                    model.fit(dataSet);
                }

                current = current.plusHours(1);
            }

            model.rnnClearPreviousState();

            try {
                ModelSerializer.writeModel(model, new File(getClass().getSimpleName() + ".zip"), true);
                logger.info("model saved");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            logger.info("epoch " + i + " finished");
        }
    }

    private void test() {
        LocalDateTime current = stopTrain;
        LocalDateTime stopTest = stopTrain.plusDays(durationTest.toDays());

        List<Double> predict = new ArrayList();
        List<Double> actuals = new ArrayList();

        while (current.isBefore(stopTest)) {
            TickData[] tickData = loadData(current);

            for (int i = 0; i < tickData.length - 1; i++) {
                TickData currentTick = tickData[i];
                TickData nextTick = tickData[i + 1];

                INDArray input = Nd4j.create(new int[]{1, nIn, 1});
                addInputData(0, 0, currentTick, input);

                INDArray output = model.rnnTimeStep(input);

                predict.add((output.getDouble(0) * (maxValues[0] - minValues[0])) + minValues[0]);
                actuals.add((nextTick.getAskPrice()));
            }

            current = current.plusDays(1);
        }

        PlotUtil.plot(predict.stream().mapToDouble(i -> i).toArray(), actuals.stream().mapToDouble(i -> i).toArray(), "EURUSD");
    }

    private TickData[] loadData(LocalDateTime date) {
        Path path = FileUtil.getFile(date, instrument);

        if (path.toFile().exists()) {
            try (Reader reader = new FileReader(path.toFile())) {
                return gson.fromJson(reader, TickData[].class);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }

        return new TickData[0];
    }

    private DataSetIterator getTrainingData(TickData[] tickData, int counter) {
        int lastWindowStart = tickData.length - windowSize - 1;

        INDArray input = Nd4j.create(new int[]{lastWindowStart, nIn, windowSize});
        INDArray labels = Nd4j.create(new int[]{lastWindowStart, nOut, windowSize});

        for (int windowOffset = 0; windowOffset < lastWindowStart; windowOffset++) {

            int currentWindowEnd = windowOffset + windowSize;

            for (int windowPosition = windowOffset; windowPosition < currentWindowEnd; windowPosition++) {
                int windowIndex = windowPosition - windowOffset;

                TickData current = tickData[windowPosition];
                TickData next = tickData[windowPosition + 1];

                addInputData(windowOffset, windowIndex, current, input);
                addLabelData(windowOffset, windowIndex, next, labels);
            }
        }

        DataSet dataSet = new DataSet(input, labels);

        return new ListDataSetIterator(dataSet.asList(), lastWindowStart);
    }

    private void addInputData(int counter, int i, TickData tickData, INDArray input) {
        input.putScalar(new int[]{counter, 0, i}, UtilMath.scale(minValues[0], maxValues[0], tickData.getAskPrice()));
        input.putScalar(new int[]{counter, 1, i}, UtilMath.scale(minValues[1], maxValues[1], tickData.getBidPrice()));
        input.putScalar(new int[]{counter, 2, i}, UtilMath.scale(minValues[2], maxValues[2], tickData.getAskVolume()));
        input.putScalar(new int[]{counter, 3, i}, UtilMath.scale(minValues[3], maxValues[3], tickData.getBidVolume()));
    }

    private void addLabelData(int counter, int i, TickData tickData, INDArray labels) {
        labels.putScalar(new int[]{counter, 0, i}, UtilMath.scale(minValues[0], maxValues[0], tickData.getAskPrice()));
        labels.putScalar(new int[]{counter, 1, i}, UtilMath.scale(minValues[1], maxValues[1], tickData.getBidPrice()));
    }


}
