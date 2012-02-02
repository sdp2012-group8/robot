package sdp.simulator;


import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import sdp.common.Robot;
import sdp.common.WorldState;
import sdp.common.WorldStateProvider;

/**
 * This is a simulator. It simulates a table
 * 
 * @author s0932707
 *
 */
public class Simulator extends WorldStateProvider {
	
	private static final float max_fps = 15; // simulation speed
	private static final double iteration_time = 1000/max_fps; // in ms
	private final static double pitch_width_cm = 244;
	private final static double pitch_height_cm = 114;
	
	private static final int MAX_NUM_ROBOTS = 2;
	
	private static VBrick[] robot = new VBrick[MAX_NUM_ROBOTS]; // blue has id 0, yellow has id 1
	private static Vector2D[] positions = new Vector2D[MAX_NUM_ROBOTS];
	private static double[] directions = new double[MAX_NUM_ROBOTS];
	

	private boolean running = true;
	
	public Simulator() {
		new Thread() {
			
			public void run() {
				long delta_time = 0;
				long old_time = System.currentTimeMillis();
				long curr_time;
				while(running) {
					// call simulation giving time elapsed
					simulate(delta_time/1000d);
					// calculate time required for simulation to return
					curr_time = System.currentTimeMillis();
					delta_time = curr_time-old_time;
					// if smaller than fps value, sleep for the remaining time
					if (delta_time < iteration_time)
						try {
							Thread.sleep((long) iteration_time-delta_time);
						} catch (Exception e) {}
					// recalculate delta
					curr_time = System.currentTimeMillis();
					delta_time = curr_time-old_time;
					old_time = curr_time;
					
				}
				
			};
			
		}.start();
	}
	
	/**
	 * Register blue robot at given position in cm
	 * @param virtual
	 * @param x
	 * @param y
	 */
	public void registerBlue(VBrick virtual, double x, double y) {
		registerRobot(virtual, x, y, 0);
	}
	
	/**
	 * Register yellow robot at given position in cm
	 * @param virtual
	 * @param x
	 * @param y
	 */
	public void registerYellow(VBrick virtual, double x, double y) {
		registerRobot(virtual, x, y, 1);
	}
	
	/**
	 * Register a robot at a given place. Current world providers can provide a state only for two robots
	 * 
	 * @param virtual the robot to register
	 * @param x initial position in cm
	 * @param y initial position in cm
	 * @param id
	 */
	private void registerRobot(VBrick virtual, double x, double y, int id) {
		robot[id] = virtual;
		positions[id] = new Vector2D(x, y);
	}
	
	/**
	 * Gracefully close
	 */
	private void stop() {
		running = false;
	}
	
	/**
	 * Do the simulation of the physics
	 * @param dt time elapsed since last call in s
	 */
	public void simulate(double dt) {
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				positions[i].addmul_to(robot[i].getRobotVelocity(dt), dt);
				directions[i] = robot[i].getDirection();
			}
		Robot blue = positions[0] == null ? new Robot(new Point2D.Double(0,0), 0) : new Robot(positions[0].getPoint(1/pitch_width_cm), directions[0]);
		Robot yellow = positions[1] == null ? new Robot(new Point2D.Double(0,0), 0) : new Robot(positions[1].getPoint(1/pitch_width_cm), directions[1]);
		WorldState state = new WorldState(new Point2D.Double(0.5,0.2), blue, yellow, null);
		setChanged();
		notifyObservers(state);
	}


}
