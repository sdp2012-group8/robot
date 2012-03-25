package sdp.common.geometry;

import java.awt.geom.Point2D;

import sdp.common.Utilities;

/**
 * A two-dimensional vector.
 * 
 * @author Martin Marinov
 */
public class Vector2D extends Point2D.Double {
	
	/** Required by Serializable class. */
	private static final long serialVersionUID = -6978991646878714685L;
	
	
	/**
	 * Create a vector from the given coordinates.
	 * 
	 * @param x X coordinate value.
	 * @param y Y coordinate value.
	 */
	public Vector2D(double x, double y) {
		super(x, y);
	}
	
	/**
	 * Create a vector from a 2D point.
	 * 
	 * @param point Point that corresponds to the vector.
	 */
	public Vector2D(Point2D.Double point) {
		super(point.getX(), point.getY());
	}
	
	/**
	 * The copy constructor.
	 * 
	 * @param other Vector to copy.
	 */
	public Vector2D(Vector2D other) {
		super(other.getX(), other.getY());
	}
	
	
	/**
	 * Get a new zero vector.
	 * 
	 * @return A zero vector.
	 */
	public static final Vector2D ZERO() {
		return new Vector2D(0, 0);
	}
	
	
	/**
	 * Set a new value of the X coordinate.
	 * 
	 * @param newX New value of the X coordinate.
	 */
	public void setX(double newX) {
		this.x = newX;
	}
	
	/**
	 * Set a new value of the Y coordinate.
	 * 
	 * @param newY New value of the Y coordinate.
	 */
	public void setY(double newY) {
		this.y = newY;
	}
	

	/**
	 * Get the length of the vector.
	 * 
	 * @return Length of the vector.
	 */
	public double getLength() {
		return Point2D.distance(0, 0, getX(), getY());
	}
	
	/**
	 * Get the direction of the vector.
	 * 
	 * @return Direction of the vector, in degrees.
	 */
	public double getDirection() {
		return Math.atan2(-this.y, this.x) * 180 / Math.PI;
	}
	
	
	/**
	 * Add two vectors together.
	 * 
	 * @param a First summand.
	 * @param b Second summand.
	 * @return The sum of two vectors.
	 */
	public static Vector2D add(Vector2D a, Vector2D b) {
		return new Vector2D(a.x + b.x, a.y + b.y);
	}
	
	/**
	 * Subtract vectors.
	 * 
	 * @param a Vector to subtract from.
	 * @param b Vector to subtract.
	 * @return Difference of the two vectors.
	 */
	public static Vector2D subtract(Vector2D a, Vector2D b) {
		return new Vector2D(a.x - b.x, a.y - b.y);
	}
	
	/**
	 * Multiply a vector by a scalar.
	 * 
	 * @param a Vector to multiply.
	 * @param k Scalar to multiply by.
	 * @return Scaled vector.
	 */
	public static Vector2D multiply(Vector2D a, double k) {
		return new Vector2D(a.x * k, a.y * k);
	}
	
	/**
	 * Divide a vector by a scalar.
	 * 
	 * @param a Vector to divide.
	 * @param k Scalar to divide by.
	 * @return Scaled vector.
	 */
	public static Vector2D divide(Vector2D a, double k) {
		if (Utilities.areDoublesEqual(k, 0.0)) {
			// TODO: Handle division by zero in some meaningful way.
		}
		return new Vector2D(a.x / k, a.y / k);
	}
	
	
	/**
	 * Multiply the given vector by a scalar and add it to this vector.
	 * 
	 * @param vector Vector to add.
	 * @param coeff Scale coefficient of the source vector.
	 */
	public void addMul(Vector2D vector, double coeff) {
		this.x += vector.x * coeff;
		this.y += vector.y * coeff;
	}
	
	
	/**
	 * Get a unit vector in the direction of the given vector.
	 * 
	 * @param vector Vector of interest.
	 * @return Vector's normal.
	 */
	public static Vector2D getNormal(Vector2D vector) {
		return divide(vector, vector.getLength());
	}
	
	
	/**
	 * Get a vector, perpendicular to the given one.
	 * 
	 * @param vector Vector of interest.
	 * @return Perpendicular vector.
	 */
	public static Vector2D getPerpendicular(Vector2D vector) {
		return new Vector2D(-vector.y, vector.x);
	}
	
	
	/**
	 * Get a vector in same direction but with different length.
	 * 
	 * @param vector Vector, whose direction should be used.
	 * @param newLength New vector length.
	 * @return A vector as described above.
	 */
	public static Vector2D changeLength(Vector2D vector, double newLength) {
		double oldLength = vector.getLength();
		
		if (Utilities.areDoublesEqual(oldLength, 0.0)) {
			return ZERO();
		} else {		
			return Vector2D.multiply(vector, newLength / oldLength);
		}
	}
	
	/**
	 * Get angle between two vectors.
	 * 
	 * @param a First vector of interest.
	 * @param b Second vector of interest.
	 * @return The angle between the two vectors, in degrees.
	 */
	public static double getBetweenAngle(Vector2D a, Vector2D b) {
		return Math.atan2(-a.y + b.y, a.x - b.x) * 180 / Math.PI;
	}
	
	
	/**
     * Rotate vector p2 around origin by the given angle in degrees.
     * 
     * @param vector Vector to rotate.
     * @param angle Angle of rotation in degrees.
     * @return Rotated point.
     */
    public static Vector2D rotateVector(Vector2D vector, double angle) {
    	return new Vector2D(GeomUtils.rotatePoint(new Point2D.Double(0.0, 0.0), vector, angle));
    }
    
    /**
     * Translate current vector by some offset.
     * 
     * @param offset Translation offset.
     */
    public void translateVector(Vector2D offset) {
    	this.x += offset.x;
    	this.y += offset.y;
    }
    
    
    /**
     * Get a unit vector, pointing into the specified direction.
     * 
     * @param angle Direction in degrees.
     * @return Unit vector with the specified angle.
     */
    public static Vector2D getDirectionUnitVector(double angle) {
    	return Vector2D.rotateVector(new Vector2D(1.0, 0.0), angle);
    }
    
    
	/**
	 * Convert vector into a human-readable string form.
	 * 
	 * @return Human-readable string.
	 */
	public String toString() {
		return "(" + String.format("%.2f", getX()).replace(',', '.') + ", "
				+ String.format("%.2f", getY()).replace(',', '.') + ")";
	}
	
	
	/**
	 * Get the dot product of two vectors.
	 * 
	 * TODO: Replace with dotProduct found in utilities.
	 * 
	 * @param a First operand.
	 * @param b Second operand.
	 * @return Dot product of two vectors.
	 */
	@Deprecated
	public static double dot(Vector2D a, Vector2D b) {
		return GeomUtils.dotProduct(a, b);
	}
	
	/**
	 * Get the direction of the vector.
	 * 
	 * TODO: Use an instance method instead of this. It makes no sense to
	 * access vector's length and direction using different methods.
	 * 
	 * @param a Vector in question.
	 * @return Direction of the said vector.
	 */
	@Deprecated
	public static double getDirection(Vector2D a) {
		return a.getDirection();
	}
}
