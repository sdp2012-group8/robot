package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Painter;
import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;

public class AIVisualServoing extends AI {

	/** Whether the robot is allowed to drive in reverse. */
	private static final boolean REVERSE_DRIVING_ENABLED = true;

	/** The multiplier to the final speed. */
	private final static double SPEED_MULTIPLIER = 1;
	private final static double TURN_SPD_MULTIPLIER = 1.8;

	private final static int COLL_SECS_COUNT = 222;
	private final static double SEC_ANGLE = 360d/COLL_SECS_COUNT;

	// corner thresholds
	private final static int THRESH_CORN_HIGH = 5;
	private final static int THRESH_CORN_LOW = 2;

	// back away threshold
	private final static int THRESH_BACK_HIGH = 25;
	private final static int THRESH_BACK_LOW = 15;

	/** Threshold for being at the target point */
	private final static int POINT_ACCURACY = 5;

	/** How close the robot should be to the ball before it attempts to kick it. */
	public static final double KICKABLE_BALL_DIST = 6;

	private final static int MAX_TURN_ANG = (int) (SPEED_MULTIPLIER*180-100);
	public final static double DEFAULT_POINT_OFF = 2*Robot.LENGTH_CM;
	private double point_off = DEFAULT_POINT_OFF;
	public final static double TARG_THRESH = 40;



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
		// Are we ready to score?
		if (ai_world_state.getDistanceToBall() < KICKABLE_BALL_DIST) {
			Point2D.Double intersection = GeomUtils.getLineIntersection(ai_world_state.getRobot().getCoords(),
					ai_world_state.getRobot().getFrontCenter(), ai_world_state.getEnemyGoal().getTop(),
					ai_world_state.getEnemyGoal().getBottom());
			if (intersection != null && Utilities.lineIntersectsRobot(ai_world_state.getRobot().getCoords(),
					intersection, ai_world_state.getEnemyRobot())) {
				return gotBall();
			}
		}

		// Get the point to drive towards.
		target = new Vector2D(ai_world_state.getBallCoords());

		Point2D.Double optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state, point_off);
		if (optimalPoint != null) {
			target = new Vector2D(optimalPoint);
		}

		// Generate command to drive towards the target point.
		boolean mustFaceTarget = (point_off != TARG_THRESH);

		Waypoint waypoint = getNextWaypoint(target, true);
		return getWaypointCommand(waypoint, mustFaceTarget);
	}


	
	/**
	 * Used for getting to the ball when shooting penalties
	 * @param priority - 1 for shooting with main goal, 2 for shooting with walls only, 3 for shooting with both
	 * @return
	 * @throws IOException
	 */
	protected Command chaseBall(int priority) throws IOException {
		// Are we ready to score?
		if (ai_world_state.getDistanceToBall() < KICKABLE_BALL_DIST) {
			Point2D.Double intersection = GeomUtils.getLineIntersection(ai_world_state.getRobot().getCoords(),
					ai_world_state.getRobot().getFrontCenter(), ai_world_state.getEnemyGoal().getTop(),
					ai_world_state.getEnemyGoal().getBottom());
			if (intersection != null && Utilities.lineIntersectsRobot(ai_world_state.getRobot().getCoords(),
					intersection, ai_world_state.getEnemyRobot())) {
				return gotBall();
			}
		}

		// Get the point to drive towards.
		target = new Vector2D(ai_world_state.getBallCoords());
		Point2D.Double optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state, point_off);
		
		switch (priority){
		case 1:
			optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state, point_off, false);
		case 2:
			optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state, point_off, true);
		case 3:
			optimalPoint = Utilities.getOptimalPointBehindBall(ai_world_state, point_off);	
		}
		
		if (optimalPoint != null) {
			target = new Vector2D(optimalPoint);
		}

		// Generate command to drive towards the target point.
		boolean mustFaceTarget = (point_off != TARG_THRESH);

		Waypoint waypoint = getNextWaypoint(target, true);
		return getWaypointCommand(waypoint, mustFaceTarget);
	}
	
	
	/**
	 * Generate a command to get closer to a target point.
	 * 
	 * @param targetDistance Distance to the target.
	 * @return A command to execute next.
	 */
	@Deprecated
	private Command chasingTarget(double targetDistance) {
		Command comm = getCommandOld(getNextWaypoint(target, true), false);

		if (targetDistance < POINT_ACCURACY) {
			chasing_target = false;
		}

		double dir_angle = Vector2D.rotateVector(Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), target), -ai_world_state.getRobot().getAngle()).getDirection();
		if (Math.abs(dir_angle) < 20) {
			reduceSpeed(comm, targetDistance, 20, 2);
		}

		normalizeSpeed(comm);
		return comm;
	}

	/**
	 * Generate a command to get closer to a target point, which happens to be
	 * the ball.
	 * 
	 * @param targetDistance Distance to the target.
	 * @return A command to execute next.
	 */
	@Deprecated
	private Command chasingBall(double targetDistance) {
		Command comm = null;
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());

		if (ai_world_state.getDistanceToBall() > Robot.LENGTH_CM*2) {
			chasing_target = true;
			return chasingTarget(targetDistance);
		}

		comm = getCommandOld(getNextWaypoint(ball, false), true);
		if (Math.abs(comm.getShortTurnSpeed()) > 3) {
			comm.speed = 0;
		} else {
			reduceSpeed(comm, ai_world_state.getDistanceToBall(), 10, 2);
		}

		if (comm.getShortSpeed() == 0 && comm.getShortTurnSpeed() == 0) {
			comm = getCommandOld(getNextWaypoint(ball, false), true);
		}

		if (ai_world_state.getMyGoalLeft()) {
			if (ball.getX() < ai_world_state.getRobot().getCoords().getX()) {
				chasing_target = true;
			}
		}  else {
			if (ball.getX() > ai_world_state.getRobot().getCoords().getX()) {
				chasing_target = true;
			}
		}

		return comm;
	}


	@Override
	protected Command gotBall() throws IOException {
		point_off = DEFAULT_POINT_OFF;
		Painter.point_off = point_off;
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
		Point2D.Double int1;
		Point2D.Double int2;
		if (ai_world_state.getMyGoalLeft()){
			int1 = new Point2D.Double(ai_world_state.getMyGoal().getTop().x + 30, ai_world_state.getMyGoal().getTop().y);
			int2 = new Point2D.Double(ai_world_state.getMyGoal().getBottom().x + 30, ai_world_state.getMyGoal().getBottom().y);
		} else {
			int1 = new Point2D.Double(ai_world_state.getMyGoal().getTop().x - 30, ai_world_state.getMyGoal().getTop().y);
			int2 = new Point2D.Double(ai_world_state.getMyGoal().getBottom().x - 30, ai_world_state.getMyGoal().getBottom().y);
		}

		Point2D.Double intercept= GeomUtils.getLineIntersection(ai_world_state.getBallCoords(), ai_world_state.getEnemyRobot().getCoords(), int1, int2);

		boolean is_main_point = false;
		Command com = new Command(0, 0, false);
		double dist = 10000;

		Point2D.Double point = new Point2D.Double(10000, 10000);
		Point2D.Double point2 = new Point2D.Double(10000, 10000);
		Point2D.Double point3 = new Point2D.Double(10000, 10000);

		if (intercept != null){

			if (ai_world_state.getMyGoalLeft()){
				point= intercept;
			} else {
				point= intercept;
			}

			is_main_point = (intercept.y < ai_world_state.getMyGoal().getBottom().y)  && (intercept.y > ai_world_state.getMyGoal().getTop().y);
			dist = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(point)).getLength();
		}

		if (ai_world_state.getMyGoalLeft()){
			point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x+30 , ai_world_state.getMyGoal().getBottom().y);
			point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x+30 , ai_world_state.getMyGoal().getTop().y);
		} else {
			point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x-30 , ai_world_state.getMyGoal().getBottom().y);
			point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x-30 , ai_world_state.getMyGoal().getTop().y);
		}

		double dist2 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(point2)).getLength();
		double dist3 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(point3)).getLength();

		Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};



		if(ai_world_state.getEnemyRobot().getAngle()>-90 && ai_world_state.getEnemyRobot().getAngle()<90)
			return chaseBall();

		if(is_main_point)	{
			if (dist > 5)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point), true), false);
			reduceSpeed(com, dist, 20, 0);
			return com;
		}

		if(ai_world_state.getEnemyRobot().getAngle()<-90 && ai_world_state.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point2), true), false);
			reduceSpeed(com, dist2, 20, 0);
			return com;
		}

		if(ai_world_state.getEnemyRobot().getAngle()>90 && ai_world_state.getEnemyRobot().getAngle()<180 ){
			if (dist3 > 10)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point3), true), false);
			reduceSpeed(com, dist3, 20, 0);
			return com;
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

		Point2D.Double interceptBall= GeomUtils.getLineIntersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getRobot().getCoords(), ai_world_state.getRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + ai_world_state.getRobot().getCoords().y);

		if (!interceptBall.equals(null)){

			if((interceptBall.y < ai_world_state.getMyGoal().getBottom().y)  && (interceptBall.y > ai_world_state.getMyGoal().getTop().y)){
				if ((interceptBall.y > ai_world_state.getRobot().getCoords().y)  ){
					short forward_speed = (short) -20;
					return new Command(forward_speed, 0, false);
				} else if((interceptBall.y < ai_world_state.getRobot().getCoords().y)) {
					short forward_speed = (short) 20;
					return new Command(forward_speed, 0, false);
				}
			}
			else
			{
				return new Command( 0, 0, false);
			}

		}

		return null;

	}

	/**
	 * Go into chaseBall(priority), which is chaseBall, but 
	 * you can specify if you want to shoot only into the main goal, 
	 * only into the image goals of in both
	 */
	@Override
	protected Command penaltiesAttack() throws IOException {
		
		/**
		 *  priority - 1 for shooting with main goal, 2 for shooting with walls only, 3 for shooting with both
		 */
		int priority = 2;
		return chaseBall(priority);
		
		/*
		Command command = new Command(0,0,false);

		Point2D.Double pointInGoal= 
				GeomUtils.getLineIntersection(ai_world_state.getRobot().getCoords(), 
						ai_world_state.getRobot().getFrontCenter(), ai_world_state.getEnemyGoal().getTop(), 
						ai_world_state.getEnemyGoal().getBottom());
		boolean clear_path = Utilities.lineIntersectsRobot(pointInGoal, ai_world_state.getBallCoords(), 
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
		}  */
	}


	/**
	 * Find a point that the robot should go to in order to reach the specified
	 * target point. It robot will attempt to avoid obstacles.
	 * 
	 * @param target Point to drive towards.
	 * @param ballIsObstacle Whether the ball should be considered an obstacle.
	 * @return The next command to execute.
	 */
	private Waypoint getNextWaypoint(Vector2D target, boolean ballIsObstacle) {
		// Target point data in local coordinates.
		Vector2D targetLocal = Utilities.getLocalVector(ai_world_state.getRobot(), target);

		// Which objects to consider as obstacles?
		int obstacleFlags = 0;
		obstacleFlags |= (ai_world_state.getMyTeamBlue()
				? Utilities.YELLOW_IS_OBSTACLE_FLAG
				: Utilities.BLUE_IS_OBSTACLE_FLAG);
		if (ballIsObstacle) {
			obstacleFlags |= Utilities.BALL_IS_OBSTACLE_FLAG;
		}

		// Check if the target point is directly visible.
		Vector2D ownCoords = new Vector2D(ai_world_state.getRobot().getCoords());
		if (Utilities.isDirectPathClear(ai_world_state, ownCoords, target, obstacleFlags)) {
			return new Waypoint(targetLocal, true);
		}

		// Compute the destination point.
		Vector2D destPoint = null;
		double destPointDist = targetLocal.getLength();

		double minAngle = Double.MAX_VALUE;
		int iterations = 0;

		while ((destPoint == null) && (iterations < 5) && (destPointDist > 0)) {
			for (int i = 0; i < COLL_SECS_COUNT; i++) {
				double curAngle = -90 + i * SEC_ANGLE + SEC_ANGLE / 2;
				curAngle = Utilities.normaliseAngle(curAngle);

				Vector2D rayDir = Vector2D.rotateVector(new Vector2D(1, 0), curAngle);
				Vector2D rayEndLocal = Vector2D.multiply(rayDir, destPointDist);
				Vector2D rayEnd = Utilities.getGlobalVector(ai_world_state.getRobot(), rayEndLocal);

				if (Utilities.isDirectPathClear(ai_world_state, ownCoords, rayEnd, obstacleFlags)) {
					double angleDiff = Utilities.normaliseAngle(curAngle - targetLocal.getDirection());
					if (Math.abs(angleDiff) < Math.abs(minAngle)) {
						minAngle = angleDiff;
						destPoint = rayEndLocal;
					}
				}
			}

			++iterations;
			destPointDist -= Robot.LENGTH_CM;
		}

		if (destPoint == null) {
			destPoint = targetLocal;
		}

		return new Waypoint(destPoint, false);
	}


	/**
	 * Generate a command that will get the robot closer to the specified
	 * waypoint.
	 * 
	 * @param waypoint Point to move towards.
	 * @param mustFaceTarget Whether the robot is required to face the point
	 * 		at all times.
	 * @return A command that will get the robot closer towards the given
	 * 		waypoint.
	 */
	private Command getWaypointCommand(Waypoint waypoint, boolean mustFaceTarget) {
		Command comm = new Command(0.0, 0.0, false);

		comm.turning_speed = waypoint.getTurningAngle();

		// Ball is behind.
		if (Math.abs(waypoint.getTurningAngle()) > 90) {
			comm.speed = -MAX_SPEED_CM_S;
			if (REVERSE_DRIVING_ENABLED && !mustFaceTarget) {
				comm.turning_speed = Utilities.normaliseAngle(waypoint.getTurningAngle() - 180);
			}		
		// Ball is in front.
		} else {
			comm.speed = MAX_SPEED_CM_S;
		}

		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? THRESH_BACK_LOW : THRESH_BACK_HIGH);
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		if ((Math.abs(waypoint.getTurningAngle()) < 45) && (waypoint.getDistance() < TARG_THRESH)) {
			point_off *= 0.7;
		}
		if (point_off < KICKABLE_BALL_DIST) {
			point_off = KICKABLE_BALL_DIST;
		}

		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		if (ai_world_state.getMyGoalLeft()) {
			if (ball.getX() < ai_world_state.getRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
			}
		} else {
			if (ball.getX() > ai_world_state.getRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
			}
		}

		reduceSpeed(comm, waypoint.getDistance(), 30, MAX_SPEED_CM_S/2);

		Painter.point_off = point_off;

		comm.turning_speed *= TURN_SPD_MULTIPLIER;
		comm.speed *= SPEED_MULTIPLIER;
		comm.acceleration = 100;

		normalizeSpeed(comm);

		return comm;
	}


	/**
	 * Checks if our robot's front or back sides collide with anything and
	 * reacts to the collision if they do.
	 * 
	 * @param command Command to modify in response to a collision.
	 * @param ballIsObstacle Whether the ball should be considered an obstacle.
	 * @param threshold Corner collision threshold. Higher values = lower
	 * 		tolerance.
	 */
	private void reactToFrontBackCollisions(Command command, boolean ballIsObstacle, double threshold) {
		double frontCollDist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(),
				-10, 10, 20, ballIsObstacle).getLength();
		double backCollDist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(),
				170, -170, 20, ballIsObstacle).getLength();

		// Collision in front.
		if (frontCollDist < threshold) {
			if (command.speed >= 0) {
				double speed_coeff = (frontCollDist / threshold) - 1;
				if (speed_coeff > 0) {
					speed_coeff = 0;
				}
				if (speed_coeff < -1) {
					speed_coeff = -1;
				}
				command.speed *= speed_coeff;
			}

		// Collision in the back.
		} else if (backCollDist < threshold) {
			if (command.speed <= 0) {
				double speed_coeff = (backCollDist / threshold) - 1;
				if (speed_coeff > 0) {
					speed_coeff = 0;
				}
				if (speed_coeff < -1) {
					speed_coeff = -1;
				}
				command.speed *= speed_coeff;
			}
		}
	}

	/**
	 * Checks if our robot's corners collide with anything and reacts to the
	 * collision if they do.
	 * 
	 * @param command Command to modify in response to a collision.
	 * @param threshold Corner collision threshold. Higher values = lower
	 * 		tolerance.
	 */
	private void reactToCornerCollisions(Command command, double threshold) {
		Vector2D nearestColl = Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords());

		Vector2D frontLeft = Utilities.getLocalVector(ai_world_state.getRobot(), Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()), nearestColl));
		Vector2D frontRight = Utilities.getLocalVector(ai_world_state.getRobot(), Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()), nearestColl));
		Vector2D backLeft = Utilities.getLocalVector(ai_world_state.getRobot(), Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()), nearestColl));
		Vector2D backRight = Utilities.getLocalVector(ai_world_state.getRobot(), Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()), nearestColl));

		boolean haveFrontLeftColl = (frontLeft.getLength() <= threshold);
		boolean haveFrontRightColl = (frontRight.getLength() <= threshold);
		boolean haveBackLeftColl = (backLeft.getLength() <= threshold);
		boolean haveBackRightColl = (backRight.getLength() <= threshold);

		boolean haveFrontColl = haveFrontLeftColl || haveFrontRightColl;
		boolean haveAnyColl = haveFrontLeftColl || haveFrontRightColl || haveBackLeftColl || haveBackRightColl;

		if (haveAnyColl) {
			command.speed = (haveFrontColl ? -MAX_SPEED_CM_S : MAX_SPEED_CM_S);
			command.turning_speed += (haveFrontColl ? -10 : 10);
		}
	}


	/**
	 * Get the distance from our robot to some point in the world.
	 * 
	 * @param point Point in global coordinates to find distance to.
	 * @return Distance from our robot to the given point in the world.
	 */
	private double distanceTo(Vector2D point) {
		return Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), point).getLength();
	}


	/**
	 * Reduces the robot's speed depending on the distance to a particular
	 * object.
	 * 
	 * @param command The command to be modified.
	 * @param distance Current distance to the object.
	 * @param threshold Distance threshold, after which speed is reduced.
	 * @param baseSpeed Minimum speed the robot should slow to.
	 */
	private void reduceSpeed(Command command, double distance, double threshold, double baseSpeed) {
		if (distance >= threshold) {
			return;
		}

		if (command.speed < 0) {
			baseSpeed = -baseSpeed;
		}

		if (Math.abs(command.speed) < Math.abs(baseSpeed)) {
			command.speed = baseSpeed;
		} else {
			double coeff = distance / threshold;
			command.speed = baseSpeed + coeff * (command.speed - baseSpeed);
		}
	}

	/**
	 * Reduce the forward speed based on the robot's turning speed.
	 * 
	 * @param comm The command to be modified.
	 */
	public void normalizeSpeed(Command comm) {
		if (Math.abs(comm.turning_speed) > MAX_TURN_ANG) {
			comm.speed = 0;
		} else {		
			double rat = Math.abs(comm.turning_speed) / MAX_TURN_ANG;
			comm.speed *= 1 - rat;
		}
	}


	/**
	 * @see sdp.AI.AI#sit()
	 */
	@Override
	public Command sit() throws IOException {
		point_off = DEFAULT_POINT_OFF;
		Painter.point_off = point_off;
		return super.sit();
	}


	/**
	 * A compatibility method to return a command from a waypoint using the
	 * old method.
	 * 
	 * @param waypoint Target waypoint.
	 * @param mustFaceTarget Require the robot to face the target point.
	 * @return A command to get closer to the waypoint.
	 */
	@Deprecated
	private Command getCommandOld(Waypoint waypoint, boolean mustFaceTarget) {
		Command comm = new Command(0.0, 0.0, false);

		comm.turning_speed = waypoint.getTurningAngle();

		// Ball is behind.
		if (Math.abs(comm.turning_speed) > 90) {
			comm.speed = -MAX_SPEED_CM_S;
			if (REVERSE_DRIVING_ENABLED && !mustFaceTarget) {
				comm.turning_speed = Utilities.normaliseAngle(comm.turning_speed - 180);
			}		
		// Ball is in front.
		} else {
			comm.speed = MAX_SPEED_CM_S;
		}

		// if we get within too close (within coll_start) of an obstacle
		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? THRESH_BACK_LOW : THRESH_BACK_HIGH);

		// check if either of the corners are in collision
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		return comm;
	}

}

