package sdp.simulator;

import java.awt.geom.Point2D;

/**
 * 
 * Takes care of vecotor/location operations
 * 
 * @author MartinMarinov
 *
 */
public class Vector2D {
	
	
	/*    * local variables * */
	
	private double x,y;
	
	
	/*    * useful constants * */
	
	public final static Vector2D VEC_X_AXIS = new Vector2D(1,0);
	public final static Vector2D VEC_Y_AXIS = new Vector2D(0,1);
	public final static Vector2D ZERO = new Vector2D(0, 0);
	
	
	/*    * constructors * */
	
	/**
	 * Clones the given vector
	 * @param vector_to_clone the vector to be cloned
	 */
	public Vector2D(Vector2D vector_to_clone) {
		this(vector_to_clone.getX(),vector_to_clone.getY());
	}
	
	/**
	 * Creates a 2D vector (x, y) <br/><br/>
	 * The new vecor will be (x, y, 0)
	 * @param x
	 * @param y
	 */
	public Vector2D(double x, double y) {
		this.x = x;
		this.y = y;
	}
	

	/**
	 * Creates vector (tuple[0], tuple[1], tuple[2]) or (tuple[0], tuple[1]) depending on the size of tuple.
	 * @param tuple an array which first three items contain the x, y and z of the vector
	 */
	public Vector2D(double[] tuple) {
		x = tuple[0];
		y = tuple[1];
	}
	
	
	/*    * getters and setters * */

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}


	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	/**
	 * Adds the scaled source to the current vector
	 * @param source
	 * @param coefficient
	 */
	public void addmul_to(Vector2D source, double coefficient) {
		this.x+=source.getX()*coefficient;
		this.y+=source.getY()*coefficient;
	}
	
	public Point2D.Double getPoint(double scale) {
		return new Point2D.Double(x*scale, y*scale);
	}
	
	
	
	/*    * overrides * */
	
	/**
	 * Turns into human readable form (x, y, z)
	 */
	public String toString() {
		return "("+String.format("%.2f", x).replace(',', '.')+", "+String.format("%.2f", y).replace(',', '.')+")";
	}
	
	/**
	 * For helping GC
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		x = 0;
		y = 0;
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
	
	
}
