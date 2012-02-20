package sdp.simulator.neural;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.encog.engine.data.BasicEngineData;
import org.encog.engine.data.EngineData;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

import sdp.AI.AIMaster;
import sdp.AI.AIWorldState.mode;
import sdp.AI.neural.AINeuralNetwork;
import sdp.common.NNetTools;
import sdp.common.Tools;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.WorldStateProvider;
import sdp.common.NNetTools.move_modes;
import sdp.simulator.VBrick;

/**
 * Simple world state recorder and neural network trainer
 * 
 * @author martinmarinov
 *
 */
public class NeuralNetworkTrainingGenerator extends VBrick {

	private NeuralNetwork[] nets = new NeuralNetwork[] {
			new MultiLayerPerceptron(16, 33, move_modes.values().length)
	};
	
	private String fname;
	
	/**
	 * How many iterations to wait until quitting. For every 1000 input points you have this many iterations
	 */
	private final static long wait_iter_for_1000_f = 250;
	/**
	 * Wait for this amount of epochs before doing anything
	 */
	private final static long skip_first_n = 5;
	
	// how much of the data should be used for testing instead of training
	private final static double percentage_test = 25;

	@SuppressWarnings("unchecked")
	private TrainingSet<SupervisedTrainingElement>[] tsets = new TrainingSet[nets.length];
	
	private BackPropagation[] propagations = new BackPropagation[nets.length];
	
	private long lastTime = 0;
	
	private WorldStateObserver mObs;
	private boolean recording = false;
	private WorldState oldWorldState = null;
	
	private boolean pause = false;

	private int frames;
	
	private boolean saving = false;

	private boolean skip = false;
	
	private AIMaster mAi;

	/**
	 * Initialize neural network
	 * @param provider the provider of world states
	 * @param dir the name of dir to store trainings
	 */
	public NeuralNetworkTrainingGenerator(WorldStateProvider provider, String dir) {
		fname = dir;
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

	public void registerAI(AIMaster ai) {
		mAi =  ai;
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
					if (!pause && mAi.getMode() == mode.sit) {
						if (oldWorldState != null && Tools.delta(oldWorldState, worldState) > 0.1) {
							// outputs normalized to 1
							//boolean
							//is_it_kicking = is_kicking;
							double dt =(System.currentTimeMillis()-lastTime)/1000d;
							// create training set
							double[] out = NNetTools.generateOutput(NNetTools.getMode(desired_speed, desired_turning_speed));
							if (out != null)
							tsets[0].addElement(new SupervisedTrainingElement(
									NNetTools.generateAIinput(worldState, oldWorldState, dt, am_i_blue, my_goal_left, 0),
									out));
							frames++;
							if (frames % 100 == 0)
								System.out.println(frames+" frames recorded last - "+frames+" and "+desired_speed);
						}
						oldWorldState = worldState;
						lastTime = System.currentTimeMillis();
					}
				}	
				System.out.println("Recording stopped. Total of "+frames+" frames are ready for saving.");
			};
		}.start();
	}

	
	@SuppressWarnings("unchecked")
	public void loadTSets() {
		// randomize networks
		if (new File(fname+"/nn"+0+".nnet").exists()) {
			System.out.println("Networks have been previously initialized! Delete the network files and restart! Aborting!");
			return;
		}
		for (int i = 0; i < tsets.length; i++)
			tsets[i] = TrainingSet.load(fname+"/ts"+i+".tset");
		for (int i = 0; i < tsets.length; i++)
			if (tsets[i] == null) {
				System.out.println("Error loading training set "+i+" from file. Aborting.");
				return;
			} else
				System.out.println("Set "+i+" ("+tsets[i].getInputSize()+"x"+tsets[i].getIdealSize()+")["+tsets[i].size()+"] loaded from file.");
		frames = tsets[0].size();
		System.out.println("Successfully done.");
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
		
		System.out.println("Training 0 size: "+training[0].size()+"; Test size: "+testing[0].size());

		// start learning
		saving = true;
		new Thread() {
			@Override
			public void run() {
				final OutputStreamWriter[] writers = new OutputStreamWriter[tsets.length];
				for (int i = 0; i < writers.length; i++) {		
					try {
						FileOutputStream fos = new FileOutputStream(fname+"/acc_report"+i+".csv"); 
						writers[i] = new OutputStreamWriter(fos, "UTF-8");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				for (int i = 0; i < tsets.length; i++) {
						skip = false;
						int outputs = nets[i].getOutputNeurons().size();
						int frames = training[i].size();
						double accuracy = 0;
										
						long elapsed = 0;
						double max_per = 0;
						int max_iters = (int) wait_iter_for_1000_f*frames/1000;
						if (max_iters < wait_iter_for_1000_f)
							max_iters = (int) wait_iter_for_1000_f;
						System.out.println("Learning network "+(i+1)+"/"+nets.length+" max waiting time after max "+max_iters);	
						int total = 0;
						while (!skip) {
							total++;
							elapsed++;
							propagations[i].doLearningEpoch(training[i]);
							accuracy = testAccuracy(nets[i], testing[i]);
							if (accuracy > max_per && total > skip_first_n) {
								System.out.print(String.format("%.2f", accuracy)+"% ("+total+");");
								max_per = accuracy;
								nets[i].save(fname+"/nn"+i+".nnet");
								elapsed = 0;
							}
							try {
								writers[i].append(accuracy+"\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
							// if you tried enough without improvement
							if (elapsed > max_iters)
								break;
						}
						nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
						accuracy = testAccuracy(nets[i], testing[i]);
						double err_n = 100-100*Math.abs(100d/outputs-accuracy)/(100d - 100d/outputs);
						System.out.println();
						if (skip)
							System.out.println("User stopped training. Skipping to next network if any.");
						System.out.println("Network trained. Accuracy "+String.format("%.2f", accuracy)+"% (stat err: "+String.format("%.2f", err_n)+"%)");
						
						testing[i].clear();
						testing[i] = null;
						training[i].clear();
						training[i] = null;
						System.out.println("Saved");
					}
				saving = false;
				for (int i = 0; i < writers.length; i++)
					try {
						writers[i].flush();
						writers[i].close();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
			int out1 = NNetTools.recoverOutputInt(noutput);
			int out2 = NNetTools.recoverOutputInt(output);
			if (out1 != -1)
				accuracy += out1  == out2 ? 100d/sisze : 0;
		}
		return accuracy;
	}

}
