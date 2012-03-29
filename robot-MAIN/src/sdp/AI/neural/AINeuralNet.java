package sdp.AI.neural;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.Weight;
import org.neuroph.nnet.MultiLayerPerceptron;

import sdp.AI.AIVisualServoing;
import sdp.AI.Command;
import sdp.common.NNetTools;
import sdp.common.world.Robot;

/**
 * Class that provides neural network implementation for the {@link AIVisualServoing}'s,
 * {@link AIVisualServoing#chaseBall()}
 * 
 * @author Martin Marinov
 *
 */
public class AINeuralNet extends AIVisualServoing {
	
	/** Set the speed at which the robot turns */
	private static final int MAX_TURNING_SPEED = 100;
	
	/** NNetwork layers count. {1, 2, 3} would mean
	 *  network with 1 input, 3 outputs and one hidden
	 *  layer with 2 neurons */
	private static final int[] LAYERS = new int[] {45, 91, NNetTools.move_modes.values().length};
	
	/** The brain that is controlling the AI */
	private NeuralNetwork nets;
	
	// implementation
	
	/**
	 * This is method that should generate the neural network command. This method
	 * is called both for {@link #chaseBall()} and {@link #defendGoal()}
	 * 
	 * @return the command that needs to be executed on the device
	 */
	private Command play() {
		
		// feed current state through the network and get result
		nets.setInput(NNetTools.generateAIinput(aiWorldState, aiWorldState.isOwnTeamBlue(), aiWorldState.isOwnGoalLeft()));
		nets.calculate();
		final double[] result = nets.getOutput();
		
		// convert result into speeds
		final int speed = NNetTools.getDesiredSpeed(NNetTools.recoverGotBallOutputMode(result), Robot.MAX_DRIVING_SPEED);
		final int turn_speed = NNetTools.getDesiredTurningSpeed(NNetTools.recoverGotBallOutputMode(result), MAX_TURNING_SPEED);
		
		// return the command
		return new Command(speed, turn_speed, false);	
	}

	
	// API
	
	/**
	 * Initialise a new neural network AI using the specified nnetwork. If you
	 * want to load a network from file, pass {@link NeuralNetwork#load(String)}
	 * @param net
	 * @see NeuralNetwork#load(String)
	 */
	public AINeuralNet(final NeuralNetwork net) {
		setNetwork(net);
	}
	
	/**
	 * @return current network that is being used
	 * @see NeuralNetwork#save(String)
	 */
	public NeuralNetwork getNetwork() {
		return nets;
	}
	
	/**
	 * Change the current neural network that controls this AI
	 * @param net
	 * @see NeuralNetwork#load(String)
	 */
	public void setNetwork(final NeuralNetwork net) {
		nets = net;
	}
	
	
	// AI integration
	
	/**
	 * Neural network implementation (calls {@link #play()}
	 */
	@Override
	protected Command chaseBall() throws IOException {
		return play();
	}

	
	/**
	 * Neural network implementation (calls {@link #play()}
	 */
	@Override
	protected Command defendGoal() throws IOException {
		return play();
	}
	
	// helpers
	
	public void getNetworkFromWeights(final double[] weights) {
		// init network
		nets = new MultiLayerPerceptron(LAYERS);
		
		// TODO - finish it
		
		// go through the input layer to the last one
		for (int l = 0; l < LAYERS.length - 1; l++) {
			
			final Layer layer = nets.getLayerAt(l);
			
			// go through all the neurons in the current layer
			for (int n = 0; n < LAYERS[l]; n++) {
				
				final Neuron neuron = layer.getNeuronAt(n);
				
				final Iterator<Weight> it = neuron.getWeightsVector().iterator();
				if (it.hasNext()) {
					
					final Double value = it.next().value;
					
				}
				
			}
			
		}
		
	}

}
