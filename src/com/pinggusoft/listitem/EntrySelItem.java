package com.pinggusoft.listitem;


public class EntrySelItem extends EntryItem implements Item {
	public boolean isChecked;
	public boolean checkFocus;
	public boolean sendClickEvent;

	public EntrySelItem(int drawable, String title, String subtitle, boolean isChecked, boolean checkFocus, boolean sendClickEvent, int id) {
		super(drawable, title, subtitle, id);
		this.checkFocus = checkFocus;
		this.isChecked = isChecked;
		this.sendClickEvent = sendClickEvent;
	}
	
	@Override
	public int getMode() {
		return MODE_ITEM_SEL;
	}

}
