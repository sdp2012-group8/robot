package sdp.simulator.neural;

import java.io.File;

import org.encog.engine.data.BasicEngineData;
import org.encog.engine.data.EngineData;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.Perceptron;

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

	private final static int network_count = 2;
	private final static int n_inputs = 3;
	
	private final static double network_desired_error = 99;
	
	private String fname;
	
	private final static long testing_time = 1000;
	private final static long min_trai_time = 10000;
	private final static long wait_time_for_1000_f = 2000;
	
	// how much of the data should be used for testing instead of training
	private final static double percentage_test = 25;

	@SuppressWarnings("unchecked")
	private TrainingSet<SupervisedTrainingElement>[] tsets = new TrainingSet[network_count];
	private NeuralNetwork[] nets = new NeuralNetwork[network_count];
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
			nets[0] = new MultiLayerPerceptron(n_inputs, 4, 3);
			nets[1] = new MultiLayerPerceptron(n_inputs, 4, 3);
			for (int i = 0; i < tsets.length; i++) {
				nets[i].randomizeWeights();
			}
		}
		mObs = new WorldStateObserver(provider);
		for (int i = 0; i < tsets.length; i++) {
			if (nets[i] == null)
				System.out.println("NET INIT ERROR for "+i+"!");
		}
				
		if (allfine)
			System.out.println("Networks loaded from file.");
		else
			System.out.println("New networks generated.");
	}

	/**
	 * Is recording?
	 * @return
	 */
	public boolean isRecording() {
		return recording;
	}

	
	
	public void Pause() {
		pause = true;
		System.out.println("Recording paused");
	}
	
	public void Resume() {
		oldWorldState = null;
		pause = false;
		System.out.println("Resumed");
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
							boolean
							is_it_kicking = is_kicking;
							// create training set
							tsets[0].addElement(new SupervisedTrainingElement(
									Tools.generateAIinput(worldState, am_i_blue, my_goal_left, 0),
									Tools.generateOutput(desired_speed == 0 ? 0 : (desired_speed > 0 ? 1 : 2), 3)));
							tsets[1].addElement(new SupervisedTrainingElement(
									Tools.generateAIinput(worldState, am_i_blue, my_goal_left, 1),
									Tools.generateOutput(desired_turning_speed == 0 ? 0 : (desired_turning_speed > 0 ? 1 : 2), 3)));
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
		final TrainingSet<SupervisedTrainingElement>[] training = new TrainingSet[network_count];
		@SuppressWarnings("unchecked")
		final TrainingSet<SupervisedTrainingElement>[] testing = new TrainingSet[network_count];
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
		// TODO relative velocity of ball instead of positt
		saving = true;
		final long waittime = (long) (wait_time_for_1000_f*frames/1000d);
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < tsets.length; i++) {
						skip = false;
						int outputs = nets[i].getOutputNeurons().size();
						double accuracy = 0;
						System.out.println("Learning network "+(i+1)+"/"+network_count);					
						nets[i].learnInNewThread(training[i]);
						long elapsed = 0;
						long last_max = 0;
						double max_per = 0;
						while (!skip) {
							elapsed+=testing_time;
							try {
								sleep(testing_time);
							} catch (InterruptedException e) {}
							nets[i].pauseLearning();
							accuracy = testAccuracy(nets[i], testing[i]);
							double err_n = 100-100*Math.abs(100d/outputs-accuracy)/(100d - 100d/outputs);
							if (accuracy > max_per) {
								max_per = accuracy;
								last_max = elapsed;
								System.out.println("Network accuracy: "+String.format("%.4f", accuracy)+"% (stat err: "+String.format("%.4f", err_n)+"%), acc on training set "+testAccuracy(nets[i], training[i])+"%. Learning may stopped in "+String.format("%.1f",(waittime/1000d))+" s");
							} else
								System.out.println("Network accuracy: "+String.format("%.4f", accuracy)+"% (stat err: "+String.format("%.4f", err_n)+"%), acc on training set "+testAccuracy(nets[i], training[i])+"%");
							nets[i].resumeLearning();
							if (accuracy >= network_desired_error || elapsed-last_max > waittime) {
								if (elapsed > min_trai_time) {
									System.out.println("Network trained well! Accuracy "+String.format("%.4f", accuracy)+"% (stat err: "+String.format("%.4f", err_n)+"%)");
									break;
								} else
									System.out.println("Network trained well, required minimum training time expires in "+String.format("%.1f", (min_trai_time-elapsed)/1000d)+" s");
							}
						}
						double err_n = 100-100*Math.abs(100d/outputs-accuracy)/(100d - 100d/outputs);
						if (skip)
							System.out.println("User stopped training. Skipping to next network if any.");
						if (accuracy < network_desired_error)
							System.out.println("Network trained, but not optimal. Accuracy "+String.format("%.4f", accuracy)+"% (stat err: "+String.format("%.4f", err_n)+"%)");
						nets[i].stopLearning();
						try {
							sleep(500);
						} catch (InterruptedException e) {}
						nets[i].save(fname+"/nn"+i+".nnet");
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
			int out1 = Tools.recoverOutput(noutput);
			int out2 = Tools.recoverOutput(output);
			if (out1 != -1)
				accuracy += out1  == out2 ? 100d/sisze : 0;
		}
		return accuracy;
	}

}
