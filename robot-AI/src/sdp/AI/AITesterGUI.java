package sdp.AI;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import sdp.AI.AI.mode;
import sdp.common.Communicator;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.communicator.JComm;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.ImageVisualInputProvider;
import sdp.vision.Vision;
import sdp.vision.VisualInputProvider;
import au.edu.jcu.v4l4j.V4L4JConstants;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JPanel;

/**
 * This class is intended to display the world as the UI "sees" it.
 * 
 * It also starts the AI.
 * 
 * @author martinmarinov
 *
 */
public class AITesterGUI {

	private JFrame frmAlphaTeamAi;
	private boolean running = false;
	private AI mAI;
	private VisualInputProvider mInput = null;
	private WorldState lastWS = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AITesterGUI window = new AITesterGUI();
					window.frmAlphaTeamAi.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AITesterGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmAlphaTeamAi = new JFrame();
		frmAlphaTeamAi.setTitle("Alpha Team AI tester");
		frmAlphaTeamAi.setBounds(100, 100, 805, 547);
		frmAlphaTeamAi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmAlphaTeamAi.getContentPane().setLayout(null);
		
		final JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Dimension d = this.getSize();
				if (lastWS != null) {
					synchronized (lastWS) {
						int width = d.width;
						g.setColor(new Color(10, 80, 0));
						g.fillRect(0, 0, width, d.height);
						g.setColor(Color.blue);
						g.fillOval(
								(int)(lastWS.getBlueRobot().getCoords().getX()*width) - 10,
								(int)(lastWS.getBlueRobot().getCoords().getY()*width) - 10,
								20, 20);
						g.setColor(Color.white);
						double dir_x = 0.03*Math.cos(lastWS.getBlueRobot().getAngle()*Math.PI/180d);
						double dir_y = -0.03*Math.sin(lastWS.getBlueRobot().getAngle()*Math.PI/180d);
						g.drawLine(
								(int)(lastWS.getBlueRobot().getCoords().getX()*width),
								(int)(lastWS.getBlueRobot().getCoords().getY()*width),
								(int)((lastWS.getBlueRobot().getCoords().getX()+dir_x)*width),
								(int)((lastWS.getBlueRobot().getCoords().getY()+dir_y)*width));
						g.setColor(new Color(220, 220, 0));
						g.fillOval(
								(int)(lastWS.getYellowRobot().getCoords().getX()*width) - 10,
								(int)(lastWS.getYellowRobot().getCoords().getY()*width) - 10,
								20, 20);
						g.setColor(Color.white);
						dir_x = 0.03*Math.cos(lastWS.getYellowRobot().getAngle()*Math.PI/180d);
						dir_y = -0.03*Math.sin(lastWS.getYellowRobot().getAngle()*Math.PI/180d);
						g.drawLine(
								(int)(lastWS.getYellowRobot().getCoords().getX()*width),
								(int)(lastWS.getYellowRobot().getCoords().getY()*width),
								(int)((lastWS.getYellowRobot().getCoords().getX()+dir_x)*width),
								(int)((lastWS.getYellowRobot().getCoords().getY()+dir_y)*width));
						g.setColor(Color.red);
						g.fillOval(
								(int)(lastWS.getBallCoords().getX()*width) - 3,
								(int)(lastWS.getBallCoords().getY()*width) - 3,
								6, 6);
						g.setColor(Color.black);
						g.fillRect(0, (int) (0.465*width), width, d.height);
					}

				} else {
					g.setColor(Color.gray);
					g.fillRect(0, 0, d.width, d.height);
				}
			}
		};
		panel.setBounds(10, 10, 640, 480);
		frmAlphaTeamAi.getContentPane().add(panel);
		
		ButtonGroup group_input = new ButtonGroup();
		
		final JRadioButton rdbtnUseCamera = new JRadioButton("Use camera");
		rdbtnUseCamera.setSelected(true);
		rdbtnUseCamera.setBounds(656, 43, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnUseCamera);
		
		final JRadioButton rdbtnUseFakeData = new JRadioButton("Use fake data");
		rdbtnUseFakeData.setBounds(656, 68, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnUseFakeData);
		
		group_input.add(rdbtnUseCamera);
		group_input.add(rdbtnUseFakeData);
		
		final JCheckBox chckbxExecuteCommands = new JCheckBox("Connect to brick");
		chckbxExecuteCommands.setSelected(true);
		chckbxExecuteCommands.setBounds(656, 95, 149, 23);
		frmAlphaTeamAi.getContentPane().add(chckbxExecuteCommands);
		
		JLabel lblOurTeam = new JLabel("Our team:");
		lblOurTeam.setBounds(656, 126, 137, 15);
		frmAlphaTeamAi.getContentPane().add(lblOurTeam);
		
		ButtonGroup group_team = new ButtonGroup();
		
		final JRadioButton rdbtnBlue = new JRadioButton("blue");
		rdbtnBlue.setBounds(656, 172, 137, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnBlue);
		rdbtnBlue.setSelected(true);
		
		final JRadioButton rdbtnNewRadioButton = new JRadioButton("yellow");
		rdbtnNewRadioButton.setBounds(656, 145, 137, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnNewRadioButton);
		
		group_team.add(rdbtnBlue);
		group_team.add(rdbtnNewRadioButton);
		
		JLabel lblOurGoal = new JLabel("Our goal:");
		lblOurGoal.setBounds(656, 200, 70, 15);
		frmAlphaTeamAi.getContentPane().add(lblOurGoal);
		
		ButtonGroup group_goal = new ButtonGroup();
		
		final JRadioButton rdbtnLeft = new JRadioButton("left");
		rdbtnLeft.setBounds(656, 217, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnLeft);
		
		final JRadioButton rdbtnRight = new JRadioButton("right");
		rdbtnRight.setSelected(true);
		rdbtnRight.setBounds(656, 244, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnRight);
		
		group_goal.add(rdbtnLeft);
		group_goal.add(rdbtnRight);
		
		JLabel lblRefreshRate = new JLabel("refresh rate");
		lblRefreshRate.setBounds(10, 496, 108, 15);
		frmAlphaTeamAi.getContentPane().add(lblRefreshRate);
		
		final JLabel lblFps = new JLabel("15 fps");
		lblFps.setBounds(304, 496, 70, 15);
		frmAlphaTeamAi.getContentPane().add(lblFps);
		
		final JSlider slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblFps.setText(slider.getValue()+" fps");
			}
		});
		slider.setValue(15);
		slider.setMaximum(30);
		slider.setMinimum(2);
		slider.setBounds(104, 495, 200, 16);
		frmAlphaTeamAi.getContentPane().add(slider);
		
		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!running) {
					Vision vision = new Vision();
					if (mInput == null)
						mInput = rdbtnUseCamera.isSelected() ?
								new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0) :
									new ImageVisualInputProvider(new String[]{
											"data/testImages/pitch2-1.png",
											"data/testImages/pitch2-2.png",
									"data/testImages/pitch2-3.png" }, 25);
						mInput.setCallback(vision);
						Communicator com;
						try {
							com = chckbxExecuteCommands.isSelected() ? new JComm() : null;
						} catch (IOException e) {
							System.out.println("Connection with brick failed! Going into testmode");
							chckbxExecuteCommands.setSelected(false);
							com = null;
						}
						mAI = new AI(com, vision);
						mInput.startCapture();
						mAI.start(rdbtnBlue.isSelected(), rdbtnLeft.isSelected());
						new Thread() {
							public void run() {
								while (running)
									if (mAI != null) {
										lastWS = mAI.getLatestWorldState();
										panel.repaint();
										try {
											Thread.sleep(1000/slider.getValue());
										} catch (InterruptedException e) {}
									}
							};
						}.start();
						running =true;
						btnConnect.setText("Disconnect");
				} else {
					mAI.close();
					running = false;
					btnConnect.setText("Connect");
				}

			}
		});
		btnConnect.setBounds(656, 10, 117, 25);
		frmAlphaTeamAi.getContentPane().add(btnConnect);
		
		final JButton btnSendComm = new JButton("DO");
		btnSendComm.setBounds(656, 300, 117, 25);
		btnSendComm.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mAI.setMode(mode.chase_ball);
			}
			
		});
		frmAlphaTeamAi.getContentPane().add(btnSendComm);

	}
}
