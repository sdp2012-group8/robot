package sdp.simulator;

import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;

public class SimulatorPhysicsEngine extends WorldStateProvider {
	
	private Integer reference_robot_id = null;
	
	// bounding box
	private AABB worldAABB; 
	// world
	private World world;
	

	public SimulatorPhysicsEngine(boolean realtime_simulation, double robot_bounciness) {
		
		// create an axis-aligned bounding box
		worldAABB = new AABB();
		worldAABB.lowerBound.set(new Vec2((float) WorldState.PITCH_WIDTH_CM, (float) WorldState.PITCH_HEIGHT_CM));  
		worldAABB.upperBound.set(new Vec2((float) 0, (float) 0));  
		
		// create the world
		world = new World(new Vec2(0, 0), true, new );
		
	}
	
	// public void setWorldState(WorldState ws, double dt, boolean is_ws_in_cm, Command command, Boolean am_i_blue)
	
	// public static WorldState simulateWs(long time_ms, int fps, WorldState[] states, boolean is_ws_in_cm, Command command, boolean am_i_blue)
	
	/**
	 * 
	 * @param id
	 *            follow a
	 */
	public void centerViewAround(Integer id) {
		reference_robot_id = id;
	}
	
//	private WorldState simulate(double dt) {
//		
//	}
	
//	http://www.4feets.com/2009/03/2d-physics-on-android-using-box2d/
	
}
