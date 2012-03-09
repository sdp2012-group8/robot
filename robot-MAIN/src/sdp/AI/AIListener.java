package sdp.AI;

import java.awt.image.BufferedImage;

import sdp.common.Utilities;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.ImageProcessorConfig;

/**
 * 
 * This is the AIListener class that updates the world the AI sees.
 * 
 * @author Martin Marinov
 *
 */

public abstract class AIListener extends WorldStateProvider {
	
	private WorldStateObserver mObs;
	private Thread mVisionThread;

	// for low pass filtering
	protected WorldState world_state = null;
	
	protected AIWorldState ai_world_state;
	private boolean my_goal_left, my_team_blue, override_vision = false;
	
	private ImageProcessorConfig config;

	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communicator for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AIListener(WorldStateProvider Obs) {
		this.mObs = new WorldStateObserver(Obs);
	}
	
	public void switchOverrideVision() {
		override_vision = ! override_vision;
	}
	
	public void setConfiguration(ImageProcessorConfig config) {
		this.config = config;
	}

	/**
	 * Starts the AI in a new decision thread. (Not true, starts a new thread that updates the world state every time it changes)
	 * 
	 * Don't start more than once!
	 * @param my_team_blue true if my team is blue, false if my team is yellow
	 * @param my_goal_left true if my goal is on the left of camera, false otherwise
	 */
	public void start(final boolean is_my_team_blue, final boolean is_my_goal_left, final boolean do_prediction) {
		this.my_team_blue = is_my_team_blue;
		this.my_goal_left = is_my_goal_left;
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = Utilities.toCentimeters(mObs.getNextState());
					// do low pass filtering
					if (world_state == null) {
						world_state = state;
						ai_world_state = new AIWorldState(world_state, my_team_blue, my_goal_left, do_prediction);
					} else {
						BufferedImage im = state.getWorldImage();
						if (override_vision) {
							ai_world_state.onDraw(im, config);
						}
						world_state = state;
					}
					ai_world_state.update(world_state, my_team_blue, my_goal_left, do_prediction);
					
					// pass coordinates to decision making logic
					setChanged();
					notifyObservers(world_state);
					worldChanged();
					
				}
			}
		};
		mVisionThread.start();
	}
	
	public void updateGoalOrTeam(final boolean is_my_team_blue, final boolean is_my_goal_left) {
		this.my_team_blue = is_my_team_blue;
		this.my_goal_left = is_my_goal_left;
	}


	/**
	 * Stops the AI
	 */
	public void stop() {
		if (mVisionThread != null)
			mVisionThread.interrupt();
	}
	


	
	protected abstract void worldChanged();

}
