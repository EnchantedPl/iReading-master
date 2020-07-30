package com.iReadingGroup.iReading.Adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.iReadingGroup.iReading.Fragment.AboutFragment;
import com.iReadingGroup.iReading.Fragment.ArticleListFragment;
import com.iReadingGroup.iReading.Fragment.RadioPlayerFragment;
import com.iReadingGroup.iReading.Fragment.CollectionFragment;
import com.iReadingGroup.iReading.Fragment.WordSearchFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivityPagesAdapter
 * A bridge between fragment and each page
 */
public class MainActivityPagesAdapter extends FragmentPagerAdapter {

    /**
     * The Fragments.
     */
    private List<Fragment> fragments = new ArrayList<>();


    /**
     * Instantiates a new Main adapter.
     *
     * @param fm the fm
     */
    public MainActivityPagesAdapter(FragmentManager fm) {
        super(fm);
        fragments.add(new ArticleListFragment());
        fragments.add(new WordSearchFragment());
        fragments.add(new CollectionFragment());
        fragments.add(new RadioPlayerFragment());
        fragments.add(new AboutFragment());
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
