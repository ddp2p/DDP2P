package widgets.components;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import widgets.components.DDP2PColoredItem.DDP2PColorPair;
import data.D_Document_Title;

/**
 * Sets Strings to Green on Gray
 * @author M Silaghi
 *
 */
public class ColoredDocumentTitleRenderer 
extends DefaultTableCellRenderer
//implements TableCellRenderer 
{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	private DDP2PColoredItem model;
	
	public ColoredDocumentTitleRenderer(DDP2PColoredItem _model) {
		model = _model;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		//JLabel result = new JLabel();
		JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//		Color foreground = model.getForeground(table, value, isSelected, hasFocus, row, column);
//		Color background = model.getBackground(table, value, isSelected, hasFocus, row, column);
		DDP2PColorPair colors = model.getColors(table, value, isSelected, hasFocus, row, column, result);
		if (value instanceof String) {
			if (DEBUG) System.out.println("DocumentTitleRenderer: getTableCellRendererComponent: string:"+value+" c="+colors);
			String val = (String) value;
			result.setText(val);
			//result = new JLabel(val);
			if (! isSelected) {
				result.setBackground(colors.getBackground());//(Color.GRAY);
				result.setForeground(colors.getForeground());//(Color.GREEN);
			}
			result.setIcon(colors.getIcon());
			return result;
		}
		
		if (value instanceof D_Document_Title){
			if (DEBUG) System.out.println("DocumentTitleRenderer: getTableCellRendererComponent: ddt="+value+" c="+colors);
			D_Document_Title dt = (D_Document_Title) value;
			if (dt.title_document == null) return null;
			String val = dt.title_document.getDocumentString();
			String fmt = dt.title_document.getFormatString();
//			if (fmt == null) {
//				result = new JLabel(val);
//				result.setBackground(colors.getBackground());
//				result.setForeground(colors.getForeground());
//				return result;
//			}
			//result = new JLabel(val);
			result.setText(val);
			if (! isSelected) {
				result.setBackground(colors.getBackground());
				result.setForeground(colors.getForeground());
			}
			result.setIcon(colors.getIcon());
			return result;
		}
		return null;
	}

}