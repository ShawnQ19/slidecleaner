# Android 照片/视频清理画廊应用 - 实施计划

## 摘要

开发一个 Android 端照片/视频批量管理 App，核心功能为按月浏览设备媒体文件，通过滑动手势快速将不需要的文件加入删除队列，最终安全移入系统回收站（30 天自动清理）。支持视频长按加速播放和 Live 图（Motion Photo）播放。

**技术栈**：Kotlin + Jetpack Compose + Material 3 + Hilt + Coil 3 + Media3 ExoPlayer + MVVM Clean Architecture

---

## 当前状态分析

这是一个全新项目，从零开始搭建。

---

## 实施方案

### 阶段一：项目初始化与基础框架

#### 1.1 创建项目 & Gradle 配置

**文件**：`app/build.gradle.kts`

- `compileSdk = 35`，`minSdk = 26`，`targetSdk = 35`
- 启用 Compose，Kotlin JVM Target = 17
- 核心依赖：
  - Compose BOM 2024.12.01（Material 3、Foundation、UI）
  - Hilt 2.53.1（依赖注入）
  - Coil 3.4.0（图片加载）
  - Media3 ExoPlayer 1.5.1（视频播放 + Motion Photo 支持）
  - ExifInterface（XMP 元数据解析，用于 Motion Photo 检测）
  - Navigation Compose 2.8.5
  - Paging 3 3.3.5
  - Lifecycle ViewModel Compose 2.8.7

#### 1.2 AndroidManifest.xml 权限配置

**文件**：`app/src/main/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

#### 1.3 Application 入口 & MainActivity

**文件**：
- `GalleryApp.kt` — `@HiltAndroidApp` 注解
- `MainActivity.kt` — `@AndroidEntryPoint`，设置 `setContent { GalleryCleanerTheme { NavHost(...) } }`

#### 1.4 Material 3 主题

**文件**：`ui/theme/Theme.kt`、`Color.kt`、`Type.kt`

- 浅色/深色主题支持
- 主色调使用蓝色系（清理工具风格）

#### 1.5 导航图

**文件**：`ui/navigation/NavGraph.kt`

三条路由：
- `gallery` → 月份浏览主屏
- `media_swipe/{yearMonth}` → 媒体滑动浏览屏
- `preview/{mediaId}` → 全屏预览屏

#### 1.6 权限请求组件

**文件**：`ui/permission/PermissionHandler.kt`

- API 33+：请求 `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`
- API 32 及以下：请求 `READ_EXTERNAL_STORAGE`
- 未授权时显示权限说明页面，引导用户前往设置

---

### 阶段二：数据层实现

#### 2.1 领域模型

**文件**：`domain/model/MediaItem.kt`、`MediaGroup.kt`、`DeleteQueue.kt`、`LivePhotoType.kt`、`LivePhotoInfo.kt`

```kotlin
enum class LivePhotoType {
    NONE,           // 不是动态照片
    GOOGLE_PIXEL,   // Google Pixel Motion Photo (单文件 XMP)
    XIAOMI,         // 小米 MicroVideo (单文件 XMP)
    OPPO,           // OPPO O Live Photo (单文件 XMP)
    VIVO,           // vivo 动态照片 (双文件: JPG+MP4)
    HUAWEI,         // 华为/荣耀动态照片 (单文件, 文件末尾标识)
    APPLE           // 苹果 Live Photo (双文件: HEIC/JPG+MOV)
}

data class LivePhotoInfo(
    val type: LivePhotoType = LivePhotoType.NONE,
    val videoOffset: Long = 0,           // 视频起始偏移量（单文件格式）
    val videoLength: Long = 0,           // 视频长度（字节）
    val pairedVideoUri: Uri? = null      // 配对视频文件 URI（vivo/Apple 双文件格式）
)

data class MediaItem(
    val id: Long, val uri: Uri, val name: String,
    val mimeType: String, val dateAdded: Long, val size: Long,
    val duration: Long? = null, val width: Int, val height: Int,
    val livePhotoInfo: LivePhotoInfo = LivePhotoInfo(),  // Live 图详细信息
    val isQueuedForDelete: Boolean = false
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isLivePhoto: Boolean get() = livePhotoInfo.type != LivePhotoType.NONE && !isVideo
}

data class MediaGroup(
    val yearMonth: YearMonth, val label: String,
    val mediaItems: List<MediaItem>
)

data class DeleteQueue(
    val items: List<MediaItem>, val currentMonth: YearMonth,
    val monthTotalCount: Int
) {
    companion object { const val MAX_QUEUE_SIZE = 50 }
    val shouldPromptDelete: Boolean
        get() = items.size >= MAX_QUEUE_SIZE || items.size >= monthTotalCount
}
```

#### 2.2 MediaStore 数据源

**文件**：`data/source/MediaStoreDataSource.kt`

- 使用 `MediaStore.Files.getContentUri("external")` 一次查询图片和视频
- 过滤条件：`MEDIA_TYPE IN (IMAGE, VIDEO) AND IS_TRASHED != 1`
- 按 `DATE_ADDED DESC` 排序
- 在 `Dispatchers.IO` 上执行查询，返回 `Flow<List<MediaItem>>`
- **多厂商 Live 图检测**：使用 `LivePhotoDetector` 工具类分层检测各厂商格式
  - **XMP 元数据检测**（优先级 1）：检查 `GCamera:MotionPhoto`（Google/OPPO）、`GCamera:MicroVideo`（小米）、`OpCamera:MotionPhotoOwner`（OPPO）
  - **文件末尾标识检测**（优先级 2）：检查 `vivo{` JSON 字符串（vivo）、`LIVE_` 标识（华为/荣耀）
  - **配对视频文件检测**（优先级 3）：检查同目录同名 `.mp4`（vivo）或 `.mov`（Apple）文件
  - **视频签名回退检测**（优先级 4）：搜索文件内 `ftyp` 或 `moov` 签名
  - 检测结果存入 `MediaItem.livePhotoInfo` 字段，包含类型、偏移量、视频长度、配对文件 URI

#### 2.3 Repository 实现

**文件**：
- `domain/repository/MediaRepository.kt` — 接口定义
- `data/repository/MediaRepositoryImpl.kt` — 实现

接口方法：
- `getMediaGroups(): Flow<List<MediaGroup>>` — 查询并按月分组
- `moveToTrash(uris: List<Uri>): TrashRequestResult` — API 30+ 移入回收站
- `deletePermanently(uris: List<Uri>): DeleteResult` — API 26-29 直接删除
- `isTrashSupported(): Boolean` — 检查回收站支持

#### 2.4 Hilt 依赖注入模块

**文件**：`di/DataModule.kt`

- 绑定 `MediaStoreDataSource`、`MediaRepository`、`UseCase` 等

---

### 阶段三：月份浏览主屏

#### 3.1 UseCase

**文件**：`domain/usecase/GetMediaGroupsUseCase.kt`

- 调用 `MediaRepository.getMediaGroups()` 返回按月分组数据

#### 3.2 ViewModel & UI State

**文件**：`ui/screen/gallery/GalleryViewModel.kt`、`GalleryUiState.kt`

```kotlin
data class GalleryUiState(
    val groups: List<MediaGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

#### 3.3 GalleryScreen 界面

**文件**：`ui/screen/gallery/GalleryScreen.kt`

布局：
```
TopAppBar: "相册清理"
LazyColumn:
  └─ MonthCard (点击进入月份详情)
       ├─ 月份标题: "2025年3月" + "128张"
       └─ 缩略图网格 (3列，最多6张预览 + "+122" 遮罩)
```

#### 3.4 通用组件

**文件**：
- `ui/component/MediaThumbnail.kt` — Coil 3 `AsyncImage` 封装
- `ui/component/VideoIndicator.kt` — 视频时长角标
- `ui/screen/gallery/MonthCard.kt` — 月份卡片

---

### 阶段四：媒体滑动浏览（核心屏幕）

#### 4.1 ViewModel & UI State

**文件**：`ui/screen/media/MediaSwipeViewModel.kt`、`MediaSwipeUiState.kt`

```kotlin
data class MediaSwipeUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val deleteQueue: DeleteQueue = DeleteQueue(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val trashRequestResult: TrashRequestResult? = null
)
```

ViewModel 核心逻辑：
- `queueForDelete(item)` — 将媒体项加入删除队列（`MutableSet` 去重）
- 每次加入后检查 `DeleteQueue.shouldPromptDelete`，满足条件则弹出确认弹窗
- `confirmDelete()` — 调用 `DeleteMediaUseCase` 获取 `PendingIntent`
- `onDeleteSuccess()` — 从列表移除已删除项，清空队列

#### 4.2 SwipeableMediaCard — 可滑动媒体卡片

**文件**：`ui/screen/media/SwipeableMediaCard.kt`

**手势方案**：
- 使用 `AnchoredDraggableState`（Material 3）处理垂直上滑
- 锚点：`Center at 0f`、`SwipedUp at -400f`
- 到达 `SwipedUp` 锚点后触发 `onQueueForDelete`，动画回弹到 `Center`
- 中心区域点击检测：`detectTapGestures` 判断点击位置在中间 60% 区域内

**视觉反馈**：
- 上滑过程中，背景层渐显红色删除提示（"松开加入删除队列" + 删除图标）
- 前景内容层随手指上移

#### 4.3 MediaSwipeScreen — 媒体滑动浏览界面

**文件**：`ui/screen/media/MediaSwipeScreen.kt`

布局：
```
TopAppBar: "← 2025年3月" + "已选 23/50"
HorizontalPager (全屏):
  └─ SwipeableMediaCard
DeleteQueueBar (底部):
  ├─ "已选 23 项" + 进度条
  └─ "确认删除" 按钮
```

**手势冲突处理**：
- `HorizontalPager` 自动处理水平滑动（左划下一张、右划上一张）
- `anchoredDraggable` 处理垂直上滑
- Compose 手势系统按方向自动分发，冲突时通过 `NestedScrollConnection` 控制优先级

#### 4.4 DeleteQueueBar — 删除队列底部栏

**文件**：`ui/screen/media/DeleteQueueBar.kt`

- 显示已选数量 / 50 上限
- 进度条可视化
- 达到阈值时按钮高亮提示

---

### 阶段五：全屏预览（含视频加速 & Live 图播放）

#### 5.1 PreviewScreen

**文件**：`ui/screen/preview/PreviewScreen.kt`

根据媒体类型分发不同预览组件：
- **普通图片**：Coil 3 `AsyncImage` 高清加载，支持双指缩放（`detectTransformGestures`）
- **普通视频**：`VideoPlayer` 组件播放，支持长按加速
- **Live 图 (Motion Photo)**：`MotionPhotoPlayer` 组件，默认显示静态图，长按播放嵌入视频
- 顶部渐变遮罩 + 返回按钮
- 底部信息栏：文件名 + 大小 + 日期

#### 5.2 VideoPlayer 组件（支持长按加速）

**文件**：`ui/screen/preview/VideoPlayer.kt`

- 使用 `AndroidView` 包装 `PlayerView`
- `DisposableEffect` 中释放 ExoPlayer 实例
- 页面切换时自动暂停/释放

**长按加速实现方案**：

手势检测：使用 `awaitEachGesture` + `awaitLongPressOrCancellation` 检测长按

```kotlin
// 核心逻辑
Modifier.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id)
        if (longPress != null) {
            // 长按触发：加速到 2x
            isSpeedingUp = true
            exoPlayer.setPlaybackSpeed(2.0f)
            // 等待手指抬起
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.changedToUp() }) break
            }
            // 抬起恢复 1x
            isSpeedingUp = false
            exoPlayer.setPlaybackSpeed(1.0f)
        }
    }
}
```

速度指示器覆盖层（`SpeedIndicatorOverlay`）：
- 长按期间居中显示半透明卡片：快进图标 + "2x" + "松开恢复正常"
- 使用 `AnimatedVisibility` + `fadeIn/scaleIn` 动画

渐进式变速（可选优化）：
- 使用 `Animatable` 实现速度平滑过渡（200ms `FastOutSlowInEasing`），避免速度突变

#### 5.3 MotionPhotoPlayer 组件（多厂商 Live 图播放）

**文件**：`ui/screen/preview/MotionPhotoPlayer.kt`

**多厂商格式支持**：

| 厂商 | 播放方案 |
|------|----------|
| Google Pixel / 小米 / OPPO | Media3 直接播放 JPEG URI（内置 `JpegMotionPhotoExtractor`）或手动提取后播放 |
| vivo | 播放同目录同名 `.mp4` 文件（`livePhotoInfo.pairedVideoUri`） |
| 华为/荣耀 | 手动提取嵌入式视频后播放（`LivePhotoExtractor.extractHuaweiVideo`） |
| Apple | 播放同目录同名 `.mov` 文件（`livePhotoInfo.pairedVideoUri`） |

**UX 交互**：
- 默认显示静态图片（Coil `AsyncImage`）
- **长按播放**：按下时根据 `livePhotoInfo.type` 选择播放方案，松开时停止并回到静态图
- 右下角显示对应厂商 Live 图标识图标（静态时可见）
- 播放时隐藏标识图标，显示 "Live" 文字标签

```kotlin
// 核心逻辑
Box {
    // 静态图片层（始终存在）
    AsyncImage(model = imageUri, ...)

    // 视频层（长按时显示）
    if (isPlaying) {
        AndroidView(factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer  // 直接播放 Motion Photo JPEG URI
                useController = false
            }
        })
    }

    // Live 图标识（根据厂商显示不同图标）
    if (!isPlaying) {
        LivePhotoBadge(type = mediaItem.livePhotoInfo.type)
    }
}
```

**手势实现**：使用 `detectTapGestures(onPress = { ... tryAwaitRelease() ... })`
- `onPress` 按下时：`exoPlayer.seekToDefaultPosition()` + `exoPlayer.play()`
- `tryAwaitRelease()` 释放时：`exoPlayer.stop()`

**兼容性处理**：
- 优先使用 Media3 直接播放（需 Media3 1.5.0+）
- 如果 Media3 播放失败（部分非标准格式设备），回退到手动提取方案：
  1. 通过 XMP 元数据获取 `MotionPhotoOffset`（视频在文件中的字节偏移）
  2. 使用 `RandomAccessFile` 从偏移位置读取到文件末尾，保存为临时 MP4 文件
  3. 用 ExoPlayer 播放临时文件
- 手动提取方案需额外依赖：`androidx.exifinterface:exifinterface`（用于 XMP 解析）

#### 5.4 LivePhotoDetector 工具类（多厂商检测）

**文件**：`util/LivePhotoDetector.kt`

**分层检测策略**：

```kotlin
object LivePhotoDetector {
    // XMP 命名空间常量
    private const val NS_GCAMERA = "http://ns.google.com/photos/1.0/camera/"
    private const val NS_OPCAMERA = "http://ns.oplus.com/photos/1.0/camera/"
    private const val NS_MICAMERA = "http://ns.xiaomi.com/photos/1.0/camera/"

    /**
     * 分层检测 Live Photo 类型
     * 优先级: XMP元数据 > 文件末尾标识 > 配对视频文件 > 视频签名
     */
    fun detect(context: Context, uri: Uri): LivePhotoInfo {
        val filePath = uri.toFilePath(context) ?: return LivePhotoInfo()

        // 1. XMP 元数据检测 (Google/小米/OPPO)
        detectByXMP(filePath)?.let { return it }

        // 2. 文件末尾标识检测 (vivo/华为)
        detectByFooter(filePath)?.let { return it }

        // 3. 配对视频文件检测 (vivo/Apple)
        detectByPairedVideo(context, uri)?.let { return it }

        // 4. 视频签名回退检测
        if (containsVideoSignature(filePath)) {
            return LivePhotoInfo(
                type = LivePhotoType.GOOGLE_PIXEL,
                videoOffset = estimateVideoOffset(filePath)
            )
        }

        return LivePhotoInfo()
    }

    private fun detectByXMP(filePath: String): LivePhotoInfo? {
        return try {
            val exif = ExifInterface(filePath)
            val xmpString = exif.getAttribute(ExifInterface.TAG_XMP) ?: return null
            
            // 解析 XMP 元数据
            // 检查 GCamera:MotionPhoto="1" (Google/OPPO)
            // 检查 GCamera:MicroVideo="1" (小米)
            // 检查 OpCamera:MotionPhotoOwner="oplus" (OPPO)
            // 提取 MotionPhotoOffset / MicroVideoOffset / VideoLength
            // ...
        } catch (e: Exception) { null }
    }

    private fun detectByFooter(filePath: String): LivePhotoInfo? {
        return try {
            RandomAccessFile(filePath, "r").use { raf ->
                val footer = readLastBytes(raf, 2048)
                
                // vivo: 检查 "vivo{" JSON 字符串
                if (footer.contains("""vivo\{""".toRegex())) {
                    return LivePhotoInfo(type = LivePhotoType.VIVO)
                }
                
                // 华为/荣耀: 检查 "LIVE_" 标识
                if (footer.contains("LIVE_")) {
                    val videoLength = parseHuaweiVideoLength(footer)
                    return LivePhotoInfo(
                        type = LivePhotoType.HUAWEI,
                        videoLength = videoLength
                    )
                }
            }
            null
        } catch (e: Exception) { null }
    }

    private fun detectByPairedVideo(context: Context, imageUri: Uri): LivePhotoInfo? {
        val parentUri = imageUri.parentUri() ?: return null
        val baseName = imageUri.fileNameWithoutExtension() ?: return null
        
        // 检查 .mp4 (vivo)
        val mp4Uri = parentUri.buildUpon()
            .appendPath("$baseName.mp4").build()
        if (uriExists(context, mp4Uri)) {
            return LivePhotoInfo(
                type = LivePhotoType.VIVO,
                pairedVideoUri = mp4Uri
            )
        }
        
        // 检查 .mov (Apple)
        val movUri = parentUri.buildUpon()
            .appendPath("$baseName.mov").build()
        if (uriExists(context, movUri)) {
            return LivePhotoInfo(
                type = LivePhotoType.APPLE,
                pairedVideoUri = movUri
            )
        }
        
        return null
    }
}
```

#### 5.5 LivePhotoExtractor 工具类（视频提取）

**文件**：`util/LivePhotoExtractor.kt`

```kotlin
object LivePhotoExtractor {
    /**
     * 根据 Live Photo 类型提取视频
     */
    fun extractVideo(
        context: Context,
        mediaItem: MediaItem,
        outputFile: File
    ): Boolean {
        return when (mediaItem.livePhotoInfo.type) {
            LivePhotoType.XIAOMI -> 
                extractXiaomiVideo(mediaItem, outputFile)
            LivePhotoType.GOOGLE_PIXEL, LivePhotoType.OPPO -> 
                extractEmbeddedVideo(mediaItem, outputFile)
            LivePhotoType.HUAWEI -> 
                extractHuaweiVideo(mediaItem, outputFile)
            LivePhotoType.VIVO, LivePhotoType.APPLE -> 
                copyPairedVideo(mediaItem, outputFile)
            else -> false
        }
    }

    private fun extractXiaomiVideo(mediaItem: MediaItem, outputFile: File): Boolean {
        val offset = mediaItem.livePhotoInfo.videoOffset
        return extractFromOffset(mediaItem.uri, outputFile, offset)
    }

    private fun extractEmbeddedVideo(mediaItem: MediaItem, outputFile: File): Boolean {
        val videoLength = mediaItem.livePhotoInfo.videoLength
        val fileLength = getFileLength(mediaItem.uri)
        val offset = fileLength - videoLength
        return extractFromOffset(mediaItem.uri, outputFile, offset, videoLength)
    }

    private fun extractHuaweiVideo(mediaItem: MediaItem, outputFile: File): Boolean {
        // 华为格式: 视频数据在文件末尾，长度由 LIVE_ 后面的数字指定
        val videoLength = mediaItem.livePhotoInfo.videoLength
        val fileLength = getFileLength(mediaItem.uri)
        val offset = fileLength - videoLength
        return extractFromOffset(mediaItem.uri, outputFile, offset, videoLength)
    }

    private fun copyPairedVideo(mediaItem: MediaItem, outputFile: File): Boolean {
        val videoUri = mediaItem.livePhotoInfo.pairedVideoUri ?: return false
        return copyUriToFile(videoUri, outputFile)
    }
}
```

---

### 阶段六：删除功能

#### 6.1 DeleteMediaUseCase

**文件**：`domain/usecase/DeleteMediaUseCase.kt`

- API 30+：调用 `MediaStore.createTrashRequest()` 生成 `PendingIntent`
- API 26-29：调用 `ContentResolver.delete()` 直接删除

#### 6.2 ConfirmDeleteDialog

**文件**：`ui/screen/confirm/ConfirmDeleteDialog.kt`

触发条件（满足任一）：
1. 删除队列达到 50 项
2. 当前月份所有媒体项均已加入队列

弹窗内容：
- 标题："确认删除"
- 内容：API 30+ 显示"移入回收站，30天后自动清理"；低版本显示"永久删除，不可撤销"
- 按钮：[取消] [确认删除]

#### 6.3 删除执行流程

- `createTrashRequest` 返回的 `PendingIntent` 通过 `ActivityResultContracts.StartIntentSenderForResult()` 启动
- 系统弹出确认对话框，用户确认后执行
- 成功后刷新列表，清空队列
- 失败时显示错误提示

---

### 阶段七：优化与完善

- 添加加载状态、空状态、错误状态 UI
- 缩略图采样优化（Coil `size(480, 480)`）
- 处理屏幕旋转状态保存（`SavedStateHandle`）
- 部分授权处理（用户只授予图片或视频权限）
- ProGuard / R8 混淆规则

---

## 项目文件结构

```
com.gallery.cleaner/
├── GalleryApp.kt
├── MainActivity.kt
├── di/
│   ├── DataModule.kt
│   └── DispatcherModule.kt
├── data/
│   ├── model/MediaItemEntity.kt
│   ├── repository/MediaRepositoryImpl.kt
│   └── source/MediaStoreDataSource.kt
├── domain/
│   ├── model/MediaItem.kt, MediaGroup.kt, DeleteQueue.kt
│   ├── repository/MediaRepository.kt
│   └── usecase/
│       ├── GetMediaGroupsUseCase.kt
│       ├── DeleteMediaUseCase.kt
│       └── CheckTrashSupportUseCase.kt
├── ui/
│   ├── navigation/NavGraph.kt
│   ├── theme/Theme.kt, Color.kt, Type.kt
│   ├── permission/PermissionHandler.kt
│   ├── screen/
│   │   ├── gallery/ (GalleryScreen, GalleryViewModel, MonthCard, GalleryUiState)
│   │   ├── media/ (MediaSwipeScreen, MediaSwipeViewModel, SwipeableMediaCard, DeleteQueueBar)
│   │   ├── preview/ (PreviewScreen, PreviewViewModel, VideoPlayer, MotionPhotoPlayer, SpeedIndicatorOverlay)
│   │   └── confirm/ (ConfirmDeleteDialog, DeleteResultDialog)
│   └── component/ (MediaThumbnail, VideoIndicator, SwipeDeleteOverlay)
└── util/ (DateUtils, PermissionUtils, Constants, LivePhotoDetector, LivePhotoExtractor)
```

---

## 关键技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| UI 框架 | Jetpack Compose + Material 3 | 声明式 UI，开发效率高 |
| 手势处理 | Compose 原生 API | 无需第三方库，维护性好 |
| 图片加载 | Coil 3 | Compose 原生支持，性能优秀 |
| 视频播放 | Media3 ExoPlayer | Android 官方推荐，内置 Motion Photo 支持 |
| Live 图播放 | 分层检测 + 多方案播放 | XMP/文件末尾/配对文件分层检测，支持 Google/小米/OPPO/vivo/华为/Apple |
| 删除方式 | createTrashRequest (API 30+) | 安全移入回收站，可恢复 |
| 媒体查询 | MediaStore.Files | 一次查询图片+视频 |
| 分组方式 | 内存中按月分组 | 灵活，避免 SQL 复杂度 |
| 最低 API | 26 (Android 8.0) | 覆盖 95%+ 设备 |

---

## 验证步骤

1. **权限验证**：在 API 33+ 和 API 29 设备上测试权限请求流程
2. **月份浏览**：验证按月分组正确性，缩略图正常加载
3. **手势验证**：测试上滑删除、左划下一张、右划上一张、中心点击预览
4. **删除验证**：测试队列满 50 项触发、月份滑完触发、API 30+ 回收站、低版本直接删除
5. **视频验证**：测试视频缩略图加载、全屏播放、生命周期管理
6. **长按加速验证**：测试长按视频加速到 2x、速度指示器显示、松开恢复 1x、渐进变速动画
7. **Live 图验证**：
   - Google Pixel：XMP MotionPhoto 检测、Media3 直接播放
   - 小米：XMP MicroVideo 检测、偏移量提取播放
   - OPPO：XMP MotionPhoto + OpCamera 检测
   - vivo：文件末尾 JSON 检测、配对 MP4 播放
   - 华为/荣耀：LIVE_ 标识检测、嵌入式视频提取
   - Apple：配对 MOV 文件检测与播放
8. **状态保存**：旋转屏幕后删除队列和页面位置不丢失
9. **边界情况**：空相册、单张照片、大量媒体（10000+）性能
