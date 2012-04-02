package sdp.common.world;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Queue;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.xml.XmlUtils;


/**
 * The state of the field.
 */
public class WorldState {
	
	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.common.world.WorldState");
	
	/** A flag that denotes that ball should be considered an obstacle. */
	public static final int BALL_IS_OBSTACLE_FLAG = 0x1;
	/** A flag that denotes that blue robot should be considered an obstacle. */
	public static final int BLUE_IS_OBSTACLE_FLAG = 0x2;
	/** A flag that denotes that yellow robot should be considered an obstacle. */
	public static final int YELLOW_IS_OBSTACLE_FLAG = 0x4;
	
	/** Radius of the ball obstacle circle. */
	private static final double BALL_OBSTACLE_RADIUS = 10;
	/** Radius of the robot obstacle circle. */
	private static final double ROBOT_OBSTACLE_RADIUS = Robot.LENGTH_CM * 0.9;
	/** The amount by which obstacles are increased in extraction. */
	private static final double OBSTACLE_SIZE_INCREASE = Robot.LENGTH_CM * 0.5;
	
	/** Direct path collision check tolerance. */
	private static final double DIRECT_PATH_CHECK_TOLERANCE = 0.001;
	
	/** Height of the pitch in centimetres. */
	public static final double PITCH_HEIGHT_CM = 113.7;
	/** Width of the pitch in centimetres. */
	public static final double PITCH_WIDTH_CM = 244;
	
	/** Height of the goals in centimetres. */
	public static final double GOAL_CENTRE_Y = PITCH_HEIGHT_CM / 2;
	
	/** Width increments of the robot ray in clear path calculations. */
	private static final double ROBOT_RAY_INCREMENT = 0.05;
	/** Maximum ray width of the robot in clear path calculations. */
	private static final double ROBOT_RAY_MAX_WIDTH = 1.2;


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
	 * Convert a 2D point from normal coordinates to real world coordinates
	 * (i.e. [0; 1] -> centimeters).
	 * 
	 * TODO: Move to GeomUtils??
	 * 
	 * @param point Point to convert.
	 * @return Converted point.
	 */
	private static Point2D.Double toCentimeters(Point2D.Double point) {
		return new Point2D.Double(point.getX() * PITCH_WIDTH_CM, point.getY() * PITCH_WIDTH_CM);
	}

	/**
	 * Convert robot's location from normal coordinates to real world
	 * coordinates (i.e. [0; 1] -> centimeters).
	 * 
	 * @param robot Robot to convert.
	 * @return Converted robot.
	 */
	private static Robot toCentimeters(Robot robot) {
		Robot cmRobot = new Robot(WorldState.toCentimeters(robot.getCoords()), robot.getAngle());
		cmRobot.setCoords(true);
		return cmRobot;
	}

	public static WorldState toCentimeters(WorldState worldState) {
		return new WorldState(
				WorldState.toCentimeters(worldState.getBallCoords()),
				WorldState.toCentimeters(worldState.getBlueRobot()),
				WorldState.toCentimeters(worldState.getYellowRobot()),
				worldState.getWorldImage());
	}
	
	public static Point2D.Double fromCentimeters(Point2D.Double point) {
		return new Point2D.Double(point.getX() / PITCH_WIDTH_CM, point.getY() / PITCH_WIDTH_CM);
	}

	private static Robot fromCentimeters(Robot robot) {
		Robot cmRobot = new Robot(WorldState.fromCentimeters(robot.getCoords()), robot.getAngle());
		cmRobot.setCoords(true);
		return cmRobot;
	}

	public static WorldState fromCentimeters(WorldState worldState) {
		return new WorldState(
				WorldState.fromCentimeters(worldState.getBallCoords()),
				WorldState.fromCentimeters(worldState.getBlueRobot()),
				WorldState.fromCentimeters(worldState.getYellowRobot()),
				worldState.getWorldImage());
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
			circles.add(new Circle(worldState.getBallCoords(),
					BALL_OBSTACLE_RADIUS + OBSTACLE_SIZE_INCREASE));
		}
		if ((obstacles & BLUE_IS_OBSTACLE_FLAG) != 0) {
			circles.add(new Circle(worldState.getBlueRobot().getCoords(),
					ROBOT_OBSTACLE_RADIUS + OBSTACLE_SIZE_INCREASE));
		}
		if ((obstacles & YELLOW_IS_OBSTACLE_FLAG) != 0) {
			circles.add(new Circle(worldState.getYellowRobot().getCoords(),
					ROBOT_OBSTACLE_RADIUS + OBSTACLE_SIZE_INCREASE));
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
	 * @param startPt Point, from which to begin checks.
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
		pathLength -= DIRECT_PATH_CHECK_TOLERANCE;
		
		double factor = 0.0;
		while (factor <= ROBOT_RAY_MAX_WIDTH) {
			Vector2D sideColls[] = getClosestSideCollisions(state, point1,
					point2, factor, obstacles);
			if ((sideColls[0].getLength() < pathLength)
					|| (sideColls[1].getLength() < pathLength)) {
				return false;
			}

			factor += ROBOT_RAY_INCREMENT;
		}

		return true;
	}
	
	
	/**
	 * Read a world state from an XML file and return it.
	 * 
	 * @param filename Name of the file that contains the world state.
	 * @param subtitle pass a new {@link StringBuilder}. Use {@link StringBuilder#toString()} to get the subtitle
	 * @return An appropriate WorldState instance.
	 */
	public static WorldState loadWorldState(String filename, StringBuilder subtitle, Queue<Vector2D> point) {
		File file = new File(filename);
		if (!file.exists()) {
			LOGGER.info("The given world state file does not exist.");
			return null;
		}
				
		Document doc = XmlUtils.openXmlDocument(file);
		
		Element rootElement = (Element) doc.getElementsByTagName("worldstate").item(0);
				
		Element ballElem = (Element) rootElement.getElementsByTagName("ball").item(0);
		double ballX = XmlUtils.getChildDouble(ballElem, "x");
		double ballY = XmlUtils.getChildDouble(ballElem, "y");
		Point2D.Double ball = new Point2D.Double(ballX, ballY);
		
		Element blueElem = (Element) rootElement.getElementsByTagName("bluerobot").item(0);
		double blueX = XmlUtils.getChildDouble(blueElem, "x");
		double blueY = XmlUtils.getChildDouble(blueElem, "y");
		double blueAngle = XmlUtils.getChildDouble(blueElem, "angle");
		Point2D.Double bluePos = new Point2D.Double(blueX, blueY);
		Robot blueRobot = new Robot(bluePos, blueAngle);
		
		Element yellowElem = (Element) rootElement.getElementsByTagName("yellowrobot").item(0);
		double yellowX = XmlUtils.getChildDouble(yellowElem, "x");
		double yellowY = XmlUtils.getChildDouble(yellowElem, "y");
		double yellowAngle = XmlUtils.getChildDouble(yellowElem, "angle");
		Point2D.Double yellowPos = new Point2D.Double(yellowX, yellowY);
		Robot yellowRobot = new Robot(yellowPos, yellowAngle);
		
		NodeList subS = rootElement.getElementsByTagName("subtitle");
		final int length = subS.getLength();
		
		for (int i = 0; i < length; i++) {
			Element subElem = (Element) subS.item(i);
			String sub = XmlUtils.getChildText(subElem, "text");
			double subX = XmlUtils.getChildDouble(subElem, "x");
			double subY = XmlUtils.getChildDouble(subElem, "y");
			if (subtitle != null)
				subtitle.append(sub);
			if (point != null) {
				point.add(new Vector2D(subX, subY));
			}
		}
		

		
		WorldState worldState = new WorldState(ball, blueRobot, yellowRobot, null);
		
		return worldState;
	}
	
	
	/**
	 * Writes the given world state into an XML file.
	 * 
	 * @param config Configuration to output.
	 * @param filename Output filename.
	 * @param subtitle write a subtitle along with the frame
	 */
	public static void writeWorldState(WorldState worldState, String filename, String subtitle, Vector2D[] point) {
		Document doc = XmlUtils.createBlankXmlDocument();
		
		Element rootElement = doc.createElement("worldstate");
		doc.appendChild(rootElement);
		
		Element ballElem = (Element) doc.createElement("ball");
		XmlUtils.addChildDouble(doc, ballElem, "x", worldState.getBallCoords().x);
		XmlUtils.addChildDouble(doc, ballElem, "y", worldState.getBallCoords().y);
		rootElement.appendChild(ballElem);
		
		Element blueElem = (Element) doc.createElement("bluerobot");
		XmlUtils.addChildDouble(doc, blueElem, "x", worldState.getBlueRobot().getCoords().x);
		XmlUtils.addChildDouble(doc, blueElem, "y", worldState.getBlueRobot().getCoords().y);
		XmlUtils.addChildDouble(doc, blueElem, "angle", worldState.getBlueRobot().getAngle());
		rootElement.appendChild(blueElem);
		
		Element yellowElem = (Element) doc.createElement("yellowrobot");
		XmlUtils.addChildDouble(doc, yellowElem, "x", worldState.getYellowRobot().getCoords().x);
		XmlUtils.addChildDouble(doc, yellowElem, "y", worldState.getYellowRobot().getCoords().y);
		XmlUtils.addChildDouble(doc, yellowElem, "angle", worldState.getYellowRobot().getAngle());
		rootElement.appendChild(yellowElem);
		
		for (int i = 0; i < point.length; i++) {
			Element subElem = (Element) doc.createElement("subtitle");
			XmlUtils.addChildText(doc, subElem, "text", subtitle);
			XmlUtils.addChildDouble(doc, subElem, "x", point[i].x);
			XmlUtils.addChildDouble(doc, subElem, "y", point[i].y);
			rootElement.appendChild(subElem);
		}

		XmlUtils.writeXmlDocument(doc, filename);
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
	
	public static void saveMovie(WorldState[] states, String dir, String[] subtitles, Vector2D[][] points) {
		for (int i = 0; i < states.length; i++)
			writeWorldState(states[i], dir+"/frame"+i+".xml", subtitles[i], points[i]);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(WorldState: BALL=" + getBallCoords() + ", BLUE=" + getBlueRobot()
				+ ", YELLOW=" + getYellowRobot() + ")";
	}
}
