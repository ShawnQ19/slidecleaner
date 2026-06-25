import Foundation
import Photos

enum PhotoLibraryError: Error {
    case accessDenied
    case accessLimited
    case unknown
}

actor PhotoLibraryService {
    
    func requestAccess() async -> PHAuthorizationStatus {
        await PHPhotoLibrary.requestAuthorization(for: .readWrite)
    }
    
    func checkAccess() -> PHAuthorizationStatus {
        PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }
    
    func fetchMediaGroups(excluding processedIds: Set<String> = []) async throws -> [MediaGroup] {
        let status = checkAccess()
        guard status == .authorized || status == .limited else {
            throw PhotoLibraryError.accessDenied
        }
        
        let fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        fetchOptions.predicate = NSPredicate(format: "mediaType == %d OR mediaType == %d",
                                              PHAssetMediaType.image.rawValue,
                                              PHAssetMediaType.video.rawValue)
        
        let results = PHAsset.fetchAssets(with: fetchOptions)
        
        var monthGroups: [String: [MediaItem]] = [:]
        let calendar = Calendar.current
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy年M月"
        
        results.enumerateObjects { asset, _, _ in
            let item = MediaItem.from(asset: asset)
            guard !processedIds.contains(item.id) else { return }
            
            guard let date = asset.creationDate else { return }
            let components = calendar.dateComponents([.year, .month], from: date)
            guard let monthDate = calendar.date(from: components) else { return }
            
            let key = dateFormatter.string(from: monthDate)
            monthGroups[key, default: []].append(item)
        }
        
        let sortedKeys = monthGroups.keys.sorted { a, b in
            let da = dateFormatter.date(from: a) ?? Date()
            let db = dateFormatter.date(from: b) ?? Date()
            return da > db
        }
        
        return sortedKeys.compactMap { key in
            guard let items = monthGroups[key], !items.isEmpty else { return nil }
            let date = dateFormatter.date(from: key) ?? Date()
            let idFormatter = DateFormatter()
            idFormatter.dateFormat = "yyyy-MM"
            return MediaGroup(
                id: idFormatter.string(from: date),
                yearMonth: date,
                label: key,
                mediaItems: items
            )
        }
    }
    
    func fetchRandomMedia(excluding processedIds: Set<String>, count: Int) async throws -> [MediaItem] {
        let fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        fetchOptions.predicate = NSPredicate(format: "mediaType == %d OR mediaType == %d",
                                              PHAssetMediaType.image.rawValue,
                                              PHAssetMediaType.video.rawValue)
        
        let results = PHAsset.fetchAssets(with: fetchOptions)
        var allItems: [MediaItem] = []
        
        results.enumerateObjects { asset, _, _ in
            let item = MediaItem.from(asset: asset)
            if !processedIds.contains(item.id) {
                allItems.append(item)
            }
        }
        
        // Reservoir sampling
        var sampled: [MediaItem] = []
        for (i, item) in allItems.enumerated() {
            if i < count {
                sampled.append(item)
            } else {
                let j = Int.random(in: 0...i)
                if j < count {
                    sampled[j] = item
                }
            }
        }
        
        return sampled
    }
    
    func moveToTrash(_ assets: [PHAsset]) async throws {
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.deleteAssets(assets as NSArray)
        }
    }
    
    func restoreFromTrash(_ assets: [PHAsset]) async throws {
        try await PHPhotoLibrary.shared().performChanges {
            for asset in assets {
                let request = PHAssetChangeRequest(for: asset)
                request.isTrashed = false
            }
        }
    }
    
    func deletePermanently(_ assets: [PHAsset]) async throws {
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.deleteAssets(assets as NSArray)
        }
    }
    
    func fetchTrashedMedia() async -> [MediaItem] {
        let fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        fetchOptions.predicate = NSPredicate(format: "isTrashed == YES")
        
        let results = PHAsset.fetchAssets(with: fetchOptions)
        var items: [MediaItem] = []
        results.enumerateObjects { asset, _, _ in
            items.append(MediaItem.from(asset: asset))
        }
        return items
    }
}
