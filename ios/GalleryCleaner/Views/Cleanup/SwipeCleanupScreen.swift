import SwiftUI
import Photos

struct SwipeCleanupScreen: View {
    let group: MediaGroup
    @StateObject private var viewModel: SwipeCleanupViewModel
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    
    init(group: MediaGroup) {
        self.group = group
        _viewModel = StateObject(wrappedValue: SwipeCleanupViewModel(items: group.mediaItems))
    }
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                statusBar
                progressBar
                
                if viewModel.isLoading {
                    Spacer()
                    ProgressView().tint(themeVM.colorScheme.primary)
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
                    message: "\(viewModel.deleteMessage)，是否继续整理下一个月？",
                    confirmText: "返回首页",
                    dismissText: "下一批",
                    onConfirm: {
                        viewModel.showPostDeleteDialog = false
                        dismiss()
                    },
                    onDismiss: {
                        viewModel.showPostDeleteDialog = false
                        // Load next month or dismiss
                        dismiss()
                    }
                )
            }
            
            if viewModel.showExitConfirmDialog {
                GlassDialog(
                    title: "确认退出",
                    message: "返回首页将不会删除任何照片，确定退出吗？",
                    confirmText: "退出",
                    isDestructive: true,
                    onConfirm: {
                        viewModel.showExitConfirmDialog = false
                        viewModel.clearDeleteQueue()
                        dismiss()
                    },
                    onDismiss: { viewModel.showExitConfirmDialog = false }
                )
            }
        }
        .navigationBarHidden(true)
    }
    
    private var statusBar: some View {
        GlassCard {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("SWIPE BOARD")
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
                    .frame(width: nil, height: 3)
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
                        Text("滑动清理")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                        Text("\(group.label) · \(viewModel.batchTotal) 张")
                            .font(.caption)
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                    Spacer()
                }
                .padding(.horizontal, AppPadding.lg)
                .padding(.vertical, AppPadding.sm)
            }
            Spacer()
        }
    }
}

struct StatusChip: View {
    let label: String
    @EnvironmentObject var themeVM: ThemeViewModel
    
    var body: some View {
        Text(label)
            .font(.caption2)
            .fontWeight(.semibold)
            .foregroundColor(themeVM.colorScheme.primary)
            .padding(.horizontal, AppPadding.sm)
            .padding(.vertical, AppPadding.xs)
            .background(
                Capsule()
                    .fill(themeVM.colorScheme.primary.opacity(0.14))
                    .overlay(
                        Capsule()
                            .stroke(themeVM.colorScheme.primary.opacity(0.24), lineWidth: 1)
                    )
            )
    }
}

struct SwipeableMediaCard: View {
    let mediaItem: MediaItem
    let onSwipeUp: () -> Void
    @EnvironmentObject var themeVM: ThemeViewModel
    @State private var offset: CGSize = .zero
    @State private var image: UIImage?
    
    var body: some View {
        ZStack {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                RoundedRectangle(cornerRadius: 16)
                    .fill(themeVM.colorScheme.surface)
                    .onAppear { loadFullImage() }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .offset(y: offset.height < 0 ? offset.height : 0)
        .scaleEffect(offset.height < 0 ? 1 + offset.height / 1000 : 1)
        .opacity(offset.height < -100 ? 0.5 : 1)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.translation.height < 0 {
                        offset = value.translation
                    }
                }
                .onEnded { value in
                    if value.translation.height < -100 {
                        withAnimation(.easeOut(duration: 0.3)) {
                            offset = CGSize(width: 0, height: -500)
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            onSwipeUp()
                            offset = .zero
                        }
                    } else {
                        withAnimation(.spring()) {
                            offset = .zero
                        }
                    }
                }
        )
        .padding(.horizontal, AppPadding.lg)
    }
    
    private func loadFullImage() {
        let screen = UIScreen.main.bounds
        let targetSize = CGSize(width: screen.width * 2, height: screen.height * 2)
        let options = PHImageRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true
        
        PHImageManager.default().requestImage(
            for: mediaItem.asset,
            targetSize: targetSize,
            contentMode: .aspectFit,
            options: options
        ) { result, _ in
            self.image = result
        }
    }
}

struct EndPageContent: View {
    let batchTotal: Int
    let keptCount: Int
    let queuedCount: Int
    let onConfirmDelete: () -> Void
    let onExit: () -> Void
    @EnvironmentObject var themeVM: ThemeViewModel
    
    var body: some View {
        GlassCard {
            VStack(spacing: AppPadding.md) {
                Text("SWIPE SEQUENCE COMPLETE")
                    .font(.caption2)
                    .foregroundColor(themeVM.colorScheme.textTertiary)
                    .fontWeight(.semibold)
                
                Text("本轮清理完成")
                    .font(.title)
                    .fontWeight(.black)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                
                HStack(spacing: AppPadding.lg) {
                    StatBadge(label: "总数", value: "\(batchTotal)")
                    StatBadge(label: "保留", value: "\(keptCount)", color: themeVM.colorScheme.success)
                    if queuedCount > 0 {
                        StatBadge(label: "删除", value: "\(queuedCount)", color: themeVM.colorScheme.destructive)
                    }
                }
                
                Spacer().frame(height: AppPadding.sm)
                
                if queuedCount > 0 {
                    Button(action: onConfirmDelete) {
                        HStack {
                            Image(systemName: "trash")
                            Text("确认删除 (\(queuedCount) 张)")
                                .fontWeight(.bold)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(themeVM.colorScheme.destructive)
                        )
                    }
                }
                
                Button(action: onExit) {
                    HStack {
                        Image(systemName: "house")
                        Text("返回首页")
                    }
                    .foregroundColor(themeVM.colorScheme.textTertiary)
                }
            }
        }
        .padding(.horizontal, AppPadding.xxl)
    }
}

struct StatBadge: View {
    let label: String
    let value: String
    var color: Color = .accentColor
    
    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.title)
                .fontWeight(.black)
                .foregroundColor(color)
            Text(label)
                .font(.caption2)
                .foregroundColor(.white.opacity(0.4))
                .fontWeight(.semibold)
        }
    }
}

struct DeleteQueueBar: View {
    let queuedCount: Int
    let canUndo: Bool
    let onConfirmDelete: () -> Void
    let onUndo: () -> Void
    @EnvironmentObject var themeVM: ThemeViewModel
    
    var body: some View {
        GlassBottomBar {
            VStack(spacing: AppPadding.sm) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("DELETE QUEUE")
                            .font(.caption2)
                            .foregroundColor(themeVM.colorScheme.textTertiary)
                            .fontWeight(.semibold)
                        Text("向上滑动加入待删除队列")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                    }
                    Spacer()
                    HStack(spacing: 4) {
                        Text("\(queuedCount)")
                            .font(.subheadline)
                            .fontWeight(.black)
                            .foregroundColor(queuedCount >= 50 ? themeVM.colorScheme.destructive : themeVM.colorScheme.primary)
                        Image(systemName: "trash")
                            .font(.caption)
                            .foregroundColor(queuedCount >= 50 ? themeVM.colorScheme.destructive : themeVM.colorScheme.primary)
                    }
                    .padding(.horizontal, AppPadding.sm)
                    .padding(.vertical, AppPadding.xs)
                    .background(
                        Circle()
                            .fill((queuedCount >= 50 ? themeVM.colorScheme.destructive : themeVM.colorScheme.primary).opacity(0.14))
                    )
                }
                
                let progress = CGFloat(queuedCount) / CGFloat(DeleteQueue.maxSize)
                RoundedRectangle(cornerRadius: 3)
                    .fill(themeVM.colorScheme.surfaceOverlay)
                    .frame(height: 5)
                    .overlay(
                        RoundedRectangle(cornerRadius: 3)
                            .fill(progress >= 1 ? themeVM.colorScheme.destructive : themeVM.colorScheme.primary)
                            .scaleEffect(x: progress, y: 1, anchor: .leading)
                    )
                
                HStack(spacing: AppPadding.sm) {
                    Button(action: onUndo) {
                        HStack {
                            Image(systemName: "arrow.uturn.backward")
                            Text("撤销")
                                .fontWeight(.bold)
                        }
                        .foregroundColor(canUndo ? themeVM.colorScheme.textPrimary : themeVM.colorScheme.textDisabled)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(themeVM.colorScheme.surfaceElevated)
                        )
                    }
                    .disabled(!canUndo)
                    
                    Button(action: onConfirmDelete) {
                        HStack {
                            Image(systemName: "trash")
                            Text("确认删除")
                                .fontWeight(.bold)
                        }
                        .foregroundColor(queuedCount > 0 ? .white : themeVM.colorScheme.textDisabled)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(queuedCount > 0 ? themeVM.colorScheme.destructive : themeVM.colorScheme.surfaceOverlay)
                        )
                    }
                    .disabled(queuedCount == 0)
                }
            }
        }
    }
}
