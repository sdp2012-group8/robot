package sdp.AI.genetic;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import sdp.AI.AIMaster;
import sdp.AI.AIVisualServoing;
import sdp.AI.AIMaster.AIState;
import sdp.AI.neural.AINeuralNet;
import sdp.common.WorldStateRandomizer;
import sdp.common.geometry.GeomUtils;
import sdp.common.world.WorldState;
import sdp.simulator.Simulator;
import sdp.simulator.SimulatorPhysicsEngine;
import sdp.simulator.VBrick;

/**
 * A game simulation
 */
public class Game implements SimulatorPhysicsEngine.Callback {
	
	/** The states that the game could be at
	 * @see Game#getState() */
	public enum gamestate {ready, running, finished};
	
	/** use to access the ids of the robots that play in this game */
	public final int[] ids;
	/** use to get the current game id */
	public final int gameId;
	
	/** callback to be notified when the game ends */
	private Callback callback;
	/** current state of the game */
	private volatile gamestate currentState = gamestate.ready;
	
	/** DONT FORGET TO SE THIS TO FALSE AFTER THE GAME HAS FINISHED */
	private boolean simulateGame = false;
	/** The most up-to-date world state */
	private WorldState state = null;
	
	/** simulation speed */
	public static final int FPS = 15;
	/** frame length */
	private static final double FRAME_TIME = 1d / FPS;
	
	private double[][] population;
	
	/** the x coordinate of the robot that would be placed on left */
	private static final double PLACEMENT_LEFT = 20; // in cm
	/** the x coordinate of the robot that would be placed on right */
	private static final double PLACEMENT_RIGHT = WorldState.PITCH_WIDTH_CM - PLACEMENT_LEFT; // in cm
	
	public static final double PLACEMENT_X_RAND = 3;
	public static final double PLACEMENT_Y_RAND = 30;
	public static final double ANGLE_RAND = 20;
	public static final double BALL_RAND = 2;
	
	private static final int MAX_BALL_SCORE = 20;
	private static final int MAX_BALL_DISTANCE = 70;
	
	private static final int GAMETIME = 3*60; // in sec
	
	private long[] scores = new long[2];
	
	private double timeElapsed = 0;
	
	private AIMaster leftAI, rightAI;
	
	private SimulatorPhysicsEngine sim;
	
	private boolean inCollBlue = false, inCollYellow = false;
	
	private int leftGoals = 0, rightGoals = 0;
	
	private static final double REPLAY_LENGTH_IN_SECONDS = 3*60;
	private static final int REPLAY_FRAME_COUNT = (int) (FPS*REPLAY_LENGTH_IN_SECONDS);
	private int replay_frames = 0;
	private Queue<FrameSubtitleEntry> replay;
	private static int replays = 0;
	
	/** set this on the frame you want to have a subtitle */
	private String subtitle = "";
	
	// calculate fitness section
	
	/**
	 * This method will be called during an initialization of a game (before any world state or score is available for you to check).
	 * If you plan to record current game, return true. If you return false and later try doing
	 * {@link #saveReplay()}, you will get an error. The purpose of this function is to avoid allocating memory
	 * for games which won't be saved in the end for sure.<br/>
	 * Generally you could use this when you want to record a game with particular id or particular participants.
	 * @return true if you plan to use {@link #saveReplay()}, false otherwise
	 */
	public boolean doWeStartRecording() {
		// record only games that the real AI takes part in
		return ids[0] == -1 || ids[1] == -1;
	}
	
	/**
	 * When new frame is available, analyse the {@link #state}
	 */
	public void onNewFrame() {
		if (inCollBlue)
			scores[0]-=10;
		if (inCollYellow)
			scores[1]-=10;
		
		final double yellowBall = GeomUtils.pointDistance(state.getYellowRobot().getFrontCenter(), state.getBallCoords());
		final double blueBall = GeomUtils.pointDistance(state.getBlueRobot().getFrontCenter(), state.getBallCoords());
		
		scores[0]+= (int) (MAX_BALL_SCORE - MAX_BALL_SCORE*blueBall/MAX_BALL_DISTANCE);
		scores[1]+= (int) (MAX_BALL_SCORE - MAX_BALL_SCORE*yellowBall/MAX_BALL_DISTANCE);		
		
		subtitle = String.format("%.2f", timeElapsed)+" : "+this.toString();
		
	}
	
	public void onTimeOut() {
		// mark end of game
		simulateGame = false;
		if (ids[0] == -1 || ids[1] == -1) {
			System.out.printf("(id %02d) %02d:%02d (%02d id)\n", ids[0], rightGoals, leftGoals, ids[1]);
			saveReplay();
		}
	}

	/**
	 * If the ball goes into the left goal
	 */
	@Override
	public void onLeftScore() {
		//System.out.println("LEFT SCORE");
		scores[1]+=50000;
		scores[0]-=30000;
		leftGoals++;
		resetPitch();
		
		//if (leftGoals > 1)
		//	saveReplay();
	}

	/**
	 * If the ball goes into the right goal
	 */
	@Override
	public void onRightScore() {
		//System.out.println("RIGHT SCORE");
		scores[0]+=50000;
		scores[1]-=30000;
		rightGoals++;
		resetPitch();
		
		//if (leftGoals > 1)
		//	saveReplay();
	}

	/**
	 * If the yellow robot collides with an obstacle
	 */
	@Override
	public void onYellowCollide() {
		//System.out.println("Yellow collided");
		scores[1]-=500;
		inCollYellow = true;
	}

	@Override
	public void onYellowStopCollide() {
		scores[1]+=500;
		inCollYellow = false;
	}

	
	/**
	 * If the blue robot collides with an obstacle
	 */
	@Override
	public void onBlueCollide() {
		//System.out.println("Blue collided");
		scores[0]-=500;
		inCollBlue = true;
	}
	
	@Override
	public void onBlueStopCollide() {
		scores[0]+=500;
		inCollBlue = false;
	}
	
	// API and simulation
	
	/**
	 * Initialize a new game
	 * @param i the left robot id
	 * @param j thr right robot id
	 * @param population arrays to pick from
	 * @param callback to be used to notify caller back when game is finished and pass scores
	 * @param gameId the id of the current game
	 */
	public Game(int i, int j, final double[][] population, Callback callback, int gameId) {
		this.population = population;
		ids = new int[]{i, j};
		this.callback = callback;
		this.gameId = gameId;
		simulateGame = true;
	}
	
	/**
	 * Does the simulation (in current thread)
	 */
	public void simulate() {
		currentState = gamestate.running;
		
		timeElapsed = 0;
		
		// create simulator
		sim = new SimulatorPhysicsEngine(false, true);
		
		final VBrick leftBrick = new VBrick(),
					rightBrick = new VBrick();
		

		// reset pitch
		sim.registerBlue(leftBrick,
				PLACEMENT_LEFT,
				WorldState.PITCH_HEIGHT_CM/2,
				0);
		sim.registerYellow(rightBrick,
				PLACEMENT_RIGHT,
				WorldState.PITCH_HEIGHT_CM/2,
				180);
		sim.putBallAt();
		
		resetPitch();
		
		sim.callback = this;
		
		leftAI = new AIMaster(leftBrick, sim, ids[0] != -1 ? new AINeuralNet(population[ids[0]]) : new AIVisualServoing());
		leftAI.setPrintStateChanges(false);
		rightAI = new AIMaster(rightBrick, sim, ids[1] != -1 ? new AINeuralNet(population[ids[1]]) : new AIVisualServoing());
		rightAI.setPrintStateChanges(false);
		
		leftAI.setOwnTeamBlue(true);
		leftAI.setOwnGoalLeft(true);
		
		rightAI.setOwnTeamBlue(false);
		rightAI.setOwnGoalLeft(false);
		
		leftAI.setState(AIState.PLAY);
		rightAI.setState(AIState.PLAY);
		
		for (int i = 0; i < Simulator.DELAY_SIZE; i++) {
			sim.simulate(FRAME_TIME);
			state = sim.getWorldState();
			sim.delayQueue.add(state);
		}
		
		replay_frames = 0;
		
		if (doWeStartRecording()) {
			replay = new LinkedList<FrameSubtitleEntry>();
		}
		
		// runs simulation
		while (simulateGame) {
			sim.simulate(FRAME_TIME);
			state = sim.getWorldState();
			sim.delayQueue.add(state);
			final WorldState frame = sim.delayQueue.poll();
			leftAI.processState(frame, false);
			rightAI.processState(frame, false);
			
			timeElapsed += FRAME_TIME;
			onNewFrame();
			if (timeElapsed > GAMETIME) {
				onTimeOut();
				timeElapsed = 0;
			}
			
			if (replay != null) {
				replay.add(new FrameSubtitleEntry(frame, subtitle));
				replay_frames++;
				if (replay_frames > REPLAY_FRAME_COUNT) {
					replay_frames--;
					replay.poll();
				}
			}
		}
		

		leftAI.stop();
		rightAI.stop();
		sim.stop();
		
		currentState = gamestate.finished;
		callback.onFinished(this, scores);

	}
	
	private void resetPitch() {
		
		sim.putBallAt(0.5 + WorldStateRandomizer.getRandom()*BALL_RAND/ WorldState.PITCH_WIDTH_CM,
					WorldState.PITCH_HEIGHT_CM / (2 * WorldState.PITCH_WIDTH_CM) + WorldStateRandomizer.getRandom()*BALL_RAND/ WorldState.PITCH_WIDTH_CM);
		
		sim.putAt((PLACEMENT_LEFT + PLACEMENT_X_RAND*WorldStateRandomizer.getRandom())/WorldState.PITCH_WIDTH_CM,
				WorldState.PITCH_HEIGHT_CM/(2*WorldState.PITCH_WIDTH_CM) + PLACEMENT_Y_RAND*WorldStateRandomizer.getRandom()/WorldState.PITCH_WIDTH_CM,
				0, WorldStateRandomizer.getRandom()*ANGLE_RAND);
		
		sim.putAt((PLACEMENT_RIGHT + PLACEMENT_X_RAND*WorldStateRandomizer.getRandom())/WorldState.PITCH_WIDTH_CM,
				WorldState.PITCH_HEIGHT_CM/(2*WorldState.PITCH_WIDTH_CM) + PLACEMENT_Y_RAND*WorldStateRandomizer.getRandom()/WorldState.PITCH_WIDTH_CM,
				1, 180+WorldStateRandomizer.getRandom()*ANGLE_RAND);
		
	}

	/**
	 * @return current game state
	 */
	public gamestate getState() {
		return currentState;
	}
	
	/**
	 * Saves the replay of the current game. This function will work only if {@link #doWeStartRecording()} returned true during initialization of this game
	 */
	private synchronized void saveReplay() {
		FrameSubtitleEntry[] entries = replay.toArray(new FrameSubtitleEntry[0]);
		String[] subtitles = new String[entries.length];
		WorldState[] states = new WorldState[entries.length];
		for (int i = 0; i < entries.length; i++) {
			states[i] = entries[i].state;
			subtitles[i] = entries[i].subtitle;
		}
		if (replay != null) {
			final String path = "data/movies/left"+(replays++)+"-"+this;
			new File(path).mkdirs();
			WorldState.saveMovie(states, path, subtitles);
		} else
			System.err.println("Called unexpected save replay on "+this+" gameid "+gameId);
	}
	
	
	// callback section
	
	/**
	 * A callback that receives fitness scores after the game has been finised
	 * 
	 * @author Martin Marinov
	 *
	 */
	public static interface Callback {
		
		/**
		 * When game finishes, this gets called
		 * @param the game that the result is coming from
		 * @param fitness of network 0 and 1
		 */
		public void onFinished(final Game caller, final long[] fitness);

	}
	
	@Override
	public String toString() {
		return String.format("(%d)%d:%d(%d)", ids[0], rightGoals, leftGoals, ids[1]);
	}
		
	private static class FrameSubtitleEntry {

		public WorldState state;
		public String subtitle;
		
		public FrameSubtitleEntry(final WorldState state, final String subtitle) {
			this.state = state;
			this.subtitle = subtitle;
		}
		
	}
}
