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


/**
 * Visual servoing AI implementation.
 */
public class AIVisualServoing extends BaseAI {
	
	/** Attack modes for optimal point calculations. */
	protected enum AttackMode { DirectOnly, WallsOnly, Full }


	/** Whether the robot is required to face the ball before kicking. */
	private static final boolean REQUIRE_FACE_BALL_TO_KICK = true;
	/** Whether the robot is allowed to drive in reverse. */
	private static final boolean REVERSE_DRIVING_ENABLED = true;
	/** Whether wall handling logic should be used. */
	private static final boolean SPECIAL_WALL_LOGIC_ENABLED = true;
	/** Whether to use the heuristic pathfinder. */
	private static final boolean USE_HEURISTIC_PATHFINDER = false;
	/** Whether to use milestone 4 defence logic. */
	private static final boolean USE_MILESTONE_4_DEFENCE = false;
	/** Whether the robot should attempt to use wall kicks. */
	private static final boolean WALL_KICKS_ENABLED = true;

	/** Distance at which the robot will begin slowing down. */
	private static final double DECELERATION_DISTANCE = 30;

	/** The multiplier of the final driving speed. */
	protected static final double DRIVING_SPEED_MULTIPLIER = 1;
	/** What fraction of forward speed the robot will lose when turning. */
	private static final double FORWARD_LOSS_MULTIPLIER = 0.7;
	/** Turning angle threshold for stop turns. */
	protected static final double STOP_TURN_THRESHOLD = 90;
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

	/** How far a robot can be away from a point before we think it is there. */
	private static final double ROBOT_POSITION_TOLERANCE = 10;

	/** At what distance to the ball should the robot begin turning in game start. */
	private static final double START_TURNING_DISTANCE = 40;
	/** X value offset of the target when the ball is reached in game start. */
	private static final double START_TURNING_X_SHIFT = 30;
	/** Y value offset of the target when the ball is reached in game start. */
	private static final double START_TURNING_Y_SHIFT = 30;

	/** The ratio by which optimal distance gets adjusted in each attempt. */
	protected static final double OPTIMAL_POINT_ADJUST = 0.8;
	/** How many times to try to find the optimal point. */
	protected static final int OPTIMAL_POINT_SEARCH_TRIES = 20;

	/** Size of the pitch's horizontal edge region. */
	private static final double PITCH_H_EDGE_REGION_SIZE = Robot.LENGTH_CM * 0.7;
	/** Size of the pitch's vertical edge region. */
	private static final double PITCH_V_EDGE_REGION_SIZE = Robot.LENGTH_CM * 0.7;

	/** High corner collision threshold. */
	private static final int HIGH_CORNER_COLL_THRESH = 5;
	/** Low corner collision threshold. */
	private static final int LOW_CORNER_COLL_THRESH = 2;

	/** High front/back collision threshold. */
	private static final int HIGH_FRONT_BACK_COLL_THRESH = 25;
	/** Low front/back collision threshold. */
	private static final int LOW_FRONT_BACK_COLL_THRESH = 15;

	// TODO: Decouple the following constants from Painter.
	/** Starting optimal point to ball offset. */
	public static final double DEFAULT_OPTIMAL_POINT_OFFSET = 2 * Robot.LENGTH_CM;
	/** Starting target distance threshold. */
	public static final double DEFAULT_TARGET_THRESHOLD = 30;

	/** The amount by which defense point is shifted on the X axis. */
	private static final double BALL_DEFENSE_X_OFFSET = 10;

	/** X coordinate adjustment of robot destination in own wall ball logic. */
	private static final double OWN_WALL_BALL_X_SHIFT = Robot.LENGTH_CM;
	/** Y coordinate adjustment of robot destination in own wall ball logic. */
	private static final double OWN_WALL_BALL_Y_SHIFT = Robot.LENGTH_CM / 4;

	/** Attack angle of the side wall ball logic. */
	private static final double SIDE_WALL_ATTACK_DIR = 30.0;
	/** Side wall ball attack point offset from the ball. */
	private static final double SIDE_WALL_OFFSET = 2 * Robot.LENGTH_CM;
	/** The amount by which the x coordinate of the ball is shifted when selecting attack point. */
	private static final double SIDE_WALL_X_ATTACK_SHIFT = 10;

	
	/** Last AI target. */
	public Vector2D lastTarget;
	
	/** Current optimal point offset. */
	protected double optimalPointOffset = DEFAULT_OPTIMAL_POINT_OFFSET;
	/** Current target distance threshold */
	private double targetThreshold = DEFAULT_TARGET_THRESHOLD;
	
	/** AI's heuristic pathfinder. */
	private Pathfinder pathfinder;
	
	// TODO: Clean up in the inevitable AIMaster logic migration.
	/** Whether we are in gotBall() state since the last check. */
	private boolean wasGotBall = false;
	
	
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
	 * @see sdp.AI.BaseAI#gotBall()
	 */
	@Override
	protected synchronized Command gotBall() throws IOException {
		if (canWeAttack(aiWorldState)) {
			wasGotBall = true;
			optimalPointOffset = DEFAULT_OPTIMAL_POINT_OFFSET;
			targetThreshold = DEFAULT_TARGET_THRESHOLD;
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

	
	/**
	 * @see sdp.AI.BaseAI#defendGoal()
	 */
	@Override
	protected Command defendGoal() throws IOException {
		if (!USE_MILESTONE_4_DEFENCE) {
			Point2D.Double target = getOptimalDefencePoint(aiWorldState);
			
			double robotToDestDist = GeomUtils.pointDistance(aiWorldState.getOwnRobot().getCoords(), target);
			if (robotToDestDist < ROBOT_POSITION_TOLERANCE) {
				return sit();
			} else {
				ArrayList<Waypoint> waypoint = pathfinder.getPath(aiWorldState, target, true);
				return getWaypointCommand(waypoint, false);
			}
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
			
			double behind_ball =aiWorldState.getBallCoords().x - BALL_DEFENSE_X_OFFSET;
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
			double behind_ball =aiWorldState.getBallCoords().x + BALL_DEFENSE_X_OFFSET;
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
				com = getWaypointCommand(pathfinder.getPath(aiWorldState, point, false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()<-90 && aiWorldState.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = getWaypointCommand(pathfinder.getPath(aiWorldState, point2, false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist2, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}

		if(aiWorldState.getEnemyRobot().getAngle()>90 && aiWorldState.getEnemyRobot().getAngle()<180 ){
			if (dist3 > 10)
				com = getWaypointCommand(pathfinder.getPath(aiWorldState, point3, false), false, 1, 20);
			
			com.acceleration = 150;
			reduceSpeed(com, dist3, 20, 0);
			Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
			return com;
		}
		
		Painter.debug = new Vector2D[] {new Vector2D(point), new Vector2D(point2), new Vector2D(point3)};
		return null;
	}

	
	/**
	 * @see sdp.AI.BaseAI#changedState()
	 */
	protected void changedState() {
		optimalPointOffset = DEFAULT_OPTIMAL_POINT_OFFSET;
		targetThreshold = DEFAULT_TARGET_THRESHOLD;
	}
	

	/**
	 * @see sdp.AI.BaseAI#penaltiesDefend()
	 */
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
	}
	
	
	/**
	 * @see sdp.AI.BaseAI#start()
	 */
	@Override
	protected Command start() throws IOException {
		Vector2D target = new Vector2D(aiWorldState.getBallCoords());
		
		double robotToBallDist = GeomUtils.pointDistance(aiWorldState.getOwnRobot().getFrontCenter(), target);
		if (robotToBallDist < START_TURNING_DISTANCE) {
			target.x += (aiWorldState.isOwnGoalLeft()
					? START_TURNING_X_SHIFT : -START_TURNING_X_SHIFT);
			target.y += START_TURNING_Y_SHIFT;
		}
		
		ArrayList<Waypoint> path = pathfinder.getPath(aiWorldState,
				target, false);
		return getWaypointCommand(path, true);
	}


	/**
	 * Get a command to attack the opponents.
	 * 
	 * @param mode Robot's attack mode. See {@link AttackMode}.
	 * @return The next command the robot should execute.
	 * @throws IOException 
	 */
	private Command attack(AttackMode mode) throws IOException {
		// Are we ready to score?
		if (canWeAttack(aiWorldState)) {
			return gotBall();
		}
		
		// Handle the cases where the ball is next to the wall.
		if (SPECIAL_WALL_LOGIC_ENABLED) {
			boolean ballAdjacency[] = getWallsBallIsAdjacentTo();
			
			if (ballAdjacency[0]) {
				return getOwnWallBallCommand();
			} else if (ballAdjacency[1]) {
				return getSideWallBallCommand();
			} else if (ballAdjacency[2]) {
				return getEnemyWallBallCommand();
			}
		}
		
		// Get the point to drive towards.
		Vector2D target = new Vector2D(aiWorldState.getBallCoords());
		
		Point2D.Double optimalPoint = null;
		for (int i = 0; i < OPTIMAL_POINT_SEARCH_TRIES; i++) {
			optimalPoint = getOptimalAttackPoint(aiWorldState, optimalPointOffset, mode);
			
			if (optimalPoint == null) {
				optimalPointOffset *= OPTIMAL_POINT_ADJUST;
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
		boolean mustFaceTarget = (REQUIRE_FACE_BALL_TO_KICK
				&& !Utilities.areDoublesEqual(optimalPointOffset, DEFAULT_OPTIMAL_POINT_OFFSET));

		lastTarget = target;
		
		ArrayList<Waypoint> path = pathfinder.getPath(aiWorldState, target, ballIsObstacle);
		return getWaypointCommand(path, mustFaceTarget);
	}
	
	
	/**
	 * Get a waypoint for our robot that completely ignores any and all
	 * obstacles to the destination.
	 * 
	 * @param dest Destination point.
	 * @return An appropriate waypoint, as described above.
	 */
	private Waypoint getUnconditionalWaypoint(Point2D.Double dest) {
		return new Waypoint(new Vector2D(aiWorldState.getOwnRobot().getCoords()),
				aiWorldState.getOwnRobot().getAngle(), new Vector2D(dest),
				WorldState.PITCH_WIDTH_CM, true);
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
		ArrayList<Waypoint> path = new ArrayList<Waypoint>();
		path.add(waypoint);
		
		return getWaypointCommand(path, mustFaceTarget);
	}
	
	/**
	 * Generate a command that will get the robot closer to the specified
	 * sequence of waypoints.
	 * 
	 * @param path Target path.
	 * @param mustFaceTarget Whether the robot is required to face the point
	 * 		at all times.
	 * @return A command that will get the robot closer towards the given
	 * 		waypoint.
	 */
	private Command getWaypointCommand(ArrayList<Waypoint> path, boolean mustFaceTarget) {
		return getWaypointCommand(path, mustFaceTarget, DRIVING_SPEED_MULTIPLIER,
				STOP_TURN_THRESHOLD);
	}

	/**
	 * Generate a command that will get the robot closer to the specified
	 * sequence of waypoints. This version allows to specify custom driving
	 * speed multiplier and maximum turning speed.
	 * 
	 * @param path Target path.
	 * @param mustFaceTarget Whether the robot is required to face the point
	 * 		at all times.
	 * @param drivingSpeedMult Driving speed multiplier.
	 * @param stopTurnThreshold Maximum turning speed of the robot.
	 * @return A command that will get the robot closer towards the given
	 * 		waypoint.
	 */
	protected Command getWaypointCommand(ArrayList<Waypoint> path,
			boolean mustFaceTarget,	double drivingSpeedMult, double stopTurnThreshold) {
		Command comm = new Command(0.0, 0.0, false);
		Waypoint waypoint = path.get(0);

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

		// React to collisions.
		reactToFrontBackCollisions(comm, true, waypoint.isEndpoint() ? LOW_FRONT_BACK_COLL_THRESH : HIGH_FRONT_BACK_COLL_THRESH);
		reactToCornerCollisions(comm, waypoint.isEndpoint() ? LOW_CORNER_COLL_THRESH : HIGH_CORNER_COLL_THRESH);

		// Optimal point offset and target threshold adjustments.
		if ((Math.abs(waypoint.getTurningAngle()) < 45)
				&& (waypoint.getDistance() < targetThreshold)) {
			optimalPointOffset *= 0.5;
			targetThreshold = optimalPointOffset * 0.5;
		}
		if (optimalPointOffset < OWN_BALL_KICK_DIST) {
			optimalPointOffset = OWN_BALL_KICK_DIST;
		}

		Vector2D ball = new Vector2D(aiWorldState.getBallCoords());
		if (aiWorldState.isOwnGoalLeft()) {
			if (ball.getX() < aiWorldState.getOwnRobot().getCoords().getX()) {
				optimalPointOffset = DEFAULT_OPTIMAL_POINT_OFFSET;
				targetThreshold = DEFAULT_TARGET_THRESHOLD;
			}
		} else {
			if (ball.getX() > aiWorldState.getOwnRobot().getCoords().getX()) {
				optimalPointOffset = DEFAULT_OPTIMAL_POINT_OFFSET;
				targetThreshold = DEFAULT_TARGET_THRESHOLD;
			}
		}

		// Finalise the command.
		reduceSpeed(comm, waypoint.getCostToDest(), DECELERATION_DISTANCE,
				Robot.MAX_DRIVING_SPEED / 2);

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
				double speedFactor = (frontCollDist / threshold) - 1;
				speedFactor = Utilities.restrictValueToInterval(speedFactor, -1, 0).doubleValue();
				command.drivingSpeed *= speedFactor;
			}

		// Collision in the back.
		} else if (backCollDist < threshold) {
			if (command.drivingSpeed <= 0) {
				double speedFactor = (backCollDist / threshold) - 1;
				speedFactor = Utilities.restrictValueToInterval(speedFactor, -1, 0).doubleValue();
				command.drivingSpeed *= speedFactor;
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
	 * Get which walls the ball is adjacent to.
	 * 
	 * @return Whether the ball is next our wall, side wall and enemy wall,
	 * 		as an array, in that order.
	 */
	private boolean[] getWallsBallIsAdjacentTo() {
		boolean ballAdjToOwnWall = false;
		boolean ballAdjToSideWall = false;
		boolean ballAdjToEnemyWall = false;
		
		Point2D.Double ball = aiWorldState.getBallCoords();
		
		// Is the ball next to a side wall?
		if ((ball.y < PITCH_H_EDGE_REGION_SIZE)
				|| (ball.y > (WorldState.PITCH_HEIGHT_CM - PITCH_H_EDGE_REGION_SIZE))) {
			ballAdjToSideWall = true;
		}
		
		// Is the ball next to our side wall?
		if ((ball.y < aiWorldState.getOwnGoal().getTop().y)
				|| (ball.y > aiWorldState.getOwnGoal().getBottom().y)) {
			if (aiWorldState.isOwnGoalLeft()) {
				if (ball.x < PITCH_V_EDGE_REGION_SIZE) {
					ballAdjToOwnWall = true;
				}
			} else {
				if (ball.x > (WorldState.PITCH_WIDTH_CM - PITCH_V_EDGE_REGION_SIZE)) {
					ballAdjToOwnWall = true;
				}
			}
		}
		
		// Is the ball next to enemy side wall?
		if ((ball.y < aiWorldState.getEnemyGoal().getTop().y)
				|| (ball.y > aiWorldState.getEnemyGoal().getBottom().y)) {
			if (aiWorldState.isOwnGoalLeft()) {
				if (ball.x > (WorldState.PITCH_WIDTH_CM - PITCH_V_EDGE_REGION_SIZE)) {
					ballAdjToEnemyWall = true;
				}
			} else {
				if (ball.x < PITCH_V_EDGE_REGION_SIZE) {
					ballAdjToEnemyWall = true;
				}
			}
		}
		
		return new boolean[] { ballAdjToOwnWall, ballAdjToSideWall, ballAdjToEnemyWall };
	}
	

	/**
	 * Get a command for handling the ball when it is next to our wall.
	 * 
	 * @return The command to execute next.
	 * @throws IOException 
	 */
	private Command getOwnWallBallCommand() throws IOException {
		Point2D.Double dest = new Point2D.Double();
		
		if (aiWorldState.isOwnGoalLeft()) {
			dest.x = aiWorldState.getOwnGoal().getCentre().x + OWN_WALL_BALL_X_SHIFT;
		} else {
			dest.x = aiWorldState.getOwnGoal().getCentre().x - OWN_WALL_BALL_X_SHIFT;
		}
		
		if (aiWorldState.getBallCoords().y < (WorldState.PITCH_HEIGHT_CM / 2)) {
			dest.y = aiWorldState.getOwnGoal().getTop().y + OWN_WALL_BALL_Y_SHIFT;
		} else {
			dest.y = aiWorldState.getOwnGoal().getBottom().y - OWN_WALL_BALL_Y_SHIFT;
		}
		
		double robotToDestDist = GeomUtils.pointDistance(aiWorldState.getOwnRobot().getCoords(), dest);
		if (robotToDestDist < ROBOT_POSITION_TOLERANCE) {
			return sit();
		} else {
			ArrayList<Waypoint> path = pathfinder.getPath(aiWorldState, dest, true);
			return getWaypointCommand(path, false);
		}
	}
	
	/**
	 * Get a command for handling the ball when it is next to the side wall.
	 * 
	 * @return The next command to execute.
	 */
	private Command getSideWallBallCommand() {
		double attackDir = 0;
		if (aiWorldState.isOwnGoalLeft()) {
			if (aiWorldState.getBallCoords().y < (WorldState.PITCH_HEIGHT_CM / 2)) {
				attackDir = -180.0 + SIDE_WALL_ATTACK_DIR;
			} else {
				attackDir = 180.0 - SIDE_WALL_ATTACK_DIR;
			}
		} else {
			if (aiWorldState.getBallCoords().y < (WorldState.PITCH_HEIGHT_CM / 2)) {
				attackDir = -SIDE_WALL_ATTACK_DIR;
			} else {
				attackDir = SIDE_WALL_ATTACK_DIR;
			}
		}
		
		Point2D.Double ball = aiWorldState.getBallCoords();
		
		Vector2D attackOffset = Vector2D.getDirectionUnitVector(attackDir);
		attackOffset = Vector2D.changeLength(attackOffset, SIDE_WALL_OFFSET);
		Point2D.Double attackPoint = GeomUtils.addPoints(ball, attackOffset);
		
		if (!WorldState.isPointInPaddedPitch(attackPoint, Robot.LENGTH_CM / 2)) {
			attackPoint.x = ball.x;
		}
		
		Point2D.Double shiftedBall = new Point2D.Double(ball.x, ball.y);
		if (aiWorldState.isOwnGoalLeft()) {
			shiftedBall.x += SIDE_WALL_X_ATTACK_SHIFT;
		} else {
			shiftedBall.x -= SIDE_WALL_X_ATTACK_SHIFT;
		}
		
		if (!Robot.lineIntersectsRobot(ball, attackPoint, aiWorldState.getOwnRobot())) {
			Waypoint waypoint = getUnconditionalWaypoint(shiftedBall);
			return getWaypointCommand(waypoint, true);
		} else {		
			ArrayList<Waypoint> path = pathfinder.getPath(aiWorldState, attackPoint, true);
			return getWaypointCommand(path, false);
		}
	}
	
	/**
	 * Get a command for handling the ball when it is next to the enemy wall.
	 * 
	 * @return The command to execute next.
	 * @throws IOException
	 */
	private Command getEnemyWallBallCommand() throws IOException {
		return defendGoal();
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
	protected Point2D.Double getOptimalAttackPoint(AIWorldState worldState,
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
	public static Point2D.Double getOptimalDefencePoint(AIWorldState worldState) {
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
		
		//System.out.println("AI thinks it can kick.");
		
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
	
	
	/**
	 * Check whether we entered the gotBall() state since the last check.
	 * 
	 * TODO: Move to BaseAI??
	 *  
	 * @return Whether we were in gotBall() since the last check.
	 */
	public boolean didIGetBall() {
		final boolean orig = wasGotBall;
		wasGotBall = false;
		return orig;
	}
}