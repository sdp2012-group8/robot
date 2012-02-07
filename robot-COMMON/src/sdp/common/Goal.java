package sdp.common;

import java.awt.geom.Point2D;

public class Goal {
	Point2D.Double centre;
	Point2D.Double top;
	Point2D.Double bottom;
	
	public Goal(Point2D.Double a){
		this.centre = a;
		this.top = new Point2D.Double(centre.x, centre.y-25);
		this.bottom = new Point2D.Double(centre.x, centre.y+25);
	}
	
	public Point2D.Double getCentre() {
		return centre;
	}

	public Point2D.Double getTop() {
		return top;
	}

	public Point2D.Double getBottom() {
		return bottom;
	}


}
