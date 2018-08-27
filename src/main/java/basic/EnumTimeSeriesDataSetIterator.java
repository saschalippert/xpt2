package basic;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

public class EnumTimeSeriesDataSetIterator implements DataSetIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4717195332667957759L;
	private int batchSize;
	private int start;
    private int current;
    private int step;
    private int timeSeriesSize;
    private int end;

	public EnumTimeSeriesDataSetIterator(int batchSize, int start, int current, int step, int timeSeriesSize, int end) {

		super();
		this.batchSize = batchSize;
		this.start = start;
		this.current = current;
		this.step = step;
		this.timeSeriesSize = timeSeriesSize;
		this.end = end;
	}

    @Override
	public boolean asyncSupported() {

		return false;
    }

    @Override
	public int batch() {

		return batchSize;
    }

    @Override
	public List<String> getLabels() {

		throw new UnsupportedOperationException();
    }

    @Override
	public DataSetPreProcessor getPreProcessor() {

		throw new UnsupportedOperationException();
    }

    @Override
	public boolean hasNext() {

		return current < end;
    }

    @Override
	public int inputColumns() {

		return 1;
    }

    @Override
	public DataSet next() {

		return next(batchSize);
    }

    @Override
	public DataSet next(int i) {

		INDArray input = Nd4j.create(new int[]{batchSize, 1, timeSeriesSize});
		INDArray label = Nd4j.create(new int[]{batchSize, 1, timeSeriesSize});
		for(int bIdx = 0; bIdx < batchSize; bIdx++) {
			for(int tsIdx = 0; tsIdx < timeSeriesSize; tsIdx++) {
				int temp = current + step;
				input.putScalar(new int[]{bIdx, 0, tsIdx}, temp / (double)end);
				label.putScalar(new int[]{bIdx, 0, tsIdx}, (temp + step) / (double)end);
			}
			current = current + step;
		}
		return new DataSet(input, label);
    }

    @Override
	public void reset() {

		current = start;
    }

    @Override
	public boolean resetSupported() {

		return true;
    }

    @Override
	public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {

		throw new UnsupportedOperationException();
    }

    @Override
	public int totalOutcomes() {

		return 1;
    }
}
