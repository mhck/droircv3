package dk.cphbusiness.droirc.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import dk.cphbusiness.droirc.ChannelFragment;
import dk.cphbusiness.droirc.MainActivity;

public class TabsPagerAdapter extends FragmentPagerAdapter {

	private MainActivity parent;
	
	public TabsPagerAdapter(FragmentManager fm, MainActivity parent) {
		super(fm);
		this.parent = parent;
	}

	@Override
	public Fragment getItem(int index) {
		
//		switch (index) {
//		case 0:
//			return new ChannelFragment();
//		case 1:
//			return new ChannelFragment();
//		case 2:
//			return new ChannelFragment();
//		}
		return new ChannelFragment();

		//return null;
	}

	@Override
	public int getCount() {
		if (parent.getConnection() == null)
			return 1;
		return parent.getConnection().getChannels().size();
	}

}