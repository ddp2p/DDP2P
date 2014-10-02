
//package components;

/*
 * TextFieldDemo.java requires one additional file:
 * content.txt
 */
package dd_p2p.plugin;
import java.awt.Button;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.*;
import javax.swing.text.*;
//import javax.swing.event.*;
import javax.swing.GroupLayout.*;

public class InviteFriendDialog extends JDialog
                           implements ActionListener {
    
    private JTextField entry;
    private JLabel jLabel1;
    private JScrollPane jScrollPane1;
    private JLabel status;
    private JTextArea textArea;
    protected JButton CmdOk,CmdCancel;
    ChatPeer chatPeer;
    
    
    
    
    public InviteFriendDialog(ChatPeer Parent)
	{		
		super(Parent,"Invite a Friend",true);
		chatPeer = Parent;			
        initComponents();
        
        InputStream in = getClass().getResourceAsStream("../content.txt");
        try {
            textArea.read(new InputStreamReader(in), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        textArea.append("\nYour Friend; \n" +chatPeer.UserName);
        setVisible(true);
      
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */

    private void initComponents() {
        entry = new JTextField();
        textArea = new JTextArea();
        status = new JLabel();
        jLabel1 = new JLabel();

        //setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent e) {setVisible(false);}});
       // setTitle("TextFieldDemo");

        textArea.setColumns(20);
        textArea.setLineWrap(true);
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        jScrollPane1 = new JScrollPane(textArea);

        jLabel1.setText("Enter Your Friend Email: ");
        
        CmdOk = new JButton("Send");
		CmdOk.addActionListener(this);
		CmdCancel = new JButton("Cancel");
		CmdCancel.addActionListener(this);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
	//Create a parallel group for the horizontal axis
	ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
	
	//Create a sequential and a parallel groups
	SequentialGroup h1 = layout.createSequentialGroup();
	ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
	
	//Add a container gap to the sequential group h1
	h1.addContainerGap();
	
	//Add a scroll pane and a label to the parallel group h2
	h2.addComponent(jScrollPane1, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
	h2.addComponent(CmdOk, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
	
	//Create a sequential group h3
	SequentialGroup h3 = layout.createSequentialGroup();
	h3.addComponent(jLabel1);
	h3.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
	h3.addComponent(entry, GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE);
	 
	//Add the group h3 to the group h2
	h2.addGroup(h3);
	//Add the group h2 to the group h1
	h1.addGroup(h2);

	h1.addContainerGap();
	
	//Add the group h1 to the hGroup
	hGroup.addGroup(GroupLayout.Alignment.TRAILING, h1);
	//Create the horizontal group
	layout.setHorizontalGroup(hGroup);
	
        
	//Create a parallel group for the vertical axis
	ParallelGroup vGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
	//Create a sequential group v1
	SequentialGroup v1 = layout.createSequentialGroup();
	//Add a container gap to the sequential group v1
	v1.addContainerGap();
	//Create a parallel group v2
	ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
	v2.addComponent(jLabel1);
	v2.addComponent(entry, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
	//Add the group v2 tp the group v1
	v1.addGroup(v2);
	v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
	v1.addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE);
	v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
	v1.addComponent(CmdOk);
	v1.addContainerGap();
	
	//Add the group v1 to the group vGroup
	vGroup.addGroup(v1);
	//Create the vertical group
	layout.setVerticalGroup(vGroup);
	pack();
	
    }

    public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource().equals(CmdOk))
		{
			
			dispose();
		}	
		
		if (evt.getSource().equals(CmdCancel))
		{
			
			dispose();
		}	
	}
       
   /* public static void main(String args[]) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
	SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
		new InviteFriendDialog().setVisible(true);
            }
        });
    }*/
    
   
}
