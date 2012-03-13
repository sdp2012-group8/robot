package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.Vector2D;

public class AIVisualServoing extends AI {
	
	
	/** The multiplier to the final speed to slow it down */
	private final static double SPEED_MULTIPLIER = 1.0;

	private final static int COLL_SECS_COUNT = 222;
	private final static double SEC_ANGLE = 360d/COLL_SECS_COUNT;
	
	// corner thresholds
	private final static int THRESH_CORN_HIGH = 5;
	private final static int THRESH_CORN_LOW = 2;
	
	// back away threshold
	private final static int THRESH_BACK_HIGH = 25;
	private final static int THRESH_BACK_LOW = 2;
	
	/** Threshold for being at the target point */
	private final static int POINT_ACCURACY = 5;
	public static final int DIST_TO_BALL = 6;

	private final static int MAX_TURN_ANG = 200;

	/**
	 * True if the robot is chasing the target.
	 * False if the robot is chasing the real ball.
	 */
	private boolean chasing_target = true;
	
	private Vector2D target = null;

	
	/**
	 * @see sdp.AI.AI#chaseBall()
	 */
	@Override
	protected Command chaseBall() throws IOException {		
		// Are we ready to score? TODO
		if (ai_world_state.getDistanceToBall() < DIST_TO_BALL){
			return gotBall();
		}
		
		// Get the point to drive towards.
		target = new Vector2D(ai_world_state.getBallCoords());
		
		Point2D.Double optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state);
		if (optimalPoint != null) {
			target = new Vector2D(optimalPoint);
		}

		// Generate command to drive towards the target point.
		Command comm = null;
		double targetDistance = distanceTo(target);
		
		if (chasing_target) {
			comm = chasingTarget(targetDistance);
		} else {
			comm = chasingBall(targetDistance);
		}

		comm.speed *= SPEED_MULTIPLIER;

		return comm;
	}
	
	
	/**
	 * Generate a command to get closer to a target point.
	 * 
	 * @param targetDistance Distance to the target.
	 * @return A command to execute next.
	 */
	private Command chasingTarget(double targetDistance) {
		Command comm = goTowardsPoint(target, true, false);

		if (targetDistance < POINT_ACCURACY)
			chasing_target = false;

		double dir_angle = Vector2D.getDirection(Vector2D.rotateVector(Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), target), -ai_world_state.getRobot().getAngle()));
		if (Math.abs(dir_angle) < 20) {
			slowDownSpeed(targetDistance, 20, comm, 2);
		}

		normalizeRatio(comm);
		return comm;
	}
	
	/**
	 * Generate a command to get closer to a target point, which happens to be
	 * the ball.
	 * 
	 * @param targetDistance Distance to the target.
	 * @return A command to execute next.
	 */
	private Command chasingBall(double targetDistance) {
		Command comm = null;
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		
		if (ai_world_state.getDistanceToBall() > Robot.LENGTH_CM*2) {
			chasing_target = true;
			return chasingTarget(targetDistance);
		}
		
		comm = goTowardsPoint(ball, false, true);
		if (Math.abs(comm.getByteTurnSpeed()) > 3)
			comm.speed = 0;
		else
			slowDownSpeed(ai_world_state.getDistanceToBall(), 10, comm, 2);


		if (comm.getByteSpeed() == 0 && comm.getByteTurnSpeed() == 0)
			comm = goTowardsPoint(ball, false, true);

		if (ai_world_state.getMyGoalLeft()) {
			if (ball.getX() < ai_world_state.getRobot().getCoords().getX())
				chasing_target = true;
		}  else {
			if (ball.getX() > ai_world_state.getRobot().getCoords().getX())
				chasing_target = true;
		}
		
		return comm;
	}

	
	@Override
	protected Command gotBall() throws IOException {
		//System.out.println("GOT BALL");
		double angle = ai_world_state.getRobot().getAngle();
		if (ai_world_state.getMyGoalLeft()) {
			if (angle > 90 || angle < -90) {
				//System.out.println("REVERSE angle = " + angle);
				return new Command(-MAX_SPEED_CM_S, 0, false);
			}
		} else {
			if (Math.abs(angle) <= 90) {
				//System.out.println("FORWARD");
				return new Command(-MAX_SPEED_CM_S, 0, false);
			}
		}
		chasing_target = true;
		return new Command(MAX_SPEED_CM_S,0,true);
	}

	@Override
	protected Command defendGoal() throws IOException {
		// TODO Auto-generated method stub


		Point2D.Double intercept= Utilities.intersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getMyGoal().getTop(), ai_world_state.getMyGoal().getBottom());


		if (intercept != null){
			Point2D.Double point;
			Point2D.Double point2 ;
			Point2D.Double point3;
			Command com = new Command(0, 0, false);
			if (ai_world_state.getMyGoalLeft()){
				point= new Point2D.Double(intercept.x+20 , intercept.y);
				point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x+20 , ai_world_state.getMyGoal().getBottom().y);
				point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x+20 , ai_world_state.getMyGoal().getTop().y);
			} else {
				point= new Point2D.Double(intercept.x -20, intercept.y);
				point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x-20 , ai_world_state.getMyGoal().getBottom().y);
				point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x-20 , ai_world_state.getMyGoal().getTop().y);
			}
			double dist = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(point)).getLength();
			double dist2 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ai_world_state.getMyGoal().getBottom())).getLength();
			double dist3 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ai_world_state.getMyGoal().getTop())).getLength();
		 if((intercept.y < ai_world_state.getMyGoal().getBottom().y+15)  && (intercept.y > ai_world_state.getMyGoal().getTop().y-15))	{
		   if (dist > 5)
				com = goTowardsPoint(new Vector2D(point), false, false);
			slowDownSpeed(dist, 20, com, 0);

			return com;
		} else if(ai_world_state.getEnemyRobot().getAngle()<0 && ai_world_state.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = goTowardsPoint(new Vector2D(point2), false, false);
			    slowDownSpeed(dist2, 20, com, 0);
			return com;
			}

		 else if(ai_world_state.getEnemyRobot().getAngle()>0 && ai_world_state.getEnemyRobot().getAngle()<180 ){
			 if (dist3 > 10)
					com = goTowardsPoint(new Vector2D(point3), false, false);
				    slowDownSpeed(dist3, 20, com, 0);
				return com;
		 }
		}

		return null;
	}

	@Override
	protected Command penaltiesDefend() throws IOException {

		//TODO: Find direction of opposing robot and move into intercept path.
		//
		// Our robot will be placed like shown bellow:
		//
		// *----------------------------------------------------------*
		// |														  |
		// |														  |
		// |_														 _|
		// |  _____		 ______										  |
		// |  |	| |		|_____||									  |
		// |  |_|_| 	|_____||									  |
		// |  |___|		         									  |
		// |														  |
		// |_														 _|
		// |	     												  |
		// |                                                          |
		// *----------------------------------------------------------*
		//
		//
		// 
		//
		// Get the direction of the enemy robot and make that a vector.
		// Find the intersection between our direction vector and the enemies robot direction, this is where our robot will need to be.
		// Make our robot move to that intersection.
		//
		//

		Point2D.Double interceptBall= Utilities.intersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getRobot().getCoords(), ai_world_state.getRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + ai_world_state.getRobot().getCoords().y);

		if (!interceptBall.equals(null)){

			if((interceptBall.y < ai_world_state.getMyGoal().getBottom().y)  && (interceptBall.y > ai_world_state.getMyGoal().getTop().y)){
				if ((interceptBall.y > ai_world_state.getRobot().getCoords().y)  ){
					byte forward_speed = (byte) -20; //Utilities.normaliseToByte((15+(interceptDistance.getLength()/40)*25));
					return new Command(forward_speed, 0, false);
					//mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				} else if((interceptBall.y < ai_world_state.getRobot().getCoords().y)) {
					byte forward_speed = (byte) 20; //Utilities.normaliseToByte(-(15+(interceptDistance.getLength()/40)*25));
					return new Command(forward_speed, 0, false);
					//mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				}
			}
			else
			{
				return new Command( 0, 0, false);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
			}

		}
		
		return null;

	}

	@Override
	protected Command penaltiesAttack() throws IOException {
		Command command = new Command(0,0,false);

		Point2D.Double pointInGoal= 
			Utilities.intersection(ai_world_state.getRobot().getCoords(), 
					ai_world_state.getRobot().getFrontCenter(), ai_world_state.getEnemyGoal().getTop(), 
					ai_world_state.getEnemyGoal().getBottom());
		boolean clear_path = Utilities.isPathClear(pointInGoal, ai_world_state.getBallCoords(), 
				ai_world_state.getEnemyRobot());
		//        System.out.println(clear_path);
		if (clear_path){
			return new Command(0,0,true);
			//mComm.sendMessage(opcode.kick);
			//System.out.println("kicking");
			//ai_world_state.setMode(mode.chase_ball);

		}

		Point2D enemyRobot;
		if(ai_world_state.isGoalVisible())        {
			enemyRobot= ai_world_state.getEnemyRobot().getCoords();
			//if enemy robot in the lower part of the goal then shoot in the upper part
			if( enemyRobot.getY() < ai_world_state.getEnemyGoal().getCentre().y){
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate,(byte) 0, (byte) 
				//ai_world_state.getEnemyGoal().getTop().y);
				// mComm.sendMessage(opcode.kick);
				//if enemy robot in the upper part of the goal then shoot in the lower part
			}else if( enemyRobot.getY() <ai_world_state.getEnemyGoal().getCentre().y){
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte)
				//ai_world_state.getEnemyGoal().getBottom().y);
				// mComm.sendMessage(opcode.kick);
			}	
			//else just kick in upper part of the goal by ...this is the default
			else{
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte) 
				//ai_world_state.getEnemyGoal().getTop().y);
				//mComm.sendMessage(opcode.kick);
			}
		}
		return null;
	}


	/**
	 * Generate a command that will get the robot closer to the given point on
	 * the field. The robot will attempt to avoid obstacles.
	 * 
	 * @param target Point to drive towards.
	 * @param ballIsObstacle Whether the ball should be considered an obstacle.
	 * @param need_to_face_point
	 * @return The next command to execute.
	 */
	private Command goTowardsPoint(Vector2D target, boolean ballIsObstacle,
			boolean need_to_face_point) {
		Command comm = new Command(0, 0, false);

		// Target point data in local coordinates.
		Vector2D targetLocal = Utilities.getLocalVector(ai_world_state.getRobot(), target);
		double targetDirLocal = Vector2D.getDirection(targetLocal);
		double targetDistLocal = targetLocal.getLength();
		
		// Which objects to consider as obstacles?
		int obstacleFlags = 0;
		obstacleFlags |= (ai_world_state.getMyTeamBlue()
				? Utilities.YELLOW_IS_OBSTACLE_FLAG
				: Utilities.BLUE_IS_OBSTACLE_FLAG);
		if (ballIsObstacle) {
			obstacleFlags |= Utilities.BALL_IS_OBSTACLE_FLAG;
		}
		
		// Check if the target point is directly visible.
		double targetCollDist = Utilities.getClosestCollisionDist(ai_world_state,
				new Vector2D(ai_world_state.getRobot().getCoords()),
				target, obstacleFlags) + Robot.LENGTH_CM;
		boolean isTargetVisible = (targetCollDist >= targetDistLocal);
		
		// Compute distance to the destination point.
		Vector2D ownCoords = new Vector2D(ai_world_state.getRobot().getCoords());
		Vector2D enemyCoords = new Vector2D(ai_world_state.getEnemyRobot().getCoords());		
		double enemyRobotDist = Vector2D.subtract(ownCoords, enemyCoords).getLength();
		
		double destPointDist = (isTargetVisible	? targetDistLocal
				: (enemyRobotDist + Robot.LENGTH_CM / 2));

		// Compute angle to the destination point.
		double destPointAngle = Double.NaN;
		
		double minAngle = Double.MAX_VALUE;
		int iterations = 0;
		
		while (Double.isNaN(destPointAngle) && (iterations < 5)) {
			for (int i = 0; i < COLL_SECS_COUNT; i++) {
				double curAngle = -90 + i * SEC_ANGLE + SEC_ANGLE / 2;
				curAngle = Utilities.normaliseAngle(curAngle);
				
				Vector2D curDir = Vector2D.rotateVector(new Vector2D(1, 0), curAngle);
				Vector2D rayEndLocal = Vector2D.multiply(curDir, destPointDist);
				Vector2D rayEnd = Utilities.getGlobalVector(ai_world_state.getRobot(), rayEndLocal);
				
				if (Utilities.isDirectPathClear(ai_world_state, ownCoords, rayEnd, obstacleFlags)) {
					double angleDiff = Utilities.normaliseAngle(curAngle - targetDirLocal);
					if (Math.abs(angleDiff) < Math.abs(minAngle)) {
						minAngle = angleDiff;
						destPointAngle = curAngle;
					}
				}
			}
			
			++iterations;			
			destPointDist -= Robot.LENGTH_CM; 	// TODO: What if this turns negative?
		}

		if (Double.isNaN(destPointAngle)) {
			destPointAngle = targetDirLocal;
		}
		
		comm.turning_speed = destPointAngle;
		System.out.println(destPointAngle + " " + minAngle);

		if (need_to_face_point) { 
			// set forward speed
			if (comm.turning_speed > 90 || comm.turning_speed < -90){ 
				// ball is behind
				comm.speed = -MAX_SPEED_CM_S;
			} else { 
				//ball is in front
				comm.speed = MAX_SPEED_CM_S;
			}
		} else {
			// go fastest to a point regardless of direction

			if (comm.turning_speed > 90 || comm.turning_speed < -90) {
				// ball is behind
				comm.speed = -MAX_SPEED_CM_S;

				// go backwards to get to ball as soon as possible
				comm.turning_speed = Utilities.normaliseAngle(comm.turning_speed-180);
			} else {
				// ball is in front
				comm.speed = MAX_SPEED_CM_S;
			}
		}

		// if we get within too close (within coll_start) of an obstacle
		backAwayIfTooClose(comm, ballIsObstacle, isTargetVisible ? THRESH_BACK_LOW : THRESH_BACK_HIGH);

		// check if either of the corners are in collision
		nearCollisionCheck(comm, isTargetVisible ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		return comm;
	}

	/**
	 * If too close to an obstacle from sectors, back away
	 * @param command
	 */
	private void backAwayIfTooClose(Command command, boolean include_ball_as_obstacle, double threshold) {
		double for_dist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(), -10, 10, 20, include_ball_as_obstacle).getLength(); // get collision distance at the front
		double back_dist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(), 170, -170, 20, include_ball_as_obstacle).getLength(); // get collision distance at the back

		if (ai_world_state.isDist_sensor()){
			if (command.speed >= 0) {
				// go backwards
				double speed_coeff = -1+for_dist/threshold;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				command.speed *= speed_coeff;
			}
		} else if (back_dist < threshold) {
			if (command.speed <= 0) {
				// same as above
				double speed_coeff = -1+back_dist/threshold;
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
	private void nearCollisionCheck(Command command, double threshold) {
		final Vector2D
		front_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		front_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())));
		final boolean
		front_left_coll = front_left.getLength() <= threshold,
		front_right_coll = front_right.getLength() <= threshold,
		back_left_coll = back_left.getLength() <= threshold,
		back_right_coll = back_right.getLength() <= threshold,
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

	/**
	 * Normalises speed to be within a upper and lower limit.
	 * @param distance Distance to the ball in which the speed is slowed down.
	 * @param threshold 
	 * @param current_speed The Command to be modified.
	 * @param slow_speed Minimum speed the robot should slow to.
	 */
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

	/**
	 * Changes the speed to be a function of the turning speed.
	 * @param comm The command to be normalised.
	 */
	public void normalizeRatio(Command comm) {
		if (Math.abs(comm.turning_speed) > MAX_TURNING_SPEED) {
			comm.speed = 0;
			return;
		}
		double rat = Math.abs(comm.turning_speed)/MAX_TURNING_SPEED;
		comm.speed *= 1-rat;
	}
	

}
