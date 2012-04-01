package sdp.simulator;

import java.io.IOException;

import sdp.AI.Command;
import sdp.common.Communicator.opcode;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * This is a simulator. It simulates a table
 * 
 * @author Martin Marinov
 * 
 */
@Deprecated
public class SimulatorOld extends Simulator {

	private static final Vector2D PITCH_MIDDLE = new Vector2D(0.5,
			WorldState.PITCH_HEIGHT_CM / (2 * WorldState.PITCH_WIDTH_CM));
	private static final double BALL_MAX_SPEED = 350; // cm/s
	private static final double BALL_FRICTION_ACC = 10; // in cm/s/s
	

	private static final double WALL_BOUNCINESS = 0.4; // 0 - inelastic, 1 -
	// elastic
	private static double ROBOT_BOUNCINESS; // 0 - 1

	// 0, yellow has
	// id 1
	protected Vector2D[] positions = new Vector2D[MAX_NUM_ROBOTS];
	protected Vector2D[] velocities = new Vector2D[MAX_NUM_ROBOTS];
	protected double[] directions = new double[MAX_NUM_ROBOTS];
	protected boolean[] will_be_in_collision = new boolean[MAX_NUM_ROBOTS];

	//boolean flag arrays for collisions
	private boolean[] collision_with_walls = new boolean[MAX_NUM_ROBOTS];
	private boolean[] collision_with_robot = new boolean[MAX_NUM_ROBOTS];

	// for use for collision prediction
	private Vector2D[] future_positions = new Vector2D[MAX_NUM_ROBOTS],
			future_velocities = new Vector2D[MAX_NUM_ROBOTS];
	private double[] future_directions = new double[MAX_NUM_ROBOTS],
			future_speeds = new double[MAX_NUM_ROBOTS],
			future_turning_speeds = new double[MAX_NUM_ROBOTS];
	// define ball
	protected Vector2D ball;
	protected Vector2D ball_velocity;
	private Vector2D // for use for collision prediction
	future_ball = Vector2D.multiply(new Vector2D(PITCH_MIDDLE),
			WorldState.PITCH_WIDTH_CM);
	private Vector2D future_ball_velocity = Vector2D.ZERO();

	private WorldState old_st = null;

	public SimulatorOld(boolean realtime_simulation) {
		this(realtime_simulation, 0.3);
	}
	
	/**
	 * Initializes a simulator
	 * @param realtime_simulation true if you plan to use the simulator as a world state provider and simulate realtime. False otherwise.
	 */
	public SimulatorOld(boolean realtime_simulation, double robot_bounciness) {
		ROBOT_BOUNCINESS = robot_bounciness;
		
		registerBlue(new VBrick(), 40, WorldState.PITCH_HEIGHT_CM / 2);
		registerYellow(new VBrick(), WorldState.PITCH_WIDTH_CM - 40, WorldState.PITCH_HEIGHT_CM / 2);
		ball =Vector2D.multiply(new Vector2D(PITCH_MIDDLE), WorldState.PITCH_WIDTH_CM);
		ball_velocity = Vector2D.ZERO();
		if (realtime_simulation)
			startRealTimeThread();
	}

	/* (non-Javadoc)
	 * @see sdp.simulator.Simulator#setWorldState(sdp.common.world.WorldState, double, boolean, sdp.AI.Command, java.lang.Boolean)
	 */
	@Override
	public void setWorldState(WorldState ws, double dt, boolean is_ws_in_cm, Command command, Boolean am_i_blue) {
		if (!is_ws_in_cm)
			ws = WorldState.toCentimeters(ws);
		boolean first_run = old_st == null || dt == 0;
		for (int id = 0; id < 2; id++) {
			boolean is_it_me = command != null && am_i_blue != null && ((am_i_blue && id == 0) || (!am_i_blue && id == 1));
			Robot rob = id == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
			Robot old_rob = null;
			if (old_st != null)
				old_rob = id == 0 ? old_st.getBlueRobot() : old_st.getYellowRobot();
				positions[id] = new Vector2D(rob.getCoords());
				directions[id] = rob.getAngle();
				will_be_in_collision[id] = false;
				if (is_it_me) {
					velocities[id] = first_run ? Vector2D.ZERO() :
						Vector2D.divide(
								Vector2D.subtract(new Vector2D(rob.getCoords()), new Vector2D(old_rob.getCoords())),dt);
					Vector2D proj = Vector2D.rotateVector(velocities[id], -rob.getAngle());
					speeds[id] = proj.x;
					turning_speeds[id] = first_run ? 0 : (rob.getAngle() - old_rob.getAngle())/dt;
					try {
						robot[id].sendMessage(opcode.operate, command.getShortDrivingSpeed(), command.getShortTurningSpeed());
					} catch (IOException e) {}
				} else {
					try {
						robot[id].sendMessage(opcode.operate, (short) 0, (short) 0);
					} catch (IOException e) {}
				}
		}
		if (ws.getBallCoords().x != -1 && ws.getBallCoords().y != -1) {
			ball = new Vector2D(ws.getBallCoords());
			ball_velocity = first_run ? Vector2D.ZERO() : 
				Vector2D.divide(
						Vector2D.subtract(new Vector2D(ws.getBallCoords()), new Vector2D(old_st.getBallCoords())), dt);
		}
		
		old_st = ws;
		im = ws.getWorldImage();
	}


	/* (non-Javadoc)
	 * @see sdp.simulator.Simulator#putBallAt(double, double)
	 */
	@Override
	public void putBallAt(double x, double y) {
		ball = new Vector2D(x * WorldState.PITCH_WIDTH_CM, y * WorldState.PITCH_WIDTH_CM);
		ball_velocity = Vector2D.ZERO();
	}

	/* (non-Javadoc)
	 * @see sdp.simulator.Simulator#putBallAt()
	 */
	@Override
	public void putBallAt() {
		putBallAt(0.5, WorldState.PITCH_HEIGHT_CM / (2 * WorldState.PITCH_WIDTH_CM));
	}

	/* (non-Javadoc)
	 * @see sdp.simulator.Simulator#putAt(double, double, int, double)
	 */
	@Override
	public void putAt(double x, double y, int id, double direction) {
		if (id < 0 || id >= MAX_NUM_ROBOTS)
			return;
		positions[id] = new Vector2D(x * WorldState.PITCH_WIDTH_CM, y * WorldState.PITCH_WIDTH_CM);
		velocities[id] = Vector2D.ZERO();
		directions[id] = direction;
		will_be_in_collision[id] = true;
		speeds[id] = 0;
		turning_speeds[id] = 0;
	}

	/**
	* Put robot at given location
	*
	* @param x
	* from 0 to 1
	* @param y
	* same as sx
	* @param id
	* robot_id
	* @param direction
	* direction in degrees
	*/
	@Override
	public void putAt(double x, double y, int id) {
		if (id < 0 || id >= MAX_NUM_ROBOTS)
			return;
		positions[id] = new Vector2D(x * WorldState.PITCH_WIDTH_CM, y * WorldState.PITCH_WIDTH_CM);
		velocities[id] = Vector2D.ZERO();
		will_be_in_collision[id] = true;
		speeds[id] = 0;
		turning_speeds[id] = 0;
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
				future_positions[i].addMul(velocities[i], 2 * dt);
			}
		// future ball friction
		double ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= BALL_FRICTION_ACC * 2 * dt) {
				Vector2D friction = Vector2D.changeLength(ball_velocity,
						-BALL_FRICTION_ACC);
				future_ball_velocity = new Vector2D(ball_velocity);
				future_ball_velocity.addMul(friction, 2 * dt);
			} else
				future_ball_velocity = Vector2D.ZERO();
		}
		// future calculate final ball position
		future_ball = new Vector2D(ball);
		future_ball.addMul(ball_velocity, 2 * dt);
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
					positions[i].addMul(velocities[i], dt);
				else
					will_be_in_collision[i] = false;

			}
		// ball friction
		ball_speed = ball_velocity.getLength();
		if (ball_speed != 0) {
			if (ball_speed >= BALL_FRICTION_ACC * dt) {
				Vector2D friction = Vector2D.changeLength(ball_velocity,
						-BALL_FRICTION_ACC);
				ball_velocity.addMul(friction, dt);
			} else
				ball_velocity = Vector2D.ZERO();
		}
		// calculate final ball position
		if (ball_velocity.getLength() > BALL_MAX_SPEED)
			ball_velocity = Vector2D.ZERO();// Vector2D.change_length(ball_velocity,
		// ball_max_speed);
		ball.addMul(ball_velocity, dt);

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
											* ROBOT_BOUNCINESS);
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
											* ROBOT_BOUNCINESS);
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
											* ROBOT_BOUNCINESS);
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
											* ROBOT_BOUNCINESS);
								break;
							}
							// fix position if needed

							// calculate the velocity of the current point of "contact"
							// and add it to ball's velocity
							// this is the magic function that adds the ability that the
							// ball is pushed around by the robot
							curr_rel_spd.addMul(
									getPointOfContactVel(curr_rel_ball,
											future_rel_ball, turning_speeds[i], dt),
											ROBOT_BOUNCINESS);
				}
				// kicker
				double ball_distance = future_rel_ball.x
						- VBrick.front_left.getX();
				if (robot[i].is_kicking) {
					if (ball_distance < KICKER_RANGE && ball_distance > 0
							&& future_rel_ball.y < VBrick.front_left.getY()
							&& future_rel_ball.y > VBrick.front_right.getY()) {
						double power = KICKER_MAX_SPEED - KICKER_MIN_SPEED
								- (KICKER_MAX_SPEED - KICKER_MIN_SPEED)
								* ball_distance / KICKER_RANGE;
						System.out.println("power " + power);
						curr_rel_spd.setX(curr_rel_spd.getX() + power);
					}
					robot[i].is_kicking = false;
				}
				ball_velocity = Vector2D.add(
						Vector2D.rotateVector(curr_rel_spd, directions[i]),
						velocities[i]);
				ball = Vector2D.add(
						Vector2D.rotateVector(curr_rel_ball, directions[i]),
						positions[i]);
			}

		// ball collision with walls
		if (future_ball.getX() - BALL_RADIUS < 0) {
			// collision with left wall

			if (Math.abs(future_ball.getY() - WorldState.PITCH_HEIGHT_CM / 2) <= GOAL_SIZE/2){
				//goal_size) {
				if (ball.getX() > -5)
					SCORE_LEFT++;
				ball = new Vector2D(-20, WorldState.PITCH_HEIGHT_CM / 2);
				ball_velocity = Vector2D.ZERO();
			} else {
				ball_velocity.setX(-ball_velocity.getX());
				ball_velocity = Vector2D.multiply(ball_velocity,
						WALL_BOUNCINESS);
			}
		} else if (future_ball.getX() + BALL_RADIUS > WorldState.PITCH_WIDTH_CM) {
			// collision with right wall
			if (Math.abs(future_ball.getY() - WorldState.PITCH_HEIGHT_CM / 2) <= GOAL_SIZE/2){
				//goal_size) {
				if (ball.getX() < WorldState.PITCH_WIDTH_CM + 5)
					SCORE_RIGHT++;
				ball = new Vector2D(WorldState.PITCH_WIDTH_CM + 20, WorldState.PITCH_HEIGHT_CM / 2);
				ball_velocity = Vector2D.ZERO();
			} else {
				ball_velocity.setX(-ball_velocity.getX());
				ball_velocity = Vector2D.multiply(ball_velocity,
						WALL_BOUNCINESS);
			}
		} else if (future_ball.getY() - BALL_RADIUS < 0) {
			// collision with bottom wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, WALL_BOUNCINESS);
		} else if (future_ball.getY() + BALL_RADIUS > WorldState.PITCH_HEIGHT_CM) {
			// collision with top wall
			ball_velocity.setY(-ball_velocity.getY());
			ball_velocity = Vector2D.multiply(ball_velocity, WALL_BOUNCINESS);
		}
		// robot - robot collision
		for (int i = 0; i < robot.length; i++){
			will_be_in_collision[i] = false;
			collision_with_walls[i] = false;
			collision_with_robot[i] = false;
		}
		for (int i = 0; i < robot.length; i++)
			if (robot[i] != null && !will_be_in_collision[i]) {
				Vector2D[] ri_ps = VBrick.getRobotCoords(future_positions[i],
						future_directions[i]);
				// check for collisions with walls
				for (int k = 0; k < ri_ps.length; k++) {
					if (ri_ps[k].getX() < 0 || ri_ps[k].getX() > WorldState.PITCH_WIDTH_CM
							|| ri_ps[k].getY() < 0
							|| ri_ps[k].getY() > WorldState.PITCH_HEIGHT_CM) {
						will_be_in_collision[i] = true;

						collision_with_walls[i] = true;					

						//if the robot isn't in a collision with another robot, set it back
						//with the same distance with which it would go outside the wall
						if (collision_with_robot[i] == false){								
							Vector2D distance = Vector2D.subtract(positions[i], future_positions[i]);
							positions[i] = Vector2D.add(distance,positions[i]);
							collision_with_walls[i] = false;
						}

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

									collision_with_robot[i] = true;
									collision_with_robot[j] = true;

									/**
									 * If the robots collide, they will back
									 * away with a distance equal to the
									 * relative velocities times the time
									 * between frames. This is not physically
									 * accurate...
									 * */

									Vector2D backAwayDistance1 = new Vector2D(
											0, 0);
									Vector2D backAwayDistance2 = new Vector2D(
											0, 0);

									//compute relative velocity and scale the rebound velocities wrt to the relative one
									Vector2D relative_velocity = Vector2D.add(velocities[i], velocities[j]);

									backAwayDistance1.addMul(Vector2D.multiply(velocities[j],velocities[j].getLength()/relative_velocity.getLength()),
											dt);
									backAwayDistance2.addMul(Vector2D.multiply(velocities[i],velocities[i].getLength()/relative_velocity.getLength()),
											dt);

									Vector2D distance1 = Vector2D.add(
											positions[i], backAwayDistance1);
									Vector2D distance2 = Vector2D.add(
											positions[j], backAwayDistance2);


									//if the future positions of the robots are still inside the pitch,
									//set the positions, else the robots remain in the same place
									if (distance1.getX() < (WorldState.PITCH_WIDTH_CM - 12)
											&& distance1.getY() < (WorldState.PITCH_HEIGHT_CM - 12)
											&& distance1.getX() > 12
											&& distance1.getY() > 12
											&& distance2.getX() < (WorldState.PITCH_WIDTH_CM - 12)
											&& distance2.getY() < (WorldState.PITCH_HEIGHT_CM - 12)
											&& distance2.getX() > 12
											&& distance2.getY() > 12
											&& Vector2D.subtract(distance1, distance2).getLength()>20) {

										if (collision_with_walls[i] == false)
											positions[i] = distance1;
										if (collision_with_walls[j] == false)
											positions[j] = distance2;

										collision_with_robot[i] = false;
										collision_with_robot[j] = false;


									}

								}
							}


			}
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
	 * @param timeprivate
	 *            passed until now (usually just dt, not 2*dt)
	 * @return the velocity in the same reference frame that should be added to
	 *         the point at the current position
	 */
	private  Vector2D getPointOfContactVel(Vector2D curr_position,
			Vector2D new_position, double rot_speed, double dt) {
		return Vector2D.divide(
				Vector2D.subtract(curr_position,
						Vector2D.rotateVector(new_position, -rot_speed * dt)),
						dt);
	}

	@Override
	protected Vector2D getBall() {
		return ball;
	}

	@Override
	protected Vector2D[] getRobotPositions() {
		return positions;
	}

	@Override
	protected double[] getRbotDirections() {
		return directions;
	}
}
