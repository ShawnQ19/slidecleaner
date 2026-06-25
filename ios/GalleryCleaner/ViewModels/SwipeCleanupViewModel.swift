import Foundation
import SwiftUI
import Photos

@MainActor
class SwipeCleanupViewModel: ObservableObject {
    @Published var items: [MediaItem] = []
    @Published var hiddenItemIds: Set<String> = []
    @Published var deleteQueue: [MediaItem] = []
    @Published var keptItemIds: Set<String> = []
    @Published var currentIndex: Int = 0
    @Published var isLoading = false
    @Published var error: String?
    @Published var showDeleteDialog = false
    @Published var showPostDeleteDialog = false
    @Published var showExitConfirmDialog = false
    @Published var deleteMessage = ""
    
    let undoManager = UndoManager()
    let batchTotal: Int
    let processedStore = ProcessedStore()
    let photoService = PhotoLibraryService()
    
    var visibleItems: [MediaItem] {
        items.filter { !hiddenItemIds.contains($0.id) }
    }
    
    var keptCount: Int { batchTotal - deleteQueue.count }
    var queuedCount: Int { deleteQueue.count }
    
    init(items: [MediaItem] = []) {
        self.items = items
        self.batchTotal = items.count
    }
    
    func queueForDelete(_ item: MediaItem) {
        guard !deleteQueue.contains(where: { $0.id == item.id }) else { return }
        hiddenItemIds.insert(item.id)
        keptItemIds.remove(item.id)
        deleteQueue.append(item)
        undoManager.push(item, at: currentIndex)
        
        if deleteQueue.count >= DeleteQueue.maxSize {
            showDeleteDialog = true
        }
    }
    
    func keepCurrent() {
        guard currentIndex < visibleItems.count else { return }
        let item = visibleItems[currentIndex]
        keptItemIds.insert(item.id)
    }
    
    func undo() {
        guard let entry = undoManager.pop() else { return }
        guard let idx = deleteQueue.firstIndex(where: { $0.id == entry.item.id }) else { return }
        deleteQueue.remove(at: idx)
        hiddenItemIds.remove(entry.item.id)
    }
    
    func confirmDelete() {
        guard !deleteQueue.isEmpty else {
            finalizeKeptItems()
            showDeleteDialog = false
            return
        }
        
        let assetsToDelete = deleteQueue.map { $0.asset }
        
        Task {
            do {
                try await photoService.moveToTrash(assetsToDelete)
                let deletedIds = Set(deleteQueue.map { $0.id })
                items.removeAll { deletedIds.contains($0.id) }
                hiddenItemIds.subtract(deletedIds)
                deleteQueue.removeAll()
                undoManager.clear()
                finalizeKeptItems()
                
                deleteMessage = "已移入系统回收站 \(assetsToDelete.count) 项"
                showDeleteDialog = false
                showPostDeleteDialog = true
            } catch {
                deleteMessage = "删除失败: \(error.localizedDescription)"
                showDeleteDialog = false
            }
        }
    }
    
    func finalizeKeptItems() {
        let ids = Array(keptItemIds)
        guard !ids.isEmpty else { return }
        Task {
            await processedStore.markProcessed(ids)
        }
        keptItemIds.removeAll()
    }
    
    func clearDeleteQueue() {
        deleteQueue.removeAll()
        hiddenItemIds.removeAll()
    }
}
