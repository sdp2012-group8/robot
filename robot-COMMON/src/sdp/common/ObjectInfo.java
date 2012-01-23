package sdp.common;

import java.awt.Point;


public class ObjectInfo {

	protected RobotDetails yellowBot = new RobotDetails_Editable(new Point(0, 0),0);
	protected RobotDetails blueBot = new RobotDetails_Editable(new Point(0, 0),0);
	protected Point ballCoors = new Point(0,0);
	
	public RobotDetails getYellowBot()
	{
            return new RobotDetails(yellowBot);
	}
	
	public RobotDetails getBlueBot()
	{
            return new RobotDetails(blueBot);
	}
	
	public Point getBallCoors()
	{
            return new Point(ballCoors);
	}
}

