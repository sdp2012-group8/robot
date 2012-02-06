package sdp.simulator.neural;

import java.io.File;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;

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

	private final static int network_count = 5;
	
	private String fname;

	@SuppressWarnings("unchecked")
	private TrainingSet<SupervisedTrainingElement>[] tsets = new TrainingSet[network_count];
	private NeuralNetwork[] nets = new NeuralNetwork[network_count];
	private WorldStateObserver mObs;
	private boolean recording = false;
	private WorldState oldWorldState = null;
	
	private boolean pause = false;

	private int frames;
	
	private boolean saving = false;

	private final static int n_inputs = 9;

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
		if (!allfine)
			for (int i = 0; i < tsets.length; i++) {
				nets[i] = new MultiLayerPerceptron(n_inputs, 5 ,2);
				nets[i].randomizeWeights();
			}
		mObs = new WorldStateObserver(provider);
		for (int i = 0; i < tsets.length; i++)
			if (nets[i] == null) {
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
			tsets[i] = new TrainingSet<SupervisedTrainingElement>(n_inputs, 2);
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
							is_going_forwards = desired_speed > 0,
							is_standing_still = desired_speed == 0 ,
							is_turning_right = desired_turning_speed > 0,
							is_not_turning = desired_turning_speed == 0,
							is_it_kicking = is_kicking;
							// create training set
							double[] input = Tools.generateAIinput(oldWorldState, am_i_blue, my_goal_left);
							tsets[0].addElement(new SupervisedTrainingElement(input, Tools.generateOutput(is_going_forwards)));
							tsets[1].addElement(new SupervisedTrainingElement(input, Tools.generateOutput(is_standing_still)));
							tsets[2].addElement(new SupervisedTrainingElement(input, Tools.generateOutput(is_turning_right)));
							tsets[3].addElement(new SupervisedTrainingElement(input, Tools.generateOutput(is_not_turning)));
							tsets[4].addElement(new SupervisedTrainingElement(input, Tools.generateOutput(is_it_kicking)));
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

	/**
	 * Save the network
	 */
	public void Save() {
		saving = true;
		final long learn_time_ms = frames*10;
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < tsets.length; i++)
					if (tsets[i] != null) {
						System.out.println("Learning network "+(i+1)+"/"+network_count+" for "+String.format("%.1f", learn_time_ms/1000d)+" s. Remaining time: "+String.format("%.1f", (network_count-i)*learn_time_ms/1000d)+" s");					
						nets[i].learnInNewThread(tsets[i]);
						try {
							sleep(learn_time_ms);
						} catch (InterruptedException e) {}
						nets[i].stopLearning();
						try {
							sleep(500);
						} catch (InterruptedException e) {}
						nets[i].save(fname+"/nn"+i+".nnet");
						tsets[i].save(fname+"/ts"+i+".tset");
						tsets[i].saveAsTxt(fname+"/csv-ts"+i+".txt", " ");
						tsets[i].clear();
						tsets[i] = null;
						System.out.println("Saved");
					}
				saving = false;
			}
		}.start();

	}

}
