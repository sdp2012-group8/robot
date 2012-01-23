/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sdp.common;

import sdp.common.ObjectInfo;
import java.awt.Point;

public class ObjectInfo_Editable extends ObjectInfo{

    public void updateYellowBot(Point p, int angle)
    {
    	if (p != null)
    	{
            yellowBot.coors.x = p.x;
            yellowBot.angle = angle;
            yellowBot.coors.y = 480 - p.y;
    	}
    }

    public void updateBlueBot(Point p, int angle)
    {
    	if (p != null)
    	{
            blueBot.coors.x = p.x;
            blueBot.coors.y = 480 - p.y;
            blueBot.angle = angle;
    	}
    }

    public void updateBall(Point p)
    {
    	if (p != null)
    	{
            ballCoors.x = p.x;
            ballCoors.y = 480 - p.y;
    	}
    }
}
