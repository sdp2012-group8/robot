package sdp.common;

import java.awt.Point;
public class RobotDetails_Editable extends RobotDetails {

    public RobotDetails_Editable(Point coors, float angle) {
        super();
        this.coors = coors;
        this.angle = angle;
        updateRect();
    }

    public void setCoorsAndAngle(double angleInRadians, Point coors)
    {
    	setAngle(angleInRadians);
    	
    	setCoors(coors);
    	updateRect();
    }
    
    private void setAngle(double angleInRadians) {
        this.angle = angleInRadians;
    }

    private void setCoors(Point coors) {
        this.coors = coors;    
    }

}
