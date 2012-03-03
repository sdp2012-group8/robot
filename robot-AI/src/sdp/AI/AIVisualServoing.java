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
	private final static int NEAR_TARGET = 5;
	private boolean chase_ball_chase_target = true;

	@Override
	protected Commands chaseBall() throws IOException {
		Commands comm = null;
		Vector2D target = new Vector2D(ai_world_state.getOptimalPointBehindBall());
		// double dist = 2*Robot.LENGTH_CM;
		//Vector2D target = new Vector2D(ai_world_state.getBallCoords().getX() + (ai_world_state.getMyGoalLeft() ? - dist : dist), ai_world_state.getBallCoords().getY());
		double targ_dist = distanceTo(target);
		if (!chase_ball_chase_target) {
			if (ai_world_state.getDistanceToBall() > 4*Robot.LENGTH_CM) {
				System.out.println("Ball too far away, switch back to go to target");
				chase_ball_chase_target = true;
			}
		}
		if (chase_ball_chase_target && targ_dist < NEAR_TARGET)
			chase_ball_chase_target = false;

		if (chase_ball_chase_target) {
			double enemy_goal_x = ai_world_state.getEnemyGoal().getCentre().getX(),
					my_x = ai_world_state.getRobot().getCoords().getX(),
					ball_x = ai_world_state.getBallCoords().getX();
			boolean between_boal_enemy_goal = Math.abs(enemy_goal_x-ball_x) > Math.abs(enemy_goal_x-my_x);
			comm = goTowardsPoint(target, true, !between_boal_enemy_goal);
			if (targ_dist < 10) {
				comm.turning_speed = 0;
				comm.acceleration = 127;
			}
		}
		else {
			Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
			
			comm = goTowardsPoint(ball, false, true);
			if (Math.abs(comm.turning_speed)>20)
				slowDownSpeed(targ_dist, 10, comm, 2);
			slowDownSpeed(ai_world_state.getDistanceToBall(), 10, comm, 2);
			System.out.println("GO TO BALL");
		}
		
		// debugging restrictions
		comm.turning_speed *= 0.5;
		comm.speed *= 0.5;
		
		return comm;

	}

	@Override
	protected Commands gotBall() throws IOException {
		chase_ball_chase_target = true;
		return new Commands(0,0,true);
	}

	@Override
	protected Commands defendGoal() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Commands penaltiesDefend() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Commands penaltiesAttack() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Makes robot proceed towards a point avoiding obstacles on its way
	 * @param point
	 */
	private Commands goTowardsPoint(Vector2D point, boolean include_ball_as_obstacle, boolean facing_point) {
		Commands command = new Commands(0, 0, false);
		// get relative ball coordinates
		final Vector2D point_rel = Tools.getLocalVector(ai_world_state.getRobot(), point);

		// get direction and distance to point
		final double point_dir = Vector2D.getDirection(point_rel);
		final double point_dist = point_rel.getLength();

		// if you go directly towards the point, at which closest point are we going to be in a collision
		final double point_left_coll_dist = Tools.reachabilityLeft2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);
		final double point_right_coll_dist = Tools.reachabilityRight2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);
		final double point_coll_dist = Math.min(point_left_coll_dist, point_right_coll_dist);
		final double point_vis_dist = Tools.visibility2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle);

		// the angle that we need to turn in order to avoid hitting our left or right corner at the obstacle
		final double turn_ang_more = Math.toDegrees(Math.atan2(Robot.LENGTH_CM, point_coll_dist));

		// get the sectors
		final double[] sectors = Tools.getSectors(ai_world_state, ai_world_state.getMyTeamBlue(), 5, COLL_SECS_COUNT, false, include_ball_as_obstacle);

		// sectors[0] starts at at -90
		double temp = 999;
		command.turning_speed = 999;

		// path planning if obstacle is away
		
		if (!Tools.reachability(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle))
		{

			if (point_vis_dist < point_dist) {
				// if point is not visible
				// find the sector that is closest to the point but has a collision distance greater than the point_coll_dist
				for (int i = 0; i < sectors.length; i++) {
					
					if (sectors[i] > point_dist+Robot.LENGTH_CM/2) {	
						double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
						double diff = Utilities.normaliseAngle(ang-point_dir);
						if (Math.abs(diff) < Math.abs(temp)) {
							temp = diff;
							command.turning_speed = ang;
						}
					}
				}
				double temp2 = 999;
				double ang2 = 999;
				for (int i = 0; i < sectors.length; i++) {
					if (sectors[i] > point_dist+Robot.LENGTH_CM/2) {	
						double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
						double diff = Utilities.normaliseAngle(ang-point_dir);
						if (Math.abs(diff) < Math.abs(temp)) {
							temp2 = diff;
							ang2 = Utilities.normaliseAngle(ang);
						}
					}
				}
				System.out.println("Orig min "+command.turning_speed+" sec min "+ang2);
				if (ang2*command.turning_speed < 0) {
					if (Math.abs(temp2) < Math.abs(temp))
						command.turning_speed = temp2;
				}
				
				command.turning_speed += point_dir < 0 ? turn_ang_more : -turn_ang_more;
				
			} else {
				// if point is visible
				command.turning_speed = Utilities.normaliseAngle(point_dir + (point_left_coll_dist < point_right_coll_dist ? turn_ang_more : -turn_ang_more));
				System.out.println("Visible angle "+command.turning_speed);
			}
		} else {
			// if the point is visible and reachable go towards it
			command.turning_speed = Utilities.normaliseAngle(point_dir);
			System.out.println("Reachable angle "+command.turning_speed);
		}

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

		// set forward speed
		command.speed = MAX_SPEED_CM_S;
		
		if (command.turning_speed > 90 || command.turning_speed < -90)
			command.speed = -MAX_SPEED_CM_S;
		
		if (!facing_point) { // go backwards towards the point
			command.turning_speed = Utilities.normaliseAngle(-180+command.turning_speed);
			command.speed = -MAX_SPEED_CM_S;
			if (command.turning_speed > 90 || command.turning_speed < -90)
				command.speed = MAX_SPEED_CM_S;
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
	private void backAwayIfTooClose(Commands command, double[] sectors) {
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
	private void nearCollisionCheck(Commands command) {
		final Vector2D
		front_left = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		front_right = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_left = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_right = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())));
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
		return Tools.getDistanceBetweenPoint(Tools.getGlobalVector(ai_world_state.getRobot(), new Vector2D(Robot.LENGTH_CM/2, 0)), global);
	}
	
	/**
	 * Distance from centre of robot to a given point on table
	 * @param global
	 * @return
	 */
	private double distanceTo(Vector2D global) {
		return Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), global).getLength();
	}
	
	private void slowDownSpeed(double distance, double threshold, Commands current_speed, double slow_speed) {
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

}
