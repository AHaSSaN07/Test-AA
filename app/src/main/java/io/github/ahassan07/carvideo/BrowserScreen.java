package io.github.ahassan07.carvideo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * The main car screen. Android Auto only gives a drawing surface to map-style templates, so the
 * browser is drawn where a navigation app would draw its map.
 */
public final class BrowserScreen extends Screen implements SurfaceCallback, DefaultLifecycleObserver {
    private static final String TAG = "CarVideo";

    private final CarBrowser browser;
    private final MotionMonitor motion;

    public BrowserScreen(@NonNull CarContext carContext) {
        super(carContext);
        browser = new CarBrowser(carContext);
        motion = new MotionMonitor(carContext, browser::setLocked);
        getLifecycle().addObserver(this);
    }

    // ---- Lifecycle ----

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        getCarContext().getCarService(AppManager.class).setSurfaceCallback(this);
        motion.start();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        motion.stop();
        browser.destroy();
        try {
            getCarContext().getCarService(AppManager.class).setSurfaceCallback(null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Surface callback was already released", e);
        }
    }

    // ---- Template ----

    @NonNull
    @Override
    public Template onGetTemplate() {
        ActionStrip top = new ActionStrip.Builder()
                .addAction(iconAction(R.drawable.ic_back, browser::goBack))
                .addAction(iconAction(R.drawable.ic_sites, this::openSites))
                .addAction(iconAction(R.drawable.ic_search, this::openSearch))
                .addAction(iconAction(R.drawable.ic_reload, browser::reload))
                .build();

        // Android Auto only forwards taps and drags to the app when a pan action is present.
        ActionStrip side = new ActionStrip.Builder()
                .addAction(Action.PAN)
                .addAction(iconAction(R.drawable.ic_up, browser::pageUp))
                .addAction(iconAction(R.drawable.ic_down, browser::pageDown))
                .build();

        return new NavigationTemplate.Builder()
                .setActionStrip(top)
                .setMapActionStrip(side)
                .build();
    }

    private Action iconAction(int drawable, OnClickListener listener) {
        CarIcon icon = new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), drawable))
                .setTint(CarColor.DEFAULT)
                .build();
        return new Action.Builder().setIcon(icon).setOnClickListener(listener).build();
    }

    private void openSites() {
        getScreenManager().pushForResult(new SitesScreen(getCarContext()), this::onUrlPicked);
    }

    private void openSearch() {
        getScreenManager().pushForResult(new SearchScreen(getCarContext()), this::onUrlPicked);
    }

    private void onUrlPicked(Object result) {
        if (result instanceof String) {
            browser.load((String) result);
        }
    }

    // ---- Surface ----

    @Override
    public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
        browser.attach(surfaceContainer);
    }

    @Override
    public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
        browser.detach();
    }

    @Override
    public void onClick(float x, float y) {
        browser.tap(x, y);
    }

    @Override
    public void onScroll(float distanceX, float distanceY) {
        browser.scrollBy(distanceX, distanceY);
    }

    @Override
    public void onFling(float velocityX, float velocityY) {
        browser.fling(velocityX, velocityY);
    }
}
