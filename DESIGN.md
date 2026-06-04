# Android 照片/视频清理画廊应用 - 设计文档

## 1. 项目概述

### 1.1 应用名称
相册清理 (Gallery Cleaner)

### 1.2 核心功能
- 按月浏览设备中的照片和视频
- 通过滑动手势快速整理媒体文件
- 批量删除并移入系统回收站（30天自动清理）
- 支持视频长按加速播放
- 支持多厂商 Live 图（Motion Photo）播放

### 1.3 目标用户
- 手机存储空间紧张的用户
- 需要定期整理照片视频的用户
- 希望快速清理无用媒体文件的用户

---

## 2. 功能规格

### 2.1 月份浏览主屏 (GalleryScreen)

**功能描述**
- 以卡片形式展示按月份分组的照片和视频
- 每张卡片显示月份名称、媒体数量、缩略图预览

**交互设计**
- 点击月份卡片进入该月份的媒体滑动浏览
- 支持下拉刷新

**UI 布局**
```
┌─────────────────────────────┐
│ 相册清理          [设置]    │  ← TopAppBar
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ 2025年3月        128项  │ │  ← MonthCard
│ │ ┌──┬──┬──┐             │ │
│ │ │  │  │  │  +122       │ │  ← 缩略图网格 (最多6张)
│ │ ├──┼──┼──┤             │ │
│ │ │  │  │  │             │ │
│ │ └──┴──┴──┘             │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ 2025年2月         86项  │ │
│ │ ...                    │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

### 2.2 媒体滑动浏览 (MediaSwipeScreen)

**功能描述**
- 进入某个月后，全屏浏览该月的照片和视频
- 通过手势操作管理媒体文件

**手势操作**
| 手势 | 动作 | 结果 |
|------|------|------|
| 上滑 | 将当前媒体加入删除队列 | 红色删除提示，松手后入队 |
| 左划 | 切换到下一张 | HorizontalPager 翻页 |
| 右划 | 切换到上一张 | HorizontalPager 翻页 |
| 点击中心 | 进入全屏预览 | 跳转 PreviewScreen |

**删除队列机制**
- 队列最大容量：50 项
- 触发删除确认条件：
  1. 队列达到 50 项
  2. 当前月份所有媒体已浏览完毕
- 删除方式：
  - API 30+：移入系统回收站（30天后自动清理）
  - API 26-29：直接永久删除

**UI 布局**
```
┌─────────────────────────────┐
│ ←  2025年3月    已选 23/50  │  ← TopAppBar + 队列计数
├─────────────────────────────┤
│ ███████████████████████████ │  ← 进度条
├─────────────────────────────┤
│                             │
│                             │
│      [媒体内容]              │  ← HorizontalPager
│                             │
│    (点击中心预览)            │
│                             │
│    ↑ 上滑删除               │
│                             │
├─────────────────────────────┤
│ 已选 23 项 ████████░░░░░░░ │  ← DeleteQueueBar
│ [确认删除]                  │
└─────────────────────────────┘
```

### 2.3 全屏预览 (PreviewScreen)

**功能描述**
- 点击媒体中心区域进入全屏预览
- 根据媒体类型提供不同的预览方式

**媒体类型处理**
| 类型 | 预览方式 | 交互 |
|------|----------|------|
| 普通图片 | Coil 高清加载 | 双指缩放 |
| 普通视频 | ExoPlayer 播放 | 长按 2x 加速 |
| Live 图 | 静态图 + 长按播放视频 | 长按播放嵌入视频 |

**视频长按加速**
- 长按屏幕：播放速度变为 2x
- 松开手指：恢复 1x 正常速度
- 显示速度指示器覆盖层：快进图标 + "2x" + "松开恢复正常"

**Live 图播放**
- 默认显示静态图片
- 长按播放嵌入视频
- 松开停止播放，回到静态图
- 右下角显示 Live 图标识

**UI 布局**
```
┌─────────────────────────────┐
│ ← 预览                      │  ← TopAppBar
├─────────────────────────────┤
│                             │
│                             │
│                             │
│      [媒体内容]              │  ← 全屏显示
│      (可缩放/播放)          │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│ 文件名.jpg                  │  ← 底部信息栏
│ 2.5 MB  2025-03-15 14:30   │
└─────────────────────────────┘
```

---

## 3. 技术架构

### 3.1 架构模式
**MVVM + Clean Architecture**

```
┌─────────────────────────────────────────────┐
│                  UI 层 (Presentation)        │
│  Screen Composables ← ViewModel ← UiState    │
├─────────────────────────────────────────────┤
│                 Domain 层                    │
│  Repository 接口 / UseCase / Domain Model    │
├─────────────────────────────────────────────┤
│                 Data 层                      │
│  Repository 实现 / MediaStore 查询 / 数据源   │
└─────────────────────────────────────────────┘
```

### 3.2 技术栈

| 组件 | 技术选择 | 版本 |
|------|----------|------|
| 编程语言 | Kotlin | 1.9.25 |
| UI 框架 | Jetpack Compose | BOM 2024.12.01 |
| 设计系统 | Material 3 | - |
| 依赖注入 | Hilt | 2.53.1 |
| 图片加载 | Coil 3 | 3.4.0 |
| 视频播放 | Media3 ExoPlayer | 1.5.1 |
| 元数据解析 | ExifInterface | 1.3.7 |
| 最低 API | Android 8.0 | API 26 |
| 目标 API | Android 15 | API 35 |

### 3.3 核心依赖

```kotlin
// Compose
implementation(platform("androidx.compose:compose-bom:2024.12.01"))
implementation("androidx.compose.material3:material3")

// Hilt
implementation("com.google.dagger:hilt-android:2.53.1")
kapt("com.google.dagger:hilt-compiler:2.53.1")

// Coil 3
implementation("io.coil-kt.coil3:coil-compose:3.4.0")

// Media3
implementation("androidx.media3:media3-exoplayer:1.5.1")
implementation("androidx.media3:media3-ui:1.5.1")

// ExifInterface
implementation("androidx.exifinterface:exifinterface:1.3.7")
```

---

## 4. 数据模型

### 4.1 LivePhotoType (Live 图类型枚举)
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

### 4.2 LivePhotoInfo (Live 图信息)
```kotlin
data class LivePhotoInfo(
    val type: LivePhotoType = LivePhotoType.NONE,
    val videoOffset: Long = 0,           // 视频起始偏移量
    val videoLength: Long = 0,           // 视频长度
    val pairedVideoUri: Uri? = null      // 配对视频文件 URI
)
```

### 4.3 MediaItem (媒体项)
```kotlin
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAdded: Long,
    val size: Long,
    val duration: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val livePhotoInfo: LivePhotoInfo = LivePhotoInfo(),
    val isQueuedForDelete: Boolean = false
)
```

### 4.4 MediaGroup (月份分组)
```kotlin
data class MediaGroup(
    val yearMonth: YearMonth,
    val label: String,
    val mediaItems: List<MediaItem>,
    val thumbnail: MediaItem? = null
)
```

### 4.5 DeleteQueue (删除队列)
```kotlin
data class DeleteQueue(
    val items: List<MediaItem> = emptyList(),
    val currentMonth: YearMonth? = null,
    val monthTotalCount: Int = 0
) {
    companion object { const val MAX_QUEUE_SIZE = 50 }
    val shouldPromptDelete: Boolean 
        get() = items.size >= MAX_QUEUE_SIZE || items.size >= monthTotalCount
}
```

---

## 5. Live 图多厂商兼容方案

### 5.1 各厂商格式对比

| 厂商 | 封装方式 | 元数据方式 | 检测方法 |
|------|----------|------------|----------|
| Google Pixel | 单文件 (JPG+MP4) | XMP MotionPhoto | XMP 属性 |
| 小米 | 单文件 (JPG+MP4) | XMP MicroVideo | XMP 属性 |
| OPPO | 单文件 (JPG+MP4) | XMP MotionPhoto + OpCamera | XMP 属性 |
| vivo | 双文件 (JPG+MP4) | 文件末尾 JSON | 文件末尾标识 + 配对文件 |
| 华为/荣耀 | 单文件 (JPG+视频) | 文件末尾标识 | 文件末尾标识 |
| Apple | 双文件 (HEIC+MOV) | EXIF MediaGroupUUID | 配对文件 |

### 5.2 分层检测策略

```
检测流程:
1. XMP 元数据检测 (优先级 1)
   ├── GCamera:MotionPhoto="1" → Google Pixel / OPPO
   ├── GCamera:MicroVideo="1" → 小米
   └── OpCamera:MotionPhotoOwner="oplus" → OPPO

2. 文件末尾标识检测 (优先级 2)
   ├── "vivo{" JSON 字符串 → vivo
   └── "LIVE_" 标识 → 华为/荣耀

3. 配对视频文件检测 (优先级 3)
   ├── 同名 .mp4 文件 → vivo
   └── 同名 .mov 文件 → Apple

4. 视频签名回退检测 (优先级 4)
   └── 搜索 ftyp/moov 签名 → 通用 Motion Photo
```

### 5.3 播放方案

| 厂商 | 播放方式 |
|------|----------|
| Google Pixel / 小米 / OPPO | Media3 直接播放 JPEG URI 或手动提取后播放 |
| vivo | 播放同目录同名 .mp4 文件 |
| 华为/荣耀 | 手动提取嵌入式视频后播放 |
| Apple | 播放同目录同名 .mov 文件 |

---

## 6. 权限设计

### 6.1 权限声明

```xml
<!-- API 33+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- API 32 及以下 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

### 6.2 权限处理
- 启动时检查权限状态
- 未授权时显示权限说明页面
- 引导用户授予权限或前往设置

---

## 7. 导航设计

### 7.1 路由定义
```kotlin
object Routes {
    const val GALLERY = "gallery"
    const val MEDIA_SWIPE = "media_swipe/{yearMonth}"
    const val PREVIEW = "preview/{mediaId}"
}
```

### 7.2 导航流程
```
GalleryScreen ──点击月份──→ MediaSwipeScreen ──点击中心──→ PreviewScreen
       ↑                                              │
       └──────────────── 返回 ←───────────────────────┘
```

---

## 8. 状态管理

### 8.1 GalleryUiState
```kotlin
data class GalleryUiState(
    val groups: List<MediaGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 8.2 MediaSwipeUiState
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

### 8.3 PreviewUiState
```kotlin
data class PreviewUiState(
    val mediaItem: MediaItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 9. 性能优化

### 9.1 图片加载
- Coil 3 自动处理内存和磁盘缓存
- 缩略图限制尺寸 480x480
- 使用 crossfade 动画

### 9.2 视频播放
- ExoPlayer 自动管理缓冲
- 页面切换时自动暂停/释放
- 使用 DisposableEffect 管理生命周期

### 9.3 列表优化
- LazyColumn 实现月份列表虚拟化
- HorizontalPager 实现媒体浏览页面复用
- 使用 key 参数优化重组

### 9.4 查询优化
- MediaStore 查询在 IO 线程执行
- 使用 Flow 实现响应式数据流
- 排除已回收站项目减少数据量

---

## 10. 安全与隐私

### 10.1 数据安全
- 删除操作使用系统 API（createTrashRequest/createDeleteRequest）
- 不保存用户媒体文件到应用私有目录
- 临时文件及时清理

### 10.2 隐私保护
- 仅申请必要的媒体读取权限
- 不上传用户数据到服务器
- 遵循 Google Play 照片访问权限政策

---

## 11. 兼容性

### 11.1 系统版本
- 最低支持：Android 8.0 (API 26)
- 目标版本：Android 15 (API 35)
- 回收站功能：Android 11+ (API 30)

### 11.2 设备厂商
- 支持 Google Pixel、小米、OPPO、vivo、华为、荣耀、Apple 等主流厂商
- Live 图播放适配各厂商格式差异

---

## 12. 未来扩展

### 12.1 可能的功能扩展
- 云端备份集成
- AI 智能分类（人物、地点、物体）
- 重复照片检测
- 视频压缩
- 相册密码保护

### 12.2 技术债务
- 考虑使用 Paging 3 处理大量媒体
- 考虑使用 WorkManager 处理批量删除
- 考虑添加数据库缓存层

---

*文档版本: 1.0*
*最后更新: 2025年*
