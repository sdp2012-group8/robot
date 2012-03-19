package sdp.common;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

import sdp.common.geometry.GeomUtils;

public class UtilitiesTest {

	private Robot robot;
	private Robot enemyRobot;
	
	@Before
	public void setUp(){	
		robot = new Robot(new Point2D.Double(0,0),0,true);
		enemyRobot = new Robot(new Point2D.Double(45.75, 83.1125), 180, true);
	}
	
	@Test
	/**
	 * Check pointInTriangle
	 */
	public void testPointInTriangle() {
		assertTrue(GeomUtils.isPointInTriangle(new Point2D.Double(1,2), new Point2D.Double(0,0), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(GeomUtils.isPointInTriangle(new Point2D.Double(0,0), new Point2D.Double(-3,-2), new Point2D.Double(4,0), new Point2D.Double(0,3)));
		assertTrue(GeomUtils.isPointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot.getBackLeft(), enemyRobot.getFrontLeft(), enemyRobot.getFrontRight()));
		assertFalse(GeomUtils.isPointInTriangle(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot.getBackRight(), enemyRobot.getBackLeft(), enemyRobot.getFrontRight()));
	}

	@Test
	/**
	 * Checks pointInRobot
	 */
	public void testPointInRobot(){
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(0,1), robot)); 
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(9,8), robot)); 
		assertTrue(Utilities.isPointInRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot));
	}
	
	@Test
	/**
	 * Checks pointAroundRobot()
	 */
	public void testPointAroundRobot(){

		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(0,1), robot)); //inside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(9,8), robot)); //inside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(11,8), robot)); //outside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(-11,-9), robot)); //outside robot
		assertFalse(Utilities.isPointAroundRobot(new Point2D.Double(-31,-9), robot)); //outside robot
		assertFalse(Utilities.isPointAroundRobot(new Point2D.Double(40,15), robot)); //outside robot
		assertTrue(Utilities.isPointAroundRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot)); //outside robot
		
	}
	
	@Test
	/**
	 * Checks isPathClear()
	 */
	public void testIsPathClear(){
		Robot robot = new Robot(new Point2D.Double(97.98125, 79.3),180.0, true);
		assertFalse(Utilities.lineIntersectsRobot(new Point2D.Double(73.3741016438359, 75.00703978582817), new Point2D.Double(108.27499999999999, 94.55), robot));
		assertTrue(GeomUtils.doesSegmentIntersectLine(robot.getBackLeft(), robot.getFrontRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));
		assertTrue(GeomUtils.doesSegmentIntersectLine(robot.getFrontLeft(), robot.getBackRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));

		
	}

}
