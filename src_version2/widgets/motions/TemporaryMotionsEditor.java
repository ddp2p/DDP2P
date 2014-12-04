package widgets.motions;

import config.MotionsListener;
import widgets.app.MainFrame;
import data.D_Motion;

/**
 * This is a place-holder Editor to be used at startup (when first loading the motions tab).
 * It will be replaced with the real editor when the selection of a motion is first done.
 * @author msilaghi
 *
 */
final class TemporaryMotionsEditor implements MotionsListener {
	private static boolean DEBUG = false;
	boolean _DEBUG = true;
	Motions motions = null;
	public TemporaryMotionsEditor(Motions motions) {
		if (Motions.DEBUG) DEBUG = true;
		this.motions = motions;
		if (DEBUG) System.out.println("TemporaryMotionsEditor: created");
	}

	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if (Motions.DEBUG) DEBUG = true;
		if (DEBUG) System.out.println("TemporaryMotionsEditor: update: start: " + d_motion);
	
		if (motID == null) {
			if (DEBUG) System.out.println("TemporaryMotionsEditor: update: null motID: " + d_motion);
			return;
		}
		synchronized (motions.motion_panel) {
			if (motions._medit != null) {
				if (DEBUG) System.out.println("TemporaryMotionsEditor: update: already set");
				return;
			}
			if (DEBUG) System.out.println("TemporaryMotionsEditor: update: set");
			MotionEditor me = new MotionEditor();
			motions._medit = me;
			//Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
			//Component panel = new JScrollPane(motions._medit = me);
			//panel.setMinimumSize(_minimumSize);
			//motions.motion_panel.setRightComponent(panel);
			
			// needed since otherwise the tab gets lost (component's parent being reassigned)
			MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_MOTS_, MainFrame.JunkPanelMots);
			motions.motion_panel = MainFrame.makeMotionPanel(me, motions.motion_panel);
			
			if (MainFrame.tabbedPane.getSelectedIndex() == MainFrame.TAB_MOTS_) {
				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_MOTS_, motions.motion_panel);
				if (DEBUG) System.out.println("TemporaryMotionsEditor: update: selected motion tab");
			} else {
				if (DEBUG) System.out.println("TemporaryMotionsEditor: update: not selected motion tab");
			}
			motions.removeListener(motions.tmpeditor);
			motions.addListener(me);
	    	MainFrame.status.addConstituentMeStatusListener(me);
			motions.showSelected();
		}
	}

	@Override
	public void motion_forceEdit(String motID) {
		if (Motions.DEBUG) DEBUG = true;
		if (Motions.DEBUG) System.out.println("TemporaryMotionsEditor: forceEdit: start ");
		
		if (motID == null) {
			if (Motions.DEBUG) System.out.println("TemporaryMotionsEditor: force: null motID ");
			return;
		}
		synchronized (motions.motion_panel) {
			if (motions._medit != null) {
				if (Motions.DEBUG) System.out.println("TemporaryMotionsEditor: force: already set");
				return;
			}
			if (Motions.DEBUG) System.out.println("TemporaryMotionsEditor: force: set");
			MotionEditor me = new MotionEditor();
			motions._medit = me;
			
//			Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
//			Component panel = new JScrollPane(motions._medit = me);
//			panel.setMinimumSize(_minimumSize);
//			motions.motion_panel.setRightComponent(panel);
			
			motions.motion_panel = MainFrame.makeMotionPanel(me, motions.motion_panel);
			if (MainFrame.tabbedPane.getSelectedIndex() == MainFrame.TAB_MOTS_)
				MainFrame.tabbedPane.setTabComponentAt(MainFrame.TAB_MOTS_, motions.motion_panel);
			motions.removeListener(motions.tmpeditor);
			motions.addListener(me);
	    	MainFrame.status.addConstituentMeStatusListener(me);
		}
		
	}
}