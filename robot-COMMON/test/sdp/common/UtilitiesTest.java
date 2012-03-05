package sdp.common;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Test;

public class UtilitiesTest {

	Robot robot = new Robot(new Point2D.Double(0,0),0,true);
	Robot rb = new Robot(new Point2D.Double(45.75, 83.1125), 180, true);
	
	@Test
	public void testPointInTriangle() {
		assertTrue(Utilities.pointInTriangle(new Point2D.Double(1,2), new Point2D.Double(0,0), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(Utilities.pointInTriangle(new Point2D.Double(0,0), new Point2D.Double(-3,-2), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(Utilities.pointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), rb.getBackLeft(), rb.getFrontLeft(), rb.getFrontRight()));
		assertFalse(Utilities.pointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), rb.getBackRight(), rb.getBackLeft(), rb.getFrontRight()));
	}

	@Test
	public void testPointInRobot(){
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(0,1), robot)); 
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(9,8), robot)); 
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), rb));
	}
	
	@Test
	public void testPointAroundRobot(){

		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(0,1), robot)); //inside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(9,8), robot)); //inside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(11,8), robot)); //outside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(-11,-9), robot)); //outside robot
		assertFalse(Utilities.isPointAroundRobot(new Point2D.Double(-31,-9), robot)); //outside robot
		assertFalse(Utilities.isPointAroundRobot(new Point2D.Double(40,15), robot)); //outside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), rb)); //outside robot
		
	}
	
}
