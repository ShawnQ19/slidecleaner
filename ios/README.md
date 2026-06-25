# GalleryCleaner iOS

相册清理 App iOS 版，使用 SwiftUI 构建。

## 技术栈

- SwiftUI (声明式 UI)
- Photos Framework (媒体访问)
- AVKit (视频播放)
- UserDefaults (持久化)

## 系统要求

- iOS 16.0+
- Xcode 15.0+

## 功能

- **按月浏览**: 相册按月份分组展示
- **滑动清理**: 上滑删除，左滑保留
- **随机清理**: 随机抽取照片进行清理
- **系统回收站**: 删除移入系统回收站，30天保护
- **回收站管理**: 多选恢复、永久删除、清空
- **全屏预览**: 照片缩放、视频播放、Live Photo
- **主题定制**: 8种预设颜色 + 自定义颜色
- **已整理归档**: 已处理照片归档管理

## 项目结构

```
GalleryCleaner/
├── App/           # 应用入口
├── Models/        # 数据模型
├── Services/      # 数据服务层
├── ViewModels/    # 视图模型
├── Views/         # 界面
│   ├── Gallery/   # 首页
│   ├── Cleanup/   # 滑动清理
│   ├── Random/    # 随机清理
│   ├── Trash/     # 回收站
│   ├── Preview/   # 预览
│   ├── Processed/ # 已整理
│   ├── Settings/  # 设置
│   └── Components/# 通用组件
├── Theme/         # 主题系统
├── Utils/         # 工具类
└── Navigation/    # 导航路由
```

## 使用方法

1. 用 Xcode 打开 `ios/GalleryCleaner.xcodeproj`
2. 选择模拟器或真机
3. Build & Run

## 注意事项

- 需要在 Info.plist 中添加相册权限描述
- 删除操作需要用户确认（系统级弹窗）
- Live Photo 在模拟器上不可用
