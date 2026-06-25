import SwiftUI

enum ThemePreset: String, CaseIterable, Identifiable {
    case oceanBlue = "海洋蓝"
    case emerald = "翡翠绿"
    case amber = "琥珀橙"
    case rose = "玫瑰红"
    case lavender = "薰衣紫"
    case cyan = "青碧"
    case sunset = "落日金"
    case mint = "薄荷绿"
    
    var id: String { rawValue }
    
    var color: Color {
        switch self {
        case .oceanBlue: return Color(red: 0.04, green: 0.52, blue: 1.0)
        case .emerald: return Color(red: 0.19, green: 0.82, blue: 0.35)
        case .amber: return Color(red: 1.0, green: 0.62, blue: 0.04)
        case .rose: return Color(red: 1.0, green: 0.22, blue: 0.37)
        case .lavender: return Color(red: 0.75, green: 0.35, blue: 0.95)
        case .cyan: return Color(red: 0.39, green: 0.82, blue: 1.0)
        case .sunset: return Color(red: 1.0, green: 0.84, blue: 0.04)
        case .mint: return Color(red: 0.40, green: 0.83, blue: 0.81)
        }
    }
}

struct AppColorScheme {
    let primary: Color
    let accent: Color
    let background: Color
    let surface: Color
    let surfaceElevated: Color
    let surfaceOverlay: Color
    let textPrimary: Color
    let textSecondary: Color
    let textTertiary: Color
    let textDisabled: Color
    let destructive: Color
    let success: Color
    let separator: Color
}

func appColorScheme(primary: Color) -> AppColorScheme {
    AppColorScheme(
        primary: primary,
        accent: Color(red: 0.39, green: 0.82, blue: 1.0),
        background: Color(red: 0.05, green: 0.06, blue: 0.10),
        surface: Color(red: 0.09, green: 0.11, blue: 0.17),
        surfaceElevated: Color(red: 0.13, green: 0.15, blue: 0.22),
        surfaceOverlay: Color.white.opacity(0.08),
        textPrimary: Color.white,
        textSecondary: Color.white.opacity(0.65),
        textTertiary: Color.white.opacity(0.40),
        textDisabled: Color.white.opacity(0.25),
        destructive: Color(red: 1.0, green: 0.23, blue: 0.19),
        success: Color(red: 0.19, green: 0.82, blue: 0.35),
        separator: Color.white.opacity(0.10)
    )
}

struct AppShape {
    static let small = RoundedRectangle(cornerRadius: 8)
    static let medium = RoundedRectangle(cornerRadius: 12)
    static let large = RoundedRectangle(cornerRadius: 16)
    static let xLarge = RoundedRectangle(cornerRadius: 20)
    static let pill = Capsule()
}

struct AppPadding {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 24
}
