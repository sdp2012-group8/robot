package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.Connection;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.NeuralNetworkCODEC;

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
	private static final int[] LAYERS = new int[] {67, NNetTools.move_modes.values().length};
	
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
		final double[] input = NNetTools.generateAIinput(aiWorldState, aiWorldState.isOwnTeamBlue(), aiWorldState.isOwnGoalLeft());
		nets.setInput(input);
		nets.calculate();
		final double[] output = nets.getOutput();
		NNetTools.move_modes mode = NNetTools.recoverMoveOutputMode(output);
		
		//System.out.println(mode+": "+NNetTools.printArray(output, ",", output.length));
		
		// convert result into speeds
		final int speed = NNetTools.getDesiredSpeed(mode, Robot.MAX_DRIVING_SPEED);
		final int turn_speed = NNetTools.getDesiredTurningSpeed(mode, MAX_TURNING_SPEED);
		
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
	 * Initialise a new neural network with the given set of weights
	 * @param weights compatible with output of {@link #getWeights()}
	 * @see #setWeights(double[])
	 */
	public AINeuralNet(final double[] weights) {
		nets = new MultiLayerPerceptron(LAYERS);
		setWeights(weights);
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
	
	/**
	 * Put the array as weights of the neural network
	 * 
	 * @param weights compatible with the output of {@link #getWeights()}
	 */
	public void setWeights(final double[] weights) {
		array2network(weights, nets);
	}
	
    /**
     * Decode a network from an array.
     * @param array The array used to decode.
     * @param network The network to decode into.
     */
    public static void array2network(final double[] array, final NeuralNetwork network) {
        int index = 0;
 
        for (Layer layer : network.getLayers()) {
            for (Neuron neuron : layer.getNeurons()) {
                for (Connection connection : neuron.getOutConnections()) {
                    connection.getWeight().setValue(array[index++]);
                }
            }
        }
    }
	
	/**
	 * Get the weights of the neural network
	 * 
	 * @return weights that are compatible with {@link #setWeights(double[])}
	 */
	public double[] getWeights() {
		
		final double[] result = new double[NeuralNetworkCODEC.determineArraySize(nets)];
		NeuralNetworkCODEC.network2array(nets, result);
		return result;
	
	}
	
	/**
	 * Returns a random array of weights that could
	 * be used with {@link #setWeights(Double[])}
	 * @return output compatible with {@link #setWeights(double[])}
	 */
	public static final double[] getRandomWeights() {
		
		// create a new ai with required network
		final AINeuralNet temp = new AINeuralNet(new MultiLayerPerceptron(LAYERS));
		
		// randomize the weights
		temp.getNetwork().randomizeWeights();
		
		// return
		return temp.getWeights();
		
	}
	
	/**
	 * Get the expected size of the weight array for the current neural network settings (depends on the {@link #LAYERS} constant)
	 * @return
	 */
	public static final int getWeightsCount() {
		return NeuralNetworkCODEC.determineArraySize(new MultiLayerPerceptron(LAYERS));
	}

}
