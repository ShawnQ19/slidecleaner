import SwiftUI
import Photos

struct RandomCleanupScreen: View {
    @StateObject private var viewModel = SwipeCleanupViewModel()
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var loaded = false
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                statusBar
                progressBar
                
                if viewModel.isLoading {
                    Spacer()
                    VStack(spacing: AppPadding.md) {
                        ProgressView().tint(themeVM.colorScheme.primary)
                        Text("正在随机抽取...")
                            .font(.body)
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                    Spacer()
                } else if viewModel.items.isEmpty {
                    Spacer()
                    Text("没有更多照片了")
                        .foregroundColor(themeVM.colorScheme.textSecondary)
                    Spacer()
                } else {
                    TabView(selection: $viewModel.currentIndex) {
                        ForEach(Array(viewModel.visibleItems.enumerated()), id: \.element.id) { index, item in
                            SwipeableMediaCard(mediaItem: item, onSwipeUp: {
                                viewModel.queueForDelete(item)
                            })
                            .tag(index)
                        }
                        EndPageContent(
                            batchTotal: viewModel.batchTotal,
                            keptCount: viewModel.keptCount,
                            queuedCount: viewModel.queuedCount,
                            onConfirmDelete: { viewModel.showDeleteDialog = true },
                            onExit: {
                                viewModel.finalizeKeptItems()
                                dismiss()
                            }
                        )
                        .tag(viewModel.visibleItems.count)
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                    
                    if viewModel.currentIndex < viewModel.visibleItems.count {
                        DeleteQueueBar(
                            queuedCount: viewModel.queuedCount,
                            canUndo: viewModel.undoManager.canUndo,
                            onConfirmDelete: { viewModel.showDeleteDialog = true },
                            onUndo: { viewModel.undo() }
                        )
                    }
                }
            }
            
            topBar
            
            if viewModel.showDeleteDialog {
                GlassDialog(
                    title: "确认删除",
                    message: "确定要将 \(viewModel.queuedCount) 个项目移入系统回收站吗？",
                    confirmText: "删除",
                    isDestructive: true,
                    onConfirm: { viewModel.confirmDelete() },
                    onDismiss: { viewModel.showDeleteDialog = false }
                )
            }
            
            if viewModel.showPostDeleteDialog {
                GlassDialog(
                    title: "删除完成",
                    message: "\(viewModel.deleteMessage)，是否进行下一批随机整理？",
                    confirmText: "下一批",
                    dismissText: "返回首页",
                    onConfirm: {
                        viewModel.showPostDeleteDialog = false
                        loadNextBatch()
                    },
                    onDismiss: {
                        viewModel.showPostDeleteDialog = false
                        dismiss()
                    }
                )
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            if !loaded {
                loaded = true
                loadNextBatch()
            }
        }
    }
    
    private func loadNextBatch() {
        Task {
            viewModel.isLoading = true
            let store = ProcessedStore()
            let processedIds = await store.getProcessedIds()
            let service = PhotoLibraryService()
            do {
                let items = try await service.fetchRandomMedia(excluding: processedIds, count: DeleteQueue.maxSize)
                viewModel.items = items
                viewModel.batchTotal = items.count
                viewModel.hiddenItemIds.removeAll()
                viewModel.deleteQueue.removeAll()
                viewModel.undoManager.clear()
                viewModel.currentIndex = 0
                viewModel.isLoading = false
            } catch {
                viewModel.error = error.localizedDescription
                viewModel.isLoading = false
            }
        }
    }
    
    private var statusBar: some View {
        GlassCard {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("RANDOM BOARD")
                        .font(.caption2)
                        .foregroundColor(themeVM.colorScheme.textTertiary)
                        .fontWeight(.semibold)
                    Text("第 \(viewModel.currentIndex + 1) / \(viewModel.batchTotal) 张")
                        .font(.subheadline)
                        .fontWeight(.black)
                        .foregroundColor(themeVM.colorScheme.textPrimary)
                }
                Spacer()
                HStack(spacing: AppPadding.xs) {
                    StatusChip(label: "QUEUE \(viewModel.queuedCount)")
                    StatusChip(label: "READY")
                }
            }
        }
        .padding(.horizontal, AppPadding.lg)
        .padding(.vertical, AppPadding.sm)
    }
    
    private var progressBar: some View {
        let progress = viewModel.batchTotal > 0 ? CGFloat(viewModel.currentIndex + 1) / CGFloat(viewModel.batchTotal) : 0
        return RoundedRectangle(cornerRadius: 3)
            .fill(themeVM.colorScheme.surfaceOverlay)
            .frame(height: 3)
            .padding(.horizontal, AppPadding.lg)
            .overlay(
                RoundedRectangle(cornerRadius: 3)
                    .fill(themeVM.colorScheme.primary)
                    .scaleEffect(x: progress, y: 1, anchor: .leading)
                    .padding(.horizontal, AppPadding.lg)
            )
    }
    
    private var topBar: some View {
        VStack {
            GlassTopBar {
                HStack {
                    Button(action: {
                        viewModel.finalizeKeptItems()
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("随机清理")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                    }
                    Spacer()
                    Button(action: { loadNextBatch() }) {
                        Image(systemName: "shuffle")
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                }
                .padding(.horizontal, AppPadding.lg)
                .padding(.vertical, AppPadding.sm)
            }
            Spacer()
        }
    }
}
