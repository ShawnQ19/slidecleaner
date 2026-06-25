import SwiftUI
import Photos

struct MediaThumbnail: View {
    let mediaItem: MediaItem
    var size: CGFloat = 120
    
    @State private var image: UIImage?
    
    var body: some View {
        ZStack {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                Rectangle()
                    .fill(Color.white.opacity(0.08))
                    .onAppear { loadThumbnail() }
            }
            
            if mediaItem.isVideo {
                Image(systemName: "play.fill")
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(6)
                    .background(Circle().fill(.ultraThinMaterial))
            }
            
            if mediaItem.isLivePhoto {
                Image(systemName: "livephoto")
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(4)
                    .background(Circle().fill(.ultraThinMaterial))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .padding(4)
            }
        }
        .clipped()
    }
    
    private func loadThumbnail() {
        let scale = UIScreen.main.scale
        let targetSize = CGSize(width: size * scale, height: size * scale)
        
        PHImageManager.default().requestImage(
            for: mediaItem.asset,
            targetSize: targetSize,
            contentMode: .aspectFill,
            options: thumbnailOptions()
        ) { result, _ in
            self.image = result
        }
    }
    
    private func thumbnailOptions() -> PHImageRequestOptions {
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.isNetworkAccessAllowed = true
        options.resizeMode = .fast
        return options
    }
}
