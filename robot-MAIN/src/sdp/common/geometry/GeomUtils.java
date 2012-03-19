package sdp.common.geometry;

import java.awt.Point;
import java.awt.geom.Point2D;

import sdp.common.Utilities;


/**
 * A container for geometry-related functions.
 */
public class GeomUtils {
	
	/**
	 * Add two points.
	 * 
	 * @param a First point to add.
	 * @param b Second point to add.
	 * @return Summand of the two points.
	 */
	public static Point2D.Double addPoints(Point2D.Double a, Point2D.Double b) {
		return new Point2D.Double(a.x + b.x, a.y + b.y);
	}

	/**
	 * Subtract one point from another one.
	 * 
	 * @param a Point to subtract from (minuend).
	 * @param b Point to subtract (subtrahend).
	 * @return Difference of the two points.
	 */
	public static Point2D.Double subtractPoints(Point2D.Double a, Point2D.Double b) {
		return new Point2D.Double(a.x - b.x, a.y - b.y);
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
	 * Get Cartesian distance between two points.
	 * 
	 * @param a First point of interest.
	 * @param b Second point of interest.
	 * @return Distance between the points.
	 */
	public static double pointDistance(Point2D.Double a, Point2D.Double b) {
		return Point2D.distance(a.x, a.y, b.x, b.y);
	}
	

	/**
	 * Checks whether a point is contained within some triangle.
	 * 
	 * @param p A point of interest.
	 * @param a First corner of a triangle.
	 * @param b Second corner of a triangle.
	 * @param c Third corner of a triangle.
	 * @return Whether a point is within a triangle.
	 */
	public static boolean isPointInTriangle(Point2D.Double p, Point2D.Double a,
			Point2D.Double b, Point2D.Double c) {
		return (GeomUtils.doesSegmentIntersectLine(p, a, b, c)
				&& GeomUtils.doesSegmentIntersectLine(p, b, a, c)
				&& GeomUtils.doesSegmentIntersectLine(p, c, a, b));
	}
	
	/**
	 * Checks whether a point is contained within some quadrilateral.
	 * 
	 * This function assumes that the quadrilateral in question is convex and
	 * that the vertices are passed in clockwise order.
	 * 
	 * @param p A point of interest.
	 * @param q1 First vertex of the quadrilateral.
	 * @param q2 Second vertex of the quadrilateral.
	 * @param q3 Third vertex of the quadrilateral.
	 * @param q4 Fourth vertex of the quadrilateral.
	 * @return Whether the point is inside the quadrilateral.
	 */
	public static boolean isPointInQuadrilateral(Point2D.Double p, Point2D.Double q1,
			Point2D.Double q2, Point2D.Double q3, Point2D.Double q4) {
		return (GeomUtils.isPointInTriangle(p, q1, q2, q3)
				|| GeomUtils.isPointInTriangle(p, q3, q4, q1));
	}
	

	/**
	 * Find the point where two lines intersect. If such point does not exist,
	 * null is returned instead.
	 * 
	 * @param l1pt1 First point on the first line.
	 * @param l1pt2 Second point on the first line.
	 * @param l2pt1 First point on the second line.
	 * @param l2pt2 Second point on the second line.
	 * @return Point of intersection of the lines.
	 */
	public static Point2D.Double getLineIntersection(Point2D.Double l1pt1, Point2D.Double l1pt2,
			Point2D.Double l2pt1, Point2D.Double l2pt2) {
		Point2D.Double l1dir = GeomUtils.subtractPoints(l1pt1, l1pt2);
		Point2D.Double l2dir = GeomUtils.subtractPoints(l2pt1, l2pt2);
		double denom = GeomUtils.crossProduct(l1dir, l2dir);
		
		if (Utilities.areDoublesEqual(denom, 0.0)) {
			return null;
		} else {
			double l1cp = GeomUtils.crossProduct(l1pt1, l1pt2);
			double l2cp = GeomUtils.crossProduct(l2pt1, l2pt2);
			
			double xi = (l2dir.x * l1cp - l1dir.x * l2cp) / denom;
			double yi = (l2dir.y * l1cp - l1dir.y * l2cp) / denom;
			return new Point2D.Double(xi, yi);
		}
	}

	/**
	 * Check if a line segment intersects the given line.
	 * 
	 * @param segPt1 First endpoint of the line segment.
	 * @param segPt2 Second endpoint of the line segment.
	 * @param linePt1 First point on the line.
	 * @param linePt2 Second point on the line.
	 * @return Whether the line segment intersects the line.
	 */
	public static boolean doesSegmentIntersectLine(Point2D.Double segPt1, Point2D.Double segPt2,
			Point2D.Double linePt1, Point2D.Double linePt2){
		double cp1 = GeomUtils.crossProduct(GeomUtils.subtractPoints(linePt2, linePt1),
				GeomUtils.subtractPoints(segPt1, linePt1));
		double cp2 = GeomUtils.crossProduct(GeomUtils.subtractPoints(linePt2, linePt1),
				GeomUtils.subtractPoints(segPt2, linePt1));
		return ((cp1 * cp2) >= 0);
	}

	/**
	 * Get a cross product of two 2D vectors. The value returned is the cross
	 * product's magnitude.
	 * 
	 * @param a First operand vector.
	 * @param b Second operand vector.
	 * @return Cross product of the two vectors.
	 */
	public static double crossProduct(Point2D.Double a, Point2D.Double b) {
		return (a.x * b.y) - (a.y * b.x);
	}

	/**
	 * Get a cross product of two 3D vectors.
	 * 
	 * @param a First operand vector.
	 * @param b Second operand vector.
	 * @return Cross product of the two vectors.
	 */
	public static Point3D crossProduct(Point3D a, Point3D b) {
		return new Point3D((a.y * b.z) - (a.z * b.y), (a.x * b.z) - (a.z * b.x), (a.x * b.y) - (a.y * b.x));
	}

	/**
	 * Get the dot product of two 2D point.
	 * 
	 * @param a First operand.
	 * @param b Second operand.
	 * @return Dot product of the points.
	 */
	public static double dotProduct(Point2D.Double a, Point2D.Double b) {
		return (a.x * b.x) + (a.y * b.y);
	}

	/**
	 * Get the dot product of two 3D points.
	 * 
	 * @param a First operand.
	 * @param b Second operand.
	 * @return Dot product of the points.
	 */
	public static double dotProduct(Point3D a, Point3D b) {
		return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
	}



	/**
	 * Takes points A, B and C and calculates the angle BAC.
	 * 
	 * @param a Point A.
	 * @param b Point B.
	 * @param c Point C.
	 * @return Angle BAC.
	 */
	public static double getAngle(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
		double ab = GeomUtils.pointDistance(a,b);
		double ac = GeomUtils.pointDistance(a,c);
		double bc = GeomUtils.pointDistance(b,c);
	
		return Math.acos((ac * ac + ab * ab - bc * bc) / (2 * ac * ab));
	}

}
