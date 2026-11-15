# Car Video

A small Android app that shows a web browser on the Android Auto screen, with YouTube as the
home page. It is a personal, sideloaded app and is not on the Play Store.

**Status: builds on GitHub, not yet tested on a real car screen.**

## Install

1. Open the [latest release](../../releases/latest) on your phone and download `CarVideo.apk`.
2. Install it (allow your browser to install unknown apps when asked) and open it once.
3. Tap **Allow speed detection** and accept the prompts.
4. In Android Auto settings, tap **Version** about ten times to unlock developer mode, then open
   the three-dot menu, choose **Developer settings** and turn on **Unknown sources**.
5. Connect to the car and open **Car Video** from the app list on the car screen.

## Using it

Buttons on the car screen:

| Button | What it does |
| --- | --- |
| Arrow | Back (also leaves fullscreen video) |
| Star | Saved sites |
| Magnifier | Search YouTube or type a web address (car keyboard, parked only) |
| Circular arrow | Reload |
| Up / down | Scroll one page |

Tap and drag on the page as usual. Typing inside a web page is not possible on the car screen;
use the search button, or type on the phone and tap **Send to car**.

To sign in to a site, use **Open on phone** in the phone app. Sign-ins are shared with the car
screen. Google sign-in does not work in embedded browsers, so YouTube runs signed out.

## Limits

- **Moving car:** the picture is covered while the car moves and sound keeps playing. This uses
  the car's speed reading, or the phone's GPS if the car does not report speed. If neither is
  available the app cannot tell and does not cover the picture. Do not watch while driving.
- **Netflix and similar services** use copy protection that does not play in this kind of browser.
- **Touch input:** Android Auto passes only taps and scroll distances to apps, so pinch-zoom,
  long-press and sideways swipes inside the page do not work.
- Android Auto may hide sideloaded apps on some phone and Android versions.

## How it works

Android Auto only gives a drawing surface to map-style templates. The app registers as a
navigation-category Car App Library app, wraps that surface in a virtual display, and shows a
`WebView` on it. Source is in `app/src/main/java`.

## Build

Every push to `main` builds the APK with GitHub Actions and publishes it as a release.
Locally: `./gradlew assembleRelease` (needs the Android SDK).
