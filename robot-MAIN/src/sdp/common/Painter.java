package sdp.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import sdp.AI.AIVisualServoing;
import sdp.AI.AIWorldState;
import sdp.common.geometry.Vector2D;

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
	public static double point_off = AIVisualServoing.DEFAULT_POINT_OFF;
	public static double targ_thresh = AIVisualServoing.DEFAULT_TARG_THRESH;

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

			// draw nearest points of collision
			if (j < 2 && state_cm != null) {
				color = brighter(color);
				g.setColor(new Color(color.getRed(), color.getGreen(), color
						.getBlue(), 50));
				g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f },
						0.0f));
				boolean am_i_blue = j == 0;
				robot = am_i_blue ? state_cm.getBlueRobot() : state_cm
						.getYellowRobot();
				Vector2D local_origin = new Vector2D(Robot.LENGTH_CM/2+2,0);
				drawVector(Utilities.getGlobalVector(robot, local_origin),  DeprecatedCode.raytraceVector(state_cm, robot, local_origin, new Vector2D(1,0), true), true);
				if ((my_team_blue && j == 0) || (!my_team_blue && j == 1)) {
					AIWorldState ai_world_state = Utilities.getAIWorldState(state_cm, my_team_blue, my_goal_left);
					Vector2D target;
					
					try {
						// p_target = new Vector2D(Utilities.getOptimalPointBehindBall(Utilities.getAIWorldState(state_cm, my_goal_left, my_team_blue, false), point_distance));
						target = new Vector2D(DeprecatedCode.getOptimalPointBehindBall(ai_world_state, point_off));

					} catch (NullPointerException e) {
						target = new Vector2D(state_cm.getBallCoords());
					}

					Vector2D startPt = new Vector2D(robot.getCoords());
					Vector2D dir =  Vector2D.subtract(target, startPt);
					double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
					final double length = Robot.LENGTH_CM/2;
					double cos = Math.cos(angle)*length;
					double sin = Math.sin(angle)*length;
					Vector2D right = new Vector2D(cos, sin);
					Vector2D left = new Vector2D(-cos, -sin);
					drawVector(Vector2D.add(startPt, right),  DeprecatedCode.raytraceVector(state_cm, Vector2D.add(startPt, right), dir, am_i_blue, true), true);
					drawVector(Vector2D.add(startPt, left), DeprecatedCode.raytraceVector(state_cm, Vector2D.add(startPt, left), dir, am_i_blue, true), true);



					g.setStroke(new BasicStroke(8.0f));
					final int COLL_SECS_COUNT = 222;
					final double SEC_ANGLE = 360d/COLL_SECS_COUNT;

					g.setColor(new Color(255, 255, 255, 15));
					final boolean include_ball_as_obstacle = true;
					final Vector2D point_rel = Utilities.getLocalVector(ai_world_state.getOwnRobot(), target);
					final double point_dir = Vector2D.getDirection(point_rel);
					final double direct_dist = point_rel.getLength();
					final double vis_dist = DeprecatedCode.visibility2(ai_world_state, target, ai_world_state.isOwnTeamBlue(), include_ball_as_obstacle) + Robot.LENGTH_CM;
					final double other_rob_dist = Vector2D.subtract(new Vector2D(ai_world_state.getOwnRobot().getCoords()), new Vector2D(ai_world_state.getEnemyRobot().getCoords())).getLength();
					final boolean point_visible = vis_dist >= direct_dist;
					
					double turn_ang = 999;
					int id = -1;

					double point_dist = direct_dist;//point_visible ? direct_dist : other_rob_dist+Robot.LENGTH_CM/2;
					double temp = 999;
					int t = 0;
					while (turn_ang == 999) {
						for (int i = 0; i < COLL_SECS_COUNT; i++) {
							double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
							Vector2D vec = Vector2D.multiply(Vector2D.rotateVector(new Vector2D(1, 0), ang), point_dist);
							if (DeprecatedCode.reachability(ai_world_state, Utilities.getGlobalVector(ai_world_state.getOwnRobot(), vec), ai_world_state.isOwnTeamBlue(), include_ball_as_obstacle, 1.5)) {	
								double diff = Utilities.normaliseAngle(ang-point_dir);
								drawVector(new Vector2D(robot.getCoords()), Vector2D.subtract(Utilities.getGlobalVector(ai_world_state.getOwnRobot(), vec), new Vector2D(robot.getCoords())), false);
								if (Math.abs(diff) < Math.abs(temp)) {
									temp = diff;
									turn_ang = ang;
									id = i;
								}
							}
						}
						t++;
						if (t == 5)
							break;
						point_dist -= Robot.LENGTH_CM;
					}

					g.setColor(new Color(255, 0, 0, 200));
					double ang = Utilities.normaliseAngle(((-90+id*SEC_ANGLE)+(-90+(id+1)*SEC_ANGLE))/2);
					double dista = Utilities.getSector(ai_world_state, ai_world_state.isOwnTeamBlue(), Utilities.normaliseAngle(-90+id*SEC_ANGLE), Utilities.normaliseAngle(-90+(id+1)*SEC_ANGLE), 20, true).getLength();
					Vector2D vec = Vector2D.multiply(Vector2D.rotateVector(new Vector2D(1, 0), ang), dista);
					Vector2D coor = new Vector2D(robot.getCoords());
					drawVector(coor, Vector2D.subtract(Utilities.getGlobalVector(robot, vec), coor), true);
						
						g.setColor(new Color(255, 255, 255, 255));
						fillOval((int)(target.x* width / WorldState.PITCH_WIDTH_CM-3), (int) (target.y* width / WorldState.PITCH_WIDTH_CM-3), 6, 6, true);
						
						g.setStroke(new BasicStroke(1.0f));
						Vector2D dist = DeprecatedCode.raytraceVector(state_cm, target, Vector2D.changeLength(Vector2D.subtract(new Vector2D(state_cm.getBallCoords()), target), 200), null, false);
						drawVector(target, dist, false);
						
						g.setColor(new Color(255, 255, 255, 20));
						fillOval((int)((target.x-targ_thresh)* width / WorldState.PITCH_WIDTH_CM), (int) ((target.y-targ_thresh)* width / WorldState.PITCH_WIDTH_CM), (int) (2*targ_thresh* width / WorldState.PITCH_WIDTH_CM), (int) (2*targ_thresh* width / WorldState.PITCH_WIDTH_CM), true);
						
						Vector2D global_dist = Vector2D.add(dist, target);
						if (global_dist.getY() < 5) {
							double a = Vector2D.getDirection(dist);
							a += 180-2*(90-(180-a));
							Vector2D dist2 = Vector2D.rotateVector(new Vector2D(200,0), a);
							drawVector(global_dist, dist2, false);
						} else if (global_dist.getY() > WorldState.PITCH_HEIGHT_CM-5) {
							double a = Vector2D.getDirection(dist);
							a -= 2*a-360;
							Vector2D dist2 = Vector2D.rotateVector(new Vector2D(200,0), a);
							drawVector(global_dist, dist2, false);
						}
						
						if (debug != null) {
							g.setStroke(new BasicStroke(1.0f));
							g.setColor(new Color(0, 255, 255));
							for (int i = 0; i < debug.length; i++) {
								fillOval((int)(debug[i].x* width / WorldState.PITCH_WIDTH_CM-3), (int) (debug[i].y* width / WorldState.PITCH_WIDTH_CM-3), 6, 6, true);
							}
						}
						
					}
				}
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
		return Vector2D.add(centre_pitch, Utilities.getLocalVector(rob, new Vector2D(x, y)));

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
