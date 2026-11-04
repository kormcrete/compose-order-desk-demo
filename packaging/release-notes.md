Order Desk in Compose Multiplatform, the demo of the [Lagoon theme](https://github.com/kormcrete/lagoon-theme) for
Compose, ready to run. The same app as a plain Swing application:
[swing-order-desk-demo](https://github.com/kormcrete/swing-order-desk-demo). The Mac, Windows and Linux downloads include
their own Java; the Java zip runs on your own Java 17 or newer, on any computer.

| Your computer | Download |
|---|---|
| Mac with Apple Silicon (M1 and later) | `kormcrete-order-desk-compose-macos-arm64.dmg` |
| Mac with Intel | `kormcrete-order-desk-compose-macos-x64.dmg` |
| Windows (x64) | `kormcrete-order-desk-compose-windows-x64.zip` |
| Linux (x64), Debian or Ubuntu | `kormcrete-order-desk-compose-linux-x64.deb` |
| Any computer with Java 17 or newer | `kormcrete-order-desk-compose-java.zip` |

**First launch.** The downloads are not signed yet.

- macOS: open the `.dmg` and drag **Kormcrete Order Desk Compose** to Applications. The app is not notarized by Apple
  yet, so macOS refuses to open it ("Apple could not verify…"). Clear the download mark once, in Terminal, then open it:

      xattr -dr com.apple.quarantine "/Applications/Kormcrete Order Desk Compose.app"
- Windows: unzip, open the **Kormcrete Order Desk Compose** folder and run **Kormcrete Order Desk Compose.exe**. If
  SmartScreen stops it, click **More info → Run anyway**.
- Linux: `sudo apt install ./kormcrete-order-desk-compose-linux-x64.deb`, then start it from the applications menu.
- Java zip: unzip, then run `order-desk` in the **Kormcrete Order Desk Compose** folder (`order-desk.cmd` on Windows). It
  uses `JAVA_HOME` when set, else the `java` on your PATH.
