package sdp.AI.genetic;

import sdp.AI.neural.AINeuralNet;
import sdp.common.world.WorldState;
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
	private static final int FRAME_TIME = 1000 / FPS;
	
	/** the x coordinate of the robot that would be placed on left */
	private static final double PLACEMENT_LEFT = 20; // in cm
	/** the x coordinate of the robot that would be placed on right */
	private static final double PLACEMENT_RIGHT = WorldState.PITCH_WIDTH_CM - PLACEMENT_LEFT; // in cm
	
	
	// calculate fitness section
	
	/**
	 * When new frame is available, analyse the {@link #state}
	 */
	public void onNewFrame() {
		
	}

	/**
	 * If the ball goes into the left goal
	 */
	@Override
	public void onLeftScore() {
		// TODO Auto-generated method stub
		
		// mark end of game
		simulateGame = false;
	}

	/**
	 * If the ball goes into the right goal
	 */
	@Override
	public void onRightScore() {
		// TODO Auto-generated method stub
		
		// mark end of game
		simulateGame = false;
	}

	/**
	 * If the yellow robot collides with an obstacle
	 */
	@Override
	public void onYellowCollide() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * If the blue robot collides with an obstacle
	 */
	@Override
	public void onBlueCollide() {
		// TODO Auto-generated method stub
		
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
		new AINeuralNet(population[i]);
		new AINeuralNet(population[j]);
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
		
		// create simulator
		final SimulatorPhysicsEngine sim = new SimulatorPhysicsEngine(false);
		
		// reset pitch
		sim.registerBlue(new VBrick(),
				PLACEMENT_LEFT,
				WorldState.PITCH_HEIGHT_CM/2,
				0);
		sim.registerYellow(new VBrick(),
				PLACEMENT_RIGHT,
				WorldState.PITCH_HEIGHT_CM/2,
				180);
		sim.putBallAt();
		
		// runs simulation
		while (simulateGame) {
			sim.simulate(FRAME_TIME);
			state = sim.getWorldState();
			onNewFrame();
		}
		
		// finsih simulation
		currentState = gamestate.finished;
		callback.onFinished(this, new long[]{0, 1});
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
