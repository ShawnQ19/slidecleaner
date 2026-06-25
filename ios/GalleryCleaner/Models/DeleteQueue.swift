import Foundation

struct DeleteQueue {
    static let maxSize = 50
    
    var items: [MediaItem] = []
    
    var shouldPromptDelete: Bool {
        items.count >= Self.maxSize
    }
}
