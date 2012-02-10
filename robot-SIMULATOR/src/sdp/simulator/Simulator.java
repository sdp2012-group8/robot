package sdp.simulator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Vector2D;
import sdp.common.WorldState;
import sdp.common.WorldStateProvider;

/**
 * This is a simulator. It simulates a table
 * 
 * @author Martin Marinov
 * 
 */
public class Simulator extends WorldStateProvider {

	private static final float max_fps = 25; // simulation speed
	private static final double iteration_time = 1000 / max_fps; // in ms
	public final static double pitch_width_cm = Tools.PITCH_WIDTH_CM;
	public final static double pitch_height_cm = Tools.PITCH_HEIGHT_CM;
	private final static Vector2D pitch_middle = new Vector2D(0.5,
			pitch_height_cm / (2 * pitch_width_cm));
	private final static double ball_max_speed = 350; // cm/s
	private final static double ball_friction_acc = 25; // in cm/s
	private final static double ball_radius = 4.27 / 2; // in cm

	private WorldState state = null;

	private final static double wall_bounciness = 0.4; // 0 - inelastic, 1 -
														// elastic
	private final static double robot_bounciness = 0.3; // 0 - 1
	private final static double flipper_bounciness = 0.8; // 0 - 1
	private final static double goal_size = 60; // cm

	private final static double kicker_range = 10; // cm
	private final static double kicker_max_speed = 300; // cm/s
	private final static double kicker_min_speed = 50; // cm/s

	private final static double flipper_size = 2; // cm

	private final static int IMAGE_WIDTH = 640,
			IMAGE_HEIGHT = (int) (IMAGE_WIDTH * pitch_height_cm / pitch_width_cm),
			IMAGE_INFO_SEC_HEIGHT = 100;

	private static final int MAX_NUM_ROBOTS = 2;

	private static int score_left = 0, score_right = 0;

	private static boolean mouse_over_ball = false;
	private static int mouse_over_robot = -1;

	// define robots
	private static VBrick[] robot = new VBrick[MAX_NUM_ROBOTS]; // blue has id
																// 0, yellow has
																// id 1
	private static Vector2D[] positions = new Vector2D[MAX_NUM_ROBOTS],
			velocities = new Vector2D[MAX_NUM_ROBOTS];
	private static double[] directions = new double[MAX_NUM_ROBOTS],
			speeds = new double[MAX_NUM_ROBOTS],
			turning_speeds = new double[MAX_NUM_ROBOTS];
	private static boolean[] will_be_in_collision = new boolean[MAX_NUM_ROBOTS];
	// for use for collision prediction
	private static Vector2D[] future_positions = new Vector2D[MAX_NUM_ROBOTS],
			future_velocities = new Vector2D[MAX_NUM_ROBOTS];
	private static double[] future_directions = new double[MAX_NUM_ROBOTS],
			future_speeds = new double[MAX_NUM_ROBOTS],
			future_turning_speeds = new double[MAX_NUM_ROBOTS];
	// define ball
	private static Vector2D ball = Vector2D.multiply(
			new Vector2D(pitch_middle), pitch_width_cm),
			ball_velocity = Vector2D.ZERO(),
			// for use for collision prediction
			future_ball = Vector2D.multiply(new Vector2D(pitch_middle),
					pitch_width_cm), future_ball_velocity = Vector2D.ZERO();
	// define graphics
	private BufferedImage im = null;
	private Graphics2D g = null;

	private boolean paused = false;
	private boolean running = true;

	private Integer reference_robot_id = null;

	public Simulator() {
		registerBlue(new VBrick(), 40, pitch_height_cm / 2);
		registerYellow(new VBrick(), pitch_width_cm - 40, pitch_height_cm / 2);
		new Thread() {

			public void run() {
				long delta_time = 0;
				long old_time = System.currentTimeMillis();
				long curr_time;
				while (running) {
					// call simulation giving time elapsed
					if (!paused)
						simulate(delta_time / 1000d);
					// calculate time required for simulation to return
					curr_time = System.currentTimeMillis();
					delta_time = curr_time - old_time;
					// if smaller than fps value, sleep for the remaining time
					if (delta_time < iteration_time)
						try {
							Thread.sleep((long) iteration_time - delta_time);
						} catch (Exception e) {
						}
					// recalculate delta
					curr_time = System.currentTimeMillis();
					delta_time = curr_time - old_time;
					old_time = curr_time;

				}

			};

		}.start();
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
	public void putBallAt(double x, double y) {
		ball = new Vector2D(x * pitch_width_cm, y * pitch_width_cm);
		ball_velocity = Vector2D.ZERO();
	}

	/**
	 * Put ball at center
	 */
	public void putBallAt() {
		putBallAt(0.5, pitch_height_cm / (2 * pitch_width_cm));
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
	public void putAt(double x, double y, int id, double direction) {
		if (id < 0 || id >= MAX_NUM_ROBOTS)
			return;
		positions[id] = new Vector2D(x * pitch_width_cm, y * pitch_width_cm);
		velocities[id] = Vector2D.ZERO();
		directions[id] = direction;
		will_be_in_collision[id] = true;
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
	 */
	public void putAt(double x, double y, int id) {
		if (id < 0 || id >= MAX_NUM_ROBOTS)
			return;
		positions[id] = new Vector2D(x * pitch_width_cm, y * pitch_width_cm);
		velocities[id] = Vector2D.ZERO();
		will_be_in_collision[id] = true;
	}

	/**
	 * Whether or not to highlight the ball (for mouseover)
	 * 
	 * @param highlight
	 *            true to highlight false to remove highlight
	 */
	public void highlightBall(boolean highlight) {
		mouse_over_ball = highlight;
	}

	/**
	 * Whether or not to highlight robot
	 * 
	 * @param id
	 *            -1 to unhighlight, robot id to highlight
	 */
	public void highlightRobot(int id) {
		mouse_over_robot = id;
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
		double x = sx * pitch_width_cm;
		double y = sy * pitch_width_cm;
		return Math.abs(x - ball.getX()) <= ball_radius
				&& Math.abs(y - ball.getY()) <= ball_radius;
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
		double x = sx * pitch_width_cm;
		double y = sy * pitch_width_cm;
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

	/**
	 * Register blue robot at given position in cm
	 * 
	 * @param virtual
	 * @param xy
	 * @param y
	 * @param dir
	 *            direction in degrees
	 */
	public void registerBlue(VBrick virtual, double x, double y, double dir) {
		registerRobot(virtual, x, y, 0, dir);
	}

	/**
	 * Register blue robot at given position in cm
	 * 
	 * @param virtual
	 * @param xy
	 * @param y
	 */
	public void registerBlue(VBrick virtual, double x, double y) {
		registerBlue(virtual, x, y, 0);
	}

	/**
	 * Register yellow robot at given position in cm
	 * 
	 * @param virtual
	 * @param x
	 * @param y
	 * @param dir
	 *            direction in degrees
	 */
	public void registerYellow(VBrick virtual, double x, double y, double dir) {
		registerRobot(virtual, x, y, 1, dir);
	}

	/**
	 * Register yellow robot at given position in cm
	 * 
	 * @param virtual
	 * @param x
	 * @param y
	 */
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
	private void registerRobot(VBrick virtual, double x, double y, int id,
			double dir) {
		robot[id] = virtual;
		positions[id] = new Vector2D(x, y);
		velocities[id] = Vector2D.ZERO();
		directions[id] = dir;
		speeds[id] = 0;
		turning_speeds[id] = 0;
		will_be_in_collision[id] = false;
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
	 * @param dt
	 *            time elapsed since last call in s
	 */
	public void simulate(double dt) {
		// calculate for a future
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				future_speeds[i] = robot[i].calculateSpeed(speeds[i], 2 * dt);
				future_turning_speeds[i] = robot[i].calculateTurningSpeed(
						turning_speeds[i], 2 * dt);
				future_directions[i] = directions[i] + turning_speeds[i] * 2
						* dt;
				// there is a problem in the vision system which returns a
				// "reversed y" coordinates
				// remove the - in front of direction in case this is fixed
				if (future_velocities[i] == null)
					future_velocities[i] = Vector2D.ZERO();
				future_velocities[i].setLocation(
						speeds[i] * Math.cos(directions[i] * Math.PI / 180),
						speeds[i] * Math.sin(-directions[i] * Math.PI / 180));
				future_positions[i] = new Vector2D(positions[i]);
				future_positions[i].addmul_to(velocities[i], 2 * dt);
			}
		// future ball friction
		double ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= ball_friction_acc * 2 * dt) {
				Vector2D friction = Vector2D.change_length(ball_velocity,
						-ball_friction_acc);
				future_ball_velocity = new Vector2D(ball_velocity);
				future_ball_velocity.addmul_to(friction, 2 * dt);
			} else
				future_ball_velocity = Vector2D.ZERO();
		}
		// future calculate final ball position
		future_ball = new Vector2D(ball);
		future_ball.addmul_to(ball_velocity, 2 * dt);
		// calculate final positions
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				speeds[i] = robot[i].calculateSpeed(speeds[i], dt);
				turning_speeds[i] = robot[i].calculateTurningSpeed(
						turning_speeds[i], dt);
				if (!will_be_in_collision[i])
					directions[i] += turning_speeds[i] * dt;
				// there is a problem in the vision system which returns a
				// "reversed y" coordinates
				// remove the - in front of direction in case this is fixed
				velocities[i].setLocation(
						speeds[i] * Math.cos(directions[i] * Math.PI / 180),
						speeds[i] * Math.sin(-directions[i] * Math.PI / 180));
				if (!will_be_in_collision[i])
					positions[i].addmul_to(velocities[i], dt);
				else
					will_be_in_collision[i] = false;

			}
		// ball friction
		ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= ball_friction_acc * dt) {
				Vector2D friction = Vector2D.change_length(ball_velocity,
						-ball_friction_acc);
				ball_velocity.addmul_to(friction, dt);
			} else
				ball_velocity = Vector2D.ZERO();
		}
		// calculate final ball position
		if (ball_velocity.getLength() > ball_max_speed)
			ball_velocity = Vector2D.ZERO();// Vector2D.change_length(ball_velocity,
											// ball_max_speed);
		ball.addmul_to(ball_velocity, dt);

		// ball collision with robots
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null) {
				// transform ball into "brick" relative coordinates
				Vector2D future_rel_ball = Vector2D.rotateVector(
						Vector2D.subtract(future_ball, future_positions[i]),
						-future_directions[i]);
				Vector2D curr_rel_ball = Vector2D.rotateVector(
						Vector2D.subtract(ball, positions[i]), -directions[i]);
				Vector2D curr_rel_spd = Vector2D.rotateVector(
						Vector2D.subtract(ball_velocity, velocities[i]),
						-directions[i]);
				// check whether the ball is "inside" brick
				if (future_rel_ball.getX() > VBrick.back_left.getX()
						&& future_rel_ball.getX() < VBrick.front_right.getX()
						&& future_rel_ball.getY() > VBrick.front_right.getY()
						&& future_rel_ball.getY() < VBrick.back_left.getY()) {
					// start collision detection to make speed reflection
					// and to prevent ball penetrating the virtual robot

					// collisions with walls penetration depth calculation
					double left_pen = future_rel_ball.getX()
							- VBrick.back_left.getX();
					double right_pen = VBrick.front_right.getX()
							- future_rel_ball.getX();
					double top_pen = future_rel_ball.getY()
							- VBrick.front_right.getY();
					double bottom_pen = VBrick.back_left.getY()
							- future_rel_ball.getY();
					// use the smallest penetration to determine collision side
					int collision_id = 0; // 0 - left, 1 - right, 2 - top, 3 -
											// bottom
					double min = left_pen;
					if (right_pen < min) {
						min = right_pen;
						collision_id = 1;
					}
					if (top_pen < min) {
						min = top_pen;
						collision_id = 2;
					}
					if (bottom_pen < min) {
						min = bottom_pen;
						collision_id = 3;
					}
					// once we have collision side, react
					boolean is_currently_colliding = curr_rel_ball.getX() > VBrick.back_left
							.getX()
							&& curr_rel_ball.getX() < VBrick.front_right.getX()
							&& curr_rel_ball.getY() > VBrick.front_right.getY()
							&& curr_rel_ball.getY() < VBrick.back_left.getY();
					switch (collision_id) {
					case 0:
						// left
						if (is_currently_colliding) {
							double dy = future_rel_ball.getY()
									- curr_rel_ball.getY();
							double dx = future_rel_ball.getX()
									- curr_rel_ball.getX();
							double mx = VBrick.back_left.getX()
									- curr_rel_ball.getX();
							curr_rel_ball.setLocation(
									VBrick.back_left.getX() - 0.5,
									curr_rel_ball.getY() + dy * mx / dx);
						} else
							curr_rel_spd.setX(-curr_rel_spd.getX()
									* robot_bounciness);
						break;
					case 1:
						// right
						if (is_currently_colliding) {
							double dy = curr_rel_ball.getY()
									- future_rel_ball.getY();
							double dx = curr_rel_ball.getX()
									- future_rel_ball.getX();
							double mx = curr_rel_ball.getX()
									- VBrick.front_right.getX();
							curr_rel_ball.setLocation(
									VBrick.front_right.getX() + 0.5,
									curr_rel_ball.getY() - dy * mx / dx);
						} else
							curr_rel_spd.setX(-curr_rel_spd.getX()
									* robot_bounciness);
						break;
					case 2:
						// top
						if (is_currently_colliding) {
							double dy = future_rel_ball.getY()
									- curr_rel_ball.getY();
							double dx = future_rel_ball.getX()
									- curr_rel_ball.getX();
							double my = -VBrick.back_left.getY()
									- curr_rel_ball.getY();
							curr_rel_ball.setLocation(curr_rel_ball.getX() + dx
									* my / dy, -VBrick.back_left.getY() - 0.5);
						} else
							curr_rel_spd.setY(-curr_rel_spd.getY()
									* robot_bounciness);
						break;
					case 3:
						// bottom
						if (is_currently_colliding) {
							double dy = curr_rel_ball.getY()
									- future_rel_ball.getY();
							double dx = curr_rel_ball.getX()
									- future_rel_ball.getX();
							double my = curr_rel_ball.getY()
									+ VBrick.front_right.getY();
							curr_rel_ball.setLocation(curr_rel_ball.getX() - dx
									* my / dy, -VBrick.back_right.getY() + 0.5);
						} else
							curr_rel_spd.setY(-curr_rel_spd.getY()
									* robot_bounciness);
						break;
					}
					// fix position if needed

					// calculate the velocity of the current point of "contact"
					// and add it to ball's velocity
					// this is the magic function that adds the ability that the
					// ball is pushed around by the robot
					curr_rel_spd.addmul_to(
							getPointOfContactVel(curr_rel_ball,
									future_rel_ball, turning_speeds[i], dt),
							robot_bounciness);
				}
				// kicker
				double ball_distance = future_rel_ball.x
						- VBrick.front_left.getX();
				if (robot[i].is_kicking) {
					if (ball_distance < kicker_range && ball_distance > 0
							&& future_rel_ball.y < VBrick.front_left.getY()
							&& future_rel_ball.y > VBrick.front_right.getY()) {
						double power = kicker_max_speed - kicker_min_speed
								- (kicker_max_speed - kicker_min_speed)
								* ball_distance / kicker_range;
						System.out.println("power " + power);
						curr_rel_spd.setX(curr_rel_spd.getX() + power);
					}
					robot[i].is_kicking = false;
				}
				// top flipper
				// coming from below
				if (future_rel_ball.getY() - ball_radius < -VBrick.front_left
						.getY()
						&& curr_rel_ball.getY() + ball_radius > -VBrick.front_left
								.getY()
						&& curr_rel_ball.getX() + ball_radius > VBrick.front_left
								.getX()
						&& (curr_rel_ball.getX() - VBrick.front_left.getX() - ball_radius) < flipper_size) {
					curr_rel_spd.setY(-curr_rel_spd.getY() * robot_bounciness);
					curr_rel_spd.addmul_to(
							getPointOfContactVel(curr_rel_ball,
									future_rel_ball, turning_speeds[i], dt),
							flipper_bounciness);

				}
				// coming from above
				else if (curr_rel_ball.getY() - ball_radius < -VBrick.front_left
						.getY()
						&& future_rel_ball.getY() + ball_radius > -VBrick.front_left
								.getY()
						&& future_rel_ball.getX() + ball_radius > VBrick.front_left
								.getX()
						&& (future_rel_ball.getX() - VBrick.front_left.getX())
								- ball_radius < flipper_size) {
					curr_rel_spd.setY(-curr_rel_spd.getY() * robot_bounciness);
					curr_rel_spd.addmul_to(
							getPointOfContactVel(curr_rel_ball,
									future_rel_ball, turning_speeds[i], dt),
							flipper_bounciness);
				}
				// bottom flipper
				// coming from top
				if (future_rel_ball.getY() + ball_radius > -VBrick.front_right
						.getY()
						&& curr_rel_ball.getY() - ball_radius < -VBrick.front_right
								.getY()
						&& curr_rel_ball.getX() + ball_radius > VBrick.front_right
								.getX()
						&& (curr_rel_ball.getX() - VBrick.front_right.getX())
								- ball_radius < flipper_size) {
					// System.out.println("Ball from top going to front right flipper");
					curr_rel_spd.setY(-curr_rel_spd.getY() * robot_bounciness);
					curr_rel_spd.addmul_to(
							getPointOfContactVel(curr_rel_ball,
									future_rel_ball, turning_speeds[i], dt),
							flipper_bounciness);
				}
				// coming from below
				else if (curr_rel_ball.getY() + ball_radius > -VBrick.front_right
						.getY()
						&& future_rel_ball.getY() - ball_radius < -VBrick.front_right
								.getY()
						&& future_rel_ball.getX() + ball_radius > VBrick.front_right
								.getX()
						&& (future_rel_ball.getX() - VBrick.front_right.getX())
								- ball_radius < flipper_size) {
					// System.out.println("Ball from below going to front right flipper");
					curr_rel_spd.setY(-curr_rel_spd.getY() * robot_bounciness);
					curr_rel_spd.addmul_to(
							getPointOfContactVel(curr_rel_ball,
									future_rel_ball, turning_speeds[i], dt),
							flipper_bounciness);
				}
				// apply velocity change
				ball_velocity = Vector2D.add(
						Vector2D.rotateVector(curr_rel_spd, directions[i]),
						velocities[i]);
				ball = Vector2D.add(
						Vector2D.rotateVector(curr_rel_ball, directions[i]),
						positions[i]);
			}

		// ball collision with walls
		if (future_ball.getX() - ball_radius < 0) {
			// collision with left wall
			if (Math.abs(future_ball.getY() - pitch_height_cm / 2) <= goal_size) {
				if (ball.getX() > -5)
					score_left++;
				ball = new Vector2D(-20, pitch_height_cm / 2);
				ball_velocity = Vector2D.ZERO();
			} else {
				ball_velocity.setX(-ball_velocity.getX());
				ball_velocity = Vector2D.multiply(ball_velocity,
						wall_bounciness);
			}
		} else if (future_ball.getX() + ball_radius > pitch_width_cm) {
			// collision with right wall
			if (Math.abs(future_ball.getY() - pitch_height_cm / 2) <= goal_size) {
				if (ball.getX() < pitch_width_cm + 5)
					score_right++;
				ball = new Vector2D(pitch_width_cm + 20, pitch_height_cm / 2);
				ball_velocity = Vector2D.ZERO();
			} else {
				ball_velocity.setX(-ball_velocity.getX());
				ball_velocity = Vector2D.multiply(ball_velocity,
						wall_bounciness);
			}
		} else if (future_ball.getY() - ball_radius < 0) {
			// collision with bottom wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		} else if (future_ball.getY() + ball_radius > pitch_height_cm) {
			// collision with top wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, wall_bounciness);
		}
		// robot - robot collision
		for (int i = 0; i < robot.length; i++)
			will_be_in_collision[i] = false;
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null && !will_be_in_collision[i]) {
				Vector2D[] ri_ps = VBrick.getRobotCoords(future_positions[i],
						future_directions[i]);
				// check for collisions with walls
				for (int k = 0; k < ri_ps.length; k++) {
					if (ri_ps[k].getX() < 0 || ri_ps[k].getX() > pitch_width_cm
							|| ri_ps[k].getY() < 0
							|| ri_ps[k].getY() > pitch_height_cm) {
						will_be_in_collision[i] = true;

						// when it hits a wall, the robot bounces back with the
						// same speed
						Vector2D backAwayDistance = new Vector2D(0, 0);
						backAwayDistance.addmul_to(velocities[i], -dt);
						positions[i] = Vector2D.add(positions[i],
								backAwayDistance);

						break;
					}
				}
				// check for collisions with other robots
				if (!will_be_in_collision[i])
					for (int j = 0; j < robot.length; j++)
						if (j != i && robot[j] != null
								&& !will_be_in_collision[j])
							for (int k = 0; k < ri_ps.length; k++) {
								// for every point k (front_left, front_right,
								// etc.) from robot i
								// try to see whether is inside robot j
								Vector2D rel_pos = Vector2D.rotateVector(
										Vector2D.subtract(ri_ps[k],
												future_positions[j]),
										-future_directions[j]);
								if (rel_pos.getX() > VBrick.back_left.getX()
										&& rel_pos.getX() < VBrick.front_right
												.getX()
										&& rel_pos.getY() > VBrick.front_right
												.getY()
										&& rel_pos.getY() < VBrick.back_left
												.getY()) {
									// we have collision, freeze both robots

									will_be_in_collision[i] = true;
									will_be_in_collision[j] = true;

									/**
									 * If the robots collide, they will back
									 * away with a distance equal to the
									 * difference in velocities times the time
									 * between frames This is not physically
									 * accurate...
									 * */
									
									Vector2D backAwayDistance1 = new Vector2D(
											0, 0);
									Vector2D backAwayDistance2 = new Vector2D(
											0, 0);
									
									
									Vector2D relative_velocity = Vector2D.add(velocities[i], velocities[j]);

									//scale velocities
									backAwayDistance1.addmul_to(Vector2D.multiply(velocities[j],1/relative_velocity.getLength()),
											dt);
									backAwayDistance2.addmul_to(Vector2D.multiply(velocities[i],1/relative_velocity.getLength()),
											dt);

									Vector2D distance1 = Vector2D.add(
											positions[i], backAwayDistance1);
									Vector2D distance2 = Vector2D.add(
											positions[j], backAwayDistance2);
											

									//if the future positions of the robots are still inside the pitch,
									//set the positions, else the robots remain in the same place
									if (distance1.getX() < (pitch_width_cm - 10)
											&& distance1.getY() < (pitch_height_cm - 10)
											&& distance1.getX() > 10
											&& distance1.getY() > 10
											&& distance2.getX() < (pitch_width_cm - 10)
											&& distance2.getY() < (pitch_height_cm - 10)
											&& distance2.getX() > 10
											&& distance2.getY() > 10
											&& Vector2D.subtract(positions[i], positions[j]).getLength()>20) {
										positions[i] = distance1;
										positions[j] = distance2;
										
									}

								}
							}
			}
		// notify that we have change
		state = new WorldState(Vector2D.divide(ball, pitch_width_cm),
				new Robot(Vector2D.divide(positions[0], pitch_width_cm),
						directions[0]), new Robot(Vector2D.divide(positions[1],
						pitch_width_cm), directions[1]), im);
		image(dt);
		setChanged();
		notifyObservers(state);
	}

	/**
	 * Creates visualization
	 * 
	 * @return
	 */
	private void image(double dt) {
		// create image if not existing
		if (im == null) {
			im = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT
					+ IMAGE_INFO_SEC_HEIGHT, BufferedImage.TYPE_INT_RGB);
			g = im.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		}
		// draw table
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT + IMAGE_INFO_SEC_HEIGHT);
		g.setColor(new Color(10, 80, 0));
		fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		// draw goals
		g.setColor(new Color(180, 180, 180));

		fillRect(0,
				(int) (IMAGE_WIDTH*(pitch_height_cm/2-goal_size/2)/pitch_width_cm),
				(int) (IMAGE_WIDTH*2/pitch_width_cm),
				(int) (IMAGE_WIDTH*goal_size/pitch_width_cm));
		fillRect((int) (IMAGE_WIDTH - IMAGE_WIDTH*2/pitch_width_cm),
				(int) (IMAGE_WIDTH*(pitch_height_cm/2-goal_size/2)/pitch_width_cm),
				(int) (IMAGE_WIDTH*2/pitch_width_cm),
				(int) (IMAGE_WIDTH*goal_size/pitch_width_cm));

		// draw robots
		WorldState state_cm = null;
		if (state != null)
			state_cm = Tools.toCentimeters(state);
		for (int i = 0; i < robot.length; i++) {
			Robot robot;
			Color color = Color.gray;
			// chose robot color
			switch (i) {
			case 0:
				color = Color.blue;
				break;
			case 1:
				color = new Color(220, 220, 0);
				break;
			}
			if (i == mouse_over_robot)
				g.setColor(brighter(color));
			else
				g.setColor(color);
			g.setStroke(new BasicStroke(1.0f));
			robot = new Robot(Vector2D.divide(positions[i], pitch_width_cm),
					directions[i]);
			// draw body of robot

			fillPolygon(new int[] {
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
			}, 5);
			// draw flipper's
			g.setStroke(new BasicStroke(3.0f));
			double dir_x = flipper_size*Math.cos(robot.getAngle()*Math.PI/180d)/pitch_width_cm;
			double dir_y = -flipper_size*Math.sin(robot.getAngle()*Math.PI/180d)/pitch_width_cm;
			drawLine(
					(int)(robot.getFrontLeft().getX()*IMAGE_WIDTH),
					(int)(robot.getFrontLeft().getY()*IMAGE_WIDTH),
					(int)((robot.getFrontLeft().getX()+dir_x)*IMAGE_WIDTH),
					(int)((robot.getFrontLeft().getY()+dir_y)*IMAGE_WIDTH));
			drawLine(
					(int)(robot.getFrontRight().getX()*IMAGE_WIDTH),
					(int)(robot.getFrontRight().getY()*IMAGE_WIDTH),
					(int)((robot.getFrontRight().getX()+dir_x)*IMAGE_WIDTH),
					(int)((robot.getFrontRight().getY()+dir_y)*IMAGE_WIDTH));

			// draw direction pointer
			double shift_x = 0.01 * Math.cos(robot.getAngle() * Math.PI / 180d);
			double shift_y = -0.01
					* Math.sin(robot.getAngle() * Math.PI / 180d);
			g.setColor(Color.white);
			g.setStroke(new BasicStroke(10.0f));

			dir_x = 0.04*Math.cos(robot.getAngle()*Math.PI/180d);
			dir_y = -0.04*Math.sin(robot.getAngle()*Math.PI/180d);
			drawLine(
					(int)((robot.getCoords().getX()-shift_x)*IMAGE_WIDTH),
					(int)((robot.getCoords().getY()-shift_y)*IMAGE_WIDTH),
					(int)((robot.getCoords().getX()+dir_x-shift_x)*IMAGE_WIDTH),
					(int)((robot.getCoords().getY()+dir_y-shift_y)*IMAGE_WIDTH));
			dir_x = 0.03*Math.cos((robot.getAngle()+90)*Math.PI/180d);
			dir_y = -0.03*Math.sin((robot.getAngle()+90)*Math.PI/180d);
			drawLine(
					(int)((robot.getCoords().getX()-dir_x/2-shift_x)*IMAGE_WIDTH),
					(int)((robot.getCoords().getY()-dir_y/2-shift_y)*IMAGE_WIDTH),
					(int)((robot.getCoords().getX()+dir_x/2-shift_x)*IMAGE_WIDTH),
					(int)((robot.getCoords().getY()+dir_y/2-shift_y)*IMAGE_WIDTH));

			// draw nearest points of collision
			if (i < 2 && state_cm != null) {
				color = brighter(color);
				g.setColor(new Color(color.getRed(), color.getGreen(), color
						.getBlue(), 50));
				g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f },
						0.0f));
				boolean am_i_blue = i == 0;
				robot = am_i_blue ? state_cm.getBlueRobot() : state_cm
						.getYellowRobot();
//				drawVector(new Vector2D(robot.getFrontLeft()),
//						Tools.getNearestCollisionPoint(state_cm, am_i_blue,
//								robot.getFrontLeft()));
//				drawVector(new Vector2D(robot.getFrontRight()),
//						Tools.getNearestCollisionPoint(state_cm, am_i_blue,
//								robot.getFrontRight()));
//				drawVector(new Vector2D(robot.getBackLeft()),
//						Tools.getNearestCollisionPoint(state_cm, am_i_blue,
//								robot.getBackLeft()));
//				drawVector(new Vector2D(robot.getBackRight()),
//						Tools.getNearestCollisionPoint(state_cm, am_i_blue,
//								robot.getBackRight()));
				Vector2D local_origin = new Vector2D(Robot.LENGTH_CM/2+2,0);
				drawVector(Tools.getGlobalVector(robot, local_origin),  Tools.raytraceVector(state_cm, robot, local_origin, new Vector2D(1,0)), true);
			}
		}
		// draw ball
		g.setColor(Color.red);
		if (mouse_over_ball)
			g.setColor(brighter(g.getColor()));
		g.setStroke(new BasicStroke(1.0f));
		fillOval(

				(int) ((ball.getX() - ball_radius) * IMAGE_WIDTH / pitch_width_cm),
				(int) ((ball.getY() - ball_radius) * IMAGE_WIDTH / pitch_width_cm),
				(int) (2 * ball_radius * IMAGE_WIDTH / pitch_width_cm),
				(int) (2 * ball_radius * IMAGE_WIDTH / pitch_width_cm));
		// draw Strings
		g.setColor(Color.BLACK);
		g.fillRect(0, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_INFO_SEC_HEIGHT);
		g.setColor(Color.white);
		g.drawString((int) (1 / dt) + " fps", IMAGE_WIDTH - 50, 20);
		g.drawString("Score: " + score_left + " : " + score_right,
				IMAGE_WIDTH / 2, 20);
		g.drawString(
				"blue - ball: "
						+ String.format("%.1f", (Vector2D.subtract(ball,
								positions[0]).getLength()))
						+ " cm; "
						+ String.format("%.1f",
								Vector2D.getAngle(ball, positions[0])) + "°",
				20, IMAGE_HEIGHT + 20);
		g.drawString(
				"blue : " + positions[0] + "; "
						+ String.format("%.1f", directions[0]) + "°", 20,
				IMAGE_HEIGHT + 40);
		g.drawString(
				"yellow - ball: "
						+ String.format("%.1f", (Vector2D.subtract(ball,
								positions[1]).getLength()))
						+ " cm; "
						+ String.format("%.1f",
								Vector2D.getAngle(ball, positions[1])) + "°",
				20, IMAGE_HEIGHT + 60);
		g.drawString(
				"yellow : " + positions[1] + "; "
						+ String.format("%.1f", directions[1]) + "°", 20,
				IMAGE_HEIGHT + 80);
		g.drawString("ball : " + ball, IMAGE_WIDTH - 150, IMAGE_HEIGHT + 20);
	}

	// helpers

	
	private void fillRect(int x, int y, int w, int h) {
		fillPolygon(new int[] {
				x,
				x+w,
				x+w,
				x,
				x
		}, new int[] {
				y,
				y,
				y+h,
				y+h,
				y
		}, 5);
	}
	
	/**
	 * Use instead of g.fillOval
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	private void fillOval(int x, int y, int w, int h) {
		Vector2D l_r = transformScreenVectorToLocalOne(x-w/2, y-h/2);
		Vector2D t_r = transformScreenVectorToLocalOne(x+w/2, y+h/2);
		Vector2D cent = Vector2D.divide(Vector2D.add(l_r, t_r), 2);
		g.fillOval((int) cent.getX(), (int) cent.getY(), w, h);
	}
	
	/**
	 * Use instead of g.fillPolygon
	 * @param xs
	 * @param ys
	 * @param size number of points
	 */
	private void fillPolygon(int[] xs, int[] ys, int size) {
		Vector2D[] points = new Vector2D[size];
		int[] newxs = new int[size], newys = new int[size];
		for (int i = 0; i < size; i++) {
			points[i] = transformScreenVectorToLocalOne(xs[i], ys[i]);
			newxs[i] = (int) points[i].getX();
			newys[i] = (int) points[i].getY();
		}
		g.fillPolygon(newxs, newys, size);
	}
	

	/**
	 * Use instead of g.DrawLine
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	private void drawLine(int x1, int y1, int x2, int y2) {
		if (reference_robot_id != null) {

			Vector2D start = transformScreenVectorToLocalOne(x1, y1);
			Vector2D end = transformScreenVectorToLocalOne(x2, y2);
			g.drawLine((int) start.getX(), (int) start.getY(),
					(int) end.getX(), (int) end.getY());
		} else
			g.drawLine(x1, y1, x2, y2);
	}

	/**
	 * Draw vecrtor
	 * 
	 * @param origin
	 *            in cm
	 * @param vector
	 *            in cm
	 */
	private void drawVector(Vector2D origin, Vector2D vector, boolean draw_point_in_end) {
		double ex = (origin.getX()+vector.getX())*IMAGE_WIDTH/Tools.PITCH_WIDTH_CM, ey = (origin.getY()+vector.getY())*IMAGE_WIDTH/Tools.PITCH_WIDTH_CM;
		drawLine(
				(int)(origin.getX()*IMAGE_WIDTH/Tools.PITCH_WIDTH_CM),
				(int)(origin.getY()*IMAGE_WIDTH/Tools.PITCH_WIDTH_CM),
				(int)(ex),
				(int)(ey));
		if (draw_point_in_end) {
			fillOval((int) ex-3, (int) ey-3, 6, 6);
		}

	}

	private Vector2D transformScreenVectorToLocalOne(int x, int y) {

		if (reference_robot_id == null)
			return new Vector2D(x, y);
		Robot rob = new Robot(Vector2D.multiply(positions[reference_robot_id], IMAGE_WIDTH/pitch_width_cm), directions[reference_robot_id]);
		Vector2D centre_pitch = new Vector2D(0.5*IMAGE_WIDTH, 0.5*pitch_height_cm*IMAGE_WIDTH/pitch_width_cm);
		return Vector2D.add(centre_pitch, Tools.getLocalVector(rob, new Vector2D(x, y)));

	}

	/**
	 * Returns the velocity that should be added to current position due to
	 * rotation of a rigid body. All coordinates supplied must be in the
	 * coordinate system of the rigit body, i.e. (0,0) is the centre of mass of
	 * the rigid body.
	 * 
	 * @param curr_position
	 *            current position of the point that is about to collide
	 * @param new_position
	 *            the position of the point that is going to be next. We have a
	 *            collision at this position
	 * @param rot_speed
	 *            the speed in deg/sec at which the body is rotating
	 * @param time
	 *            passed until now (usually just dt, not 2*dt)
	 * @return the velocity in the same reference frame that should be added to
	 *         the point at the current position
	 */
	private Vector2D getPointOfContactVel(Vector2D curr_position,
			Vector2D new_position, double rot_speed, double dt) {
		return Vector2D.divide(
				Vector2D.subtract(curr_position,
						Vector2D.rotateVector(new_position, -rot_speed * dt)),
				dt);
	}

	/**
	 * Gets a brighter color, suitable for highlighting
	 */
	private Color brighter(Color a) {
		double dr = a.getRed() + 0.6 * (255 - a.getRed());
		double dg = a.getGreen() + 0.6 * (255 - a.getGreen());
		double db = a.getBlue() + 0.6 * (255 - a.getBlue());
		int r = dr < 255 ? (int) dr : 255;
		int g = dg < 255 ? (int) dg : 255;
		int b = db < 255 ? (int) db : 255;
		return new Color(r, g, b);
	}

}
