package sdp.simulator;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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
	
	private static final float max_fps = 25; // simulation speed
	private static final double iteration_time = 1000/max_fps; // in ms
	private final static double pitch_width_cm = 244;
	private final static double pitch_height_cm = 113.7;
	private final static Vector2D pitch_middle = new Vector2D(0.5, pitch_height_cm/(2*pitch_width_cm));
	private final static double ball_friction_acc = 15; // in cm/s
	private final static double mass_robot_kg = 1; // in kg
	private final static double mass_ball_kg = 0.045; // in kg
	
	private final static double wall_bounciness = 0.5; // 0 - inelastic, 1 - elastic
	private final static double robot_bounciness = 0.9; // 0 - 1
	
	private final static double kicker_range = 10; // cm
	private final static double kicker_max_speed = 300; // cm/s
	private final static double kicker_min_speed = 50; // cm/s
	
	private final static double flipper_size = 2; //cm
	
	private final static int IMAGE_WIDTH = 640;
	
	private static final int MAX_NUM_ROBOTS = 2;
	
	// define robots
	private static VBrick[] robot = new VBrick[MAX_NUM_ROBOTS]; // blue has id 0, yellow has id 1
	private static Vector2D[]
			positions = new Vector2D[MAX_NUM_ROBOTS],
			velocities = new Vector2D[MAX_NUM_ROBOTS];
	private static double[]
			directions = new double[MAX_NUM_ROBOTS],
			speeds = new double[MAX_NUM_ROBOTS],
			turning_speeds = new double[MAX_NUM_ROBOTS];
	// for use for collision prediction
	private static Vector2D[]
			future_positions = new Vector2D[MAX_NUM_ROBOTS],
			future_velocities = new Vector2D[MAX_NUM_ROBOTS];
	private static double[]
			future_directions = new double[MAX_NUM_ROBOTS],
			future_speeds = new double[MAX_NUM_ROBOTS],
			future_turning_speeds = new double[MAX_NUM_ROBOTS];
	// define ball
	private static Vector2D 
			ball = Vector2D.multiply(new Vector2D(pitch_middle), pitch_width_cm),
			ball_velocity = Vector2D.ZERO(),
	// for use for collision prediction
			future_ball = Vector2D.multiply(new Vector2D(pitch_middle), pitch_width_cm),
			future_ball_velocity = Vector2D.ZERO();
	// define graphics
	private BufferedImage im = null;
	private Graphics2D g = null;
	

	private boolean running = true;
	
	public Simulator() {
		registerBlue(new VBrick(), 40, pitch_height_cm/2);
		registerYellow(new VBrick(), pitch_width_cm-40, pitch_height_cm/2);
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
		velocities[id] = Vector2D.ZERO();
		directions[id] = 0;
		speeds[id] = 0;
		turning_speeds[id] = 0;
	}
	
	/**
	 * Gracefully close
	 */
	public void stop() {
		running = false;
		g.dispose();
	}
	
	/**
	 * Do the simulation of the physics.
	 * 
	 * Collision detection is implemented through prediction.
	 * 
	 * @param dt time elapsed since last call in s
	 */
	public void simulate(double dt) {
		// for prediction
		
		// calculate for a future
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				future_speeds[i] = robot[i].calculateSpeed(speeds[i], 2*dt);
				future_turning_speeds[i] = robot[i].calculateTurningSpeed(turning_speeds[i], 2*dt);
				future_directions[i] = directions[i]+turning_speeds[i]*2*dt;
				// there is a problem in the vision system which returns a "reversed y" coordinates
				// remove the - in front of direction in case this is fixed
				if (future_velocities[i] == null)
					future_velocities[i] = Vector2D.ZERO();
				future_velocities[i].setLocation(
						speeds[i]*Math.cos(directions[i]*Math.PI/180),
						speeds[i]*Math.sin(-directions[i]*Math.PI/180));
				future_positions[i] = new Vector2D(positions[i]);
				future_positions[i].addmul_to(velocities[i], 2*dt);
			}
		// future ball friction
		double ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= ball_friction_acc*2*dt) {
				Vector2D friction = Vector2D.change_length(ball_velocity, -ball_friction_acc);
				future_ball_velocity = new Vector2D(ball_velocity);
				future_ball_velocity.addmul_to(friction, 2*dt);
			} else
				future_ball_velocity = Vector2D.ZERO();
		}
		// future calculate final ball position
		future_ball = new Vector2D(ball);
		future_ball.addmul_to(ball_velocity, 2*dt);
		// calculate final positions
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				speeds[i] = robot[i].calculateSpeed(speeds[i], dt);
				turning_speeds[i] = robot[i].calculateTurningSpeed(turning_speeds[i], dt);
				directions[i]+=turning_speeds[i]*dt;
				// there is a problem in the vision system which returns a "reversed y" coordinates
				// remove the - in front of direction in case this is fixed
				velocities[i].setLocation(
						speeds[i]*Math.cos(directions[i]*Math.PI/180),
						speeds[i]*Math.sin(-directions[i]*Math.PI/180));
				positions[i].addmul_to(velocities[i], dt);
			}
		// ball friction
		ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= ball_friction_acc*dt) {
				Vector2D friction = Vector2D.change_length(ball_velocity, -ball_friction_acc);
				ball_velocity.addmul_to(friction, dt);
			} else
				ball_velocity = Vector2D.ZERO();
		}
		// calculate final ball position
		ball.addmul_to(ball_velocity, dt);
		
		// ball collision with robots
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				// transform ball into "brick" relative coordinates
				Vector2D ball_pos = Vector2D.rotateVector(Vector2D.subtract(future_ball, future_positions[i]), -future_directions[i]);
				Vector2D curr_rel_spd = Vector2D.rotateVector(Vector2D.subtract(ball_velocity, velocities[i]), -directions[i]);
				// check whether the ball is "inside" brick
				if (
						ball_pos.getX() > VBrick.back_left.getX() && 
						ball_pos.getX() < VBrick.front_right.getX() &&
						ball_pos.getY() > VBrick.front_right.getY() &&
						ball_pos.getY() < VBrick.back_left.getY()
				) {
					// collisions with walls
					double left_pen = ball_pos.getX() - VBrick.back_left.getX();
					double right_pen = VBrick.front_right.getX() - ball_pos.getX();
					double top_pen = ball_pos.getY() - VBrick.front_right.getY();
					double bottom_pen = VBrick.back_left.getY() - ball_pos.getY();
					double min = left_pen;
					// use the smallest penetration to correct position
					if (right_pen < min) min = right_pen;
					if (top_pen < min) min = top_pen;
					if (bottom_pen < min) min = bottom_pen;
					if (left_pen == min) {
						curr_rel_spd.setX(-curr_rel_spd.getX());
					} else if (right_pen == min) {
						curr_rel_spd.setX(-curr_rel_spd.getX());
					} else if (bottom_pen == min) {
						curr_rel_spd.setY(-curr_rel_spd.getY());
					} else {
						curr_rel_spd.setY(-curr_rel_spd.getY());
					}
					curr_rel_spd = Vector2D.multiply(curr_rel_spd, robot_bounciness);
					//double mom_inertia = mass_robot_kg*(VBrick.ROBOT_LENGTH*VBrick.ROBOT_LENGTH+VBrick.ROBOT_WIDTH*VBrick.ROBOT_WIDTH)/12;
					Vector2D current_dot_pos = Vector2D.rotateVector(ball_pos, -turning_speeds[i]*dt);
					Vector2D add_vel = Vector2D.divide(Vector2D.subtract(ball_pos, current_dot_pos), dt);
					curr_rel_spd = Vector2D.add(curr_rel_spd, add_vel);
				}
				// kicker
				double ball_distance = ball_pos.x-VBrick.front_left.getX();
				if (robot[i].is_kicking) {
					if (ball_distance < kicker_range && ball_distance > 0) {
						double power = kicker_max_speed-kicker_min_speed-(kicker_max_speed-kicker_min_speed)*ball_distance/kicker_range;
						System.out.print("power "+power);
						curr_rel_spd.setX(curr_rel_spd.getX()+power);
					}
					robot[i].is_kicking = false;
				}
				// apply velocity change
				ball_velocity = Vector2D.add(Vector2D.rotateVector(curr_rel_spd, directions[i]), velocities[i]);
			}
		// ball collision with walls
		if (future_ball.getX() < 0) {
			// collision with left wall
			ball_velocity.setX(-ball_velocity.getX());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		} else if (future_ball.getX() > pitch_width_cm) {
			// collision with right wall
			ball_velocity.setX(-ball_velocity.getX());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		} else if (future_ball.getY() < 0) {
			// collision with bottom wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		} else if (future_ball.getY() > pitch_height_cm) {
			// collision with top wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		}
		// notify that we have change
		WorldState state = new WorldState(
				Vector2D.divide(ball, pitch_width_cm),
				new Robot(Vector2D.divide(positions[0], pitch_width_cm), directions[0]),
				new Robot(Vector2D.divide(positions[1], pitch_width_cm), directions[1]), image(dt));
		setChanged();
		notifyObservers(state);
	}
	
	/**
	 * Creates visualization
	 * @return
	 */
	private BufferedImage image(double dt) {
		// create image if not existing
		if (im == null) {
			im = new BufferedImage(IMAGE_WIDTH, (int) (IMAGE_WIDTH*pitch_height_cm/pitch_width_cm), BufferedImage.TYPE_INT_RGB);
			g = im.createGraphics();
		}
		int width = im.getWidth();
		int height = im.getHeight();
		g.setColor(new Color(10, 80, 0));
		g.fillRect(0, 0, width, height);
		for (int i = 0; i < robot.length; i++) {
			Robot robot;
			// chose robot color
			switch (i) {
			case 0:
				g.setColor(Color.blue);
				break;
			case 1:
				g.setColor(new Color(220, 220, 0));
				break;
			default:
				g.setColor(Color.gray);
				break;
			}
			robot = new Robot(Vector2D.divide(positions[i], pitch_width_cm), directions[i]);
			// draw body of robot
			g.fillPolygon(new int[] {
					(int)(robot.getFrontLeft().getX()*width),
					(int)(robot.getFrontRight().getX()*width),
					(int)(robot.getBackRight().getX()*width),
					(int)(robot.getBackLeft().getX()*width),
					(int)(robot.getFrontLeft().getX()*width)
			}, new int[] {
					(int)(robot.getFrontLeft().getY()*width),
					(int)(robot.getFrontRight().getY()*width),
					(int)(robot.getBackRight().getY()*width),
					(int)(robot.getBackLeft().getY()*width),
					(int)(robot.getFrontLeft().getY()*width)
			}, 5);
			// draw direction pointer
			g.setColor(Color.white);
			double dir_x = 0.03*Math.cos(robot.getAngle()*Math.PI/180d);
			double dir_y = -0.03*Math.sin(robot.getAngle()*Math.PI/180d);
			g.drawLine(
					(int)(robot.getCoords().getX()*width),
					(int)(robot.getCoords().getY()*width),
					(int)((robot.getCoords().getX()+dir_x)*width),
					(int)((robot.getCoords().getY()+dir_y)*width));
		}
		// draw ball
		g.setColor(Color.red);
		g.fillOval(
				(int)(ball.getX()*width/pitch_width_cm) - 3,
				(int)(ball.getY()*width/pitch_width_cm) - 3,
				6, 6);
		// draw fps
		g.drawString((int)(1/dt)+" fps", width-50, 20);
		return im;
	}


}
