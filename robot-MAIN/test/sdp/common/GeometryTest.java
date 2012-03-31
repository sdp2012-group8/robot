package sdp.common;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;

/**
 * Contains tests for some functions in the sdp.common.geometry.GeomUtils class
 * @author mihaela_laura_ionescu
 *
 */
public class GeometryTest {

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
		assertTrue(Robot.isPointInRobot(new Point2D.Double(0,1), robot)); 
		assertTrue(Robot.isPointInRobot(new Point2D.Double(9,8), robot)); 
		assertTrue(Robot.isPointInRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot));
	}
	
	@Test
	/**
	 * Checks pointAroundRobot()
	 */
	public void testPointAroundRobot(){

		assertTrue(Robot.isPointAroundRobot(new Point2D.Double(0,1), robot)); //inside robot
		assertTrue(Robot.isPointAroundRobot(new Point2D.Double(9,8), robot)); //inside robot
		assertTrue(Robot.isPointAroundRobot(new Point2D.Double(11,8), robot)); //outside robot
		assertTrue(Robot.isPointAroundRobot(new Point2D.Double(-11,-9), robot)); //outside robot
		assertFalse(Robot.isPointAroundRobot(new Point2D.Double(-31,-9), robot)); //outside robot
		assertFalse(Robot.isPointAroundRobot(new Point2D.Double(40,15), robot)); //outside robot
		assertTrue(Robot.isPointAroundRobot(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot)); //outside robot
		
	}
	
	@Test
	/**
	 * Checks isPathClear()
	 */
	public void testIsPathClear(){
		Robot robot = new Robot(new Point2D.Double(97.98125, 79.3),180.0, true);
		assertFalse(Robot.lineIntersectsRobot(new Point2D.Double(73.3741016438359, 75.00703978582817), new Point2D.Double(108.27499999999999, 94.55), robot));
		assertTrue(GeomUtils.doesSegmentIntersectLine(robot.getBackLeft(), robot.getFrontRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));
		assertTrue(GeomUtils.doesSegmentIntersectLine(robot.getFrontLeft(), robot.getBackRight(), new Point2D.Double(50.527974093792345, 93.47062110637931), new Point2D.Double(87.6875, 108.27499999999999)));

		
	}
	
	@Test
	/**
	 * Checks normaliseAngle
	 * @see sdp.common.geometry.GeomUtils#normaliseAngle(double angle)
	 */
	public void testNormaliseAngle(){
		assertEquals(GeomUtils.normaliseAngle(275),-85,0);
		assertEquals(GeomUtils.normaliseAngle(-560),160,0);
		assertEquals(GeomUtils.normaliseAngle(-360),0,0);
	}
	
	@Test
	/**
	 * @see sdp.common.geometry.GeomUtils#isPointInAxisAlignedBox(Point2D.Double p, Point2D.Double a, Point2D.Double b) 
	 */
	public void testIsPointInAxisAlignedBox(){
		assertTrue(GeomUtils.isPointInAxisAlignedBox(new Point2D.Double(3,4), new Point2D.Double(0,0), new Point2D.Double(4,6)));
		assertTrue(GeomUtils.isPointInAxisAlignedBox(new Point2D.Double(3,4), new Point2D.Double(0,0), new Point2D.Double(3,4)));
		assertFalse(GeomUtils.isPointInAxisAlignedBox(new Point2D.Double(0,2), new Point2D.Double(0,0), new Point2D.Double(0,1)));
		assertFalse(GeomUtils.isPointInAxisAlignedBox(new Point2D.Double(-3,4), new Point2D.Double(-2,-3), new Point2D.Double(4,6)));
	}
	
	@Test
	/**
	 * @see sdp.common.geometry.GeomUtils#isPointInQuadrilateral(Point2D.Double p, Point2D.Double q1, Point2D.Double q2, Point2D.Double q3, Point2D.Double q4)
	 */
	public void testIsPointInQuadrilateral(){
		assertTrue(GeomUtils.isPointInQuadrilateral(new Point2D.Double(0,1), robot.getFrontRight(), robot.getBackRight(), robot.getBackLeft(), robot.getFrontLeft()));
		assertTrue(GeomUtils.isPointInQuadrilateral(new Point2D.Double(9,8), robot.getFrontRight(), robot.getBackRight(), robot.getBackLeft(), robot.getFrontLeft()));
		assertTrue(GeomUtils.isPointInQuadrilateral(new Point2D.Double(41.12771286455945, 76.14973554308418), enemyRobot.getFrontRight(), enemyRobot.getBackRight(), enemyRobot.getBackLeft(), enemyRobot.getFrontLeft()));
		assertFalse(GeomUtils.isPointInQuadrilateral(new Point2D.Double(41.12771286455945, 76.14973554308418), robot.getFrontRight(), robot.getBackRight(), robot.getBackLeft(), robot.getFrontLeft()));
	}
	
	@Test
	/**
	 * @see sdp.common.geometry.GeomUtils#getLineLineIntersection(Point2D.Double l1pt1, Point2D.Double l1pt2,	Point2D.Double l2pt1, Point2D.Double l2pt2)
	 */
	public void testGetLineLineIntersection(){
		assertEquals(GeomUtils.getLineLineIntersection(new Point2D.Double(2,2), new Point2D.Double(-1,-1), new Point2D.Double(0,3), new Point2D.Double(7,3)), new Point2D.Double(3,3));
		assertEquals(GeomUtils.getLineLineIntersection(new Point2D.Double(2,2), new Point2D.Double(-1,-1), new Point2D.Double(-1,-2), new Point2D.Double(2,1)), null);

	}
	
	@Test
	/**
	 * @see sdp.common.geometry.GeomUtils#getLocalRaySegmentIntersection(Point2D.Double rayOrigin,
	 *		Vector2D rayDir, Point2D.Double segPt1, Point2D.Double segPt2)
	 */
	public void testGetLocalRaySegmentIntersection(){
		assertEquals(GeomUtils.getLocalRaySegmentIntersection(new Point2D.Double(1,2), new Vector2D(10,2), new Point2D.Double(9,3), new Point2D.Double(7,1)), null);
		assertEquals(GeomUtils.getLocalRaySegmentIntersection(new Point2D.Double(1,2), new Vector2D(10,2), new Point2D.Double(9,3), new Point2D.Double(7,1)), null);
	}
	
	@Test
	/**
	* @see sdp.common.geometry.GeomUtils#getClosestPointToLine(Point2D.Double p,
	*		Point2D.Double a, Point2D.Double b)
	*/
	public void testGetClosestPointToLine(){
		assertEquals(GeomUtils.getClosestPointToLine(new Point2D.Double(6,3), new Point2D.Double(7,1), new Point2D.Double(8,2)), new Point2D.Double(7.5,1.5));
		assertEquals(GeomUtils.getClosestPointToLine(new Point2D.Double(6,2), new Point2D.Double(7,1), new Point2D.Double(8,2)), new Point2D.Double(7,1));

	}
	
	@Test
	/**
	 *  @see sdp.common.geometry.GeomUtils#doesSegmentIntersectLine(Point2D.Double segPt1, Point2D.Double segPt2,
	 *		Point2D.Double linePt1, Point2D.Double linePt2)
	 */
	public void testDoesSegmentIntersectLine(){
		assertFalse(GeomUtils.doesSegmentIntersectLine(new Point2D.Double(1,2), new Vector2D(10,2), new Point2D.Double(9,3), new Point2D.Double(7,1)));
		assertTrue(GeomUtils.doesSegmentIntersectLine(new Point2D.Double(2,2), new Point2D.Double(6,6), new Point2D.Double(-1,-2), new Point2D.Double(2,1)));
	}

}
