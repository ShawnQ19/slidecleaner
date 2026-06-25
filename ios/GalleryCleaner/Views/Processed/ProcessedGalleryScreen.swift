import SwiftUI
import Photos

struct ProcessedGalleryScreen: View {
    @StateObject private var viewModel = ProcessedViewModel()
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                topBar
                
                if viewModel.isLoading {
                    Spacer()
                    ProgressView().tint(themeVM.colorScheme.primary)
                    Spacer()
                } else if viewModel.groups.isEmpty {
                    Spacer()
                    Text("没有已整理的项目")
                        .foregroundColor(themeVM.colorScheme.textSecondary)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVStack(spacing: AppPadding.lg) {
                            ForEach(viewModel.groups) { group in
                                MonthCard(group: group)
                                    .padding(.horizontal, AppPadding.lg)
                            }
                        }
                        .padding(.top, AppPadding.md)
                        .padding(.bottom, AppPadding.xl)
                    }
                }
            }
            
            if viewModel.showResetConfirm {
                GlassDialog(
                    title: "重置已整理",
                    message: "确定要重置所有已整理的项目吗？它们将重新出现在相册中。",
                    confirmText: "重置",
                    isDestructive: true,
                    onConfirm: {
                        viewModel.showResetConfirm = false
                        viewModel.resetAll()
                    },
                    onDismiss: { viewModel.showResetConfirm = false }
                )
            }
        }
        .navigationBarHidden(true)
        .onAppear { viewModel.load() }
    }
    
    private var topBar: some View {
        GlassTopBar {
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(themeVM.colorScheme.textPrimary)
                }
                Text("已整理")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                Spacer()
                Button(action: { viewModel.showResetConfirm = true }) {
                    Text("重置")
                        .font(.subheadline)
                        .foregroundColor(themeVM.colorScheme.destructive)
                }
            }
            .padding(.horizontal, AppPadding.lg)
            .padding(.vertical, AppPadding.sm)
        }
    }
}

@MainActor
class ProcessedViewModel: ObservableObject {
    @Published var groups: [MediaGroup] = []
    @Published var isLoading = false
    @Published var showResetConfirm = false
    
    private let photoService = PhotoLibraryService()
    private let processedStore = ProcessedStore()
    
    func load() {
        isLoading = true
        Task {
            let processedIds = await processedStore.getProcessedIds()
            do {
                let allGroups = try await photoService.fetchMediaGroups(excluding: [])
                groups = allGroups.map { group in
                    MediaGroup(
                        id: group.id,
                        yearMonth: group.yearMonth,
                        label: group.label,
                        mediaItems: group.mediaItems.filter { processedIds.contains($0.id) }
                    )
                }.filter { !$0.mediaItems.isEmpty }
            } catch {}
            isLoading = false
        }
    }
    
    func resetAll() {
        Task {
            await processedStore.resetAll()
            groups = []
        }
    }
}
