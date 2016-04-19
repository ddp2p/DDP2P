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
        setOpaque(true); 
    }
    public Component getTableCellRendererComponent(
                            JTable table, Object items,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
         if(items==null)
        	 	if(DEBUG)System.out.println("updates:PanelRenderer:null items in getTableCellRendererComponent");                  
        return (Component) items;
    }
}
