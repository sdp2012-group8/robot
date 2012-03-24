/**
 * 
 */
package sdp.AI;

import sdp.common.Utilities;

public class Command {
	
	public static final double default_acceleration = 69.81317d;
	
	public double speed, turning_speed, acceleration = default_acceleration;
	public boolean kick = false;
	
	public Command(double speed, double turning_speed, boolean kick) {
		this.speed = speed;
		this.turning_speed = turning_speed;
		this.kick = kick;
	}
	
	public short getShortSpeed() {
		return Utilities.normaliseSpeed(Utilities.normaliseAngleToShort(speed));
	}
	
	public short getShortTurnSpeed() {
		return Utilities.normaliseAngleToShort(turning_speed);
	}
	
	public short getShortAcc() {
		return Utilities.normaliseAngleToShort(acceleration);
	}
	
	public boolean isDefaultAcc() {
		return acceleration == default_acceleration;
	}

	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "spd: "+getShortSpeed()+", tspd: "+getShortTurnSpeed()+", kick "+kick+", acc: "+getShortAcc();
	}
}