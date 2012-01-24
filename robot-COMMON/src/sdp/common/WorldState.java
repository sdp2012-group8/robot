package sdp.common;

import java.awt.Point;


/**
 * The state of the field.
 * 
 * @author Gediminas Liktaras
 */
public class WorldState {

	/** Location of the ball. */
	private Point ballCoords;
	/** The blue robot. */
	private Robot blueRobot;
	/** The yellow robot. */
	private Robot yellowRobot;
	
	
	/**
	 * The main constructor.
	 * 
	 * @param ballCoords Coordinates of the ball.
	 * @param blueRobot The blue robot.
	 * @param yellowRobot The yellow robot.
	 */
	public WorldState(Point ballCoords, Robot blueRobot, Robot yellowRobot) {
		this.ballCoords = ballCoords;
		this.blueRobot = blueRobot;
		this.yellowRobot = yellowRobot;
	}

	
	/**
	 * Get the location of the ball.
	 * 
	 * @return The location of the ball.
	 */
	public Point getBallCoords() {
		return ballCoords;
	}

	/**
	 * Get the blue robot.
	 * 
	 * @return The blue robot.
	 */
	public Robot getBlueRobot() {
		return blueRobot;
	}

	/**
	 * Get the yellow robot.
	 * 
	 * @return The yellow robot.
	 */
	public Robot getYellowRobot() {
		return yellowRobot;
	}
	
}
