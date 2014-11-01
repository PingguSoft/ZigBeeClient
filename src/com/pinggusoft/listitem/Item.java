package com.pinggusoft.listitem;

public interface Item {
	public final int MODE_SECTION = 1;
	public final int MODE_ITEM_DEFAULT = 2;
	public final int MODE_ITEM_SEL = 3;
	
	public int getMode();

}
