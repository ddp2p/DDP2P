/**
 * @(#)tableJButton.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */
package widgets.updates; 
 
import javax.swing.JButton;

public class TableJButton extends JButton {
    public int rowNo=-1;
    public TableJButton() {
    	super();
    }
    
     public TableJButton(String label) {
     	super(label);
    }
    
    public TableJButton(String label, int row) {
     	super(label);
     	rowNo=row;
    }
    public void setRow(int row){
    	rowNo = row;
    }
}