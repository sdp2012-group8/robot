package sdp.gui;

import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import sdp.AI.genetic.Game;
import sdp.common.Painter;
import sdp.common.WorldStateObserver;
import sdp.common.WorldStatePlayer;
import sdp.common.world.WorldState;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JSlider;
import javax.swing.JLabel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingConstants;

/**
 * Plays back "recorded games" i.e. a folder containing world state frames. Those frames would be normally generated via {@link WorldState#saveMovie(WorldState[], String)
 * @author Martin Marinov
 *
 */
public class WorldStatePlayerGUI {
	
	JFileChooser fileChooser;

	private final static int DEFAULT_MOVIE_FPS = Game.FPS;
	private JFrame frmWorldstateMoviePlayer;
	private WorldState lastWS = null;
	private Thread playThread;
	private JPanel panel;
	private JSlider sliderFPS, sliderProgress;
	final private WorldStatePlayer player = new WorldStatePlayer(DEFAULT_MOVIE_FPS);
	final private WorldStateObserver obs = new WorldStateObserver(player);
	private boolean manualDrag = false;
	private JLabel lblFrame;
	private JButton btnNewButton;
	private JLabel lblTotalFrames;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WorldStatePlayerGUI window = new WorldStatePlayerGUI();
					window.frmWorldstateMoviePlayer.setVisible(true);
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
		
		fileChooser = new JFileChooser(new File("data/movies/"));
		fileChooser.setDialogTitle("Choose movie folder");
	    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		
		frmWorldstateMoviePlayer = new JFrame();
		frmWorldstateMoviePlayer.setResizable(false);
		frmWorldstateMoviePlayer.setTitle("WorldState movie player");
		frmWorldstateMoviePlayer.setBounds(100, 100, 666, 463);
		frmWorldstateMoviePlayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmWorldstateMoviePlayer.getContentPane().setLayout(null);
		
		panel = new JPanel() {
			private static final long serialVersionUID = -568325754904250978L;

			protected void paintComponent(Graphics g) {
				Dimension d = this.getSize();
				final int IMAGE_WIDTH = (int) d.getWidth(),
						IMAGE_HEIGHT = (int) (IMAGE_WIDTH*WorldState.PITCH_HEIGHT_CM/WorldState.PITCH_WIDTH_CM);
				
				if (lastWS != null) {
					synchronized (lastWS) {
						BufferedImage im = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
						Painter p = new Painter(im, WorldState.toCentimeters(lastWS));
						p.setOffsets(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
						
						// draw table
						p.g.setColor(new Color(10, 80, 0));
						p.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
						// draw goals
						p.g.setColor(new Color(180, 180, 180));
						p.fillRect(0,
								(int) (IMAGE_WIDTH*(WorldState.PITCH_HEIGHT_CM/2-60/2)/WorldState.PITCH_WIDTH_CM),
								(int) (IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
								(int) (IMAGE_WIDTH*60/WorldState.PITCH_WIDTH_CM));
						p.fillRect((int) (IMAGE_WIDTH - IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
								(int) (IMAGE_WIDTH*(WorldState.PITCH_HEIGHT_CM/2-60/2)/WorldState.PITCH_WIDTH_CM),
								(int) (IMAGE_WIDTH*2/WorldState.PITCH_WIDTH_CM),
								(int) (IMAGE_WIDTH*60/WorldState.PITCH_WIDTH_CM));
						
						p.image(true, true);
						g.drawImage(im, 0, 0, null);
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
		panel.setBounds(12, 12, 640, 298);
		frmWorldstateMoviePlayer.getContentPane().add(panel);
		
		JButton btnLoad = new JButton("load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});
		btnLoad.setBounds(534, 387, 118, 25);
		frmWorldstateMoviePlayer.getContentPane().add(btnLoad);
		
		lblFrame = new JLabel("0 frame");
		lblFrame.setBounds(12, 322, 275, 15);
		frmWorldstateMoviePlayer.getContentPane().add(lblFrame);
		
		sliderProgress = new JSlider();
		sliderProgress.setMaximum(1000);
		sliderProgress.setValue(0);
		sliderProgress.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				manualDrag = true;
				player.setFPS(0);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				manualDrag = false;
				player.setFPS(getFPS());
			}
		});
		sliderProgress.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (manualDrag) {
					player.setFrame(getFrame());
				}
				lblFrame.setText(String.format("%.2f frame", player.getFrame()));
			}
		});
		sliderProgress.setBounds(12, 349, 640, 16);
		frmWorldstateMoviePlayer.getContentPane().add(sliderProgress);
		
		JLabel lblPlaybackSpeed = new JLabel("Playback speed:");
		lblPlaybackSpeed.setBounds(142, 377, 118, 15);
		frmWorldstateMoviePlayer.getContentPane().add(lblPlaybackSpeed);
		
		final JLabel lblFps = new JLabel(DEFAULT_MOVIE_FPS+" fps");
		lblFps.setBounds(272, 377, 118, 15);
		frmWorldstateMoviePlayer.getContentPane().add(lblFps);
		
		sliderFPS = new JSlider();
		sliderFPS.setMinorTickSpacing(1);
		sliderFPS.setMajorTickSpacing(5);
		sliderFPS.setSnapToTicks(true);
		sliderFPS.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				player.setFPS(getFPS());
				lblFps.setText(String.format("%.2f fps ", getFPS()));
				
				if (btnNewButton != null)
					btnNewButton.setText(sliderFPS.getValue() != 0 ? "❚❚" : "►");
			}
		});
		
		sliderFPS.setMaximum(250);
		sliderFPS.setMinimum(-250);
		sliderFPS.setValue(10*DEFAULT_MOVIE_FPS);
		sliderFPS.setBounds(142, 404, 380, 16);
		frmWorldstateMoviePlayer.getContentPane().add(sliderFPS);
		
		btnNewButton = new JButton("❚❚");
		btnNewButton.setMnemonic('P');
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (getFPS() == 0) {
					setFPS(DEFAULT_MOVIE_FPS);
				} else {
					setFPS(0);
				}
			}
		});
		btnNewButton.setBounds(12, 387, 118, 25);
		frmWorldstateMoviePlayer.getContentPane().add(btnNewButton);
		
		lblTotalFrames = new JLabel("Total: 0 frames");
		lblTotalFrames.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTotalFrames.setBounds(435, 322, 217, 15);
		frmWorldstateMoviePlayer.getContentPane().add(lblTotalFrames);
		
	}
	
	public void load() {
		String movieDir;
		String name;
		if (fileChooser.showOpenDialog(frmWorldstateMoviePlayer) == JFileChooser.APPROVE_OPTION) {
			
			movieDir = fileChooser.getSelectedFile().getAbsolutePath();
			name = fileChooser.getSelectedFile().getName();
			
		} else
			return;

		try {
			player.loadMovie(movieDir);
			frmWorldstateMoviePlayer.setTitle("WorldState movie player - "+name+" - "+String.format("%.1f sec", player.getFrameCount()/(double) DEFAULT_MOVIE_FPS));
			lblTotalFrames.setText("Total: "+player.getFrameCount()+" frames");
			player.setFPS(getFPS());
			playThread = new Thread() {
				public void run() {
					while (!interrupted()) {
						lastWS = obs.getNextState();
						panel.repaint();
						synchronized (sliderProgress) {
							sliderProgress.setValue((int) (1000*player.getFrame()/player.getFrameCount()));
						}
					}
				};
			};
			playThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setFPS(double value) {
		sliderFPS.setValue((int) (value * 10));
	}
	
	private double getFPS() {
		double value = sliderFPS.getValue()/10d;
		if (Math.abs(value) < 5) {
			return value/5d;
		}
		if (Math.abs(value) < 10) {
			if (value > 0) {
				return 1 + 9*(value - 5)/5;
			} else {
				return -1 - 9*(-value - 5)/5;
			}
		}
		
		if (Math.abs(value) > 20) {
			if (value > 0) {
				return 20 + (value - 20)*20;
			} else {
				return -20 - (-value - 20)*20;
			}
		}
		return value;
	}
	
	private double getFrame() {
		return player.getFrameCount()*sliderProgress.getValue()/1000d;
	}
}
