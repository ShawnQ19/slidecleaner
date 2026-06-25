import SwiftUI

@MainActor
class ThemeViewModel: ObservableObject {
    @Published var preset: ThemePreset = .oceanBlue
    @Published var customColor: Color?
    
    private let presetKey = "theme_preset"
    private let customColorKey = "theme_custom_color"
    
    var colorScheme: AppColorScheme {
        appColorScheme(primary: activeColor)
    }
    
    var activeColor: Color {
        customColor ?? preset.color
    }
    
    init() {
        load()
    }
    
    func selectPreset(_ preset: ThemePreset) {
        self.preset = preset
        self.customColor = nil
        save()
    }
    
    func setCustomColor(_ color: Color) {
        self.customColor = color
        save()
    }
    
    private func load() {
        if let name = UserDefaults.standard.string(forKey: presetKey),
           let p = ThemePreset(rawValue: name) {
            preset = p
        }
    }
    
    private func save() {
        UserDefaults.standard.set(preset.rawValue, forKey: presetKey)
    }
}
