package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import sdp.AI.Command;
import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.WorldStateObserver;
import sdp.common.Communicator.opcode;
import sdp.common.geometry.GeomUtils;
import sdp.common.world.WorldState;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.ImageProcessorConfig;


/**
 * Interface for AI-system communication. This class is responsible for
 * bridging AI implementations with the rest of the system. 
 */
public class AIMaster extends WorldStateProvider {

	/** Operation modes, common to all AI implementations. */
	public enum AIState {
		START, PLAY, DEFEND_GOAL, SIT, DEFEND_PENALTIES, SHOOT_PENALTIES, MANUAL_CONTROL
	}
	
	/** Whether the AI's should switch to defence or not. */
	private static final boolean DEFENCE_ENABLED = false;
	
	/** Distance from the goal when the AI changes from Penalty mode to play mode */
	private static final int PENALTIES_THRESH = 30; 
	/** How much the ball has to move before AI thinks the start position has passed. */
	private static final int START_POS_BALL_THRESHOLD = 5;

	
	/** The observer object that provides world state updates. */
	private WorldStateObserver observer;
	/** A thread that executes the main logic loop. */
	private Thread updateThread = null;
	
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
	
	/** Currently used image processor configuration. */
	private ImageProcessorConfig config;
	
	/** Starting ball position. */
	private Point2D.Double ballStartPos;
	/** Whether the start state was entered in the last frame. */
	private boolean startStateEntered = false;
	
	/** Whether the AI should print its state on the world image. */
	private boolean drawOnWorldImage = false;
	/** Whether state changes should be printed to stdout. */
	private boolean printStateChanges = true;

	
	public AIMaster(Communicator comm, final BaseAI ai) {
		this.ai = ai;
		this.communicator = comm;
	}
	
	/**
	 * Create a new AI controller.
	 * 
	 * @param comm Robot communicator to use.
	 * @param observer World state provider to use.
	 * @param aiType Which AI implementation to use.
	 */
	public AIMaster(Communicator comm, WorldStateProvider observer, final BaseAI ai) {
		this.ai = ai;
		this.observer = new WorldStateObserver(observer);

		this.communicator = comm;
		communicator.registerListener(new MessageListener() {
			@Override
			public synchronized void receiveMessage(opcode op, short[] args, Communicator controller) {
//				System.out.println(op + " " + args[0]);
				
				switch (op) {
				case SENSOR_KICKER:
//					try {
//						communicator.sendMessage(opcode.kick);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
					aiWorldState.setFrontSensorActive(args[0] == 1);
					try {
						executeCommand(AIMaster.this.ai.gotBall());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					break;
				case SENSOR_LEFT:
//					aiWorldState.setLeftSensorActive(args[0] == 1);
					break;
				case SENSOR_RIGHT:
//					aiWorldState.setRightSensorActive(args[0] == 1);
					break;
				case BATTERY:
					aiWorldState.setBatteryLevel(args[0]);
					break;
				}
			}
		});
		
	}
	

	/**
	 * Start a new AI execution thread. Permits at most one active thread.
	 * 
	 * @param ownTeamBlue true if my team is blue, false if my team is yellow
	 * @param ownGoalLeft true if my goal is on the left of camera, false otherwise
	 */
	public void start(boolean ownTeamBlue, boolean ownGoalLeft) {
		if (updateThread != null) {
			stop();
		}
		
		this.isOwnTeamBlue = ownTeamBlue;
		this.isOwnGoalLeft = ownGoalLeft;
		
		updateThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					processState(observer.getNextState(), true);
				}
				//TODO: cleanup
			}
		};
		updateThread.start();
	}
	
	/**
	 * Respond to a state. Make sure {@link #setOwnGoalLeft(boolean)} and {@link #setOwnTeamBlue(boolean)} is set correctly prior calling this.
	 * @param state in normalised coordinates
	 */
	public void processState(WorldState state, boolean notifyListeners) {
		state = WorldState.toCentimeters(state);

		if (worldState == null) {
			worldState = state;
			aiWorldState = new AIWorldState(worldState, isOwnTeamBlue, isOwnGoalLeft);
		} else {
			if (drawOnWorldImage) {
				aiWorldState.onDraw(state.getWorldImage(), config);
			}
			worldState = state;
		}
		
		aiWorldState.update(worldState, isOwnTeamBlue, isOwnGoalLeft);
		
		if (notifyListeners) {
			setChanged();
			notifyObservers(worldState);
		}
		
		executeNextCommand();
	}
	
	/**
	 * Stop the AI execution thread.
	 */
	public void stop() {
		if (updateThread != null) {
			updateThread.interrupt();
			updateThread = null;
		}
	}

	
	public AIWorldState getAIWorldState() {
		return aiWorldState;
	}
	
	/**
	 * Execute the next command on the current world state.
	 */
	private synchronized void executeNextCommand() {
		updateCurrentAIState();
		ai.update(aiWorldState);
		
		try {
			executeCommand(getCommand());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Check which state should transition to, based on the current world state.
	 */
	private void updateCurrentAIState() {
		if (aiState == AIState.START) {
			// START -> PLAY
			if (!isGameInStart()) {
				setState(AIState.PLAY);
			}
			startStateEntered = false;
			
		} else if (aiState == AIState.PLAY) {
			// PLAY -> DEFEND_GOAL
			if (DEFENCE_ENABLED && AIVisualServoing.canEnemyAttack(aiWorldState)) {
				setState(AIState.DEFEND_GOAL);
			}
						
		} else if (aiState == AIState.DEFEND_GOAL) {
			// DEFEND_GOAL -> PLAY
			if (!AIVisualServoing.canEnemyAttack(aiWorldState)) {
				setState(AIState.PLAY);
			}

		} else if (aiState == AIState.DEFEND_PENALTIES) {
			// DEFEND_PENALTIES -> PLAY
			double ballX = aiWorldState.getBallCoords().x;
			
			boolean ballCloseEnough = false;
			ballCloseEnough |= (aiWorldState.isOwnGoalLeft() && (ballX < PENALTIES_THRESH));
			ballCloseEnough |= (!aiWorldState.isOwnGoalLeft() && (ballX > (WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH)));
			
			if (ballCloseEnough) {
				setState(AIState.PLAY);
			}
			
		} else if (aiState == AIState.SHOOT_PENALTIES) {
			// SHOOT_PENALTIES -> PLAY
			double ballX = aiWorldState.getBallCoords().x;
			
			boolean ballCloseEnough = false;
			ballCloseEnough |= (!aiWorldState.isOwnGoalLeft() && (ballX < PENALTIES_THRESH));
			ballCloseEnough |= (aiWorldState.isOwnGoalLeft() && (ballX > (WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH)));

			if (ballCloseEnough) {
				setState(AIState.PLAY);
			}
		} 
	}
	
	
	/**
	 * Get the next command for execution from the AI, based on the AI's
	 * current state. 
	 * 
	 * @return The next command to execute.
	 * @throws IOException
	 */
	private Command getCommand() throws IOException {
		switch (getState()) {
		case START:
			return ai.start();
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
	
	
	/**
	 * Send the given command to the robot for immediate execution.
	 * 
	 * @param command Command to execute.
	 * @throws IOException
	 */
	private void executeCommand(Command command) throws IOException {
		if (aiWorldState.isLeftSensorActive()) {
			command.turningSpeed = 90;
		} else if (aiWorldState.isRightSensorActive()) {
			command.turningSpeed = -90;
		}
		
		if (command != null){
			aiWorldState.setCommand(command);
			if (command.isAccelerationDefault()) {
				communicator.sendMessage(opcode.operate, command.getShortDrivingSpeed(),
						command.getShortTurningSpeed());
			} else {
				communicator.sendMessage(opcode.operate, command.getShortDrivingSpeed(),
						command.getShortTurningSpeed(), command.getShortAcceleration());
			}
			
			if (command.kick) {
				//System.out.println("Kicking");
				communicator.sendMessage(opcode.kick);
			}
		}
		
//		System.out.println("ws: " + aiWorldState.isOwnGoalLeft());
	}
	
	public void changeAI(BaseAI newAi) {
		ai = newAi;
	}
	
	
	/**
	 * Set current image processor configuration.
	 * 
	 * @param config Current image processor configuration.
	 */
	public void setConfiguration(ImageProcessorConfig config) {
		this.config = config;
	}
	
	
	/**
	 * Set whether our own goal is on the left side.
	 * 
	 * @param isOwnGoalLeft Whether our goal is on the left.
	 */
	public void setOwnGoalLeft(boolean isOwnGoalLeft) {
		this.isOwnGoalLeft = isOwnGoalLeft;
	}

	
	/**
	 * Set whether our team is blue.
	 * 
	 * @param isOwnTeamBlue Whether our team is blue.
	 */
	public void setOwnTeamBlue(boolean isOwnTeamBlue) {
		this.isOwnTeamBlue = isOwnTeamBlue;
	}
	
	
	/**
	 * Get current AI state.
	 * 
	 * @return The current AI state.
	 */
	public AIState getState() {
		return aiState;
	}
	
	/**
	 * Set the new AI state.
	 * 
	 * @param newState The new AI state.
	 */
	public void setState(AIState newState) {
		aiState = newState;
		if (printStateChanges) {
			System.out.println("Changed State to - " + aiState);
		}
		
		// TODO: Review this bit of logic.
		if (aiState == AIState.DEFEND_GOAL) {
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					setState(AIMaster.AIState.PLAY);
				}
			}, 6000);
		}
		
		startStateEntered = (aiState == AIState.START);
		ai.changedState();
	}
	
	
	/**
	 * Check whether the game is in its beginning stages.
	 * 
	 * @param aiWorldState Current world state.
	 * @return
	 */
	public boolean isGameInStart() {
		if (startStateEntered || (ballStartPos == null)) {
			ballStartPos = new Point2D.Double();
			ballStartPos.x = aiWorldState.getBallCoords().x;
			ballStartPos.y = aiWorldState.getBallCoords().y;
		}
		
		double ballOffset = GeomUtils.pointDistance(ballStartPos, aiWorldState.getBallCoords());
		return (ballOffset < START_POS_BALL_THRESHOLD);
	}
	

	/**
	 * Set whether AI state changes should be printed.
	 * 
	 * @param printChanges Whether AI state changes should be printed.
	 */
	public void setPrintStateChanges(boolean printChanges) {
		this.printStateChanges = printChanges;
	}
	
	/**
	 * Toggle the AI drawing on the world image.
	 */
	public void toggleDrawingOnWorldImage() {
		drawOnWorldImage = !drawOnWorldImage;
	}
	
	public BaseAI getAI() {
		return ai;
	}
}
