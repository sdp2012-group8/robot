package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Contains various utility methods, which do not fit anywhere else.
 */
public class Utilities {
	
	private static final double POINT_OFFSET = 2*Robot.LENGTH_CM;

	/**
	 * Return a deep copy of the given BufferedImage.
	 * 
	 * Taken from
	 * http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage.
	 * 
	 * @param image BufferedImage to copy.
	 * @return A deep copy of image.
	 */
	public static BufferedImage deepBufferedImageCopy(BufferedImage image) {
		ColorModel cm = image.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = image.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	
	/**
	 * Convert java.awt.geom.Point2D to java.awt.Point.
	 * @param pt
	 * @return
	 */
	public static Point pointFromPoint2D(Point2D pt) {
		return new Point((int)pt.getX(), (int)pt.getY());
	}
	
	/**
     * Rotate point p2 around point p1 by the given angle in degrees.
     * 
     * @param origin Rotation point.
     * @param point Point to rotate.
     * @param angle Angle of rotation in degrees.
     * @return Rotated point.
     */
    public static Point2D.Double rotatePoint(Point2D.Double origin,
    		Point2D.Double point, double degrees)
    {
    	double radAngle = Math.toRadians(degrees);
    	
    	double xDiff = point.x - origin.x;
    	double yDiff = point.y - origin.y;
    	
    	double rotX = (xDiff * Math.cos(radAngle)) - (yDiff * Math.sin(-radAngle)) + origin.x;
    	double rotY = (xDiff * Math.sin(-radAngle)) + (yDiff * Math.cos(radAngle)) + origin.y;
    	
    	return new Point2D.Double(rotX, rotY);
    }
    
    /**
     * Rotate point p2 around point p1 by the given angle in degrees.
     * 
     * @param origin Rotation point.
     * @param point Point to rotate.
     * @param angle Angle of rotation in degrees.
     * @return Rotated point.
     */
    public static Point rotatePoint(Point origin, Point point, double angle) {
    	Point2D.Double origin_f = new Point2D.Double(origin.x, origin.y);
    	Point2D.Double point_f = new Point2D.Double(point.x, point.y);
    	Point2D.Double retValue_f = rotatePoint(origin_f, point_f, angle);
    	
    	return new Point((int)retValue_f.x, (int)retValue_f.y);
    }
    
    
    /**
     * Translate the given point by some offset.
     * 
     * @param point The point to translate.
     * @param offset Translate offset.
     */
    public static void translatePoint(Point2D.Double point, Point2D.Double offset) {
    	point.x += offset.x;
    	point.y += offset.y;
    }
    
    
    /**
     * Check whether the given value is within specified bounds.
     * 
     * If lower > upper, the function checks if the given value is within the
     * (-INF; upper] OR [lower; +INF) interval.
     * 
     * @param value Value to check.
     * @param lower Lower bound of the interval.
     * @param upper Upper bound of the interval.
     * @return Whether the value is within the specified interval.
     */
    public static boolean valueWithinBounds(int value, int lower, int upper) {
    	if (lower > upper) {
    		return ((value >= lower) || (value <= upper));
    	} else {
    		return ((value >= lower) && (value <= upper));
    	}
    }


	/**
	 * Normalizes the given angle
	 * @param initial given angle in degrees
	 * @return normalized angle between -Pi and Pi
	 */
	public static double normaliseAngle(double initial) {
		initial = initial % 360;
		if (initial > 180)
			initial -= 360;
		if (initial < -180)
			initial += 360;
		return initial;
	}


	/**
	 * Calculates if a point p is within the triangle abc
	 * @param p Point in triangle
	 * @param a
	 * @param b
	 * @param c
	 * @return returns true if point is in triangle, false otherwise
	 */
	public static boolean pointInTriangle(Point2D.Double p, Point2D.Double a, Point2D.Double b, Point2D.Double c) {
		if (Utilities.sameSide(p,a, b,c) && Utilities.sameSide(p,b, a,c) && Utilities.sameSide(p,c, a,b)) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Determines if a robot is in the way of the path AB
	 * @param A First point of line
	 * @param B Second point of line
	 * @param robot Robot to test against
	 * @return returns true if the path is clear
	 */
	public static boolean isPathClear(Point2D.Double A, Point2D.Double B, Robot robot) {
		boolean diagonal1 = Utilities.sameSide(robot.getBackLeft(), robot.getFrontRight(), A, B);
		boolean diagonal2 = Utilities.sameSide(robot.getFrontLeft(), robot.getBackRight(), A, B);
		return (diagonal1 && diagonal2);
	}


	/**
	 * Calculates the point of intersection between two lines given 4 points
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @return Returns the point of intersection or null if none exist
	 */
	public static Point2D.Double intersection(Point2D.Double a, Point2D.Double b, Point2D.Double c, Point2D.Double d) {
		double D = (a.x-b.x)*(c.y-d.y) - (a.y-b.y)*(c.x-d.x);
		if (D == 0) return null;
		double xi = ((c.x-d.x)*(a.x*b.y-a.y*b.x)-(a.x-b.x)*(c.x*d.y-c.y*d.x))/D;
		double yi = ((c.y-d.y)*(a.x*b.y-a.y*b.x)-(a.y-b.y)*(c.x*d.y-c.y*d.x))/D;
	
		return new Point2D.Double(xi,yi);
	}


	public static boolean sameSide(Point2D.Double p1, Point2D.Double p2, Point2D.Double a, Point2D.Double b){
		Point3D cp1 = Utilities.crossProduct(Utilities.pointSubtract(b,a), Utilities.pointSubtract(p1,a));
		Point3D cp2 = Utilities.crossProduct(Utilities.pointSubtract(b,a), Utilities.pointSubtract(p2,a));
		if (Utilities.dotProduct(cp1, cp2) >= 0) {
			return true;
		} else {
			return false;
		}
	}


	public static Point3D crossProduct(Point2D.Double a, Point2D.Double b) {
		return Utilities.crossProduct(new Point3D(a.x,a.y,0), new Point3D(b.x,b.y,0));
	}


	public static Point3D crossProduct(Point3D a, Point3D b) {
		return new Point3D(a.y*b.z - a.z*b.y, a.x*b.z - a.z*b.x, a.x*b.y - a.y*b.x);
	}


	public static double dotProduct(Point3D a, Point3D b) {
		return a.x*b.x + a.y*b.y + a.z*b.z;
	}


	public static Point2D.Double pointSubtract(Point2D.Double a, Point2D.Double  b) {
		return new Point2D.Double(a.x-b.x, a.y-b.y);
	}


	/**
	 * Calculates the angle BAC
	 * @param A
	 * @param B
	 * @param C
	 * @return angle BAC
	 */
	public static double getAngle(Point2D.Double A, Point2D.Double B, Point2D.Double C) {
		double angle = 0d;
		double AB = Tools.getDistanceBetweenPoint(A,B);
		double AC = Tools.getDistanceBetweenPoint(A,C);
		double BC = Tools.getDistanceBetweenPoint(B,C);
		
		angle = Math.acos((AC*AC + AB*AB - BC*BC)/(2*AC*AB));
		
		return angle;
	}
    
	/**
	 * converts to a byte
	 * @param angle
	 * @return double between -128 and 127
	 */
	public static byte normaliseToByte(double angle){
		if (angle > 127)
			angle = 127;
		if (angle < -128)
			angle = -128;
		return (byte)angle;
	}

    
    /**
     * Strip a string from whitespace.
     * 
     * Adapted from http://www.java2s.com/Code/Java/Data-Type/stripstring.htm.
     * 
     * @param string String to strip.
     * @return Stripped string.
     */
    public static String stripString(String string) {
    	if ((string == null) || (string.length() == 0)) {
    		return string;
    	}
    	        
        int start = 0;
        while((start < string.length()) && Character.isWhitespace(string.charAt(start))) {
            start++;
        }
        
        int end = string.length();
        while((end > start) && Character.isWhitespace(string.charAt(end - 1))) {
            end--;
        }
        
        if (start == end) {
        	return "";
        } else {
        	return string.substring(start, end);
        }
    }
    
    /**
     * Calculates if the given point is within the field.
     * @param point The point to be checked
     * @return True if the point is within the bounds of the field.
     */
    public static boolean isPointInField(Point2D.Double point) {
    	if (point.getX() >= 0 && point.getX() <= WorldState.PITCH_WIDTH_CM) {
    		if (point.getY() >= 0 && point.getY() <= WorldState.PITCH_HEIGHT_CM) {
    			return true;
    		}
    	}
		return false;
    }
    
    
    /**
	 * Returns the point the robot should go to behind the ball.
	 * Distance behind the ball set by POINT_OFFSET
	 * @param point The target point on the goal the robot should be aligned to.
	 * @return Point2D.Double behind the ball
	 */
	public static Point2D.Double getPointBehindBall(Point2D.Double point, Point2D.Double ball, boolean my_goal_left) {

		if (point.getY() == ball.getY()) {
			return new Point2D.Double(my_goal_left ? ball.getX() - POINT_OFFSET : ball.getX() + POINT_OFFSET, ball.getY());
		} else {
			double x, y, a, b;
			a = point.getY() - ball.getY();
			b = point.getX() - ball.getX();

			if (my_goal_left) {
				y = ball.getY() - POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() + (b*(y - ball.getY())/a);
			} else {
				y = ball.getY() + POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() - (b*(y - ball.getY())/a);
			}

			//x = ball.getX() + (b*(y - ball.getY())/a);

			return new Point2D.Double(x,y);
		}
	}


	/**
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * @return The point closest to the robot that would allow it to shoot.
	 * @throws NullPointerException Throws exception when the robot can't see a goal.
	 */
	public static Point2D.Double getOptimalPointBehindBall(WorldState ws, boolean my_goal_left, boolean my_team_blue) throws NullPointerException {
		Goal enemy_goal;
		Robot robot, enemy_robot;
		
		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_Y_CM ));
		}

		//update variables
		if (my_team_blue) {
			robot = ws.getBlueRobot();
			enemy_robot = ws.getYellowRobot();
		} else {
			robot = (ws.getYellowRobot());
			enemy_robot = (ws.getBlueRobot());
		}
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = WorldState.PITCH_WIDTH_CM*2;

		goal_points.add(enemy_goal.getCentre());
		goal_points.add(enemy_goal.getTop());
		goal_points.add(enemy_goal.getBottom());
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y - WorldState.PITCH_HEIGHT_CM));
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y + WorldState.PITCH_HEIGHT_CM));

		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			if (!Utilities.isPathClear(point, ws.getBallCoords(), enemy_robot)) {
				itr.remove();
			}
		}

		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = getPointBehindBall(point, ws.getBallCoords(), my_goal_left);
			
			if (Utilities.isPointInField(temp_point)) { 
				if (!isPointInRobot(temp_point, enemy_robot)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				}
			}
		}
		
		//System.out.println(min_point);

		return min_point;
	}
	
	/**
	 * Helper function to find if a specific point is within the enemy robot.
	 * @param point The point to check.
	 * @return Returns true if the point is within the enemy robot.
	 */
	public static boolean isPointInRobot(Point2D.Double point, Robot enemy_robot) {
		if (Utilities.pointInTriangle(point, enemy_robot.getFrontLeft(), enemy_robot.getFrontRight(), enemy_robot.getBackLeft()) && 
				Utilities.pointInTriangle(point, enemy_robot.getBackLeft(), enemy_robot.getBackRight(), enemy_robot.getFrontRight())){
			return true;
		}
		return false;
	}

    
}
