package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

public class Photo {

	private String filePath;
	private String ID;
	private String bitmap;

	
	public Photo(String _filePath, String _ID, String _bitmap) {
		filePath = _filePath;
		ID = _ID;
		bitmap = _bitmap;
	}
	

	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public String getBitmap() {
		return bitmap;
	}

	public void setBitmap(String bitmap) {
		this.bitmap = bitmap;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}
	
	
	
	
	
}
