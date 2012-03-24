package sdp.common;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


/**
 * The state of the field.
 */
public class WorldState {
	
	/** Height of the pitch in centimetres. */
	public static final double PITCH_HEIGHT_CM = 113.7;
	/** Width of the pitch in centimetres. */
	public static final double PITCH_WIDTH_CM = 244;
	
	/** Height of the goals in centimetres. */
	public static final double GOAL_HEIGHT_CM = PITCH_HEIGHT_CM / 2;


	/** Location of the ball. */
	private Point2D.Double ballCoords;
	/** The blue robot. */
	private Robot blueRobot;
	/** The yellow robot. */
	private Robot yellowRobot;
	
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
	 * Get the image of the world.
	 * 
	 * @return The image of the world.
	 */
	public final BufferedImage getWorldImage() {
		return worldImage;
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
	public void update(Point2D.Double ballCoords, Robot blueRobot, Robot yellowRobot, BufferedImage worldImage) {
		this.ballCoords = ballCoords;
		this.blueRobot = blueRobot;
		this.yellowRobot = yellowRobot;
		this.worldImage = worldImage;
	}
}
