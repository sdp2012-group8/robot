package sdp.common;

import java.awt.Point;


/**
 * This class represents a robot on the field.
 * 
 * @author Gediminas Liktaras
 */
public class Robot {
	
	/** Length of the robot's top plate. */
	public static final int LENGTH = 70;
	/** Width of the robot's top plate. */
	public static final int WIDTH = 50;

	/** Coordinate's of the robot's center on the field. */
	protected Point coords;
	/** The angle the robot is facing, in radians. */
	protected double angle;

	
	/**
	 * The main constructor.
	 * 
	 * @param coords The robot's coordinates.
	 * @param d The angle the robot is facing, in radians.
	 */
	public Robot(Point coords, double d) {
		super();
		this.coords = coords;
		this.angle = d;
	}

	
	/**
	 * Get coordinates of the robot's center.
	 * 
	 * @return Coordinates of the robot's center.
	 */
	public Point getCoords() {
		return coords;
	}

	/**
	 * Get the angle the robot is facing, in radians.
	 * 
	 * @return The angle the robot is facing, in radians.
	 */
	public double getAngle() {
		return angle;
	}

	
	/**
	 * Get coordinates of the front-left corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the front-left corner of the robot's rectangle.
	 */
	public Point getFrontLeft() {
		Point ret = Tools.rotatePoint(new Point(0, 0), new Point((int)LENGTH / 2, (int)WIDTH / 2), (int) Math.toDegrees(angle));
		ret.x += coords.x;
		ret.y += coords.y;
		return ret;
	}

	/**
	 * Get coordinates of the front-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the front-right corner of the robot's rectangle.
	 */
	public Point getFrontRight() {
		Point ret = Tools.rotatePoint(new Point(0, 0), new Point((int)LENGTH / 2, (int)-WIDTH / 2), (int) Math.toDegrees(angle));
		ret.x += coords.x;
		ret.y += coords.y;
		return ret;
	}

	/**
	 * Get coordinates of the back-left corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-left corner of the robot's rectangle.
	 */
	public Point getBackLeft() {
		Point ret = Tools.rotatePoint(new Point(0, 0), new Point((int)-LENGTH / 2, (int)WIDTH / 2), (int) Math.toDegrees(angle));
		ret.x += coords.x;
		ret.y += coords.y;
		return ret;
	}

	/**
	 * Get coordinates of the back-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-right corner of the robot's rectangle.
	 */
	public Point getBackRight() {
		Point ret = Tools.rotatePoint(new Point(0, 0), new Point((int)-LENGTH / 2, (int)-WIDTH / 2), (int) Math.toDegrees(angle));
		ret.x += coords.x;
		ret.y += coords.y;
		return ret;
	}

}