package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import sdp.AI.pathfinding.FullPathfinder;
import sdp.AI.pathfinding.HeuristicPathfinder;
import sdp.AI.pathfinding.Pathfinder;
import sdp.AI.pathfinding.Waypoint;
import sdp.common.Painter;
import sdp.common.Utilities;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


public class AIVisualServoing extends BaseAI {
	
	/** Attack modes for optimal point calculations. */
	private enum AttackMode { DirectOnly, WallsOnly, Full }


	/** Whether the robot is allowed to drive in reverse. */
	private static final boolean REVERSE_DRIVING_ENABLED = true;
	/** Whether to use the heuristic pathfinder. */
	private static final boolean USE_HEURISTIC_PATHFINDER = false;
	/** Whether the robot should attempt to use wall kicks. */
	private static final boolean WALL_KICKS_ENABLED = true;
	
	/** Distance at which the robot will begin slowing down. */
	private static final double DECELERATION_DISTANCE = 30;

	/** The multiplier of the final driving speed. */
	private static final double DRIVING_SPEED_MULTIPLIER = 1;
	/** What fraction of forward speed the robot will lose when turning. */
	private static final double FORWARD_LOSS_MULTIPLIER = 0.7;
	/** Turning angle threshold for stop turns. */
	private static final double STOP_TURN_THRESHOLD = 120;
	/** The multiplier of the final turning speed. */
	private static final double TURNING_SPEED_MULTIPLIER = 1.8;
	
	/** The number of targets, when considering a direct attack. */
	private static final int DIRECT_ATTACK_TARGET_COUNT = 9;
	
	/** How close our robot should be to the ball before it attempts to kick it. */
	private static final double OWN_BALL_KICK_DIST = 6;
	/** How close enemy robot should be to the ball before we think it will kick it. */
	private static final double ENEMY_BALL_KICK_DIST = Robot.LENGTH_CM;
	
	/** Distance threshold for ball-robot direction ray tests. */
	private static final double BALL_TO_DIRECTION_PROXIMITY_THRESHOLD = Robot.WIDTH_CM / 2;
	

	// corner thresholds
	private static final int THRESH_CORN_HIGH = 5;
	private static final int THRESH_CORN_LOW = 2;

	// back away threshold
	private static final int THRESH_BACK_HIGH = 25;
	private static final int THRESH_BACK_LOW = 15;

	public static final double DEFAULT_POINT_OFF = 2*Robot.LENGTH_CM;
	private double point_off = DEFAULT_POINT_OFF;
	public static final double DEFAULT_TARG_THRESH = 30;
	private double targ_thresh = DEFAULT_TARG_THRESH;

	private static final double DEFEND_BALL_THRESHOLD = 10;


	private Vector2D target = null;
	
	/** AI's heuristic pathfinder. */
	private Pathfinder pathfinder;
	
	
	/**
	 * Create a new visual servoing AI instance.
	 */
	public AIVisualServoing() {
		if (USE_HEURISTIC_PATHFINDER) {
			pathfinder = new HeuristicPathfinder();
		} else {
			pathfinder = new FullPathfinder();
		}
	}


	/**
	 * @see sdp.AI.BaseAI#chaseBall()
	 */
	@Override
	protected Command chaseBall() throws IOException {
		return attack(AttackMode.Full);
	}


	/**
	 * Get a command to attack the opponents.
	 * 
	 * @param mode Robot's attack mode. See {@link AttackMode}.
	 * @return The next command the robot should execute.
	 * @throws IOException 
	 */
	protected Command attack(AttackMode mode) throws IOException {
		// Are we ready to score?
		if (canWeAttack(aiWorldState)) {
			return gotBall();
		}
		
		// Get the point to drive towards.
		target = new Vector2D(aiWorldState.getBallCoords());
		
		Point2D.Double optimalPoint = null;
		for (int t = 0; t < 20; t++) {
			optimalPoint = getOptimalAttackPoint(aiWorldState, point_off, mode);
			
			if (optimalPoint == null) {
				point_off *= 0.8;
			} else {
				break;
			}
		}

		boolean ballIsObstacle = false;
		if (optimalPoint != null) {
			target = new Vector2D(optimalPoint);
			ballIsObstacle = true;
		}

		// Generate command to drive towards the target point.
		boolean mustFaceTarget = !Utilities.areDoublesEqual(point_off, DEFAULT_TARG_THRESH);

		Waypoint waypoint = pathfinder.getWaypointForOurRobot(aiWorldState, target, true);
		return getWaypointCommand(waypoint, mustFaceTarget);
	}

	@Override
	protected synchronized Command gotBall() throws IOException {
		if (canWeAttack(aiWorldState)) {
			point_off = DEFAULT_POINT_OFF;
			targ_thresh = DEFAULT_TARG_THRESH;
			Painter.point_off = point_off;
			Painter.targ_thresh = targ_thresh;
			//System.out.println("GOT BALL");
			double angle = aiWorldState.getOwnRobot().getAngle();
			if (aiWorldState.isOwnGoalLeft()) {
				if (angle > 90 || angle < -90) {
					//System.out.println("REVERSE angle = " + angle);
					return new Command(-Robot.MAX_DRIVING_SPEED, 0, false);
				}
			} else {
				if (Math.abs(angle) <= 90) {
					//System.out.println("FORWARD");
					return new Command(-Robot.MAX_DRIVING_SPEED, 0, false);
				}
			}
			return new Command(Robot.MAX_DRIVING_SPEED,0,true);
		} else {
			return chaseBall();
		}
	}

	@Override
	protected Command defendGoal() throws IOException {
		boolean useMilestone = false;
		
		if (!useMilestone) {
			Point2D.Double target = getOptimalDefencePoint(aiWorldState);
			Waypoint waypoint = pathfinder.getWaypointForOurRobot(aiWorldState, target, true);

			return getWaypointCommand(waypoint, false);
		}
		
		if (Math.abs(Robot.getTurningAngle(aiWorldState.getOwnRobot(), new Vector2D(aiWorldState.getBallCoords()))) < 15 &&
				aiWorldState.getDistanceToBall() < 40) {
			if (canWeAttack(aiWorldState))
				return gotBall();
			return new Command(Robot.MAX_DRIVING_SPEED, 0, false);
		}
		
		Point2D.Double int1;
		Point2D.Double int2;
		
		if (aiWorldState.isOwnGoalLeft()){
			
			double behind_ball =aiWorldState.getBallCoords().x - DEFEND_BALL_THRESHOLD;
			double rob_ang = GeomUtils.normaliseAngle(aiWorldState.getOwnRobot().getAngle());
			double x = Math.max(aiWorldState.getOwnRobot().getCoords().x, 30);
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
			double rob_ang = GeomUtils.normaliseAngle(aiWorldState.getOwnRobot().getAngle());
			double x = Math.min(aiWorldState.getOwnRobot().getCoords().x, WorldState.PITCH_WIDTH_CM - 30);

			if (behind_ball > x)
				x = behind_ball;
			else if (Math.abs(rob_ang) < 45)
				x = (x + aiWorldState.getBallCoords().x)/2d;
			
			if (x > WorldState.PITCH_WIDTH_CM-50)
				x = WorldState.PITCH_WIDTH_CM-50;
			
			int1 = new Point2D.Double(x, Robot.LENGTH_CM);
			int2 = new Point2D.Double(x, WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
		}

		Point2D.Double intercept= GeomUtils.getLineLineIntersection(aiWorldState.getBallCoords(), aiWorldState.getEnemyRobot().getCoords(), int1, int2);

		boolean can_we_go_to_intercept = false;
		Command com = new Command(0, 0, false);
		double dist = 10000;

		Point2D.Double point = new Point2D.Double(10000, 10000);
		Point2D.Double point2 = new Point2D.Double(10000, 10000);
		Point2D.Double point3 = new Point2D.Double(10000, 10000);

		if (intercept != null){
			point = intercept;

			can_we_go_to_intercept = (intercept.y > Robot.LENGTH_CM)  && (intercept.y < WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			dist = Vector2D.subtract(new Vector2D(aiWorldState.getOwnRobot().getCoords()), new Vector2D(point)).getLength();
		}

		if (aiWorldState.isOwnGoalLeft()){
			point2= new Point2D.Double(aiWorldState.getOwnRobot().getCoords().x, WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			point3= new Point2D.Double(aiWorldState.getOwnRobot().getCoords().x, Robot.LENGTH_CM);
		} else {
			point2= new Point2D.Double(aiWorldState.getOwnRobot().getCoords().x , WorldState.PITCH_HEIGHT_CM - Robot.LENGTH_CM);
			point3= new Point2D.Double(aiWorldState.getOwnRobot().getCoords().x , Robot.LENGTH_CM);
		}

		double dist2 = Vector2D.subtract(new Vector2D(aiWorldState.getOwnRobot().getCoords()), new Vector2D(point2)).getLength();
		double dist3 = Vector2D.subtract(new Vector2D(aiWorldState.getOwnRobot().getCoords()), new Vector2D(point3)).getLength();

		



		if(aiWorldState.getEnemyRobot().getAngle()>-90 && aiWorldState.getEnemyRobot().getAngle()<90)
			return attack(AttackMode.Full);

		if(can_we_go_to_intercept)	{			
			if (dist > 5)
				com = getWaypointCommand(pathfinder.getWaypointForOurRobot(aiWorldState, point, false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()<-90 && aiWorldState.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = getWaypointCommand(pathfinder.getWaypointForOurRobot(aiWorldState, point2, false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist2, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()>90 && aiWorldState.getEnemyRobot().getAngle()<180 ){
			if (dist3 > 10)
				com = getWaypointCommand(pathfinder.getWaypointForOurRobot(aiWorldState, point3, false), false, 1, 20);
			
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

		Point2D.Double interceptBall= GeomUtils.getLineLineIntersection(aiWorldState.getEnemyRobot().getFrontCenter(), aiWorldState.getEnemyRobot().getCoords(), aiWorldState.getOwnRobot().getCoords(), aiWorldState.getOwnRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + aiWorldState.getOwnRobot().getCoords().y);
		double dist = Vector2D.subtract(new Vector2D(aiWorldState.getOwnRobot().getCoords()), new Vector2D(interceptBall)).getLength();
		Command com =new Command(0, 0, false);
		if (!interceptBall.equals(null)){

			if((interceptBall.y < aiWorldState.getOwnGoal().getBottom().y)  && (interceptBall.y > aiWorldState.getOwnGoal().getTop().y)){
				if ((interceptBall.y > aiWorldState.getOwnRobot().getCoords().y)  ){
					short forward_speed = (short) -20;
					com.drivingSpeed=forward_speed;
					reduceSpeed(com, dist, 20, 0);
					return com;
				} else if((interceptBall.y < aiWorldState.getOwnRobot().getCoords().y)) {
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
		return attack(AttackMode.WallsOnly);

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
		return getWaypointCommand(waypoint, mustFaceTarget, DRIVING_SPEED_MULTIPLIER,
				STOP_TURN_THRESHOLD);
	}

	/**
	 * Generate a command that will get the robot closer to the specified
	 * waypoint. This version allows to specify custom driving speed multiplier
	 * and maximum turning speed.
	 * 
	 * @param waypoint Point to move towards.
	 * @param mustFaceTarget Whether the robot is required to face the point
	 * 		at all times.
	 * @param drivingSpeedMult Driving speed multiplier.
	 * @param stopTurnThreshold Maximum turning speed of the robot.
	 * @return A command that will get the robot closer towards the given
	 * 		waypoint.
	 */
	private Command getWaypointCommand(Waypoint waypoint, boolean mustFaceTarget,
			double drivingSpeedMult, double stopTurnThreshold) {
		Command comm = new Command(0.0, 0.0, false);

		comm.turningSpeed = waypoint.getTurningAngle();

		// Ball is behind.
		if (Math.abs(waypoint.getTurningAngle()) > 90) {
			comm.drivingSpeed = -Robot.MAX_DRIVING_SPEED;
			if (REVERSE_DRIVING_ENABLED && !mustFaceTarget) {
				comm.turningSpeed = GeomUtils.normaliseAngle(waypoint.getTurningAngle() - 180);
			}		
		// Ball is in front.
		} else {
			comm.drivingSpeed = Robot.MAX_DRIVING_SPEED;
		}

		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? THRESH_BACK_LOW : THRESH_BACK_HIGH);
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		if ((Math.abs(waypoint.getTurningAngle()) < 45)
				&& (waypoint.getDistance() < targ_thresh)) {
			point_off *= 0.5;
			targ_thresh = point_off*0.5;
		}
		if (point_off < OWN_BALL_KICK_DIST) {
			point_off = OWN_BALL_KICK_DIST;
		}

		Vector2D ball = new Vector2D(aiWorldState.getBallCoords());
		if (aiWorldState.isOwnGoalLeft()) {
			if (ball.getX() < aiWorldState.getOwnRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
				targ_thresh = DEFAULT_TARG_THRESH;
			}
		} else {
			if (ball.getX() > aiWorldState.getOwnRobot().getCoords().getX()) {
				point_off = DEFAULT_POINT_OFF;
				targ_thresh = DEFAULT_TARG_THRESH;
			}
		}

		reduceSpeed(comm, waypoint.getCostToDest(), DECELERATION_DISTANCE,
				Robot.MAX_DRIVING_SPEED / 2);

		Painter.point_off = point_off;
		Painter.targ_thresh = targ_thresh;

		comm.turningSpeed *= TURNING_SPEED_MULTIPLIER;
		comm.drivingSpeed *= drivingSpeedMult;
		comm.acceleration = 100;

		normalizeSpeed(comm, stopTurnThreshold);

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
		double frontCollDist = Utilities.getSector(aiWorldState, aiWorldState.isOwnTeamBlue(),
				-10, 10, 20, ballIsObstacle).getLength();
		double backCollDist = Utilities.getSector(aiWorldState, aiWorldState.isOwnTeamBlue(),
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
		Vector2D nearestColl = WorldState.getNearestCollisionPoint(aiWorldState, aiWorldState.isOwnTeamBlue(), aiWorldState.getOwnRobot().getCoords());

		Vector2D frontLeft = Robot.getLocalVector(aiWorldState.getOwnRobot(), Vector2D.add(new Vector2D(aiWorldState.getOwnRobot().getFrontLeft()), nearestColl));
		Vector2D frontRight = Robot.getLocalVector(aiWorldState.getOwnRobot(), Vector2D.add(new Vector2D(aiWorldState.getOwnRobot().getFrontRight()), nearestColl));
		Vector2D backLeft = Robot.getLocalVector(aiWorldState.getOwnRobot(), Vector2D.add(new Vector2D(aiWorldState.getOwnRobot().getBackLeft()), nearestColl));
		Vector2D backRight = Robot.getLocalVector(aiWorldState.getOwnRobot(), Vector2D.add(new Vector2D(aiWorldState.getOwnRobot().getBackRight()), nearestColl));

		boolean haveFrontLeftColl = (frontLeft.getLength() <= threshold);
		boolean haveFrontRightColl = (frontRight.getLength() <= threshold);
		boolean haveBackLeftColl = (backLeft.getLength() <= threshold);
		boolean haveBackRightColl = (backRight.getLength() <= threshold);

		boolean haveFrontColl = haveFrontLeftColl || haveFrontRightColl;
		boolean haveAnyColl = haveFrontLeftColl || haveFrontRightColl || haveBackLeftColl || haveBackRightColl;

		if (haveAnyColl) {
			command.drivingSpeed = (haveFrontColl ? -Robot.MAX_DRIVING_SPEED : Robot.MAX_DRIVING_SPEED);
			command.turningSpeed += (haveFrontColl ? -10 : 10);
		}
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
	private void reduceSpeed(Command command, double distance, double threshold,
			double baseSpeed) {
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
	 * @param command The command to be modified.
	 * @param stopTurnThreshold Threshold at which the robot will turn without
	 * 		driving forward.
	 */
	private void normalizeSpeed(Command command, double stopTurnThreshold) {
		if (Math.abs(command.turningSpeed) > stopTurnThreshold) {
			command.drivingSpeed = 0;
		} else {		
			double lossFactor = Math.abs(command.turningSpeed) / stopTurnThreshold;
			lossFactor *= FORWARD_LOSS_MULTIPLIER;
			lossFactor = Utilities.restrictValueToInterval(lossFactor, 0.0, 1.0).doubleValue();
			command.drivingSpeed *= 1.0 - lossFactor;
		}
	}


	/**
	 * Calculate the optimal attack point in the given world state. The robot
	 * should seek to move to the said point in order to make an attacking move.
	 * 
	 * TODO: Move all logic out of deprecated functions here.
	 * 
	 * @param worldState Current world state.
	 * @param distToBall Desired distance to the ball.
	 * @param mode Robot's attack mode.
	 * @return Optimal attack point.
	 */
	private Point2D.Double getOptimalAttackPoint(AIWorldState worldState,
			double distToBall, AttackMode mode) {
		ArrayList<Point2D.Double> attackTargets = new ArrayList<Point2D.Double>();
		
		// Add direct attack targets, if any.
		if ((mode == AttackMode.Full) || (mode == AttackMode.DirectOnly)) {
			double goalHeight = worldState.getEnemyGoal().getBottom().y - worldState.getEnemyGoal().getTop().y;
			
			for (int i = 0; i < DIRECT_ATTACK_TARGET_COUNT; ++i) {
				double ratio = ((double) i) / (DIRECT_ATTACK_TARGET_COUNT - 1);
				double heightOffset = goalHeight * ratio;
				
				attackTargets.add(new Point2D.Double(worldState.getEnemyGoal().getTop().x,
						worldState.getEnemyGoal().getTop().y + heightOffset));
			}
			attackTargets.add(worldState.getEnemyGoal().getCentre());
		}
		
		// Add wall attack targets, if any.
		if ((WALL_KICKS_ENABLED && (mode == AttackMode.Full))
				|| (mode == AttackMode.WallsOnly)) {
			Point2D.Double enemyGoalCentre = worldState.getEnemyGoal().getCentre();
			attackTargets.add(new Point2D.Double(enemyGoalCentre.x,
					enemyGoalCentre.y - WorldState.PITCH_HEIGHT_CM));
			attackTargets.add(new Point2D.Double(enemyGoalCentre.x,
					enemyGoalCentre.y + WorldState.PITCH_HEIGHT_CM));
		}
		
		// Filter invisible attack targets.
		Iterator<Point2D.Double> it = attackTargets.iterator();
		while (it.hasNext()) {
			Point2D.Double curTarget = it.next();
			
			Robot robotImage = worldState.getEnemyRobot();
			if (curTarget.y < 0) {
				robotImage = worldState.getEnemyRobot().getTopImage();
			} else if (curTarget.y > WorldState.PITCH_HEIGHT_CM) {
				robotImage = worldState.getEnemyRobot().getBottomImage();
			}
			
			if (!Robot.lineIntersectsRobot(curTarget, worldState.getBallCoords(), robotImage)) { 
				it.remove();
			} else if (!WorldState.isPointInPitch(curTarget)) {
				it.remove();
			}
		}
		
		// Find the optimal attack point.
		double minOptimalDist = Double.MAX_VALUE;
		Point2D.Double bestPoint = null;
		
		for (Point2D.Double curTarget : attackTargets) {
			Vector2D ballToTarget = Vector2D.subtract(new Vector2D(curTarget),
					new Vector2D(worldState.getBallCoords()));
			Vector2D targetOffset = Vector2D.changeLength(ballToTarget, -distToBall);
			Point2D.Double curOptimal = GeomUtils.addPoints(worldState.getBallCoords(), targetOffset);
			double distToOptimal = GeomUtils.pointDistance(worldState.getOwnRobot().getCoords(), curOptimal);

			if (WorldState.isPointInPaddedPitch(curOptimal, Robot.LENGTH_CM / 2)
					&& !Robot.isPointAroundRobot(curOptimal, worldState.getEnemyRobot())
					&& Robot.lineIntersectsRobot(curTarget, worldState.getBallCoords(), worldState.getEnemyRobot())
					&& (distToOptimal < minOptimalDist)) {
				bestPoint = curOptimal;
				minOptimalDist = distToOptimal;
			}
		}
	
		return bestPoint;
	}
	
	/**
	 * Calculate the optimal defence point in the given world state. The robot
	 * should seek to move to the said point in order to make a defensive move.
	 * 
	 * @param worldState Current world state.
	 * @return Optimal defence point.
	 */
	private Point2D.Double getOptimalDefencePoint(AIWorldState worldState) {
		Vector2D ballVec = new Vector2D(worldState.getBallCoords());
		
		Vector2D ballGoalVec = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getCentre()), ballVec);
		Vector2D ballOffset = Vector2D.changeLength(ballGoalVec, Robot.LENGTH_CM);
		Vector2D ballPoint = Vector2D.add(ballVec, ballOffset);
		
		Vector2D closestPoint = new Vector2D(GeomUtils.getClosestPointToLine(worldState.getOwnRobot().getCoords(),
				new Vector2D(worldState.getOwnGoal().getCentre()), new Vector2D(worldState.getBallCoords())));
		
		double distToClosest = GeomUtils.pointDistance(worldState.getOwnRobot().getCoords(), closestPoint);
		
		Vector2D optimalPoint = ballPoint;
		
		double goalDir = ballGoalVec.getDirection();
		double closePtDir = Vector2D.subtract(closestPoint, ballPoint).getDirection();
		
		if (Utilities.areDoublesEqual(goalDir, closePtDir)) {
			optimalPoint = closestPoint;
			
			if (distToClosest < Robot.LENGTH_CM / 2) {
				optimalPoint = ballPoint;
			}
		}
		
		return optimalPoint;
	}


	/**
	 * Checks whether our robot can directly attack the enemy goal.
	 * 
	 * TODO: Decouple from AI Master, make private.
	 * 
	 * @param worldState Current world state.
	 * @return Whether the gates can be attacked.
	 */
	public static boolean canWeAttack(AIWorldState worldState) {
		Vector2D localBall = Robot.getLocalVector(worldState.getOwnRobot(),
				new Vector2D(worldState.getBallCoords()));
		
		if (!((localBall.x < (Robot.LENGTH_CM / 2 + OWN_BALL_KICK_DIST))
				&& (localBall.x > 0)
				&& (localBall.y < (Robot.WIDTH_CM / 2))
				&& (localBall.y > (-Robot.WIDTH_CM / 2)))) {
			return false;
		}
		
		if (!Robot.lineIntersectsRobot(worldState.getOwnRobot().getCoords(),
				worldState.getOwnRobot().getFrontCenter(), worldState.getEnemyRobot())) {
			return false;
		}
		
		System.out.println("AI thinks it can kick.");
		
		return true;
	}
	
	/**
	 * Checks whether enemy robot can directly attack our goal.
	 * 
	 * TODO: Decouple from AIMaster, make private.
	 * 
	 * @param worldState Current world state.
	 * @return Whether the gates can be attacked.
	 */
	public static boolean canEnemyAttack(AIWorldState worldState) {
		// Check that the ball is between our goal and the enemy.
		if (worldState.isOwnGoalLeft()) {
			if (worldState.getEnemyRobot().getCoords().x < worldState.getBallCoords().x) {
				return false;
			}
		} else {
			if (worldState.getBallCoords().x < worldState.getEnemyRobot().getCoords().x) {
				return false;
			}
		}
		
		// Check that the enemy is pointing at us.
		double curEnemyAngle = worldState.getEnemyRobot().getAngle();
		double upperEnemyAngle = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getTop()),
				new Vector2D(worldState.getEnemyRobot().getCoords())).getDirection();
		double lowerEnemyAngle = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getBottom()),
				new Vector2D(worldState.getEnemyRobot().getCoords())).getDirection();
		
		double curToLower = GeomUtils.getAngleDifference(lowerEnemyAngle, curEnemyAngle);
		double curToUpper = GeomUtils.getAngleDifference(upperEnemyAngle, curEnemyAngle);
		double lowerToUpper = GeomUtils.getAngleDifference(lowerEnemyAngle, upperEnemyAngle);
		
		if (!Utilities.areDoublesEqual(curToLower + curToUpper, lowerToUpper)) {
			return false;
		}
		
		// Check that the enemy is in line with the ball.
		Point2D.Double ballRayPoint = GeomUtils.getClosestPointToLine(worldState.getBallCoords(), 
				worldState.getEnemyRobot().getCoords(), worldState.getEnemyRobot().getFrontCenter());
		double ballDistToRay = GeomUtils.pointDistance(worldState.getBallCoords(), ballRayPoint);
		
		if (ballDistToRay > BALL_TO_DIRECTION_PROXIMITY_THRESHOLD) {
			return false;
		}
		
		// Check that the enemy is within shooting distance.
		double enemyToBallDist = GeomUtils.pointDistance(worldState.getBallCoords(),
				worldState.getEnemyRobot().getCoords());
		
		if (enemyToBallDist > (ENEMY_BALL_KICK_DIST + Robot.LENGTH_CM / 2)) {
			return false;
		}
		
		return true;
	}
}