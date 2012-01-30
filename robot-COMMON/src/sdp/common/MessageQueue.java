package sdp.common;

import java.io.IOException;
import java.sql.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import sdp.common.Communicator.opcode;


/**
 * This class implements a message queue scheduler. It could send messages in a queue
 * with a given delay.
 * 
 * Ideally the AI should use this method to have its message queue built
 * before sending it via a Communicator.
 * 
 * @author Martin Marinov
 *
 */
public class MessageQueue {
	
	private Timer mTimer = null;
	private Date mLastMsg = null;
	private Communicator mComm;
	private int tasks_pending = 0;
	
	private final static Logger LOGGER = Logger.getLogger(MessageQueue.class .getName());
	
	/**
	 * Intialize a new message queue
	 * @param com the communicator that will be used to send the delayed commands
	 */
	public MessageQueue(Communicator com) {
		mComm = com;
		if (com == null)
			LOGGER.info("TEST MODE: No Communicator supplied. Messages will be instead printed in LOGGER.");
	}
	
	/**
	 * Adds current message to the message queue.
	 * @param delay delay in s to wait after sending previous message
	 * @param op the opcode of the message
	 * @param args the arguments of the message
	 */
	public void addMessageToQueue(double delay_s, final opcode op, final byte... args) {
		final long delay = (long) (delay_s*1000);
		if (mLastMsg == null)
			mLastMsg = new Date(System.currentTimeMillis()+delay);
		else
			mLastMsg = new Date(mLastMsg.getTime()+delay);
		if (mTimer == null)
			mTimer = new Timer();
		tasks_pending++;
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if (mComm != null)
						mComm.sendMessage(op, args);
					else
						LOGGER.info(op+" args"+getHumanReadableArgs(args));
				} catch (IOException e) {
					LOGGER.warning("Error sending message "+op+" from queue");
					e.printStackTrace();
				}
				tasks_pending--;
				if (tasks_pending == 0) {
					// if no more tasks left, kill timer so we might be able to free some memory
					mTimer.cancel();
					mTimer = null;
				}
			}
		}, mLastMsg);
	}
	
	/**
	 * Cancels all messages pending on the queue.
	 */
	public void cancelAllMessages() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		mLastMsg = null;
		tasks_pending = 0;
	}
	
	/**
	 * Close the MessageQueue and the Communicator gracefully.
	 */
	public void close() {
		cancelAllMessages();
		if (mComm != null) {
			mComm.close();
		}
	}
	
	/**
	 * @return the number of remaining messages on queue
	 */
	public int tasks_pending() {
		return tasks_pending;
	}
	
	private String getHumanReadableArgs(byte[] args) {
		if (args.length == 0)
			return "[∅]";
		else if (args.length == 1)
			return "["+args[0]+"]";
		String ans = "["+args[0];
		for (int i = 1; i < args.length; i++)
			ans += ", "+args[i];
		return ans+"]";
	}

}
