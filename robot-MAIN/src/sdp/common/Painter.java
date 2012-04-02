package sdp.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import sdp.AI.pathfinding.Waypoint;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public class Painter {

	public int MOUSE_OVER_ROBOT = -1;
	public boolean MOUSE_OVER_BALL = false;
	public Integer reference_robot_id = null;

	public  Graphics2D g;
	private  int width, height;
	private double ratio;
	private static final double BALL_RADIUS = 4.27 / 2;
	private  WorldState state_cm;
	private int off_x = 0, off_y = 0;
	private  Robot[] robots;
	public static Vector2D[] debug;
	public static Vector2D[] target;
	public static ArrayList<Waypoint> fullPath;

	public Painter(BufferedImage im, WorldState ws) {
		g = im.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		width = im.getWidth();
		height = im.getHeight();
		ratio = (WorldState.PITCH_WIDTH_CM/(double) WorldState.PITCH_HEIGHT_CM) * (height/(double) width);
		state_cm = ws;
		robots = new Robot[]{state_cm.getBlueRobot(), state_cm.getYellowRobot()};
	}

	public final  void dispose() {
		g.dispose();
	}

	public void setOffsets(int x, int y, int w, int h) {
		off_x = x;
		off_y = y;
		width = w;
		height = h;
		ratio = (WorldState.PITCH_WIDTH_CM/(double) WorldState.PITCH_HEIGHT_CM) * (height/(double) width);
	}

	/**
	 * Creates visualization
	 * 
	 * @return
	 */
	public  void image(boolean my_team_blue, boolean my_goal_left) {
		for (int j = 0; j < robots.length; j++) {
			Robot robot;
			Color color = Color.gray;
			// chose robot color
			switch (j) {
			case 0:
				color = new Color(0, 0, 255, 200);
				break;
			case 1:
				color = new Color(220, 220, 0, 200);
				break;
			}
			if (j == MOUSE_OVER_ROBOT)
				g.setColor(brighter(color));
			else
				g.setColor(color);
			g.setStroke(new BasicStroke(1.0f));
			robot = new Robot(Vector2D.divide(new Vector2D(robots[j].getCoords()), WorldState.PITCH_WIDTH_CM),
					robots[j].getAngle());
			// draw body of robot

			fillPolygon(new int[] {
					(int)(robot.getFrontLeft().getX()*width),
					(int)(robot.getFrontRight().getX()*width),
					(int)(robot.getBackRight().getX()*width),
					(int)(robot.getBackLeft().getX()*width),
					(int)(robot.getFrontLeft().getX()*width)
			}, new int[] {
					(int)(robot.getFrontLeft().getY()*width),
					(int)(robot.getFrontRight().getY()*width),
					(int)(robot.getBackRight().getY()*width),
					(int)(robot.getBackLeft().getY()*width),
					(int)(robot.getFrontLeft().getY()*width)
			}, 5, true);

			// draw direction pointer
			double shift_x = 0.01 * Math.cos(robot.getAngle() * Math.PI / 180d);
			double shift_y = -0.01
					* Math.sin(robot.getAngle() * Math.PI / 180d);
			g.setColor(Color.white);
			g.setStroke(new BasicStroke(10.0f));

			double dir_x = 0.04*Math.cos(robot.getAngle()*Math.PI/180d);
			double dir_y = -0.04*Math.sin(robot.getAngle()*Math.PI/180d);
			drawLine(
					(int)((robot.getCoords().getX()-shift_x)*width),
					(int)((robot.getCoords().getY()-shift_y)*width),
					(int)((robot.getCoords().getX()+dir_x-shift_x)*width),
					(int)((robot.getCoords().getY()+dir_y-shift_y)*width));
			dir_x = 0.03*Math.cos((robot.getAngle()+90)*Math.PI/180d);
			dir_y = -0.03*Math.sin((robot.getAngle()+90)*Math.PI/180d);
			drawLine(
					(int)((robot.getCoords().getX()-dir_x/2-shift_x)*width),
					(int)((robot.getCoords().getY()-dir_y/2-shift_y)*width),
					(int)((robot.getCoords().getX()+dir_x/2-shift_x)*width),
					(int)((robot.getCoords().getY()+dir_y/2-shift_y)*width));

		}
		
		// draw target
		if (target != null) {
			g.setColor(new Color(255, 255, 255, 255));
			for (int i = 0; i < target.length; i++)
				fillOval((int)(target[i].x* width / WorldState.PITCH_WIDTH_CM-3), (int) (target[i].y* width / WorldState.PITCH_WIDTH_CM-3), 6, 6, true);
		}
		
		if (fullPath != null)
		for (Waypoint wp : fullPath) {
			
			drawLine((int)(wp.getOriginPos().x* width / WorldState.PITCH_WIDTH_CM), (int)(wp.getOriginPos().y* width / WorldState.PITCH_WIDTH_CM),
					(int)(wp.getTarget().x* width / WorldState.PITCH_WIDTH_CM), (int)(wp.getTarget().y* width / WorldState.PITCH_WIDTH_CM));
		}
		
		// draw ball
		g.setColor(Color.red);
		if (MOUSE_OVER_BALL)
			g.setColor(brighter(g.getColor()));
		g.setStroke(new BasicStroke(1.0f));
		fillOval(

				(int) ((state_cm.getBallCoords().getX() - BALL_RADIUS) * width / WorldState.PITCH_WIDTH_CM),
				(int) ((state_cm.getBallCoords().getY() - BALL_RADIUS) * width / WorldState.PITCH_WIDTH_CM),
				(int) (2 * BALL_RADIUS * width / WorldState.PITCH_WIDTH_CM),
				(int) (2 * BALL_RADIUS * width / WorldState.PITCH_WIDTH_CM), true);
		drawLine((int)(state_cm.getBallCoords().getX()* width / WorldState.PITCH_WIDTH_CM), 0, (int) (state_cm.getBallCoords().getX()* width / WorldState.PITCH_WIDTH_CM), (int) (height/ratio));
		drawLine(0, (int)(state_cm.getBallCoords().getY()* width / WorldState.PITCH_WIDTH_CM), width, (int)(state_cm.getBallCoords().getY()* width / WorldState.PITCH_WIDTH_CM));
	}

	// helpers


	public void fillRect(int x, int y, int w, int h) {
		fillPolygon(new int[] {
				x,
				x+w,
				x+w,
				x,
				x
		}, new int[] {
				y,
				y,
				y+h,
				y+h,
				y
		}, 5, true);
	}

	/**
	 * Use instead of g.fillOval
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public  void fillOval(int x, int y, int w, int h, boolean fill) {
		Vector2D l_r = transformScreenVectorToLocalOne(x-w/2, y-h/2);
		Vector2D t_r = transformScreenVectorToLocalOne(x+w/2, y+h/2);
		Vector2D cent = Vector2D.divide(Vector2D.add(l_r, t_r), 2);
		if (fill)
			g.fillOval(off_x+(int) cent.getX(), off_y+(int) (cent.getY() * ratio), w, h);
		else
			g.drawOval(off_x+(int) cent.getX(), off_y+(int) (cent.getY() * ratio), w, h);
	}

	/**
	 * Use instead of g.fillPolygon
	 * @param xs
	 * @param ys
	 * @param size number of points
	 */
	public void fillPolygon(int[] xs, int[] ys, int size, boolean fill) {
		Vector2D[] points = new Vector2D[size];
		int[] newxs = new int[size], newys = new int[size];
		for (int i = 0; i < size; i++) {
			points[i] = transformScreenVectorToLocalOne(xs[i], ys[i]);
			newxs[i] = off_x+(int) points[i].getX();
			newys[i] = off_y+(int) (points[i].getY()*ratio);
		}
		if (fill)
			g.fillPolygon(newxs, newys, size);
		else
			g.drawPolygon(newxs, newys, size);
	}


	/**
	 * Use instead of g.DrawLine
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public  void drawLine(int x1, int y1, int x2, int y2) {
		if (reference_robot_id != null) {

			Vector2D start = transformScreenVectorToLocalOne(x1, y1);
			Vector2D end = transformScreenVectorToLocalOne(x2, y2);
			g.drawLine(off_x+(int) start.getX(), off_y+(int) (start.getY()*ratio),
					off_x+(int) end.getX(), off_y+(int) (end.getY()*ratio));
		} else
			g.drawLine(off_x+x1, off_y+(int) (y1*ratio), off_x+x2, off_y+(int) (y2*ratio));
	}

	/**
	 * Draw vecrtor
	 * 
	 * @param origin
	 *            in cm
	 * @param vector
	 *            in cm
	 */
	private  void drawVector(Vector2D origin, Vector2D vector, boolean draw_point_in_end) {

		double ex = (origin.getX()+vector.getX())*width/WorldState.PITCH_WIDTH_CM, ey = (origin.getY()+vector.getY())*width/WorldState.PITCH_WIDTH_CM;
		drawLine(
				(int)(origin.getX()*width/WorldState.PITCH_WIDTH_CM),
				(int)(origin.getY()*width/WorldState.PITCH_WIDTH_CM),
				(int)(ex),
				(int)(ey));
		if (draw_point_in_end) {
			fillOval((int) ex-3, (int) ey-3, 6, 6, true);
		}

	}

	private  Vector2D transformScreenVectorToLocalOne(int x, int y) {

		if (reference_robot_id == null)
			return new Vector2D(x, y);
		Robot rob = new Robot(Vector2D.multiply(new Vector2D(robots[reference_robot_id].getCoords()), width/WorldState.PITCH_WIDTH_CM), robots[reference_robot_id].getAngle());
		Vector2D centre_pitch = new Vector2D(0.5*width, 0.5*WorldState.PITCH_HEIGHT_CM*width/WorldState.PITCH_WIDTH_CM);
		return Vector2D.add(centre_pitch, Robot.getLocalVector(rob, new Vector2D(x, y)));

	}


	/**
	 * Gets a brighter color, suitable for highlighting
	 */
	private  Color brighter(Color a) {
		double dr = a.getRed() + 0.6 * (255 - a.getRed());
		double dg = a.getGreen() + 0.6 * (255 - a.getGreen());
		double db = a.getBlue() + 0.6 * (255 - a.getBlue());
		int r = dr < 255 ? (int) dr : 255;
		int g = dg < 255 ? (int) dg : 255;
		int b = db < 255 ? (int) db : 255;
		return new Color(r, g, b);
	}

}
