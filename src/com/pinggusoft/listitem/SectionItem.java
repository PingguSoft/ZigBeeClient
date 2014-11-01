package com.pinggusoft.listitem;

public class SectionItem implements Item{

	private final String title;
	
	public SectionItem(String title) {
		this.title = title;
	}
	
	public String getTitle(){
		return title;
	}
	
	@Override
	public int getMode() {
		return MODE_SECTION;
	}

}
