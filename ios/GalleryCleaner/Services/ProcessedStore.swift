import Foundation

actor ProcessedStore {
    private let key = "processed_media_ids"
    
    func getProcessedIds() -> Set<String> {
        let array = UserDefaults.standard.stringArray(forKey: key) ?? []
        return Set(array)
    }
    
    func markProcessed(_ ids: [String]) {
        var current = getProcessedIds()
        current.formUnion(ids)
        UserDefaults.standard.set(Array(current), forKey: key)
    }
    
    func resetAll() {
        UserDefaults.standard.removeObject(forKey: key)
    }
}
