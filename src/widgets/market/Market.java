package widgets.market;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class Market implements ComponentListener, ActionListener {
	static DB_Operator db;
	public JFrame frame;
	Container con;
	public JLabel mainLabel;
	GridBagLayout layout;
	GridBagConstraints cons;
	int rowNumberT;
	JLabel l1;
	JLabel l2;
	JFrame uploadF;
	ImageIcon upload;
	ImageIcon refresh;
	JButton[] buttonDes;
	JButton[] buttonDow;
	JButton button1;// upload
	JButton button2;// refresh
	JButton button3;// chose file
	JButton button4;// confirm upload
	JButton button5;// reset upload info
	JTextArea fileDir;// upload file dir
	JScrollPane bottomScroll;
	String fileName;
	JTextField textName;
	JComboBox<String> uploadCombo;
	JTextField textBrand;
	JTextField textPrice;
	JTextArea textDes;
	JTable table;
	static String[] dir;

	public static void main(String[] args) {
		new Market().drawMarket();
	}

	public JFrame drawMarket() {
		db = new DB_Operator();
		try {
			db.database("src/deliberation-app.db");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		frame = new JFrame("Market");
		con = frame.getContentPane();
		con.setBackground(Color.white);
		mainLabel = new JLabel();
		layout = new GridBagLayout();
		cons = new GridBagConstraints();
		mainLabel.setLayout(layout);

		cons.weightx = 1;
		cons.weighty = 1;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		cons.gridx = 0;
		cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		cons.anchor = GridBagConstraints.CENTER;

		l1 = drawTopLabel();
		// l1.addComponentListener(this);
		l2 = drawCentralLabel();
		JTable bottomTable = drawBottomLabel();
		bottomScroll = new JScrollPane();
		bottomScroll.setViewportView(bottomTable);

		layout.setConstraints(l1, cons);
		mainLabel.add(l1);
		cons.gridheight = 1;
		cons.weighty = 1;
		cons.gridy = 1;
		cons.gridx = 0;
		layout.setConstraints(l2, cons);
		mainLabel.add(l2);

		cons.weighty = 1;
		// cons.gridheight = 2;
		cons.gridy = 2;
		cons.gridx = 0;
		layout.setConstraints(bottomScroll, cons);
		mainLabel.add(bottomScroll);

		con.add(mainLabel, "Center");
		frame.setSize(500, 500);
		// frame.pack();
		frame.setVisible(false);
		return frame;
	}

	public JLabel drawTopLabel() {
		JLabel topLabel = new JLabel(new ImageIcon(
				"src/widgets/market/market.jpg"));
		topLabel.setBackground(Color.white);
		return topLabel;
	}

	public JLabel drawCentralLabel() {
		JLabel centralLabel = new JLabel();
		JPanel left = new JPanel();
		JPanel right = new JPanel();
		centralLabel.setLayout(new GridLayout(1, 2));
		upload = new ImageIcon("src/widgets/market/upload.png",
				"a background of button");
		refresh = new ImageIcon("src/widgets/market/refresh.png",
				"a background of button");
		button1 = new JButton("Upload", upload);
		button1.addActionListener(this);
		button2 = new JButton("Refresh", refresh);
		button2.addActionListener(this);
		left.setBackground(Color.white);
		right.setBackground(Color.white);
		left.add(button1);
		right.add(button2);
		centralLabel.add(left);
		centralLabel.add(right);
		return centralLabel;
	}

	public JTable drawBottomLabel() {
		try {
			String category = null;
			String product = null;
			String brand = null;
			String price = null;
			String[] des;
			String date = null;
			Object tableData[][];
			Object tableName[] = { "Category", "Product Name", "Brand",
					"Price", "Description", "Publish Time", "Buy It" };
			rowNumberT = 0;
			rowNumberT = db.findRowNumber();
			buttonDes = new JButton[rowNumberT];
			buttonDow = new JButton[rowNumberT];
			dir = new String[rowNumberT];
			des = new String[rowNumberT];
			int columnNumber = tableName.length;
			tableData = new Object[rowNumberT][columnNumber];
			ResultSet result = db.select_DB("select * from market");
			for (int i = 0; i < rowNumberT; i++) {
				result.next();
				category = result.getString("category");
				product = result.getString("name");
				brand = result.getString("brand");
				price = result.getString("price");
				des[i] = result.getString("description");
				dir[i] = result.getString("dir");
				buttonDes[i] = new JButton("des");
				buttonDow[i] = new JButton("dow");
				date = result.getString("date");
				tableData[i][0] = category;
				tableData[i][1] = product;
				tableData[i][2] = brand;
				tableData[i][3] = price;
				tableData[i][4] = des[i];
				tableData[i][5] = date;
				tableData[i][6] = buttonDow[i];
			}
			table = new JTable(tableData, tableName);
			table.setCellSelectionEnabled(true);
			table.getColumn("Buy It")
					.setCellRenderer(new MyTableCellRenderer());
		} catch (IOException e) {
			System.out.println(e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return table;
	}

	public void redraw(JLabel l) {

		System.out.println(l.getWidth());
		ImageIcon i = new ImageIcon(new ImageIcon(
				"src/widgets/market/market.jpg").getImage().getScaledInstance(
				l.getWidth(), l.getHeight(), Image.SCALE_DEFAULT));
		l.setIcon(i);
	}

	public void upload() {
		uploadF = new JFrame("Upload Product-Zhou Jiuyang");
		// setting the frame size
		int inset_width = 400;
		int inset_hight = 120;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		uploadF.setBounds(inset_width, inset_hight, screenSize.width
				- inset_width * 2, screenSize.height - inset_hight * 2);
		// setting the frame icon
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.getImage("src/widgets/market/upload.png");
		uploadF.setIconImage(img);
		// setting frame resizable and disable the maximization button
		uploadF.setResizable(false);

		Container uploadC = uploadF.getContentPane();
		GridBagLayout uploadL = new GridBagLayout();
		GridBagConstraints uploadLC = new GridBagConstraints();
		uploadC.setLayout(uploadL);
		uploadF.setVisible(true);

		uploadLC.weightx = 1;
		uploadLC.weighty = 1;
		uploadLC.gridheight = 1;
		uploadLC.gridwidth = 1;
		uploadLC.gridx = 0;
		uploadLC.gridy = 0;
		uploadLC.fill = GridBagConstraints.BOTH;
		uploadLC.anchor = GridBagConstraints.CENTER;
		uploadLC.insets = new Insets(5, 5, 5, 5);

		uploadLC.gridwidth = 2;
		JLabel uploadTop = new JLabel(new ImageIcon(
				"src/widgets/market/upload.jpg"));
		uploadL.setConstraints(uploadTop, uploadLC);
		uploadF.add(uploadTop);

		uploadLC.gridy = 1;
		uploadLC.gridwidth = 1;
		JLabel uploadName = new JLabel("Product Name:");
		JPanel p1 = new JPanel();
		p1.setLayout(new FlowLayout(FlowLayout.LEFT));
		p1.add(uploadName);
		uploadL.setConstraints(p1, uploadLC);
		uploadF.add(p1);

		uploadLC.gridx = 1;
		textName = new JTextField(15);
		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.LEFT));
		p2.add(textName);
		uploadL.setConstraints(p2, uploadLC);
		uploadF.add(p2);

		uploadLC.gridy = 2;
		uploadLC.gridx = 0;
		JLabel uploadCate = new JLabel("Product Category:");
		JPanel p3 = new JPanel();
		p3.setLayout(new FlowLayout(FlowLayout.LEFT));
		p3.add(uploadCate);
		uploadL.setConstraints(p3, uploadLC);
		uploadF.add(p3);

		uploadLC.gridx = 1;
		String[] category = { "Book", "Paper", "Game", "Software", "Music",
				"Movie", "Other" };
		JPanel p4 = new JPanel();
		p4.setLayout(new FlowLayout(FlowLayout.LEFT));
		uploadCombo = new JComboBox<String>(category);
		p4.add(uploadCombo, "South");
		uploadL.setConstraints(p4, uploadLC);
		uploadF.add(p4);

		uploadLC.gridy = 3;
		uploadLC.gridx = 0;
		JLabel uploadBrand = new JLabel("Brand Name:");
		JPanel p5 = new JPanel();
		p5.setLayout(new FlowLayout(FlowLayout.LEFT));
		p5.add(uploadBrand);
		uploadL.setConstraints(p5, uploadLC);
		uploadF.add(p5);

		uploadLC.gridx = 1;
		textBrand = new JTextField(15);
		JPanel p6 = new JPanel();
		p6.add(textBrand);
		p6.setLayout(new FlowLayout(FlowLayout.LEFT));
		uploadL.setConstraints(p6, uploadLC);
		uploadF.add(p6);

		uploadLC.gridy = 4;
		uploadLC.gridx = 0;
		JLabel uploadPrice = new JLabel("Product Price:");
		JPanel p7 = new JPanel();
		p7.setLayout(new FlowLayout(FlowLayout.LEFT));
		p7.add(uploadPrice);
		uploadL.setConstraints(p7, uploadLC);
		uploadF.add(p7);

		uploadLC.gridx = 1;
		textPrice = new JTextField("$0.0", 5);
		textPrice.setEditable(false);
		textPrice.setHorizontalAlignment(JTextField.RIGHT);
		JPanel p8 = new JPanel();
		p8.setLayout(new FlowLayout(FlowLayout.LEFT));
		p8.add(textPrice);
		uploadL.setConstraints(p8, uploadLC);
		uploadF.add(p8);

		uploadLC.gridy = 5;
		uploadLC.gridx = 0;
		JLabel uploadDes = new JLabel("Product Description:");
		JPanel p9 = new JPanel();
		p9.setLayout(new FlowLayout(FlowLayout.LEFT));
		p9.add(uploadDes);
		uploadL.setConstraints(p9, uploadLC);
		uploadF.add(p9);

		uploadLC.gridx = 1;
		textDes = new JTextArea(3, 10);
		JScrollPane p10 = new JScrollPane(textDes,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		uploadL.setConstraints(p10, uploadLC);
		uploadF.add(p10);

		uploadLC.gridy = 6;
		uploadLC.gridx = 0;
		JLabel uploadDir = new JLabel("Product Dir:");
		JPanel p11 = new JPanel();
		p11.setLayout(new FlowLayout(FlowLayout.LEFT));
		p11.add(uploadDir);
		uploadL.setConstraints(p11, uploadLC);
		uploadF.add(p11);

		uploadLC.gridx = 1;
		button3 = new JButton("Chose File");
		button3.addActionListener(this);
		fileDir = new JTextArea(2, 15);
		fileDir.setEditable(false);
		JScrollPane fileDirSc = new JScrollPane(fileDir,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel p12 = new JPanel();
		p12.add(button3);
		p12.add(fileDirSc);
		p12.setLayout(new FlowLayout(FlowLayout.LEFT));
		uploadL.setConstraints(p12, uploadLC);
		uploadF.add(p12);

		uploadLC.gridy = 7;
		uploadLC.gridx = 0;
		button4 = new JButton("Confirm", upload);
		button4.addActionListener(this);
		JPanel p13 = new JPanel();
		p13.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p13.add(button4);
		uploadL.setConstraints(p13, uploadLC);
		uploadF.add(p13);

		uploadLC.gridx = 1;
		button5 = new JButton("Reset", refresh);
		textPrice.setHorizontalAlignment(JTextField.RIGHT);
		JPanel p14 = new JPanel();
		p14.setLayout(new FlowLayout(FlowLayout.CENTER));
		p14.add(button5);
		button5.addActionListener(this);
		uploadL.setConstraints(p14, uploadLC);
		uploadF.add(p14);
	}

	public void doUpload() {
		try {
			int ID = 0;
			String name = null;
			String category = null;
			String brand = null;
			String price = null;
			String des = null;
			String dir = null;
			String date = null;
			int downloadNumber = 0;
			name = textName.getText();
			category = uploadCombo.getSelectedItem().toString();
			brand = textBrand.getText();
			price = textPrice.getText();
			des = textDes.getText();
			dir = fileDir.getText();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			date = df.format(new Date()).toString();

			ID = db.findRowNumber() + 1;
			String command = new String(
					"insert into market (marketID,name,category,brand,price,description,dir,downloadnumber,date) values ('"
							+ String.valueOf(ID)
							+ "','"
							+ name
							+ "','"
							+ category
							+ "','"
							+ brand
							+ "','"
							+ price
							+ "','"
							+ des
							+ "','"
							+ dir
							+ "',"
							+ downloadNumber
							+ ",'"
							+ date + "')");
			// System.out.println(command);
			db.operate_DB(command);
			// db.clearMarketTabel();
			if (dir != null) {
				try {
					int i;
					FileInputStream fin = new FileInputStream(dir);
					File file = new File("src/Market_Server/" + fileName);
					if (file.exists() == false) {
						file.createNewFile();
					}
					FileOutputStream fout = new FileOutputStream(file);
					FileChannel cin = fin.getChannel();
					ByteBuffer buffer = ByteBuffer.allocate(1024000);
					buffer.clear();
					while (true) {
						i = cin.read(buffer);
						// System.out.println(i);
						if (i == -1)
							break;
					}
					byte[] b = buffer.array();
					buffer.clear();
					buffer.put(b);
					FileChannel cout = fout.getChannel();
					buffer.flip();
					cout.write(buffer);
					JOptionPane.showMessageDialog(null, "Upload successfully!");
					uploadF.setVisible(false);
					// db.clearMarketTabel();
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		} catch (IOException e) {
		} catch (SQLException e) {
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		redraw(l1);
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == button1) {
			upload();
		}
		if (e.getSource() == button2) {
			mainLabel.remove(bottomScroll);
			JTable bottomTable = drawBottomLabel();
			bottomScroll = new JScrollPane();
			bottomScroll.setViewportView(bottomTable);
			cons.weighty = 1;
			cons.gridy = 2;
			cons.gridx = 0;
			layout.setConstraints(bottomScroll, cons);
			mainLabel.add(bottomScroll);
		}
		if (e.getSource() == button3) {
			String fileDirStr;
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				fileDirStr = chooser.getSelectedFile().getAbsolutePath();
				fileName = chooser.getSelectedFile().getName();
				fileDir.setText(fileDirStr);
			}
		}
		if (e.getSource() == button4) {
			doUpload();
		}
		if (e.getSource() == button5) {
			textName.setText("");
			uploadCombo.setSelectedIndex(0);
			textBrand.setText("");
			textDes.setText("");
			fileDir.setText("");
		}
	}
}

class MyTableCellRenderer extends JButton implements TableCellRenderer,
		ActionListener {
	public int count = 0;
	JFrame downloadF;
	JButton b1;
	JButton b2;
	int rowN;

	public MyTableCellRenderer() {
		super();

	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		this.setText("Buy");
		//
		if (count == 0) {
			if (isSelected) {
				rowN = row;
				downloadF = new JFrame("Download");
				int inset_width = 450;
				int inset_hight = 200;
				Dimension screenSize = Toolkit.getDefaultToolkit()
						.getScreenSize();
				downloadF.setBounds(inset_width, inset_hight, screenSize.width
						- inset_width * 2, screenSize.height - inset_hight * 2);
				// setting frame resizable and disable the maximization button
				downloadF.setResizable(false);
				Container downC = downloadF.getContentPane();
				downC.setBackground(Color.white);
				// System.out.println(row);
				JLabel top = new JLabel(new ImageIcon(
						"src/widgets/market/download.jpg"));
				JLabel center = new JLabel(new ImageIcon(
						"src/widgets/market/center.jpg"));
				JPanel bottom = new JPanel();
				b1 = new JButton();
				b2 = new JButton();
				b1 = new JButton("Yes", new ImageIcon(
						"src/widgets/market/upload.png"));
				b1.addActionListener(this);
				b2 = new JButton("No", new ImageIcon(
						"src/widgets/market/refresh.png"));
				b2.addActionListener(this);
				bottom.setBackground(Color.white);
				bottom.add(b1);
				bottom.add(b2);

				downC.add(top, "North");
				downC.add(center, "Center");
				downC.add(bottom, "South");

				downloadF.setVisible(true);
				count++;
			} else {

			}

			if (hasFocus) {
				super.setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			} else {
				super.setForeground(table.getForeground());
				super.setBackground(table.getBackground());
			}
		}
		return this;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == b1) {
			int number;
			File sFile;
			File dFile;
			String desDir = null;
			String d = Market.dir[rowN];
			number = d.lastIndexOf('\\');
			d = d.substring(number + 1);
			d = "src/Market_Server/" + d;
			// System.out.println(d);
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("g:/"));
			chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			int returnVal = chooser.showDialog(null, "save");
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				desDir = chooser.getSelectedFile().getAbsolutePath();

				// fileName = chooser.getSelectedFile().getName();
				// fileDir.setText(fileDirStr);
			}
			desDir = desDir.replace('\\', '/');
			// System.out.println(desDir);
			dFile = new File(desDir);
			sFile = new File(d);
			try {
				int i;
				FileInputStream fin = new FileInputStream(sFile);
				if (dFile.exists() == false) {
					dFile.createNewFile();
				}
				FileOutputStream fout = new FileOutputStream(dFile);
				FileChannel cin = fin.getChannel();
				ByteBuffer bu = ByteBuffer.allocate(1024000);
				bu.clear();
				while (true) {
					i = cin.read(bu);
					// System.out.println(i);
					if (i == -1 || i == 0)
						break;
				}
				byte[] b = bu.array();
				bu.clear();
				bu.put(b);
				FileChannel cout = fout.getChannel();
				bu.flip();
				cout.write(bu);

				JOptionPane.showMessageDialog(null,
						"Download successfully!\nThe product is saved at:"
								+ desDir);
				downloadF.setVisible(false);
			} catch (Exception ee) {
				System.out.println(ee);
			}
		}
		if (e.getSource() == b2) {
			downloadF.setVisible(false);
		}
	}
}
