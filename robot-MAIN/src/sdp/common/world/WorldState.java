package sdp.common.world;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;


/**
 * The state of the field.
 */
public class WorldState {
	
	/** A flag that denotes that ball should be considered an obstacle. */
	public static final int BALL_IS_OBSTACLE_FLAG = 0x1;
	/** A flag that denotes that blue robot should be considered an obstacle. */
	public static final int BLUE_IS_OBSTACLE_FLAG = 0x2;
	/** A flag that denotes that yellow robot should be considered an obstacle. */
	public static final int YELLOW_IS_OBSTACLE_FLAG = 0x4;
	
	/** Radius of the ball obstacle circle. */
	public static final double BALL_OBSTACLE_RADIUS = 10;
	/** Radius of the robot obstacle circle. */
	public static final double ROBOT_OBSTACLE_RADIUS = Robot.LENGTH_CM * 1.5;
	
	/** Height of the pitch in centimetres. */
	public static final double PITCH_HEIGHT_CM = 113.7;
	/** Width of the pitch in centimetres. */
	public static final double PITCH_WIDTH_CM = 244;
	
	/** Height of the goals in centimetres. */
	public static final double GOAL_CENTRE_Y = PITCH_HEIGHT_CM / 2;


	/** Location of the ball. */
	private Point2D.Double ballCoords;
	/** The blue robot. */
	private Robot blueRobot;
	/** The yellow robot. */
	private Robot yellowRobot;
	/** The left goal. */
	private Goal leftGoal;
	/** The right goal. */
	private Goal rightGoal;
	
	/** Picture of the world. */
	private BufferedImage worldImage;

	
	/**
	 * Create a new world state.
	 * 
	 * @param ballCoords Coordinates of the ball.
	 * @param blueRobot The blue robot.
	 * @param yellowRobot The yellow robot.
	 * @param worldImage The picture of the field.
	 */
	public WorldState(Point2D.Double ballCoords, Robot blueRobot, Robot yellowRobot, BufferedImage worldImage) {
		update(ballCoords, blueRobot, yellowRobot, worldImage);
		
		leftGoal = new Goal(new Point2D.Double(0, GOAL_CENTRE_Y));
		rightGoal = new Goal(new Point2D.Double(PITCH_WIDTH_CM, GOAL_CENTRE_Y));
	}

	
	/**
	 * Get the location of the ball.
	 * 
	 * @return The location of the ball.
	 */
	public final Point2D.Double getBallCoords() {
		return ballCoords;
	}

	/**
	 * Get the blue robot.
	 * 
	 * @return The blue robot.
	 */
	public final Robot getBlueRobot() {
		return blueRobot;
	}

	/**
	 * Get the yellow robot.
	 * 
	 * @return The yellow robot.
	 */
	public final Robot getYellowRobot() {
		return yellowRobot;
	}
	
	/**
	 * Get the left goal.
	 * 
	 * @return The left goal.
	 */
	public final Goal getLeftGoal() {
		return leftGoal;
	}
	
	/**
	 * Get the right goal.
	 * 
	 * @return The right goal.
	 */
	public final Goal getRightGoal() {
		return rightGoal;
	}
	
	/**
	 * Get the image of the world.
	 * 
	 * @return The image of the world.
	 */
	public final BufferedImage getWorldImage() {
		return worldImage;
	}
	
	
	/**
	 * Get whether the ball is present in the world state.
	 * 
	 * @return Whether the ball is present in the world state.
	 */
	public final boolean isBallPresent() {
		return !GeomUtils.isPointNegative(ballCoords);
	}


	/**
	 * Update the world state.
	 * 
	 * It is worth pointing out that this class was originally supposed to
	 * be immutable. So much for that.
	 * 
	 * @param ballCoords Coordinates of the ball.
	 * @param blueRobot The blue robot.
	 * @param yellowRobot The yellow robot.
	 * @param worldImage The image of the world.
	 */
	public void update(Point2D.Double ballCoords, Robot blueRobot, Robot yellowRobot,
			BufferedImage worldImage) {
		this.ballCoords = ballCoords;
		this.blueRobot = blueRobot;
		this.yellowRobot = yellowRobot;
		this.worldImage = worldImage;
	}
	
	
	/**
	 * Check whether the given point is inside the football pitch.
	 * 
	 * @param point Point in question.
	 * @return Whether the point is inside the pitch.
	 */
	public static boolean isPointInPitch(Point2D.Double point) {
		return WorldState.isPointInPaddedPitch(point, 0.0);
	}

	/**
	 * Check whether the given point is inside the football pitch with some
	 * padding added on the sides.
	 * 
	 * @param point Point in question.
	 * @param padding The amount of wall padding of the pitch.
	 * @return Whether the point is inside the padded pitch.
	 */
	public static boolean isPointInPaddedPitch(Point2D.Double point, double padding) {
		return ((point.x >= padding) && (point.y >= padding)
				&& (point.x <= (PITCH_WIDTH_CM - padding))
				&& (point.y <= (PITCH_HEIGHT_CM - padding)));
	}
	

	/**
	 * Create an obstacle bitfield.
	 * 
	 * @param ballIsObstacle Whether the ball is an obstacle.
	 * @param blueIsObstacle Whether the blue robot is an obstacle.
	 * @param yellowIsObstacle Whether the yellow robot is an obstacle.
	 * @return Obstacle bitfield that matches the given parameter values.
	 */
	public static int makeObstacleFlags(boolean ballIsObstacle, boolean blueIsObstacle,
			boolean yellowIsObstacle) {
		int flags = 0;
		if (ballIsObstacle) {
			flags |= WorldState.BALL_IS_OBSTACLE_FLAG;
		}
		if (blueIsObstacle) {
			flags |= WorldState.BLUE_IS_OBSTACLE_FLAG;
		}
		if (yellowIsObstacle) {
			flags |= WorldState.YELLOW_IS_OBSTACLE_FLAG;
		}
		
		return flags;
	}
	
	/**
	 * Create an obstacle bitfield to match our opponent robot.
	 * 
	 * @param ballIsObstacle Whether the ball is an obstacle.
	 * @param isOwnTeamBlue Whether our robot is blue.
	 * @return Obstacle bitfield that matches the opponent robot.
	 */
	public static int makeObstacleFlagsForOpponent(boolean ballIsObstacle,
			boolean isOwnTeamBlue) {
		return makeObstacleFlags(ballIsObstacle, !isOwnTeamBlue, isOwnTeamBlue);
	}
	
	
	/**
	 * Extract all obstacles out of a world state as circles.
	 * 
	 * @param worldState World state of interest.
	 * @param obstacles Objects that should be considered as obstacles.
	 * @return A list of requested obstacles, represented as circles.
	 */
	public static ArrayList<Circle> getObstacleCircles(WorldState worldState, int obstacles) {
		ArrayList<Circle> circles = new ArrayList<Circle>();
		
		if ((obstacles & BALL_IS_OBSTACLE_FLAG) != 0) {
			circles.add(new Circle(worldState.getBallCoords(), BALL_OBSTACLE_RADIUS));
		}
		if ((obstacles & BLUE_IS_OBSTACLE_FLAG) != 0) {
			circles.add(new Circle(worldState.getBlueRobot().getCoords(), ROBOT_OBSTACLE_RADIUS));
		}
		if ((obstacles & YELLOW_IS_OBSTACLE_FLAG) != 0) {
			circles.add(new Circle(worldState.getYellowRobot().getCoords(), ROBOT_OBSTACLE_RADIUS));
		}
		
		return circles;
	}

	
	/**
	 * Find the closest collision from the given point in the specified
	 * direction and return it as a vector. The vector's direction will match
	 * the given parameter and its length will match the distance to the first
	 * collision.
	 * 
	 * @param state World state in which to perform the search.
	 * @param origin The point from which to cast the ray.
	 * @param direction Direction of the ray.
	 * @param obstacles A bitfield that denotes which objects are considered
	 * 		to be obstacles.
	 * @return Collision vector, as described above.
	 */
	public static Vector2D getClosestCollisionVec(WorldState state, Vector2D origin,
			Vector2D direction, int obstacles) {
		if (!isPointInPitch(origin)) {
			return Vector2D.ZERO();
		}
	
		class VectorPair {
			public Vector2D vec1;
			public Vector2D vec2;
			
			public VectorPair(Vector2D vec1, Vector2D vec2) {
				this.vec1 = vec1;
				this.vec2 = vec2;
			}
		}		
		ArrayList<VectorPair> segments = new ArrayList<VectorPair>();
		
		// Wall collisions.
		segments.add(new VectorPair(
				new Vector2D(0.0, 0.0),
				new Vector2D(PITCH_WIDTH_CM, 0.0))
		);
		segments.add(new VectorPair(
				new Vector2D(PITCH_WIDTH_CM, 0.0),
				new Vector2D(PITCH_WIDTH_CM, PITCH_HEIGHT_CM))
		);
		segments.add(new VectorPair(
				new Vector2D(PITCH_WIDTH_CM, PITCH_HEIGHT_CM),
				new Vector2D(0.0, PITCH_HEIGHT_CM))
		);
		segments.add(new VectorPair(
				new Vector2D(0.0, PITCH_HEIGHT_CM),
				new Vector2D(0.0, 0.0))
		);
		
		// Ball collisions.
		if ((obstacles & BALL_IS_OBSTACLE_FLAG) != 0) {
			Vector2D perpDir = Vector2D.getPerpendicular(direction);
			Vector2D offsetVec1 = Vector2D.changeLength(perpDir, BALL_OBSTACLE_RADIUS);
			Vector2D offsetVec2 = Vector2D.changeLength(perpDir, -BALL_OBSTACLE_RADIUS);
			
			segments.add(new VectorPair(
					Vector2D.add(new Vector2D(state.getBallCoords()), offsetVec1),
					Vector2D.add(new Vector2D(state.getBallCoords()), offsetVec2))
			);
		}
		
		// Blue robot collisions.
		if ((obstacles & BLUE_IS_OBSTACLE_FLAG) != 0) {
			segments.add(new VectorPair(
					new Vector2D(state.getBlueRobot().getFrontLeft()),
					new Vector2D(state.getBlueRobot().getFrontRight()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getBlueRobot().getFrontRight()),
					new Vector2D(state.getBlueRobot().getBackRight()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getBlueRobot().getBackRight()),
					new Vector2D(state.getBlueRobot().getBackLeft()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getBlueRobot().getBackLeft()),
					new Vector2D(state.getBlueRobot().getFrontLeft()))
			);
		}
		
		// Yellow robot collisions.
		if ((obstacles & YELLOW_IS_OBSTACLE_FLAG) != 0) {
			segments.add(new VectorPair(
					new Vector2D(state.getYellowRobot().getFrontLeft()),
					new Vector2D(state.getYellowRobot().getFrontRight()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getYellowRobot().getFrontRight()),
					new Vector2D(state.getYellowRobot().getBackRight()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getYellowRobot().getBackRight()),
					new Vector2D(state.getYellowRobot().getBackLeft()))
			);
			segments.add(new VectorPair(
					new Vector2D(state.getYellowRobot().getBackLeft()),
					new Vector2D(state.getYellowRobot().getFrontLeft()))
			);
		}
		
		// Actual collision test.
		Vector2D nearest = Vector2D.changeLength(direction, PITCH_WIDTH_CM);
		for (VectorPair segment : segments) {
			Vector2D current = GeomUtils.getLocalRaySegmentIntersection(origin, direction,
					segment.vec1, segment.vec2);
			if ((current != null) && (current.getLength() < nearest.getLength())) {
				nearest = current;
			}
		}
		
		return nearest;
	}
	
	/**
	 * Find the closest collision from the given point in the specified
	 * direction and return the distance to it.
	 * 
	 * @param state World state in which to perform the search.
	 * @param origin The point from which to cast the ray.
	 * @param direction Direction of the ray.
	 * @param obstacles A bitfield that denotes which objects are considered
	 * 		to be obstacles.
	 * @return Distance to the closest collision, as described above.
	 */
	public static double getClosestCollisionDist(WorldState state, Vector2D origin,
			Vector2D direction, int obstacles) {
		Vector2D collVec = getClosestCollisionVec(state, origin, direction, obstacles);
		return collVec.getLength();
	}
	
	/**
	 * Get the closest collision from a robot positioned in the starting
	 * point, looking into the direction of the specified point. Two values
	 * are reported: shortest collision of the left and the right sides of
	 * the robot.
	 * 
	 * Here is a "helpful" graphic:
	 * 
	 *  v- left side                                   /\ <- obstacle         |
	 * +----+--------------left collision vector----->[||]                    |
	 * |o |=| <- robot facing east                     \/       O <- target   |
	 * +----+--------------right collision vector---------------------------->|
	 *  ^- right side                                                 wall -> |
	 * 
	 * @param state Current world state.
	 * @param dirPt Point, in whose direction the check will be performed.
	 * @param widthFactor Factor by which the robot width we consider is
	 * 		modified.
	 * @param obstacles A bitfield that denotes which objects are considered
	 * 		to be obstacles.
	 * @return Left and right collision distances, as described above.
	 */
	public static Vector2D[] getClosestSideCollisions(WorldState state,
			Vector2D startPt, Vector2D dirPt, double widthFactor, int obstacles) {
		Vector2D dir = Vector2D.subtract(dirPt, startPt);
		double angle = (-dir.getDirection() + 90) * Math.PI / 180d;
		
		double factor = widthFactor * Robot.WIDTH_CM / 2;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		Vector2D leftOffset = new Vector2D(cos * factor, sin * factor);
		Vector2D leftStartPt = Vector2D.add(startPt, leftOffset);
		Vector2D leftColl = getClosestCollisionVec(state, leftStartPt, dir, obstacles);
		
		Vector2D rightOffset = new Vector2D(-cos * factor, -sin * factor);
		Vector2D rightStartPt = Vector2D.add(startPt, rightOffset);
		Vector2D rightColl = getClosestCollisionVec(state, rightStartPt, dir, obstacles);
		
		Vector2D retValue[] = { leftColl, rightColl };
		return retValue;
	}

	/**
	 * Check whether a robot could drive between two points in a straight line
	 * without colliding with anything.
	 * 
	 * This function is pessimistic, since it considers a tunnel that is a bit
	 * wider than the robot.
	 * 
	 * @param state Current world state.
	 * @param point1 One of the path's endpoints.
	 * @param point2 Another of the path's endpoints.
	 * @param widthFactor Factor by which the robot width we consider is
	 * 		modified.
	 * @param obstacles A bitfield that denotes which objects are considered
	 * 		to be obstacles.
	 * @return Whether the path between two points is clear.
	 */
	public static boolean isDirectPathClear(WorldState state, Vector2D point1,
			Vector2D point2, int obstacles) {
		double pathLength = Vector2D.subtract(point2, point1).getLength();		
		double widthFactors[] = { 0.0, 0.25, 0.5, 0.75, 1.0, 1.2 };
		
		for (double factor : widthFactors) {
			Vector2D sideColls[] = getClosestSideCollisions(state, point1,
					point2, factor, obstacles);
			if ((sideColls[0].getLength() < pathLength)
					|| (sideColls[1].getLength() < pathLength)) {
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * TODO: Clean up.
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point) {
		return WorldState.getNearestCollisionPoint(ls, am_i_blue, point, true);
	}

	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * TODO: Clean up.
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @param include_enemy whether to include enemy
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point, boolean include_enemy) {
		Robot enemy = am_i_blue ? ls.getYellowRobot() : ls.getBlueRobot();
		Vector2D[] enemy_pts = new Vector2D[] {
				new Vector2D(enemy.getFrontLeft()),
				new Vector2D(enemy.getFrontRight()),
				new Vector2D(enemy.getBackLeft()),
				new Vector2D(enemy.getBackRight())
		};
		// top wall test
		Vector2D temp = Vector2D.subtract(new Vector2D(0, PITCH_HEIGHT_CM), new Vector2D(0, point.getY()));
		Vector2D min = temp;
		// bottom wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(0, point.getY()));
		if (temp.getLength() < min.getLength())
			min = temp;
		// left wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// right wall test
		temp = Vector2D.subtract(new Vector2D(PITCH_WIDTH_CM, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// closest distance to enemy
		if (include_enemy) {
			temp = GeomUtils.getMinVectorToPoints(enemy_pts, new Vector2D(point));
			if (temp.getLength() < min.getLength())
				min = temp;
		}
		// we have our point
		return min;
	}
}
