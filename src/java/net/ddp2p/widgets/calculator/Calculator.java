/**************************************************
 * Author:(Jiuyang Zhou, May 29,2013 at Florida)
 * Charset: UTF-8
 ***************************************************/
package net.ddp2p.widgets.calculator;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;

public class Calculator implements ActionListener {
	private final static int OP_NULL = -1;
	private final static int OP_PLUS = 1;
	private final static int OP_MINUS = 2;
	private final static int OP_TIME = 3;
	private final static int OP_DIV = 4;

	public JFrame frame;
	public Container con;

	public JPanel panel;
	public JPanel displayPanel;

	public JMenuBar menuBar;
	public JMenu checkMenu;
	public JMenu editMenu;
	public JMenu helpMenu;
	public JMenuItem standardItem;
	public JMenuItem copyItem;
	public JMenuItem pasteItem;
	public JMenuItem aboutItem;

	public JTextField displayFieldTop;
	public JTextField displayFieldBottom;

	public JButton buttonMC;
	public JButton buttonMR;
	public JButton buttonMS;
	public JButton buttonMPlus;
	public JButton buttonMMinus;
	public JButton buttonBack;
	public JButton buttonCE;
	public JButton buttonC;
	public JButton buttonNegative;
	public JButton buttonRoot;
	public JButton button7;
	public JButton button8;
	public JButton button9;
	public JButton buttonDiv;
	public JButton buttonPercent;
	public JButton button4;
	public JButton button5;
	public JButton button6;
	public JButton buttonTime;
	public JButton buttonBackwards;
	public JButton button1;
	public JButton button2;
	public JButton button3;
	public JButton buttonMinus;
	public JButton buttonEqual;
	public JButton button0;
	public JButton buttonDot;
	public JButton buttonPlus;

	public JPanel panel1;
	public JButton[] buttonArray;

	// operation variables
	public String stock;
	public String displayTop;
	public String displayBottom;
	public String firstOP;
	public String secondOP;
	public String result;
	public int firstInt;
	public int secondInt;
	public int resultInt;
	public float firstFloat;
	public float secondFloat;
	public float resultFloat;
	public BigDecimal b1;
	public BigDecimal b2;
	public int op;
	public boolean errorFlag;
	public boolean dotFlag;
	public boolean plusFlag;
	public boolean minusFlag;
	public boolean timeFlag;
	public boolean divFlag;
	public boolean equalFlag;

	public Calculator() {
		frame = new JFrame("Calculator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// setting the frame size
		int inset_width = 570;
		int inset_hight = 224;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setBounds(inset_width, inset_hight, screenSize.width
				- inset_width * 2, screenSize.height - inset_hight * 2);
		// setting the frame icon
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.getImage("src/programe/title.png");
		frame.setIconImage(img);
		// setting frame resizable and disable the maximization button
		frame.setResizable(false);

		// get the container of the frame
		con = frame.getContentPane();
		con.setLayout(new GridLayout(1, 1));

		// initializing panel
		panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		displayPanel = new JPanel();
		displayPanel.setBorder(new LineBorder(Color.gray));

		// initializing menu bar, menu, and menu items
		menuBar = new JMenuBar();
		checkMenu = new JMenu("Check(V)");
		editMenu = new JMenu("Edit(E)");
		helpMenu = new JMenu("Help(H)");
		standardItem = new JMenuItem("Standard(T)");
		copyItem = new JMenuItem("Copy(C)");
		copyItem.addActionListener(this);
		pasteItem = new JMenuItem("Paste(P)");
		pasteItem.addActionListener(this);
		aboutItem = new JMenuItem("About(A)");
		aboutItem.addActionListener(this);

		// setting font and keyboard shortcuts
		//checkMenu.setFont(new Font("", Font.PLAIN, 13));
		checkMenu.setMnemonic('v');
		//editMenu.setFont(new Font("", Font.PLAIN, 13));
		editMenu.setMnemonic('e');
		//helpMenu.setFont(new Font("", Font.PLAIN, 13));
		helpMenu.setMnemonic('h');
		//standardItem.setFont(new Font("", Font.PLAIN, 13));
		standardItem.setMnemonic('t');
		standardItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
				InputEvent.ALT_MASK));
		//copyItem.setFont(new Font("", Font.PLAIN, 13));
		copyItem.setMnemonic('c');
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				InputEvent.CTRL_MASK));
		//pasteItem.setFont(new Font("", Font.PLAIN, 13));
		pasteItem.setMnemonic('p');
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
				InputEvent.CTRL_MASK));
		//aboutItem.setFont(new Font("", Font.PLAIN, 13));
		aboutItem.setMnemonic('a');
		aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

		displayFieldTop = new JTextField(17);
		displayFieldTop.setEditable(false);
		displayFieldTop.setHorizontalAlignment(JTextField.RIGHT);
		displayFieldTop.setBorder(new LineBorder(Color.white));
		displayFieldTop.setFont(new Font("Consolas", Font.PLAIN, 12));
		displayFieldBottom = new JTextField(17);
		displayFieldBottom.setEditable(false);
		displayFieldBottom.setBorder(new LineBorder(Color.white));
		displayFieldBottom.setHorizontalAlignment(JTextField.RIGHT);
		displayFieldBottom.setFont(new Font("Consolas", Font.PLAIN, 20));
		displayFieldBottom.setText("0");

		// initializing buttons
		buttonMC = new JButton("MC");
		buttonMC.addActionListener(this);
		buttonMR = new JButton("MR");
		buttonMR.addActionListener(this);
		buttonMS = new JButton("MS");
		buttonMS.addActionListener(this);
		buttonMPlus = new JButton("M+");
		buttonMPlus.addActionListener(this);
		buttonMMinus = new JButton("M-");
		buttonMMinus.addActionListener(this);
		buttonBack = new JButton("Back");
		buttonBack.addActionListener(this);
		buttonCE = new JButton("CE");
		buttonCE.addActionListener(this);
		buttonC = new JButton("C");
		buttonC.addActionListener(this);
		buttonNegative = new JButton("-");
		buttonNegative.addActionListener(this);
		buttonRoot = new JButton("Root");
		buttonRoot.addActionListener(this);
		button7 = new JButton("7");
		button7.addActionListener(this);
		button8 = new JButton("8");
		button8.addActionListener(this);
		button9 = new JButton("9");
		button9.addActionListener(this);
		buttonDiv = new JButton("/");
		buttonDiv.addActionListener(this);
		buttonPercent = new JButton("%");
		buttonPercent.addActionListener(this);
		button4 = new JButton("4");
		button4.addActionListener(this);
		button5 = new JButton("5");
		button5.addActionListener(this);
		button6 = new JButton("6");
		button6.addActionListener(this);
		buttonTime = new JButton("X");
		buttonTime.addActionListener(this);
		buttonBackwards = new JButton("1/X");
		buttonBackwards.addActionListener(this);
		button1 = new JButton("1");
		button1.addActionListener(this);
		button2 = new JButton("2");
		button2.addActionListener(this);
		button3 = new JButton("3");
		button3.addActionListener(this);
		buttonMinus = new JButton("-");
		buttonMinus.addActionListener(this);
		buttonEqual = new JButton("=");
		buttonEqual.addActionListener(this);
		button0 = new JButton("0");
		button0.addActionListener(this);
		buttonDot = new JButton(".");
		buttonDot.addActionListener(this);
		buttonPlus = new JButton("+");
		buttonPlus.addActionListener(this);

		// buttonArray
		buttonArray = new JButton[28];
		buttonArray[0] = buttonMC;
		buttonArray[1] = buttonMR;
		buttonArray[2] = buttonMS;
		buttonArray[3] = buttonMPlus;
		buttonArray[4] = buttonMMinus;
		buttonArray[5] = buttonBack;
		buttonArray[6] = buttonCE;
		buttonArray[7] = buttonC;
		buttonArray[8] = buttonNegative;
		buttonArray[9] = buttonRoot;
		buttonArray[10] = button7;
		buttonArray[11] = button8;
		buttonArray[12] = button9;
		buttonArray[13] = buttonDiv;
		buttonArray[14] = buttonPercent;
		buttonArray[15] = button4;
		buttonArray[16] = button5;
		buttonArray[17] = button6;
		buttonArray[18] = buttonTime;
		buttonArray[19] = buttonBackwards;
		buttonArray[20] = button1;
		buttonArray[21] = button2;
		buttonArray[22] = button3;
		buttonArray[23] = buttonMinus;
		buttonArray[24] = buttonEqual;
		buttonArray[25] = button0;
		buttonArray[26] = buttonDot;
		buttonArray[27] = buttonPlus;
		for (int i = 0; i < buttonArray.length; i++) {
			buttonArray[i].setMargin(new Insets(3, 5, 3, 5));
		}

		stock = "";
		displayTop = "";
		displayBottom = "";
		firstOP = "";
		secondOP = "";
		result = "";
		firstInt = 0;
		secondInt = 0;
		resultInt = 0;
		firstFloat = 0f;
		secondFloat = 0f;
		resultFloat = 0f;
		op = OP_NULL;
		plusFlag = true;
		minusFlag = true;
		timeFlag = true;
		divFlag = true;

		dotFlag = true;
		equalFlag = true;

		errorFlag = false;
	}

	public JFrame createGUI() {
		frame.setJMenuBar(menuBar);
		menuBar.add(checkMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		checkMenu.add(standardItem);
		editMenu.add(copyItem);
		editMenu.add(pasteItem);
		helpMenu.add(aboutItem);

		// setting the layout of panel
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		GridBagConstraints cons = new GridBagConstraints();
		cons.fill = GridBagConstraints.BOTH;
		cons.anchor = GridBagConstraints.CENTER;
		cons.insets = new Insets(2, 2, 2, 2);

		// setting the layout of display panel
		GridBagLayout displayLayout = new GridBagLayout();
		displayPanel.setLayout(displayLayout);
		GridBagConstraints displayCons = new GridBagConstraints();
		displayCons.fill = GridBagConstraints.BOTH;
		displayCons.anchor = GridBagConstraints.CENTER;

		// adding components
		cons.gridx = 0;
		cons.gridy = 0;
		cons.gridheight = 2;
		cons.gridwidth = 5;
		displayPanel.setBackground(Color.blue);
		layout.setConstraints(displayPanel, cons);
		panel.add(displayPanel);
		displayCons.gridy = 0;
		displayCons.gridx = 0;
		displayLayout.setConstraints(displayFieldTop, displayCons);
		displayPanel.add(displayFieldTop);
		displayCons.gridy = 1;
		displayLayout.setConstraints(displayFieldBottom, displayCons);
		displayPanel.add(displayFieldBottom);

		// MC
		cons.gridx = 0;
		cons.gridy = 2;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMC, cons);
		panel.add(buttonMC);

		// MR
		cons.gridx = 1;
		cons.gridy = 2;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMR, cons);
		panel.add(buttonMR);
		// MS
		cons.gridx = 2;
		cons.gridy = 2;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMS, cons);
		panel.add(buttonMS);
		// M+
		cons.gridx = 3;
		cons.gridy = 2;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMPlus, cons);
		panel.add(buttonMPlus);
		// M-
		cons.gridx = 4;
		cons.gridy = 2;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMMinus, cons);
		panel.add(buttonMMinus);
		// Back
		cons.gridx = 0;
		cons.gridy = 3;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonBack, cons);
		panel.add(buttonBack);
		// CE
		cons.gridx = 1;
		cons.gridy = 3;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonCE, cons);
		panel.add(buttonCE);
		// C
		cons.gridx = 2;
		cons.gridy = 3;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonC, cons);
		panel.add(buttonC);
		// 
		cons.gridx = 3;
		cons.gridy = 3;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonNegative, cons);
		panel.add(buttonNegative);
		// 
		cons.gridx = 4;
		cons.gridy = 3;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonRoot, cons);
		panel.add(buttonRoot);
		// 7
		cons.gridx = 0;
		cons.gridy = 4;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button7, cons);
		panel.add(button7);
		// 8
		cons.gridx = 1;
		cons.gridy = 4;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button8, cons);
		panel.add(button8);
		// 9
		cons.gridx = 2;
		cons.gridy = 4;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button9, cons);
		panel.add(button9);
		// 
		cons.gridx = 3;
		cons.gridy = 4;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonDiv, cons);
		panel.add(buttonDiv);
		// %
		cons.gridx = 4;
		cons.gridy = 4;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonPercent, cons);
		panel.add(buttonPercent);
		// 4
		cons.gridx = 0;
		cons.gridy = 5;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button4, cons);
		panel.add(button4);
		// 5
		cons.gridx = 1;
		cons.gridy = 5;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button5, cons);
		panel.add(button5);
		// 6
		cons.gridx = 2;
		cons.gridy = 5;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button6, cons);
		panel.add(button6);
		// X
		cons.gridx = 3;
		cons.gridy = 5;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonTime, cons);
		panel.add(buttonTime);
		// 1/x
		cons.gridx = 4;
		cons.gridy = 5;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonBackwards, cons);
		panel.add(buttonBackwards);
		// 1
		cons.gridx = 0;
		cons.gridy = 6;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button1, cons);
		panel.add(button1);
		// 2
		cons.gridx = 1;
		cons.gridy = 6;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button2, cons);
		panel.add(button2);
		// 3
		cons.gridx = 2;
		cons.gridy = 6;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(button3, cons);
		panel.add(button3);
		// -
		cons.gridx = 3;
		cons.gridy = 6;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonMinus, cons);
		panel.add(buttonMinus);
		// =
		cons.gridx = 4;
		cons.gridy = 6;
		cons.gridheight = 2;
		cons.gridwidth = 1;
		layout.setConstraints(buttonEqual, cons);
		panel.add(buttonEqual);
		// 0
		cons.gridx = 0;
		cons.gridy = 7;
		cons.gridheight = 1;
		cons.gridwidth = 2;
		layout.setConstraints(button0, cons);
		panel.add(button0);
		// .
		cons.gridx = 2;
		cons.gridy = 7;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonDot, cons);
		panel.add(buttonDot);
		// +
		cons.gridx = 3;
		cons.gridy = 7;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		layout.setConstraints(buttonPlus, cons);
		panel.add(buttonPlus);

		con.add(panel);

//		frame.setVisible(true);
		return frame;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (errorFlag == true)
			initial();
		if (e.getSource() == button0) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "0";
			displayFieldBottom.setText(displayBottom);
			if (displayBottom.startsWith("0")
					&& (displayBottom.contains(new String(".")) == false)) {
				displayBottom = "";
				displayFieldBottom.setText("0");
			} else if (displayBottom.startsWith("-0")
					&& (displayBottom.contains(new String(".")) == false)) {
				displayBottom = "-0";
				displayFieldBottom.setText("-0");
			}
		}
		if (e.getSource() == button1) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "1";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button2) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "2";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button3) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "3";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button4) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "4";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button5) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "5";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button6) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "6";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button7) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "7";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button8) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "8";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == button9) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			displayBottom += "9";
			displayFieldBottom.setText(displayBottom);
		}
		if (e.getSource() == buttonDot) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
			}
			if (dotFlag == true) {
				dotFlag = false;
				if (displayBottom == "")
					displayBottom = "0";
				displayBottom += ".";
				displayFieldBottom.setText(displayBottom);
			}
		}
		if (e.getSource() == buttonPlus) {
			if (minusFlag == false || timeFlag == false || divFlag == false) {
				displayBottom = firstOP;
				plusFlag = true;
			}
			if (plusFlag == true) {
				plusFlag = false;
				firstOP = displayBottom;
				if (firstOP == "" || displayBottom == "-") {
					firstOP = "0";
				}
				try {
					firstInt = Integer.parseInt(firstOP.trim());
					firstOP = String.valueOf(firstInt);
				} catch (NumberFormatException ex) {
					firstFloat = Float.parseFloat(firstOP.trim());
					firstOP = String.valueOf(firstFloat);
				}
				displayTop = firstOP;
				displayTop += "+";
				displayFieldTop.setText(displayTop);
				displayBottom = "";
				dotFlag = true;
				op = OP_PLUS;
			}
		}
		if (e.getSource() == buttonMinus) {
			if (plusFlag == false || timeFlag == false || divFlag == false) {
				displayBottom = firstOP;
				minusFlag = true;
			}
			if (minusFlag == true) {
				minusFlag = false;
				firstOP = displayBottom;
				if (firstOP == "" || displayBottom == "-") {
					firstOP = "0";
				}
				try {
					firstInt = Integer.parseInt(firstOP.trim());
					firstOP = String.valueOf(firstInt);
				} catch (NumberFormatException ex) {
					firstFloat = Float.parseFloat(firstOP.trim());
					firstOP = String.valueOf(firstFloat);
				}
				displayTop = firstOP;
				displayTop += "-";
				displayFieldTop.setText(displayTop);
				displayBottom = "";
				dotFlag = true;
				op = OP_MINUS;
			}
		}
		if (e.getSource() == buttonTime) {
			if (minusFlag == false || plusFlag == false || divFlag == false) {
				displayBottom = firstOP;
				timeFlag = true;
			}
			if (timeFlag == true) {
				timeFlag = false;
				firstOP = displayBottom;
				if (firstOP == "" || displayBottom == "-") {
					firstOP = "0";
				}
				try {
					firstInt = Integer.parseInt(firstOP.trim());
					firstOP = String.valueOf(firstInt);
				} catch (NumberFormatException ex) {
					firstFloat = Float.parseFloat(firstOP.trim());
					firstOP = String.valueOf(firstFloat);
				}
				displayTop = firstOP;
				displayTop += "X";
				displayFieldTop.setText(displayTop);
				displayBottom = "";
				dotFlag = true;
				op = OP_TIME;
			}
		}
		if (e.getSource() == buttonDiv) {
			if (minusFlag == false || timeFlag == false || plusFlag == false) {
				displayBottom = firstOP;
				divFlag = true;
			}
			if (divFlag == true) {
				divFlag = false;
				firstOP = displayBottom;
				if (firstOP == "" || displayBottom == "-") {
					firstOP = "0";
				}
				try {
					firstInt = Integer.parseInt(firstOP.trim());
					firstOP = String.valueOf(firstInt);
				} catch (NumberFormatException ex) {
					firstFloat = Float.parseFloat(firstOP.trim());
					firstOP = String.valueOf(firstFloat);
				}
				displayTop = firstOP;
				displayTop += "/";
				displayFieldTop.setText(displayTop);
				displayBottom = "";
				dotFlag = true;
				op = OP_DIV;
			}

		}
		if (e.getSource() == buttonEqual) {
			if (firstOP != "") {
				if (equalFlag == true) {
					equalFlag = false;
					secondOP = displayBottom;
					if (secondOP == "" || displayBottom == "-")
						secondOP = "0";
				}
				operation();
				dotFlag = true;
			}
		}
		if (e.getSource() == buttonRoot) {
			if (displayBottom == "" || displayBottom == "-")
				displayBottom = "0";
			double temp = Double.parseDouble(displayBottom);
			if (temp >= 0) {
				temp = Math.sqrt(temp);
				displayBottom = String.valueOf(temp);
				if (displayBottom.endsWith(".0")) {
					displayBottom = displayBottom.substring(0,
							displayBottom.indexOf('.'));
				}
				displayFieldBottom.setText(displayBottom);
				displayFieldBottom.setCaretPosition(0);
			} else {
				displayFieldBottom.setText("Error!");
				errorFlag = true;
			}
		}
		if (e.getSource() == buttonBackwards) {
			if (displayBottom == "" || displayBottom == "-")
				displayBottom = "0";
			double temp = Double.parseDouble(displayBottom);
			if (temp != 0) {
				try {
					temp = new BigDecimal("1.0").divide(
							new BigDecimal(String.valueOf(temp))).doubleValue();
				} catch (ArithmeticException ex) {
					temp = 1 / temp;
				}
				displayBottom = String.valueOf(temp);
				if (displayBottom.endsWith(".0")) {
					displayBottom = displayBottom.substring(0,
							displayBottom.indexOf('.'));
				}
				displayFieldBottom.setText(displayBottom);
				displayFieldBottom.setCaretPosition(0);
			} else {
				displayFieldBottom.setText("Error!");
				errorFlag = true;
			}
		}
		if (e.getSource() == buttonPercent) {
			if (plusFlag && minusFlag && timeFlag && divFlag == true) {
				initial();
			} else {
				if (displayBottom == "" || displayBottom == "-") {
					displayBottom = "0";
				}
				double temp = Double.parseDouble(displayBottom);
				firstFloat = Float.parseFloat(firstOP.trim());
				try {
					temp = new BigDecimal(String.valueOf(firstFloat)).multiply(
							new BigDecimal(String.valueOf(temp))
									.divide(new BigDecimal("100.0")))
							.doubleValue();
				} catch (ArithmeticException ex) {
					temp = firstFloat * (temp / 100);
				}
				displayBottom = String.valueOf(temp);
				if (displayBottom.endsWith(".0")) {
					displayBottom = displayBottom.substring(0,
							displayBottom.indexOf('.'));
				}
				displayFieldBottom.setText(displayBottom);
				displayFieldBottom.setCaretPosition(0);
			}
		}
		if (e.getSource() == buttonNegative) {
			if (equalFlag == false) {
				displayBottom = "";
				displayFieldBottom.setText(displayBottom);
				equalFlag = true;
				plusFlag = true;
				minusFlag = true;
				timeFlag = true;
				divFlag = true;
			}
			if (displayBottom == "" || displayBottom == "-") {
				displayBottom = "-";
				displayFieldBottom.setText(displayBottom);
				displayFieldBottom.setCaretPosition(0);
			} else {
				double temp = Double.parseDouble(displayBottom);
				temp = -temp;
				displayBottom = String.valueOf(temp);
				if (displayBottom.endsWith(".0")) {
					displayBottom = displayBottom.substring(0,
							displayBottom.indexOf('.'));
				}
				displayFieldBottom.setText(displayBottom);
				displayFieldBottom.setCaretPosition(0);
			}
			if (displayBottom.equals("0"))
				dotFlag = true;
		}
		if (e.getSource() == buttonBack) {
			String temp = displayBottom;
			if (temp.length() <= 1) {
				temp = "0";
				displayBottom = "";
				displayFieldBottom.setText(temp);
			} else {
				temp = temp.substring(0, temp.length() - 1);
				displayBottom = temp;
				displayFieldBottom.setText(displayBottom);
			}
		}
		if (e.getSource() == buttonC) {
			initial();
		}
		if (e.getSource() == buttonCE) {
			displayBottom = "";
			displayFieldBottom.setText("0");
		}
		if (e.getSource() == copyItem) {
			stock = displayBottom;
		}
		if (e.getSource() == pasteItem) {
			displayBottom = stock;
			if (displayBottom == "") {
				displayFieldBottom.setText("0");
			} else {
				displayFieldBottom.setText(displayBottom);
			}
		}
		if (e.getSource() == aboutItem) {
			JOptionPane.showMessageDialog(null,
					"Author: Jiuyang Zhou\r\nMay 29, 2013 @ Florida Tech");
		}
	}

	public void operation() {
		try {
			secondInt = Integer.parseInt(secondOP.trim());
			secondOP = String.valueOf(secondInt);
			secondFloat = secondInt;
		} catch (NumberFormatException ex) {
			secondFloat = Float.parseFloat(secondOP.trim());
			secondOP = String.valueOf(secondFloat);
		}
		firstFloat = Float.parseFloat(firstOP.trim());
		b1 = new BigDecimal(String.valueOf(firstFloat));
		b2 = new BigDecimal(String.valueOf(secondFloat));
		switch (op) {
		case OP_PLUS:
			resultFloat = b1.add(b2).floatValue();
			result = String.valueOf(resultFloat);
			if (result.endsWith(".0")) {
				result = result.substring(0, result.indexOf('.'));
			}
			displayBottom = result;
			displayFieldBottom.setText(displayBottom);
			displayTop = firstOP + "+" + secondOP;
			displayFieldTop.setText(displayTop);
			firstOP = displayBottom;
			try {
				firstInt = Integer.parseInt(firstOP.trim());
				firstOP = String.valueOf(firstInt);
			} catch (NumberFormatException ex) {
				firstFloat = Float.parseFloat(firstOP.trim());
				firstOP = String.valueOf(firstFloat);
			}
			plusFlag = true;
			System.out.println(firstFloat);
			System.out.println(secondFloat);
			System.out.println(resultFloat);
			// System.out.println(displayBottom);
			break;
		case OP_MINUS:
			resultFloat = b1.subtract(b2).floatValue();
			result = String.valueOf(resultFloat);
			if (result.endsWith(".0")) {
				result = result.substring(0, result.indexOf('.'));
			}
			displayBottom = result;
			displayFieldBottom.setText(displayBottom);
			displayTop = firstOP + "-" + secondOP;
			displayFieldTop.setText(displayTop);
			firstOP = displayBottom;
			try {
				firstInt = Integer.parseInt(firstOP.trim());
				firstOP = String.valueOf(firstInt);
			} catch (NumberFormatException ex) {
				firstFloat = Float.parseFloat(firstOP.trim());
				firstOP = String.valueOf(firstFloat);
			}
			minusFlag = true;
			System.out.println(firstFloat);
			System.out.println(secondFloat);
			System.out.println(resultFloat);
			break;
		case OP_TIME:
			resultFloat = b1.multiply(b2).floatValue();
			result = String.valueOf(resultFloat);
			if (result.endsWith(".0")) {
				result = result.substring(0, result.indexOf('.'));
			}
			displayBottom = result;
			displayFieldBottom.setText(displayBottom);
			displayTop = firstOP + "X" + secondOP;
			displayFieldTop.setText(displayTop);
			firstOP = displayBottom;
			try {
				firstInt = Integer.parseInt(firstOP.trim());
				firstOP = String.valueOf(firstInt);
			} catch (NumberFormatException ex) {
				firstFloat = Float.parseFloat(firstOP.trim());
				firstOP = String.valueOf(firstFloat);
			}
			timeFlag = true;
			System.out.println(firstFloat);
			System.out.println(secondFloat);
			System.out.println(resultFloat);
			break;
		case OP_DIV:
			try {
				resultFloat = b1.divide(b2).floatValue();
			} catch (ArithmeticException ex) {
				resultFloat = b1.floatValue() / b2.floatValue();
			}
			result = String.valueOf(resultFloat);
			if (result.endsWith(".0")) {
				result = result.substring(0, result.indexOf('.'));
			}
			displayBottom = result;
			displayFieldBottom.setText(displayBottom);
			displayTop = firstOP + "/" + secondOP;
			displayFieldTop.setText(displayTop);
			if (displayBottom.equalsIgnoreCase("Infinity")) {
				errorFlag = true;
			} else
				firstOP = displayBottom;
			try {
				firstInt = Integer.parseInt(firstOP.trim());
				firstOP = String.valueOf(firstInt);
			} catch (NumberFormatException ex) {
				firstFloat = Float.parseFloat(firstOP.trim());
				firstOP = String.valueOf(firstFloat);
			}
			divFlag = true;
			System.out.println(firstFloat);
			System.out.println(secondFloat);
			System.out.println(resultFloat);
			break;
		case OP_NULL:
			break;
		}
	}

	public void initial() {
		displayFieldTop.setText("");
		displayFieldBottom.setText("0");
		displayBottom = "";
		displayTop = "";
		firstOP = "";
		secondOP = "";
		errorFlag = false;
	}

	public static void main(String args[]) {
		new Calculator().createGUI();
	}
}
