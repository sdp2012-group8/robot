package sdp.common;

import java.awt.Point;

public class Line {

	Point p1;
	Point p2;
	
	double angle;
	
	double gradient;
	double yIntercept;

	public Line(Point p1, Point p2)
	{
		this.p1 = p1;
		this.p2 = p2;
		
		gradient = getGradient(p1, p2);
		yIntercept = getIntercept(p1, gradient);
	}
	public Line(Point p1, double angle)
	{
		this.p1 = p1;
		this.angle = angle;
		
		gradient = Math.tan(angle);
		yIntercept = getIntercept(p1, gradient);
	}
	
	public int getYfromX(int x)
	{
		return (int) Math.round(x * gradient + yIntercept);
	}
	
	public double getYfromX(double x)
	{
		return (double)x * gradient + yIntercept;
	}
	
	public boolean isPointAboveLine(Point p)
	{
		return getYfromX(p.x) < p.y;
	}
	
	public void printEquation()
	{
		System.out.println("y = " + gradient + "x + " + yIntercept);
	}
	
    public static float getGradient(Point p1, Point p2)
    {
    	//slight hack allowing us to ignore when p1.x == p2.x
    	if (p1.x == p2.x)
    		return Float.MAX_VALUE;
    	
    	return ((float) (p2.y - p1.y)) / (p2.x-p1.x);
    }
    
    public static double getIntercept(Point p, double gradient2)
    {
    	return (p.y - gradient2 * p.x);
    }
    
    public boolean isPointOnTheLine(Point o)
    {
    	return distanceBetweenPointAndALine(o) < 1;
    }
    
    public double distanceBetweenPointAndALine(Point o)
    {
    	//variables are left as x0, y0 etc for clarity.static
		int x0 = o.x;
		int y0 = o.y;
		
		int x1 = p1.x;
		int y1 = p1.y;
		
		double x2;
		double y2;
		
		if(p2 == null){
			x2 = x1+1;
			y2 = this.getYfromX((double)x1+1);
		} else {
			x2 = p2.x;
			y2 = p2.y;
		}
		//http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
		return Math.abs((x2-x1)*(y1-y0)-(x1-x0)*(y2-y1))/Math.sqrt(Math.pow((x2-x1), 2)+Math.pow((y2-y1), 2));
    }
}
