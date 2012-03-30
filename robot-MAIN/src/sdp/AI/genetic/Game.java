package sdp.AI.genetic;

import sdp.AI.AIMaster;
import sdp.AI.AIMaster.AIState;
import sdp.AI.neural.AINeuralNet;
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
	private static final int FPS = 15;
	/** frame length */
	private static final double FRAME_TIME = 1d / FPS;
	
	private double[][] population;
	
	/** the x coordinate of the robot that would be placed on left */
	private static final double PLACEMENT_LEFT = 20; // in cm
	/** the x coordinate of the robot that would be placed on right */
	private static final double PLACEMENT_RIGHT = WorldState.PITCH_WIDTH_CM - PLACEMENT_LEFT; // in cm
	
	private static final int GAMETIME = 3*60; // in sec
	
	private long[] scores = new long[2];
	
	private double timeElapsed = 0;
	
	private AIMaster leftAI, rightAI;
	
	private SimulatorPhysicsEngine sim;
	
	// calculate fitness section
	
	/**
	 * When new frame is available, analyse the {@link #state}
	 */
	public void onNewFrame() {
		
	}
	
	public void onTimeOut() {
		// mark end of game
		simulateGame = false;
	}

	/**
	 * If the ball goes into the left goal
	 */
	@Override
	public void onLeftScore() {
		System.out.println("LEFT SCORE");
		scores[1]+=5000;
		resetPitch();
	}

	/**
	 * If the ball goes into the right goal
	 */
	@Override
	public void onRightScore() {
		System.out.println("RIGHT SCORE");
		scores[0]+=5000;
		resetPitch();
	}

	/**
	 * If the yellow robot collides with an obstacle
	 */
	@Override
	public void onYellowCollide() {
		//System.out.println("Yellow collided");
		scores[1]-=100;
	}

	/**
	 * If the blue robot collides with an obstacle
	 */
	@Override
	public void onBlueCollide() {
		//System.out.println("Blue collided");
		scores[0]-=100;
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
		sim = new SimulatorPhysicsEngine(false);
		
		final VBrick leftBrick = new VBrick(),
					rightBrick = new VBrick();
		
		leftAI = new AIMaster(leftBrick, sim, new AINeuralNet(population[ids[0]]));
		leftAI.setPrintStateChanges(false);
		rightAI = new AIMaster(rightBrick, sim, new AINeuralNet(population[ids[1]]));
		rightAI.setPrintStateChanges(false);
		
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
		
		sim.callback = this;
		
		leftAI.start(true, true);
		rightAI.start(false, false);
		
		leftAI.setState(AIState.PLAY);
		rightAI.setState(AIState.PLAY);
		
		for (int i = 0; i < Simulator.DELAY_SIZE; i++) {
			sim.simulate(FRAME_TIME);
			sim.delayQueue.add(sim.getWorldState());
		}
		
		// runs simulation
		while (simulateGame) {
			sim.simulate(FRAME_TIME);
			state = sim.getWorldState();
			sim.delayQueue.add(state);
			sim.broadcastState(sim.delayQueue.poll());
			timeElapsed += FRAME_TIME;
			onNewFrame();
			if (timeElapsed > GAMETIME) {
				onTimeOut();
				timeElapsed = 0;
			}
		}
		

		leftAI.stop();
		rightAI.stop();
		sim.stop();
		
		currentState = gamestate.finished;
		callback.onFinished(this, scores);

	}
	
	private void resetPitch() {
		sim.putAt(PLACEMENT_LEFT, WorldState.PITCH_HEIGHT_CM/2, 0, 0);
		sim.putAt(PLACEMENT_RIGHT, WorldState.PITCH_HEIGHT_CM/2, 1, 180);
		sim.putBallAt();
	}

	/**
	 * @return current game state
	 */
	public gamestate getState() {
		return currentState;
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

	
}
