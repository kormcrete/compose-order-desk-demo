# compose-order-desk-demo

Order Desk, a small order entry app, written once in Compose Multiplatform and running on Android,
iPhone and iPad, and desktop (macOS, Windows, Linux). It shows the
[Lagoon theme](https://github.com/kormcrete/swing-flatlaf-lagoon-theme) for Compose: the same colours,
type and shapes on every platform, with sizes that follow the platform (touch on phones, denser on desktop).

The same app also exists as a plain Swing application:
[swing-order-desk-demo](https://github.com/kormcrete/swing-order-desk-demo).

## What it does

- **Orders**: search, filter by status, grouped by month; an order with its delivery, shipping, payment,
  items and totals; cancel a placed order (stock and credit go back); show the receipt.
- **New order**: pick a customer (recent first), browse products by category or search, add with +,
  change quantities with steppers. Low and missing stock is shown; 10 or more of one product take 5% off.
- **Checkout** in four steps: address (checked per country), shipping (Express only to Germany and
  Austria, Standard free from €100), payment (invoice with a PO number where the customer needs one, or
  card), review with the credit check, then place the order.

All customers, products and orders are invented demo data.

## One UI, two layouts

The window width decides the layout, not the platform:

- **Narrow** (phones, narrow windows): one screen at a time with a back stack; the cart is a floating bar;
  checkout is a full-screen flow.
- **Wide** (desktop, tablets in landscape): a sidebar with open drafts, the orders table next to the
  selected order, and categories, products and cart side by side; checkout is a dialog.

## Project layout

| Module | |
|---|---|
| `shared` | The model (`model`, plain Kotlin, with tests) and the UI (`ui`), common to all platforms |
| `androidApp` | The Android app: an activity that shows `App()` |
| `desktopApp` | The desktop app: window, menu bar, packaging, and the memory probe |
| `iosApp` | The iOS app: a SwiftUI shell around `MainViewController()`, as an XcodeGen project |

## Build and run

Until the Lagoon theme is published, Gradle builds it from source. Clone both repositories side by side:

```
git clone https://github.com/kormcrete/swing-flatlaf-lagoon-theme
git clone https://github.com/kormcrete/compose-order-desk-demo
cd compose-order-desk-demo
```

(`-Plagoon.build=<path>/compose` points to another checkout.) You need JDK 17 for the build and JDK 25 for
the desktop app; Gradle finds an installed JDK 25 or takes `-Porderdesk.jdk=<path>`.

- **Desktop**: `./gradlew :desktopApp:run`, or `./gradlew :desktopApp:packageReleaseDmg`
  (`packageReleaseMsi` on Windows, `packageReleaseDeb` on Linux) for an installer.
- **Android**: `./gradlew :androidApp:installDebug` with a device or emulator, or open the project in Android Studio.
- **iPhone/iPad** (on a Mac with Xcode): `brew install xcodegen`, then `cd iosApp && xcodegen`, open
  `OrderDesk.xcodeproj`, pick a simulator or your device (set your team under Signing) and run.
  The Xcode build compiles the Kotlin framework itself.
- **Tests**: `./gradlew :shared:jvmTest` (model on the JVM), `:shared:iosSimulatorArm64Test` on a Mac.
- **Screens**: `./gradlew :shared:jvmTest -Pscreenshots=../screenshots` renders the main screens headless.

## Desktop runtime

A Java desktop app does not have to be big or slow to start. The desktop app ships its own JDK 25 runtime,
trimmed to the modules it uses, and starts with:

| Option | Why |
|---|---|
| `-XX:+UseSerialGC`, `-Xms24m -Xmx384m` | A collector made for small heaps, with a cap |
| `-XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30` | Gives unused heap back to the system |
| `-XX:+UseCompactObjectHeaders` | Smaller objects (JDK 25) |
| `-XX:+UseStringDeduplication` | Repeated strings stored once |
| `-XX:TieredStopAtLevel=1`, `-XX:ReservedCodeCacheSize=48m` | Quick JIT only: less compiler memory and fewer threads, plenty fast for UI code |
| `-Xss512k` | Smaller thread stacks |

Release builds are shrunk with ProGuard. `-Dorderdesk.probe=<file>` makes the app walk through its main
screens, then write what it uses (heap, non-heap, resident memory, the JVM's native breakdown) and quit;
CI runs it on the packaged app on Linux, macOS and Windows.

## License

Apache License 2.0. The Lagoon theme bundles the Inter font under the SIL Open Font License.
