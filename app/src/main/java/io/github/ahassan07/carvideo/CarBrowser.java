package io.github.ahassan07.carvideo;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.car.app.SurfaceContainer;

import java.lang.ref.WeakReference;

/**
 * Draws a WebView on the surface Android Auto hands to the app.
 *
 * <p>The surface is wrapped in a virtual display and a {@link Presentation} holds the views. Android
 * Auto does not forward raw touch events, only taps and scroll distances, so taps are replayed
 * into the view tree as synthetic touch events and scrolling is applied to the page directly.
 * Everything here runs on the main thread.
 */
final class CarBrowser {
    private static final String TAG = "CarVideo";
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    private static WeakReference<CarBrowser> sActive = new WeakReference<>(null);

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private VirtualDisplay virtualDisplay;
    private Presentation presentation;
    private FrameLayout root;
    private FrameLayout fullscreenHolder;
    private TextView lockView;
    private WebView webView;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private Bundle savedState;
    private String pendingUrl;
    private boolean locked;
    private boolean destroyed;

    CarBrowser(Context context) {
        this.context = context;
        sActive = new WeakReference<>(this);
    }

    /** True while a car session is running. */
    static boolean isCarConnected() {
        CarBrowser browser = sActive.get();
        return browser != null && !browser.destroyed;
    }

    /** Opens an address on the car screen from the phone. Returns false if no car session runs. */
    static boolean loadOnCar(String url) {
        CarBrowser browser = sActive.get();
        if (browser == null || browser.destroyed) {
            return false;
        }
        browser.load(url);
        return true;
    }

    // ---- Surface lifecycle ----

    void attach(SurfaceContainer container) {
        detach();
        Surface surface = container.getSurface();
        if (destroyed || surface == null || !surface.isValid()) {
            return;
        }
        int width = container.getWidth();
        int height = container.getHeight();
        int dpi = Math.max(120, Math.round(container.getDpi() * Prefs.scale(context)));
        try {
            DisplayManager displays = context.getSystemService(DisplayManager.class);
            virtualDisplay = displays.createVirtualDisplay(
                    "CarVideo", width, height, dpi, surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            presentation = new Presentation(context, virtualDisplay.getDisplay());
            Window window = presentation.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
            buildViews(presentation.getContext());
            presentation.setContentView(root);
            presentation.show();
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not show the browser on the car screen", e);
            detach();
            return;
        }

        if (pendingUrl != null) {
            webView.loadUrl(pendingUrl);
        } else if (savedState == null || webView.restoreState(savedState) == null) {
            webView.loadUrl(Prefs.home(context));
        }
        pendingUrl = null;
        savedState = null;
    }

    private void buildViews(Context viewContext) {
        root = new FrameLayout(viewContext);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(viewContext);
        WebSetup.configure(webView);
        webView.setWebChromeClient(new ChromeClient());
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(MATCH, MATCH));

        fullscreenHolder = new FrameLayout(viewContext);
        fullscreenHolder.setBackgroundColor(Color.BLACK);
        fullscreenHolder.setVisibility(View.GONE);
        root.addView(fullscreenHolder, new FrameLayout.LayoutParams(MATCH, MATCH));

        lockView = new TextView(viewContext);
        lockView.setText(R.string.car_lock_message);
        lockView.setTextColor(Color.WHITE);
        lockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        lockView.setGravity(Gravity.CENTER);
        lockView.setBackgroundColor(Color.BLACK);
        lockView.setClickable(true);
        lockView.setVisibility(locked ? View.VISIBLE : View.GONE);
        root.addView(lockView, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    void detach() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            hideCustomView();
            Bundle state = new Bundle();
            if (webView.saveState(state) != null) {
                savedState = state;
            }
            webView.stopLoading();
            root.removeAllViews();
            webView.destroy();
            webView = null;
        }
        root = null;
        fullscreenHolder = null;
        lockView = null;
        if (presentation != null) {
            try {
                presentation.dismiss();
            } catch (RuntimeException e) {
                Log.w(TAG, "Presentation was already gone", e);
            }
            presentation = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    void destroy() {
        detach();
        destroyed = true;
        savedState = null;
        if (sActive.get() == this) {
            sActive = new WeakReference<>(null);
        }
    }

    // ---- Commands from the car buttons and the phone ----

    void load(String url) {
        if (url == null || destroyed) {
            return;
        }
        if (webView == null) {
            // The surface is away (another screen is showing); open it when the surface returns.
            pendingUrl = url;
            return;
        }
        hideCustomView();
        webView.loadUrl(url);
    }

    void goHome() {
        load(Prefs.home(context));
    }

    /** Leaves fullscreen video first, then walks back through the page history. */
    void goBack() {
        if (customView != null) {
            hideCustomView();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }

    void reload() {
        if (webView != null) {
            webView.reload();
        }
    }

    void pageUp() {
        if (canScroll()) {
            webView.pageUp(false);
        }
    }

    void pageDown() {
        if (canScroll()) {
            webView.pageDown(false);
        }
    }

    /** Covers the picture while the car is moving. Sound keeps playing underneath. */
    void setLocked(boolean locked) {
        this.locked = locked;
        if (lockView != null) {
            lockView.setVisibility(locked ? View.VISIBLE : View.GONE);
        }
    }

    // ---- Touch input from Android Auto ----

    void tap(float x, float y) {
        if (root == null || locked) {
            return;
        }
        final long downTime = SystemClock.uptimeMillis();
        dispatchTouch(MotionEvent.ACTION_DOWN, downTime, downTime, x, y);
        handler.postDelayed(
                () -> dispatchTouch(MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), x, y),
                40);
    }

    private void dispatchTouch(int action, long downTime, long eventTime, float x, float y) {
        View target = root;
        if (target == null) {
            return;
        }
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            target.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    /** Distances follow the Android gesture convention: positive means the finger moved up/left. */
    void scrollBy(float distanceX, float distanceY) {
        if (canScroll()) {
            webView.scrollBy(Math.round(distanceX), Math.round(distanceY));
        }
    }

    /** Velocities are finger velocities, so the page flings the opposite way. */
    void fling(float velocityX, float velocityY) {
        if (canScroll()) {
            webView.flingScroll(Math.round(-velocityX), Math.round(-velocityY));
        }
    }

    private boolean canScroll() {
        return webView != null && !locked && customView == null;
    }

    // ---- Fullscreen video ----

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        if (fullscreenHolder != null) {
            fullscreenHolder.removeView(customView);
            fullscreenHolder.setVisibility(View.GONE);
        }
        customView = null;
        WebChromeClient.CustomViewCallback callback = customViewCallback;
        customViewCallback = null;
        if (callback != null) {
            callback.onCustomViewHidden();
        }
    }

    private final class ChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null || fullscreenHolder == null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            fullscreenHolder.addView(view, new FrameLayout.LayoutParams(MATCH, MATCH));
            fullscreenHolder.setVisibility(View.VISIBLE);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            // Avoids the grey placeholder WebView draws before a video has a frame.
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }
}
