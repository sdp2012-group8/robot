package sdp.brick;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

import lejos.nxt.Battery;
import lejos.nxt.LCD;
import lejos.nxt.Motor;

/**
 * This is the program that should be uploaded to the NXT Brick.
 * It translates PC commands into movements. Modify {@link #receiveMessage(opcode, byte[], Communicator)}
 * method to add new abilities. To add new opcodes, modify {@link opcode}.
 * 
 * @author martinmarinov
 *
 */
public class Brick {

	private static Communicator mCont;
	
	/**
	 * The entry point of the program
	 * @param args
	 */
	public static void main(String[] args) {
		// connect with PC and start receiving messages
		mCont = new BComm(new MessageListener() {
			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controler) {
				// to send messages back to PC, use mCont.sendMessage
				switch (op) {
				
				case exit:
					mCont.close();
					System.exit(0);
					break;
					
				case move:
					float voltage = Battery.getVoltage();
					Motor.A.setSpeed(voltage*200);
					Motor.C.setSpeed(voltage*200);
					Motor.A.forward();
					Motor.C.forward();

					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Motor.C.setSpeed(0);
					LCD.drawString("C stopped", 0, 0);
					Motor.A.setSpeed(0);
					LCD.drawString("A stopped", 0, 1);
					break;
				}
				
			}
		});
	}
}
