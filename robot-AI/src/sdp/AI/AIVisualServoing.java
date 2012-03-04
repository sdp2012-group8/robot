package sdp.AI;

import java.io.IOException;

import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.Vector2D;

public class AIVisualServoing extends AI {

	private final static int COLL_SECS_COUNT = 110;
	private final static double SEC_ANGLE = 360d/COLL_SECS_COUNT;
	private final static int COLL_ANGLE = 25;
	private final static int CORNER_COLL_THRESHOLD = 3;
	private final static int NEAR_TARGET = 2;
	private final static int POINT_ACCURACY = 10;
	
	private final static int MAX_TURN_ANG = 127;

	/**
	 * True if the robot is chasing the target.
	 * False if the robot is chasing the real ball.
	 */
	private boolean chasing_target = true;

	@Override
	protected Command chaseBall() throws IOException {
		Command comm = null;
		Vector2D target = null;

		try {
			target = new Vector2D(Utilities.getOptimalPointBehindBall(ai_world_state, ai_world_state.getMyGoalLeft(), ai_world_state.getMyTeamBlue()));
		} catch (NullPointerException e) {
			// Robot can't see a goal
			// TODO: decide on what to do when the robot can't see the goal.
			System.out.println("Can't see a goal");
			target = new Vector2D(ai_world_state.getBallCoords());
		}

		//		double dist = 2*Robot.LENGTH_CM;
		//		Vector2D target = new Vector2D(ai_world_state.getBallCoords().getX() + (ai_world_state.getMyGoalLeft() ? - dist : dist), ai_world_state.getBallCoords().getY());

		//		if (true) {
		//			comm = goTowardsPoint(target, true, true);
		//			comm.speed = 0;
		//			return comm;
		//		}


		double targ_dist = distanceTo(target);

		if (chasing_target) {
			comm = goTowardsPoint(target, true, false);
			// if oscillating near zero
			if (targ_dist < POINT_ACCURACY)
				chasing_target = false;

			double dir_angle = Vector2D.getDirection(Vector2D.rotateVector(Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), target), -ai_world_state.getRobot().getAngle()));
			if (Math.abs(dir_angle) > 20) {
				slowDownSpeed(targ_dist, 20, comm, 30); // limits speed to 30
			}
//			if (targ_dist < 20)
//				comm.turning_speed = 0;
		}

		if (!chasing_target) {
			Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
			double ball_dist = ai_world_state.getDistanceToBall();
			if (ball_dist > 20 && ball_dist < 50) {
				double dir = Vector2D.getDirection(Vector2D.rotateVector(Vector2D.subtract(ball, target), -ai_world_state.getRobot().getAngle()));
				comm = new Command(Math.abs(dir) < 10 ? MAX_SPEED_CM_S : 0, dir, false);
			} else
				comm = goTowardsPoint(ball, false, true);
			slowDownSpeed(ai_world_state.getDistanceToBall(), 10, comm, 2);
			//System.out.print("B");
		}

		normalizeRatio(comm);
		
		// debugging restrictions
		//comm.turning_speed *= 10;
		//comm.speed *= 1;
		
		return comm;

	}

	@Override
	protected Command gotBall() throws IOException {
		chasing_target = true;
		return new Command(0,0,true);
	}

	@Override
	protected Command defendGoal() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Command penaltiesDefend() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Command penaltiesAttack() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Makes robot proceed towards a point avoiding obstacles on its way
	 * @param point
	 */
	private Command goTowardsPoint(Vector2D point, boolean include_ball_as_obstacle, boolean need_to_face_point) {
		Command command = new Command(0, 0, false);
		// get relative ball coordinates
		final Vector2D point_rel = Utilities.getLocalVector(ai_world_state.getRobot(), point);

		// get direction and distance to point
		final double point_dir = Vector2D.getDirection(point_rel);
		final double point_dist = point_rel.getLength();

		// if you go directly towards the point, at which closest point are we going to be in a collision
		final double point_left_coll_dist = Utilities.reachabilityLeft2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);
		final double point_right_coll_dist = Utilities.reachabilityRight2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);
		final double point_coll_dist = Math.min(point_left_coll_dist, point_right_coll_dist);
		final double point_vis_dist = Utilities.visibility2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);

		// the angle that we need to turn in order to avoid hitting our left or right corner at the obstacle
		final double turn_ang_more = Math.toDegrees(Math.atan2(Robot.LENGTH_CM, point_coll_dist));

		// get the sectors
		final double[] sectors = Utilities.getSectors(ai_world_state, ai_world_state.getMyTeamBlue(), 5, COLL_SECS_COUNT, false, include_ball_as_obstacle);

		// sectors[0] starts at -90
		double temp = 999;
		double turn_ang = 999;
		int id = -1;
		for (int i = 0; i < sectors.length; i++) {
			if (sectors[i] > point_dist+Robot.LENGTH_CM/2) {	
				double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
				double diff = Utilities.normaliseAngle(ang-point_dir);
				if (Math.abs(diff) < Math.abs(temp)) {
					temp = diff;
					turn_ang = ang;
					id = i;
				}
			}
		}

		double temp2 = 999;
		double turn_ang2 = 999;
		for (int i = 0; i < sectors.length; i++) {
			if (sectors[i] > point_dist+Robot.LENGTH_CM/2) {	
				double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
				double diff = Utilities.normaliseAngle(ang-point_dir);
				if (Math.abs(diff) < Math.abs(temp) && i != id) {
					temp2 = diff;
					turn_ang2 = ang;
				}
			}
		}

		if (Math.abs(Utilities.normaliseAngle(turn_ang2-turn_ang)) > SEC_ANGLE*2 && Math.abs(turn_ang2) < Math.abs(turn_ang)) {
			command.turning_speed = turn_ang2;
		} else
			command.turning_speed = turn_ang;

		//System.out.println(String.format("%.2f l="+point_left_coll_dist+" r="+point_right_coll_dist+" d="+point_dist,command.turning_speed));

		if (point_left_coll_dist < point_dist || point_right_coll_dist < point_dist)
			command.turning_speed += point_left_coll_dist > point_right_coll_dist ? turn_ang_more : -turn_ang_more;


			// if we have no way of reaching the point go into the most free direction
			if (command.turning_speed == 999) {
				temp = 0;
				for (int i = 0; i < sectors.length; i++) {
					if (sectors[i] > temp) {
						temp = sectors[i];
						double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
						command.turning_speed = Utilities.normaliseAngle(- ang - (point_left_coll_dist < point_right_coll_dist ? turn_ang_more : -turn_ang_more));
					}
				}
			} 


			if (need_to_face_point) { 
				// set forward speed
				if (command.turning_speed > 90 || command.turning_speed < -90){ 
					// ball is behind
					command.speed = -MAX_SPEED_CM_S;
				} else { 
					//ball is in front
					command.speed = MAX_SPEED_CM_S;
				}
			} else {
				// go fastest to a point regardless of direction

				if (command.turning_speed > 90 || command.turning_speed < -90) {
					// ball is behind
					command.speed = -MAX_SPEED_CM_S;

					// go backwards to get to ball as soon as possible
					command.turning_speed = Utilities.normaliseAngle(command.turning_speed-180);
				} else {
					// ball is in front
					command.speed = MAX_SPEED_CM_S;
				}
			}

			// if we get within too close (within coll_start) of an obstacle
			backAwayIfTooClose(command, sectors);

			// check if either of the corners are in collision
			nearCollisionCheck(command);

			return command;
	}

	/**
	 * If too close to an obstacle from sectors, back away
	 * @param command
	 * @param sectors
	 */
	private void backAwayIfTooClose(Command command, double[] sectors) {
		double for_dist = getMin(sectors, anid(-10), anid(10)); // get collision distance at the front
		double back_dist = getMin(sectors, anid(170), anid(190)); // get collision distance at the back

		if (for_dist < COLL_ANGLE) {
			if (command.speed >= 0) {
				// go backwards
				double speed_coeff = -1+for_dist/COLL_ANGLE;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				command.speed *= speed_coeff;
			}
		} else if (back_dist < COLL_ANGLE) {
			if (command.speed <= 0) {
				// same as above
				double speed_coeff = -1+back_dist/COLL_ANGLE;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				command.speed *= speed_coeff;
			}
		}
	}

	/**
	 * Checks whether there is a collision with either corner and tries to react to it
	 * @param command
	 */
	private void nearCollisionCheck(Command command) {
		final Vector2D
		front_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		front_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())));
		final boolean
		front_left_coll = front_left.getLength() <= CORNER_COLL_THRESHOLD,
		front_right_coll = front_right.getLength() <= CORNER_COLL_THRESHOLD,
		back_left_coll = back_left.getLength() <= CORNER_COLL_THRESHOLD,
		back_right_coll = back_right.getLength() <= CORNER_COLL_THRESHOLD,
		any_collision = front_left_coll || front_right_coll || back_left_coll || back_right_coll;

		if (any_collision) {
			//System.out.println("Collision "+(front_left_coll || front_right_coll ? "FRONT" : "BACK")+" "+(front_left_coll || back_left_coll ? "LEFT" : "RIGHT"));
			command.speed = front_left_coll || front_right_coll ? -MAX_SPEED_CM_S : MAX_SPEED_CM_S;
			command.turning_speed += front_left_coll || back_left_coll ? -10 : 10;
		}
		command.turning_speed = Utilities.normaliseAngle(command.turning_speed);
	}

	/**
	 * Gives the ID of a given angle. Angle is wrt to (1, 0) local vector on robot (i.e. 0 is forward)
	 * @param angle should not exceed
	 * @return
	 */
	private int anid(double angle) {
		int id =  (int) (COLL_SECS_COUNT*(angle+90)/360);
		return id < 0 ? COLL_SECS_COUNT + id : (id >= COLL_SECS_COUNT ? id - COLL_SECS_COUNT : id);
	}

	/**
	 * Find the smallest element in the array from start to end inclusive
	 * 
	 * @param array
	 * @param start
	 * @param end
	 * @return
	 */
	private static final double getMin(double[] array, int start, int end) {
		double min = 9999;
		for (int i = start; i <= end; i++)
			if (array[i] < min)
				min = array[i];
		return min;
	}

	/**
	 * Distance from front of robot to a given point on table
	 * @param global
	 * @return
	 */
	private double frontDistanceTo(Vector2D global) {
		return Utilities.getDistanceBetweenPoint(Utilities.getGlobalVector(ai_world_state.getRobot(), new Vector2D(Robot.LENGTH_CM/2, 0)), global);
	}

	/**
	 * Distance from centre of robot to a given point on table
	 * @param global
	 * @return
	 */
	private double distanceTo(Vector2D global) {
		return Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), global).getLength();
	}

	private double directionTo(Vector2D global) {
		final Vector2D point_rel = Utilities.getLocalVector(ai_world_state.getRobot(), global);
		return Vector2D.getDirection(point_rel);
	}

	private void slowDownSpeed(double distance, double threshold, Command current_speed, double slow_speed) {
		if (distance >= threshold)
			return;
		if (current_speed.speed < 0)
			slow_speed = -slow_speed;
		if (Math.abs(current_speed.speed) < Math.abs(slow_speed)) {
			current_speed.speed = slow_speed;
			return;
		}
		double coeff = distance / threshold;
		current_speed.speed = slow_speed+coeff*(current_speed.speed-slow_speed);
	}
	
	public void normalizeRatio(Command comm) {
		if (Math.abs(comm.turning_speed) > MAX_TURNING_SPEED) {
			comm.speed = MAX_SPEED_CM_S;
			return;
		}
		double rat = 1 - Math.abs(comm.turning_speed)/MAX_SPEED_CM_S;
		comm.speed *= rat;
	}

}
