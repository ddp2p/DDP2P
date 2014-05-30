package tools;

import hds.StartUp;
import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.DD;

public class Directories {
	/**
	 * Stores in database the version and root paths (parent of parameter)
	 * @param crt_version_path
	 */
	public static void setLinuxPaths(String crt_version_path) {
		String linux_path = crt_version_path;
		if(!linux_path.endsWith(Application.LINUX_PATH_SEPARATOR)) linux_path += Application.LINUX_PATH_SEPARATOR;
		String _linux_parent=Util.getParent(linux_path);
		if((_linux_parent!=null) && !_linux_parent.endsWith(Application.LINUX_PATH_SEPARATOR)) _linux_parent += Application.LINUX_PATH_SEPARATOR;
		try {
			DD.setAppText(DD.APP_LINUX_INSTALLATION_PATH, linux_path);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH, _linux_parent);
			DD.setAppText(DD.APP_LINUX_SCRIPTS_PATH, null);
			DD.setAppText(DD.APP_LINUX_PLUGINS_PATH, null);
			DD.setAppText(DD.APP_LINUX_DATABASE_PATH, null);
			DD.setAppText(DD.APP_LINUX_LOGS_PATH, null);
			DD.setAppText(DD.APP_LINUX_DD_JAR_PATH, null);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		System.out.println("Saved in application field="+DD.APP_LINUX_INSTALLATION_PATH+" value=\""+crt_version_path+"\"\nparent: \""+_linux_parent+"\"");
	}
	/**
	 * Stores in database the version and root paths (parent of parameter)
	 * @param crt_version_path
	 */
	public static void setWindowsPaths(String crt_version_path) {
		String win_path = crt_version_path;
		if(!win_path.endsWith(Application.WINDOWS_PATH_SEPARATOR)){
			System.out.println("Windows path without terminator="+win_path);
			win_path += Application.WINDOWS_PATH_SEPARATOR;
			System.out.println("Windows path with terminator="+win_path);
		}
		System.out.println("Windows path with terminator="+win_path);
		String _win_parent=Util.getParent(win_path);
		if((_win_parent!=null) && !_win_parent.endsWith(Application.WINDOWS_PATH_SEPARATOR)) _win_parent += Application.WINDOWS_PATH_SEPARATOR;
		try {
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_PATH, win_path);
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH, _win_parent);
			DD.setAppText(DD.APP_LINUX_SCRIPTS_PATH, null);
			DD.setAppText(DD.APP_LINUX_PLUGINS_PATH, null);
			DD.setAppText(DD.APP_LINUX_DATABASE_PATH, null);
			DD.setAppText(DD.APP_LINUX_LOGS_PATH, null);
			DD.setAppText(DD.APP_LINUX_DD_JAR_PATH, null);

		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		System.out.println("Saved in application field="+DD.APP_WINDOWS_INSTALLATION_PATH+" value=\""+crt_version_path+"\"\nparent: \""+_win_parent+"\"");
	}
	public static void setCrtPathsInDB(String path_version_dir) {
		int os = StartUp.getCrtOS();
		switch (os) {
		case DD.WINDOWS:
			setWindowsPaths(path_version_dir);
			break;
		case DD.LINUX:
			setLinuxPaths(path_version_dir);
			break;
		case DD.MAC:
			setLinuxPaths(path_version_dir);
			break;
		default:
		}
	}
	public static void setCrtPaths(String path_version_dir) {
		setCrtPathsInDB(path_version_dir);
		//StartUpThread.detect_OS_fill_var();
		try {
			StartUp.fill_install_paths_all_OSs_from_DB();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} 
		StartUp.switch_install_paths_to_ones_for_current_OS();		
	}
	static public void main(String args[]) {
		System.out.println("Saved in application field values="+args.length);
		if (args.length==0) return;
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			setLinuxPaths(args[0]);
			
			if(args.length > 1)
				setWindowsPaths(args[1]);
			
			StartUp.detect_OS_and_store_in_DD_OS_var();
			StartUp.fill_install_paths_all_OSs_from_DB(); // to be done before importing!!!
			StartUp.switch_install_paths_to_ones_for_current_OS();		
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}
