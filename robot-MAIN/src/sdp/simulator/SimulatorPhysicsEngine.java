package sdp.simulator;


import java.io.IOException;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import sdp.AI.Command;
import sdp.common.Painter;
import sdp.common.Utilities;
import sdp.common.Communicator.opcode;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

import static java.lang.Math.PI;

public class SimulatorPhysicsEngine extends Simulator {

	private static float sim_coeff = (float) (WorldState.PITCH_WIDTH_CM/5); // the width of the simulation
	private static float goal_depth = 5f; // in cm
	private WorldState old_st = null; // for setting world state

	/**
	 * Contains the world simulation of box2d
	 */
	private World world;
	/**
	 * Contains the active palyers
	 */
	private Body bodyBall, bodyYellow, bodyBlue;

	/**
	 * The table bodies and their indices to access
	 */
	private static final int 
	TAB_BOT = 0,
	TAB_TOP = 1,
	TAB_LEFT_UP = 2,
	TAB_LEFT_LO = 3,
	TAB_RGHT_UP = 4,
	TAB_RGHT_LO = 5,
	TAB_RGHT_OUT = 6,
	TAB_LEFT_OUT = 7;
	private Body[] table = new Body[8];

	/**
	 * Constructor
	 * 
	 * @param realtime_simulation
	 * @param robot_bounciness
	 */
	public SimulatorPhysicsEngine(boolean realtime_simulation) {		

		// create world
		world = new World(new Vec2(0, 0), true);

		// define table positions
		final BodyDef[] tableDef = new BodyDef[table.length];

		// initialize definitions
		for (int i = 0; i < tableDef.length; i++) {
			tableDef[i] = new BodyDef();
			tableDef[i].position.set(0, 0);
		}

		// create ground bodies
		for (int i = 0; i < table.length; i++)
			table[i] = world.createBody(tableDef[i]);

		// create line shapes for table corners
		final PolygonShape[] tableShape = new PolygonShape[table.length];
		for (int i = 0; i < table.length; i++)
			tableShape[i] = new PolygonShape();

		// get vertical distance from table to edge of a goal
		final float tableEdgeToGoalEdgeDistance = (float) (WorldState.PITCH_HEIGHT_CM - WorldState.GOAL_CENTRE_Y)/2;

		// set sizes of table sides
		tableShape[TAB_BOT]		.setAsEdge(new Vec2((float) 0,										(float) cmToPx(WorldState.PITCH_HEIGHT_CM)),								new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),					cmToPx(WorldState.PITCH_HEIGHT_CM)));
		tableShape[TAB_TOP]		.setAsEdge(new Vec2((float) 0,										(float) 0),																	new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),					(float) 0));
		tableShape[TAB_LEFT_LO]	.setAsEdge(new Vec2((float) 0,										(float) cmToPx(WorldState.PITCH_HEIGHT_CM - tableEdgeToGoalEdgeDistance)),	new Vec2((float) 0,											cmToPx(WorldState.PITCH_HEIGHT_CM)));
		tableShape[TAB_LEFT_UP]	.setAsEdge(new Vec2((float) 0,										(float) 0),																	new Vec2((float) 0,											cmToPx(tableEdgeToGoalEdgeDistance)));
		tableShape[TAB_RGHT_LO]	.setAsEdge(new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),				(float) cmToPx(WorldState.PITCH_HEIGHT_CM - tableEdgeToGoalEdgeDistance)),	new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),					cmToPx(WorldState.PITCH_HEIGHT_CM)));
		tableShape[TAB_RGHT_UP]	.setAsEdge(new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),				(float) 0),																	new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM),					cmToPx(tableEdgeToGoalEdgeDistance)));
		tableShape[TAB_RGHT_OUT].setAsEdge(new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM + goal_depth),	(float) cmToPx(tableEdgeToGoalEdgeDistance)),								new Vec2(cmToPx(WorldState.PITCH_WIDTH_CM + goal_depth),	cmToPx(WorldState.PITCH_HEIGHT_CM- tableEdgeToGoalEdgeDistance)));
		tableShape[TAB_LEFT_OUT].setAsEdge(new Vec2(cmToPx(-goal_depth),							(float) cmToPx(tableEdgeToGoalEdgeDistance)),								new Vec2(cmToPx(-goal_depth),								cmToPx(WorldState.PITCH_HEIGHT_CM- tableEdgeToGoalEdgeDistance)));
		
		// create fixtures
		for (int i = 0; i < table.length; i++) {

			final FixtureDef tableFixture = new FixtureDef();
			tableFixture.shape = tableShape[i];
			tableFixture.friction = 1f;
			tableFixture.restitution = 0f;

			table[i].createFixture(tableFixture);
		}

		// register
		registerBlue(new VBrick(), 40, WorldState.PITCH_HEIGHT_CM / 2);
		registerYellow(new VBrick(), WorldState.PITCH_WIDTH_CM - 40, WorldState.PITCH_HEIGHT_CM / 2);
		putBallAt();

		if (realtime_simulation)
			startRealTimeThread();

	}

	/**
	 * Do the physics simulation
	 */
	protected void simulate(double dt) {	

		if (bodyBlue == null || bodyYellow == null || bodyBall == null)
			return;

		// calculate brick speeds
		calculateBrickSpeeds(dt);

		// set brick velocities
		setVector(bodyBlue.getLinearVelocity(), bodyBlue.getAngle(), speeds[0]);
		setVector(bodyYellow.getLinearVelocity(), bodyYellow.getAngle(), speeds[1]);

		// set turning speeds
		bodyBlue.setAngularVelocity((float) (turning_speeds[0] * PI / 180));
		bodyYellow.setAngularVelocity((float) (turning_speeds[1] * PI / 180));

		// do kicking
		final Vector2D[] positions = getRobotPositions();
		final Vector2D ball = getBall();
		final double[] directions = getRbotDirections();
		for (int i = 0; i < robot.length; i++)
			if (robot[i].is_kicking) {
				final Body me = i == 0 ? bodyBlue : bodyYellow;

				final Vector2D future_rel_ball = Vector2D.rotateVector(
						Vector2D.subtract(ball, positions[i]),
						-directions[i]);
				final double ball_distance = future_rel_ball.x
						- VBrick.front_left.getX();
				if (ball_distance < KICKER_RANGE && ball_distance > 0
						&& future_rel_ball.y < VBrick.front_left.getY()
						&& future_rel_ball.y > VBrick.front_right.getY()) {
					final Vec2 force = new Vec2();
					setVector(force, me.getAngle(), 400);
					bodyBall.applyForce(force, bodyBall.getPosition());
				} else
					robot[i].is_kicking = false;
			}

		// do simulation
		world.step((float) dt, 6, 2);
		
		// check for scores
		if (bodyBall.getPosition().x < 0) {
			SCORE_LEFT++;
			putBallAt();
		} if (bodyBall.getPosition().x > sim_coeff) {
			SCORE_RIGHT++;
			putBallAt();
		}
	}

	@Override
	public void setWorldState(WorldState ws, double dt, boolean is_ws_in_cm,
			Command command, Boolean am_i_blue) {
		if (!is_ws_in_cm)
			ws = Utilities.toCentimeters(ws);
		boolean first_run = old_st == null || dt == 0;
		for (int id = 0; id < 2; id++) {
			boolean is_it_me = command != null && am_i_blue != null && ((am_i_blue && id == 0) || (!am_i_blue && id == 1));
			final Robot rob = id == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
			final Body bodyRob = id == 0 ? bodyBlue : bodyYellow;
			Robot old_rob = null;
			if (old_st != null)
				old_rob = id == 0 ? old_st.getBlueRobot() : old_st.getYellowRobot();
				putAt(rob.getCoords().x / WorldState.PITCH_WIDTH_CM, rob.getCoords().y / WorldState.PITCH_WIDTH_CM, id, rob.getAngle());
				if (is_it_me) {
					
					final Vector2D velocity = first_run ? Vector2D.ZERO() :
						Vector2D.divide(
								Vector2D.subtract(new Vector2D(rob.getCoords()), new Vector2D(old_rob.getCoords())),dt);
					
					bodyRob.setLinearVelocity(new Vec2(cmToPx(velocity.x), cmToPx(velocity.y)));
					
					Vector2D proj = Vector2D.rotateVector(velocity, -rob.getAngle());
					speeds[id] = proj.x;
					turning_speeds[id] = first_run ? 0 : (rob.getAngle() - old_rob.getAngle())/dt;
					try {
						robot[id].sendMessage(opcode.operate, command.getShortDrivingSpeed(), command.getShortTurningSpeed());
					} catch (IOException e) {}
				} else {
					try {
						robot[id].sendMessage(opcode.operate, (short) 0, (short) 0);
					} catch (IOException e) {}
				}
		}
		if (ws.getBallCoords().x != -1 && ws.getBallCoords().y != -1) {

			putBallAt(ws.getBallCoords().x / WorldState.PITCH_WIDTH_CM, ws.getBallCoords().y / WorldState.PITCH_WIDTH_CM);
			
			final Vector2D ball_velocity = first_run ? Vector2D.ZERO() : 
				Vector2D.divide(
						Vector2D.subtract(new Vector2D(ws.getBallCoords()), new Vector2D(old_st.getBallCoords())), dt);
			
			bodyBall.setLinearVelocity(new Vec2(cmToPx(ball_velocity.x), cmToPx(ball_velocity.y)));
		}
		
		old_st = ws;
		im = ws.getWorldImage();
	}

	@Override
	public void putBallAt(double x, double y) {

		try {

			final float newx = (float) (x * sim_coeff);
			final float newy = (float) (y * sim_coeff);

			// delete body if exists
			if (bodyBall != null) {
				world.destroyBody(bodyBall);
			}

			// define ball
			final BodyDef ballDef = new BodyDef();
			ballDef.type = BodyType.DYNAMIC;
			ballDef.position.set(newx, newy);

			bodyBall =  world.createBody(ballDef);

			final CircleShape ballShape = new CircleShape();
			ballShape.m_radius = cmToPx(BALL_RADIUS);

			final FixtureDef ballFixture = new FixtureDef();
			ballFixture.shape = ballShape;
			ballFixture.density = 1f;
			ballFixture.friction = 1f;
			ballFixture.restitution = 0.8f;

			bodyBall.createFixture(ballFixture);

			bodyBall.setLinearDamping(0.3f);
			bodyBall.setAngularDamping(0.3f);

		} catch (Exception e) {}
	}

	@Override
	public void putAt(double x, double y, int id, double direction) {
		putAt(x, y, id);
		final float angRad = (float) (direction*PI/180);

		try {

			if (id == 0)
				bodyBlue.m_sweep.a =  angRad;
			else if (id == 1)
				bodyYellow.m_sweep.a = angRad;

		} catch (Exception e) {}
	}

	@Override
	public void putAt(double x, double y, int id) {
		try {

			final float newx = (float) (x * sim_coeff);
			final float newy = (float) (y * sim_coeff);

			switch (id) {
			case 0:
				// delete body if exists
				if (bodyBlue != null) {
					world.destroyBody(bodyBlue);
				}

				// define blue robot
				final BodyDef blueDef = new BodyDef();
				blueDef.type = BodyType.DYNAMIC;
				blueDef.position.set(newx, newy);

				bodyBlue = world.createBody(blueDef);

				final PolygonShape blueShape = new PolygonShape();
				blueShape.setAsBox(cmToPx(Robot.LENGTH_CM/2), cmToPx(Robot.WIDTH_CM/2));

				final FixtureDef blueFixture = new FixtureDef();
				blueFixture.shape = blueShape;
				blueFixture.density = 100f;
				blueFixture.friction = 1f;

				bodyBlue.createFixture(blueFixture);


				break;

			case 1:

				// delete body if exists
				if (bodyYellow != null) {
					world.destroyBody(bodyYellow);
				}

				// define yellow robot
				final BodyDef yellowDef = new BodyDef();
				yellowDef.type = BodyType.DYNAMIC;
				yellowDef.position.set(newx, newy);

				bodyYellow = world.createBody(yellowDef);

				final PolygonShape yellowShape = new PolygonShape();
				yellowShape.setAsBox(cmToPx(Robot.LENGTH_CM/2), cmToPx(Robot.WIDTH_CM/2));

				final FixtureDef yellowFixture = new FixtureDef();
				yellowFixture.shape = yellowShape;
				yellowFixture.density = 100f;
				yellowFixture.friction = 1f;

				bodyYellow.createFixture(yellowFixture);
				break;
			}

		} catch (Exception e) {}

	}

	@Override
	protected Vector2D getBall() {
		final Vec2 ball = bodyBall == null ? new Vec2(-1, -1) : bodyBall.getPosition();//bodyBall.m_xf.position;
		return new Vector2D(pxToCm(ball.x), pxToCm(ball.y));
	}

	@Override
	protected Vector2D[] getRobotPositions() {
		final Vec2 yellow = bodyYellow == null ? new Vec2(-1, -1) : bodyYellow.getPosition();
		final Vec2 blue = bodyBlue == null ? new Vec2(-1, -1) : bodyBlue.getPosition();
		return new Vector2D[] {new Vector2D(pxToCm(blue.x), pxToCm(blue.y)), new Vector2D(pxToCm(yellow.x), pxToCm(yellow.y))};
	}

	@Override
	protected double[] getRbotDirections() {
		return new double[]{bodyBlue == null ? 0 : bodyBlue.getAngle()*180/PI, bodyYellow == null ? 0 : bodyYellow.getAngle()*180/PI};
	}

	/**
	 * Sets the provided vector to match the new one (which is provided in polar coordinates)
	 * @param velocity
	 * @param ang_rad
	 * @param magnitude cm/sec
	 */
	private final static void setVector(Vec2 velocity, double ang_rad, double magnitude) {
		final float pxmag = cmToPx(magnitude);
		velocity.set((float) (pxmag * Math.cos(ang_rad)), (float) (- pxmag * Math.sin(ang_rad)));
	}

	private final static float cmToPx(double cm) {
		return (float) (cm*sim_coeff/WorldState.PITCH_WIDTH_CM);
	}

	private final static double pxToCm(float px) {
		return px*WorldState.PITCH_WIDTH_CM/sim_coeff;
	}
	
	@Override
	protected void sketchWs(Painter p, WorldState ws, boolean fill) {
		super.sketchWs(p, ws, fill);
		try {
		
			final int ball_x = (int) (bodyBall.getPosition().x*IMAGE_WIDTH/sim_coeff),
				ball_y = (int) (bodyBall.getPosition().y*IMAGE_WIDTH/sim_coeff),
				ball_dir_x = (int) (Math.cos(bodyBall.getAngle())*BALL_RADIUS*2),
				ball_dir_y = (int) (Math.sin(bodyBall.getAngle())*BALL_RADIUS*2);
		p.drawLine(ball_x, ball_y, ball_x+ball_dir_x, ball_y+ball_dir_y);
		
		} catch (Exception e) {}
	}


}
