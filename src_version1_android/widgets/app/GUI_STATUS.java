package widgets.app;

import config.Application;
import config.Identity;
import data.D_Constituent;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_News;
import data.D_Organization;
import data.D_Peer;
import util.DDP2P_DoubleLinkedList_Node;
import util.Util;

@Deprecated
public class GUI_STATUS {
	/*
	@Deprecated
	class CRT_GUI_STATUS implements util.DDP2P_DoubleLinkedList_Node_Payload<CRT_GUI_STATUS>{
		private DDP2P_DoubleLinkedList_Node<CRT_GUI_STATUS> node;
		public String identity_ID;
		//public String agent_peer_ID;
		//public String agent_instance_ID;
		public String selected_peer_ID;
		public String selected_organization_ID;
		public String selected_neighborhood_ID;
		public String selected_constituent_ID;
		public String selected_motion_ID;
		public String selected_justification_ID;
		public String selected_news_ID;

		public long _identity_ID;
		//public long _agent_peer_ID;
		//public long _agent_instance_ID;
		public long _selected_peer_ID;
		public long _selected_organization_ID;
		public long _selected_neighborhood_ID;
		public long _selected_constituent_ID;
		public long _selected_motion_ID;
		public long _selected_justification_ID;
		public long _selected_news_ID;

		// Each of them should have a counter of references
		// from this object: int status_refrences=0
		// Only free from cache when the counter is 0.
		public Identity selected_identity;
		//public long _agent_peer_ID;
		//public long _agent_instance_ID;
		public D_Peer selected_peer;
		public D_Organization selected_organization;
		public D_Neighborhood selected_neighborhood;
		public D_Constituent selected_constituent;
		public D_Motion selected_motion;
		public D_Justification selected_justification;
		public D_News selected_news;

		public CRT_GUI_STATUS() {}
		public CRT_GUI_STATUS(CRT_GUI_STATUS crt_state) {
			if(crt_state == null) return;
			selected_identity = crt_state.selected_identity;
			if(selected_identity!=null) selected_identity.status_references++;
			
			selected_peer = crt_state.selected_peer;
			if(selected_peer!=null) selected_peer.status_references++;
			
			selected_organization = crt_state.selected_organization;
			if(selected_organization!=null) selected_organization.status_references++;
			
			selected_neighborhood = crt_state.selected_neighborhood;
			if(selected_neighborhood!=null) selected_neighborhood.status_references++;
			
			selected_constituent = crt_state.selected_constituent;
			if(selected_constituent!=null) selected_constituent.status_references++;
			
			selected_motion = crt_state.selected_motion;
			if(selected_motion!=null) selected_motion.status_references++;
			
			selected_justification = crt_state.selected_justification;
			if(selected_justification!=null) selected_justification.status_references++;
			
			selected_news = crt_state.selected_news;
			if(selected_news!=null) selected_news.status_references++;
		}

		@Override
		public DDP2P_DoubleLinkedList_Node<CRT_GUI_STATUS> set_DDP2P_DoubleLinkedList_Node(
				DDP2P_DoubleLinkedList_Node<CRT_GUI_STATUS> _node) {
			DDP2P_DoubleLinkedList_Node<CRT_GUI_STATUS> _old = node;
			node = _node;
			return _old;
		}
 
		@Override
		public DDP2P_DoubleLinkedList_Node<CRT_GUI_STATUS> get_DDP2P_DoubleLinkedList_Node() {
			return node;
		}
		
	}
	util.DDP2P_DoubleLinkedList<CRT_GUI_STATUS> states = new util.DDP2P_DoubleLinkedList<CRT_GUI_STATUS>();
	static CRT_GUI_STATUS crt_state = null;
	
	private void push(){
		CRT_GUI_STATUS cgs = new CRT_GUI_STATUS(crt_state);
		states.offerFirst(cgs);
	}
	
	public static String getCrtConstituentID() {
		if(true)return Util.getString(Application.constituents.tree.getModel().getConstituentIDMyself());
		//widgets.constituent.ConstituentsModel.getConstituentIDMyself();
		if(crt_state == null) return null;
		return crt_state.selected_constituent_ID;
	}

	public static long getCrt_ConstituentID() {
		if(true)return Application.constituents.tree.getModel().getConstituentIDMyself();
		//widgets.constituent.ConstituentsModel.getConstituentIDMyself();
		if(crt_state == null) return -1;
		return crt_state._selected_constituent_ID;
	}
*/	
}