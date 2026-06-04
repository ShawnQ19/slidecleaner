# 相册清理 (Gallery Cleaner) - 更新日志

> 本文档按时间倒序记录项目所有版本变更，最新版本在前。
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，版本号采用 `主版本.次版本.修订号` 语义化版本控制。

---

## [1.3.0] - 2026-06-04

### 修复 (Fixed)

- **已整理相册为空**
  - 问题根因：`MediaRepositoryImpl.buildProcessedGroups()` 仅通过 `getMediaItemsByUris()` 查询 MediaStore 中未删除的活动项（`IS_TRASHED != 1`）。当用户将照片移入系统回收站后，这些已整理项的 `IS_TRASHED` 变为 1，导致查询返回空结果，已整理相册显示为空。
  - 解决方案：
    1. 在 `buildProcessedGroups()` 中先查询活动项：`dataSource.getMediaItemsByUris(processedUris.map(Uri::parse))`；
    2. 计算未找到的 URI（`missingUris = processedUris - foundUris`）；
    3. 对 missingUris 调用 `dataSource.queryTrashedMedia()` 查询回收站中的已整理项；
    4. 合并 `activeItems + trashedItems`，去重后按月分组。
  - 关键代码变更：新增 `val trashedItems = if (missingUris.isNotEmpty()) { dataSource.queryTrashedMedia().filter { it.uri.toString() in missingUris } } else emptyList()`，以及日志输出 `active=${activeItems.size}, trashed=${trashedItems.size}`。
  - 相关文件：[MediaRepositoryImpl.kt](app/src/main/java/com/gallery/cleaner/data/repository/MediaRepositoryImpl.kt)
  - 负责人：开发团队

- **已删除照片错误出现在已整理相册中**
  - 问题根因：`MediaStoreDataSource.queryMediaItemsByIds()` 和 `getMediaItemByUri()` 未过滤 `IS_TRASHED` 状态。当用户删除照片后，这些照片虽然被标记为已整理，但仍存在于回收站中（`IS_TRASHED=1`），查询时未排除导致它们错误出现在已整理相册。
  - 解决方案：
    1. `queryMediaItemsByIds()` 的 selection 从 `"${MediaStore.MediaColumns._ID} IN (${placeholders})"` 改为 `"${MediaStore.MediaColumns._ID} IN (${placeholders}) AND ${MediaStore.MediaColumns.IS_TRASHED} = 0"`；
    2. `getMediaItemByUri()` 新增 selection `"${MediaStore.MediaColumns.IS_TRASHED} = 0"`。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **月份相册中左滑保留的照片未标记为已整理**
  - 问题根因：`MediaSwipeViewModel.keepCurrent()` 方法已存在且正确调用了 `markProcessed`，但 `MediaSwipeScreen` 的 UI 层没有任何入口触发该方法。`SwipeableMediaCard` 只支持上滑删除（`detectVerticalDragGestures`），没有水平拖动手势，用户无法执行保留操作。
  - 解决方案：在 `DeleteQueueBar` 底部栏新增"保留"按钮，点击后调用 `viewModel.keepCurrent()`。同时 `RandomCleanupScreen` 也传入相同的 `onKeep` 回调，保持两种整理模式交互一致。
  - 相关文件：[DeleteQueueBar.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/DeleteQueueBar.kt)、[MediaSwipeScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/MediaSwipeScreen.kt)、[RandomCleanupScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/random/RandomCleanupScreen.kt)
  - 负责人：开发团队

- **月份相册中确认删除后保留项未进入已整理相册**
  - 问题根因：`MediaSwipeViewModel.confirmDelete()` 和 `onTrashIntentResult()` 只调用了 `finalizeKeptItems()` 来标记保留项，但删除操作后 `keptItems` 集合可能未正确维护。同时 `MediaStoreDataSource.queryMediaItemsByIds()` 未过滤回收站项，导致查询结果包含已删除项，影响已整理相册的显示逻辑。
  - 解决方案：
    1. 确保 `confirmDelete()` 和 `onTrashIntentResult()` 在删除成功后正确调用 `finalizeKeptItems()`；
    2. 在 `queryMediaItemsByIds()` 中添加 `IS_TRASHED = 0` 过滤，确保已删除项不会出现在查询结果中；
    3. 添加日志 `AppLogger.i(TAG, "confirmDelete: 标记已保留项...")` 便于调试。
  - 相关文件：[MediaSwipeViewModel.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/MediaSwipeViewModel.kt)、[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **首页缩略图视频指示器样式不一致**
  - 问题根因：`MediaThumbnail` 组件中的视频指示器使用自定义实现：蓝色圆形背景（`AppColors.Primary`）+ 白色播放图标 + 半透明黑色遮罩，与 `VideoIndicator` 组件的白色圆形背景 + 深色播放图标样式完全不统一。
  - 解决方案：移除 `MediaThumbnail` 中的自定义视频指示器代码块（包含 `Box` + `Icon` + `background(AppColors.Primary)`），直接复用 `VideoIndicator(modifier = Modifier.align(Alignment.Center))` 组件。
  - 相关文件：[MediaThumbnail.kt](app/src/main/java/com/gallery/cleaner/ui/component/MediaThumbnail.kt)
  - 负责人：开发团队

- **Live Photo 播放失败无日志定位**
  - 问题根因：Live 图检测、视频提取和播放过程缺乏详细日志，当播放失败时无法判断是检测阶段（未识别为 Live Photo）、提取阶段（视频数据提取失败）还是播放阶段（ExoPlayer 初始化/播放错误）出现问题。
  - 解决方案：
    1. **MotionPhotoPlayer.kt**：新增 Compose 时日志（id、name、liveType、isVideo、videoOffset、videoLength、pairedVideoUri）；`prepareVideoUri()` 方法添加 enter/exit 日志记录参数和返回值；ExoPlayer 创建、释放、播放状态变化（onIsPlayingChanged、onPlaybackStateChanged）均添加日志；长按触发/松开日志；PlayerView 创建日志。
    2. **LivePhotoExtractor.kt**：`extractVideo()` 方法添加 enter 日志（type、videoOffset、videoLength、outputFile）；各提取分支（小米 offset、Google/OPPO 末尾、华为 LIVE_、vivo/Apple 配对文件）添加过程日志；新增 `tryExtractFromEnd()` 回退方案日志；exit 日志记录结果和文件大小。
    3. **LivePhotoDetector.kt**：`detect()` 方法添加 uri 和 filePath 日志；XMP 检测命中时输出 type、offset、length；Footer 检测命中时输出 type、offset、length；配对视频检测命中时输出 type、pairedVideoUri；视频签名回退命中时输出 type、offset、length；非 Live Photo 判定日志。
  - 相关文件：[MotionPhotoPlayer.kt](app/src/main/java/com/gallery/cleaner/ui/screen/preview/MotionPhotoPlayer.kt)、[LivePhotoExtractor.kt](app/src/main/java/com/gallery/cleaner/util/LivePhotoExtractor.kt)、[LivePhotoDetector.kt](app/src/main/java/com/gallery/cleaner/util/LivePhotoDetector.kt)
  - 负责人：开发团队

### 改进 (Changed)

- **DeleteQueueBar 底部栏新增"保留"按钮**
  - 新增 `onKeep: (() -> Unit)? = null` 可选参数。
  - 按钮布局从左到右依次为：保留（绿色，如有）→ 撤销（灰色，如有）→ 确认删除（红色）。
  - "保留"按钮样式：`AppColors.Success` 绿色背景，白色 `Icons.Default.CheckCircle` 勾选图标，文字"保留"，无边框圆角矩形（`AppShape.Medium`）。
  - 按钮始终启用（无需判断状态），点击后立即调用 `onKeep()`。
  - 相关文件：[DeleteQueueBar.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/DeleteQueueBar.kt)
  - 负责人：开发团队

- **SwipeableMediaCard 移除左滑手势（避免与 HorizontalPager 冲突）**
  - 之前尝试添加 `detectHorizontalDragGestures` 实现左滑保留，但与 `HorizontalPager` 的左右翻页手势冲突，导致用户无法右滑查看上一张照片。
  - 已完全移除水平拖动手势相关代码（`offsetX`、`detectHorizontalDragGestures`、`KeepSwipeHint` 组件），恢复为仅支持上滑删除。
  - 保留操作改为通过底部"保留"按钮触发。
  - 相关文件：[SwipeableMediaCard.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/SwipeableMediaCard.kt)
  - 负责人：开发团队

- **构建脚本自动复制 APK 到根目录**
  - `build.bat` 编译成功后自动将 debug APK 从 `app/build/outputs/apk/debug/` 复制到项目根目录。
  - 文件名格式：`GalleryCleaner-debug-yyyyMMdd.apk`（如 `GalleryCleaner-debug-20260604.apk`）。
  - 控制台输出复制后的文件路径和大小（如 `19.98 MB`）。
  - 相关文件：[build.bat](build.bat)
  - 负责人：开发团队

### 已知问题 (Known Issues)

- `MediaRepositoryImpl.moveToTrash()` 中创建的 `pendingIntent` 变量当前未被使用，系统回收站确认弹窗尚未接入 `ActivityResultContracts.StartIntentSenderForResult()`。
  - 影响：Android 11+ 设备上点击"确认删除"后直接返回成功，未弹出系统二次确认。
  - 计划：在后续版本中完善 `PendingIntent` 的启动流程。

---

## [1.2.0] - 2026-06-02

### 修复 (Fixed)

- **左滑保留后无法右滑回看上一张**
  - 问题根因：左滑时立即调用 markProcessed 将当前媒体标记为已整理，导致该项从列表移除，用户无法右滑返回查看上一张。
  - 解决方案：左滑只将项加入内存 keptItems 集合，不立即持久化；当用户退出整理或点击"确认删除"时，统一调用 markProcessed 持久化所有保留项。
  - 相关文件：MediaSwipeViewModel.kt、RandomCleanupViewModel.kt
  - 负责人：开发团队

- **随机整理确认删除时错误标记剩余批次为已整理**
  - 问题根因：RandomCleanupViewModel 的 confirmDelete 逻辑中，删除成功后会将当前批次剩余所有照片标记为已整理，导致无意图的大批量整理标记。
  - 解决方案：删除 pendingProcessedItems 相关逻辑，confirmDelete 后仅清空删除队列，不将剩余项标记为已整理。
  - 相关文件：RandomCleanupViewModel.kt
  - 负责人：开发团队

- **Live Photo 检测失败（Android 10+ 设备，包括苹果、vivo、小米等厂商）**
  - 问题根因：原始实现依赖 MediaStore DATA 字段获取文件路径，在 Android 10+ 上该字段可能为 null；部分厂商的 Live Photo 格式不匹配原始检浍规则。
  - 解决方案：
    1. 重写 LivePhotoDetector，XMP 检浍和文件末尾检浍同时支持文件路径和 URI 流（contentResolver.openInputStream）两种读取方式；
    2. 重写 LivePhotoExtractor，统一通过 URI 流读取，支持小米 MicroVideoOffset、vivo 文件末尾 JSON、华为 LIVE_标识、苹果 .mov 配对文件等多厂商格式；
    3. 新增文件长度获取工具方法（getFileLength 通过 openFileDescriptor）。
  - 相关文件：LivePhotoDetector.kt、LivePhotoExtractor.kt
  - 负责人：开发团队

### 改进 (Changed)

- **整理已保留项的持久化时机优化**
  - 左滑保留不再立即调用 markProcessed，改为在 onBackPressed 和 confirmDelete 时统一执行。
  - 新增 finalizeKeptItems() 方法，在退出整理或确认删除时才将 keptItems 中的项持久化到 ProcessedMediaStore。
  - 相关文件：MediaSwipeViewModel.kt、RandomCleanupViewModel.kt
  - 负责人：开发团队

- **移除随机整理层"保留"按钮**
  - RandomCleanupScreen TopAppBar 中的"保留"按钮已移除，用户通过左滑表示保留，无需额外按钮。
  - 移除相关文件：import androidx.compose.material.icons.filled.Done
  - 相关文件：RandomCleanupScreen.kt
  - 负责人：开发团队

- **AppLogger.enter 参数类型调整**
  - enter 方法参整改为 vararg params: Pair<String, Any?>，支持多个键值对参数。
  - 相关文件：AppLogger.kt、MediaSwipeViewModel.kt、RandomCleanupViewModel.kt
  - 负责人：开发团队

### 构建与工具 (Build & Tools)

- **build.bat 改进**
  - 编译后 APK 自动复制到项目根目录，文件名格式 GalleryCleaner-debug-yyyyMMdd.apk。
  - 相关文件：build.bat
  - 负责人：开发团队

---

## [1.1.0] - 2026-05-15

### 修复 (Fixed)

#### 媒体读取权限与兼容性问题
- **修复 vivo Android 15 设备授权后显示 "0 个项目" 的问题**
  - 问题根因：`MediaStore.Files.getContentUri("external")` 在 Android 15 (API 35) 尤其是 vivo OriginOS 5 上返回空结果集，导致无法读取任何媒体文件。
  - 解决方案：将统一查询拆分为 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` 和 `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` 分别查询，再合并排序。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **修复 Android 14+ 部分媒体访问权限缺失**
  - 新增 `READ_MEDIA_VISUAL_USER_SELECTED` 权限声明，适配 Android 14 (API 34) 引入的「部分照片访问」功能。当用户选择"仅允许访问部分照片"时，应用可正常读取已选中的媒体。
  - 相关文件：[AndroidManifest.xml](app/src/main/AndroidManifest.xml)
  - 负责人：开发团队

- **修复权限授予后应用不自动刷新媒体列表**
  - 问题根因：权限弹窗关闭后，ViewModel 未收到通知重新加载数据，界面持续显示空状态或加载中。
  - 解决方案：
    1. 在 `PermissionHandler` 中新增 `onPermissionGranted: () -> Unit` 回调参数；
    2. 添加 `LifecycleEventObserver` 监听 `ON_RESUME` 事件，当用户从系统设置返回时自动检测权限变化并触发回调；
    3. `GalleryViewModel` 新增 `onPermissionGranted()` 方法调用 `loadMediaGroups()`；
    4. `GalleryScreen` 将 `viewModel::onPermissionGranted` 传入 `PermissionHandler`。
  - 相关文件：[PermissionHandler.kt](app/src/main/java/com/gallery/cleaner/ui/permission/PermissionHandler.kt)、[GalleryViewModel.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/GalleryViewModel.kt)、[GalleryScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/GalleryScreen.kt)
  - 负责人：开发团队

- **修复 MediaStore 查询异常导致无限加载**
  - 问题根因：查询过程中抛出 `SecurityException` 或其他异常时未捕获，Flow 流中断，界面持续显示加载圆圈。
  - 解决方案：在 `queryMedia()` 中添加 `try-catch` 块捕获 `SecurityException` 和通用 `Exception`，异常时 `emit(emptyList())` 并输出日志。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **修复 `getMediaItemById` 编译错误**
  - 将内联的 `if` 表达式重构为独立的 `queryImageById()` 和 `queryVideoById()` 私有方法，避免 Kotlin 编译器将 `if (cursor.moveToFirst()) { return ... }` 识别为缺少 else 分支的表达式错误。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

### 改进 (Changed)

- **优化 MediaStore 查询日志输出**
  - 在 `queryImages()` 和 `queryVideos()` 中添加 `Log.d` 输出查询 URI 和 cursor count，便于在 vivo Android 15 等设备上调试媒体读取问题。
  - 在 `queryMedia()` 中添加 `Log.d` 输出最终合并后的媒体总数。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **增强 Gallery 界面空状态与错误状态体验**
  - 空状态：显示 "没有找到照片或视频" 提示语，并增加 "刷新" 按钮调用 `viewModel.retry()`。
  - 错误状态：显示错误标题、详细错误信息，并增加 "重试" 按钮。
  - 加载状态：居中显示蓝色加载指示器 (`BlueAccent`)。
  - 相关文件：[GalleryScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/GalleryScreen.kt)
  - 负责人：开发团队

- **优化 GalleryViewModel 加载状态管理**
  - 新增 `isLoading` 防重入检查：`if (_uiState.value.isLoading) return`。
  - 使用 `.onStart { _uiState.update { isLoading = true } }` 和 `.catch { ... }` 替代手动状态管理，代码更简洁。
  - 新增 `retry()` 公共方法供 UI 调用。
  - 相关文件：[GalleryViewModel.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/GalleryViewModel.kt)
  - 负责人：开发团队

### 构建与工具 (Build & Tools)

- **升级 Android Gradle Plugin 至 8.5.2**
  - 原版本：8.2.x（不支持 compileSdk=35）。
  - 升级后消除 `compileSdk=35` 不支持的编译警告。
  - 相关文件：[build.gradle.kts (project)](build.gradle.kts)
  - 负责人：开发团队

- **添加 `android.suppressUnsupportedCompileSdk=35` 到 gradle.properties**
  - 用于抑制 Android Gradle Plugin 8.5.2 对 compileSdk 35 的兼容性提示。
  - 相关文件：[gradle.properties](gradle.properties)
  - 负责人：开发团队

- **创建 `build.bat` 一键编译脚本**
  - 支持参数：`debug`（默认）/`release`、`--no-clean`（增量编译）、`--install` / `-i`（编译后自动安装）。
  - 自动检测 Java 环境、查找 APK 输出、显示文件大小。
  - 相关文件：[build.bat](build.bat)
  - 负责人：开发团队

### 已知问题 (Known Issues)

- `MediaRepositoryImpl.moveToTrash()` 中创建的 `pendingIntent` 变量当前未被使用，系统回收站确认弹窗尚未接入 `ActivityResultContracts.StartIntentSenderForResult()`。
  - 影响：Android 11+ 设备上点击"确认删除"后直接返回成功，未弹出系统二次确认。
  - 计划：在后续版本中完善 `PendingIntent` 的启动流程。

---

## [1.0.0] - 2025-05-15

### 新增 (Added)

#### 项目基础架构
- **初始化项目结构** — 采用 Kotlin + Jetpack Compose + Material 3 + Hilt + MVVM Clean Architecture 架构。
  - 模块划分：`ui`（表现层）、`domain`（领域层）、`data`（数据层）、`di`（依赖注入）、`util`（工具类）。
  - 相关目录：`app/src/main/java/com/gallery/cleaner/`
  - 负责人：开发团队

- **配置 Gradle 构建设置**
  - `compileSdk = 35`，`minSdk = 26`，`targetSdk = 35`。
  - Kotlin JVM Target = 17，启用 Compose，`kotlinCompilerExtensionVersion = 1.5.10`。
  - 相关文件：[app/build.gradle.kts](app/build.gradle.kts)
  - 负责人：开发团队

#### 核心依赖集成
- **Jetpack Compose BOM 2024.02.02** — UI 框架、Material 3、Foundation、Animation。
- **Hilt 2.50** — 依赖注入，包含 `hilt-navigation-compose` 1.1.0。
- **Coil 2.6.0** — 图片加载（含 GIF 支持），用于缩略图和预览。
- **Media3 ExoPlayer 1.3.0** — 视频播放与 Motion Photo 支持。
- **ExifInterface 1.3.7** — XMP 元数据解析，用于 Live Photo 检测。
- **Navigation Compose 2.7.7** — 页面导航。
- 相关文件：[app/build.gradle.kts](app/build.gradle.kts)
- 负责人：开发团队

#### 数据层 (Data Layer)
- **MediaStoreDataSource** — 封装 Android MediaStore 查询逻辑。
  - 使用 `MediaStore.Files.getContentUri("external")` 统一查询图片和视频。
  - 过滤 `IS_TRASHED != 1`，按 `DATE_ADDED DESC` 排序。
  - 在 `Dispatchers.IO` 上执行，返回 `Flow<List<MediaItem>>`。
  - 提供 `groupByMonth()` 方法将媒体按年月分组。
  - 相关文件：[MediaStoreDataSource.kt](app/src/main/java/com/gallery/cleaner/data/source/MediaStoreDataSource.kt)
  - 负责人：开发团队

- **MediaRepositoryImpl** — `MediaRepository` 接口实现。
  - `getMediaGroups()`：调用 DataSource 查询并按月分组。
  - `moveToTrash()`：Android 11+ 使用 `MediaStore.createTrashRequest()` 移入系统回收站；低版本回退到永久删除。
  - `deletePermanently()`：使用 `ContentResolver.delete()` 直接删除。
  - `getMediaItemById()`：通过 ID 查询单条媒体。
  - 相关文件：[MediaRepositoryImpl.kt](app/src/main/java/com/gallery/cleaner/data/repository/MediaRepositoryImpl.kt)
  - 负责人：开发团队

#### 领域层 (Domain Layer)
- **核心数据模型**
  - `MediaItem`：媒体项（id、uri、名称、MIME 类型、日期、大小、时长、宽高、Live Photo 信息、删除标记）。
  - `MediaGroup`：月份分组（yearMonth、label、媒体列表、缩略图）。
  - `DeleteQueue`：删除队列（最大容量 50，支持 `shouldPromptDelete` 判断）。
  - `LivePhotoType` 枚举：支持 Google Pixel、小米、OPPO、vivo、华为/荣耀、Apple。
  - `LivePhotoInfo`：Live Photo 详细信息（类型、视频偏移量、视频长度、配对视频 URI）。
  - 相关目录：`app/src/main/java/com/gallery/cleaner/domain/model/`
  - 负责人：开发团队

- **UseCase 层**
  - `GetMediaGroupsUseCase`：获取按月分组的媒体列表。
  - `GetMediaItemUseCase`：通过 ID 获取单条媒体。
  - `DeleteMediaUseCase`：根据系统版本选择回收站或永久删除。
  - 相关目录：`app/src/main/java/com/gallery/cleaner/domain/usecase/`
  - 负责人：开发团队

#### UI 层 (Presentation Layer)
- **Material 3 主题系统**
  - 浅色/深色双主题支持。
  - 主色调蓝色系：`BluePrimary` (#0969DA)、`BlueAccent` (#58A6FF)。
  - 暗色主题背景：`BackgroundDark` (#0D1117)、`SurfaceDark` (#161B22)。
  - 删除红色：`DeleteRed` (#DA3633)。
  - 相关文件：[Theme.kt](app/src/main/java/com/gallery/cleaner/ui/theme/Theme.kt)、[Color.kt](app/src/main/java/com/gallery/cleaner/ui/theme/Color.kt)、[Type.kt](app/src/main/java/com/gallery/cleaner/ui/theme/Type.kt)
  - 负责人：开发团队

- **权限处理组件 (PermissionHandler)**
  - API 33+：请求 `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`。
  - API 32 及以下：请求 `READ_EXTERNAL_STORAGE`。
  - 未授权时显示权限说明页面，提供"请求权限"和"前往设置"按钮。
  - 相关文件：[PermissionHandler.kt](app/src/main/java/com/gallery/cleaner/ui/permission/PermissionHandler.kt)
  - 负责人：开发团队

- **月份浏览主屏 (GalleryScreen)**
  - `TopAppBar` 显示应用标题和项目总数。
  - `LazyColumn` 展示 `MonthCard` 列表，支持点击进入月份详情。
  - `MonthCard` 显示月份标签、项目数量、3x2 缩略图网格（最多 6 张 + 剩余数量遮罩）。
  - 相关文件：[GalleryScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/GalleryScreen.kt)、[MonthCard.kt](app/src/main/java/com/gallery/cleaner/ui/screen/gallery/MonthCard.kt)
  - 负责人：开发团队

- **媒体滑动浏览屏 (MediaSwipeScreen)**
  - `HorizontalPager` 全屏浏览当月媒体。
  - 上滑手势：将当前媒体加入删除队列，显示红色删除提示动画。
  - 左划/右划：切换上一张/下一张。
  - 点击中心：进入全屏预览。
  - 底部 `DeleteQueueBar`：显示已选数量、进度条、确认删除按钮。
  - 队列满 50 项或当月媒体全部标记时自动弹出删除确认弹窗。
  - 相关文件：[MediaSwipeScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/MediaSwipeScreen.kt)、[SwipeableMediaCard.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/SwipeableMediaCard.kt)、[DeleteQueueBar.kt](app/src/main/java/com/gallery/cleaner/ui/screen/media/DeleteQueueBar.kt)
  - 负责人：开发团队

- **全屏预览屏 (PreviewScreen)**
  - 普通图片：Coil `AsyncImage` 高清加载，支持双指缩放。
  - 普通视频：`VideoPlayer` 组件播放，支持长按 2x 加速，显示速度指示器覆盖层。
  - Live 图：`MotionPhotoPlayer` 组件，默认显示静态图，自动播放嵌入视频。
  - 底部信息栏：文件名、文件大小、拍摄日期。
  - 相关文件：[PreviewScreen.kt](app/src/main/java/com/gallery/cleaner/ui/screen/preview/PreviewScreen.kt)
  - 负责人：开发团队

#### Live Photo (动态照片) 多厂商支持
- **LivePhotoDetector 分层检测工具**
  - 优先级 1 — XMP 元数据检测：`GCamera:MotionPhoto`（Google/OPPO）、`GCamera:MicroVideo`（小米）、`OpCamera:MotionPhotoOwner`（OPPO）。
  - 优先级 2 — 文件末尾标识检测：`vivo{` JSON（vivo）、`LIVE_` 标识（华为/荣耀）。
  - 优先级 3 — 配对视频文件检测：同名 `.mp4`（vivo）、同名 `.mov`（Apple）。
  - 优先级 4 — 视频签名回退检测：`ftyp`/`moov` 签名。
  - 相关文件：[LivePhotoDetector.kt](app/src/main/java/com/gallery/cleaner/util/LivePhotoDetector.kt)
  - 负责人：开发团队

- **LivePhotoExtractor 视频提取工具**
  - 小米：按 XMP `MicroVideoOffset` 偏移量提取。
  - Google Pixel / OPPO：按文件末尾偏移量提取嵌入式视频。
  - 华为/荣耀：按 `LIVE_` 标识后的长度从文件末尾提取。
  - vivo / Apple：直接复制配对视频文件。
  - 相关文件：[LivePhotoExtractor.kt](app/src/main/java/com/gallery/cleaner/util/LivePhotoExtractor.kt)
  - 负责人：开发团队

- **MotionPhotoPlayer 组件**
  - 根据 `livePhotoInfo.type` 自动选择播放方案。
  - 单文件格式（Google/小米/OPPO/华为）：提取临时 MP4 后用 ExoPlayer 播放。
  - 双文件格式（vivo/Apple）：直接播放配对视频 URI。
  - 右下角显示对应厂商 Live 图标识（Motion / MicroVideo / O-Live / 动态照片 / Live）。
  - 相关文件：[MotionPhotoPlayer.kt](app/src/main/java/com/gallery/cleaner/ui/screen/preview/MotionPhotoPlayer.kt)
  - 负责人：开发团队

#### 导航与组件
- **导航图 (NavGraph)**
  - 三条路由：`gallery` → `media_swipe/{yearMonth}` → `preview/{mediaId}`。
  - 相关文件：[NavGraph.kt](app/src/main/java/com/gallery/cleaner/ui/navigation/NavGraph.kt)、[Routes.kt](app/src/main/java/com/gallery/cleaner/ui/navigation/Routes.kt)
  - 负责人：开发团队

- **通用组件**
  - `MediaThumbnail`：Coil `AsyncImage` 封装，支持缩略图尺寸限制和视频角标。
  - `VideoIndicator`：圆形播放图标角标。
  - 相关目录：`app/src/main/java/com/gallery/cleaner/ui/component/`
  - 负责人：开发团队

#### 依赖注入
- **Hilt 模块配置**
  - `DataModule`：绑定 `MediaRepository` 接口到 `MediaRepositoryImpl`。
  - `DispatcherModule`：提供 `@IoDispatcher`、`@DefaultDispatcher`、`@MainDispatcher`。
  - `GalleryApp`：`@HiltAndroidApp` 应用入口。
  - `MainActivity`：`@AndroidEntryPoint`，设置 Compose Content 和导航图。
  - 相关文件：[DataModule.kt](app/src/main/java/com/gallery/cleaner/di/DataModule.kt)、[DispatcherModule.kt](app/src/main/java/com/gallery/cleaner/di/DispatcherModule.kt)、[GalleryApp.kt](app/src/main/java/com/gallery/cleaner/GalleryApp.kt)、[MainActivity.kt](app/src/main/java/com/gallery/cleaner/MainActivity.kt)
  - 负责人：开发团队

### 文档 (Documentation)

- **DESIGN.md** — comprehensive 设计文档，涵盖功能规格、技术架构、数据模型、Live Photo 兼容方案、权限设计、导航设计、状态管理、性能优化、安全隐私、兼容性、未来扩展。
  - 相关文件：[DESIGN.md](DESIGN.md)
  - 负责人：开发团队

- **PLAN.md** — 详细实施计划文档，按阶段划分（项目初始化、数据层、月份浏览主屏、媒体滑动浏览、全屏预览、删除功能、优化完善），包含文件结构、技术决策、验证步骤。
  - 相关文件：[PLAN.md](PLAN.md)
  - 负责人：开发团队

---

## 版本对比速览

| 版本 | 日期 | 核心变更 |
|------|------|----------|
| 1.3.0 | 2026-06-04 | 已整理相册为空修复；已删除照片过滤；保留按钮；视频指示器样式统一；Live Photo 日志增强；APK 自动复制 |
| 1.2.0 | 2026-06-02 | 左滑保留不立即持久化；修复随机整理误标记已整理；Live Photo URI 流检测；移除保留按钮；build.bat 改进 |
| 1.1.0 | 2026-05-15 | 修复 vivo Android 15 媒体读取、权限回调、无限加载；优化查询与日志；升级 AGP 8.5.2；新增 build.bat |
| 1.0.0 | 2025-05-15 | 项目初始化，完整实现月份浏览、滑动删除、视频加速、Live Photo 多厂商支持 |

---

## 贡献指南

如需提交变更，请按以下格式在对应版本下添加条目：

```markdown
### 类型 (Category)

- **简短标题**
  - 详细描述（可选多行）。
  - 相关文件：[文件名](路径)
  - 负责人：姓名/团队
```

类型包括：`新增 (Added)`、`修复 (Fixed)`、`改进 (Changed)`、`废弃 (Deprecated)`、`移除 (Removed)`、`安全 (Security)`、`构建与工具 (Build & Tools)`、`文档 (Documentation)`。
