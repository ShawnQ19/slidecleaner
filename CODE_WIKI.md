# Gallery Cleaner - Code Wiki

> **相册清理应用** - 跨平台移动端照片/视频批量管理应用

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术架构](#2-技术架构)
- [3. 项目结构](#3-项目结构)
- [4. 核心模块详解](#4-核心模块详解)
- [5. 数据模型](#5-数据模型)
- [6. 关键类与函数](#6-关键类与函数)
- [7. 依赖关系](#7-依赖关系)
- [8. 构建与运行](#8-构建与运行)
- [9. 版本历史](#9-版本历史)

---

## 1. 项目概述

### 1.1 应用简介

**Gallery Cleaner（相册清理）** 是一款跨平台的移动端照片/视频批量管理应用，支持 Android 和 iOS 平台。

**核心功能：**
- 按月浏览设备中的照片和视频
- 通过滑动手势快速整理媒体文件
- 批量删除并移入系统回收站（30天自动清理）
- 支持视频长按加速播放（2x）
- 支持多厂商 Live 图（Motion Photo）播放
- 随机清理模式
- 已整理归档管理
- 主题定制

### 1.2 目标用户

- 手机存储空间紧张的用户
- 需要定期整理照片视频的用户
- 希望快速清理无用媒体文件的用户

### 1.3 平台支持

| 平台 | 最低版本 | 目标版本 | 当前状态 |
|------|----------|----------|----------|
| Android | API 26 (Android 8.0) | API 35 (Android 15) | 完整实现 |
| iOS | iOS 16.0 | - | 基础框架 |

---

## 2. 技术架构

### 2.1 架构模式

采用 **MVVM + Clean Architecture** 架构：

```
┌─────────────────────────────────────────────────────┐
│              UI 层 (Presentation Layer)              │
│  Screen Composables ← ViewModel ← UiState            │
│  ├── StateFlow 状态管理                               │
│  ├── Compose 声明式 UI                                │
│  └── Hilt 依赖注入                                    │
├─────────────────────────────────────────────────────┤
│              Domain 层 (Business Logic)               │
│  Repository 接口 / UseCase / Domain Model            │
│  ├── 业务逻辑封装                                     │
│  └── 数据模型定义                                     │
├─────────────────────────────────────────────────────┤
│              Data 层 (Data Access)                    │
│  Repository 实现 / MediaStore 查询 / 数据源          │
│  ├── MediaStore 数据源                                │
│  ├── DataStore 持久化                                 │
│  └── 系统 API 封装                                    │
└─────────────────────────────────────────────────────┘
```

### 2.2 技术栈

#### Android 技术栈

| 组件 | 技术选择 | 版本 |
|------|----------|------|
| 编程语言 | Kotlin | 1.9.22 |
| UI 框架 | Jetpack Compose | BOM 2024.02.02 |
| 设计系统 | Material 3 | - |
| 依赖注入 | Hilt | 2.50 |
| 图片加载 | Coil | 2.6.0 |
| 视频播放 | Media3 ExoPlayer | 1.3.0 |
| 元数据解析 | ExifInterface | 1.3.7 |
| 导航 | Navigation Compose | 2.7.7 |
| 持久化 | DataStore Preferences | 1.0.0 |
| 最低 API | Android 8.0 | API 26 |
| 目标 API | Android 15 | API 35 |

#### iOS 技术栈

| 组件 | 技术选择 |
|------|----------|
| 编程语言 | Swift 5 |
| UI 框架 | SwiftUI |
| 媒体访问 | Photos Framework |
| 视频播放 | AVKit |
| 持久化 | UserDefaults |

---

## 3. 项目结构

```
slidecleaner/
├── app/                          # Android 应用模块
│   ├── src/main/
│   │   ├── java/com/gallery/cleaner/
│   │   │   ├── GalleryApp.kt     # Application 入口
│   │   │   ├── MainActivity.kt   # Activity 入口
│   │   │   ├── di/               # 依赖注入模块
│   │   │   │   ├── DataModule.kt
│   │   │   │   └── DispatcherModule.kt
│   │   │   ├── domain/           # 领域层
│   │   │   │   ├── model/        # 领域模型
│   │   │   │   ├── repository/   # 仓库接口
│   │   │   │   ├── usecase/      # 用例
│   │   │   │   └── undo/         # 撤销管理
│   │   │   ├── data/             # 数据层
│   │   │   │   ├── source/       # 数据源
│   │   │   │   └── repository/   # 仓库实现
│   │   │   ├── ui/               # 表现层
│   │   │   │   ├── component/    # 通用组件
│   │   │   │   ├── navigation/   # 导航
│   │   │   │   ├── permission/   # 权限处理
│   │   │   │   ├── screen/       # 页面
│   │   │   │   │   ├── gallery/  # 月份浏览主页
│   │   │   │   │   ├── media/    # 媒体滑动浏览
│   │   │   │   │   ├── preview/  # 全屏预览
│   │   │   │   │   ├── processed/# 已整理相册
│   │   │   │   │   ├── random/   # 随机清理
│   │   │   │   │   ├── settings/ # 设置
│   │   │   │   │   └── trash/    # 回收站
│   │   │   │   └── theme/        # 主题
│   │   │   └── util/             # 工具类
│   │   │       ├── log/          # 日志系统
│   │   │       ├── LivePhotoDetector.kt
│   │   │       ├── LivePhotoExtractor.kt
│   │   │       └── ActivityProvider.kt
│   │   ├── res/                  # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── ios/                          # iOS 应用
│   └── GalleryCleaner/
│       ├── App/                  # 应用入口
│       ├── Models/               # 数据模型
│       ├── Services/             # 服务层
│       ├── ViewModels/           # 视图模型
│       ├── Views/                # 视图
│       ├── Theme/                # 主题
│       ├── Utils/                # 工具类
│       └── Navigation/           # 导航
├── build.bat                     # 一键编译脚本
├── build.gradle.kts              # 项目级 Gradle 配置
├── settings.gradle.kts
├── DESIGN.md                     # 设计文档
├── PLAN.md                       # 实施计划
└── CHANGELOG.md                  # 更新日志
```

---

## 4. 核心模块详解

### 4.1 依赖注入模块 (di/)

**职责：** 管理应用所有依赖的创建和注入

#### DataModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
```

**绑定关系：**
- `MediaRepository` → `MediaRepositoryImpl`
- `ThemeRepository` → `ThemeRepositoryImpl`

#### DispatcherModule.kt

提供协程调度器依赖：
- `@IoDispatcher` → `Dispatchers.IO`
- `@DefaultDispatcher` → `Dispatchers.Default`
- `@MainDispatcher` → `Dispatchers.Main`

---

### 4.2 数据层 (data/)

#### MediaStoreDataSource.kt

**职责：** 封装 Android MediaStore 查询逻辑

**核心方法：**

| 方法 | 说明 |
|------|------|
| `queryMediaBatched(): Flow<List<MediaItem>>` | 分批加载媒体，支持大数据量 |
| `queryMedia(): Flow<List<MediaItem>>` | 一次性查询所有媒体 |
| `getRandomMediaItems(excludedUris, sampleSize)` | 随机采样媒体（水库抽样算法） |
| `getMediaItemsByUris(uris)` | 根据 URI 列表查询媒体 |
| `getMediaItemByUri(uri)` | 根据 URI 查询单个媒体 |
| `getMediaItemById(id)` | 根据 ID 查询单个媒体 |
| `groupByMonth(items)` | 按月份分组 |
| `queryTrashedMedia()` | 查询回收站中的媒体 |

**关键特性：**
- 使用 `IS_TRASHED = 0` 过滤已删除项
- 支持 API 33+ 细粒度权限和 API 14+ 部分照片访问
- 分批加载优化（BATCH_SIZE = 600）
- 时间戳解析：优先使用 DATE_TAKEN，回退到 EXIF，最后使用 DATE_ADDED

#### MediaRepositoryImpl.kt

**职责：** MediaRepository 接口实现，协调数据源和已整理存储

**核心方法：**

| 方法 | 说明 |
|------|------|
| `getMediaGroups()` | 获取按月分组的活跃媒体 |
| `getProcessedMediaGroups()` | 获取已整理分组的媒体 |
| `getMediaItemsByMonth(yearMonth)` | 获取指定月份的媒体 |
| `moveToTrash(uris)` | 移入系统回收站（API 30+） |
| `restoreFromTrash(uris)` | 从回收站恢复 |
| `deletePermanently(uris)` | 永久删除 |
| `getTrashedMediaItems()` | 获取回收站中的媒体 |

**删除策略：**
```
API 30+ (Android 11+): MediaStore.createTrashRequest() → 系统回收站
API 26-29: ContentResolver.delete() → 永久删除
```

#### ProcessedMediaStore.kt

**职责：** 管理已整理（保留）的媒体 URI 持久化

**存储方式：** DataStore Preferences

**核心方法：**
- `markProcessed(uris)` - 标记为已整理
- `unmarkProcessed(uris)` - 取消已整理标记
- `getProcessedUris()` - 获取已整理 URI 集合
- `observeProcessedUris()` - 观察已整理 URI 变化
- `clearProcessed()` - 清空已整理记录

---

### 4.3 领域层 (domain/)

#### 数据模型

##### MediaItem.kt

```kotlin
data class MediaItem(
    val id: Long,                    // 媒体 ID
    val uri: Uri,                    // 内容 URI
    val name: String,                // 文件名
    val mimeType: String,            // MIME 类型
    val dateAdded: Long,             // 添加时间（秒）
    val size: Long,                  // 文件大小（字节）
    val duration: Long? = null,      // 视频时长（毫秒）
    val width: Int = 0,              // 宽度
    val height: Int = 0,             // 高度
    val livePhotoInfo: LivePhotoInfo, // Live 图信息
    val isQueuedForDelete: Boolean = false // 是否已加入删除队列
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isLivePhoto: Boolean get() = livePhotoInfo.type != LivePhotoType.NONE && !isVideo
}
```

##### MediaGroup.kt

```kotlin
data class MediaGroup(
    val yearMonth: YearMonth,        // 年月
    val label: String,               // 显示标签（如 "2025年3月"）
    val mediaItems: List<MediaItem>, // 媒体列表
    val thumbnail: MediaItem? = null // 缩略图
)
```

##### DeleteQueue.kt

```kotlin
data class DeleteQueue(
    val items: List<MediaItem>,      // 队列中的项
    val currentMonth: YearMonth?,    // 当前月份
    val monthTotalCount: Int = 0     // 当月总数
) {
    companion object { const val MAX_QUEUE_SIZE = 50 }
    val shouldPromptDelete: Boolean
        get() = items.size >= MAX_QUEUE_SIZE || (monthTotalCount > 0 && items.size >= monthTotalCount)
}
```

##### LivePhotoType.kt

```kotlin
enum class LivePhotoType {
    NONE,           // 不是动态照片
    GOOGLE_PIXEL,   // Google Pixel Motion Photo
    XIAOMI,         // 小米 MicroVideo
    OPPO,           // OPPO O Live Photo
    VIVO,           // vivo 动态照片
    HUAWEI,         // 华为/荣耀动态照片
    APPLE           // 苹果 Live Photo
}
```

##### LivePhotoInfo.kt

```kotlin
data class LivePhotoInfo(
    val type: LivePhotoType = LivePhotoType.NONE,
    val videoOffset: Long = 0,           // 视频起始偏移量
    val videoLength: Long = 0,           // 视频长度（字节）
    val pairedVideoUri: Uri? = null      // 配对视频文件 URI
)
```

#### Use Cases

##### GetMediaGroupsUseCase.kt

```kotlin
class GetMediaGroupsUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaGroup>> = repository.getMediaGroups()
}
```

##### DeleteMediaUseCase.kt

```kotlin
class DeleteMediaUseCase @Inject constructor(
    val repository: MediaRepository
) {
    suspend operator fun invoke(uris: List<Uri>): TrashResult {
        return if (repository.isTrashSupported()) {
            repository.moveToTrash(uris)
        } else {
            when (val result = repository.deletePermanently(uris)) {
                is DeleteResult.Success -> TrashResult.Success(result.deletedCount)
                is DeleteResult.Error -> TrashResult.Error(result.message)
            }
        }
    }
}
```

#### 撤销管理 (undo/)

##### UndoManager.kt

```kotlin
class UndoManager(private val maxHistory: Int = 10) {
    private val history = ArrayDeque<UndoCommand>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    fun push(command: UndoCommand)
    fun undo()
    fun clear()
}
```

---

### 4.4 表现层 (ui/)

#### 导航 (navigation/)

##### Routes.kt

```kotlin
object Routes {
    const val GALLERY = "gallery"
    const val PROCESSED_GALLERY = "processed_gallery"
    const val MEDIA_SWIPE = "media_swipe/{yearMonth}"
    const val PROCESSED_MONTH = "processed_month/{yearMonth}"
    const val PREVIEW = "preview/{mediaId}"
    const val RANDOM_CLEANUP = "random_cleanup"
    const val TRASH = "trash"
    const val THEME_SETTINGS = "theme_settings"

    fun mediaSwipe(yearMonth: String) = "media_swipe/$yearMonth"
    fun processedMonth(yearMonth: String) = "processed_month/$yearMonth"
    fun preview(mediaId: Long) = "preview/$mediaId"
}
```

##### NavGraph.kt

**导航结构：**
```
GalleryScreen (首页)
├── MediaSwipeScreen (月份滑动浏览)
│   └── PreviewScreen (全屏预览)
├── ProcessedGalleryScreen (已整理相册)
│   └── ProcessedMonthScreen (已整理月份)
│       └── PreviewScreen
├── RandomCleanupScreen (随机清理)
│   └── PreviewScreen
├── TrashScreen (回收站)
└── ThemeSettingsScreen (主题设置)
```

**转场动画：**
- 默认：水平滑动（spring 动画）
- 预览页：fadeIn + slideInHorizontally

#### 权限处理 (permission/)

##### PermissionHandler.kt

**权限策略：**

| API 级别 | 权限 |
|----------|------|
| API 34+ | `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO` + `READ_MEDIA_VISUAL_USER_SELECTED` |
| API 33 | `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO` |
| API 32- | `READ_EXTERNAL_STORAGE` |

**特性：**
- 首次进入自动请求权限
- 从系统设置返回时自动检测权限变化
- vivo 设备特殊处理（300ms 延迟）
- 部分照片访问权限检测

#### 页面 (screen/)

##### GalleryScreen.kt - 月份浏览主页

**布局结构：**
```
GlassTopBar (顶部栏)
├── 标题："相册清理"
├── 主题设置按钮
├── 回收站按钮
└── 项目总数

LazyColumn (月份列表)
├── GalleryHeroPanel (统计面板)
│   ├── 总项目数 / 月份数 / 月均
│   ├── "已整理" 按钮
│   └── "随机清理" 按钮
└── MonthCard (月份卡片) × N
    ├── 月份标签 + 项目数
    └── 缩略图网格 (3×2)
```

**状态管理：**
```kotlin
data class GalleryUiState(
    val groups: List<MediaGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val error: String? = null
)
```

##### MediaSwipeScreen.kt - 媒体滑动浏览

**布局结构：**
```
GlassTopBar
├── 返回按钮
├── 月份标题
└── 已选计数 (x/50)

SwipeStatusStrip (状态条)
├── 当前进度 (第 x / 总张)
└── 待删计数

LinearProgressIndicator (进度条)

HorizontalPager (媒体浏览)
├── SwipeableMediaCard × N
│   ├── 媒体内容（图片/视频）
│   ├── 上滑删除手势
│   ├── 点击中心预览
│   └── 缩放支持（图片）
└── MonthEndPageContent (月份结束页)
    ├── 统计信息
    ├── "确认删除" 按钮
    └── "返回首页" 按钮

DeleteQueueBar (底部栏)
├── 已选计数
├── 进度条
├── 撤销按钮
└── "确认删除" 按钮
```

**手势交互：**
| 手势 | 动作 |
|------|------|
| 上滑 | 加入删除队列 |
| 左划/右划 | 切换上一张/下一张 |
| 点击中心 | 进入全屏预览 |
| 双指缩放 | 缩放图片（仅图片） |

##### PreviewScreen.kt - 全屏预览

**媒体类型处理：**
| 类型 | 组件 | 交互 |
|------|------|------|
| 普通图片 | `ZoomablePreviewImage` | 双指缩放、双击放大 |
| 普通视频 | `VideoPlayer` | 长按 2x 加速 |
| Live 图 | `MotionPhotoPlayer` | 长按播放嵌入视频 |

**布局：**
```
GlassTopBar
├── 返回按钮
├── 标题 + 提示
└── 信息切换按钮

媒体内容区
├── 图片/视频/Live 图
└── 信息栏（文件名、大小、日期）
```

##### SwipeableMediaCard.kt - 可滑动媒体卡片

**手势实现：**
```kotlin
// 上滑检测
detectVerticalDragGestures(
    onDragStart = { targetScale = 0.98f },
    onVerticalDrag = { change, dragAmount ->
        // 更新偏移量和透明度
    },
    onDragEnd = {
        if (offsetY.value < -SWIPE_THRESHOLD) {
            onSwipeUp() // 触发删除
        } else {
            // 回弹动画
        }
    }
)

// 点击检测
detectTapGestures(
    onTap = { if (!isPhotoZoomed) onTap() }
)
```

**视觉反馈：**
- 上滑时背景渐显红色删除提示
- 前景内容层随手指上移
- 缩放动画（0.98f → 1.0f）

##### VideoPlayer.kt - 视频播放器

**长按加速实现：**
```kotlin
Modifier.pointerInput(Unit) {
    detectTapGestures(
        onLongPress = { isSpeedingUp = true },
        onPress = {
            try { awaitRelease() }
            finally { isSpeedingUp = false }
        }
    )
}
```

**速度指示器：**
- 长按期间显示 "2x" 标签
- 使用 AnimatedVisibility + fadeIn/scaleIn 动画

##### MotionPhotoPlayer.kt - Live 图播放器

**多厂商支持：**

| 厂商 | 检测方式 | 播放方案 |
|------|----------|----------|
| Google Pixel | XMP `GCamera:MotionPhoto` | 提取嵌入视频 |
| 小米 | XMP `GCamera:MicroVideo` | 按偏移量提取 |
| OPPO | XMP `OpCamera:MotionPhotoOwner` | 提取嵌入视频 |
| vivo | 文件末尾 `vivo{` JSON | 播放配对 .mp4 |
| 华为/荣耀 | 文件末尾 `LIVE_` 标识 | 按长度提取 |
| Apple | 配对 .mov 文件 | 播放配对 .mov |

**播放逻辑：**
1. 默认显示静态图片（Coil AsyncImage）
2. 长按触发视频播放
3. 松开停止播放，回到静态图

#### 组件 (component/)

##### GlassComponents.kt - 玻璃拟态组件

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppShape.Medium,
    contentPadding: PaddingValues = AppPadding.MD,
    content: @Composable ColumnScope.() -> Unit
)

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

@Composable
fun GlassBottomBar(...)
```

##### MediaThumbnail.kt - 媒体缩略图

```kotlin
@Composable
fun MediaThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isVideo: Boolean = false
)
```

##### Animations.kt - 动画系统

```kotlin
object Motion {
    object Enter {
        fun fadeIn(duration: Int = 280)
        fun slideInFromBottom()
        fun slideInFromLeft()
        fun slideInFromRight()
        fun scaleIn(from: Float = 0.92f)
    }
    object Exit {
        fun fadeOut(duration: Int = 220)
        fun slideOutToBottom()
        fun slideOutToLeft()
        fun slideOutToRight()
        fun scaleOut(to: Float = 0.92f)
    }
    object Duration {
        const val Fast = 120
        const val Normal = 280
        const val Slow = 450
    }
}
```

#### 主题 (theme/)

##### Color.kt - 颜色系统

```kotlin
@Immutable
data class AppColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceOverlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val destructive: Color,
    val destructiveDim: Color,
    val success: Color,
    val warning: Color,
    val separator: Color,
    val separatorOpaque: Color,
    val ripple: Color
)
```

**主题色生成：**
- 基于主色调（primary）自动生成完整的颜色方案
- 使用 HSL 色彩空间计算变体

##### Theme.kt - 主题配置

```kotlin
@Composable
fun GalleryCleanerTheme(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val themeConfig by viewModel.themeConfig.collectAsState()
    val colorScheme = appColorScheme(themeConfig.primaryColor)
    val darkScheme = buildDarkScheme(colorScheme)
    // ...
}
```

---

### 4.5 工具类 (util/)

#### LivePhotoDetector.kt - Live 图检测

**分层检测策略：**

```
检测流程（按优先级）：
1. XMP 元数据检测
   ├── GCamera:MotionPhoto="1" → Google Pixel / OPPO
   ├── GCamera:MicroVideo="1" → 小米
   └── OpCamera:MotionPhotoOwner="oplus" → OPPO

2. 文件末尾标识检测
   ├── "vivo{" JSON 字符串 → vivo
   └── "LIVE_" 标识 → 华为/荣耀

3. 配对视频文件检测
   ├── 同名 .mp4 文件 → vivo
   └── 同名 .mov 文件 → Apple

4. 视频签名回退检测
   └── 搜索 ftyp/moov 签名 → 通用 Motion Photo
```

**核心方法：**
```kotlin
fun detect(context: Context, uri: Uri): LivePhotoInfo
private fun detectByXMP(filePath: String): LivePhotoInfo?
private fun detectByFooter(filePath: String): LivePhotoInfo?
private fun detectByPairedVideo(context: Context, imageUri: Uri): LivePhotoInfo?
```

#### LivePhotoExtractor.kt - 视频提取

**提取策略：**

| 厂商 | 方法 |
|------|------|
| 小米 | 按 `MicroVideoOffset` 偏移量提取 |
| Google Pixel / OPPO | 按文件末尾偏移量提取 |
| 华为/荣耀 | 按 `LIVE_` 后的长度从文件末尾提取 |
| vivo / Apple | 直接复制配对视频文件 |

**回退方案：**
- 当 `videoLength` 未知时，搜索文件末尾 1MB 区域中的 `ftyp` 签名

#### 日志系统 (log/)

**日志级别：**
```kotlin
enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
}
```

**日志类型：**
- `AppLogger.enter(TAG, method, params...)` - 方法进入
- `AppLogger.exit(TAG, method, result)` - 方法退出
- `AppLogger.d(TAG, message)` - 调试
- `AppLogger.i(TAG, message)` - 信息
- `AppLogger.w(TAG, message)` - 警告
- `AppLogger.e(TAG, message, throwable)` - 错误
- `AppLogger.exception(TAG, message, throwable)` - 异常
- `AppLogger.perf(TAG, operation, startTime)` - 性能
- `AppLogger.userAction(TAG, action, params...)` - 用户行为
- `AppLogger.dataChange(TAG, change, details)` - 数据变化

---

## 5. 数据流

### 5.1 媒体加载流程

```
GalleryScreen
    ↓
GalleryViewModel.loadMediaGroups()
    ↓
GetMediaGroupsUseCase()
    ↓
MediaRepositoryImpl.getMediaGroups()
    ↓
MediaStoreDataSource.queryMediaBatched()
    ↓ (分批返回)
MediaStore.Images.Media.EXTERNAL_CONTENT_URI
MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ↓
Flow<List<MediaItem>>
    ↓ (按月分组)
MediaStoreDataSource.groupByMonth()
    ↓
Flow<List<MediaGroup>>
    ↓
GalleryUiState.groups
    ↓
LazyColumn 渲染 MonthCard
```

### 5.2 删除流程

```
用户上滑
    ↓
SwipeableMediaCard.onSwipeUp()
    ↓
MediaSwipeViewModel.queueForDelete(item)
    ↓
_deleteQueueItems.add(item)
    ↓
DeleteQueue.shouldPromptDelete?
    ↓ (是)
显示删除确认对话框
    ↓
用户确认
    ↓
CleanupViewModel.confirmDelete()
    ↓
DeleteMediaUseCase(uris)
    ↓
API 30+: MediaStore.createTrashRequest() → IntentSender
API 26-29: ContentResolver.delete()
    ↓
onActivityResult(resultCode)
    ↓
CleanupViewModel.onTrashIntentResult(success, count)
    ↓
更新 UI 状态
```

### 5.3 Live 图播放流程

```
用户长按 Live 图
    ↓
MotionPhotoPlayer
    ↓
prepareVideoUri(context, mediaItem)
    ↓
LivePhotoExtractor.extractVideo(context, mediaItem, outputFile)
    ↓
根据 livePhotoInfo.type 选择提取策略:
├── XIAOMI: extractXiaomiVideo() → 按偏移量提取
├── GOOGLE_PIXEL/OPPO: extractEmbeddedVideo() → 按文件末尾提取
├── HUAWEI: extractHuaweiVideo() → 按 LIVE_ 长度提取
└── VIVO/APPLE: copyPairedVideo() → 复制配对文件
    ↓
Uri.fromFile(tempFile)
    ↓
ExoPlayer 播放
    ↓
用户松开手指 → 停止播放
```

---

## 6. 关键类与函数

### 6.1 Application 入口

#### GalleryApp.kt

```kotlin
@HiltAndroidApp
class GalleryApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        ActivityProvider.init(this)
        AppLogger.init(context = this, level = LogLevel.DEBUG)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
```

### 6.2 Activity 入口

#### MainActivity.kt

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val REQUEST_CODE_TRASH = 1001
        const val REQUEST_CODE_DELETE = 1002
        const val REQUEST_CODE_RESTORE = 1003
        var pendingTrashUris: List<Uri> = emptyList()
        var onTrashResult: ((Boolean, Int) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContent {
            GalleryCleanerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_TRASH -> {
                val success = resultCode == RESULT_OK
                onTrashResult?.invoke(success, count)
            }
            // ...
        }
    }
}
```

### 6.3 ViewModel 层次结构

```
CleanupViewModel (抽象基类)
├── MediaSwipeViewModel (月份滑动浏览)
└── RandomCleanupViewModel (随机清理)

GalleryViewModel (月份浏览主页)
PreviewViewModel (全屏预览)
ProcessedGalleryViewModel (已整理相册)
ProcessedMonthViewModel (已整理月份)
TrashViewModel (回收站)
ThemeViewModel (主题设置)
```

**CleanupViewModel 核心方法：**
```kotlin
fun queueForDelete(item: MediaItem)      // 加入删除队列
fun keepCurrent(silent: Boolean = false)  // 保留当前项
fun undo()                                // 撤销
fun confirmDelete()                        // 确认删除
fun onBackPressed()                        // 返回处理
fun onTrashIntentResult(success, count)    // 删除结果回调
```

---

## 7. 依赖关系

### 7.1 模块依赖图

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  Screen → ViewModel → UseCase → Repository Interface         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│  UseCase → Repository Interface ← Domain Model               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                            │
│  Repository Impl → DataSource / DataStore                    │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Hilt 依赖注入图

```
@HiltAndroidApp
    ↓
@AndroidEntryPoint (MainActivity)
    ↓
@HiltViewModel (ViewModels)
    ↓
@Inject constructor
    ├── MediaRepository (DataModule)
    ├── DeleteMediaUseCase
    ├── ProcessedMediaStore
    ├── @IoDispatcher (DispatcherModule)
    └── SavedStateHandle
```

### 7.3 外部依赖

| 依赖 | 用途 |
|------|------|
| `androidx.compose:compose-bom` | Compose UI 框架 |
| `com.google.dagger:hilt-android` | 依赖注入 |
| `io.coil-kt:coil-compose` | 图片加载 |
| `androidx.media3:media3-exoplayer` | 视频播放 |
| `androidx.exifinterface:exifinterface` | EXIF/XMP 元数据解析 |
| `androidx.datastore:datastore-preferences` | 偏好设置持久化 |
| `androidx.navigation:navigation-compose` | 页面导航 |

---

## 8. 构建与运行

### 8.1 环境要求

- **JDK**: 17 或更高版本
- **Android SDK**: compileSdk 35, minSdk 26
- **Android Gradle Plugin**: 8.5.2
- **Kotlin**: 1.9.22

### 8.2 构建命令

#### 使用 build.bat（推荐）

```bash
# 编译 Debug APK
build.bat

# 编译 Release APK
build.bat release

# 编译并安装到设备
build.bat debug --install

# 增量编译（跳过 clean）
build.bat debug --no-clean
```

#### 使用 Gradle 命令

```bash
# 清理
./gradlew clean

# 编译 Debug
./gradlew assembleDebug

# 编译 Release
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

### 8.3 输出位置

- **APK 文件**: `app/build/outputs/apk/{type}/`
- **复制到项目根目录**: `GalleryCleaner-{version}-{type}-{date}.apk`

### 8.4 iOS 构建

1. 用 Xcode 打开 `ios/GalleryCleaner.xcodeproj`
2. 选择模拟器或真机
3. Build & Run (⌘+R)

### 8.5 调试技巧

**日志过滤：**
```bash
# 查看所有日志
adb logcat -s GalleryApp:* MediaStoreDS:* MediaRepoImpl:*

# 查看 Live Photo 相关
adb logcat -s LivePhotoDetector:* LivePhotoExtractor:* MotionPhotoPlayer:*

# 查看用户行为
adb logcat | grep "userAction"
```

---

## 9. 版本历史

| 版本 | 日期 | 核心变更 |
|------|------|----------|
| 1.7.2 | 2026-06-24 | 随机清理确认删除后页面跳转修复 |
| 1.5.0 | 2026-06-12 | UI/UX 全面优化；动画系统增强；并发安全修复 |
| 1.4.10 | 2026-06-05 | Snackbar 回调优化；页面转场动画改为水平滑动 |
| 1.3.0 | 2026-06-04 | 已整理相册为空修复；保留按钮；Live Photo 日志增强 |
| 1.2.0 | 2026-06-02 | 左滑保留不立即持久化；Live Photo URI 流检测 |
| 1.1.0 | 2026-05-15 | 修复 vivo Android 15 媒体读取、权限回调 |
| 1.0.0 | 2025-05-15 | 项目初始化，完整实现核心功能 |

---

## 附录

### A. 权限清单

| 权限 | API 级别 | 用途 |
|------|----------|------|
| `READ_MEDIA_IMAGES` | 33+ | 读取图片 |
| `READ_MEDIA_VIDEO` | 33+ | 读取视频 |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 34+ | 部分照片访问 |
| `ACCESS_MEDIA_LOCATION` | - | 读取媒体位置信息 |
| `READ_EXTERNAL_STORAGE` | ≤32 | 读取外部存储 |
| `WRITE_EXTERNAL_STORAGE` | ≤28 | 写入外部存储 |
| `MANAGE_MEDIA` | - | 媒体管理（vivo 需要） |

### B. Live 图厂商格式对比

| 厂商 | 封装方式 | 元数据方式 | 检测方法 |
|------|----------|------------|----------|
| Google Pixel | 单文件 (JPG+MP4) | XMP MotionPhoto | XMP 属性 |
| 小米 | 单文件 (JPG+MP4) | XMP MicroVideo | XMP 属性 |
| OPPO | 单文件 (JPG+MP4) | XMP MotionPhoto + OpCamera | XMP 属性 |
| vivo | 双文件 (JPG+MP4) | 文件末尾 JSON | 文件末尾标识 + 配对文件 |
| 华为/荣耀 | 单文件 (JPG+视频) | 文件末尾标识 | 文件末尾标识 |
| Apple | 双文件 (HEIC+MOV) | EXIF MediaGroupUUID | 配对文件 |

### C. 状态机

**删除队列状态：**
```
[空队列] → [添加项] → [队列中] → [达到阈值/月份结束] → [确认删除] → [执行删除] → [空队列]
                  ↑__________________________________________|
                              (撤销操作)
```

**媒体项状态：**
```
[活跃] → [加入删除队列] → [隐藏] → [删除成功] → [已删除]
   ↓
[保留] → [标记为已整理] → [从活跃列表移除]
```

---

*文档版本: 1.0*
*最后更新: 2026-07-27*
