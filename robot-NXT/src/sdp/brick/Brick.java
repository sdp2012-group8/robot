package sdp.brick;

import java.io.File;
import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.Communicator.sensorType;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXT;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;

/**
 * This is the program that should be uploaded to the NXT Brick.
 * It translates PC commands into movements. Modify {@link #receiveMessage(opcode, byte[], Communicator)}
 * method to add new abilities. To add new opcodes, modify {@link opcode}.
 * 
 * @author martinmarinov
 *
 */
public class Brick {

	private static final int coll_threshold = 30; // cm
	private static final int back_speed = -10; // cm per sec
	private static final int angle_threshold = 5; // degrees per sec
	private static final int turning_boost = 20; // degrees per sec
	private static boolean is_on = true;

	private static Communicator mComm;
	private static UltrasonicSensor sens;
	private static boolean collision = false;
	private static boolean can_kick = false;

	private static TouchSensor kickSensor;

	/**
	 * The entry point of the program
	 * @param args
	 */
	public static void main(String[] args) {
		// connect with PC and start receiving messages
		sens = new UltrasonicSensor(SensorPort.S1);
		sens.continuous();
		kickSensor = new TouchSensor(SensorPort.S2);
		new Thread() {
			public void run() {
				while (is_on) {
					int dist = sens.getDistance();
					collision = dist < coll_threshold;
					//					try {
					//						if ( collision && mComm != null){
					//							try {
					//								mComm.sendMessage(opcode.sensor, (byte) 0, (byte) dist);
					//								Thread.sleep(10);
					//							} catch (IOException e) {
					//								e.printStackTrace();
					//							}
					//						}
					//						Thread.sleep(100);
					//					} catch (InterruptedException e) {
					//						e.printStackTrace();
					//					}
				}
				sens.off();
			};
		}.start();
		final Thread kicker_retractor = new Thread() {
			public void run() {
				while (is_on) {
					boolean initial = kickSensor.isPressed();
					if (!initial) {
						if (can_kick) {can_kick = false;}
						Motor.B.setSpeed((int)(Motor.B.getMaxSpeed()/2));
						Motor.B.setAcceleration(6000);
						Motor.B.backward();
						while (!kickSensor.isPressed()) {
							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						Motor.B.stop();
					}
					can_kick = true;


					try {
						Thread.sleep(100);						
					} catch (InterruptedException e) {}
				}
			};
		};
		kicker_retractor.start();
		mComm = new BComm();
		mComm.registerListener(new MessageListener() {

			// for joypad
			private float speed_a = 0;
			private float speed_c = 0;

			// for conversions
			private int acc = 1000; // acc in degrees/s/s
			private double acceleration = acc*0.017453292519943295*WHEELR;//69.8;
			private double turn_acceleration = acc*WHEELR/ROBOTR;

			// for tacho

			public static final float ROBOTR = 7.1F;
			public static final float WHEELR = 4F;
			float slowest = Motor.A.getMaxSpeed() > Motor.B.getMaxSpeed() ? Motor.B.getMaxSpeed()-10 : Motor.A.getMaxSpeed()-10;

			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controller) {
				final int def_vol = Sound.getVolume();
				// to send messages back to PC, use mCont.sendMessage
				switch (op) {

				case exit:
					mComm.close();
					Sound.setVolume(def_vol);
					is_on = false;
					try {
						Thread.sleep(200);
					} catch (Exception e) {}
					sens.off();
					NXT.shutDown();
					break;

				case kick:
					if (can_kick) {
						Motor.B.setSpeed(Motor.B.getMaxSpeed());
						Motor.B.setAcceleration(10000);
						Motor.B.backward();
						can_kick = false;
					}
					break;

				case rotate_kicker_stop:
					if (args.length > 0) {
						Motor.B.setSpeed(slowest);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					}
					Motor.B.stop();
					break;

				case float_kicker:
					Motor.B.flt();
					break;	

				case move_to_wall:
					Motor.A.setSpeed(slowest);
					Motor.C.setSpeed(slowest);
					Motor.A.setAcceleration(1000);
					Motor.C.setAcceleration(1000);
					Motor.A.forward();
					Motor.C.forward();

					while (true) { // if no other sensors interfere?
						int dist = sens.getDistance();
						LCD.clear(2);
						LCD.clear(3);
						LCD.drawString(String.valueOf(dist), 0, 2);
						LCD.drawString(sens.getUnits(), 0, 3);
						if (dist < 30)
							break;
					}
					Motor.A.setSpeed(0);
					Motor.C.setSpeed(0);
					Motor.C.stop();
					Motor.A.stop();

					break;

				case operate:
					// args[0] - speed in cm per second
					// args[1] - turning speed in degrees per second around centre of robot
					// args[2] - acceleration in cm/s/s
					if (args.length > 0) {
						// collision detection
						//						if (collision && Math.abs(args[1]) >= angle_threshold) {
						//							args[0] = back_speed;
						//							args[1] += args[1] > 0 ? turning_boost : -turning_boost;
						//						}
						float old_a = speed_a;
						float old_c = speed_c;
						// convert the degrees per second around robot
						// to degrees per second for the motor
						float conv_angle = 0;
						if (args.length > 1) conv_angle = args[1]*ROBOTR/WHEELR;
						float speed_angle = args[0]/(0.017453292519943295f*WHEELR); // Radius*Pi*Angle/180
						// set desired speed
						speed_a = speed_angle+conv_angle;
						speed_c = speed_angle-conv_angle;
						float corr = 0;
						if (Math.abs(speed_a) > Motor.A.getMaxSpeed()) {
							corr = Math.abs(speed_a) - Motor.A.getMaxSpeed();
						} else if (Math.abs(speed_c) > Motor.C.getMaxSpeed()){
							corr = Math.abs(speed_c) - Motor.C.getMaxSpeed();
						}
						LCD.clear();
						LCD.drawString("corr " + corr, 0, 0);
						// change speed according to turning
						Motor.A.setSpeed(Math.abs(speed_a)-corr);
						Motor.C.setSpeed(Math.abs(speed_c)-corr);

						// check if we need to start motors or turn their direction
						if (args.length > 2) {
							//change acceleration
							acceleration = args[2];
							acc = (int) (acceleration/(0.017453292519943295*WHEELR));
							turn_acceleration = acc*WHEELR/ROBOTR;
						} else
							acc = 1000;
						if (old_a == 0 || old_a*speed_a < 0) {
							Motor.A.setAcceleration(acc);
							if (speed_a > 0)
								Motor.A.forward();
							else
								Motor.A.backward();
						}
						// check if we need to start motors or turn their direction
						if (old_c == 0 || old_c*speed_c < 0) {
							Motor.C.setAcceleration(acc);
							if (speed_c > 0)
								Motor.C.forward();
							else
								Motor.C.backward();
						}
						// check if we need to stop motors
						if (speed_a == 0)
							Motor.A.stop();
						if (speed_c == 0)
							Motor.C.stop();
					}
					break;

				case play_sound:
					Sound.setVolume(Sound.VOL_MAX);
					Sound.playSample(new File("SMB.wav"));
					break;

				}
			}
		});
		// if the communicator is not listening inside a new thread, this code below this point will never be reached!
	}
}
