package sdp.simulator.neural;

import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;

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

	private String fname;

	@SuppressWarnings("unchecked")
	private TrainingSet<SupervisedTrainingElement>[] tsets = new TrainingSet[5];
	private WorldStateObserver mObs;
	private boolean recording = false;
	private WorldState oldWorldState = null;

	private int frames;

	private final static int n_inputs = 10;

	/**
	 * Initialize neural network
	 * @param provider the provider of world states
	 * @param dir the name of dir to store trainings
	 */
	@SuppressWarnings("unchecked")
	public NeuralNetworkTrainingGenerator(WorldStateProvider provider, String dir) {
		this.fname = dir;
		boolean allfine = true;
		try {
			for (int i = 0; i < tsets.length; i++) {
				tsets[i] = TrainingSet.load(fname+"/ts"+i+".tset");
				if (tsets[i] == null) {
					allfine = false;
					break;
				}
			}
		} catch (Exception e) {}
		if (!allfine)
			for (int i = 0; i < tsets.length; i++)
				tsets[i] = new TrainingSet<SupervisedTrainingElement>(n_inputs, 2);
		mObs = new WorldStateObserver(provider);
		for (int i = 0; i < tsets.length; i++)
			if (tsets[i] == null) {
				System.out.println("TRAINING INIT ERROR for "+i+"!");
			}
		System.out.println("Training set ready");
	}

	/**
	 * Is recording?
	 * @return
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * Start recording from provider. Make sure you save first or all data will be lost.
	 * @param am_i_blue
	 */
	public void Record(final boolean am_i_blue, final boolean my_goal_left) {
		frames = 0;
		recording = true;
		System.out.println("Starting record");
		new Thread() {
			public void run() {
				while (recording) {
					WorldState worldState = Tools.toCentimeters(mObs.getNextState());
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
			};
		}.start();
	}

	/**
	 * Stop recording
	 */
	public void Stop() {
		System.out.println("Recording stopped. Total of "+frames+" frames are ready for saving.");
		recording = false;
	}

	/**
	 * Save the network
	 */
	public void Save() {
		for (int i = 0; i < tsets.length; i++)
			if (tsets[i] != null)
				tsets[i].saveAsTxt(fname+"/ts"+i+".tset", " ");
		// TODO self train and change in AI
	}

}
