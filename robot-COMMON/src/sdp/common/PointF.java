package sdp.common;

import java.awt.Point;

public class PointF {

	public float x;
	public float y;
	
	public PointF(PointF p)
	{
		this.x = p.x;
		this.y = p.y;
	}
	
	public PointF(Point p)
	{
		this.x = p.x;
		this.y = p.y;
	}
	
	
	public PointF(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Point toPoint()
	{
		return new Point((int) this.x,(int)this.y);
	}
	
	public void rotateBy(double angleInRadians)
	{
		float oldX = x;
		float oldY = y;
		double cos = Math.cos(angleInRadians);
		double sin = Math.sin(angleInRadians);
		x = (float) (oldX * cos - oldY * sin);
		y = (float) (oldX * sin + oldY * cos);	
	}
}
