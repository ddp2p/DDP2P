package widgets.justifications;

import java.awt.Panel;

import javax.swing.JLabel;

import widgets.components.DocumentEditor;
import config.JustificationsListener;
import config.MotionsListener;
import data.D_Document;
import data.D_Justification;
import data.D_Motion;

/**
 * Just Viewing Justifications without editing them (e.g. at the bottom of Debate).
 * @author msilaghi
 *
 */

@SuppressWarnings("serial")
public class JustificationViewer extends Panel implements JustificationsListener, MotionsListener {
		DocumentEditor viewer;
		private D_Justification justificationViewed;
		private JLabel labelViewer;
		JustificationViewer() {
			Panel p = this;
			labelViewer = new JLabel();
			viewer = new DocumentEditor();
			
			viewer.init(widgets.justifications.JustificationEditor.TEXT_LEN_ROWS);
	/*
			//viewer.addListener(this);
			//p.add(panel_body, c);
			//panel_body.add(viewer.getComponent());
			viewer.getComponent(D_Document.RTEDIT).setVisible(false);
			viewer.getComponent(D_Document.TEXTAREA).setVisible(false);
			viewer.getComponent(D_Document.PDFVIEW).setVisible(false);

			viewer.getComponent(D_Document.RTEDIT).setEnabled(false);
			viewer.getComponent(D_Document.TEXTAREA).setEnabled(false);
			viewer.getComponent(D_Document.PDFVIEW).setEnabled(false);

			p.add(viewer.getComponent(D_Document.TEXTAREA));
			p.add(viewer.getComponent(D_Document.RTEDIT));
			p.add(viewer.getComponent(D_Document.PDFVIEW));
*/
			
			labelViewer.setVisible(false);
			p.add(labelViewer);
			this.viewer.setEnabled(false);
		}
		@Override
		public void motion_update(String motID, int col, D_Motion d_motion) {
			// TODO Auto-generated method stub
			this.viewer.setText("");
			
		}

		@Override
		public void motion_forceEdit(String motID) {
			// TODO Auto-generated method stub
			this.viewer.setText("");
			
		}

		@Override
		public void justUpdate(String justID, int col, boolean db_sync,
				D_Justification just) {
			if (just == null && justID != null)
				just = D_Justification.getJustByLID(justID, true, false);
			if (just != null && justID == null)
				justID = just.getLIDstr();
			
			justificationViewed = just;
			if (just == null) {
				this.viewer.setText("");
				return;
			}
			
			labelViewer.setVisible(false);
			viewer.getComponent().setVisible(false);
//			viewer.getComponent(D_Document.RTEDIT).setVisible(false);
//			viewer.getComponent(D_Document.TEXTAREA).setVisible(false);
//			viewer.getComponent(D_Document.PDFVIEW).setVisible(false);
			
			String format = justificationViewed.getJustificationBody().getFormatString();
			String document = justificationViewed.getJustificationBody().getDocumentString();
			if (D_Document.PDF_BODY_FORMAT.equals(format)) {
				this.viewer.setType(format);
				this.viewer.setText(document);			
				viewer.getComponent().setVisible(true);
				this.viewer.setEnabled(false);
			} else {
				this.labelViewer.setText(document);
				labelViewer.setVisible(true);
			}
			//viewer.getComponent(viewer.get.getJustificationBody().getFormatString()).setVisible(false);
		}

		@Override
		public void forceJustificationEdit(String justID) {
			D_Justification just = null;
			if (just == null && justID != null)
				just = D_Justification.getJustByLID(justID, true, false);
			if (just != null && justID == null)
				justID = just.getLIDstr();
			
			justificationViewed = just;
			if (just == null) {
				this.viewer.setText("");
				return;
			}
			viewer.getComponent().setVisible(false);
			this.viewer.setType(justificationViewed.getJustificationBody().getFormatString());
			this.viewer.setText(justificationViewed.getJustificationBody().getDocumentString());			
			viewer.getComponent().setVisible(true);
			this.viewer.setEnabled(false);
		}
		
	}