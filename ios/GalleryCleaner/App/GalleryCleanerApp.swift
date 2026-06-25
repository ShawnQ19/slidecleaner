import SwiftUI

@main
struct GalleryCleanerApp: App {
    @StateObject private var themeViewModel = ThemeViewModel()
    @StateObject private var router = AppRouter()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(themeViewModel)
                .environmentObject(router)
                .preferredColorScheme(.dark)
        }
    }
}

struct ContentView: View {
    @EnvironmentObject var themeVM: ThemeViewModel
    @EnvironmentObject var router: AppRouter
    
    var body: some View {
        NavigationStack(path: $router.path) {
            GalleryScreen()
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .mediaSwipe(let groupId):
                        MediaSwipeDestination(groupId: groupId)
                    case .randomCleanup:
                        RandomCleanupScreen()
                    case .processedGallery:
                        ProcessedGalleryScreen()
                    case .trash:
                        TrashScreen()
                    case .preview(let mediaId):
                        PreviewDestination(mediaId: mediaId)
                    case .themeSettings:
                        ThemeSettingsScreen()
                    }
                }
        }
        .tint(themeVM.colorScheme.primary)
    }
}

struct MediaSwipeDestination: View {
    let groupId: String
    // In real implementation, load group by ID from ViewModel
    var body: some View {
        Text("Loading group \(groupId)...")
    }
}

struct PreviewDestination: View {
    let mediaId: String
    var body: some View {
        Text("Loading media \(mediaId)...")
    }
}
