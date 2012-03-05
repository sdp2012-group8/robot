package sdp.common;

import java.awt.geom.Point2D;

public class Goal {
	Point2D.Double centre;
	Point2D.Double top;
	Point2D.Double bottom;
	double size = 60;
	private static final int OFFSET=5;
	
	public Goal(Point2D.Double a){
		this.centre = a;
		this.top = new Point2D.Double(centre.x, centre.y-size/2-OFFSET);
		this.bottom = new Point2D.Double(centre.x, centre.y+size/2+OFFSET);
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
