package widgets.components;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;

public interface DDP2PColoredItem {
	public static class DDP2PColorPair {
		private Color foreground;
		private Color background;
		private Icon icon;
		public DDP2PColorPair(Color foreground, Color background, Icon icon) {
			this.setForeground(foreground);
			this.setBackground(background);
			this.setIcon(icon);
		}
		public Color getForeground() {
			return foreground;
		}
		public void setForeground(Color foreground) {
			this.foreground = foreground;
		}
		public Color getBackground() {
			return background;
		}
		public void setBackground(Color background) {
			this.background = background;
		}
		public String toString() {
			return "COLOR["+foreground+"/"+background+"]";
		}
		public Icon getIcon() {
			return icon;
		}
		public void setIcon(Icon icon) {
			this.icon = icon;
		}
	}
//	public Color getForeground(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);
//	public Color getBackground(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);
	public DDP2PColorPair getColors(JTable table, Object value, boolean isSelected, boolean hasFocus, int row_view, int column_view, Component component);
}