import Foundation

struct MediaGroup: Identifiable {
    let id: String          // "2025-03" format
    let yearMonth: Date     // first day of month
    let label: String       // "2025年3月"
    let mediaItems: [MediaItem]
    
    var thumbnail: MediaItem? { mediaItems.first }
    var count: Int { mediaItems.count }
}
