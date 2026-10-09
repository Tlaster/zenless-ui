import SwiftUI
import Gallery

@main
struct ZenlessGalleryApp: App {
    var body: some Scene {
        WindowGroup { GalleryView().ignoresSafeArea() }
    }
}

struct GalleryView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { MainKt.MainViewController() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
