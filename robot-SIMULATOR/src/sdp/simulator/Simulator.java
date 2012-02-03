package sdp.simulator;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import sdp.common.Robot;
import sdp.common.Tools;
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
	private final static double ball_friction_acc = 10; // in cm/s
	
	private final static int IMAGE_WIDTH = 640;
	
	private static final int MAX_NUM_ROBOTS = 2;
	
	private static VBrick[] robot = new VBrick[MAX_NUM_ROBOTS]; // blue has id 0, yellow has id 1
	private static Vector2D[] positions = new Vector2D[MAX_NUM_ROBOTS];
	private static Vector2D 
			ball = Vector2D.multiply(new Vector2D(pitch_middle), pitch_width_cm),
			ball_velocity = Vector2D.ZERO();
	private static double[] directions = new double[MAX_NUM_ROBOTS];
	private static Vector2D[] velocities = new Vector2D[MAX_NUM_ROBOTS];
	private Robot blue = new Robot(new Vector2D(0.2, 0.2), 0), yellow = new Robot(new Vector2D(0.8, 0.2), 0);
	private BufferedImage im = null;
	private Graphics2D g = null;
	

	private boolean running = true;
	
	public Simulator() {
		robot[0] = new VBrick();
		robot[1] = new VBrick();
		positions[0] = Vector2D.multiply(new Vector2D(0.2, 0.2), pitch_width_cm);
		positions[1] = Vector2D.multiply(new Vector2D(0.8, 0.2), pitch_width_cm);
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
	public void stop() {
		running = false;
		g.dispose();
	}
	
	/**
	 * Do the simulation of the physics
	 * @param dt time elapsed since last call in s
	 */
	public void simulate(double dt) {
		// ball robot positions
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				robot[i].calculateVelocity(dt);
				velocities[i] = robot[i].getVelocity();
				positions[i].addmul_to(velocities[i], dt);
				directions[i] = robot[i].getDirection();
			}
		if (positions[0] != null)
			blue.setCoords(Vector2D.divide(positions[0], pitch_width_cm), directions[0]);
		if (positions[1] != null)
			yellow.setCoords(Vector2D.divide(positions[1], pitch_width_cm), directions[1]);
		// ball collision with robots
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				// transform ball into "brick" relative coordinates
				Vector2D ball_pos = Vector2D.rotateVector(Vector2D.subtract(ball, positions[i]), -directions[i]);
				// check whether the ball is "inside" brick
				if (
						ball_pos.getX() > VBrick.back_left.getX() && 
						ball_pos.getX() < VBrick.front_right.getX() &&
						ball_pos.getY() > VBrick.front_right.getY() &&
						ball_pos.getY() < VBrick.back_left.getY()
				) {
					// if we have collision:
					// get relative speed
					Vector2D rel_spd = Vector2D.rotateVector(Vector2D.subtract(ball_velocity, velocities[i]), -directions[i]);
					// get penetrations
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
						ball_pos.setX(VBrick.back_left.getX());
						rel_spd.setX(-rel_spd.getX()+min/dt);
					} else if (right_pen == min) {
						ball_pos.setX(VBrick.front_right.getX());
						rel_spd.setX(-rel_spd.getX()-min/dt);
					} else if (bottom_pen == min) {
						ball_pos.setY(VBrick.back_left.getY());
						rel_spd.setY(-rel_spd.getY()-min/dt);
					} else {
						ball_pos.setY(VBrick.front_right.getY());
						rel_spd.setY(-rel_spd.getY()+min/dt);
					}
					// correct position and use it to give the ball some momentum
					ball = Vector2D.add(Vector2D.rotateVector(ball_pos, directions[i]), positions[i]);
					ball_velocity = Vector2D.add(Vector2D.rotateVector(rel_spd, directions[i]), velocities[i]);
				}
			}
		// ball collision with walls
		if (ball.getX() < 0) {
			// collision with left wall
			double pen = 0 - ball.getX();
			ball.setX(0);
			ball_velocity.setX(-ball_velocity.getX());
		} else if (ball.getX() > pitch_width_cm) {
			// collision with right wall
			double pen = ball.getX() - pitch_width_cm;
			ball.setX(pitch_width_cm);
			ball_velocity.setX(-ball_velocity.getX());
		} else if (ball.getY() < 0) {
			// collision with bottom wall
			double pen = 0 - ball.getY();
			ball.setY(0);
			ball_velocity.setY(-ball_velocity.getY());
		} else if (ball.getY() > pitch_height_cm) {
			// collision with top wall
			double pen = ball.getY() - pitch_height_cm;
			ball.setY(pitch_height_cm);
			ball_velocity.setY(-ball_velocity.getY());
		}
		// ball friction
		double ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= ball_friction_acc*dt) {
				Vector2D friction = Vector2D.change_length(ball_velocity, -ball_friction_acc);
				ball_velocity.addmul_to(friction, dt);
			} else
				ball_velocity = Vector2D.ZERO();
		}
		// ball position
		ball.addmul_to(ball_velocity, dt);
		WorldState state = new WorldState(Vector2D.divide(ball, pitch_width_cm), blue, yellow, image(dt));
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
				robot = blue;
				break;
			case 1:
				g.setColor(new Color(220, 220, 0));
				robot = yellow;
				break;
			default:
				g.setColor(Color.gray);
				robot = new Robot(Vector2D.divide(positions[i], pitch_width_cm), directions[i]);
				break;
			}
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
