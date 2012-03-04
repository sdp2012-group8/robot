package sdp.common;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


/**
 * The state of the field.
 * 
 * @author Gediminas Liktaras
 */
public class WorldState {

	/** Location of the ball. */
	private Point2D.Double ballCoords;
	/** The blue robot. */
	private Robot blueRobot;
	/** The yellow robot. */
	private Robot yellowRobot;
	
	/** Picture of the world. */
	private BufferedImage worldImage;
	
	// pitch constants
	public final static double PITCH_WIDTH_CM = 244;
	public final static double PITCH_HEIGHT_CM = 113.7;
	public final static double GOAL_Y_CM = PITCH_HEIGHT_CM/2;
	
	/**
	 * The main constructor.
	 * 
	 * @param ballCoords Coordinates of the ball.
	 * @param blueRobot The blue robot.
	 * @param yellowRobot The yellow robot.
	 * @param worldImage The picture of the field.
	 */
	public WorldState(Point2D.Double ballCoords, Robot blueRobot, Robot yellowRobot, BufferedImage worldImage) {
		this.ballCoords = ballCoords;
		this.blueRobot = blueRobot;
		this.yellowRobot = yellowRobot;
		this.worldImage = worldImage;
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


	public void update(Point2D.Double ballCoords, Robot blueRobot, Robot yellowRobot, BufferedImage worldImage) {
		this.ballCoords = ballCoords;
		this.blueRobot = blueRobot;
		this.yellowRobot = yellowRobot;
		this.worldImage = worldImage;
	}
	
}
