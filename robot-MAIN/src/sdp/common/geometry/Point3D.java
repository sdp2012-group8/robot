package sdp.common.geometry;


/**
 * A three-dimensional point.
 */
public class Point3D {
	
	/** X coordinate value of the point. */
	public double x;
	/** Y coordinate value of the point. */
	public double y;
	/** Z coordinate value of the point. */
	public double z;
	
	/**
	 * Create a new 3D point.
	 * 
	 * @param x X coordinate value.
	 * @param y Y coordinate value.
	 * @param z Z coordinate value.
	 */
	public Point3D (double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}