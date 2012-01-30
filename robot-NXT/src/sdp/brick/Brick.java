package sdp.brick;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

import lejos.nxt.Battery;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXT;

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
				float max;
				switch (op) {
				
				case exit:
					mCont.close();
					NXT.shutDown();
					break;
					
				case move:
					try {
						max = Battery.getVoltage()*100;
						Motor.A.setSpeed(max+args[2]);
						Motor.C.setSpeed(max+args[3]);
						Motor.A.setAcceleration(args[1]*100);
						Motor.C.setAcceleration(args[1]*100);
						Motor.A.forward();
						Motor.C.forward();
					} catch (Exception e){
						LCD.drawString("Command Error: Check Args", 2, 2);
					}
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Motor.A.setSpeed(0);
					Motor.C.setSpeed(0);
					break;
					
				case moveback:
					try {
						max = Battery.getVoltage()*100;
						Motor.A.setSpeed(max);
						Motor.C.setSpeed(max);
						Motor.A.setAcceleration(args[1]*100);
						Motor.C.setAcceleration(args[1]*100);
						Motor.A.backward();
						Motor.C.backward();
					} catch (Exception e){
						LCD.drawString("Command Error: Check Args", 2, 2);
					}
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Motor.C.setSpeed(0);
					Motor.A.setSpeed(0);
					break;
					
				case moveangle:
					
						Motor.A.rotate(args[0], true);
						Motor.C.rotate(args[0], true);
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;	
				case kick:
						max = Battery.getVoltage()*100;
						Motor.B.setSpeed(max);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(-70);
						Motor.B.rotate(70);
						Motor.B.stop();
					break;
					
				case rotate_kicker:
						max = Battery.getVoltage()*100;
						Motor.B.setSpeed(max);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					break;
				default:
					LCD.drawString("Unknown Command", 2, 2);
					break;
				}
				
			}
		});
	}
}
