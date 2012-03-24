package sdp.AI;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import sdp.AI.Command;
import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Utilities;
import sdp.common.WorldStateObserver;
import sdp.common.Communicator.opcode;
import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.common.geometry.Vector2D;
import sdp.vision.processing.ImageProcessorConfig;


/**
 * Interface for AI-system communication. This class is responsible for
 * bridging AI implementations with the rest of the system. 
 */
public class AIMaster extends WorldStateProvider {

	/** Operation modes, common to all AI implementations. */
	public enum AIState {
		PLAY, DEFEND_GOAL, SIT, DEFEND_PENALTIES, SHOOT_PENALTIES, MANUAL_CONTROL
	}

	/** Available AI implementations. */
	public enum AIType {
		VISUAL_SERVOING, NEURAL_NETWORKS
	}


	// TODO: Come back here.
	/** The threshold distance for defend goal */
	private static final double DEFEND_THRESH = (WorldState.PITCH_WIDTH_CM / 2) - 20;
	/** Distance from the goal when the AI changes from Penalty mode to play mode */
	private static final int PENALTIES_THRESH = 30; 

	
	/** The observer object that provides world state updates. */
	private WorldStateObserver observer;
	/** A thread that executes the main logic loop. */
	private Thread updateThread;
	
	/** AI implementation in use. */
	private BaseAI ai;
	/** Current AI state. */
	private AIState aiState = AIState.SIT;
	
	/** Robot communicator. */
	private Communicator communicator;

	/** Current world state. */
	private WorldState worldState = null;
	/** Current AI world state. */
	private AIWorldState aiWorldState;
	
	/** Whether our goal is on the left. */
	private boolean isOwnGoalLeft;
	/** Whether our team is blue. */
	private boolean isOwnTeamBlue;
	
	/** Whether the AI should print its state on the world image. */
	private boolean drawOnWorldImage = false;
	
	/** Currently used image processor configuration. */
	private ImageProcessorConfig config;
	
	
	
	public void switchOverrideVision() {
		drawOnWorldImage = !drawOnWorldImage;
	}
	
	public void setConfiguration(ImageProcessorConfig config) {
		this.config = config;
	}

	/**
	 * Starts the AI in a new decision thread. (Not true, starts a new thread that updates the world state every time it changes)
	 * 
	 * Don't start more than once!
	 * @param isOwnTeamBlue true if my team is blue, false if my team is yellow
	 * @param isOwnGoalLeft true if my goal is on the left of camera, false otherwise
	 */
	public void start(final boolean is_my_team_blue, final boolean is_my_goal_left) {
		this.isOwnTeamBlue = is_my_team_blue;
		this.isOwnGoalLeft = is_my_goal_left;
		updateThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = Utilities.toCentimeters(observer.getNextState());
					// do low pass filtering
					if (worldState == null) {
						worldState = state;
						aiWorldState = new AIWorldState(worldState, isOwnTeamBlue, isOwnGoalLeft);
					} else {
						BufferedImage im = state.getWorldImage();
						if (drawOnWorldImage) {
							aiWorldState.onDraw(im, config);
						}
						worldState = state;
					}
					aiWorldState.update(worldState, isOwnTeamBlue, isOwnGoalLeft);
					
					// pass coordinates to decision making logic
					setChanged();
					notifyObservers(worldState);
					worldChanged();
					
				}
			}
		};
		updateThread.start();
	}
	
	public void updateGoalOrTeam(final boolean is_my_team_blue, final boolean is_my_goal_left) {
		this.isOwnTeamBlue = is_my_team_blue;
		this.isOwnGoalLeft = is_my_goal_left;
	}


	/**
	 * Stops the AI
	 */
	public void stop() {
		if (updateThread != null)
			updateThread.interrupt();
	}

	

	public AIMaster(Communicator comm, WorldStateProvider obs, AIType ai_mode) {
		this.observer = new WorldStateObserver(obs);

		this.communicator = comm;
			communicator.registerListener(new MessageListener() {
				@Override
				public synchronized void receiveMessage(opcode op, short[] args, Communicator controler) {
					//System.out.println(op+" "+args[0]);
					switch (op) {
					case SENSOR_KICKER:
//						try {
//							mComm.sendMessage(opcode.kick);
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
						aiWorldState.setFrontSensorActive(args[0] == 1);
						try {
							execCommand(ai.gotBall());
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						break;
					case SENSOR_LEFT:
					//	ai_world_state.setLeft_sensor(args[0] == 1);
						break;
					case SENSOR_RIGHT:
						//ai_world_state.setRight_sensor(args[0] == 1);
						break;
					case BATTERY:
						aiWorldState.setBatteryLevel(args[0]);
						break;
					}

				}
			});

		setAIMode(ai_mode);
	}

	/**
	 * This method is fired when a new state is available.
	 * The methods called are in all types of the AI.
	 */
	protected synchronized void worldChanged() {
		
		Command command;

		checkState();
		ai.update(aiWorldState);
		
		try {
			execCommand(getCommand());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void execCommand(Command command) throws IOException {
			final boolean dist_sens = aiWorldState.isFrontSensorActive(),
					left_sens = aiWorldState.isLeftSensorActive(),
					right_sens = aiWorldState.isRightSensorActive();
			
			if (left_sens) {
				command.turningSpeed = 90;
			} else if (right_sens) {
				command.turningSpeed = -90;
			}
			
			if (command != null){
				aiWorldState.setCommand(command);
			
			
	//		ai_world_state.setCommand(command);

			if (command.isAccelerationDefault())
				communicator.sendMessage(opcode.operate, command.getShortDrivingSpeed(), command.getShortTurningSpeed());
			else
				communicator.sendMessage(opcode.operate, command.getShortDrivingSpeed(), command.getShortTurningSpeed(), command.getShortAcceleration());
			
			if (command.kick) {
				System.out.println("kicking");
				communicator.sendMessage(opcode.kick);
			}
			}
			//System.out.println("ws: "+ai_world_state.getMyGoalLeft());
	}

	private Command getCommand() throws IOException {
		switch (getState()) {
		case PLAY:
			return ai.chaseBall();
		case SIT:
			return ai.sit();
		case DEFEND_PENALTIES:
			return ai.penaltiesDefend();
		case SHOOT_PENALTIES:
			return ai.penaltiesAttack();
		case DEFEND_GOAL:
			return ai.defendGoal();
		default:
			return null;
		}
	}

	private void checkState() {
		// Check the new world state and decide what state we should be in.

		final boolean my_goal_left = aiWorldState.isOwnGoalLeft();
		final Point2D.Double ball = aiWorldState.getBallCoords();
		
		// DEFEND_PENALTIES -> PLAY
		if (aiState == AIState.DEFEND_PENALTIES) {
			if (my_goal_left && ball.x < PENALTIES_THRESH ||
					!my_goal_left && ball.x > WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH) {
				setState(AIState.PLAY);
			}
		} else // SHOOT_PENALTIES -> PLAY
			if (aiState == AIState.SHOOT_PENALTIES) {
				if (!my_goal_left && ball.x < PENALTIES_THRESH ||
						my_goal_left && ball.x > WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH) {
					setState(AIState.PLAY);
				}
		} else  //PLAY->DEFEND_GOAL
			if (aiState == AIState.PLAY) {
				//if the enemy robot has the ball and it is close to our goal, go to defend
//				Vector2D enemyDistance = Vector2D.subtract(new Vector2D(ai_world_state.getEnemyRobot().getCoords()), new Vector2D(ball));
//				
//				if ((((my_goal_left && ball.x < DEFEND_THRESH)  || 
//						(!my_goal_left && ball.x < WorldState.PITCH_WIDTH_CM - DEFEND_THRESH))
//						&& enemyDistance.getLength() < 30)){
//					setState(mode.DEFEND_GOAL);
//				}
						
			}
			else  //DEFEND_GOAL -> PLAY 
				if (aiState == AIState.DEFEND_GOAL) {
					//if the enemy robot is at a greater distance from the ball, go into play mode
//					Vector2D enemyDistance = Vector2D.subtract(new Vector2D(ai_world_state.getEnemyRobot().getCoords()), new Vector2D(ball));
//					
//					if (enemyDistance.getLength() > 30){
//						setState(mode.PLAY);
//					}
					
					Vector2D myDistance = Vector2D.subtract(new Vector2D(aiWorldState.getOwnRobot().getCoords()), new Vector2D(ball));
					
//					if (myDistance.getLength() < 20)
//						setState(mode.PLAY);
				}

	}

	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setState(AIState new_state) {
		aiState = new_state;
		System.out.println("Changed State to - " + aiState);

		
		if (aiState == AIState.DEFEND_GOAL) {
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					setState(AIMaster.AIState.PLAY);
				}
			}, 6000);
		}

		ai.changedState();

	}
	
	/**
	 * Change the AI mode. Can be used in the simulator to test separate modes
	 * @param new_ai
	 */
	public void setAIMode(AIType new_ai_mode){
		switch (new_ai_mode){
			case VISUAL_SERVOING:
				ai = new AIVisualServoing();
				break;
			case NEURAL_NETWORKS: 
				ai = new AITest();
				break;
		}
	}
	
	/**
	 * Gets AI mode
	 * @return
	 */
	public AIState getState() {
		return aiState;
	}

}
