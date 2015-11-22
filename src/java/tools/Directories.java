package tools;
public class Directories {
	/**
	 * Stores in database the version and root paths (parent of parameter)
	 * @param crt_version_path
	 */
	public static void setLinuxPaths(String crt_version_path) {
		net.ddp2p.tools.Directories.setLinuxPaths(crt_version_path);
	}
	/**
	 * Stores in database the version and root paths (parent of parameter)
	 * @param crt_version_path
	 */
	public static void setWindowsPaths(String crt_version_path) {
		net.ddp2p.tools.Directories.setWindowsPaths(crt_version_path);
	}
	public static void setCrtPathsInDB(String path_version_dir) {
		net.ddp2p.tools.Directories.setCrtPathsInDB(path_version_dir);
	}
	public static void setCrtPaths(String path_version_dir) {
		net.ddp2p.tools.Directories.setCrtPaths(path_version_dir);
	}
}
