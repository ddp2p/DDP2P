package net.ddp2p.widgets.dir_management;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
public class ComboBoxRenderer extends JComboBox
                           implements TableCellRenderer {
   JComboBox b;
    public ComboBoxRenderer () {
        setOpaque(true); 
    }
    public Component getTableCellRendererComponent(
                            JTable table, Object items,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
         if(items==null) return null;                  
        return (JComboBox) items;
    }
}
