import SwiftUI

enum AppRoute: Hashable {
    case mediaSwipe(String)
    case randomCleanup
    case processedGallery
    case trash
    case preview(String)
    case themeSettings
}

class AppRouter: ObservableObject {
    @Published var path = NavigationPath()
    
    func navigate(to route: AppRoute) {
        path.append(route)
    }
    
    func pop() {
        path.removeLast()
    }
    
    func popToRoot() {
        path = NavigationPath()
    }
}
