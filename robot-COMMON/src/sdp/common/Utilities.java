package sdp.common;

import java.awt.Point;


/**
 * Contains various utility methods, which do not fit anywhere else.
 * 
 * @author Gediminas Liktaras
 */
public class Utilities {
	
	/**
     * Rotate point p2 around point p1 by the given angle in radians.
     * 
     * @param origin Rotation point.
     * @param point Point to rotate.
     * @param angle Angle of rotation in radians.
     * @return Rotated point.
     */
    public static Point rotatePoint(Point origin, Point point, double angle)
    {
    	int xDiff = point.x - origin.x;
    	int yDiff = point.y - origin.y;
    	
    	int rotX = (int) Math.round((xDiff * Math.cos(angle)) - (yDiff * Math.sin(angle))) + origin.x;
    	int rotY = (int) Math.round((xDiff * Math.sin(angle)) + (yDiff * Math.cos(angle))) + origin.y;
    	
    	return new Point(rotX + origin.x, rotY + origin.y);
    }

}
