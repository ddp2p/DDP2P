package net.ddp2p.widgets.updates;

import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;

public class PanelRenderer extends JPanel
                           implements TableCellRenderer {
    
    static final boolean DEBUG = false;

	public PanelRenderer () {
        setOpaque(true); //MUST do this for background to show up.
//        setEditable(true);
//        setSize(2,2);
//        setEnabled(true);
    }

    public Component getTableCellRendererComponent(
                            JTable table, Object items,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
           
         if(items==null)
        	 	if(DEBUG)System.out.println("updates:PanelRenderer:null items in getTableCellRendererComponent");                  
//        ArrayList<String> a = (ArrayList<String>) items;
//        String[] arr = new String[a.size()];
//        for(int i=0; i< arr.length; i++)
//        	arr[i]=(String) a.get(i);
    //    table.getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).setCellEditor(new MyComboBoxEditor(arr));
//        JComboBox cc= (JComboBox) 
        //	table.getColumnModel().getColumn(UpdatesModel.TABLE_COL_QOT_ROT).getCellEditor().;
 //       	cc.addItem(arr[0]);                     	
        
//        for(int i=0; i< a.size(); i++)
//           this.addItem(a.get(i).toString());
//           
//           if (isSelected) {
//      setForeground(table.getSelectionForeground());
//      super.setBackground(table.getSelectionBackground());
//    } else {
//      setForeground(table.getForeground());
//      setBackground(table.getBackground());
//    }
   // setSelectedItem(a.get(0));
    
        return (Component) items;
    }
}
