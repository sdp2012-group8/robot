package sdp.common;

import java.awt.geom.Point2D;


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
    public static Point2D.Double rotatePoint(Point2D.Double origin,
    		Point2D.Double point, double angle)
    {
    	double xDiff = point.x - origin.x;
    	double yDiff = point.y - origin.y;
    	
    	double rotX = (xDiff * Math.cos(angle)) - (yDiff * Math.sin(angle)) + origin.x;
    	double rotY = (xDiff * Math.sin(angle)) + (yDiff * Math.cos(angle)) + origin.y;
    	
    	return new Point2D.Double(rotX + origin.x, rotY + origin.y);
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

}
