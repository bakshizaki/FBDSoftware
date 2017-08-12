package edu.buffalo.eguru;

import java.awt.Point;

public class ForcePoint extends Point {
	
	public ForcePoint(Point p) {
		this.setLocation(p);
	}
	
	public ForcePoint(int x, int y) {
		this.setLocation(x,y);
	}
	
	//used for selection and hence deletion
	boolean isSelected;
	
	//true if point has a froce/moment attached to it
	boolean isCorrect = false;

	
	enum forceType {
		KNOWN, UNKNOWN, CLOCKWISE, ANTICLOCKWISE
	}
	
	forceType type;
	int angle;
	public boolean isSelected() {
		return isSelected;
	}
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	public forceType getType() {
		return type;
	}
	public void setType(forceType type) {
		this.type = type;
	}
	public int getAngle() {
		return angle;
	}
	public void setAngle(int angle) {
		this.angle = angle;
	}
	public boolean isCorrect() {
		return isCorrect;
	}

	public void setCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}


}
