import SwiftUI
import Shared

@main
struct OrderDeskApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose draws edge to edge and pads itself for the notch and home indicator.
            ComposeView().ignoresSafeArea(.all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
