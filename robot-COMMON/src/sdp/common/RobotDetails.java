package sdp.common;

import java.awt.Point;

public class RobotDetails
{
	public static final int ROBOT_LENGTH = 70;
	public static final int ROBOT_WIDTH = 50;
	
    protected Point coors;
    protected double angle;
    protected Rectangle rect = new Rectangle(); //rectangle is the rotated rectangle around the robot
    
    protected RobotDetails() {}

    public RobotDetails(Point coors, float angle) {
        super();
        this.coors = coors;
        this.angle = angle;
        updateRect();
    }
    
    //gets a copy of the class
    public RobotDetails(RobotDetails old)
    {
        coors = old.getCoors();
        angle = Math.toRadians(old.getAngle());
        updateRect();
    }

    public Point getCoors()
    {
        return coors;
    }

    public double getAngle()
    {
        return angle;
    }
    
    public Rectangle getRect()
    {
    	return rect;
    }

    public Point getFrontLeft()
    {
    	PointF cornerPoint = new PointF( ROBOT_LENGTH/2,ROBOT_WIDTH/2);
    	Point ret = Tools.rotatePoint(new Point(0,0), cornerPoint.toPoint(), (int) Math.toDegrees(angle));
    	ret.x += coors.x;
    	ret.y += coors.y;
    	return ret;
    }
    
    public Point getFrontRight()
    {
    	PointF cornerPoint = new PointF( ROBOT_LENGTH/2, -ROBOT_WIDTH/2);
    	Point ret = Tools.rotatePoint(new Point(0,0), cornerPoint.toPoint(), (int) Math.toDegrees(angle));
    	ret.x += coors.x;
    	ret.y += coors.y;
    	return ret;
    }
    
    public Point getBackLeft()
    {
    	PointF cornerPoint = new PointF( -ROBOT_LENGTH/2, ROBOT_WIDTH/2);
    	Point ret = Tools.rotatePoint(new Point(0,0), cornerPoint.toPoint(), (int) Math.toDegrees(angle));
    	ret.x += coors.x;
    	ret.y += coors.y;
    	return ret;
    }
    
    public Point getBackRight()
    {
    	PointF cornerPoint = new PointF( -ROBOT_LENGTH/2, -ROBOT_WIDTH/2);
    	Point ret = Tools.rotatePoint(new Point(0,0), cornerPoint.toPoint(), (int) Math.toDegrees(angle));
    	ret.x += coors.x;
    	ret.y += coors.y;
    	return ret;
    }
    
    public void updateRect()
    {
    	rect.setCorners(getFrontLeft(), getFrontRight(), getBackLeft(), getBackRight());
    }
    
    
}