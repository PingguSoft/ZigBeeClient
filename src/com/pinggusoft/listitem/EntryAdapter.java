package com.pinggusoft.listitem;

import java.util.ArrayList;

import com.pinggusoft.zigbee_client.R;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class EntryAdapter extends ArrayAdapter<Item> {
	private Context context;
	private ArrayList<Item> items;
	private LayoutInflater vi;
	private int itemRes;
	private int itemFocused;
	private boolean allCheckSel;

	public EntryAdapter(Context context, ArrayList<Item> items, int itemRes) {
		super(context,0, items);
		this.context = context;
		this.items = items;
		this.itemRes = itemRes;
		vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		allCheckSel = true;
		for (Item item : items) {
			if (item.getMode() != Item.MODE_ITEM_SEL) {
				allCheckSel = false;
				break;
			}
		}
	}
	
	public void setFocus(int position) {
		itemFocused = position;
	}

	@Override
	public boolean isEnabled(int position) {
	    EntryItem ei = (EntryItem)items.get(position);
	    
	    return ei.isEnabled();
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		View v = convertView;

		final Item i = items.get(position);
		
		if (i != null) {
			if(i.getMode() == Item.MODE_SECTION) {
				SectionItem si = (SectionItem)i;
				v = vi.inflate(R.layout.list_item_section, null);

				v.setOnClickListener(null);
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
				
				final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
				sectionView.setText(si.getTitle());
			} else {
				v = vi.inflate(itemRes, null);
				EntryItem ei = (EntryItem)i;
				
				int next = position + 1; 
				if (next < items.size()) {
					Item ni = items.get(next);
					if (ni.getMode() == Item.MODE_SECTION) {
					    if (ei.isEnabled())
	                        v.setBackgroundResource(R.drawable.shape_no_seperator);
	                    else
	                        v.setBackgroundResource(R.drawable.shape_no_seperator_disabled);
					}
					else {
					    if (ei.isEnabled())
					        v.setBackgroundResource(R.drawable.shape_seperator);
					    else
					        v.setBackgroundResource(R.drawable.shape_seperator_disabled);
					}
				} else {
				    if (ei.isEnabled())
				        v.setBackgroundResource(R.drawable.shape_no_seperator);
				    else
				        v.setBackgroundResource(R.drawable.shape_no_seperator_disabled);
				}

				if (allCheckSel && position == itemFocused) {
					v.setBackgroundColor(context.getResources().getColor(R.color.focused));
				}
				
				final TextView title = (TextView)v.findViewById(R.id.list_item_entry_title);
				final TextView subtitle = (TextView)v.findViewById(R.id.list_item_entry_summary);
				final CheckBox box = (CheckBox)v.findViewById(R.id.list_item_checkSel);
				final ImageView image = (ImageView)v.findViewById(R.id.list_item_entry_drawable);
				
				
				if (box != null) {
					if (i.getMode() == Item.MODE_ITEM_DEFAULT) 
						box.setVisibility(View.INVISIBLE);
					else {
						EntrySelItem si = (EntrySelItem)i;
						box.setVisibility(View.VISIBLE);
						box.setChecked(si.isChecked);
						if (si.checkFocus == true) {
							box.setOnClickListener(new View.OnClickListener () {
								public void onClick(View v) {
									EntrySelItem si = (EntrySelItem)i;
									si.isChecked = !si.isChecked;
									if (si.sendClickEvent) {
										ListView av = (ListView)parent;
										av.performItemClick(av, position, ((1L << 31) | si.id));
									}
								}
							});
						}
					}
				}

				if (image != null && ei.drawable > 0) {
					image.setImageResource(ei.drawable);
				}
					
				if (title != null) 
					title.setText(ei.title);
				if(subtitle != null)
					subtitle.setText(ei.subtitle);
				
                if (ei.isEnabled()) {
                    v.setEnabled(true);
                }
                else {
                    v.setEnabled(false);
                }
			}
		}
		return v;
	}

}
