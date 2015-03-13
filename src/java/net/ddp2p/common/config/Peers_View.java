package net.ddp2p.common.config;

import net.ddp2p.common.hds.ASNPluginInfo;

public interface Peers_View {

	void setConnectionState(String peer_ID, int stateConnectionTcp);

	void disconnectWidget();

	Object get_privateOrgPanel();

	String get_privateOrgPanel__get_organizationID();

	void setPluginsInfo(ASNPluginInfo[] plugins, String _global_peer_ID,
			String _peer_ID);

	boolean registerPluginMenu(String plugin_ID, String plugin_name,
			int column, 
			//JMenuItem
			Object plugin_menu_item);

	boolean deregisterPluginMenu(String plugin_ID, int column);

	void registerPlugin(String plugin_GID, String plugin_name,
			String plugin_info, String plugin_url, 
			//TableCellRenderer
			Object renderer,
			//TableCellEditor
			Object editor);

	boolean deregisterPlugin(String plugin_GID);

	boolean registerPluginMenuAction(String plugin_GID, String plugin_name,
			int column, 
			//Action
			Object plugin_menuItem);
	
}