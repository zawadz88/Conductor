package com.bluelinelabs.conductor.support;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;

import java.util.List;

/**
 * An adapter for ViewPagers that uses Routers as pages
 */
public abstract class RouterPagerAdapter extends PagerAdapter {

    private static final String KEY_SAVED_PAGES = "RouterPagerAdapter.savedStates";
    private static final String KEY_SAVES_STATE = "RouterPagerAdapter.savesState";

    private final Controller host;
    private boolean savesState;
    private SparseArray<Bundle> savedPages = new SparseArray<>();

    /**
     * Creates a new RouterPagerAdapter using the passed host.
     */
    public RouterPagerAdapter(Controller host, boolean saveRouterState) {
        this.host = host;
        savesState = saveRouterState;
    }

    /**
     * Called when a router is instantiated. Here the router's root should be set if needed.
     *
     * @param router   The router used for the page
     * @param position The page position to be instantiated.
     */
    public abstract void configureRouter(Router router, int position);

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final String name = makeRouterName(container.getId(), getItemId(position));

        Router router = host.getChildRouter(container, name);
        if (savesState && !router.hasRootController()) {
            Bundle routerSavedState = savedPages.get(position);

            if (routerSavedState != null) {
                router.restoreInstanceState(routerSavedState);
            }
        }

        router.rebindIfNeeded();
        configureRouter(router, position);
        return router;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Router router = (Router)object;

        if (savesState) {
            Bundle savedState = new Bundle();
            router.saveInstanceState(savedState);
            savedPages.put(position, savedState);
        }

        host.removeChildRouter(router);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        Router router = (Router)object;
        final List<RouterTransaction> backstack = router.getBackstack();
        for (RouterTransaction transaction : backstack) {
            if (transaction.controller().getView() == view) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Parcelable saveState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SAVES_STATE, savesState);
        bundle.putSparseParcelableArray(KEY_SAVED_PAGES, savedPages);
        return bundle;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle bundle = (Bundle)state;
        if (state != null) {
            savesState = bundle.getBoolean(KEY_SAVES_STATE, false);
            savedPages = bundle.getSparseParcelableArray(KEY_SAVED_PAGES);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    private static String makeRouterName(int viewId, long id) {
        return viewId + ":" + id;
    }

}