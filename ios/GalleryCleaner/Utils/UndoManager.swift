import Foundation
import SwiftUI

class UndoManager: ObservableObject {
    @Published var canUndo: Bool = false
    
    private var stack: [(item: MediaItem, index: Int)] = []
    
    func push(_ item: MediaItem, at index: Int) {
        stack.append((item, index))
        canUndo = !stack.isEmpty
    }
    
    func pop() -> (item: MediaItem, index: Int)? {
        guard !stack.isEmpty else { return nil }
        let result = stack.removeLast()
        canUndo = !stack.isEmpty
        return result
    }
    
    func clear() {
        stack.removeAll()
        canUndo = false
    }
}
