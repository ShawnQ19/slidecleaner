import SwiftUI
import Photos

struct GalleryScreen: View {
    @StateObject private var viewModel = GalleryViewModel()
    @EnvironmentObject var themeVM: ThemeViewModel
    @State private var permissionStatus: PHAuthorizationStatus = .notDetermined
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            switch true {
            case viewModel.isInitialLoading:
                loadingView
            case viewModel.error != nil:
                errorView
            case viewModel.groups.isEmpty && !viewModel.isLoading:
                emptyView
            default:
                mainContent
            }
            
            topBar
        }
        .onAppear { checkPermission() }
    }
    
    private var loadingView: some View {
        VStack(spacing: AppPadding.md) {
            ProgressView()
                .tint(themeVM.colorScheme.primary)
            Text("正在扫描相册...")
                .font(.body)
                .foregroundColor(themeVM.colorScheme.textSecondary)
        }
    }
    
    private var errorView: some View {
        VStack(spacing: AppPadding.md) {
            Text("加载失败")
                .font(.title3)
                .foregroundColor(themeVM.colorScheme.destructive)
            Text(viewModel.error ?? "")
                .font(.body)
                .foregroundColor(themeVM.colorScheme.textSecondary)
            Button("重试") { viewModel.retry() }
                .buttonStyle(.borderedProminent)
        }
        .padding(32)
    }
    
    private var emptyView: some View {
        VStack(spacing: AppPadding.md) {
            Text("没有找到照片或视频")
                .font(.body)
                .foregroundColor(themeVM.colorScheme.textSecondary)
            Button("刷新") { viewModel.retry() }
                .buttonStyle(.borderedProminent)
        }
        .padding(32)
    }
    
    private var mainContent: some View {
        ScrollView {
            LazyVStack(spacing: AppPadding.lg) {
                HeroPanel(
                    totalCount: viewModel.totalCount,
                    monthCount: viewModel.monthCount,
                    averagePerMonth: viewModel.averagePerMonth
                )
                .padding(.horizontal, AppPadding.lg)
                
                ForEach(viewModel.groups) { group in
                    MonthCard(group: group)
                        .padding(.horizontal, AppPadding.lg)
                }
            }
            .padding(.top, 80)
            .padding(.bottom, AppPadding.xl)
        }
    }
    
    private var topBar: some View {
        VStack {
            GlassTopBar {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("相册清理")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                        if !viewModel.groups.isEmpty {
                            Text("\(viewModel.groups.count) 个月份")
                                .font(.caption)
                                .foregroundColor(themeVM.colorScheme.textSecondary)
                        }
                    }
                    Spacer()
                    Text("\(viewModel.totalCount) 项")
                        .font(.caption)
                        .foregroundColor(themeVM.colorScheme.textTertiary)
                }
                .padding(.horizontal, AppPadding.lg)
                .padding(.vertical, AppPadding.sm)
            }
            Spacer()
        }
    }
    
    private func checkPermission() {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        permissionStatus = status
        if status == .authorized || status == .limited {
            viewModel.onPermissionGranted()
        } else if status == .notDetermined {
            Task {
                let newStatus = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
                permissionStatus = newStatus
                if newStatus == .authorized || newStatus == .limited {
                    viewModel.onPermissionGranted()
                }
            }
        }
    }
}

struct HeroPanel: View {
    let totalCount: Int
    let monthCount: Int
    let averagePerMonth: Int
    @EnvironmentObject var themeVM: ThemeViewModel
    
    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: AppPadding.md) {
                HStack {
                    Text("SCENE 01 · DEEP BLUE")
                        .font(.caption2)
                        .foregroundColor(themeVM.colorScheme.textTertiary)
                        .fontWeight(.semibold)
                    Spacer()
                    Text(totalCount > 0 ? "READY" : "WAITING")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(themeVM.colorScheme.primary)
                        .padding(.horizontal, AppPadding.sm)
                        .padding(.vertical, AppPadding.xs)
                        .background(
                            Capsule()
                                .fill(themeVM.colorScheme.primary.opacity(0.14))
                        )
                }
                
                Text("相册清理")
                    .font(.title)
                    .fontWeight(.black)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                
                Text("按月浏览设备媒体 · 批量加入删除队列 · 系统回收站保护")
                    .font(.body)
                    .foregroundColor(themeVM.colorScheme.textSecondary)
                
                RoundedRectangle(cornerRadius: 3)
                    .fill(themeVM.colorScheme.surfaceOverlay)
                    .frame(height: 6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 3)
                            .fill(themeVM.colorScheme.primary)
                            .frame(width: 120, height: 6),
                        alignment: .leading
                    )
                
                HStack(spacing: AppPadding.sm) {
                    StatTile(label: "总项目", value: "\(totalCount)")
                    StatTile(label: "月份", value: "\(monthCount)")
                    StatTile(label: "月均", value: "\(averagePerMonth)")
                }
                
                Text("随机从全局相册抽取，处理后会进入已整理，从普通月份相册自动消失")
                    .font(.caption)
                    .foregroundColor(themeVM.colorScheme.textTertiary)
            }
        }
    }
}

struct StatTile: View {
    let label: String
    let value: String
    @EnvironmentObject var themeVM: ThemeViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.title3)
                .fontWeight(.black)
                .foregroundColor(themeVM.colorScheme.textPrimary)
            Text(label)
                .font(.caption2)
                .foregroundColor(themeVM.colorScheme.textTertiary)
                .fontWeight(.semibold)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, AppPadding.md)
        .padding(.vertical, AppPadding.sm)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(themeVM.colorScheme.surfaceElevated.opacity(0.78))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(themeVM.colorScheme.separator, lineWidth: 1)
                )
        )
    }
}

struct MonthCard: View {
    let group: MediaGroup
    @EnvironmentObject var themeVM: ThemeViewModel
    
    private let columns = 3
    private let spacing: CGFloat = 6
    
    var body: some View {
        GlassCard(padding: 0) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(group.label)
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(themeVM.colorScheme.textPrimary)
                        Text("\(group.count) 个项目")
                            .font(.caption)
                            .foregroundColor(themeVM.colorScheme.textSecondary)
                    }
                    Spacer()
                    Text("❯")
                        .font(.title3)
                        .foregroundColor(themeVM.colorScheme.primary)
                }
                .padding(.horizontal, AppPadding.lg)
                .padding(.vertical, AppPadding.md)
                
                let previewItems = Array(group.mediaItems.prefix(columns * 2))
                if !previewItems.isEmpty {
                    let rows = previewItems.chunked(into: columns)
                    VStack(spacing: spacing) {
                        ForEach(rows.indices, id: \.self) { rowIdx in
                            HStack(spacing: spacing) {
                                ForEach(rows[rowIdx]) { item in
                                    MediaThumbnail(mediaItem: item, size: 80)
                                        .frame(maxWidth: .infinity)
                                        .aspectRatio(1, contentMode: .fill)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                // Fill remaining space
                                ForEach(0..<(columns - rows[rowIdx].count), id: \.self) { _ in
                                    Spacer()
                                        .frame(maxWidth: .infinity)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, AppPadding.md)
                    .padding(.bottom, AppPadding.md)
                }
                
                let remaining = group.count - min(columns * 2, group.count)
                if remaining > 0 {
                    Text("+\(remaining)")
                        .font(.caption)
                        .foregroundColor(themeVM.colorScheme.textTertiary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(themeVM.colorScheme.surfaceOverlay)
                        )
                        .padding(.horizontal, AppPadding.md)
                        .padding(.bottom, AppPadding.md)
                }
            }
        }
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
