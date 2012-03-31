package sdp.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import sdp.common.Painter;
import sdp.common.WorldStateObserver;
import sdp.common.WorldStatePlayer;
import sdp.common.WorldStateProvider;
import sdp.common.world.WorldState;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class WorldStatePlayerGUI {
	
	private static final String movieDir = "data/movies/right0-(-1)0:1(14)";

	private JFrame frame;
	private WorldState lastWS = null;
	private Thread playThread;
	private JPanel panel;
	private WorldStatePlayer player;
	private WorldStateObserver obs;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WorldStatePlayerGUI window = new WorldStatePlayerGUI();
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
	public WorldStatePlayerGUI() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 678, 576);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		panel = new JPanel() {
			protected void paintComponent(Graphics g) {
				Dimension d = this.getSize();
				final int IMAGE_WIDTH = (int) d.getWidth(),
						IMAGE_HEIGHT = (int) (IMAGE_WIDTH*WorldState.PITCH_HEIGHT_CM/WorldState.PITCH_WIDTH_CM);
				
				if (lastWS != null) {
					synchronized (lastWS) {
						BufferedImage im = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
						Painter p = new Painter(im, WorldState.toCentimeters(lastWS));
						p.setOffsets(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
						p.image(true, true);
						g.drawImage(im, 0, 0, null);
						g.setColor(Color.white);
						g.drawString(player.getFrame()+" frame", IMAGE_WIDTH-80, 50);
					}
				} else {
					g.setColor(Color.gray);
					g.fillRect(0, 0, d.width, d.height);
					g.drawString("No world state available", 10, 10);
				}
			}
		};
		panel.setFocusable(true);
		panel.setBackground(Color.BLACK);
		panel.setBounds(12, 12, 640, 393);
		frame.getContentPane().add(panel);
		
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				play();
			}
		});
		btnStart.setBounds(12, 448, 118, 25);
		frame.getContentPane().add(btnStart);
		
		JButton btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		btnStop.setBounds(12, 485, 118, 25);
		frame.getContentPane().add(btnStop);
		
		JButton btnLoad = new JButton("load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});
		btnLoad.setBounds(12, 411, 118, 25);
		frame.getContentPane().add(btnLoad);
		
	}
	
	public void play() {
		player.start();
	}
	
	public void load() {
		try {
		player = new WorldStatePlayer(movieDir);
		obs = new WorldStateObserver(player);
		playThread = new Thread() {
			public void run() {
				while (!interrupted()) {
					lastWS = obs.getNextState();
					panel.repaint();
				}
			};
		};
		playThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void stop() {
		player.stop();
		playThread.interrupt();
	}
}
