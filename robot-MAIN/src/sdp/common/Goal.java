package sdp.common;

import java.awt.geom.Point2D;


/**
 * Goal gate data.
 */
public class Goal {
	
	/** Size of the goal gate. */
	private static final double GOAL_SIZE = 60;
	/** Offset of the goal gate. */
	private static final int GOAL_OFFSET = 15;

	/** Center point of the gate. */
	private Point2D.Double centre;
	/** Top (low Y) of the gate. */
	private Point2D.Double top;
	/** Bottom (high Y) of the gate. */
	private Point2D.Double bottom;
	
	
	/**
	 * Create a new goal.
	 * 
	 * @param centre Centre point of the goal.
	 */
	public Goal(Point2D.Double centre){
		this.centre = centre;
		this.top = new Point2D.Double(centre.x, centre.y - GOAL_SIZE / 2 + GOAL_OFFSET);
		this.bottom = new Point2D.Double(centre.x, centre.y + GOAL_SIZE / 2 - GOAL_OFFSET);
	}
	
	
	/**
	 * Get the centre point of the goal.
	 * 
	 * @return Centre of the goal.
	 */
	public Point2D.Double getCentre() {
		return centre;
	}

	/**
	 * Get the top point of the goal.
	 * 
	 * @return Top of the goal.
	 */
	public Point2D.Double getTop() {
		return top;
	}

	/**
	 * Get the bottom point of the goal.
	 * 
	 * @return Bottom of the goal.
	 */
	public Point2D.Double getBottom() {
		return bottom;
	}

}
