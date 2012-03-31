package sdp.common.geometry;

import java.awt.Point;
import java.awt.geom.Point2D;

import sdp.common.Utilities;


/**
 * A container for geometry-related functions.
 */
public class GeomUtils {
	
	/**
	 * Ensure that the given angle in degrees is within the interval [-180; 180).
	 * 
	 * @param angle Angle, in degrees.
	 * @return Normalised angle, as described above.
	 */
	public static double normaliseAngle(double angle) {
		angle = angle % 360;
		if (angle > 180) {
			angle -= 360;
		}
		if (angle < -180) {
			angle += 360;
		}
		
		return angle;
	}
	
	
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
	 * Check whether coordinates of the given point are both negative.
	 * 
	 * @param point Point in question.
	 * @return Whether both x and y coordinates are negative.
	 */
	public static boolean isPointNegative(Point2D.Double point) {
		return ((point.x < 0) && (point.y < 0));
	}
	
	
	/**
	 * Check whether a point is contained within an axis-aligned box.
	 * 
	 * @param p A point of interest.
	 * @param a One of the corners of the box.
	 * @param b The opposite corner of the box.
	 * @return Whether a point is within the box.
	 */
	public static boolean isPointInAxisAlignedBox(Point2D.Double p, Point2D.Double a,
			Point2D.Double b) {
		double xLow = Math.min(a.x, b.x);
		double xHigh = Math.max(a.x, b.x);
		double yLow = Math.min(a.y, b.y);
		double yHigh = Math.max(a.y, b.y);
		
		return ((xLow <= p.x) && (p.x <= xHigh) && (yLow <= p.y) && (p.y <= yHigh)); 
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
	public static Point2D.Double getLineLineIntersection(Point2D.Double l1pt1, Point2D.Double l1pt2,
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
	 * Return the intersection of a ray with a line segment in ray-local
	 * coordinates. If there is no intersection, null is returned instead.
	 * 
	 * @param rayOrigin Origin of the ray.
	 * @param rayDir Direction of the ray.
	 * @param segPt1 First endpoint of the line segment.
	 * @param segPt2 Second endpoint of the line segment.
	 * @return Intersection coordinates in ray-local coordinates.
	 */
	public static Vector2D getLocalRaySegmentIntersection(Point2D.Double rayOrigin,
			Vector2D rayDir, Point2D.Double segPt1, Point2D.Double segPt2) {
		Vector2D segPt1Local = new Vector2D(GeomUtils.getLocalPoint(rayOrigin, rayDir, segPt1));
		Vector2D segPt2Local = new Vector2D(GeomUtils.getLocalPoint(rayOrigin, rayDir, segPt2));
		
		if ((segPt1Local.x < 0) && (segPt2Local.x < 0)) {
			return null;
		}
		
		if (segPt1Local.y * segPt2Local.y <= 0 ) {
			double slope = (segPt2Local.x - segPt1Local.x) / (segPt2Local.y - segPt1Local.y);
			double intDist = segPt1Local.x - (segPt1Local.y * slope);
			
			if (intDist < 0) {
				return null;
			} else {
				return Vector2D.changeLength(rayDir, intDist);
			}
		} else {
			return null;
		}
	}
	
	
	/**
	 * Get the closest point from a point to a line.
	 * 
	 * @param p Point of interest.
	 * @param a First point on the line.
	 * @param b Second point on the line.
	 * @return Point on the line ab that is closest to p.
	 */
	public static Point2D.Double getClosestPointToLine(Point2D.Double p,
			Point2D.Double a, Point2D.Double b) {
		Vector2D ap = Vector2D.subtract(new Vector2D(p), new Vector2D(a));
		Vector2D ab = Vector2D.subtract(new Vector2D(b), new Vector2D(a));
		double dot = GeomUtils.dotProduct(ap, ab);
		
		double abDistSq = Point2D.distanceSq(a.x, a.y, b.x, b.y);
		
		double t = dot / abDistSq;
		
		return new Point2D.Double(a.x + ab.x * t, a.y + ab.y * t);
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
	 * @return Angle BAC, in radians.
	 */
	public static double getAngle(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
		double ab = GeomUtils.pointDistance(a,b);
		double ac = GeomUtils.pointDistance(a,c);
		double bc = GeomUtils.pointDistance(b,c);
	
		return Math.acos((ac * ac + ab * ab - bc * bc) / (2 * ac * ab));
	}
	
	/**
	 * Get angular difference between two angles, expressed in degrees.
	 * 
	 * @param angle1 First angle.
	 * @param angle2 Second angle.
	 * @return Difference between two angles.
	 */
	public static double getAngleDifference(double angle1, double angle2) {
		double diff = (GeomUtils.normaliseAngle(angle1) - GeomUtils.normaliseAngle(angle2) + 360) % 360;
		diff = Math.min(diff, 360 - diff);
		return diff;
	}
	
	
	/**
	 * Convert a point from some local coordinate system to the global one.
	 * 
	 * @param refOrigin Local system's origin.
	 * @param refDir Local system's 0 degree direction vector.
	 * @param localPoint Local point to convert.
	 * @return A corresponding global point.
	 */
	public static Point2D.Double getGlobalPoint(Point2D.Double refOrigin, Vector2D refDir,
			Point2D.Double localPoint) {
		Point2D.Double globalPoint = GeomUtils.rotatePoint(new Point2D.Double(0.0, 0.0),
				localPoint, refDir.getDirection());
		globalPoint = GeomUtils.addPoints(globalPoint, refOrigin);
		return globalPoint;
	}
	
	/**
	 * Convert a point from the global coordinate system to some local one.
	 * 
	 * @param refOrigin Local system's origin.
	 * @param refDir Local system's 0 degree direction vector.
	 * @param globalPoint Global point to convert.
	 * @return A corresponding local point.
	 */
	public static Point2D.Double getLocalPoint(Point2D.Double refOrigin, Vector2D refDir,
			Point2D.Double globalPoint) {
		Point2D.Double localPoint = GeomUtils.subtractPoints(globalPoint, refOrigin);
		localPoint = GeomUtils.rotatePoint(new Point2D.Double(0.0, 0.0),
				localPoint, -refDir.getDirection());
		return localPoint;
	}

	
	/**
	 * Return the distance to the closest point in the set
	 * @param points set of points
	 * @param origin the point we are standing at
	 * @return the distance from my point to the closest one in the set
	 */
	public static Vector2D getMinVectorToPoints(Vector2D[] points, Vector2D origin) {
		Vector2D minVec = null;
		
		for (int i = 0; i < points.length; i++) {
			Vector2D curVec = Vector2D.subtract(points[i], origin);
			if ((minVec == null) || (curVec.getLength() < minVec.getLength())) {
				minVec = curVec;
			}
		}
		
		return minVec;
	}
	
	
	/**
	 * Get corners of a rectangle, positioned in the specified location.
	 * 
	 * @param width Width of the rectangle.
	 * @param height Height of the rectangle.
	 * @param pos Position of the rectangle.
	 * @param angle Direction the rectangle is facing, in angles.
	 * @return Angles of the positioned rectangle. Returned values are front
	 * 		left, front right, back right and back left point, in an array,
	 * 		in that order.
	 */
	public static Point2D.Double[] positionRectangle(double width,
			double height, Point2D.Double pos, double angle) {
		Point2D.Double frontLeftPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(width / 2, height / 2), angle);
		GeomUtils.translatePoint(frontLeftPoint, pos);
		
		Point2D.Double frontRightPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(width / 2, -height / 2), angle);
		GeomUtils.translatePoint(frontRightPoint, pos);
		
		Point2D.Double backLeftPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(-width / 2, height / 2), angle);
		GeomUtils.translatePoint(backLeftPoint, pos);
		
		Point2D.Double backRightPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(-width / 2, -height / 2), angle);
		GeomUtils.translatePoint(backRightPoint, pos);
		
		return new Point2D.Double[] {
				frontLeftPoint, frontRightPoint, backRightPoint, backLeftPoint
		};
	}


	/**
	 * Translate a point and change the distance from a circle to some point.
	 * 
	 * The resulting point will have the same direction relative to the circle's
	 * centre, but its distance will be the specified amount.
	 * 
	 * @param circle Circle of interest.
	 * @param point Point to move.
	 * @param newDist New circle-point distance.
	 * @return Another point, as described above.
	 */
	public static Point2D.Double changePointDistanceToCircle(Circle circle,
			Point2D.Double point, double newDist) {
		Vector2D circleToPoint = Vector2D.subtract(new Vector2D(point), new Vector2D(circle.getCentre()));
		Vector2D newPointOffset = Vector2D.changeLength(circleToPoint, newDist);
		Vector2D newPoint = Vector2D.add(new Vector2D(circle.getCentre()), newPointOffset);
		
		return newPoint;
	}
	
	/**
	 * Get the tangent line to circle intersection points.
	 * 
	 * What the above sentence means is that this function works like so:
	 * 1) Find two lines that are tangent to the given circle and go through
	 *    the given point.
	 * 2) Find the coordinates of the points where lines and the circle touch.
	 * 3) Return those intersection points.
	 * 
	 * If the specified point of interest is inside the circle, null is
	 * returned instead.
	 * 
	 * Helpful graphic:
	 * 
	 * -
	 *  ----
	 *  /---X---
	 * |     |  ----
	 * |  c  |      ----p
	 * |     |  ----
	 *  \---X---      c - Circle centre.
	 *  ----          p - Point of interest.
	 * -              X - Tangent line collision points.
	 * 
	 * @param circle Circle of interest.
	 * @param point Point of interest.
	 * @return Circle tangent line intersection points, as described above.
	 */
	public static Point2D.Double[] circleTangentPoints(Circle circle, Point2D.Double point) {
		double hypotenuse = pointDistance(circle.getCentre(), point);
		if (hypotenuse <= circle.getRadius()) {
			return null;
		}
		
		double shortLeg = circle.getRadius();
		double longLeg = Math.sqrt(hypotenuse * hypotenuse - shortLeg * shortLeg);		
		double angle = Math.toDegrees(Math.asin(circle.getRadius() / hypotenuse));
		
		Vector2D pointToCircle = Vector2D.subtract(new Vector2D(circle.getCentre()), new Vector2D(point));
		Vector2D tangentOffset = Vector2D.changeLength(pointToCircle, longLeg);
		
		Vector2D tangent1 = Vector2D.rotateVector(tangentOffset, angle);
		tangent1 = Vector2D.add(new Vector2D(point), tangent1);
		
		Vector2D tangent2 = Vector2D.rotateVector(tangentOffset, -angle);
		tangent2 = Vector2D.add(new Vector2D(point), tangent2);

		return new Point2D.Double[] { tangent1, tangent2 };
	}
}
