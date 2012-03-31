package sdp.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import sdp.common.WorldStateObserver;
import sdp.common.world.WorldState;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;

public class WorldStatePlayer {

	private JFrame frame;
	private WorldState lastWS = null;
	private Thread playThread;
	private JPanel panel;
	private WorldStateObserver obs;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WorldStatePlayer window = new WorldStatePlayer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WorldStatePlayer() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 908, 574);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		panel = new JPanel() {
			protected void paintComponent(Graphics g) {
				Dimension d = this.getSize();
				if (lastWS != null) {
					synchronized (lastWS) {
						g.drawImage(lastWS.getWorldImage(), 0, 0, null);
					}
				} else {
					g.setColor(Color.gray);
					g.fillRect(0, 0, d.width, d.height);
				}
			}
		};
		panel.setFocusable(true);
		panel.setBackground(Color.BLACK);
		panel.setBounds(12, 12, 640, 393);
		frame.getContentPane().add(panel);
		
	}
	
	
	public void play() {
		// TODO init observer
		playThread = new Thread() {
			public void run() {
				while (!interrupted()) {
					lastWS = obs.getNextState();
					panel.repaint();
				}
			};
		};
	}
	
	
	public void stop() {
		playThread.interrupt();
	}
	
}
