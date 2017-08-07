package edu.buffalo.eguru;

import java.awt.Point;

public class ZPoint extends Point {

	boolean isSelected = false;
	int cutCounter;
	public ZPoint(int x, int y, int cutC) {
		this.setLocation(x, y);
		this.cutCounter = cutC;
	}
	
	public ZPoint(Point p, int cutC) {
		this.setLocation(p);
		this.cutCounter = cutC;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean var) {
		isSelected = var;
	}

	public int getCutCounter() {
		return cutCounter;
	}

	public void setCutCounter(int cutCounter) {
		this.cutCounter = cutCounter;
	}
	
	
}
