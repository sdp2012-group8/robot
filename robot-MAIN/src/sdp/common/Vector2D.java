package sdp.common;

import java.awt.geom.Point2D;

/**
 * 
 * Takes care of vector/location operations
 * 
 * @author MartinMarinov
 *
 */
public class Vector2D extends Point2D.Double {
	
	
	private static final long serialVersionUID = -6978991646878714685L;
	
	
	/*    * useful constants * */
	
	public static final Vector2D ZERO() {
		return new Vector2D(0, 0);
	}
	
	/*    * constructors * */
	
	/**
	 * Clones the given vector
	 * @param vector_to_clone the vector to be cloned
	 */
	public Vector2D(Vector2D vector_to_clone) {
		super(vector_to_clone.getX(),vector_to_clone.getY());
	}
	
	/**
	 * Clones a point
	 * @param clone
	 */
	public Vector2D(Point2D.Double clone) {
		super(clone.getX(), clone.getY());
	}
	
	/**
	 * Creates a 2D vector (x, y) <br/><br/>
	 * The new vecor will be (x, y, 0)
	 * @param x
	 * @param y
	 */
	public Vector2D(double x, double y) {
		super(x, y);
	}
	

	/**
	 * Creates vector (tuple[0], tuple[1], tuple[2]) or (tuple[0], tuple[1]) depending on the size of tuple.
	 * @param tuple an array which first three items contain the x, y and z of the vector
	 */
	public Vector2D(double[] tuple) {
		super(tuple[0], tuple[1]);
	}
	
	public void setX(double value) {
		setLocation(value, getY());
	}
	
	public void setY(double value) {
		setLocation(getX(), value);
	}
	
	
	/**
	 * Adds the scaled source to the current vector
	 * @param source
	 * @param coefficient
	 */
	public void addmul_to(Vector2D source, double coefficient) {
		setLocation(getX()+source.getX()*coefficient, getY()+source.getY()*coefficient);
	}
	
	/**
     * Rotate vector p2 around origin by the given angle in degrees.
     * 
     * @param point Vector to rotate.
     * @param angle_rad Angle of rotation in degrees.
     * @return Rotated point.
     */
    public static Vector2D rotateVector(final Vector2D point, double angle)
    {
    	double angle_rad = angle*Math.PI/180d;
    	
    	double xDiff = point.getX();// - origin.getX();
    	double yDiff = point.getY();// - origin.getY();
    	
    	double rotX = (xDiff * Math.cos(angle_rad)) - (yDiff * Math.sin(-angle_rad));// + origin.getX();
    	double rotY = (xDiff * Math.sin(-angle_rad)) + (yDiff * Math.cos(angle_rad));// + origin.getY();
    	
    	return new Vector2D(rotX, rotY);// + origin.getX(), rotY + origin.getY());
    }
    
    /**
     * Translate current vector by some offset.
     * 
     * @param offset Translate offset.
     */
    public void translateVector(Vector2D offset) {
    	setLocation(getX()+offset.getX(), getY()+offset.getY());
    }
	
	
	/*    * overrides * */
	
	/**
	 * Turns into human readable form (x, y, z)
	 */
	public String toString() {
		return "("+String.format("%.2f", getX()).replace(',', '.')+", "+String.format("%.2f", getY()).replace(',', '.')+")";
	}
	

	/*    * APIs * */
	
	/**
	 * Gets sqrt(r*r) where r is the vector
	 * @return the length of the vector
	 */
	public double getLength() {
		return (double) Math.sqrt(sqr(getX())+sqr(getY()));
	}

	
	/*    * helpers * */
	
	/**
	 * Just square a number
	 */
	private double sqr(double a) {
		return a*a;
	}
	
	
	/*    * class APIs * */
	
	
	/**
	 * Dot product
	 * @param a
	 * @param b
	 * @return new {@link Vector3D} = a . b
	 */
	public static double dot(Vector2D a, Vector2D b) {
		return (a.getX()*b.getX()+a.getY()*b.getY());
	}
	

	
	/**
	 * Get a unit vector in the direction of a
	 * @param a
	 * @param b
	 * @return new unit vector
	 */
	public static Vector2D getNormal(Vector2D a) {
		return divide(a,a.getLength());
	}
	
	/**
	 * @param a
	 * @param k
	 * @return new {@link Vector3D} = (x*k, y*k, z*k)
	 */
	public static Vector2D multiply(Vector2D a, double k) {
		return new Vector2D(a.getX()*k, a.getY()*k);
	}
	
	/**
	 * @param a
	 * @param k
	 * @return new {@link Vector3D} = (x/k, y/k, z/k)
	 */
	public static Vector2D divide(Vector2D a, double k) {
		return new Vector2D(a.getX()/k, a.getY()/k);
	}
	
	/**
	 * @param a
	 * @param b
	 * @return new {@link Vector3D} = a + b
	 */
	public static Vector2D add(Vector2D a, Vector2D b) {
		return new Vector2D(a.getX()+b.getX(), a.getY()+b.getY());
	}
	
	/**
	 * @param a
	 * @param b
	 * @return new {@link Vector3D} = a - b
	 */
	public static Vector2D subtract(Vector2D a, Vector2D b) {
		return new Vector2D(a.getX()-b.getX(), a.getY()-b.getY());
	}
	
	/**
	 * Get vector in same direction with different length
	 * @param a
	 * @param new_length
	 * @return
	 */
	public static Vector2D change_length(Vector2D a, double new_length) {
		double old_length = a.getLength();
		if (old_length == 0)
			return ZERO();
		double nlool = new_length/old_length;
		return new Vector2D(nlool*a.getX(), nlool*a.getY());
	}
	
	/**
	 * Get angle between a and b
	 * @param a
	 * @param b
	 * @return in degrees
	 */
	public static double getAngle(Vector2D a, Vector2D b) {
		return Math.atan2(-a.getY()+b.getY(), a.getX()-b.getX())*180/Math.PI;
	}
	
	public static double getAngle(Vector2D a) {
		return getAngle(new Vector2D(0,0), a);
	}
	
	/**
	 * Get direction of a vector
	 * @param a
	 * @param b
	 * @return in degrees
	 */
	public static double getDirection(Vector2D a) {
		return Math.atan2(-a.getY(), a.getX())*180/Math.PI;
	}
}
