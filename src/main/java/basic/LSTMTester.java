package basic;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class LSTMTester {

    public static void main(String[] args) {

        int nIn = 1;
        int nHidden = 32;
        int nOut = 1;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(1e-4))
                .l2(1e-4)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(nIn)
                        .nOut(nHidden)
                        .activation(Activation.TANH)
                        .gateActivationFunction(Activation.HARDSIGMOID)
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

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(100));

        EnumTimeSeriesDataSetIterator trainIterator = new EnumTimeSeriesDataSetIterator(5, 1, 1, 1, 10, 1000);

        for (int i = 0; i < 1000; i++) {
            net.fit(trainIterator);
            trainIterator.reset();
            net.rnnClearPreviousState();
        }

        for (int i = 0; i < 10000; i++) {
            INDArray input = Nd4j.create(new int[]{1, 1, 1});
            input.putScalar(new int[]{0, 0, 0}, i / (double) 1000);

            INDArray output = net.rnnTimeStep(input);
            System.out.println(output.getDouble(0) * 1000);
        }

//        DataSet dataSets = iterator.next();
//
//        for (int bIdx = 0; bIdx < 5; bIdx++) {
//            System.out.println("batch " + bIdx);
//            for (int tsIdx = 0; tsIdx < 10; tsIdx++) {
//                double input = dataSets.getFeatures().getDouble(bIdx, 0, tsIdx) * Integer.MAX_VALUE;
//                double label = dataSets.getLabels().getDouble(bIdx, 0, tsIdx) * Integer.MAX_VALUE;
//
//                System.out.println(input + " -> " + label);
//            }
//        }
    }
}
