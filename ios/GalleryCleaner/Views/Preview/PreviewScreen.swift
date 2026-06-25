import SwiftUI
import Photos
import AVKit

struct PreviewScreen: View {
    let mediaItem: MediaItem
    @EnvironmentObject var themeVM: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showInfo = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if mediaItem.isVideo {
                VideoPreviewView(asset: mediaItem.asset)
            } else if mediaItem.isLivePhoto {
                LivePhotoPreviewView(asset: mediaItem.asset)
            } else {
                PhotoPreviewView(asset: mediaItem.asset)
            }
            
            VStack {
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .font(.title3)
                            .foregroundColor(.white)
                            .padding(10)
                            .background(Circle().fill(.ultraThinMaterial))
                    }
                    Spacer()
                    Button(action: { showInfo.toggle() }) {
                        Image(systemName: "info.circle")
                            .font(.title3)
                            .foregroundColor(.white)
                            .padding(10)
                            .background(Circle().fill(.ultraThinMaterial))
                    }
                }
                .padding(.horizontal, AppPadding.lg)
                .padding(.top, AppPadding.md)
                Spacer()
                
                if showInfo {
                    infoBar
                }
            }
        }
        .navigationBarHidden(true)
    }
    
    private var infoBar: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: AppPadding.xs) {
                Text(mediaItem.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .lineLimit(1)
                HStack {
                    Text(formatSize(mediaItem.size))
                    Text("·")
                    Text("\(mediaItem.width)×\(mediaItem.height)")
                    Text("·")
                    Text(mediaItem.isVideo ? "VIDEO" : (mediaItem.isLivePhoto ? "LIVE" : "PHOTO"))
                }
                .font(.caption)
                .foregroundColor(.white.opacity(0.6))
                
                if let date = mediaItem.dateAdded as? Date {
                    Text(date.formatted(date: .long, time: .shortened))
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.4))
                }
            }
        }
        .padding(.horizontal, AppPadding.lg)
        .padding(.bottom, AppPadding.xl)
    }
    
    private func formatSize(_ bytes: Int64) -> String {
        if bytes < 1024 { return "\(bytes) B" }
        if bytes < 1024 * 1024 { return String(format: "%.1f KB", Double(bytes) / 1024) }
        if bytes < 1024 * 1024 * 1024 { return String(format: "%.1f MB", Double(bytes) / 1024 / 1024) }
        return String(format: "%.1f GB", Double(bytes) / 1024 / 1024 / 1024)
    }
}

struct PhotoPreviewView: View {
    let asset: PHAsset
    @State private var image: UIImage?
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    
    var body: some View {
        GeometryReader { geo in
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .scaleEffect(scale)
                    .gesture(
                        MagnificationGesture()
                            .onChanged { value in
                                scale = lastScale * value
                            }
                            .onEnded { _ in
                                lastScale = scale
                                if scale < 1 { withAnimation { scale = 1; lastScale = 1 } }
                                if scale > 5 { withAnimation { scale = 5; lastScale = 5 } }
                            }
                    )
                    .onTapGesture(count: 2) {
                        withAnimation {
                            scale = scale > 1 ? 1 : 2
                            lastScale = scale
                        }
                    }
            } else {
                ProgressView()
                    .tint(.white)
                    .onAppear { loadFullImage() }
            }
        }
    }
    
    private func loadFullImage() {
        let screen = UIScreen.main.bounds
        let targetSize = CGSize(width: screen.width * 3, height: screen.height * 3)
        let options = PHImageRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true
        
        PHImageManager.default().requestImage(
            for: asset, targetSize: targetSize,
            contentMode: .aspectFit, options: options
        ) { result, _ in self.image = result }
    }
}

struct VideoPreviewView: View {
    let asset: PHAsset
    @State private var player: AVPlayer?
    @State private var isLongPressing = false
    
    var body: some View {
        ZStack {
            if let player = player {
                VideoPlayer(player: player)
                    .onAppear {
                        player.play()
                        player.actionAtItemEnd = .none
                        NotificationCenter.default.addObserver(
                            forName: .AVPlayerItemDidPlayToEndTime,
                            object: player.currentItem, queue: .main
                        ) { _ in player.seek(to: .zero); player.play() }
                    }
            } else {
                ProgressView()
                    .tint(.white)
                    .onAppear { loadVideo() }
            }
            
            if isLongPressing {
                Text("2x")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(6)
                    .background(Capsule().fill(Color.black.opacity(0.6)))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .padding(AppPadding.lg)
            }
        }
        .onLongPressGesture(
            minimumDuration: .infinity,
            pressing: { pressing in
                isLongPressing = pressing
                player?.rate = pressing ? 2.0 : 1.0
            },
            perform: {}
        )
    }
    
    private func loadVideo() {
        let options = PHVideoRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true
        
        PHImageManager.default().requestAVAsset(
            forVideo: asset, options: options
        ) { avAsset, _, _ in
            guard let avAsset = avAsset else { return }
            DispatchQueue.main.async {
                self.player = AVPlayer(playerItem: AVPlayerItem(asset: avAsset))
            }
        }
    }
}

struct LivePhotoPreviewView: View {
    let asset: PHAsset
    @State private var livePhoto: PHLivePhoto?
    
    var body: some View {
        ZStack {
            if let livePhoto = livePhoto {
                LivePhotoView(livePhoto: livePhoto)
            } else {
                ProgressView()
                    .tint(.white)
                    .onAppear { loadLivePhoto() }
            }
        }
    }
    
    private func loadLivePhoto() {
        let options = PHLivePhotoRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true
        
        PHImageManager.default().requestLivePhoto(
            for: asset, targetSize: PHImageManagerMaximumSize,
            contentMode: .default, options: options
        ) { result, _ in self.livePhoto = result }
    }
}

struct LivePhotoView: UIViewRepresentable {
    let livePhoto: PHLivePhoto
    
    func makeUIView(context: Context) -> PHLivePhotoView {
        let view = PHLivePhotoView()
        view.livePhoto = livePhoto
        view.contentMode = .scaleAspectFit
        view.startPlayback(with: .hint)
        return view
    }
    
    func updateUIView(_ uiView: PHLivePhotoView, context: Context) {
        uiView.livePhoto = livePhoto
    }
}
