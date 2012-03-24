package sdp.AI;

import java.io.IOException;


/**
 * Base class for all AI implementations.
 * 
 * @author Gediminas Liktaras
 * @author Martin Marinov
 */
public abstract class BaseAI {

	/** Current world state, as seen by the AI. */
	protected AIWorldState aiWorldState = null;
	
	
	/**
	 * Create a new AI instance.
	 */
	public BaseAI() { }
	
	
	/**
	 * Describes the robot's behaviour when it needs to get to the ball.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	protected abstract Command chaseBall() throws IOException;
	
	/**
	 * Describes the robot's behaviour when it has the ball.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	protected abstract Command gotBall() throws IOException;
	
	/**
	 * Describes the robot's gate defence behaviour.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	protected abstract Command defendGoal() throws IOException;
	
	/**
	 * Describes the robot's penalty attack behaviour.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	protected abstract Command penaltiesAttack() throws IOException;
	
	/**
	 * Describes the robot's penalty defence behaviour.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	protected abstract Command penaltiesDefend() throws IOException;

	/**
	 * Describes the robot's null behaviour. I.e. what the robot should do
	 * to do nothing.
	 * 
	 * @return The next command that has to be executed.
	 * @throws IOException
	 */
	public Command sit() throws IOException {
		return new Command(0, 0, false);
	}
	
	
	/**
	 * This method is called by {@link AIMaster} every time the world state
	 * changes.
	 */
	protected abstract void changedState();
	
	/**
	 * Updates the held ai_world_state of the AI.
	 * @param ai_world_state
	 */
	protected final void update(AIWorldState ai_world_state) {
		this.aiWorldState = ai_world_state;
	}
}
