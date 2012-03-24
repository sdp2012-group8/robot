package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Painter;
import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.WorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;

public class AIVisualServoing extends BaseAI {

	/** Whether the robot is allowed to drive in reverse. */
	private static final boolean REVERSE_DRIVING_ENABLED = true;

	/** The multiplier to the final speed. */
	private static final double SPEED_MULTIPLIER = 1;
	private static final double TURN_SPD_MULTIPLIER = 1.8;

	private static final int COLL_SECS_COUNT = 222;
	private static final double SEC_ANGLE = 360d/COLL_SECS_COUNT;

	// corner thresholds
	private static final int THRESH_CORN_HIGH = 5;
	private static final int THRESH_CORN_LOW = 2;

	// back away threshold
	private static final int THRESH_BACK_HIGH = 25;
	private static final int THRESH_BACK_LOW = 15;

	/** Threshold for being at the target point */
	private static final int POINT_ACCURACY = 5;

	public static final double DEFAULT_POINT_OFF = 2*Robot.LENGTH_CM;
	private double point_off = DEFAULT_POINT_OFF;
	public static final double DEFAULT_TARG_THRESH = 30;
	private double targ_thresh = DEFAULT_TARG_THRESH;

	private static final double DEFEND_BALL_THRESHOLD = 10;

	/**
	 * True if the robot is chasing the target.
	 * False if the robot is chasing the real ball.
	 */
	private boolean chasing_target = true;

	private Vector2D target = null;


	/**
	 * @see sdp.AI.BaseAI#chaseBall()
	 */
	@Override
	protected Command chaseBall() throws IOException {
		return attack(Utilities.AttackMode.Full, true);
	}


	/**
	 * Get a command to attack the opponents.
	 * 
	 * @param mode Robot's attack mode. See {@link Utilities.AttackMode}.
	 * @param defend Whether to consider defending the gate.
	 * @return The next command the robot should execute.
	 * @throws IOException 
	 */
	protected Command attack(Utilities.AttackMode mode, boolean defend) throws IOException {
		if (defend && Utilities.canEnemyAttack(aiWorldState)) {
			return defendGoal();
		}

		// Are we ready to score?
		if (Utilities.canWeAttack(aiWorldState)) {
			return gotBall();
		}

		boolean chasing_ball_instead = false;
		
		// Get the point to drive towards.
		target = new Vector2D(aiWorldState.getBallCoords());
		
		Point2D.Double optimalPoint = null;
		for (int t = 0; t < 20; t++) {
			optimalPoint = Utilities.getOptimalPoint(aiWorldState, point_off, mode);
			
			if (optimalPoint == null)
				point_off *= 0.8;
			else
				break;
		}

		if (optimalPoint != null) {
			target = new Vector2D(optimalPoint);
			chasing_ball_instead = true;
		}

		// Generate command to drive towards the target point.
		boolean mustFaceTarget = (point_off != DEFAULT_TARG_THRESH);

		Waypoint waypoint = getNextWaypoint(target, !chasing_ball_instead);
		return getWaypointCommand(waypoint, mustFaceTarget, SPEED_MULTIPLIER, SPEED_MULTIPLIER*180-100);
	}

	@Override
	protected synchronized Command gotBall() throws IOException {
		if (Utilities.canWeAttack(aiWorldState)) {
			point_off = DEFAULT_POINT_OFF;
			targ_thresh = DEFAULT_TARG_THRESH;
			Painter.point_off = point_off;
			Painter.targ_thresh = targ_thresh;
			//System.out.println("GOT BALL");
			double angle = aiWorldState.getRobot().getAngle();
			if (aiWorldState.getMyGoalLeft()) {
				if (angle > 90 || angle < -90) {
					//System.out.println("REVERSE angle = " + angle);
					return new Command(-Robot.MAX_SPEED_CM_S, 0, false);
				}
			} else {
				if (Math.abs(angle) <= 90) {
					//System.out.println("FORWARD");
					return new Command(-Robot.MAX_SPEED_CM_S, 0, false);
				}
			}
			chasing_target = true;
			return new Command(Robot.MAX_SPEED_CM_S,0,true);
		} else {
			return chaseBall();
		}
	}

	@Override
	protected Command defendGoal() throws IOException {
		
		if (Math.abs(Utilities.getTurningAngle(aiWorldState.getRobot(), new Vector2D(aiWorldState.getBallCoords()))) < 15 &&
				aiWorldState.getDistanceToBall() < 40) {
			if (Utilities.canWeAttack(aiWorldState))
				return gotBall();
			return new Command(Robot.MAX_SPEED_CM_S, 0, false);
		}
		
		Point2D.Double int1;
		Point2D.Double int2;
		
		if (aiWorldState.getMyGoalLeft()){
			
			double behind_ball =aiWorldState.getBallCoords().x - DEFEND_BALL_THRESHOLD;
			double rob_ang = Utilities.normaliseAngleToDegrees(aiWorldState.getRobot().getAngle());
			double x = Math.max(aiWorldState.getRobot().getCoords().x, 30);
			if (behind_ball < x)
				x = behind_ball;
			else if (Math.abs(rob_ang) > 135)
				x = (x + aiWorldState.getBallCoords().x)/2d;
			
			if (x < 50)
				x = 50;
			
			int1 = new Point2D.Double(x, Robot.LENGTH_CM);
			int2 = new Point2D.Double(x, WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
		} else {
			
			double behind_ball =aiWorldState.getBallCoords().x + DEFEND_BALL_THRESHOLD;
			double rob_ang = Utilities.normaliseAngleToDegrees(aiWorldState.getRobot().getAngle());
			double x = Math.min(aiWorldState.getRobot().getCoords().x, WorldState.PITCH_WIDTH_CM - 30);
			if (behind_ball > x)
				x = behind_ball;
			else if (Math.abs(rob_ang) < 45)
				x = (x + aiWorldState.getBallCoords().x)/2d;
			
			if (x > WorldState.PITCH_WIDTH_CM-50)
				x = WorldState.PITCH_WIDTH_CM-50;
			
			int1 = new Point2D.Double(x, Robot.LENGTH_CM);
			int2 = new Point2D.Double(x, WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
		}

		Point2D.Double intercept= GeomUtils.getLineIntersection(aiWorldState.getBallCoords(), aiWorldState.getEnemyRobot().getCoords(), int1, int2);

		boolean can_we_go_to_intercept = false;
		Command com = new Command(0, 0, false);
		double dist = 10000;

		Point2D.Double point = new Point2D.Double(10000, 10000);
		Point2D.Double point2 = new Point2D.Double(10000, 10000);
		Point2D.Double point3 = new Point2D.Double(10000, 10000);

		if (intercept != null){
			point = intercept;

			can_we_go_to_intercept = (intercept.y > Robot.LENGTH_CM)  && (intercept.y < WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			dist = Vector2D.subtract(new Vector2D(aiWorldState.getRobot().getCoords()), new Vector2D(point)).getLength();
		}

		if (aiWorldState.getMyGoalLeft()){
			point2= new Point2D.Double(aiWorldState.getRobot().getCoords().x, WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			point3= new Point2D.Double(aiWorldState.getRobot().getCoords().x, Robot.LENGTH_CM);
		} else {
			point2= new Point2D.Double(aiWorldState.getRobot().getCoords().x , WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			point3= new Point2D.Double(aiWorldState.getRobot().getCoords().x , Robot.LENGTH_CM);
		}

		double dist2 = Vector2D.subtract(new Vector2D(aiWorldState.getRobot().getCoords()), new Vector2D(point2)).getLength();
		double dist3 = Vector2D.subtract(new Vector2D(aiWorldState.getRobot().getCoords()), new Vector2D(point3)).getLength();

		



		if(aiWorldState.getEnemyRobot().getAngle()>-90 && aiWorldState.getEnemyRobot().getAngle()<90)
			return attack(Utilities.AttackMode.Full, false);

		if(can_we_go_to_intercept)	{			
			if (dist > 5)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point), false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()<-90 && aiWorldState.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point2), false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist2, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()>90 && aiWorldState.getEnemyRobot().getAngle()<180 ){
			if (dist3 > 10)
				com = getWaypointCommand(getNextWaypoint(new Vector2D(point3), false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist3, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}
		
		Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
		return null;
	}

	/**
	 * This is called by the AIMaster every time the state is changed
	 */
	protected void changedState() {
		point_off = DEFAULT_POINT_OFF;
		targ_thresh = DEFAULT_TARG_THRESH;
		Painter.point_off = point_off;
		Painter.targ_thresh = targ_thresh;
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

		Point2D.Double interceptBall= GeomUtils.getLineIntersection(aiWorldState.getEnemyRobot().getFrontCenter(), aiWorldState.getEnemyRobot().getCoords(), aiWorldState.getRobot().getCoords(), aiWorldState.getRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + aiWorldState.getRobot().getCoords().y);
		double dist = Vector2D.subtract(new Vector2D(aiWorldState.getRobot().getCoords()), new Vector2D(interceptBall)).getLength();
		Command com =new Command(0, 0, false);
		if (!interceptBall.equals(null)){

			if((interceptBall.y < aiWorldState.getMyGoal().getBottom().y)  && (interceptBall.y > aiWorldState.getMyGoal().getTop().y)){
				if ((interceptBall.y > aiWorldState.getRobot().getCoords().y)  ){
					short forward_speed = (short) -20;
					com.drivingSpeed=forward_speed;
					reduceSpeed(com, dist, 20, 0);
					return com;
				} else if((interceptBall.y < aiWorldState.getRobot().getCoords().y)) {
					short forward_speed = (short) 20;
					com.drivingSpeed=forward_speed;
					reduceSpeed(com, dist, 20, 0);
					return  com;
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
		return attack(Utilities.AttackMode.WallsOnly, false);

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
		Vector2D targetLocal = Utilities.getLocalVector(aiWorldState.getRobot(), target);

		// Which objects to consider as obstacles?
		int obstacleFlags = 0;
		obstacleFlags |= (aiWorldState.getMyTeamBlue()
				? Utilities.YELLOW_IS_OBSTACLE_FLAG
						: Utilities.BLUE_IS_OBSTACLE_FLAG);
		if (ballIsObstacle) {
			obstacleFlags |= Utilities.BALL_IS_OBSTACLE_FLAG;
		}

		// Check if the target point is directly visible.
		Vector2D ownCoords = new Vector2D(aiWorldState.getRobot().getCoords());
		if (Utilities.isDirectPathClear(aiWorldState, ownCoords, target, obstacleFlags)) {
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
				curAngle = Utilities.normaliseAngleToDegrees(curAngle);

				Vector2D rayDir = Vector2D.rotateVector(new Vector2D(1, 0), curAngle);
				Vector2D rayEndLocal = Vector2D.multiply(rayDir, destPointDist);
				Vector2D rayEnd = Utilities.getGlobalVector(aiWorldState.getRobot(), rayEndLocal);

				if (Utilities.isDirectPathClear(aiWorldState, ownCoords, rayEnd, obstacleFlags)) {
					double angleDiff = Utilities.normaliseAngleToDegrees(curAngle - targetLocal.getDirection());
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
	private Command getWaypointCommand(Waypoint waypoint, boolean mustFaceTarget, double speed_multiplier, double max_turn_speed) {
		Command comm = new Command(0.0, 0.0, false);

		comm.turningSpeed = waypoint.getTurningAngle();

		// Ball is behind.
		if (Math.abs(waypoint.getTurningAngle()) > 90) {
			comm.drivingSpeed = -Robot.MAX_SPEED_CM_S;
			if (REVERSE_DRIVING_ENABLED && !mustFaceTarget) {
				comm.turningSpeed = Utilities.normaliseAngleToDegrees(waypoint.getTurningAngle() - 180);
			}		
			// Ball is in front.
		} else {
			comm.drivingSpeed = Robot.MAX_SPEED_CM_S;
		}

		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? THRESH_BACK_LOW : THRESH_BACK_HIGH);
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		if ((Math.abs(waypoint.getTurningAngle()) < 45) && (waypoint.getDistance() < targ_thresh)) {
			point_off *= 0.5;
			targ_thresh = point_off*0.5;
		}
		if (point_off < Utilities.KICKABLE_BALL_DIST) {
			point_off = Utilities.KICKABLE_BALL_DIST;
		}

		Vector2D ball = new Vector2D(aiWorldState.getBallCoords());
		if (aiWorldState.getMyGoalLeft()) {
			if (ball.getX() < aiWorldState.getRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
				targ_thresh = DEFAULT_TARG_THRESH;
			}
		} else {
			if (ball.getX() > aiWorldState.getRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
				targ_thresh = DEFAULT_TARG_THRESH;
			}
		}

		reduceSpeed(comm, waypoint.getDistance(), 30, Robot.MAX_SPEED_CM_S/2);

		Painter.point_off = point_off;
		Painter.targ_thresh = targ_thresh;

		comm.turningSpeed *= TURN_SPD_MULTIPLIER;
		comm.drivingSpeed *= speed_multiplier;
		comm.acceleration = 100;

		normalizeSpeed(comm, speed_multiplier, max_turn_speed);

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
		double frontCollDist = Utilities.getSector(aiWorldState, aiWorldState.getMyTeamBlue(),
				-10, 10, 20, ballIsObstacle).getLength();
		double backCollDist = Utilities.getSector(aiWorldState, aiWorldState.getMyTeamBlue(),
				170, -170, 20, ballIsObstacle).getLength();

		// Collision in front.
		if (frontCollDist < threshold) {
			if (command.drivingSpeed >= 0) {
				double speed_coeff = (frontCollDist / threshold) - 1;
				if (speed_coeff > 0) {
					speed_coeff = 0;
				}
				if (speed_coeff < -1) {
					speed_coeff = -1;
				}
				command.drivingSpeed *= speed_coeff;
			}

			// Collision in the back.
		} else if (backCollDist < threshold) {
			if (command.drivingSpeed <= 0) {
				double speed_coeff = (backCollDist / threshold) - 1;
				if (speed_coeff > 0) {
					speed_coeff = 0;
				}
				if (speed_coeff < -1) {
					speed_coeff = -1;
				}
				command.drivingSpeed *= speed_coeff;
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
		Vector2D nearestColl = Utilities.getNearestCollisionPoint(aiWorldState, aiWorldState.getMyTeamBlue(), aiWorldState.getRobot().getCoords());

		Vector2D frontLeft = Utilities.getLocalVector(aiWorldState.getRobot(), Vector2D.add(new Vector2D(aiWorldState.getRobot().getFrontLeft()), nearestColl));
		Vector2D frontRight = Utilities.getLocalVector(aiWorldState.getRobot(), Vector2D.add(new Vector2D(aiWorldState.getRobot().getFrontRight()), nearestColl));
		Vector2D backLeft = Utilities.getLocalVector(aiWorldState.getRobot(), Vector2D.add(new Vector2D(aiWorldState.getRobot().getBackLeft()), nearestColl));
		Vector2D backRight = Utilities.getLocalVector(aiWorldState.getRobot(), Vector2D.add(new Vector2D(aiWorldState.getRobot().getBackRight()), nearestColl));

		boolean haveFrontLeftColl = (frontLeft.getLength() <= threshold);
		boolean haveFrontRightColl = (frontRight.getLength() <= threshold);
		boolean haveBackLeftColl = (backLeft.getLength() <= threshold);
		boolean haveBackRightColl = (backRight.getLength() <= threshold);

		boolean haveFrontColl = haveFrontLeftColl || haveFrontRightColl;
		boolean haveAnyColl = haveFrontLeftColl || haveFrontRightColl || haveBackLeftColl || haveBackRightColl;

		if (haveAnyColl) {
			command.drivingSpeed = (haveFrontColl ? -Robot.MAX_SPEED_CM_S : Robot.MAX_SPEED_CM_S);
			command.turningSpeed += (haveFrontColl ? -10 : 10);
		}
	}


	/**
	 * Get the distance from our robot to some point in the world.
	 * 
	 * @param point Point in global coordinates to find distance to.
	 * @return Distance from our robot to the given point in the world.
	 */
	private double distanceTo(Vector2D point) {
		return Vector2D.subtract(new Vector2D(aiWorldState.getRobot().getCoords()), point).getLength();
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

		if (command.drivingSpeed < 0) {
			baseSpeed = -baseSpeed;
		}

		if (Math.abs(command.drivingSpeed) < Math.abs(baseSpeed)) {
			command.drivingSpeed = baseSpeed;
		} else {
			double coeff = distance / threshold;
			command.drivingSpeed = baseSpeed + coeff * (command.drivingSpeed - baseSpeed);
		}
	}

	/**
	 * Reduce the forward speed based on the robot's turning speed.
	 * 
	 * @param comm The command to be modified.
	 */
	public void normalizeSpeed(Command comm, double speed_multiplier, double max_turn_spd) {
		if (Math.abs(comm.turningSpeed) > (speed_multiplier*180-100)) {
			comm.drivingSpeed = 0;
		} else {		
			double rat = Math.abs(comm.turningSpeed) / (speed_multiplier*180-100);
			comm.drivingSpeed *= 1 - rat;
		}
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

		comm.turningSpeed = waypoint.getTurningAngle();

		// Ball is behind.
		if (Math.abs(comm.turningSpeed) > 90) {
			comm.drivingSpeed = -Robot.MAX_SPEED_CM_S;
			if (REVERSE_DRIVING_ENABLED && !mustFaceTarget) {
				comm.turningSpeed = Utilities.normaliseAngleToDegrees(comm.turningSpeed - 180);
			}		
			// Ball is in front.
		} else {
			comm.drivingSpeed = Robot.MAX_SPEED_CM_S;
		}

		// if we get within too close (within coll_start) of an obstacle
		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? THRESH_BACK_LOW : THRESH_BACK_HIGH);

		// check if either of the corners are in collision
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		return comm;
	}

}

