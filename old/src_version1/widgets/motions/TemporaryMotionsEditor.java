package widgets.motions;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JScrollPane;

import config.DD;
import data.D_Motion;

final class TemporaryMotionsEditor implements MotionsListener {
	boolean _DEBUG = true;
	Motions motions = null;
	public TemporaryMotionsEditor(Motions motions) {
		this.motions = motions;
		if(Motions.DEBUG) System.out.println("TmpMotions:created");
	}

	@Override
	public void motion_update(String motID, int col,
			D_Motion d_motion) {
	
		if(motID==null){
			if(Motions.DEBUG) System.out.println("TmpMotions:update: null motID: "+d_motion);
			return;
		}
		synchronized(motions.motion_panel) {
			if(motions._medit!=null){
				if(Motions.DEBUG) System.out.println("TmpMotions:update: already set");
				return;
			}
			if(Motions.DEBUG) System.out.println("TmpMotions:update: set");
			MotionEditor me = new MotionEditor();
			//Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
			//Component panel = new JScrollPane(motions._medit = me);
			//panel.setMinimumSize(_minimumSize);
			//motions.motion_panel.setRightComponent(panel);
			
			// needed since otherwise the tab gets lost (component's parent being reassigned)
			DD.tabbedPane.setComponentAt(DD.TAB_MOTS_, DD.JunkPanelMots);
			motions.motion_panel = DD.makeMotionPanel(me, motions.motion_panel);
			if(DD.tabbedPane.getSelectedIndex() == DD.TAB_MOTS_){
				DD.tabbedPane.setComponentAt(DD.TAB_MOTS_, motions.motion_panel);
				if(_DEBUG) System.out.println("TmpMotions:selected motion");
			}else{
				if(_DEBUG) System.out.println("TmpMotions:not selected");
			}
			motions.removeListener(motions.tmpeditor);
			motions.addListener(me);
		}
		
	}

	@Override
	public void motion_forceEdit(String motID) {
		
		if(motID==null){
			if(Motions.DEBUG) System.out.println("TmpMotions: force: null motID ");
			return;
		}
		synchronized(motions.motion_panel) {
			if(motions._medit!=null){
				if(Motions.DEBUG) System.out.println("TmpMotions:force: already set");
				return;
			}
			if(Motions.DEBUG) System.out.println("TmpMotions:force: set");
			MotionEditor me = new MotionEditor();
//			Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
//			Component panel = new JScrollPane(motions._medit = me);
//			panel.setMinimumSize(_minimumSize);
//			motions.motion_panel.setRightComponent(panel);
			motions.motion_panel = DD.makeMotionPanel(me, motions.motion_panel);
			if(DD.tabbedPane.getSelectedIndex() == DD.TAB_MOTS_)
				DD.tabbedPane.setTabComponentAt(DD.TAB_MOTS_, motions.motion_panel);
			motions.removeListener(motions.tmpeditor);
			motions.addListener(me);
		}
		
	}
}