package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import net.ddp2p.common.data.D_Motion;

public class MotionItem {
	D_Motion motion;
	public String mot_name;
	public String body;
	public String choice1;
	public String choice2;
	public String choice3;
	public String toString() {
		if (motion == null) return mot_name;
		return motion.getMotionTitle().title_document.getDocumentString();
	}
}