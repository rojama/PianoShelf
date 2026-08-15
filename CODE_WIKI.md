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
