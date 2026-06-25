import SwiftUI

struct ThemeSettingsScreen: View {
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            themeVM.colorScheme.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                topBar
                
                ScrollView {
                    VStack(spacing: AppPadding.xl) {
                        previewSection
                        presetsSection
                    }
                    .padding(.top, AppPadding.md)
                    .padding(.bottom, AppPadding.xl)
                }
            }
        }
        .navigationBarHidden(true)
    }
    
    private var topBar: some View {
        GlassTopBar {
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(themeVM.colorScheme.textPrimary)
                }
                Text("主题设置")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                Spacer()
            }
            .padding(.horizontal, AppPadding.lg)
            .padding(.vertical, AppPadding.sm)
        }
    }
    
    private var previewSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: AppPadding.md) {
                Text("预览")
                    .font(.headline)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                
                HStack(spacing: AppPadding.md) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(themeVM.activeColor)
                        .frame(height: 48)
                    
                    Button("示例按钮") {}
                        .buttonStyle(.borderedProminent)
                        .tint(themeVM.activeColor)
                }
                
                HStack(spacing: AppPadding.sm) {
                    ForEach([0.15, 0.3, 0.5, 0.8, 1.0], id: \.self) { alpha in
                        RoundedRectangle(cornerRadius: 6)
                            .fill(themeVM.activeColor.opacity(alpha))
                            .frame(height: 24)
                    }
                }
            }
        }
        .padding(.horizontal, AppPadding.lg)
    }
    
    private var presetsSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: AppPadding.md) {
                Text("主题色")
                    .font(.headline)
                    .foregroundColor(themeVM.colorScheme.textPrimary)
                
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: AppPadding.md) {
                    ForEach(ThemePreset.allCases) { preset in
                        Button(action: { themeVM.selectPreset(preset) }) {
                            VStack(spacing: 6) {
                                Circle()
                                    .fill(preset.color)
                                    .frame(width: 44, height: 44)
                                    .overlay(
                                        Circle()
                                            .stroke(Color.white, lineWidth: themeVM.preset == preset ? 3 : 0)
                                    )
                                Text(preset.rawValue)
                                    .font(.caption2)
                                    .foregroundColor(themeVM.colorScheme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, AppPadding.lg)
    }
}
