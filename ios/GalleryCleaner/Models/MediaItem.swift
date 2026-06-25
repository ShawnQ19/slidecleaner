import Foundation
import Photos

struct MediaItem: Identifiable, Hashable {
    let id: String          // PHAsset localIdentifier
    let asset: PHAsset
    let name: String
    let mimeType: String
    let dateAdded: Date
    let size: Int64
    let duration: TimeInterval?
    let width: Int
    let height: Int
    
    var isVideo: Bool { asset.mediaType == .video }
    var isLivePhoto: Bool { asset.mediaSubtypes.contains(.photoLive) }
    
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: MediaItem, rhs: MediaItem) -> Bool { lhs.id == rhs.id }
}

extension MediaItem {
    static func from(asset: PHAsset) -> MediaItem {
        let resources = PHAssetResource.assetResources(for: asset).first
        let fileName = resources?.originalFilename ?? "Unknown"
        let utType = resources?.uniformTypeIdentifier ?? ""
        let fileSize = resources?.value(forKey: "fileSize") as? Int64 ?? 0
        
        return MediaItem(
            id: asset.localIdentifier,
            asset: asset,
            name: fileName,
            mimeType: utType,
            dateAdded: asset.creationDate ?? Date(),
            size: fileSize,
            duration: asset.mediaType == .video ? asset.duration : nil,
            width: asset.pixelWidth,
            height: asset.pixelHeight
        )
    }
}
