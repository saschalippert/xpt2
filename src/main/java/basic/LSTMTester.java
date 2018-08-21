package basic;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.SingletonDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

public class LSTMTester {

    public static void main(String[] args) {

        int nIn = 1;
        int nHidden = 32;
        int nOut = 1;

        File file = new File(LSTMTester.class.getSimpleName() + ".zip");

        MultiLayerNetwork net;

        if (file.exists()) {
            try {
                System.out.println("model loaded");
                net = ModelSerializer.restoreMultiLayerNetwork(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(12345)
                    .biasInit(0)
                    .updater(new Adam(1e-4))
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .weightInit(WeightInit.XAVIER)
                    .list()
                    .layer(0, new LSTM.Builder()
                            .nIn(nIn)
                            .nOut(nHidden)
                            .activation(Activation.TANH)
                            .build())
                    .layer(1, new RnnOutputLayer.Builder()
                            .nIn(nHidden)
                            .nOut(nOut)
                            .activation(Activation.IDENTITY)
                            .lossFunction(LossFunctions.LossFunction.MSE)
                            .build())
                    .pretrain(false)
                    .backprop(true)
                    .build();

            net = new MultiLayerNetwork(conf);
            net.init();
        }

        net.addListeners(new ScoreIterationListener(50));

        int end = 1000;

        INDArray input = Nd4j.create(new int[]{1, 1, end});
        INDArray label = Nd4j.create(new int[]{1, 1, end});

        for (int pos = 0; pos < end; pos++) {
            input.putScalar(new int[]{0, 0, pos}, pos / (double) end);
            label.putScalar(new int[]{0, 0, pos}, ((pos + 1) % end) / (double) end);
        }

        DataSet dataSets = new DataSet(input, label);

        for (int bIdx = 0; bIdx < input.shape()[0]; bIdx++) {
            System.out.println("batch " + bIdx);
            for (int tsIdx = 0; tsIdx < input.shape()[2]; tsIdx++) {
                double di = dataSets.getFeatures().getDouble(bIdx, 0, tsIdx) * end;
                double dl = dataSets.getLabels().getDouble(bIdx, 0, tsIdx) * end;

                System.out.println(di + " -> " + dl);
            }
        }

        UIServer uiServer = UIServer.getInstance();

        DataSetIterator trainIterator = new SingletonDataSetIterator(dataSets);

        StatsStorage statsStorage = new InMemoryStatsStorage();
        int listenerFrequency = 1;
        net.addListeners(new StatsListener(statsStorage, listenerFrequency));
        uiServer.attach(statsStorage);

        int epochs = 10000;

        for (int i = 0; i < epochs; i++) {
            net.fit(trainIterator);
            trainIterator.reset();
            net.rnnClearPreviousState();

            if (i % 1000 == 0) {
                try {
                    ModelSerializer.writeModel(net, new File(LSTMTester.class.getSimpleName() + ".zip"), true);
                    System.out.println("model saved");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        for (int i = 0; i < 1000; i++) {
            INDArray input2 = Nd4j.create(new int[]{1, 1, 1});
            input2.putScalar(new int[]{0, 0, 0}, i / (double) end);

            INDArray output = net.rnnTimeStep(input);
            System.out.println(output.getDouble(0) * end);
        }
    }
}
