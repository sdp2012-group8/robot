package sdp.common;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Test;

public class UtilitiesTest {

	Robot robot = new Robot(new Point2D.Double(0,0),0,true);
	Robot rb = new Robot(new Point2D.Double(45.75, 83.1125), 180, true);
	
	@Test
	public void testPointInTriangle() {
		assertTrue(Utilities.isPointInTriangle(new Point2D.Double(1,2), new Point2D.Double(0,0), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(Utilities.isPointInTriangle(new Point2D.Double(0,0), new Point2D.Double(-3,-2), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(Utilities.isPointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), rb.getBackLeft(), rb.getFrontLeft(), rb.getFrontRight()));
		assertFalse(Utilities.isPointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), rb.getBackRight(), rb.getBackLeft(), rb.getFrontRight()));
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
	
	@Test
	public void testIsPathClear(){
		Robot robot = new Robot(new Point2D.Double(97.98125, 79.3),180.0, true);
		assertFalse(Utilities.lineIntersectsRobot(new Point2D.Double(73.3741016438359, 75.00703978582817), new Point2D.Double(108.27499999999999, 94.55), robot));
		assertTrue(Utilities.doesSegmentIntersectLine(robot.getBackLeft(), robot.getFrontRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));
		assertTrue(Utilities.doesSegmentIntersectLine(robot.getFrontLeft(), robot.getBackRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));

		
	}
	
}
