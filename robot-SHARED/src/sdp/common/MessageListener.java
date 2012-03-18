package sdp.common;

import sdp.common.Communicator.opcode;

/**
 * Implement this interface if you want to receive messages from a {@link Communicator}
 * 
 * @author martinmarinov
 *
 */
public interface MessageListener {
	
	/**
	 * This method will be called when a new message is available.
	 * @param op the opcode of the message
	 * @param args the arguments of the message
	 * @param controler which controller is returning the result
	 */
	public void receiveMessage(opcode op, short[] args, Communicator controler);

}
