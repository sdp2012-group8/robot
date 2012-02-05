package sdp.simulator.neural;

import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;

import sdp.common.Robot;
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

	private TrainingSet<SupervisedTrainingElement> trainingSet = null;
	private WorldStateObserver mObs;
	private boolean recording = false;
	
	private int frames;

	private final static int n_inputs = 6, n_outputs = 3;

	/**
	 * Initialize neural network
	 * @param provider the provider of world states
	 * @param fname the name of the neural network, if it exists it will be appended, otherwise will be created
	 */
	public NeuralNetworkTrainingGenerator(WorldStateProvider provider, String fname) {
		this.fname = fname;
		// input:
		//		my_x, my_y, enemy_x, enemy_y, ball_x, ball_y
		// output:
		//		speed, turning_speed, kick
		
		try {
			trainingSet = TrainingSet.load(fname);
		} catch (Exception e) {}
		if (trainingSet == null)
			trainingSet = new TrainingSet<SupervisedTrainingElement>(n_inputs, n_outputs);
		mObs = new WorldStateObserver(provider);
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
	public void Record(final boolean am_i_blue) {
		frames = 0;
		recording = true;
		System.out.println("Starting record");
		new Thread() {
			public void run() {
				while (recording) {
					WorldState ws = mObs.getNextState();
					Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
					Robot enemy = am_i_blue ? ws.getYellowRobot() : ws.getBlueRobot();
					trainingSet.addElement(new SupervisedTrainingElement(
							new double[]{
									me.getCoords().getX(),
									me.getCoords().getY(),
									enemy.getCoords().getX(),
									enemy.getCoords().getY(),
									ws.getBallCoords().getX(),
									ws.getBallCoords().getY()},
							new double[]{
									desired_speed,
									desired_turning_speed,
									is_kicking ? 1 : 0}));
					frames++;
					if (frames % 100 == 0)
						System.out.println(frames+" frames recorded last - "+me.getCoords().getX()+" and "+desired_speed);
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
		if (trainingSet != null) {
			trainingSet.saveAsTxt(fname, " ");
		}
	}

}
