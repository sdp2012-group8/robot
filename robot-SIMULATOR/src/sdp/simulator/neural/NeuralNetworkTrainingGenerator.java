package sdp.simulator.neural;

import java.io.File;

import org.encog.engine.data.BasicEngineData;
import org.encog.engine.data.EngineData;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

import sdp.common.NNetTools;
import sdp.common.Tools;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.WorldStateProvider;
import sdp.simulator.VBrick;

/**
 * Simple world state recorder and neural network trainer
 * 
 * @author martinmarinov
 *
 */
public class NeuralNetworkTrainingGenerator extends VBrick {

	private NeuralNetwork[] nets = new NeuralNetwork[] {
			new MultiLayerPerceptron(11, 5, 7, 3),
			new MultiLayerPerceptron(11, 5, 7, 3)
	};
	
	private final static double network_desired_error = 80;
	
	private String fname;
	
	/**
	 * How many iterations to wait until quitting. For every 1000 input points you have this many iterations
	 */
	private final static long wait_iter_for_1000_f = 200;
	
	// how much of the data should be used for testing instead of training
	private final static double percentage_test = 25;

	@SuppressWarnings("unchecked")
	private TrainingSet<SupervisedTrainingElement>[] tsets = new TrainingSet[nets.length];
	
	private BackPropagation[] propagations = new BackPropagation[nets.length];
	
	private WorldStateObserver mObs;
	private boolean recording = false;
	private WorldState oldWorldState = null;
	
	private boolean pause = false;

	private int frames;
	
	private boolean saving = false;

	private boolean skip = false;

	/**
	 * Initialize neural network
	 * @param provider the provider of world states
	 * @param dir the name of dir to store trainings
	 */
	public NeuralNetworkTrainingGenerator(WorldStateProvider provider, String dir) {
		this.fname = dir;
		boolean allfine = true;
		try {
			for (int i = 0; i < tsets.length; i++) {
				if (! new File(fname+"/nn"+i+".nnet").exists()) {
					allfine = false;
					break;
				}
				nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
			}
		} catch (Exception e) {}
		if (!allfine) {
			// INITIALIZE NETWORKS HERE
			System.out.println("New networks generated.");
			for (int i = 0; i < tsets.length; i++) {
				nets[i].randomizeWeights();
			}
		}
		mObs = new WorldStateObserver(provider);
		for (int i = 0; i < tsets.length; i++) {
			if (nets[i] == null)
				System.out.println("NET INIT ERROR for "+i+"!");
			else {
				propagations[i] = new BackPropagation();
				nets[i].setLearningRule(propagations[i]);
			}
		}
				
		if (allfine)
			System.out.println("Networks loaded from file.");
	}

	/**
	 * Is recording?
	 * @return
	 */
	public boolean isRecording() {
		return recording;
	}

	
	
	public void Pause() {
		if (!recording) 
			return;
		if (pause)
			return;
		pause = true;
		System.out.println("Recording paused");
	}
	
	public void Resume() {
		if (!recording) 
			return;
		if (!pause)
			return;
		oldWorldState = null;
		pause = false;
		System.out.println("Resumed");
	}
	
	public boolean isPaused() {
		return pause;
	}

	/**
	 * Start recording from provider. Make sure you save first or all data will be lost.
	 * @param am_i_blue
	 */
	public void Record(final boolean am_i_blue, final boolean my_goal_left) {
		if (saving) {
			System.out.println("Please wait until training is done!");
			return;
		}
		frames = 0;
		recording = true;
		// reset training states
		for (int i = 0; i < tsets.length; i++) {
			if (tsets[i] != null) {
				tsets[i].clear();
				tsets[i] = null;
			}
			tsets[i] = new TrainingSet<SupervisedTrainingElement>(nets[i].getInputNeurons().size(), nets[i].getOutputNeurons().size());
		}
		System.out.println("Starting record");
		new Thread() {
			public void run() {
				while (recording) {
					WorldState worldState = Tools.toCentimeters(mObs.getNextState());
					if (!pause) {
						if (oldWorldState != null && Tools.delta(oldWorldState, worldState) > 0.1) {
							// outputs normalized to 1
							//boolean
							//is_it_kicking = is_kicking;
							// create training set
							int spd_test = desired_speed == 0 ? 1 : (desired_speed > 0 ? 2 : 0);
							int trn_test = desired_turning_speed == 0 ? 1 : (desired_turning_speed > 0 ? 2 : 0);
							tsets[0].addElement(new SupervisedTrainingElement(
									NNetTools.generateAIinput(worldState, am_i_blue, my_goal_left, 0),
									NNetTools.generateOutput(spd_test, 3)));
							tsets[1].addElement(new SupervisedTrainingElement(
									NNetTools.generateAIinput(worldState, am_i_blue, my_goal_left, 1),
									NNetTools.generateOutput(trn_test, 3)));
							frames++;
							if (frames % 100 == 0)
								System.out.println(frames+" frames recorded last - "+frames+" and "+desired_speed);
						}
						oldWorldState = worldState;
					}
				}	
				System.out.println("Recording stopped. Total of "+frames+" frames are ready for saving.");
			};
		}.start();
	}

	
	@SuppressWarnings("unchecked")
	public void loadTSets() {
		for (int i = 0; i < tsets.length; i++)
			tsets[i] = TrainingSet.load(fname+"/ts"+i+".tset");
		for (int i = 0; i < tsets.length; i++)
			if (tsets[i] == null) {
				System.out.println("Error loading training sets from file.");
				return;
			}
		System.out.println("Training sets loaded from file.");
	}
	
	/**
	 * Stop recording
	 */
	public void Stop() {
		recording = false;
	}
	
	public boolean isLearning() {
		return saving;
	}
	
	public void stopLearning() {
		skip = true;
	}

	/**
	 * Save the network
	 */
	public void Save() {
		double testing_last = frames*percentage_test/100d;
		System.out.println("Preparing sets...");
		// split training arrays into testing and training
		@SuppressWarnings("unchecked")
		final TrainingSet<SupervisedTrainingElement>[] training = new TrainingSet[nets.length];
		@SuppressWarnings("unchecked")
		final TrainingSet<SupervisedTrainingElement>[] testing = new TrainingSet[nets.length];
		for (int i = 0; i < training.length; i++) {
			training[i] = new TrainingSet<SupervisedTrainingElement>(nets[i].getInputNeurons().size(), nets[i].getOutputNeurons().size());
			testing[i] = new TrainingSet<SupervisedTrainingElement>(nets[i].getInputNeurons().size(), nets[i].getOutputNeurons().size());
			int size = tsets[i].size();
			for (int j = 0; j < size; j++) {
				EngineData pair = new BasicEngineData(null);
				tsets[i].getRecord(j, pair);
				if (size - j < testing_last)
					testing[i].addElement(new SupervisedTrainingElement(pair.getInputArray(), pair.getIdealArray()));
				else
					training[i].addElement(new SupervisedTrainingElement(pair.getInputArray(), pair.getIdealArray()));
			}
			tsets[i].save(fname+"/ts"+i+".tset");
			tsets[i].saveAsTxt(fname+"/csv-ts"+i+".txt", " ");
			tsets[i].clear();
			tsets[i] = null;
		}
		// start learning
		saving = true;
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < tsets.length; i++) {
						skip = false;
						int outputs = nets[i].getOutputNeurons().size();
						int frames = training[i].size();
						double accuracy = 0;
						System.out.println("Learning network "+(i+1)+"/"+nets.length);					
						long elapsed = 0;
						double max_per = 0;
						int max_iters = (int) wait_iter_for_1000_f*frames/1000;
						if (max_iters < wait_iter_for_1000_f)
							max_iters = (int) wait_iter_for_1000_f;
						int total = 0;
						while (!skip) {
							total++;
							elapsed++;
							propagations[i].doLearningEpoch(training[i]);
							accuracy = testAccuracy(nets[i], testing[i]);
							if (accuracy > max_per) {
								System.out.println("Max accuracy reached: "+String.format("%.2f", accuracy)+"% (on iteration "+total+"). Wait for improvement in the next "+max_iters+" iterations...");
								max_per = accuracy;
								nets[i].save(fname+"/nn"+i+".nnet");
								elapsed = 0;
							}
							// if you tried enough without improvement
							if (elapsed > max_iters)
								break;
						}
						nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
						accuracy = testAccuracy(nets[i], testing[i]);
						double err_n = 100-100*Math.abs(100d/outputs-accuracy)/(100d - 100d/outputs);
						if (skip)
							System.out.println("User stopped training. Skipping to next network if any.");
						if (accuracy < network_desired_error)
							System.out.println("Network trained. Accuracy "+String.format("%.2f", accuracy)+"% (stat err: "+String.format("%.2f", err_n)+"%)");
						
						testing[i].clear();
						testing[i] = null;
						training[i].clear();
						training[i] = null;
						System.out.println("Saved");
					}
				saving = false;
			}
		}.start();

	}
	
	private static double testAccuracy(NeuralNetwork net, TrainingSet<?> tset) {
		double accuracy = 0;
		int sisze = tset.size();
		for (int j = 0; j < sisze; j++) {
			EngineData pair = new BasicEngineData(null);
			tset.getRecord(j, pair);
			double[] input = pair.getInputArray();
			double[] output = pair.getIdealArray();
			net.setInput(input);
			net.calculate();
			double[] noutput = net.getOutput();
			int out1 = NNetTools.recoverOutput(noutput);
			int out2 = NNetTools.recoverOutput(output);
			if (out1 != -1)
				accuracy += out1  == out2 ? 100d/sisze : 0;
		}
		return accuracy;
	}

}
