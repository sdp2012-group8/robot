package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.Tools;

/**
 * The GUI of the system.
 */
public class GUI extends javax.swing.JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static String className = null;

	/** Creates new form GUI */
	public GUI() {
		initComponents();
	}

	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		radioOurTeam = new javax.swing.ButtonGroup();
		radioOurGoal = new javax.swing.ButtonGroup();
		onScreenImage = new javax.swing.JLabel();
		btnDebugger = new javax.swing.JButton();
		btnBarrelCorrection = new javax.swing.JButton();
		debugSlider = new javax.swing.JSlider();
		blueThreshSlider = new javax.swing.JSlider();
		yellThreshSlider = new javax.swing.JSlider();
		ballSearchSlider = new javax.swing.JSlider();
		debugLabel = new javax.swing.JLabel();
		blueThreshLabel = new javax.swing.JLabel();
		yellThreshLabel = new javax.swing.JLabel();
		ballSearchLabel = new javax.swing.JLabel();
		btnPitch1 = new javax.swing.JButton();
		btnPitch2 = new javax.swing.JButton();
		modeLabel = new javax.swing.JLabel();
		modeSlider = new javax.swing.JSlider();
		radioBlue = new javax.swing.JRadioButton();
		radioYell = new javax.swing.JRadioButton();
		radioLeftGoal = new javax.swing.JRadioButton();
		radioRightGoal = new javax.swing.JRadioButton();
		labelDebug = new javax.swing.JLabel();
		labelOurGoal = new javax.swing.JLabel();
		toggleStop = new javax.swing.JToggleButton();
		toggleMouse = new javax.swing.JToggleButton();
		toggleDefend = new javax.swing.JToggleButton();
		toggleAttack = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setName("Vision"); // NOI18N
		setResizable(false);

		onScreenImage.setText("Image goes here");

		btnDebugger.setText("Debugger");
		btnDebugger.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDebuggerActionPerformed(evt);
			}
		});

		btnBarrelCorrection.setText("Toggle Barrel Correction");
		btnBarrelCorrection
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						btnBarrelCorrectionActionPerformed(evt);
					}
				});

		debugSlider.setMaximum(5);
		debugSlider.setPaintLabels(true);
		debugSlider.setToolTipText("");
		debugSlider.setValue(3);
		debugSlider.setName("Debug"); // NOI18N
		debugSlider.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				debugSliderStateChanged(evt);
			}
		});

		blueThreshSlider.setMaximum(765);
		blueThreshSlider.setValue(350);
		blueThreshSlider
				.addChangeListener(new javax.swing.event.ChangeListener() {
					public void stateChanged(javax.swing.event.ChangeEvent evt) {
						blueThreshSliderStateChanged(evt);
					}
				});

		yellThreshSlider.setMaximum(765);
		yellThreshSlider.setValue(150);
		yellThreshSlider
				.addChangeListener(new javax.swing.event.ChangeListener() {
					public void stateChanged(javax.swing.event.ChangeEvent evt) {
						yellThreshSliderStateChanged(evt);
					}
				});

		ballSearchSlider.setMaximum(1000);
		ballSearchSlider.setValue(700);
		ballSearchSlider
				.addChangeListener(new javax.swing.event.ChangeListener() {
					public void stateChanged(javax.swing.event.ChangeEvent evt) {
						ballSearchSliderStateChanged(evt);
					}
				});

		debugLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		debugLabel.setLabelFor(radioBlue);
		debugLabel.setText("Our team:");

		blueThreshLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		blueThreshLabel.setLabelFor(blueThreshSlider);
		blueThreshLabel.setText("Blue threshold:");

		yellThreshLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		yellThreshLabel.setLabelFor(yellThreshSlider);
		yellThreshLabel.setText("Yellow threshold:");

		ballSearchLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		ballSearchLabel.setLabelFor(ballSearchSlider);
		ballSearchLabel.setText("Ball search distance:");

		btnPitch1.setText("Pitch 1");
		btnPitch1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnPitch1ActionPerformed(evt);
			}
		});

		btnPitch2.setText("Pitch 2");
		btnPitch2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnPitch2ActionPerformed(evt);
			}
		});

		modeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		modeLabel.setLabelFor(modeSlider);
		modeLabel.setText("Statistical mode for angle:");

		modeSlider.setMaximum(24);
		modeSlider.setMinimum(1);
		modeSlider.setValue(ImageProcessor.mode);
		modeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				modeSliderStateChanged(evt);
			}
		});

		radioOurTeam.add(radioBlue);
		try {
			radioBlue.setSelected((boolean) Class.forName(className).getField(
					"weAreBlueTeam").getBoolean(Class.forName(className)));
		} catch (Exception e) {
			System.out
					.println("Warning: you are calling Vision from an incompatible class");
//			radioBlue.setSelected(Main2.weAreBlueTeam);
			radioBlue.setSelected(true);
		}
		radioBlue.setText("Blue Team");
		radioBlue.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				radioBlueActionPerformed(evt);
			}
		});

		radioOurTeam.add(radioYell);
		radioYell.setText("Yell Team");
		try {
			radioYell.setSelected(!(boolean) Class.forName(className).getField(
					"weAreBlueTeam").getBoolean(Class.forName(className)));
		} catch (Exception e) {
//			radioYell.setSelected(!Main2.weAreBlueTeam);
			radioYell.setSelected(false);
		}
		radioYell.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				radioYellActionPerformed(evt);
			}
		});

		radioOurGoal.add(radioLeftGoal);
		try {
			radioLeftGoal.setSelected(!(boolean) Class.forName(className)
					.getField("shootingLeft").getBoolean(
							Class.forName(className)));
		} catch (Exception e) {
//			radioLeftGoal.setSelected(!Main2.shootingLeft);
			radioLeftGoal.setSelected(false);
		}
		radioLeftGoal.setText("Left goal");
		radioLeftGoal.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				radioLeftGoalActionPerformed(evt);
			}
		});

		radioOurGoal.add(radioRightGoal);
		try {
			radioRightGoal.setSelected((boolean) Class.forName(className)
					.getField("shootingLeft").getBoolean(
							Class.forName(className)));
		} catch (Exception e) {
//			radioRightGoal.setSelected(Main2.shootingLeft);
			radioRightGoal.setSelected(true);
		}
		radioRightGoal.setText("Right Goal");
		radioRightGoal.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				radioRightGoalActionPerformed(evt);
			}
		});

		labelDebug.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		labelDebug.setLabelFor(debugSlider);
		labelDebug.setText("Debug level:");

		labelOurGoal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		labelOurGoal.setLabelFor(radioLeftGoal);
		labelOurGoal.setText("Our goal:");

		toggleStop.setText("Start Planning");
		toggleStop.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				toggleStopActionPerformed(evt);
			}
		});
		toggleMouse.setText("Mouse Mode");
		toggleMouse.setSelected(ImageProcessor.useMouse);
		toggleMouse.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				toggleMouseActionPerformed(evt);
			}
		});

		toggleDefend.setText("Defend Penalty");
		toggleAttack.setText("Play Penalty");
		toggleDefend.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				toggleDefendActionPerformed(evt);
			}
		});

		toggleAttack.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {

				toggleAttackActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addComponent(
												onScreenImage,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												640,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(
																toggleStop,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																toggleMouse,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																btnBarrelCorrection,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																ballSearchSlider,
																0, 0,
																Short.MAX_VALUE)
														.addComponent(
																yellThreshSlider,
																0, 0,
																Short.MAX_VALUE)
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				radioLeftGoal)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																				17,
																				Short.MAX_VALUE)
																		.addComponent(
																				radioRightGoal))
														.addComponent(
																debugSlider,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				btnPitch1)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				btnDebugger,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				65,
																				Short.MAX_VALUE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				btnPitch2))
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				toggleDefend,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				90,
																				Short.MAX_VALUE)
																		.addComponent(
																				toggleAttack,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				90,
																				Short.MAX_VALUE))
														.addComponent(
																blueThreshLabel,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																blueThreshSlider,
																0, 0,
																Short.MAX_VALUE)
														.addGroup(
																javax.swing.GroupLayout.Alignment.TRAILING,
																layout
																		.createSequentialGroup()
																		.addComponent(
																				radioBlue)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addComponent(
																				radioYell)
																		.addGap(
																				4,
																				4,
																				4))
														.addComponent(
																debugLabel,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																labelDebug,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																labelOurGoal,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																yellThreshLabel,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																ballSearchLabel,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																modeLabel,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE)
														.addComponent(
																modeSlider,
																javax.swing.GroupLayout.Alignment.TRAILING,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																187,
																Short.MAX_VALUE))
										.addContainerGap()));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addGroup(
												layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(
																layout
																		.createSequentialGroup()
																		.addComponent(
																				debugLabel,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				17,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addGap(
																				3,
																				3,
																				3)
																		.addGroup(
																				layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.BASELINE)
																						.addComponent(
																								radioBlue)
																						.addComponent(
																								radioYell))
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				labelOurGoal,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				17,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(
																				layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.BASELINE)
																						.addComponent(
																								radioLeftGoal)
																						.addComponent(
																								radioRightGoal))
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				labelDebug,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				17,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				debugSlider,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				blueThreshLabel)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				blueThreshSlider,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				yellThreshLabel)
																		.addGap(
																				2,
																				2,
																				2)
																		.addComponent(
																				yellThreshSlider,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				ballSearchLabel)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				ballSearchSlider,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				modeLabel)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				modeSlider,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				javax.swing.GroupLayout.DEFAULT_SIZE,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(
																				btnBarrelCorrection)
																		.addComponent(
																				toggleStop)
																		.addComponent(
																				toggleMouse)
																		.addGap(
																				1,
																				1,
																				1)
																		.addGroup(
																				layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.BASELINE)
																						.addComponent(
																								btnPitch1)
																						.addComponent(
																								btnDebugger)
																						.addComponent(
																								btnPitch2))
																		.addGap(
																				2,
																				2,
																				2)
																		.addGroup(
																				layout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.BASELINE)
																						.addComponent(
																								toggleDefend)
																						.addComponent(
																								toggleAttack))
																		.addGap(
																				2,
																				2,
																				2))
														.addComponent(
																onScreenImage,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																480,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void btnBarrelCorrectionActionPerformed(
			java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBarrelCorrectionActionPerformed
		ImageProcessor.useBarrelDistortion = !(ImageProcessor.useBarrelDistortion);

	}// GEN-LAST:event_btnBarrelCorrectionActionPerformed

	private void btnDebuggerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDebuggerActionPerformed
		// TODO: ITS HERE

//		Main2.nxt.adjustWheelSpeeds(0, -600);
		Tools.rest(500);
//		Main2.nxt.stop();
	}// GEN-LAST:event_btnDebuggerActionPerformed

	private void btnPitch1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnPitch1ActionPerformed
		blueThreshSlider.setValue(350);
		ImageProcessor.blueThreshold = 350;
		yellThreshSlider.setValue(150);
		ImageProcessor.yellThreshold = 150;
		ballSearchSlider.setValue(700);
		ImageProcessor.searchdistance = 700;
		modeSlider.setValue(5);
		ImageProcessor.mode = 5;
		ImageProcessor.xlowerlimit = 0;
		ImageProcessor.xupperlimit = 630;
		ImageProcessor.ylowerlimit = 85;
		ImageProcessor.yupperlimit = 410;

	}// GEN-LAST:event_btnPitch1ActionPerformed

	private void btnPitch2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnPitch2ActionPerformed
		blueThreshSlider.setValue(350);
		ImageProcessor.blueThreshold = 350;
		yellThreshSlider.setValue(150);
		ImageProcessor.yellThreshold = 150;
		ballSearchSlider.setValue(700);
		ImageProcessor.searchdistance = 700;
		modeSlider.setValue(5);
		ImageProcessor.mode = 5;
		ImageProcessor.xlowerlimit = 10;
		ImageProcessor.xupperlimit = 640;
		ImageProcessor.ylowerlimit = 70;
		ImageProcessor.yupperlimit = 405;
	}// GEN-LAST:event_btnPitch2ActionPerformed

	private void debugSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_debugSliderStateChanged
		ImageProcessor.DEBUG_LEVEL = debugSlider.getValue();
		if (debugSlider.getValue() < 4) {
			// debugOutputBlue.setText("");
			// debugOutputYell.setText("");
		}
	}// GEN-LAST:event_debugSliderStateChanged

	private void blueThreshSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_blueThreshSliderStateChanged
		ImageProcessor.blueThreshold = blueThreshSlider.getValue();
	}// GEN-LAST:event_blueThreshSliderStateChanged

	private void yellThreshSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_yellThreshSliderStateChanged
		ImageProcessor.yellThreshold = yellThreshSlider.getValue();
	}// GEN-LAST:event_yellThreshSliderStateChanged

	private void ballSearchSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_ballSearchSliderStateChanged
		ImageProcessor.searchdistance = ballSearchSlider.getValue();
	}// GEN-LAST:event_ballSearchSliderStateChanged

	private void modeSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_modeSliderStateChanged
		int v = modeSlider.getValue();
		if (v == 1) {
			ImageProcessor.method = 2;
		} else {
			ImageProcessor.method = 1;
			ImageProcessor.mode = v;
		}
	}// GEN-LAST:event_modeSliderStateChanged

	private void radioLeftGoalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_radioLeftGoalActionPerformed
		try {
			Class.forName(className).getField("shootingLeft").setBoolean(
					Class.forName(className), false);
		} catch (Exception e) {
//			Main2.shootingLeft = false;
		}
	}// GEN-LAST:event_radioLeftGoalActionPerformed

	private void radioBlueActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_radioBlueActionPerformed
		try {
			Class.forName(className).getField("weAreBlueTeam").setBoolean(
					Class.forName(className), true);
		} catch (Exception e) {
//			Main2.weAreBlueTeam = true;
		}
	}// GEN-LAST:event_radioBlueActionPerformed

	private void radioYellActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_radioYellActionPerformed
		try {
			Class.forName(className).getField("weAreBlueTeam").setBoolean(
					Class.forName(className), false);
		} catch (Exception e) {
//			Main2.weAreBlueTeam = false;
		}
	}// GEN-LAST:event_radioYellActionPerformed

	private void radioRightGoalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_radioRightGoalActionPerformed
		try {
			Class.forName(className).getField("shootingLeft").setBoolean(
					Class.forName(className), true);
		} catch (Exception e) {
//			Main2.shootingLeft = true;
		}
	}// GEN-LAST:event_radioRightGoalActionPerformed

	private void toggleStopActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleStopActionPerformed
		if (toggleStop.getText().startsWith("Start")) {
			toggleStop.setText("Stop Planning");
		} else {
			toggleStop.setText("Start Planning");
		}
		try {
			Class.forName(className).getField("robotMoving").setBoolean(
					Class.forName(className),
					!(Class.forName(className).getField("robotMoving")
							.getBoolean(Class.forName(className))));
		} catch (Exception e) {
			System.out
					.println("Warning: you are calling Vision from an incompatible class");
//			Main2.robotMoving = !Main2.robotMoving;
		}
	}// GEN-LAST:event_toggleStopActionPerformed

	private void toggleMouseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleStopActionPerformed
		ImageProcessor.useMouse = !ImageProcessor.useMouse;
	}// GEN-LAST:event_toggleMouseActionPerformed

	private void toggleDefendActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleStopActionPerformed
		System.out.println("DEFEND BUTTON");
//		Main2.blockingMode = !Main2.blockingMode;
	}// GEN-LAST:event_toggleDefendActionPerformed

	private void toggleAttackActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_toggleStopActionPerformed
		// TODO: the hardcoded shooting penalty code goes here
		System.out.println("ATTACK BUTTON");

		boolean side = Math.random() > 0.5;
		double angle = side ? Math.toRadians(-25) : Math.toRadians(25);

//		Main2.nxt.rotateBy(angle);
//		Main2.nxt.kick();
//		Main2.nxt.stop();


	}// GEN-LAST:event_toggleAttackActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel ballSearchLabel;
	private javax.swing.JSlider ballSearchSlider;
	private javax.swing.JLabel blueThreshLabel;
	private javax.swing.JSlider blueThreshSlider;
	private javax.swing.JButton btnBarrelCorrection;
	private javax.swing.JButton btnPitch1;
	private javax.swing.JButton btnPitch2;
	private javax.swing.JButton btnDebugger;
	private javax.swing.JLabel debugLabel;
	private javax.swing.JSlider debugSlider;
	private javax.swing.JLabel labelDebug;
	private javax.swing.JLabel labelOurGoal;
	private javax.swing.JLabel modeLabel;
	private javax.swing.JSlider modeSlider;
	private javax.swing.JLabel onScreenImage;
	private javax.swing.JRadioButton radioBlue;
	private javax.swing.JRadioButton radioLeftGoal;
	private javax.swing.ButtonGroup radioOurGoal;
	private javax.swing.ButtonGroup radioOurTeam;
	private javax.swing.JRadioButton radioRightGoal;
	private javax.swing.JRadioButton radioYell;
	private javax.swing.JToggleButton toggleStop;
	private javax.swing.JToggleButton toggleMouse;
	private javax.swing.JToggleButton toggleDefend;
	private javax.swing.JButton toggleAttack;
	private javax.swing.JLabel yellThreshLabel;
	private javax.swing.JSlider yellThreshSlider;

	// End of variables declaration//GEN-END:variables

	public void setImage(BufferedImage image) {
		if (image != null)
			onScreenImage.getGraphics().drawImage(image, 0, 0,
					image.getWidth(), image.getHeight(), null);
	}

	public javax.swing.JLabel getImage() {
		return onScreenImage;
	}

	public static void setDebugOutputBlue(String text) {
		if (text != null) {
			// debugOutputBlue.setText(text);
		}
	}

	public static void setDebugOutputYell(String text) {
		if (text != null) {
			// debugOutputYell.setText(text);
		}
	}
}
