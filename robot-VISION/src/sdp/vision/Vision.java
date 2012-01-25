package sdp.vision;

import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import sdp.common.WorldState;
import sdp.common.WorldStateObservable;


/**
 * The main vision subsystem class.
 * 
 * @author Gediminas Liktaras
 */
public class Vision extends WorldStateObservable implements Observer {
	
	/** Image processor. */
	OldImageProcessor imageProcessor;
	
	
	/**
	 * The main constructor.
	 */
	public Vision() {
		imageProcessor = new OldImageProcessor();
	}
	

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof VisualInputObservable) {
			BufferedImage frame = (BufferedImage) arg;
			
			imageProcessor.process(frame);
			WorldState state = imageProcessor.worldState;
			
			setChanged();
			notifyObservers(state);
		}
	}
	
}