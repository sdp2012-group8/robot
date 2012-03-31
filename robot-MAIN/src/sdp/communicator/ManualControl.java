package sdp.communicator;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;

import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 
 * This is the class that gives easy GUI way of sending raw commands to the brick.
 * 
 * Most of the code is automatically generated by the designer so it is a mess. Don't expect comments inside.
 * 
 * @author s0932707
 *
 */
public class ManualControl {

	private JFrame frmManualNxtCommand;
	private JTextField textField;
	private JButton btnConnect;
	private Communicator mComm;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ManualControl window = new ManualControl();
					window.frmManualNxtCommand.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ManualControl() {
		initialize();
	}
	
	private Timer btn_W_pressed = null, btn_A_pressed = null, btn_S_pressed = null, btn_D_pressed = null, btn_SPACE_pressed = null, btn_ENTER_pressed = null;

	private int current_speed = 0, current_turn_speed = 0;
	private static final int max_speed = 53;
	private static final int turn_speed = 627;
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmManualNxtCommand = new JFrame();
		frmManualNxtCommand.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				if (mComm == null)
					return;
				try {
					mComm.sendMessage(opcode.exit);
					Thread.sleep(100);
					mComm.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		frmManualNxtCommand.getContentPane().setFocusable(true);
		
		final JButton btnKick = new JButton("KICK");
		btnKick.setEnabled(false);
		btnKick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					mComm.sendMessage(opcode.kick);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		btnKick.setBounds(12, 75, 117, 25);
		frmManualNxtCommand.getContentPane().add(btnKick);
		
		final JButton btnMoveToWall = new JButton("Move to Wall");
		btnMoveToWall.setEnabled(false);
		btnMoveToWall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					mComm.sendMessage(opcode.move_to_wall);
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});
		btnMoveToWall.setBounds(147, 75, 153, 25);
		frmManualNxtCommand.getContentPane().add(btnMoveToWall);
		
		final JButton btn_control_on = new JButton("Joypad ON");
		btn_control_on.setEnabled(false);
		btn_control_on.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmManualNxtCommand.getContentPane().requestFocus();
			}
		});
		btn_control_on.setBounds(12, 119, 117, 25);
		
		final JLabel lblWAS = new JLabel("W A S D Space Enter");
		lblWAS.setBounds(147, 124, 268, 15);
		frmManualNxtCommand.getContentPane().add(lblWAS);
		
		frmManualNxtCommand.getContentPane().addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				btn_control_on.setEnabled(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
				btn_control_on.setEnabled(false);
			}
		});
		
		frmManualNxtCommand.getContentPane().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				switch (arg0.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W: 
					if (btn_W_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("W");
						try {
							current_speed = max_speed;
							mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							System.out.println("Sending W");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_W_pressed.cancel();
						btn_W_pressed = null;
					}

					break;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A: 
					if (btn_A_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("A");
						try {
							current_turn_speed = turn_speed;
							mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							System.out.println("Sending A");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_A_pressed.cancel();
						btn_A_pressed = null;
					}

					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S: 
					if (btn_S_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("S");
						try {
							current_speed = -max_speed;
							mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							System.out.println("Sending S");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_S_pressed.cancel();
						btn_S_pressed = null;
					}

					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D: 
					if (btn_D_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("D");
						System.out.println("Sending D");
						try {
							current_turn_speed =  -turn_speed;
							mComm.sendMessage(opcode.operate, (short) current_speed, (short)current_turn_speed);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_D_pressed.cancel();
						btn_D_pressed = null;
					}

					break;
				case KeyEvent.VK_SPACE: 
					if (btn_SPACE_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("SPACE");
						System.out.println("Sending SPACE");
						try {
							mComm.sendMessage(opcode.play_sound);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_SPACE_pressed.cancel();
						btn_SPACE_pressed = null;
					}

					break;
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_E:
					if (btn_ENTER_pressed == null) {
						// if pressing button for first time
						lblWAS.setText("ENTER");
						System.out.println("Sending ENTER");
						try {
							mComm.sendMessage(opcode.kick);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						btn_ENTER_pressed.cancel();
						btn_ENTER_pressed = null;
					}
					break;
				}
			}
			
			
			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W: 
					btn_W_pressed = new Timer();
					btn_W_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping W");
							try {
								current_speed = 0;
								mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							} catch (Exception e) {
								e.printStackTrace();
							}
							btn_W_pressed.cancel();
							btn_W_pressed = null;
						}
						
					}, 60);
					break;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A: 
					btn_A_pressed = new Timer();
					btn_A_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping A");
							btn_A_pressed.cancel();
							btn_A_pressed = null;
							try {
								current_turn_speed = 0;
								mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
					}, 60);
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S: 
					btn_S_pressed = new Timer();
					btn_S_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping S");
							try {
								current_speed = 0;
								mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							} catch (Exception e) {
								e.printStackTrace();
							}
							btn_S_pressed.cancel();
							btn_S_pressed = null;
						}
						
					}, 60);
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D: 
					btn_D_pressed = new Timer();
					btn_D_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping D");
							btn_D_pressed.cancel();
							btn_D_pressed = null;
							try {
								current_turn_speed = 0;
								mComm.sendMessage(opcode.operate, (short) current_speed, (short) current_turn_speed);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
					}, 60);
					break;
				case KeyEvent.VK_SPACE: 
					btn_SPACE_pressed = new Timer();
					btn_SPACE_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping SPACE");
							btn_SPACE_pressed.cancel();
							btn_SPACE_pressed = null;
						}
						
					}, 60);
					break;
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_E:
					btn_ENTER_pressed = new Timer();
					btn_ENTER_pressed.schedule(new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							lblWAS.setText(" ");
							System.out.println("Stopping ENTER");
							btn_ENTER_pressed.cancel();
							btn_ENTER_pressed = null;
						}
						
					}, 60);
					break;
				}
			}
		});
		frmManualNxtCommand.setTitle("Manual NXT Command Sender");
		frmManualNxtCommand.setBounds(100, 100, 450, 188);
		frmManualNxtCommand.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmManualNxtCommand.getContentPane().setLayout(null);
		
		final JComboBox comboBox = new JComboBox();
		final opcode[] ops = opcode.values();
		for (int i = 0; i < ops.length; i++)
			comboBox.addItem(ops[i]);
		comboBox.setBounds(12, 9, 166, 24);
		frmManualNxtCommand.getContentPane().add(comboBox);
		
		textField = new JTextField();
		textField.setBounds(190, 12, 246, 19);
		frmManualNxtCommand.getContentPane().add(textField);
		textField.setColumns(10);
		

		
		final JButton btnNewButton = new JButton("Send");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				opcode op = ops[comboBox.getSelectedIndex()];
				if (textField.getText().trim().length() == 0) {
					try {
					mComm.sendMessage(op);
					} catch (Exception e) {
						System.out.println("Can't send message");
					} finally {
						System.out.println("Message "+op+" sent with NO arguments");
					}
				}
				String[] sargs = textField.getText().split(",");
				short[] args = new short[sargs.length];
				for (int i = 0; i < args.length; i++) {
					try {
						args[i] = (short) (int) Integer.parseInt(sargs[i].trim());
					} catch (Exception e) {
						System.out.println("Error sending message. Cannot parse argument '"+sargs[i].trim()+"'");
						return;
					}
				}
				try {
				mComm.sendMessage(op, args);
				} catch (Exception e) {
					System.out.println("Can't send message");
				} finally {
					System.out.println("Message "+op+" sent with "+args.length+" arguments");
				}
			}
		});
		btnNewButton.setEnabled(false);
		btnNewButton.setBounds(12, 38, 166, 25);
		frmManualNxtCommand.getContentPane().add(btnNewButton);
		
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnConnect.setText("Wait...");
				btnConnect.setEnabled(false);

				boolean repeat = true;
				while (repeat) {
					try {
						mComm = new AIComm();
						repeat = false;
					} catch (Exception e) {
						System.out.println("Connection failed. Reattempting...");
					}
				}
				btnConnect.setText("Ready!");
				btnNewButton.setEnabled(true);
				btnKick.setEnabled(true);
				btnMoveToWall.setEnabled(true);
				btn_control_on.setEnabled(!frmManualNxtCommand.getContentPane().hasFocus());
				
			}
		});
		btnConnect.setBounds(190, 38, 246, 25);
		frmManualNxtCommand.getContentPane().add(btnConnect);
		
		frmManualNxtCommand.getContentPane().add(btn_control_on);
		

		
		

	}
}
