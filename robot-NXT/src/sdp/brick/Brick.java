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
			int lastCountA = 0;
			int lastCountC = 0;
			float slowest = Motor.A.getMaxSpeed() > Motor.B.getMaxSpeed() ? Motor.B.getMaxSpeed()-10 : Motor.A.getMaxSpeed()-10;
			
			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controler) {
				// to send messages back to PC, use mCont.sendMessage
				switch (op) {
				
				case exit:
					mCont.close();
					NXT.shutDown();
					break;
					
				case move:
					float voltage = Battery.getVoltage();
					
					Motor.A.setSpeed(slowest);
					Motor.C.setSpeed(slowest);
					Motor.A.setAcceleration(args[1]*100);
					Motor.C.setAcceleration(args[1]*100);
					Motor.A.forward();
					Motor.C.forward();
					try {
						Thread.sleep(args[0]*500);
						LCD.drawString("M-A: " + (Motor.A.getTachoCount()-lastCountA), 2, 3);
						LCD.drawString("M-C: " + (Motor.C.getTachoCount()-lastCountC), 2, 4);
						lastCountA = Motor.A.getTachoCount();
						lastCountC = Motor.C.getTachoCount();
						Thread.sleep(args[0]*500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Motor.A.setSpeed(0);
					Motor.C.setSpeed(0);
					Motor.C.stop();
					Motor.A.stop();
					break;
					
				case moveback:
					voltage = Battery.getVoltage();
					Motor.A.setSpeed(voltage*200);
					Motor.C.setSpeed(voltage*200);
					Motor.A.backward();
					Motor.C.backward();

					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Motor.C.setSpeed(0);
					Motor.A.setSpeed(0);
					Motor.C.stop();
					Motor.A.stop();
					break;
					
				case kick:
					voltage = Battery.getVoltage();
					Motor.B.setSpeed(voltage*200);
					Motor.B.setAcceleration(100000);
					Motor.B.rotate(-70);
					Motor.B.rotate(70);
					Motor.B.stop();
					break;
					
				case floatMotor:
					Motor.A.flt();
					Motor.C.flt();
					break;
				case rotate_kicker:
					voltage = Battery.getVoltage();
					Motor.B.setSpeed(voltage*200);
					Motor.B.setAcceleration(100000);
					Motor.B.rotate(args[0]);
					break;
				}
				
			}
		});
	}
}
