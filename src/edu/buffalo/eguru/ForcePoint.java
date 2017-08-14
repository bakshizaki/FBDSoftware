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

	enum EntityType {
		FORCE, MOMENT
	}
	enum EntityProperty {
		KNOWN, UNKNOWN
	}
	
	enum EntityDirection {
		 CLOCKWISE, ANTICLOCKWISE
	}
	
	EntityProperty property;
	EntityType type;
	EntityDirection direction;
	int angle;
	public boolean isSelected() {
		return isSelected;
	}
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
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

	public EntityProperty getProperty() {
		return property;
	}

	public void setProperty(EntityProperty property) {
		this.property = property;
	}

	public EntityType getType() {
		return type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public EntityDirection getDirection() {
		return direction;
	}

	public void setDirection(EntityDirection direction) {
		this.direction = direction;
	}


}
