import Foundation
import SwiftUI

@MainActor
class GalleryViewModel: ObservableObject {
    @Published var groups: [MediaGroup] = []
    @Published var isLoading = false
    @Published var isInitialLoading = false
    @Published var error: String?
    
    private let photoService = PhotoLibraryService()
    private let processedStore = ProcessedStore()
    private var hasLoaded = false
    
    var totalCount: Int { groups.reduce(0) { $0 + $1.count } }
    var monthCount: Int { groups.count }
    var averagePerMonth: Int { monthCount > 0 ? totalCount / monthCount : 0 }
    
    func onPermissionGranted() {
        guard !hasLoaded || groups.isEmpty else { return }
        guard !isLoading else { return }
        hasLoaded = true
        loadMediaGroups()
    }
    
    func loadMediaGroups() {
        guard !isLoading else { return }
        
        isLoading = true
        isInitialLoading = groups.isEmpty
        error = nil
        
        Task {
            do {
                let processedIds = await processedStore.getProcessedIds()
                let fetched = try await photoService.fetchMediaGroups(excluding: processedIds)
                groups = fetched
                isInitialLoading = false
                isLoading = false
            } catch {
                self.error = error.localizedDescription
                isInitialLoading = false
                isLoading = false
            }
        }
    }
    
    func retry() {
        error = nil
        isLoading = false
        isInitialLoading = false
        hasLoaded = false
        loadMediaGroups()
    }
}
