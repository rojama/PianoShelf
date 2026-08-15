# PianoShelf Code Wiki

> **便携乐谱（PianoShelf）** - 一款基于 Android 的 MusicXML 格式钢琴乐谱查看与播放应用。

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [目录结构](#3-目录结构)
4. [核心模块详解](#4-核心模块详解)
5. [关键类与函数说明](#5-关键类与函数说明)
6. [依赖关系](#6-依赖关系)
7. [数据存储设计](#7-数据存储设计)
8. [资源文件说明](#8-资源文件说明)
9. [项目运行方式](#9-项目运行方式)
10. [构建与配置](#10-构建与配置)

---

## 1. 项目概述

### 1.1 项目简介

PianoShelf 是一款运行在 Android 平台上的开源乐谱查看器，专注于 MusicXML 格式的钢琴乐谱。用户可以通过文件浏览器浏览、打开并查看乐谱，同时支持乐谱的实时播放功能。应用还提供乐谱收藏、历史记录、自定义显示颜色等增强功能。

### 1.2 核心功能

| 功能模块 | 功能描述 |
|---------|---------|
| **乐谱浏览** | 以 Tab 页形式提供文件浏览、最近打开、收藏夹三种视图 |
| **乐谱渲染** | 将 MusicXML 数据解析并绘制为可视化五线谱图像 |
| **乐谱播放** | 按时间轴逐音符播放乐谱，使用 SoundPool 播放音频采样 |
| **交互操作** | 支持双指缩放、单指拖动乐谱、翻页、横竖屏切换 |
| **收藏与历史** | 使用 SQLite 数据库存储最近打开和收藏的乐谱 |
| **个性化设置** | 支持自定义前景/背景颜色、历史记录数量等配置 |

### 1.3 技术栈

- **语言**: Java (JDK 6+)
- **平台**: Android SDK (minSdkVersion=8, target=android-19)
- **构建工具**: Apache Ant (传统 Android 构建系统)
- **UI 框架**: 原生 Android View 体系 (TabHost, ListView, ImageView 等)
- **数据存储**: SQLite (内置数据库)
- **音频播放**: Android SoundPool API
- **乐谱解析**: Xenoage Zong 库 (MusicXML 2.0 解析)

---

## 2. 整体架构

### 2.1 架构分层图

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                     │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐   │
│  │   Activity  │  │    Views      │  │ Preferences  │   │
│  │ (UI 控制器) │  │  (自定义视图) │  │   (设置项)   │   │
│  └──────┬──────┘  └───────┬───────┘  └──────┬───────┘   │
└─────────┼────────────────┼──────────────────┼───────────┘
          │                │                  │
┌─────────▼────────────────▼──────────────────▼───────────┐
│                    Business Logic Layer                  │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐   │
│  │    渲染     │  │    播放控制    │  │   数据管理    │   │
│  │ Paint* 系列 │  │  Thread 系列   │  │ Database*    │   │
│  └──────┬──────┘  └───────┬───────┘  └──────┬───────┘   │
└─────────┼────────────────┼──────────────────┼───────────┘
          │                │                  │
┌─────────▼────────────────▼──────────────────▼───────────┐
│                   Data / Input Layer                     │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐   │
│  │  FileReader │  │  SoundPool    │  │  SharedPrefs │   │
│  │  (文件读取)  │  │   (音频池)    │  │  (配置存储)  │   │
│  └─────────────┘  └───────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心流程

#### 2.2.1 乐谱打开流程

```
用户点击文件 (TabBrowseList/Recent/Favorite)
        │
        ▼
PianoShelfActivity ──► 写入数据库最近记录
        │
        ▼
Intent 启动 GraphicsActivity (传入文件 URI)
        │
        ▼
GraphicsActivity.onCreate()
  ├─ 获取文件路径
  ├─ 创建 GraphicsView 实例
  └─ 调用 graphicsView.showView()
        │
        ▼
GraphicsView.AsyncLoader (异步线程)
  ├─ FileReader.loadScores() 读取并解析 MusicXML
  ├─ 初始化 CommonTransfer 渲染上下文
  ├─ 加载 SymbolPool (乐谱符号库)
  ├─ MxlScorePartwise.paint() 逐页绘制
  │     └─ PaintTransfer 执行 Canvas 绘图
  │         ├─ 画谱号 (G/F 谱号)
  │         ├─ 画调号 (升降号)
  │         ├─ 画拍号
  │         ├─ 画小节线
  │         └─ 画音符 + 收集 Note 对象用于播放
  └─ 生成 Bitmap 乐谱图像
        │
        ▼
ViewScroll + TouchView 显示图像 (支持缩放/拖动)
```

#### 2.2.2 乐谱播放流程

```
用户点击菜单「播放」
        │
        ▼
GraphicsView.play()
  ├─ time = 0 (时间轴起点)
  ├─ maxTime = 333 (时间轴终点，初始占位值)
  ├─ TimeThread.start() ──► 每 5ms 推进 time+1
  └─ PlayThread.start()
        │
        ▼
PlayThread.run()
  └─ 遍历 scorePartsNotes (每个声部)
     └─ PlayPartThread.start()
           │
           ▼
PlayPartThread.run()
  └─ 遍历该声部所有 Note
     ├─ 更新 maxTime (取最大 duration)
     └─ PlayNoteThread.start()
           │
           ▼
PlayNoteThread.run()
  ├─ 等待: while(note.duration > time) sleep(...)
  └─ SoundPoolUtiil.playSound(rawID) 播放对应音符合成音
        │
        ▼
TimeThread 检测到 time >= maxTime
  └─ 若有下一页: dispalyPageNo++，reShowView() 并继续播放
```

---

## 3. 目录结构

```
/workspace
├── AndroidManifest.xml              # Android 清单文件 (权限、Activity 注册)
├── project.properties               # Ant 构建配置 (target=android-19)
├── proguard.cfg                     # ProGuard 混淆规则
├── README.md                        # 项目说明
│
├── src/                             # 源代码目录
│   └── com/rojama/pianoshelf/       # ===== 主应用包 =====
│       ├── PianoShelfActivity.java  # 主界面 (TabHost)
│       ├── GraphicsActivity.java    # 乐谱查看界面
│       ├── AppPreferenceActivity.java # 设置界面
│       ├── GraphicsView.java        # 乐谱核心：渲染 + 播放控制
│       ├── CommonTransfer.java      # 全局渲染上下文 / 数据传递对象
│       ├── PaintTransfer.java       # 绘图状态封装 + 基础绘图方法
│       ├── PaintUtil.java           # Paint 样式工具类
│       ├── Note.java                # 音符数据模型 (播放用)
│       ├── SoundPoolUtiil.java      # 音频池管理 (加载/播放)
│       ├── DatabaseHelper.java      # SQLite 数据库帮助类
│       ├── TabBrowseList.java       # Tab: 文件浏览器
│       ├── TabRecentList.java       # Tab: 最近打开
│       ├── TabFavoriteList.java     # Tab: 收藏夹
│       ├── ViewScroll.java          # 乐谱滚动容器 (AbsoluteLayout)
│       ├── TouchView.java           # 可缩放拖动的 ImageView
│       │
│       └── musicxml/                # ===== MusicXML 文件读取模块 =====
│           ├── FileReader.java          # 统一文件加载入口
│           ├── FileType.java            # 文件类型枚举
│           ├── FileTypeReader.java      # 文件类型检测 (魔数+XML根元素)
│           ├── MusicXMLScoreFileInput.java  # 普通 .xml 乐谱读取
│           ├── CompressedFileInput.java     # .mxl 压缩包读取 (ZIP)
│           └── OpusFileInput.java           # .opus 乐谱合集读取
│
│   └── com/xenoage/zong/            # ===== Xenoage Zong 库 (第三方) =====
│       ├── musicxml/                # MusicXML 数据模型 (100+ 类)
│       │   ├── MusicXMLDocument.java    # MusicXML 文档根对象
│       │   ├── types/                   # 类型定义 (MxlScorePartwise 等)
│       │   └── util/                    # 解析异常与工具
│       ├── symbols/                 # 乐谱符号库 (Symbol 渲染)
│       │   ├── SymbolPool.java          # 符号池 (加载默认符号集)
│       │   ├── Symbol.java              # 符号基础类
│       │   └── common/CommonSymbol.java # 常用符号 ID 枚举
│       └── util/                    # 工具类 (数学、事件等)
│
├── lib/                             # 第三方 JAR 依赖
│   ├── core.jar                     # Xenoage 核心库
│   ├── pdlib.jar                    # 持久化/数据结构库 (PVector 等)
│   └── util.jar                     # Xenoage 通用工具库
│
├── res/                             # Android 资源目录
│   ├── layout/                      # 布局 XML
│   │   ├── main.xml                     # 主界面布局 (TabHost)
│   │   └── shelf.xml                    # 乐谱查看布局
│   ├── drawable-{hdpi,ldpi,mdpi}/   # 多分辨率图标
│   ├── raw/                         # 钢琴音符音频采样 (.ogg)
│   │   ├── c_1.ogg ~ c_6.ogg            # C 音 (C1-C6)
│   │   ├── cs_1.ogg ~ cs_6.ogg          # C# 音
│   │   ├── d_1.ogg ~ d_6.ogg            # D 音
│   │   ├── ...                          # (D#, E, F, F#, G, G#, A, A#, B)
│   │   └── 共约 84 个音频文件           # 覆盖 7 个八度
│   ├── values/                      # 默认字符串资源 (英文)
│   │   └── strings.xml
│   ├── values-zh-rCN/               # 中文字符串资源
│   │   └── strings.xml
│   └── xml/                         # XML 配置
│       ├── perference.xml               # Preference 设置项
│       ├── colour.xml                   # 颜色配置
│       └── tex_default.xml              # 纹理默认配置
│
├── gen/                             # 自动生成文件 (R.java)
├── bin/                             # Ant 构建输出目录
│   ├── classes/                         # 编译后的 .class
│   ├── PianoShelf.apk                   # 生成的 APK 安装包
│   └── AndroidManifest.xml
│
└── data/                            # 示例数据与参考文件
    ├── WAVE/                            # WAV 格式音符样本
    ├── BeetAnGeSample.xml               # 示例 MusicXML
    ├── HelloMX.xml
    ├── test.mxl                         # 示例压缩乐谱
    ├── musicxml.xsd                     # MusicXML Schema
    └── opus.xsd / container.xsd 等      # 其他 XSD 参考
```

---

## 4. 核心模块详解

### 4.1 UI 控制层 (Activities)

#### 4.1.1 PianoShelfActivity - 主界面

**位置**: [PianoShelfActivity.java](file:///workspace/src/com/rojama/pianoshelf/PianoShelfActivity.java)

**职责**:
- 应用入口 Activity (LAUNCHER)
- 初始化 TabHost，挂载三个 Tab 页面：
  - `tab_browse` → TabBrowseList (文件浏览)
  - `tab_recent` → TabRecentList (最近打开)
  - `tab_favorite` → TabFavoriteList (收藏夹)
- 初始化 SQLite 数据库连接 (DatabaseHelper)
- 后台线程预加载 SoundPool 音频资源 (LoadThread)
- 选项菜单：设置 / 退出

**关键方法**:
| 方法 | 说明 |
|-----|------|
| `onCreate()` | 初始化 Tab、数据库、启动音频加载线程 |
| `LoadThread.run()` | 后台调用 `SoundPoolUtiil.loadSound()` 预加载 84 个音符 |
| `onOptionsItemSelected()` | 菜单处理：启动设置页 or 退出应用 |

#### 4.1.2 GraphicsActivity - 乐谱查看界面

**位置**: [GraphicsActivity.java](file:///workspace/src/com/rojama/pianoshelf/GraphicsActivity.java)

**职责**:
- 通过 Intent 接收 `.xml` / `.mxl` 文件 URI
- 创建并托管 GraphicsView，传入文件路径和屏幕尺寸
- 处理横竖屏切换，通知 GraphicsView 重新布局
- 提供乐谱操作菜单：上一页 / 下一页 / 播放

**Intent Filter**:
```
Action: VIEW
MIME:   application/vnd.recordare.musicxml
Path:   *.mxl | *.xml
```
→ 支持从文件管理器直接打开 MusicXML 文件。

**关键方法**:
| 方法 | 说明 |
|-----|------|
| `onCreate()` | 解析 URI → 创建 GraphicsView → `showView()` |
| `onOptionsItemSelected()` | 翻页：`graphicsView.dispalyPageNo ± 1`；播放：`graphicsView.play()` |
| `onConfigurationChanged()` | 屏幕旋转时刷新 `screenWidth/Height`，调用 `changeOrientation()` |

#### 4.1.3 AppPreferenceActivity - 设置界面

**位置**: [AppPreferenceActivity.java](file:///workspace/src/com/rojama/pianoshelf/AppPreferenceActivity.java)

**职责**:
- 基于 PreferenceActivity，从 `R.xml.perference` 加载设置项
- SharedPreferences 存储名：`appPreferences`

**设置项** (参考 [perference.xml](file:///workspace/res/xml/perference.xml)):
| Key | 说明 | 可选值 |
|-----|------|-------|
| `foreground` | 前景颜色 | red/blue/green/black/white/gray/cyan/magenta/yellow/lightgray/darkgray |
| `background` | 背景颜色 | 同上 |
| `max_recent` | 最近记录数量 | 0, 10, 20, 50, 100, 200, 500, 1000 |

---

### 4.2 渲染与播放核心 (GraphicsView)

**位置**: [GraphicsView.java](file:///workspace/src/com/rojama/pianoshelf/GraphicsView.java)

**职责**: 整个应用的心脏，统筹乐谱的解析、渲染和播放三大流程。

**内部类概览**:
| 内部类 | 类型 | 职责 |
|-------|------|------|
| `AsyncLoader` | AsyncTask | 异步加载和渲染乐谱，返回 Bitmap |
| `TimeThread` | Thread | 播放时间轴：每 `parttime`(5ms) 推进 time+1 |
| `PlayThread` | Thread | 总播放线程：按声部分配 PlayPartThread |
| `PlayPartThread` | Thread | 声部播放线程：为每个音符创建 PlayNoteThread |
| `PlayNoteThread` | Thread | 单音符播放：等待到指定时间点后播放音频 |

**渲染流程 (AsyncLoader.doInBackground)**:
```
1. new CommonTransfer() → 初始化渲染上下文
2. IO.initApplication() → 初始化 Xenoage IO
3. FileReader.loadScores() → 解析 MusicXML → List<MxlScorePartwise>
4. SymbolPool.loadDefault() → 加载乐谱符号位图
5. ct.setScreen() / ct.setDisPageNo() → 配置屏幕和页码
6. 遍历 scoreList: sub.paint(ct) → 绘制到 Canvas，收集 Note
7. return ct.bitmap → 渲染完成的乐谱位图
```

**播放时间单位**:
- `parttime = 5` ms：MusicXML 中以 256 分音符为基准的时间单位
- 时间轴 `time` 每 5ms +1
- `Note.duration` = 该音符在时间轴上的触发点（64 ≈ 四分音符）

**音调映射方法**:
- `getRawID(Pitch pitch)` → 将 MusicXML Pitch (八度+音级+变音记号) 映射为 `R.raw.*` 音频资源 ID
- 覆盖音域：G0 (MIDI 23) ~ B6 (MIDI 95)，共约 7 个八度

---

### 4.3 渲染上下文与绘图

#### 4.3.1 CommonTransfer - 全局渲染上下文

**位置**: [CommonTransfer.java](file:///workspace/src/com/rojama/pianoshelf/CommonTransfer.java)

**职责**: 作为 **PaintTransfer** 和 **MxlScorePartwise.paint()** 之间的共享状态对象，贯穿整个渲染流程。

**核心字段**:
| 字段 | 类型 | 说明 |
|-----|------|------|
| `bitmap` | Bitmap | 渲染目标位图 (RGB_565) |
| `canvas` | Canvas | 绑定到 bitmap 的画布 |
| `paint` | Paint | 画笔 (颜色取自 SharedPreferences) |
| `symbolPool` | SymbolPool | Xenoage 乐谱符号池 |
| `scorePartsNotes` | `Map<String, Vector<Note>>` | **播放用数据**: 每个声部 ID → 该声部所有音符列表 |
| `scoreParts` | `Map<String, MxlScorePart>` | 声部元数据 |
| `context/appPrefs` | Context / SharedPreferences | Android 上下文和配置 |
| `screenWidth/Height` | int | 屏幕尺寸 (横竖屏变化时更新) |
| `pageWidth/Height` | float | MusicXML 定义的乐谱页面尺寸 |
| `isUpright` | boolean | 是否竖屏 |
| `disPageNo` | int | 当前渲染页码 |
| `maxPage` | int | 总页数 |
| `defaults` | MxlDefaults | MusicXML defaults 元素 (默认布局参数) |

#### 4.3.2 PaintTransfer - 绘图状态机

**位置**: [PaintTransfer.java](file:///workspace/src/com/rojama/pianoshelf/PaintTransfer.java)

**职责**: 封装 Mxl* 系列对象 paint() 过程中的当前绘制状态 (游标位置、当前小节、谱号等)，并提供原子绘图方法。

**状态字段**:
| 字段 | 说明 |
|-----|------|
| `nowPartID` | 当前处理的声部 ID |
| `nowPage / nowLine / nowMeasure` | 当前页/行/小节号 |
| `oldX / oldY` | 当前画笔坐标 (MusicXML 坐标系：原点左下) |
| `nowDuration` | 当前累积时间 (播放时序用) |
| `measureLeft / measureUp / measureWidth` | 当前小节框坐标 |
| `nowClefType` | `Map<staffNum, ClefType>` 当前谱号 (G/F) |
| `nowFifths` | 当前调号 (五度圈值：-4 ~ +4) |
| `nowTime` | 当前拍号 (MxlTime) |
| `divisions` | 当前 divisions (每四分音符包含的 256 分音符数) |
| `staffLayout` | `Map<staffNum, distance>` 各谱行间距 |
| `isNewSystem` | 是否新的一行谱表 |

**绘图方法**:
| 方法 | 说明 |
|-----|------|
| `drawBitmap(bmp, left, top)` | 绘制符号位图 (带颜色叠加 MULTIPLY 模式) |
| `drawText(text, x, y)` | 绘制文字 |
| `drawLine(x1,y1,x2,y2)` | 画直线 (小节线等) |
| `drawPath(path)` | 画任意路径 |
| `drawBezierPath(start, ctrl, end)` | 画二次贝塞尔曲线 (连线、延音线) |
| `printHand(printTimeOnly)` | 画谱号+调号+拍号 (行首标记) |
| `printClef(key, x, y)` | 画 G/F 谱号符号 |
| `printKey(x, y)` | 画调号升降号 |
| `printTime(x, y)` | 画拍号 (上下数字) |
| `getPointFromMxlPosition(pos)` | 将 MxlPosition 转为屏幕坐标 |

**坐标转换**:
- MusicXML 使用数学坐标系 (原点左下，Y 向上)
- Android Canvas 使用屏幕坐标系 (原点左上，Y 向下)
- 转换公式：`canvasY = pageHeight - musicxmlY`

---

### 4.4 数据模型

#### 4.4.1 Note - 音符播放模型

**位置**: [Note.java](file:///workspace/src/com/rojama/pianoshelf/Note.java)

**字段说明**:
| 字段 | 类型 | 说明 |
|-----|------|------|
| `measureNum` | int | 所属小节号 |
| `pageNum` | int | 所属页码 |
| `partID` | String | 所属声部 ID |
| `mxlNote` | MxlNote | 原始 MusicXML 音符对象引用 |
| `duration` | int | 播放时序触发点 (基准：64 = 四分音符) |
| `point` | PointF | 音符在 Bitmap 上的坐标 |
| `pitch` | Pitch | 音高 (八度+音级+变音记号) |
| `volume` | int | 音量 (预留，当前固定为1) |

---

### 4.5 文件管理 (三个 Tab List)

三者均继承自 `ListView`，实现 `OnItemClickListener` + `OnItemLongClickListener`。

| 类 | 位置 | 点击行为 | 长按行为 |
|----|------|---------|---------|
| **TabBrowseList** | [TabBrowseList.java](file:///workspace/src/com/rojama/pianoshelf/TabBrowseList.java) | 文件：打开乐谱；目录：进入子目录 | 添加收藏 |
| **TabRecentList** | [TabRecentList.java](file:///workspace/src/com/rojama/pianoshelf/TabRecentList.java) | 打开该乐谱（同时刷新最近记录） | 添加收藏 |
| **TabFavoriteList** | [TabFavoriteList.java](file:///workspace/src/com/rojama/pianoshelf/TabFavoriteList.java) | 打开该乐谱 | **移除收藏** |

**文件过滤器 (MusicFileFilter)**:
- 只显示可读的目录，以及 `.xml` / `.mxl` 后缀的文件
- 跳过隐藏文件 (`.`)、`LOST.DIR`、`DCIM`

---

### 4.6 视图交互

#### 4.6.1 ViewScroll - 乐谱容器

**位置**: [ViewScroll.java](file:///workspace/src/com/rojama/pianoshelf/ViewScroll.java)

**职责**: 继承 `AbsoluteLayout`，计算屏幕可用区域，创建并放置 TouchView。

#### 4.6.2 TouchView - 可缩放拖动的图像视图

**位置**: [TouchView.java](file:///workspace/src/com/rojama/pianoshelf/TouchView.java)

**继承**: `ImageView`

**支持的手势**:
| 模式 | 触发条件 | 行为 |
|-----|---------|------|
| DRAG (拖动) | 单指按下并移动 | 平移乐谱图像 |
| ZOOM (缩放) | 双指按下，间距变化 | 按捏合手势缩放 (0.4x ~ 1.4x) |
| BIGGER / SMALLER | 点击底部 ZoomControls 按钮 | 按固定比例缩放 |

**边界回弹**:
- `processOut()` 在 ACTION_UP 时检测图像是否超出屏幕边界
- 若超出则使用 `TranslateAnimation` 弹回边界内 (500ms 动画)

---

### 4.7 MusicXML 文件读取模块

**包**: `com.rojama.pianoshelf.musicxml`

#### 4.7.1 FileType - 文件类型枚举

**位置**: [FileType.java](file:///workspace/src/com/rojama/pianoshelf/musicxml/FileType.java)

| 枚举值 | 对应格式 | 扩展名 |
|-------|---------|--------|
| `XMLScorePartwise` | 逐声部逐小节 MusicXML | .xml |
| `XMLScoreTimewise` | 逐小节逐声部 MusicXML (当前未实现渲染) | .xml |
| `XMLOpus` | Opus 乐谱合集 | .xml |
| `Compressed` | 压缩 MusicXML (ZIP 格式) | .mxl |

#### 4.7.2 FileTypeReader - 文件类型检测

**位置**: [FileTypeReader.java](file:///workspace/src/com/rojama/pianoshelf/musicxml/FileTypeReader.java)

**检测逻辑**:
1. 读取前 2 字节 → 若为 `0x50 0x4B` ("PK") → ZIP 即 `Compressed`
2. 否则解析 XML → 读取根元素 tag:
   - `<score-partwise>` → `XMLScorePartwise`
   - `<score-timewise>` → `XMLScoreTimewise`
   - `<opus>` → `XMLOpus`

#### 4.7.3 FileReader - 统一加载入口

**位置**: [FileReader.java](file:///workspace/src/com/rojama/pianoshelf/musicxml/FileReader.java)

**核心方法**: `loadScores(path, filter) → List<MxlScorePartwise>`

根据文件类型分发：
- **XMLScorePartwise** → `MusicXMLScoreFileInput.read()` → 直接解析
- **XMLOpus** → `OpusFileInput` → 解析合集，递归加载每个链接的分数
- **Compressed (.mxl)** → `CompressedFileInput` → 解压 ZIP，读取 `META-INF/container.xml` 找到根文件后解析

#### 4.7.4 CompressedFileInput - MXL 压缩格式解析

**位置**: [CompressedFileInput.java](file:///workspace/src/com/rojama/pianoshelf/musicxml/CompressedFileInput.java)

处理流程：
```
1. 解压 ZIP 到临时目录 (/tmp/{uuid}/)
2. 读取 META-INF/container.xml → 获取 <rootfile full-path="...">
3. 检测根文件类型 (Opus / ScorePartwise)
4. 解析根文件 → 获取所有 .xml 乐谱路径
5. 逐一调用 MusicXMLScoreFileInput 解析
6. finalize() / close() 时删除临时目录
```

---

## 5. 关键类与函数说明

### 5.1 类关系图 (核心)

```
PianoShelfActivity (主界面)
    │
    ├──► DatabaseHelper (SQLite)
    │       ├── RECENT 表
    │       └── FAVORITE 表
    │
    ├──► TabBrowseList / TabRecentList / TabFavoriteList
    │       └── (点击) ──► Intent ──► GraphicsActivity
    │
    └──► LoadThread ──► SoundPoolUtiil.loadSound()

GraphicsActivity (乐谱界面)
    │
    └──► GraphicsView
            │
            ├──► AsyncLoader ──┬──► FileReader.loadScores()
            │                  │      ├── MusicXMLScoreFileInput
            │                  │      ├── OpusFileInput
            │                  │      └── CompressedFileInput
            │                  ├──► CommonTransfer (上下文)
            │                  ├──► SymbolPool (符号库)
            │                  └──► MxlScorePartwise.paint() ──► PaintTransfer.* ──► Canvas
            │
            ├──► TimeThread (时间轴)
            ├──► PlayThread (总播放)
            │       └──► PlayPartThread (声部)
            │               └──► PlayNoteThread (单音) ──► SoundPoolUtiil.playSound()
            │
            └──► ViewScroll ──► TouchView (缩放/拖动)

SoundPoolUtiil (音频池)
    └──► SoundPool.play(raw_ogg_id)
```

### 5.2 关键函数速查

#### 数据库操作 (DatabaseHelper)

| 函数 | 签名 | 说明 |
|-----|------|------|
| insertRecentItem | `void (String filepath)` | 插入最近记录（去重+超1000条删最早） |
| insertFavoriteItem | `void (String filepath)` | 插入收藏（去重） |
| deleteFavoriteItem | `void (String filepath)` | 按路径删除收藏 |
| selectRecentItem | `Vector<String> (String limit)` | 查询最近记录（按时间倒序） |
| selectFavoriteItem | `Vector<String> ()` | 查询所有收藏（按时间倒序） |

#### 音频操作 (SoundPoolUtiil)

| 函数 | 签名 | 说明 |
|-----|------|------|
| loadSound | `static void (Context ctx)` | 批量加载 R.raw.a_0 ~ R.raw.gs_6 共 84 个音频 |
| playSound | `static void (int resid)` | 播放指定资源 ID 的音频（音量1.0，不循环，速率1.0） |

#### 渲染流程入口 (GraphicsView)

| 函数 | 签名 | 说明 |
|-----|------|------|
| showView | `void ()` | 首次加载，AsyncLoader 参数 0（初始化 CommonTransfer） |
| reShowView | `void ()` | 翻页/刷新，AsyncLoader 参数 1（复用已有 CommonTransfer） |
| play | `void ()` | 启动播放：TimeThread + PlayThread 同时启动 |
| getRawID | `int (Pitch pitch)` | Pitch → R.raw.* 资源 ID 映射 |

#### 坐标与绘图 (PaintTransfer)

| 函数 | 签名 | 说明 |
|-----|------|------|
| getMxlAllMargins | `MxlAllMargins ()` | 取当前页的页边距（奇偶页不同） |
| getPointFromMxlPosition | `Point (MxlPosition pos)` | MusicXML 位置 → 屏幕像素点（原点转换） |
| printHand | `void (boolean printTimeOnly)` | 绘制谱号+调号+拍号组合 |

---

## 6. 依赖关系

### 6.1 外部 JAR 依赖

| JAR 文件 | 路径 | 作用 | 提供的核心类 |
|---------|------|------|-------------|
| **core.jar** | [lib/core.jar](file:///workspace/lib/core.jar) | Xenoage 核心库 (Zong! 项目) | `MusicXMLDocument`, `MxlScorePartwise`, 所有 `Mxl*` 类型 |
| **pdlib.jar** | [lib/pdlib.jar](file:///workspace/lib/pdlib.jar) | 持久化/不可变数据结构库 | `PVector<T>` (持久化 Vector) |
| **util.jar** | [lib/util.jar](file:///workspace/lib/util.jar) | Xenoage 通用工具 | `IO`, `XMLReader`, `FileTools`, `ZipTools`, `Pitch`, `ClefType`, `Accidental` 等 |

> **Xenoage Zong** 是一个开源的 MusicXML 处理和乐谱渲染 Java 库，本项目大量复用其 MusicXML 数据模型层 (`com.xenoage.zong.musicxml.*`) 和符号库层 (`com.xenoage.zong.symbols.*`)，但重写了渲染层（Android Canvas 实现）。

### 6.2 Android SDK 依赖

| SDK 组件 | 用途 |
|---------|------|
| `android.app.Activity / TabHost / ListView` | UI 组件 (传统 Android API) |
| `android.graphics.*` (Bitmap, Canvas, Paint, Path) | 2D 图形渲染 |
| `android.media.SoundPool` | 低延迟音频播放 (钢琴单音) |
| `android.database.sqlite.SQLite*` | 本地数据库 (RECENT/FAVORITE) |
| `android.content.SharedPreferences` | 配置存储 |
| `android.view.MotionEvent` | 多点触控手势处理 |
| `android.os.AsyncTask` | 异步乐谱加载 |
| `android.widget.ZoomControls` | 缩放控制按钮 |

### 6.3 AndroidManifest 权限

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
→ 需要读写外部存储以浏览文件系统和读取乐谱文件。

### 6.4 SDK 版本

```
minSdkVersion: 8    (Android 2.2 Froyo)
targetSdkVersion: 未显式设置 (默认 target=android-19, Android 4.4 KitKat)
```

---

## 7. 数据存储设计

### 7.1 SQLite 数据库

**数据库名**: `dbForPianoShelf.db`
**版本**: 3

#### 表结构

**表 1: RECENT (最近打开记录)**

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| `created_at` | TIMESTAMP | PRIMARY KEY | 创建时间戳 (Unix 秒) |
| `filepath` | VARCHAR(512) | - | 乐谱文件绝对路径 |

- 最大记录数：**1000 条**（超出则删除最早的）
- 插入策略：先按 filepath 删除旧条目，再插入新条目（保证最新）
- 查询排序：`created_at DESC`

**表 2: FAVORITE (收藏夹)**

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| `created_at` | TIMESTAMP | PRIMARY KEY | 创建时间戳 (Unix 秒) |
| `filepath` | VARCHAR(512) | - | 乐谱文件绝对路径 |

- 最大记录数：无限制
- 插入策略：同 RECENT (先删后插)
- 删除方式：长按列表项 → `delete from FAVORITE where filepath = ?`

### 7.2 SharedPreferences

**文件**: `appPreferences.xml`

| Key | 默认值 | 类型 | 说明 |
|-----|--------|------|------|
| `foreground` | `"black"` | String | 前景色 (音符/谱线颜色) |
| `background` | `"white"` | String | 背景色 (乐谱纸张颜色) |
| `max_recent` | `"100"` | String | 最近记录最大显示条数 |

### 7.3 临时文件

- MXL (ZIP) 解压路径：`{系统临时目录}/{UUID随机串}/`
- 读取完成后通过 `CompressedFileInput.close()` → `FileTools.deleteDirectory()` 清理

---

## 8. 资源文件说明

### 8.1 布局文件 (res/layout)

#### main.xml - 主界面

**结构**:
```
LinearLayout (vertical)
  └── TabHost (@android:id/tabhost)
        └── LinearLayout (vertical)
              ├── TabWidget (@android:id/tabs)
              └── FrameLayout (@android:id/tabcontent)
                    ├── LinearLayout (@id/tab_browse)
                    │     └── ListView (@id/listView_browse)
                    ├── LinearLayout (@id/tab_recent)
                    │     └── ListView (@id/listView_recent)
                    └── LinearLayout (@id/tab_favorite)
                          └── ListView (@id/listView_favorite)
```

#### shelf.xml - 乐谱查看界面

**结构**:
```
RelativeLayout (@id/relativeLayout)
  ├── LinearLayout (@id/linearLayout_image)        ← 乐谱位图容器
  ├── ZoomControls (@id/zoomControls)              ← 底部缩放按钮
  └── ProgressBar (@id/progressBar)                ← 加载中进度圈
```

### 8.2 字符串资源 (多语言)

| 文件 | 语言 | 应用名 |
|------|------|--------|
| [res/values/strings.xml](file:///workspace/res/values/strings.xml) | 英文 (默认) | PianoShelf |
| [res/values-zh-rCN/strings.xml](file:///workspace/res/values-zh-rCN/strings.xml) | 简体中文 | 便携乐谱 |

中文资源额外定义：菜单项（上一页/下一页/播放/设置/退出）、提示语（添加收藏/文件不可读/第X页）、颜色列表等。

### 8.3 音频资源 (res/raw)

采用 **Ogg Vorbis (.ogg)** 格式（Android 原生支持、体积小）。

**命名规则**: `{音名简写}_{八度}.ogg`
- `c` = C, `cs` = C♯, `d` = D, `ds` = D♯, `e` = E, `f` = F, `fs` = F♯, `g` = G, `gs` = G♯, `a` = A, `as` = A♯, `b` = B
- 八度范围：0 ~ 6 (G0 开始到 B6)
- 总数：约 84 个独立采样

**加载方式** (SoundPoolUtiil.loadSound):
```java
// 利用 R.raw 资源 ID 在 a_0 ~ gs_6 之间连续分配的特性
for (int resid = R.raw.a_0; resid <= R.raw.gs_6; resid++) {
    soundPoolIdMap.put(resid, soundPool.load(context, resid, 0));
}
```

### 8.4 图标资源

| 文件 | 说明 | 分辨率版本 |
|------|------|-----------|
| `icon.png` | 应用启动图标 | ldpi / mdpi / hdpi |
| `tex_default.png` | 默认纹理 (乐谱渲染用) | ldpi / mdpi / hdpi |

---

## 9. 项目运行方式

### 9.1 环境要求

| 工具 | 最低版本 | 说明 |
|-----|---------|------|
| JDK | 1.6+ | Java 编译 |
| Android SDK | Android 4.4 (API 19) | project target |
| Apache Ant | 1.8+ | 构建系统 |
| Android 模拟器 / 真机 | Android 2.2 (API 8)+ | 运行环境 |

### 9.2 构建步骤 (Ant)

```bash
# 1. 确保 ANDROID_HOME 环境变量已设置
export ANDROID_HOME=/path/to/android-sdk

# 2. 生成构建文件（如果需要）
android update project -p /workspace

# 3. Debug 构建
cd /workspace
ant debug
# → 输出: bin/PianoShelf-debug.apk

# 4. Release 构建 (需要签名)
ant release
# → 输出: bin/PianoShelf-release-unsigned.apk

# 5. 安装到设备
adb install -r bin/PianoShelf-debug.apk
```

### 9.3 部署测试数据

项目 `data/` 目录提供了示例乐谱，可推送到设备 SD 卡测试：

```bash
# 推送示例乐谱到设备
adb push /workspace/data/BeetAnGeSample.xml /sdcard/
adb push /workspace/data/test.mxl /sdcard/

# 运行应用后从「浏览乐谱」进入 /sdcard/ 目录点击即可打开
```

### 9.4 启动流程

1. 安装 APK 后，点击桌面图标 **「便携乐谱」**
2. 进入主界面，底部三个 Tab：
   - **浏览乐谱**：从 `/` 根目录开始浏览文件系统
   - **最近打开**：查看最近打开过的乐谱
   - **收藏夹**：查看收藏的乐谱
3. 点击任意 `.xml` 或 `.mxl` 文件 → 进入乐谱查看页
4. 乐谱查看页菜单键 (Menu)：
   - 上一页 / 下一页：翻页
   - 播放：开始自动播放当前乐谱

---

## 10. 构建与配置

### 10.1 构建配置文件

#### project.properties
```
target=android-19
```
→ 指定使用 Android 4.4 SDK Platform 编译。

#### proguard.cfg
- 保留所有 Activity/Service/Receiver/Provider 的继承类
- 保留 native 方法、自定义 View 构造函数、Parcelable CREATOR
- 保留枚举 values() / valueOf()
- 关闭预校验 (`-dontpreverify`)，5 轮优化

### 10.2 Activity 配置 (AndroidManifest.xml)

| Activity | 主题 | 配置变更 | 说明 |
|----------|------|---------|------|
| PianoShelfActivity | `@android:style/Theme` | orientation\|keyboard | 启动页 + 文件浏览 |
| GraphicsActivity | `Theme.Black.NoTitleBar.Fullscreen` | orientation\|keyboard | 全屏查看乐谱 + Intent Filter 打开外部文件 |
| AppPreferenceActivity | 默认 | - | 设置页 |

### 10.3 可配置项汇总

用户可配置 (SharedPreferences)：
1. **前景颜色**: 12 种颜色选择，影响乐谱音符、谱线颜色
2. **背景颜色**: 12 种颜色选择，影响乐谱纸张背景
3. **最近记录数**: 8 档 (0 ~ 1000)

开发者可调参数 (源代码常量)：

| 常量 | 位置 | 默认值 | 说明 |
|-----|------|--------|------|
| `parttime` | GraphicsView.L37 | `5` ms | 每个 256 分音符的播放时长（越小越快） |
| `maxTime` | GraphicsView.play() | `333` | 每页最大时间轴长度 (占位值，播放中动态更新) |
| `DATABASE_VERSION` | DatabaseHelper.L14 | `3` | 数据库版本号 |
| `RECENT` 表上限 | DatabaseHelper.L81 | `1000` 条 | 最近记录最大物理存储数 |
| 缩放比例 `scale` | TouchView.L31 | `0.2f` | 单次缩放按钮触发的缩放比例 |
| 缩放上下限 | TouchView.L175/L181 | `0.4x ~ 1.4x` | 乐谱可缩放范围 |

---

## 附录 A. 术语表

| 术语 | 英文 | 说明 |
|-----|------|------|
| MusicXML | Music Extensible Markup Language | 基于 XML 的乐谱交换格式标准，2.0 版本 |
| MXL | Compressed MusicXML | ZIP 压缩的 MusicXML 格式，扩展名 .mxl |
| Opus | Opus Document | MusicXML 乐谱合集格式，包含多个 score 引用 |
| 谱号 | Clef | G谱号(高音谱表) / F谱号(低音谱表) |
| 调号 | Key Signature | 五度圈表示 (Fifths)，+N=N个升号，-N=N个降号 |
| 拍号 | Time Signature | 如 4/4, 3/8，用上下数字表示 |
| 小节 | Measure / Bar | 两条小节线之间的单位 |
| 声部 | Part | 独立的乐器/演奏线条 (Piano 通常含 Right/Left 两手) |
| Divisions | Divisions per Quarter Note | 每四分音符划分的最小时间单位数 |
| Pitch | Pitch | 音高 = 八度(Octave) + 音级(Step:CDEFGAB) + 变音(Alter) |
| SoundPool | - | Android 低延迟短音频播放 API，适用于钢琴按键音 |

---

## 附录 B. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|-----|---------|---------|
| 打开乐谱提示「文件格式有误」 | XML 损坏 / 非标准 MusicXML / MXL 缺少 container.xml | 用 MuseScore 等工具验证并重新导出 MusicXML |
| 播放无声 | SoundPool 加载失败 / 设备媒体音量为 0 | 检查设备音量；确认 R.raw.* 资源完整 |
| 乐谱显示不全/错位 | MusicXML 使用了未实现的元素 (如 `<score-timewise>`) | 转换为 `score-partwise` 格式后重试 |
| 翻页后音符与播放不同步 | maxTime 初始值较小导致第一页播放提前结束翻页 | 可考虑修改为基于 Note.duration 预计算总时长 |
| 中文显示为英文 | 设备语言非 zh-CN | 系统设置切换为简体中文，或在 strings.xml 添加其他语言翻译 |

---

## 11. 现代化改造 & Bug 修复记录 (v2.0.0-modernized)

> 本章节记录了将项目从 **Ant + Android 4.4 (API 19)** 升级到 **Gradle + Android 14 (API 34)** 过程中所做的全部修复、优化和兼容性改进。

### 11.1 构建系统升级 (Ant → Gradle)

| 变更项 | 说明 |
|-------|------|
| **构建系统** | Apache Ant → Gradle (AGP 适配) |
| **compileSdk** | 19 → **34** (Android 14) |
| **targetSdk** | 19 → **34** (Android 14) |
| **minSdk** | 8 → **21** (Android 5.0 Lollipop) |
| **versionCode** | 1 → 2 |
| **versionName** | 1.0 → **2.0.0-modernized** |
| **新增文件** | `settings.gradle`, `build.gradle`, `pianoshelf.gradle`, `gradle.properties` |

**Gradle 依赖升级** (从 Support Library → AndroidX + Material):
```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.preference:preference:1.2.1'
implementation 'androidx.core:core:1.12.0'
```

---

### 11.2 P0 级 Bug 修复 (崩溃 / 安全 / 性能)

#### 11.2.1 SQL 注入漏洞 (DatabaseHelper)

**问题**: 原代码使用字符串拼接构造 SQL，存在注入风险：
```java
// 不安全
db.execSQL("delete from RECENT where filepath = '" + filepath + "';");
```

**修复**: 
- 全部改为 `?` 参数化查询 + `ContentValues`
- `delete()` / `insert()` 使用 Android SDK 安全方法
- 批量裁剪使用 `execSQL(sql, bindArgs)` 绑定参数

#### 11.2.2 Cursor 资源泄漏 (DatabaseHelper)

**问题**: `selectRecentItem` / `selectFavoriteItem` 在异常情况下 `Cursor` 可能未关闭，导致内存泄漏。

**修复**: 
- 所有 Cursor 操作包装在 `try-catch-finally` 中
- `finally` 块确保 `cursor.close()` 被调用
- 新增 `closeQuietly(Cursor)` 工具方法

#### 11.2.3 线程爆炸 / OOM (GraphicsView 播放模型)

**问题**: 原实现每音符一个 `PlayNoteThread`，一首 500 音符乐谱会创建 500+ 线程，极易 OOM 和卡顿。

**修复**: 
- 移除 `TimeThread` / `PlayThread` / `PlayPartThread` / `PlayNoteThread` 全部内部线程类
- 改用 **单线程池调度器**：`ScheduledExecutorService.newScheduledThreadPool(2)`
- 构建播放计划表：`Map<Integer /*tick*/, List<Note>>`
- 每 `PARTTIME_MS` (5ms) 触发一次 tick，批量播放该 tick 下所有音符
- 新增 `shutdown()` 方法安全释放线程池

#### 11.2.4 SoundPool 废弃构造 + 空指针

**问题**: 
- `SoundPool(int, int, int)` 在 API 21+ 已废弃
- 未加载完成即 `playSound()` 会导致 NPE 或无声
- 从未调用 `release()` 导致资源泄漏

**修复**:
- 使用 `SoundPool.Builder` (API 21+) + AudioAttributes
- 低版本保留 Legacy 构造 (兼容 minSdk=21 但保留判断)
- 新增加载状态回调：`OnLoadCompleteListener` + 计数等待
- 新增 `release()` 静态方法，`Activity.onDestroy()` 时释放
- `playSound()` 前校验资源已加载

#### 11.2.5 getRawID() 性能优化 (O(80-switch) → O(1) 查表)

**问题**: 原实现 ~80 条分支的嵌套 switch-case，每次调用需多次条件判断。

**修复**: 
- 构建二维查表数组：`int[12 /*add*/][8 /*octave*/]`
- `Pitch` → 索引 → O(1) 直达
- 静态 `volatile` 标志 + synchronized 确保只初始化一次

---

### 11.3 P1 级兼容性修复 (AndroidX + 新 SDK 适配)

#### 11.3.1 Activity 继承体系升级

| Activity | 原父类 | 新父类 | 说明 |
|----------|--------|--------|------|
| `PianoShelfActivity` | `Activity` | `AppCompatActivity` | 获得 ActionBar/Toolbar/Lifecycle 支持 |
| `GraphicsActivity` | `Activity` | `AppCompatActivity` | 同上 |
| `AppPreferenceActivity` | `PreferenceActivity` (deprecated) | `AppCompatActivity` + `PreferenceFragmentCompat` | 使用 AndroidX Preference |

#### 11.3.2 FileUriExposedException (Android 7.0+)

**问题**: Android 7.0+ 禁止 app 间暴露 `file://` URI，直接启动会崩溃。

**修复**:
- `AndroidManifest.xml` 声明 `FileProvider`:
  ```xml
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="com.rojama.pianoshelf.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data android:resource="@xml/file_paths" />
  </provider>
  ```
- 新增 `res/xml/file_paths.xml` 配置可共享目录
- 三个 Tab List 统一使用 `TabBrowseList.launchGraphicsActivity()`，通过 `FileProvider.getUriForFile()` 生成 `content://` URI
- Intent 添加 `FLAG_GRANT_READ_URI_PERMISSION`

#### 11.3.3 动态权限申请 (Android 6.0+)

**问题**: Android 6.0+ 需要动态申请危险权限，原代码仅在 Manifest 声明，Browse/Recent/Favorite 列表为空。

**修复**:
- `PianoShelfActivity`: 启动时检查 `WRITE_EXTERNAL_STORAGE`，未授权则弹出请求
- `GraphicsActivity`: 打开文件前检查 `READ_EXTERNAL_STORAGE`，通过 `pendingPath` 延迟初始化
- 回调 `onRequestPermissionsResult` 中刷新列表或继续加载乐谱

#### 11.3.4 Deprecated API 替换

| 废弃 API | 位置 | 替换方案 |
|---------|------|---------|
| `AsyncTask` | GraphicsView | `new Thread()` + `Handler(Looper.getMainLooper())` 回主线程 |
| `FloatMath.sqrt()` | TouchView | `Math.sqrt()` (API 17+ 已推荐) |
| `Display.getWidth() / getHeight()` | GraphicsActivity / TouchView | `Resources.getDisplayMetrics().widthPixels/heightPixels` |
| `AbsoluteLayout` | ViewScroll | `FrameLayout` + `setX()/setY()` 实现子 View 定位 |
| `System.exit(0)` | PianoShelfActivity.onDestroy | 移除；改用 `finishAffinity()` 优雅退出 |
| `PreferenceActivity.addPreferencesFromResource()` | AppPreferenceActivity | `PreferenceFragmentCompat.addPreferencesFromResource()` |

#### 11.3.5 AndroidManifest 现代化

- `targetSdkVersion` → 34
- 新增 `READ_EXTERNAL_STORAGE` 显式权限
- `WRITE_EXTERNAL_STORAGE` 添加 `android:maxSdkVersion="32"` (Android 13+ 不再需要)
- `android:exported`: 含 `intent-filter` 的 Activity 标记为 `true`，其余为 `false` (Android 12+ 要求)
- `android:supportsRtl="true"` (国际化支持)
- `android:allowBackup="true"` (云备份)
- `application` 主题改为 `@style/Theme.PianoShelf` (Material)
- `GraphicsActivity` 单独使用 `Theme.PianoShelf.Fullscreen`
- `AppPreferenceActivity` 使用 `Theme.PianoShelf.Settings`

---

### 11.4 P2 级清理 & 细节修复

#### 11.4.1 资源文件修复

- **styles.xml**: 新增三套 Material Design 主题：
  - `Theme.PianoShelf` → `Theme.MaterialComponents.Light.DarkActionBar.Bridge` (主界面)
  - `Theme.PianoShelf.Fullscreen` → `Theme.MaterialComponents.NoActionBar.Bridge` (乐谱查看全屏)
  - `Theme.PianoShelf.Settings` → 带 ActionBar 的设置页主题
- **main.xml**: `fill_parent` → `match_parent` (API 8+ 官方推荐替换)
- **英文 strings.xml**: 补齐缺失的 `menu_stop` / `menu_pause` 键，确保英文环境不崩溃
- **中文 strings.xml**: 补齐缺失的 `menu_stop` / `menu_pause` 键，与英文同步

#### 11.4.2 代码清理

- **GraphicsView**: 移除 `(Activity) context` 多余强制转换（参数类型已是 `GraphicsActivity`）
- 清理所有 Java 文件中未使用的 import 语句
- 各 Activity `onDestroy` 中补齐资源释放：
  - `PianoShelfActivity`: `SoundPoolUtiil.release()` + `dbhelp.close()`
  - `GraphicsActivity`: `graphicsView.shutdown()` + `SoundPoolUtiil.release()`

---

### 11.5 修复前后对比总结

| 维度 | 修复前 | 修复后 |
|------|--------|--------|
| **编译 SDK** | Android 4.4 (API 19) | Android 14 (API 34) |
| **最低支持** | Android 2.2 (API 8) | Android 5.0 (API 21)，覆盖 99%+ 活跃设备 |
| **构建系统** | Apache Ant (废弃) | Gradle + AndroidX (主流) |
| **SQL 安全** | 存在注入漏洞 | 参数化查询，无注入风险 |
| **播放模型** | 每音符 1 线程 (OOM 风险) | 单调度器 + tick 批量调度 (稳定) |
| **文件分享** | Android 7+ 直接崩溃 | FileProvider content:// URI (安全) |
| **权限处理** | Android 6+ 列表空 | 动态授权 + 回调刷新 |
| **UI 主题** | 老式 Android 2.x 风格 | Material Design (现代) |
| **资源泄漏** | Cursor/Thread/SoundPool 未释放 | finally + onDestroy 统一释放 |
| **代码规范** | Deprecated API 滥用 | 全部替换为当前推荐 API |

---

## 12. 新增功能：在线 MusicXML 乐谱下载 & 打开

> 在「本地文件浏览」之外新增了 **Tab 4（在线乐谱）**，支持从推荐的免费 MusicXML 分享平台下载乐谱并直接打开，打通从网络到渲染播放的完整链路。

### 12.1 推荐的免费在线 MusicXML 平台

经过调研，选择 **4 个完全免费 / 公有领域** 主流平台作为内置推荐（预置在 `OnlineScoreDownloader.PRESET_PLATFORMS`）：

| 平台 | 规模 | MusicXML 情况 | 卡片跳转 URL |
|------|------|--------------|-------------|
| **MuseScore** | 100 万+乐谱，全球最大社区 | 公有领域作品可**免费下载 MusicXML**；PRO 订阅可下载版权作品 | https://musescore.com/sheetmusic |
| **IMSLP (Petrucci)** | 18 万+公有领域经典乐谱（巴赫/贝多芬/莫扎特等） | 部分 PDF 包含 **MusicXML 附件**，完全免费 | https://imslp.org/wiki/Main_Page |
| **Mutopia Project** | LilyPond 志愿者高质量排版，古典乐 | 每份乐谱均提供 **MusicXML + PDF 双格式** 免费下载 | http://www.mutopiaproject.org/ |
| **OpenScore (MuseScore)** | 公有领域交互式乐谱开源项目 | MuseScore / MusicXML / PDF / MIDI / MP3 / 盲文 格式齐全 | https://musescore.com/openscore |

> 使用流程（以 MuseScore 为例）：MuseScore.com 打开乐谱 → Share → Download → 选择 MusicXML → 复制链接 → 返回 App 粘贴 URL → 下载自动打开。

---

### 12.2 新增文件 & 依赖

#### 12.2.1 Gradle 依赖升级
在 [pianoshelf.gradle](file:///workspace/pianoshelf.gradle#L66-L79) 增加 OkHttp 4.x 网络栈：
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```
（OkHttp 自带连接池、gzip 透明解压、HTTP/2、TLS 兼容，是 Android 工业级标准选择。）

#### 12.2.2 新增 Java 源代码

| 文件 | 位置 | 职责 |
|------|------|------|
| **OnlineScoreDownloader.java** | [OnlineScoreDownloader.java](file:///workspace/src/com/rojama/pianoshelf/OnlineScoreDownloader.java) | **下载核心**：OkHttp 封装、URL 校验、SHA-256 缓存、进度回调、预置平台信息 |
| **OnlineScoreActivity.java** | [OnlineScoreActivity.java](file:///workspace/src/com/rojama/pianoshelf/OnlineScoreActivity.java) | **下载页**：URL 输入 + 粘贴按钮 + 进度条 + 推荐平台卡片 + 启动 GraphicsActivity |
| **TabOnlineWelcome.java** | [TabOnlineWelcome.java](file:///workspace/src/com/rojama/pianoshelf/TabOnlineWelcome.java) | **主界面 Tab 4 入口**：引导卡片 + 跳转按钮 |

#### 12.2.3 新增资源文件

| 文件 | 位置 | 用途 |
|------|------|------|
| `res/layout/online_score.xml` | [online_score.xml](file:///workspace/res/layout/online_score.xml) | 下载页布局：URL 输入、进度条、平台容器、帮助卡片 |
| `res/layout/online_platform_item.xml` | [online_platform_item.xml](file:///workspace/res/layout/online_platform_item.xml) | 单条平台卡片（CardView），OnlineScoreActivity 动态加载 |
| `res/layout/tab_online_welcome.xml` | [tab_online_welcome.xml](file:///workspace/res/layout/tab_online_welcome.xml) | Tab 4 入口引导卡片布局 |
| `res/drawable/online_tab_icon_bg.xml` | [online_tab_icon_bg.xml](file:///workspace/res/drawable/online_tab_icon_bg.xml) | 在线 Tab 图标圆形渐变背景（替代复杂矢量图） |

---

### 12.3 核心架构设计

#### 12.3.1 OnlineScoreDownloader 设计要点

```
调用方 (UI 线程)
   │  downloadAsync(context, url, force, callback)
   ▼
URL 校验 (http/https + .xml/.mxl/.musicxml 后缀)
   │ 通过
   ▼
计算缓存文件 = cacheDir/online_scores/ + SHA-256(url).hex + 扩展名
   │
   ├─► 存在 && !force → 直接 postOk (命中缓存，0 流量)
   │
   └─► 不存在 → new Thread("PianoShelf-Downloader"):
              ├─ OkHttp.newCall().execute() (15s 连接 / 60s 读超时)
              ├─ 读取 Content-Length（未知则 -1）
              ├─ .part 临时文件写入 + 每 BUFFER_SIZE 刷新进度
              ├─ fsync() 保证刷盘
              └─ renameTo() 原子替换 → postOk
                      │
                      ▼
           Handler(Looper.getMainLooper()) 统一分发回调 (onProgress/onSuccess/onFailure)
```

**关键特性**：
- **SHA-256 去重缓存**：相同 URL 不会重复下载，节省流量
- **.part 原子写入**：避免下载中断导致的损坏文件
- **主线程回调**：UI 代码无需关心线程切换
- **文件大小限制**：MusicXML 纯文本一般 < 5MB；压缩 MXL 更小，不会 OOM
- **缓存清理 API**：`clearCache(context)` 可一键释放磁盘空间

#### 12.3.2 OnlineScoreActivity 交互流程

```
用户进入 Tab 4 「在线乐谱」
   │
   ├─► 点击「进入下载」或 菜单项「在线乐谱」
   │
   ▼
OnlineScoreActivity
   ├─ URL 输入框 (http/https + xml/mxl/musicxml 结尾校验)
   ├─ 「粘贴」按钮 → 读 ClipboardManager 首项文本
   ├─ 「下载并打开」按钮 → 调用 OnlineScoreDownloader.downloadAsync
   │     ├─ 进度条 (Content-Length 已知 = 确定进度；未知 = 不确定动画)
   │     ├─ 失败 → Toast 显示错误
   │     └─ 成功 → 写入最近记录 → FileProvider 包装 → startActivity(GraphicsActivity)
   │
   └─ 推荐平台 4 张卡片（动态构建）
         └─ 点击 → Intent.ACTION_VIEW 打开外部浏览器
```

---

### 12.4 与现有模块的集成点

| 集成点 | 说明 |
|-------|------|
| **主界面 Tab** | [main.xml](file:///workspace/res/layout/main.xml#L19-L23) 新增 `tab_online` 容器；[PianoShelfActivity.java](file:///workspace/src/com/rojama/pianoshelf/PianoShelfActivity.java#L57-L70) 注册第 4 个 Tab，挂载 `TabOnlineWelcome` |
| **菜单项** | 主界面选项菜单新增 **「在线乐谱」** 入口（与设置/退出并列） |
| **最近记录** | 在线下载成功后，**本地缓存路径写入 DatabaseHelper RECENT 表**，下次可从「最近打开」直接再次打开（无需重下） |
| **FileProvider** | 下载完成后走与本地文件**完全一致**的启动通路：`FileProvider.getUriForFile()` + `FLAG_GRANT_READ_URI_PERMISSION` → GraphicsActivity，避免重复开发打开逻辑 |
| **权限** | `INTERNET`（正常权限，Manifest 声明即获得）已在原 Manifest 中存在，直接复用 |
| **多语言** | [values/strings.xml](file:///workspace/res/values/strings.xml) + [values-zh-rCN/strings.xml](file:///workspace/res/values-zh-rCN/strings.xml) 各新增约 25 个在线模块专用字符串键，中英文完全同步 |

---

### 12.5 用户使用路径总结

```
【路径 1】主界面 → Tab 4「在线」 → 卡片「进入下载」
        ↓
     URL 输入框 粘贴 或 访问推荐平台复制直链
        ↓
     下载 → 自动跳转 GraphicsActivity 渲染 + 播放
        ↓
     之后可直接在「最近打开」Tab 重新打开（缓存命中）

【路径 2】主界面 → 菜单键 → 「在线乐谱」
        ↓（直接跳到 OnlineScoreActivity）
     同上流程
```

所有在线下载的乐谱都会被自动加入「最近打开」列表，并且缓存到 `{app_cache}/online_scores/` 目录，在存储空间足够的情况下可以零流量重开。

---

## 13. 新增功能（进阶）：应用内爬取分类目录 + 平台内 WebView 直接下载

> 用户不再需要手动复制 URL 到外部浏览器。现在 **推荐平台卡片** 点击后，在应用内即可完成「浏览分类 → 选作品 → 一键下载打开」的完整闭环。
>
> 采用 **双轨架构**（针对不同平台特性选择最优实现）：
>   - **轨道 A · 应用内目录浏览器（Mutopia Project）**：平台 CGI 结构规整、每首必有 MusicXML → 直接解析，用原生 ListView 展示，体验最流畅
>   - **轨道 B · 内置 WebView + 链接拦截（MuseScore / IMSLP / OpenScore）**：平台是 SPA 或结构复杂 → 以 WebView 形式打开，JS/Cookie 完全兼容，当用户点击 MusicXML 文件链接时自动拦截并调用下载器

### 13.1 双轨架构总览

```
                 【OnlineScoreActivity 推荐平台卡片】
                                  │
         ┌────────────────────────┴────────────────────────┐
         │ Mutopia Project（按钮显示「应用内浏览」）        │ 其它 3 个平台（按钮显示「访问」）
         ▼                                                 ▼
   InAppBrowseActivity                             PlatformWebViewActivity
         │                                                 │
         │ Tab 1：作曲家 (A-Z 200+)                       │ WebView 加载真实网页
         │ Tab 2：乐器 (Piano/Guitar/Violin/...)           │ - JS/Cookie/缩放全启用
         │ Tab 3：风格 (Baroque/Classical/Romantic/...)    │ - 顶部加载进度条
         ▼                                                 │
   【点击分类行】                                           │ shouldOverrideUrlLoading
         │ loadPieces(make-table.cgi?Composer=Xxx)         │   拦截 .xml/.mxl/.musicxml 直链
         ▼                                                 │   → 交 OnlineScoreDownloader
   【作品列表 ListView】                                    │
   - 标题 / 作曲家 / 乐器 / 风格                           │ setDownloadListener(按钮下载)
   - 行右侧「获取」按钮                                     │   - 命中 MusicXML → App 内下载
         │ 点击作品 或 点击按钮                             │   - 其他类型 → DownloadManager
         ▼                                                 │   - 完成后若为 MusicXML 自动打开
 MutopiaCatalogParser.resolveMusicXmlUrl(piece-info.cgi)   │
         │ 从详情页解析出 ftp/...-musicxml.xml.gz           │
         ▼                                                 │
 OnlineScoreDownloader.downloadAsync → 缓存 → 写入 RECENT  │
         ▼                                                 │
  GraphicsActivity（渲染 + 播放 + 翻页 + 收藏）◄───────────┘
```

### 13.2 新增 Java 源码（核心实现）

| 文件 | 说明 | 行数 (约) |
|------|------|-----------|
| [MutopiaCatalogParser.java](file:///workspace/src/com/rojama/pianoshelf/MutopiaCatalogParser.java) | Mutopia 爬虫核心：**轻量正则解析**（不引入 jsoup 减体积），6 个静态方法 + 3 种分类解析 + make-table 作品列表解析 + piece-info MusicXML 直链解析，全部独立线程 | ~400 |
| [InAppBrowseActivity.java](file:///workspace/src/com/rojama/pianoshelf/InAppBrowseActivity.java) | 目录浏览 UI：**3 维度 Material TabLayout**，2 层栈式导航（分类层 ⇄ 作品层，`onBackPressed` 返回键 & ActionBar 回退键双支持），独立 Category / Piece 两个 ListAdapter | ~360 |
| [PlatformWebViewActivity.java](file:///workspace/src/com/rojama/pianoshelf/PlatformWebViewActivity.java) | 平台浏览器：`WebChromeClient` 进度回调 + `WebViewClient` URL 拦截 + `setDownloadListener` MimeType 分流；第三方 Cookies 与 `MIXED_CONTENT_COMPATIBILITY_MODE` 解决 MuseScore 登录 & 混合内容下载；非 MusicXML 走 `DownloadManager` + 广播监听完成 → 自动识别打开 | ~340 |

### 13.3 新增资源（布局 + 图像 + 多语言）

**布局（3×列表行 + 主页面 + WebView 页面）**：
- `res/layout/inapp_browse.xml` — 目录浏览器主页（TabLayout + ListView + 空态/加载态）
- `res/layout/item_category.xml` — 分类行（4dp 彩色指示条 + 名/副标题/数量角标）
- `res/layout/item_piece.xml` — 作品行（标题 + 作曲家·乐器标签 + 风格小字 + 右侧「获取」Button）
- `res/layout/platform_webview.xml` — WebView 容器（顶部水平 ProgressBar + 中心圆形进度）
- `res/drawable/catalog_indicator.xml` — 分类侧边指示条（圆角矩形 Material Indigo 500）

**多语言 strings（25 个键，中英文完全同步）**：

| 分类 | 键示例 |
|------|--------|
| 目录浏览器 | `browse_title` / `browse_tab_composer` / `browse_loading_pieces` / `browse_resolving` / `browse_dl_starting` |
| WebView 浏览器 | `webview_default_title` / `webview_intercepted_dl` / `webview_sysdl_starting` / `webview_perm_denied` |
| 平台卡片扩展 | `online_btn_inapp` = 「应用内浏览 / Browse in App」（Mutopia 按钮特殊文案） |

**AndroidManifest**：`InAppBrowseActivity` + `PlatformWebViewActivity` 两个 Activity 已注册（`exported=false` + 横竖屏/屏幕尺寸 configChanges 防止销毁重建）。

### 13.4 关键技术决策

#### 13.4.1 为何选择 Mutopia 做「应用内目录」？
- **结构稳定可预测**：全站由 `composers.html` → `instruments.html` → `browse.html` → `make-table.cgi` → `piece-info.cgi` 组成，20+ 年结构几乎没变；
- **MusicXML 覆盖率 100%**：所有 LilyPond 源文件均会导出 `-musicxml.xml.gz`；
- **无反爬 & 无版权墙**：所有作品均为公有领域，无登录、无 CAPTCHA、无 CF 盾，轻量正则即可解析；
- **体积最小**：仅用 OkHttp（已加依赖）+ `java.util.regex`，零额外 APK 体积增量。

#### 13.4.2 为何其余平台使用 WebView？
- **MuseScore/IMSLP** 存在 SPA（React/Vue）+ 登录墙 + Cloudflare 保护，原生解析成本极高且易失效；
- **WebView 是唯一语义一致的方案**：CSS/JS/Cookie/富交互完全保持平台体验；
- **关键链路仍打通**：只要用户在 WebView 内点到 MusicXML 下载（或服务器触发下载），`shouldOverrideUrlLoading` + `DownloadListener` **双保险**都会把 URL 截获到 App 内，交给 `OnlineScoreDownloader` 并 `FileProvider` → `GraphicsActivity`，零断点。

#### 13.4.3 性能与合规
- **网络**：OkHttp 自带连接池，Mutopia 每次打开分类 ~60-200KB，低流量；
- **爬取礼节**：所有 Mutopia 请求带 UA 头 `PianoShelf-Android/2.0 (compat; +https://www.mutopiaproject.org/)`，同时解析层**不并行不并发**，每次用户点击才发起一个请求（约等于一个普通用户在浏览器里点网页）；
- **版权**：严格只下载「MusicXML 文件」，不为分类结果做图片抓取 / 全文缓存；Mutopia 所有文件均为 Public Domain / CC BY-SA，与 App 教育用途完全吻合。
- **存储**：低版本写 Download 目录时需 `WRITE_EXTERNAL_STORAGE`，已在 `PlatformWebViewActivity` 实现运行时申请 + 结果回调；Android 10+ 用 `setDestinationInExternalPublicDir` 无需权限。

### 13.5 用户现在的 5 条使用路径

```
【路径 1：Mutopia 应用内直达 👑 推荐】
Tab 4「在线」 → OnlineScoreActivity
  → Mutopia Project 卡片  → 「应用内浏览」按钮
  → InAppBrowseActivity [Tab: 作曲家/乐器/风格]
  → 选分类（如: Composer=BachJS, Instrument=Piano, Style=Baroque）
  → 选作品行 / 点「获取」
  → 自动解析 MusicXML URL → 下载 → GraphicsActivity 打开

【路径 2：MuseScore / IMSLP / OpenScore 应用内 WebView】
同上 OnlineScoreActivity 卡片
  → PlatformWebViewActivity（像普通浏览器一样浏览网站）
  → 用户点击页面内任何 .xml / .mxl / .musicxml 链接
  → 自动拦截 → 下载 → 立即打开（不离开 App）

【路径 3：平台 WebView 内点下载按钮（非链接）】
  → setDownloadListener 触发，按 MimeType/URL 判断
  → MusicXML → 交 OnlineScoreDownloader；其它 → DownloadManager；
  → DownloadManager 完成时若检测为 MusicXML → 也会自动打开

【路径 4：传统粘贴直链】（保留不删）
OnlineScoreActivity → URL 输入框 / 粘贴 → 下载并打开

【路径 5：菜单项进入】
主界面 Menu「在线乐谱」→ 直达 OnlineScoreActivity（含 4 张平台卡片）
```

**所有 5 条路径最终都汇聚到：OnlineScoreDownloader 下载 → 写 DatabaseHelper.RECENT → GraphicsActivity 渲染/播放，并且之后可以直接在「最近打开」Tab 零流量重开（缓存命中）**，形成了高度一致的数据闭环。
