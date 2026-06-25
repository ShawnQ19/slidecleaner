import SwiftUI
import Photos

@MainActor
class TrashViewModel: ObservableObject {
    @Published var items: [MediaItem] = []
    @Published var isLoading = false
    @Published var isSelectMode = false
    @Published var selectedIds: Set<String> = []
    @Published var message = ""
    
    private let photoService = PhotoLibraryService()
    
    var selectedAssets: [PHAsset] {
        items.filter { selectedIds.contains($0.id) }.map { $0.asset }
    }
    
    func loadTrash() {
        isLoading = true
        Task {
            items = await photoService.fetchTrashedMedia()
            isLoading = false
        }
    }
    
    func toggleSelect(_ item: MediaItem) {
        if selectedIds.contains(item.id) {
            selectedIds.remove(item.id)
        } else {
            selectedIds.insert(item.id)
        }
    }
    
    func selectAll() {
        selectedIds = Set(items.map { $0.id })
    }
    
    func deselectAll() {
        selectedIds.removeAll()
    }
    
    func restoreSelected() {
        Task {
            do {
                try await photoService.restoreFromTrash(selectedAssets)
                items.removeAll { selectedIds.contains($0.id) }
                selectedIds.removeAll()
                isSelectMode = false
                message = "已恢复 \(selectedAssets.count) 项"
            } catch {
                message = "恢复失败: \(error.localizedDescription)"
            }
        }
    }
    
    func deleteSelected() {
        Task {
            do {
                try await photoService.deletePermanently(selectedAssets)
                items.removeAll { selectedIds.contains($0.id) }
                selectedIds.removeAll()
                isSelectMode = false
                message = "已永久删除"
            } catch {
                message = "删除失败: \(error.localizedDescription)"
            }
        }
    }
    
    func emptyTrash() {
        let allAssets = items.map { $0.asset }
        Task {
            do {
                try await photoService.deletePermanently(allAssets)
                items.removeAll()
                message = "回收站已清空"
            } catch {
                message = "清空失败: \(error.localizedDescription)"
            }
        }
    }
}

struct TrashScreen: View {
    @StateObject private var viewModel = TrashViewModel()
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showEmptyConfirm = false
    @State private var showDeleteConfirm = false
    
    private let columns = [
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2)
    ]
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                topBar
                
                if viewModel.isLoading {
                    Spacer()
                    ProgressView().tint(themeVM.colorScheme.primary)
                    Spacer()
                } else if viewModel.items.isEmpty {
                    Spacer()
                    Text("回收站为空")
                        .foregroundColor(themeVM.colorScheme.textSecondary)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 2) {
                            ForEach(viewModel.items) { item in
                                TrashGridItem(
                                    item: item,
                                    isSelected: viewModel.selectedIds.contains(item.id),
                                    isSelectMode: viewModel.isSelectMode,
                                    onTap: {
                                        if viewModel.isSelectMode {
                                            viewModel.toggleSelect(item)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    bottomBar
                }
            }
            
            if showEmptyConfirm {
                GlassDialog(
                    title: "清空回收站",
                    message: "确定要永久删除回收站中的所有项目吗？此操作不可撤销。",
                    confirmText: "清空",
                    isDestructive: true,
                    onConfirm: {
                        showEmptyConfirm = false
                        viewModel.emptyTrash()
                    },
                    onDismiss: { showEmptyConfirm = false }
                )
            }
            
            if showDeleteConfirm {
                GlassDialog(
                    title: "永久删除",
                    message: "确定要永久删除选中的 \(viewModel.selectedIds.count) 个项目吗？",
                    confirmText: "删除",
                    isDestructive: true,
                    onConfirm: {
                        showDeleteConfirm = false
                        viewModel.deleteSelected()
                    },
                    onDismiss: { showDeleteConfirm = false }
                )
            }
        }
        .navigationBarHidden(true)
        .onAppear { viewModel.loadTrash() }
    }
    
    private var topBar: some View {
        GlassTopBar {
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(themeVM.colorScheme.textPrimary)
                }
                Text("回收站")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                Spacer()
                if viewModel.isSelectMode {
                    Button(action: { viewModel.deselectAll(); viewModel.isSelectMode = false }) {
                        Text("取消")
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                } else {
                    Menu {
                        Button("全选") { viewModel.selectAll(); viewModel.isSelectMode = true }
                        Button("清空回收站", role: .destructive) { showEmptyConfirm = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                }
            }
            .padding(.horizontal, AppPadding.lg)
            .padding(.vertical, AppPadding.sm)
        }
    }
    
    private var bottomBar: some View {
        GlassBottomBar {
            HStack(spacing: AppPadding.sm) {
                Button(action: { viewModel.restoreSelected() }) {
                    Text("恢复 (\(viewModel.selectedIds.count))")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(themeVM.colorScheme.success)
                        )
                }
                .disabled(viewModel.selectedIds.isEmpty)
                
                Button(action: { showDeleteConfirm = true }) {
                    Text("删除 (\(viewModel.selectedIds.count))")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(themeVM.colorScheme.destructive)
                        )
                }
                .disabled(viewModel.selectedIds.isEmpty)
            }
        }
    }
}

struct TrashGridItem: View {
    let item: MediaItem
    let isSelected: Bool
    let isSelectMode: Bool
    let onTap: () -> Void
    @State private var image: UIImage?
    
    var body: some View {
        ZStack {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(1, contentMode: .fill)
            } else {
                Rectangle()
                    .fill(Color.white.opacity(0.08))
                    .onAppear { loadThumb() }
            }
            
            if isSelectMode {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundColor(isSelected ? .accentColor : .white.opacity(0.5))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .padding(6)
            }
            
            if item.isVideo, let duration = item.duration {
                Text(formatDuration(duration))
                    .font(.caption2)
                    .foregroundColor(.white)
                    .padding(2)
                    .background(Color.black.opacity(0.6))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                    .padding(4)
            }
        }
        .clipped()
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .onLongPressGesture {
            if !isSelectMode { onTap() }
        }
    }
    
    private func loadThumb() {
        let size = CGSize(width: 200, height: 200)
        PHImageManager.default().requestImage(
            for: item.asset, targetSize: size,
            contentMode: .aspectFill,
            options: nil
        ) { result, _ in self.image = result }
    }
    
    private func formatDuration(_ d: TimeInterval) -> String {
        let m = Int(d) / 60
        let s = Int(d) % 60
        return String(format: "%d:%02d", m, s)
    }
}
