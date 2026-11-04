# Order Desk in Compose

Order Desk, a small order entry app where a shop employee takes an order for a customer, written once in
[Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) and running on Android, iPhone and iPad,
macOS, Windows and Linux. It is the demo of the [Lagoon theme](https://github.com/kormcrete/lagoon-theme)
for Compose: the same colours, type and shapes on every platform, with sizes that follow the platform, touch on
phones and denser on desktop.

It has the parts real business apps have:

- an orders list with search, status filter and month groups, and an order with delivery, payment, items and totals;
- a new order: customers with their payment terms, products by category or search, a cart with quantity steppers;
- a four-step checkout with rules (postal codes per country, Express only to Germany and Austria, free shipping from
  €100, a PO number for some customers, the credit limit);
- cancelling an order (stock and credit go back) and a receipt;
- one UI that lays itself out as a phone app on narrow screens and as a desktop app on wide ones.

Kotlin, Compose Multiplatform 1.9, JDK 25 on desktop. Customers, products and orders are invented and live in memory:
each start begins with the same data. The same app as a plain Swing application:
[swing-order-desk-demo](https://github.com/kormcrete/swing-order-desk-demo).

<p>
  <img src="docs/android-orders.png" alt="Order Desk on Android: the orders list grouped by month" width="24%">
  <img src="docs/android-new-order.png" alt="A new order on Android: Ada Shop Ltd, products with steppers and the cart bar" width="24%">
  <img src="docs/ios-orders.png" alt="Order Desk on iPhone: the orders list grouped by month" width="24%">
  <img src="docs/ios-new-order.png" alt="A new order on iPhone: Ada Shop Ltd, products with steppers and the cart bar" width="24%">
</p>
<p>
  <img src="docs/macos-orders.png" alt="Order Desk on macOS: the orders table next to order ORD-1637" width="49%">
  <img src="docs/macos-new-order.png" alt="A new order on macOS: categories, products and the cart side by side" width="49%">
  <img src="docs/windows-orders.png" alt="Order Desk on Windows: the orders table next to order ORD-1637" width="49%">
  <img src="docs/windows-new-order.png" alt="A new order on Windows: categories, products and the cart side by side" width="49%">
  <img src="docs/linux-orders.png" alt="Order Desk on Linux: the orders table next to order ORD-1637" width="49%">
  <img src="docs/linux-new-order.png" alt="A new order on Linux: categories, products and the cart side by side" width="49%">
</p>

<sub>Android and iPhone, then macOS, Windows and Linux: the same two scenes of the same code, taken by CI on an
emulator, a simulator and the three desktop systems. The macOS and Windows machines have small screens (about 1000
pixels wide), so their windows show the compact desktop layout; Linux shows it at 1280.</sub>

## Download

Try it without building anything. The Mac, Windows and Linux downloads include their own Java; the Java zip runs on your
own Java 17 or newer, on any computer.

| Your computer | Download |
|---|---|
| Mac with Apple Silicon (M1 and later) | [kormcrete-order-desk-compose-macos-arm64.dmg](https://github.com/kormcrete/compose-order-desk-demo/releases/latest/download/kormcrete-order-desk-compose-macos-arm64.dmg) |
| Mac with Intel | [kormcrete-order-desk-compose-macos-x64.dmg](https://github.com/kormcrete/compose-order-desk-demo/releases/latest/download/kormcrete-order-desk-compose-macos-x64.dmg) |
| Windows (x64) | [kormcrete-order-desk-compose-windows-x64.zip](https://github.com/kormcrete/compose-order-desk-demo/releases/latest/download/kormcrete-order-desk-compose-windows-x64.zip) |
| Linux (x64), Debian or Ubuntu | [kormcrete-order-desk-compose-linux-x64.deb](https://github.com/kormcrete/compose-order-desk-demo/releases/latest/download/kormcrete-order-desk-compose-linux-x64.deb) |
| Any computer with Java 17 or newer | [kormcrete-order-desk-compose-java.zip](https://github.com/kormcrete/compose-order-desk-demo/releases/latest/download/kormcrete-order-desk-compose-java.zip) |

The downloads are not signed yet, so the first launch takes a few extra clicks:

- **macOS:** open the `.dmg` and drag **Kormcrete Order Desk Compose** to Applications. The app is not notarized by Apple
  yet, so macOS refuses to open it ("Apple could not verify…"). Clear the download mark once, in Terminal, then open it:

  ```bash
  xattr -dr com.apple.quarantine "/Applications/Kormcrete Order Desk Compose.app"
  ```

- **Windows:** unzip, open the **Kormcrete Order Desk Compose** folder and run **Kormcrete Order Desk Compose.exe**. If
  SmartScreen stops it, click **More info → Run anyway**.
- **Linux:** `sudo apt install ./kormcrete-order-desk-compose-linux-x64.deb`, then start it from the applications menu.
- **Java zip:** unzip, then run `order-desk` in the **Kormcrete Order Desk Compose** folder (`order-desk.cmd` on Windows).
  It uses `JAVA_HOME` when set, else the `java` on your PATH.

The downloads are for the desktop; the phone and tablet apps are built from source, see [Run it](#run-it).

## One UI, two layouts

The width of the window decides the layout, not the platform:

- **Narrow** (phones, narrow windows): one screen at a time with a back stack; the cart is a floating bar; checkout is a
  full-screen flow with a step bar.
- **Wide** (desktop, tablets in landscape): a sidebar with the open drafts, the orders table next to the selected
  order, categories, products and cart side by side; checkout is a dialog. Below 1180 px it tightens: two-line order
  rows, categories as chips, the stock next to the SKU.

The theme decides how it looks, the platform how dense it is: 44 pt touch targets and 15 pt text on phones, 36 px
controls and 14 px text on desktop.

## Run it

Clone this repository and the Lagoon theme side by side; Gradle builds the theme from source until it is published:

```bash
git clone https://github.com/kormcrete/lagoon-theme.git
git clone https://github.com/kormcrete/compose-order-desk-demo.git
cd compose-order-desk-demo
```

You need a JDK to start Gradle. The build uses JDK 17 and the desktop app JDK 25: Gradle takes installed ones or
downloads them.

- **Desktop:** `./gradlew :desktopApp:run`, or `./gradlew :desktopApp:runRelease` for the shrunk release build.
  Narrow the window below 840 px for the phone layout. ⌘N (Ctrl+N) starts a new order, Esc closes a dialog.
- **Desktop with hot reload,** for working on the UI: in IntelliJ IDEA with the Kotlin Multiplatform plugin, open
  `desktopApp/…/Main.kt` and choose **Run 'desktopApp' with Compose Hot Reload** from the gutter; or
  `./gradlew :desktopApp:hotRun --mainClass io.github.kormcrete.orderdesk.desktop.MainKt --auto`. Saving a file
  reloads the running app. It runs on the JetBrains Runtime 21, downloaded when needed.
- **Android:** start an emulator or connect a phone, then `./gradlew :androidApp:installDebug`; or open the project in
  Android Studio and run **androidApp**.
- **In IntelliJ IDEA** with the Kotlin Multiplatform plugin (on a Mac): after `cd iosApp && xcodegen` once, the run
  configurations cover all three; pick **iosApp** and a simulator, **androidApp** and an emulator, or the desktop app.
- **iPhone and iPad** in Xcode (on a Mac):

  ```bash
  brew install xcodegen
  cd iosApp && xcodegen && open OrderDesk.xcodeproj
  ```

  Pick a simulator, or your device after choosing your team under **Signing & Capabilities**, and press Run. Xcode
  builds the Kotlin part itself.

**Scenes.** A launch option opens a prepared screen, the way the screenshots above are taken: `orders`, `order`,
`new-order`, `cart`, `checkout`, `review`. Desktop: `-Dorderdesk.scene=new-order`; Android:
`adb shell am start -n io.github.kormcrete.orderdesk/.android.MainActivity --es scene new-order`; iOS: the
`ORDERDESK_SCENE` environment variable (Xcode: Edit Scheme › Run › Arguments).

## Installers

The downloads come from `desktopApp/aot/train-and-package.sh`, which adds an AOT cache for a faster start (see
[Desktop runtime](#desktop-runtime)), and from the `release` workflow, which starts each package the way a user gets it.
A `vX.Y.Z` tag publishes them.

```bash
./gradlew :desktopApp:createReleaseDistributable
desktopApp/aot/train-and-package.sh <JDK 25 home> dist   # the .dmg, the .deb or the Windows .zip of this computer
./gradlew :desktopApp:packageJavaZip                     # the Java zip
./gradlew :androidApp:assembleRelease                    # Android, signed with the debug key
```

Gradle has to run on a JDK of the computer's own architecture: it picks Skia's native library by it.

## Build and test

```bash
./gradlew :shared:jvmTest                         # the model's tests, on the JVM
./gradlew :shared:iosSimulatorArm64Test           # the same tests on iOS (on a Mac)
./gradlew :shared:jvmTest -Pscreenshots=../shots  # also renders the main screens headless into shots/
```

CI builds Android, iOS and the desktop app on Linux, macOS and Windows, runs the tests, renders the screens, measures
the packaged desktop app and takes the screenshots above. `InteractionTest` clicks through the screens headless, on both
layouts.

## Project layout

| Module | |
|---|---|
| `shared` | The model (`model`: orders, catalog, cart, pricing, checkout rules; plain Kotlin, with tests) and the UI (`ui`), common to all platforms |
| `androidApp` | The Android app: one activity showing `App()` |
| `iosApp` | The iOS app: a SwiftUI shell around `MainViewController()`, as an XcodeGen project |
| `desktopApp` | The desktop app: window and menu bar, packaging, the memory probe and the window screenshot |

The UI is in `shared/src/commonMain/kotlin/io/github/kormcrete/orderdesk/ui`: `App` picks `PhoneLayout` or
`DesktopLayout`; both use the same parts (`Parts`, `CheckoutSteps`) and the same state (`OrderDeskState`). The look
comes from `lagoon-compose`: `LagoonTheme`, its tokens and components.

## Desktop runtime

A Java desktop app does not have to be heavy. The desktop app ships its own JDK 25 runtime, trimmed to the modules it
uses, shrunk with ProGuard into one jar, and starts with:

| Option | Why |
|---|---|
| `-XX:+UseSerialGC`, `-Xms24m -Xmx384m` | A collector made for small heaps, with a cap |
| `-XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30` | Gives unused heap back to the system |
| `-XX:+UseCompactObjectHeaders` | Smaller objects (JDK 25) |
| `-XX:+UseStringDeduplication` | Repeated strings stored once |
| `-XX:TieredStopAtLevel=1`, `-XX:ReservedCodeCacheSize=48m` | Quick JIT only: less compiler memory and fewer threads, plenty for UI code |
| `-Xss512k` | Smaller thread stacks |
| `-XX:-UsePerfData` | No statistics file for monitoring tools |

An AOT cache (JDK 25) makes the start faster: classes are loaded and linked once, in a training run, and stored next
to the jars. The JVM uses a cache only with the very same jars, so `desktopApp/aot/train-and-package.sh` trains the
app where its installer puts it, on the files the installer installs, and adds the cache to that package.

The splash, where the kormcrete k builds itself as in the Swing demo, is drawn by Compose in a small window of its own.
The JVM's own splash screen would show sooner, but on macOS, painting on it frame by frame doubled the memory the app
kept using afterwards (300 MB instead of 145 MB).

With `-Dorderdesk.probe=<file>` the app walks through its main screens, then writes what it uses and quits. The
`release` workflow runs it on every package, and fails if the app does not finish or does not use its cache. On a
Mac with Apple Silicon, from the `.dmg`, installed:

| | |
|---|---|
| Download | 43 MB |
| Splash on screen | 0.75 s |
| Window with the first screen, after the splash's 1.85 s | 2.7 s |
| Memory, Activity Monitor's figure, idle | 139 MB |
| Classes from the AOT cache | 6,616 of 6,633 |
| Java heap in use | 8 MB |

## License

Apache License 2.0. The Lagoon theme bundles the Inter font under the SIL Open Font License. What the downloads contain
is listed in [NOTICE](NOTICE).
