package config;

import hds.DirectoryAnswerMultipleIdentities;

import java.util.Hashtable;

public interface DirectoriesData_View {

	void setData(
			Hashtable<String, Hashtable<String, DirectoryAnswerMultipleIdentities>> dir_data);
	
}