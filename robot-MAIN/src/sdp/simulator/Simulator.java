package sdp.simulator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import sdp.AI.Command;
import sdp.common.Painter;
import sdp.common.Utilities;
import sdp.common.WorldStateProvider;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public abstract class Simulator extends WorldStateProvider {

	// time keeping
	protected static final float MAX_FPS = 25; // simulation speed
	protected static final double ITERATION_TIME = 1000 / MAX_FPS; // in ms
	protected static final int DELAY_SIMULATION = 500; // in ms
	protected Queue<WorldState> delayQueue = new LinkedList<WorldState>();
	protected static final int DELAY_SIZE = (int) (DELAY_SIMULATION/ITERATION_TIME);

	// image data
	public static final int IMAGE_WIDTH = 640;
	public static final int IMAGE_HEIGHT = (int) (IMAGE_WIDTH
			* WorldState.PITCH_HEIGHT_CM / WorldState.PITCH_WIDTH_CM);
	protected static final int IMAGE_INFO_SEC_HEIGHT = 100;
	protected BufferedImage im = null;
	
	// misc constants
	protected static final double BALL_RADIUS = 4.27 / 2;
	protected static final double KICKER_RANGE = 10; // cm
	protected static final double KICKER_MAX_SPEED = 300; // cm/s
	protected static final double KICKER_MIN_SPEED = 50; // cm/s
	protected static final double GOAL_SIZE = 60; // cm

	// keeping score
	protected int SCORE_LEFT = 0, SCORE_RIGHT = 0;

	// mouse interaction
	protected boolean MOUSE_OVER_BALL = false;
	protected int MOUSE_OVER_ROBOT = -1;
	protected Integer reference_robot_id = null;
	
	// define robots
	protected static final int MAX_NUM_ROBOTS = 2;
	protected VBrick[] robot = new VBrick[MAX_NUM_ROBOTS]; // blue has id 0
	protected double[] speeds = new double[MAX_NUM_ROBOTS];
	protected double[] turning_speeds = new double[MAX_NUM_ROBOTS];
	
	// realtime constatns
	private boolean running = false, paused = false;
	
	
	// housekeeping simulator functions

	/**
	 * Sets the simulator so that it matches the current world state.
	 * @param ws the state that needs to be matched
	 * @param dt time passed since last setting of worldstate (in order to calculate velocities). Set to 0 if you don't need velocities.
	 * @param is_ws_in_cm is the world state in centimeters
	 * @param command to be sent to the brick or null if not available
	 * @param am_i_blue for use with a command. true if my robot blue, false otherwise. Put null if your command is null
	 */
	public abstract void setWorldState(WorldState ws, double dt,
			boolean is_ws_in_cm, Command command, Boolean am_i_blue);
	
	/**
	 * Starts the thread that would do real time simulation
	 */
	protected void startRealTimeThread() {
		
		running = true;
		
		new Thread() {

			public void run() {
				// prepare queue
				for (int i = 0; i < DELAY_SIZE; i++) {
					simulate(ITERATION_TIME/1000d);
					delayQueue.add(getWorldState());
				}
				
				long delta_time = 0;
				long old_time = System.currentTimeMillis();
				long curr_time;
				while (running) {
					// call simulation giving time elapsed
					if (!paused) {
						double dt = delta_time / 1000d;
						simulate(dt);
						WorldState current = getWorldState();
						delayQueue.add(current);
						WorldState state = delayQueue.poll();
						image(dt, state, current);
						setChanged();
						notifyObservers(state);
					}
					// calculate time required for simulation to return
					curr_time = System.currentTimeMillis();
					delta_time = curr_time - old_time;
					// if smaller than fps value, sleep for the remaining time
					if (delta_time < ITERATION_TIME)
						try {
							Thread.sleep((long) ITERATION_TIME - delta_time);
						} catch (Exception e) {}
					// recalculate delta
					curr_time = System.currentTimeMillis();
					delta_time = curr_time - old_time;
					old_time = curr_time;

				}

			};

		}.start();
	}

	private WorldState getWorldState() {
		
		final Vector2D[] positions = getRobotPositions();
		final Vector2D ball = getBall();
		final double[] directions = getRbotDirections();
		
		return new WorldState(Vector2D.divide(ball, WorldState.PITCH_WIDTH_CM),
				new Robot(Vector2D.divide(positions[0], WorldState.PITCH_WIDTH_CM),
						directions[0]), new Robot(Vector2D.divide(positions[1],
								WorldState.PITCH_WIDTH_CM), directions[1]), im);
	}
	
	/**
	 * Simulate this amount of time with this framerate according to the iternal state of the simulator.
	 * 
	 * @param a simulator to be used. Best to create a new simulator here
	 * @param time_ms the time in future to simulate in milliseconds
	 * @param fps expected emulated simulation frames per seconds
	 * @param states last several states in hierarchical order
	 * @param is_ws_in_cm is the world state in centimeters
	 * @param command to be sent to the brick or null if not available
	 * @param am_i_blue for use with a command. true if my robot blue, false otherwise.
	 * @return what the world is expected to be after this amount of time
	 */
	public static WorldState simulateWs(Simulator sim, long time_ms, int fps, WorldState[] states, boolean is_ws_in_cm, Command command, boolean am_i_blue) {
		double sec = time_ms / 1000d;
		double dt = 1d / fps;
		double duration = dt*(states.length-1);
		
		sim.setWorldState(states[0], 0, is_ws_in_cm, command, am_i_blue);
		sim.setWorldState(states[states.length-1], duration, is_ws_in_cm, command, am_i_blue);
		
		// do simulation
		WorldState ws = null;
		
		for (double t = 0; t <= sec; t+=dt) {
			sim.simulate(dt);
			ws = sim.getWorldState();
		}
			
		return Utilities.toCentimeters(ws);
	}	

	
	/**
	 * 
	 * @param id
	 *            follow a
	 */
	public void centerViewAround(Integer id) {
		reference_robot_id = id;
	}

	/**
	 * @param paused
	 *            true for pause, false for resume
	 */
	public void setPause(boolean paused) {
		this.paused = paused;
	}

	/**
	 * @return true if simulator is in pause
	 */
	public boolean getPause() {
		return paused;
	}

	/**
	 * Put ball at given location
	 * 
	 * @param x
	 *            from 0 to 1
	 * @param y
	 *            same as sx
	 */
	public abstract void putBallAt(double x, double y);

	public void putBallAt() {
		putBallAt(0.5, WorldState.PITCH_HEIGHT_CM / (2 * WorldState.PITCH_WIDTH_CM));
	}

	/**
	 * Put robot at given location
	 * 
	 * @param x
	 *            from 0 to 1
	 * @param y
	 *            same as sx
	 * @param id
	 *            robot_id
	 * @param direction
	 *            direction in degrees
	 */
	public abstract void putAt(double x, double y, int id, double direction);

	/**
	 * Put robot at given location
	 * 
	 * @param x
	 *            from 0 to 1
	 * @param y
	 *            same as sx
	 * @param id
	 *            robot_id
	 */
	public abstract void putAt(double x, double y, int id);

	/**
	 * Whether or not to highlight the ball (for mouseover)
	 * 
	 * @param highlight
	 *            true to highlight false to remove highlight
	 */
	public void highlightBall(boolean highlight) {
		MOUSE_OVER_BALL = highlight;
	}

	/**
	 * Whether or not to highlight robot
	 * 
	 * @param id
	 *            -1 to unhighlight, robot id to highlight
	 */
	public void highlightRobot(int id) {
		MOUSE_OVER_ROBOT = id;
	}

	/**
	 * True if given point is inside ball
	 * 
	 * @param sx
	 *            from 0 to 1
	 * @param sy
	 *            same as sx
	 * @return
	 */
	public boolean isInsideBall(double sx, double sy) {
		
		final Vector2D ball = getBall();
		
		double x = sx * WorldState.PITCH_WIDTH_CM;
		double y = sy * WorldState.PITCH_WIDTH_CM;
		return Math.abs(x - ball.getX()) <= BALL_RADIUS
				&& Math.abs(y - ball.getY()) <= BALL_RADIUS;
	}

	/**
	 * True if given point is a robot
	 * 
	 * @param sx
	 *            from 0 to 1
	 * @param sy
	 *            same as sx
	 * @return the id of the robot or -1 if it's not
	 */
	public int isInsideRobot(double sx, double sy) {
		
		final Vector2D[] positions = getRobotPositions();
		final double[] directions = getRbotDirections();
		
		double x = sx * WorldState.PITCH_WIDTH_CM;
		double y = sy * WorldState.PITCH_WIDTH_CM;
		for (int i = 0; i < robot.length; i++) {
			Vector2D rel_coords = Vector2D.rotateVector(
					Vector2D.subtract(new Vector2D(x, y), positions[i]),
					-directions[i]);
			if (rel_coords.getX() > VBrick.back_left.getX()
					&& rel_coords.getX() < VBrick.front_right.getX()
					&& rel_coords.getY() > VBrick.front_right.getY()
					&& rel_coords.getY() < VBrick.back_left.getY())
				return i;
		}
		return -1;
	}
	

	public void registerBlue(VBrick virtual, double x, double y, double dir) {
		registerRobot(virtual, x, y, 0, dir);
	}


	public void registerBlue(VBrick virtual, double x, double y) {
		registerBlue(virtual, x, y, 0);
	}


	public void registerYellow(VBrick virtual, double x, double y, double dir) {
		registerRobot(virtual, x, y, 1, dir);
	}


	public void registerYellow(VBrick virtual, double x, double y) {
		registerYellow(virtual, x, y, 0);
	}
	
	/**
	 * Register a robot at a given place. Current world providers can provide a
	 * state only for two robots
	 * 
	 * @param virtual
	 *            the robot to register
	 * @param x
	 *            initial position in cm
	 * @param y
	 *            initial position in cm
	 * @param dir
	 *            direction in degrees
	 * @param id
	 */
	private void registerRobot(VBrick virtual, double x, double y, int id, double dir) {
		robot[id] = virtual;
		putAt(x / WorldState.PITCH_WIDTH_CM, y / WorldState.PITCH_WIDTH_CM, id, dir);
	}

	/**
	 * Gracefully close
	 */
	public void stop() {
		running = false;
	}
	
	/**
	 * Do the physics simulation so that we could call. Make sure you call {@link #calculateBrickSpeeds(double)} somewhere inside to get
	 * the most up-to date speeds at which the brick think they are traveling.
	 * {@link #getBall()}, {@link #getRbotDirections()} and {@link #getRobotPositions()} safely.
	 * @param dt simulation time
	 */
	protected abstract void simulate(double dt);
	
	private void image(double dt, WorldState state, WorldState real) {
		final Vector2D[] positions = getRobotPositions();
		final Vector2D ball = getBall();
		final double[] directions = getRbotDirections();
		
		if (im == null) {
			im = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT
					+ IMAGE_INFO_SEC_HEIGHT, BufferedImage.TYPE_INT_RGB);
		}
		// draw table
		WorldState state_cm = Utilities.toCentimeters(state);
		Painter p = new Painter(im, state_cm);
		p.setOffsets(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		p.MOUSE_OVER_BALL = MOUSE_OVER_BALL;
		p.MOUSE_OVER_ROBOT = MOUSE_OVER_ROBOT;
		p.reference_robot_id = reference_robot_id;
		p.g.setColor(Color.BLACK);
		p.g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT + IMAGE_INFO_SEC_HEIGHT);
		p.g.setColor(new Color(10, 80, 0));
		p.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		// draw goals
		p.g.setColor(new Color(180, 180, 180));
		p.fillRect(0,
				(int) (IMAGE_WIDTH*(WorldState.PITCH_HEIGHT_CM/2-GOAL_SIZE/2)/WorldState.PITCH_WIDTH_CM),
				(int) (IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
				(int) (IMAGE_WIDTH*GOAL_SIZE/WorldState.PITCH_WIDTH_CM));
		p.fillRect((int) (IMAGE_WIDTH - IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
				(int) (IMAGE_WIDTH*(WorldState.PITCH_HEIGHT_CM/2-GOAL_SIZE/2)/WorldState.PITCH_WIDTH_CM),
				(int) (IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
				(int) (IMAGE_WIDTH*GOAL_SIZE/WorldState.PITCH_WIDTH_CM));

		//p.image(true,true);
		
		sketchWs(p, state, true);
		sketchWs(p, real, false);
		
		
		// draw Strings
		p.g.setColor(Color.BLACK);
		p.g.fillRect(0, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_INFO_SEC_HEIGHT);
		p.g.setColor(Color.white);
		p.g.drawString((int) (1 / dt) + " fps", IMAGE_WIDTH - 50, 20);
		p.g.drawString("Score: " + SCORE_LEFT + " : " + SCORE_RIGHT,
				IMAGE_WIDTH / 2, 20);
		p.g.drawString(
				"blue - ball: "
						+ String.format("%.1f", (Vector2D.subtract(ball,
								positions[0]).getLength()))
								+ " cm; "
								+ String.format("%.1f",
										Vector2D.getBetweenAngle(ball, positions[0])) + "°",
										20, IMAGE_HEIGHT + 20);
		p.g.drawString(
				"blue : " + positions[0] + "; "
						+ String.format("%.1f", directions[0]) + "°", 20,
						IMAGE_HEIGHT + 40);
		p.g.drawString(
				"yellow - ball: "
						+ String.format("%.1f", (Vector2D.subtract(ball,
								positions[1]).getLength()))
								+ " cm; "
								+ String.format("%.1f",
										Vector2D.getBetweenAngle(ball, positions[1])) + "°",
										20, IMAGE_HEIGHT + 60);
		p.g.drawString(
				"yellow : " + positions[1] + "; "
						+ String.format("%.1f", directions[1]) + "°", 20,
						IMAGE_HEIGHT + 80);
		p.g.drawString("ball : " + ball, IMAGE_WIDTH - 150, IMAGE_HEIGHT + 20);
		p.dispose();
	}
	
	protected void sketchWs(Painter p, WorldState ws, boolean fill) {
		// draw current frame
				
				for (int i = 0; i < 2; i++) {
					Robot robot = i == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
					p.g.setColor(i == 0 ? new Color(150, 150, 255, 200) : new Color(225, 225, 150, 200));
					p.g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
							BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f },
							0.0f));
					p.fillPolygon(new int[] {
							(int)(robot.getFrontLeft().getX()*IMAGE_WIDTH),
							(int)(robot.getFrontRight().getX()*IMAGE_WIDTH),
							(int)(robot.getBackRight().getX()*IMAGE_WIDTH),
							(int)(robot.getBackLeft().getX()*IMAGE_WIDTH),
							(int)(robot.getFrontLeft().getX()*IMAGE_WIDTH)
					}, new int[] {
							(int)(robot.getFrontLeft().getY()*IMAGE_WIDTH),
							(int)(robot.getFrontRight().getY()*IMAGE_WIDTH),
							(int)(robot.getBackRight().getY()*IMAGE_WIDTH),
							(int)(robot.getBackLeft().getY()*IMAGE_WIDTH),
							(int)(robot.getFrontLeft().getY()*IMAGE_WIDTH)
					}, 5, fill);
					
					p.g.setColor(new Color(255, 255, 255, 200));
					p.g.setStroke(new BasicStroke(1.0f));
					double dir_x = 0.04*Math.cos(robot.getAngle()*Math.PI/180d);
					double dir_y = -0.04*Math.sin(robot.getAngle()*Math.PI/180d);
					p.drawLine(
							(int)((robot.getCoords().getX())*IMAGE_WIDTH),
							(int)((robot.getCoords().getY())*IMAGE_WIDTH),
							(int)((robot.getCoords().getX()+dir_x)*IMAGE_WIDTH),
							(int)((robot.getCoords().getY()+dir_y)*IMAGE_WIDTH));
				}
				// draw current ball
				p.g.setStroke(new BasicStroke(1.0f));
				p.g.setColor(new Color(255, 255, 255, 200));
				p.fillOval(
						(int) (ws.getBallCoords().getX() * IMAGE_WIDTH - BALL_RADIUS * IMAGE_WIDTH / WorldState.PITCH_WIDTH_CM),
						(int) (ws.getBallCoords().getY() * IMAGE_WIDTH - BALL_RADIUS * IMAGE_WIDTH / WorldState.PITCH_WIDTH_CM),
						(int) (2 * BALL_RADIUS * IMAGE_WIDTH / WorldState.PITCH_WIDTH_CM),
						(int) (2 * BALL_RADIUS * IMAGE_WIDTH / WorldState.PITCH_WIDTH_CM), fill);
	}

	/**
	 * Call this inside your {@link #simulate(double)} to calculate brick speeds from the commands that are being sent to the bricks
	 * @param dt time elapsed
	 */
	protected void calculateBrickSpeeds(double dt) {
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				speeds[i] = robot[i].calculateSpeed(speeds[i], dt);
				turning_speeds[i] = robot[i].calculateTurningSpeed(
						turning_speeds[i], dt);
			}
	}
	
	/**
	 * Get ball coordinates
	 * @return
	 */
	protected abstract Vector2D getBall();
	
	/**
	 * Get robot coordinates. 0 is blue, 1 is yellow
	 * @return
	 */
	protected abstract Vector2D[] getRobotPositions();
	
	/**
	 * Get robot directions. 0 is blue, 1 is yellow
	 * @return
	 */
	protected abstract double[] getRbotDirections();
}