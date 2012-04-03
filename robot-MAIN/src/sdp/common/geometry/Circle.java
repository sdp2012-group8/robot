package sdp.common.geometry;

import java.awt.geom.Point2D;

import sdp.common.Utilities;


/**
 * A circle.
 */
public class Circle {
	
	/** Centre of the circle. */
	private Point2D.Double centre;
	/** Radius of the circle. */
	private double radius;
	
	
	/**
	 * Create a new circle.
	 * 
	 * @param centre Location of the centre of the circle.
	 * @param radius Radius of the circle.
	 */
	public Circle(Point2D.Double centre, double radius) {
		this.centre = centre;
		this.radius = radius;
	}


	/**
	 * Get the centre of the circle.
	 * 
	 * @return Centre of the circle.
	 */
	public Point2D.Double getCentre() {
		return centre;
	}

	/**
	 * Set the centre of the circle.
	 * 
	 * @param centre New centre of the circle.
	 */
	public void setCentre(Point2D.Double centre) {
		this.centre = centre;
	}


	/**
	 * Get the radius of the circle.
	 * 
	 * @return Radius of the circle.
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * Set the radius of the circle.
	 * 
	 * @param radius New radius of the circle.
	 */
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	
	/**
	 * Check whether some point is contained by this circle.
	 * 
	 * @param point Point of interest.
	 * @return Whether the specified point is inside the circle.
	 */
	public boolean containsPoint(Point2D.Double point) {
		return (Point2D.distance(point.x, point.y, centre.x, centre.y) <= radius);
	}


	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Circle) {
			Circle other = (Circle) obj;
			return (Utilities.areDoublesEqual(centre.x, other.centre.x)
					&& Utilities.areDoublesEqual(centre.y, other.centre.y)
					&& Utilities.areDoublesEqual(radius, other.radius));
		} else {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("(CIRCLE: (%.4f, %.4f), %.4f)", centre.x, centre.y, radius);
	}

}
