# Recorder 项目开发记录

## 已完成功能

### Phase 1: 核心功能（已完成 ✅）

#### 数据层
- ✅ **ThoughtColor.kt**: 8种颜色枚举（红/橙/黄/绿/青/蓝/紫/黑）
- ✅ **Thought.kt**: 核心数据实体（Room Entity）
  - 字段：id, title, content, audioPath, color, alarmTime, createdAt, transcribedAt, isTranscribed
- ✅ **Converters.kt**: Room 类型转换器（LocalDateTime, ThoughtColor）
- ✅ **ThoughtDao.kt**: 数据访问接口（使用 Flow 实现响应式查询）
  - `getTranscribedThoughts()`: 获取已转换的感言
  - `getOriginalThoughts()`: 获取原始感言
  - `getExpiredAlarmThoughts()`: 获取闹钟已过的感言
- ✅ **ThoughtDatabase.kt**: Room 数据库单例
- ✅ **ThoughtRepository.kt**: 数据仓库
  - 管理数据库和文件系统操作
  - 提供 Flow 用于 UI 观察

#### 音频管理
- ✅ **AudioRecorder.kt**: 录音管理
  - 封装 MediaRecorder
  - StateFlow 管理录音状态
  - 音频格式：M4A (AAC)，128kbps
  - 存储位置：`context.filesDir/recordings/`
- ✅ **AudioPlayer.kt**: 播放管理
  - 封装 MediaPlayer
  - StateFlow 管理播放状态
  - 支持播放、暂停、恢复、停止
  - **已修复**: 播放完成后正确重置状态

#### 工具类
- ✅ **TimeExtensions.kt**: 时间格式化工具
- ✅ **PermissionHandler.kt**: 权限请求封装

#### ViewModel
- ✅ **ThoughtListViewModel.kt**: 状态管理核心
  - 管理感言列表（三个分类）
  - 管理录音状态
  - 管理播放状态
  - 管理选择状态（单选/多选）
  - 管理颜色筛选
  - 增删改查操作

#### UI 组件
- ✅ **RecordButton.kt**: 录音 FAB（右下角浮动按钮）
- ✅ **RecorderTopBar.kt**: 顶部标题栏
- ✅ **ThoughtToolbar.kt**: 工具栏（批量转换、设置提醒、设置颜色、删除、筛选）
- ✅ **ThoughtItem.kt**: 三种感言卡片
  - `TranscribedThoughtItem`: 已转换感言
  - `OriginalThoughtItem`: 原始感言
  - `ExpiredThoughtItem`: 闹钟已过的感言
  - **已优化**: 颜色圆形从 32dp 缩小到 16dp，移至播放按钮左侧
- ✅ **ThoughtList.kt**: 感言列表（LazyColumn 分三个区域）
  - **已修复**: 播放暂停图标正确更新
- ✅ **RecorderScreen.kt**: 主屏幕（整合所有组件）

#### 配置
- ✅ **gradle 依赖**: Room, KSP, ViewModel, Accompanist, Material Icons Extended
- ✅ **AndroidManifest.xml**: RECORD_AUDIO 权限

---

### Phase 2: 语音转文本（已完成 ✅）

#### Whisper 离线识别（2026-01-30 完成）
- ✅ **WhisperHelper.kt**: Whisper 模型封装
  - 使用 sherpa-onnx 库
  - OpenAI Whisper tiny 模型
  - 完全离线运行
  - 支持从 assets 加载模型
  - 单例模式，避免重复初始化
- ✅ **AudioDecoder.kt**: 音频解码工具
  - 使用 MediaCodec 解码 M4A/MP3/WAV
  - 自动转换为 16kHz 单声道 PCM
  - 线性插值重采样
  - 立体声转单声道（通道平均）
- ✅ **SpeechToTextHelper.kt**: 语音识别封装
  - 真实 Whisper 实现（替换占位代码）
  - 自动从识别文本生成标题
  - 错误处理和降级方案
  - 单例模式，管理 Whisper 生命周期
- ✅ **ThoughtListViewModel.kt**: 更新转换逻辑
  - 集成 SpeechToTextHelper
  - 启动时初始化 Whisper
  - 显示加载状态
  - 完善错误处理
- ✅ **convertSelectedThoughts()**: 批量转换功能
- ✅ **editThought()**: 手动编辑功能
- ✅ **EditThoughtDialog.kt**: 编辑对话框（标题+内容）

#### 依赖配置
- ✅ **build.gradle.kts**: 添加 sherpa-onnx AAR 依赖
- ✅ **README_WHISPER.md**: 模型文件下载和安装指南

---

### Phase 3: 高级功能（部分完成 ⚠️）

#### 颜色标记与筛选（已完成 ✅）
- ✅ **ColorPickerDialog.kt**: 颜色选择对话框（8种颜色）
- ✅ **ColorFilterDialog.kt**: 颜色筛选对话框（多选+全选/清除）
- ✅ **setColorForSelectedThoughts()**: 为选中感言设置颜色
- ✅ **setColorFilter()**: 颜色筛选功能

#### 闹钟提醒（已完成 ✅）
- ✅ **AlarmHelper.kt**: 闹钟调度工具
  - **已修复**: Android 12+ 兼容性（canScheduleExactAlarms 检查）
- ✅ **AlarmReceiver.kt**: 闹钟广播接收器
- ✅ **AlarmTimePickerDialog.kt**: 闹钟时间选择对话框（简单版本）
- ✅ **setAlarmForSelectedThoughts()**: 为选中感言设置闹钟
- ✅ **AndroidManifest.xml**: SCHEDULE_EXACT_ALARM, POST_NOTIFICATIONS 权限

---

## 已修复 Bug

### 1. Material Icons 缺失 ✅
- **问题**: 编译时找不到 Mic, Stop, Message, Pause, PlayArrow 等图标
- **解决**: 添加 `material-icons-extended:1.7.6` 依赖

### 2. 播放暂停图标不更新 ✅
- **问题**: 播放暂停后图标仍显示暂停符号，无法重播
- **原因**: `isPlaying` 状态未正确传递到 ThoughtItem 组件
- **解决**:
  - ThoughtList.kt: 添加 `isPlaying` 参数并正确传递
  - AudioPlayer.kt: 修复 `setOnCompletionListener` 正确重置状态

### 3. 设置闹钟崩溃 ✅
- **问题**: 点击设置闹钟后应用闪退
- **原因**: Android 12+ 需要先检查 `canScheduleExactAlarms()` 权限
- **解决**: AlarmHelper.kt 添加版本检查和降级方案

### 4. 闹钟通知未触发 ✅
- **问题**: 设置闹钟后时间到了没有系统通知
- **原因**:
  - 通知权限未正确请求（Android 13+）
  - 通知效果不够明显
- **解决**:
  - RecorderScreen.kt: 添加通知权限请求和说明对话框
  - AlarmReceiver.kt: 改进通知实现（声音、振动、全屏通知）
  - AndroidManifest.xml: 添加 VIBRATE 和 USE_FULL_SCREEN_INTENT 权限

---

## UI/UX 改进（已完成 2026-01-29）

### 1. 选择逻辑改进 ✅
- **需求**: 将长按选择改为选择框（圆角矩形）
- **实现**:
  - 在每条感言左侧添加 24dp 圆角矩形选择框
  - 选中时显示蓝色背景和对号图标
  - 移除 `combinedClickable` 和长按逻辑
  - 点击选择框切换选中状态，点击卡片播放音频
- **影响文件**:
  - ThoughtItem.kt（三个变体）
  - ThoughtList.kt
  - RecorderScreen.kt

### 2. 轮盘式时间选择器 ✅ → 垂直滚轮选择器 ✅
- **第一版（已废弃）**: 半圆形轮盘式滑动选择器
  - 创建 WheelTimePickerDialog.kt
  - 年月选择：点击展开浮动框，使用 +/- 按钮切换
  - 日期/时间选择：半圆形轮盘滑动选择器（SemicircleWheelPicker）

- **第二版（当前）**: iOS 风格垂直滚轮选择器（2026-01-29）
  - **DrumRollPicker 组件**：
    - 使用 LazyColumn 实现垂直滚动列表
    - 5 个可见项，中心项为选中值
    - 渐变效果：字号、颜色、透明度从中心向两端递减
    - 循环滚动（月份、日期、小时、分钟）
  - **布局**：
    - 第一组：年/月/日（用 `/` 分割，32sp 大字展示）
    - 第二组：时:分（用 `:` 分割，32sp 大字展示）
  - **交互**：
    - 弹性滚动动画（rememberSnapFlingBehavior）
    - 振动反馈（HapticFeedbackConstants.CLOCK_TICK）
    - 日期动态计算，超出范围自动调整
- **影响文件**:
  - 重写 WheelTimePickerDialog.kt
  - RecorderScreen.kt（使用新选择器）

### 3. 声波图像可视化 ✅
- **需求**: 为每条录音添加声波图像
- **实现**:
  - 创建 WaveformView.kt 组件
  - 基于音频文件哈希生成一致的伪波形（60个柱状条）
  - 显示播放进度竖线（蓝色，2dp宽）
  - 已播放部分显示蓝色，未播放部分显示灰色
  - 显示录音长度（基于文件大小估算）和当前播放时间
  - 在 ThoughtListViewModel 中添加定期更新播放进度（每100ms）
  - Canvas 绘制，性能优化
- **影响文件**:
  - 新建 WaveformView.kt
  - ThoughtItem.kt（三个变体）
  - ThoughtList.kt
  - RecorderScreen.kt
  - ThoughtListViewModel.kt

---

## 待实现功能

---

## 技术架构总结

### 架构模式
- **MVVM + Clean Architecture**
- **数据层**: Room Database + File System
- **领域层**: Repository
- **展示层**: ViewModel + Jetpack Compose

### 关键技术
- **UI**: Jetpack Compose + Material 3
- **数据库**: Room + Flow
- **状态管理**: StateFlow
- **音频**: MediaRecorder + MediaPlayer
- **依赖注入**: 手动注入（未使用 Hilt/Dagger）

### 项目结构
```
org/haokee/recorder/
├── data/
│   ├── local/         (Room: Database, Dao, Converters)
│   ├── model/         (Thought, ThoughtColor)
│   └── repository/    (ThoughtRepository)
├── ui/
│   ├── screen/        (RecorderScreen)
│   ├── component/     (各种 UI 组件)
│   ├── viewmodel/     (ThoughtListViewModel)
│   └── theme/         (主题配置)
├── audio/
│   ├── recorder/      (AudioRecorder)
│   └── player/        (AudioPlayer)
├── alarm/             (AlarmHelper, AlarmReceiver)
└── util/              (TimeExtensions, PermissionHandler)
```

---

## 开发规范

### Git 提交规则
- 阶段性开发完成后先 `git add -A`，然后一次性提交
- 提交信息有意义（如 "实现 Phase 1 核心功能"）
- **不推送**到远程仓库

### 构建规则
- 开发者不主动运行构建命令
- 由用户手动构建

### 文档更新规则
- **每次接收到修改需求必须先更新 CLAUDE.md**
- 在 "需求变更记录" 章节记录变更

---

## 下一步计划

按优先级排序：

1. ✅ ~~选择框替代长按~~ - 已完成（2026-01-29）
2. ✅ ~~轮盘式时间选择器~~ - 已完成（2026-01-29）
3. ✅ ~~声波图像可视化~~ - 已完成（2026-01-29）
4. ✅ ~~修复闹钟通知~~ - 已完成（2026-01-29）
5. **Phase 4: 大模型集成** - 对话功能、标题生成
6. **Phase 4: 设置页面** - 主题切换、数据管理
7. **优化声波图像** - 使用 MediaMetadataRetriever 获取真实音频时长和波形数据

---

## 开发进度总结

### 2026-01-29 开发成果（第一阶段）
本次开发完成了所有待实现的 UI/UX 改进功能，共 4 次提交：

1. **e0c977a** - 实现选择框替代长按功能
   - 移除长按逻辑，添加圆角矩形选择框
   - 改进交互体验

2. **c7637ba** - 实现半圆形轮盘式时间选择器（已废弃）
   - 创建半圆形轮盘选择器组件
   - 正确计算闰年和大小月
   - 添加流畅动画效果

3. **c37bb27** - 实现声波图像可视化功能
   - 创建波形可视化组件
   - 显示播放进度和录音长度
   - 添加播放进度定期更新

4. **f3a73b1** - 修复闹钟通知功能
   - 添加通知权限请求
   - 改进通知效果（声音、振动、全屏）

5. **cdf404f** - 修复编译错误
   - 添加缺失的 Canvas 和 Size 导入

6. **7a0f881** - 启用 Java 8+ API desugaring
   - 解决 java.time API 在低版本 Android 的兼容性问题

### 2026-01-29 开发成果（第二阶段 - 时间选择器重构）

7. **99b4166** - 重新设计时间选择器为垂直滚轮样式
   - 废弃半圆形轮盘设计，改为 iOS 风格垂直滚轮
   - 实现 DrumRollPicker 组件（垂直滚轮选择器）
   - 渐变淡出效果（字号、颜色、透明度）
   - 循环滚动（月份、日期、小时、分钟）
   - 振动反馈（每次切换触发）
   - 弹性滚动动画
   - 日期动态计算和自动调整
   - 更新 CLAUDE.md 记录需求变更

8. **5b1b400** - 修复时间选择器的关键问题
   - 日期回滚到正确值（30日而非01日）
   - 实现惯性滚动（VelocityTracker + animateDecay）
   - 优化透明度为离散值（100% → 66% → 33% → 0%）
   - 添加 clipToBounds 限制显示范围

9. **b5a7da6** - 清理不需要的导入

10. **26704bd** - 修复日期选择器同步问题
    - 添加 key 参数到 remember 依赖
    - 修复负数取模问题
    - 使用 effectiveDay 确保日期有效性

11. **d2dde14** - 修复三个关键问题的最终版本
    - 完善惯性滚动实现
    - 优化渲染性能
    - 修复循环滚动边界问题

12. **5d0b15e** + **ae17a74** - 修复编译错误
    - 移除不存在的 cancelAnimation API
    - 简化边界检查逻辑

13. **2da1af1** - 修复拖动松手和显示同步
    - 修复拖动松手后无法吸附的问题
    - 使用 snapshotFlow 确保实时更新显示

14. **885be21** - 完全重写为模运算架构（最终版本）
    - **核心改进**：从像素滚动改为基于项目索引的滚动（scrollIndex）
    - 移除列表三倍复制（`items + items + items`），使用模运算实现真正的无限循环
    - 只渲染 ±7 个项目（最多 15 个），通过模运算重用控件
    - 月份只需 12 个控件，小时只需 24 个，分钟只需 60 个
    - 修复快速滚动时的空白帧问题
    - 简化架构，性能显著提升
    - 关键算法：`actualIndex = ((index % items.size) + items.size) % items.size`

**技术亮点**：
- 使用 Compose Canvas 绘制自定义 UI
- 模运算实现高效无限循环滚动（最少控件数量）
- Animatable + VelocityTracker 实现流畅惯性滚动
- graphicsLayer 实现渐变动画（透明度、缩放、字体）
- HapticFeedback 触觉反馈（CLOCK_TICK）
- snapshotFlow 实现实时状态同步
- 协程配合 Flow 实现响应式状态更新
- 运行时权限管理（Android 13+ 兼容）
- Core Library Desugaring 兼容低版本 Android

---

### 2026-01-30 开发成果 - Whisper 离线语音识别集成

完成了 Phase 2 语音转文本功能的真实实现，使用 OpenAI Whisper tiny 模型实现完全离线的语音识别。

#### 新增文件
1. **WhisperHelper.kt** (210 行)
   - 封装 sherpa-onnx 的 OfflineRecognizer API
   - 单例模式管理 Whisper 模型生命周期
   - 从 assets 加载模型文件
   - 提供简洁的 transcribe 接口
   - 完善的错误处理和日志

2. **AudioDecoder.kt** (170 行)
   - 使用 MediaCodec 解码音频文件
   - 支持 M4A、MP3、WAV 等格式
   - 自动转换为 16kHz 单声道 PCM
   - 线性插值重采样算法
   - 立体声转单声道（通道平均）

3. **README_WHISPER.md** (完整文档)
   - Whisper 模型下载和安装指南
   - AAR 文件配置说明
   - 常见问题解答
   - 技术细节说明

#### 修改文件
4. **SpeechToTextHelper.kt** - 完全重写
   - 从 object 改为 class（单例模式）
   - 替换占位实现为真实 Whisper 调用
   - 添加 initialize() 方法初始化模型
   - convertThought() 改为挂起函数
   - 从识别文本自动生成标题
   - 完善的错误处理和降级方案

5. **ThoughtListViewModel.kt** - 集成 Whisper
   - 添加 Context 参数
   - 集成 SpeechToTextHelper 单例
   - init 中初始化 Whisper 模型
   - convertSelectedThoughts() 添加加载状态
   - onCleared() 中释放 Whisper 资源

6. **ThoughtViewModelFactory.kt** - 更新工厂
   - 添加 Context 参数
   - 传递 Context 到 ViewModel

7. **MainActivity.kt** - 更新初始化
   - 传入 applicationContext 到 ViewModelFactory

8. **build.gradle.kts** - 添加依赖
   - 配置 flatDir 本地仓库
   - 添加 sherpa-onnx AAR 依赖引用
   - 注释说明下载链接

9. **CLAUDE.md** - 文档更新
   - 更新语音转文本技术实现说明
   - 添加 Whisper 需求变更记录

#### 技术特点
- **完全离线**：无需网络连接，保护用户隐私
- **高性能**：Whisper tiny 模型（~75MB），int8 量化，推理速度快
- **自动解码**：支持多种音频格式，自动重采样和转换
- **错误容错**：模型未加载时降级为默认文本，不影响应用运行
- **资源管理**：正确的初始化和释放流程，避免内存泄漏

#### 待完成事项
- [ ] 下载并放置 sherpa-onnx AAR 文件到 `app/libs/`
- [ ] 下载并放置 Whisper 模型文件到 `app/src/main/assets/models/whisper-tiny/`
- [ ] 首次构建测试（需要手动下载文件后）

**注意**：由于模型文件较大（AAR 27.4MB + 模型 75MB），未直接提交到代码仓库。用户需要按照 README_WHISPER.md 的说明手动下载和配置。

---

## 当前项目状态（截至 2026-01-29）

### 已完成功能模块

#### ✅ Phase 1: 核心功能（100%）
- 数据层：Room 数据库、数据仓库、类型转换器
- 音频管理：录音（MediaRecorder）、播放（MediaPlayer）
- UI 组件：录音按钮、顶部栏、工具栏、感言列表、感言卡片
- 状态管理：ViewModel、StateFlow、Flow
- 权限处理：录音权限、通知权限、闹钟权限

#### ✅ Phase 2: 语音转文本（100%）
- SpeechToTextHelper 封装（占位实现）
- 批量转换功能
- 手动编辑对话框
- 标题和内容编辑

#### ✅ Phase 3: 高级功能（100%）
- 颜色标记系统（8 种颜色）
- 颜色选择对话框
- 颜色筛选功能（多选）
- 闹钟提醒系统（AlarmManager）
- 闹钟广播接收器
- 通知系统（声音、振动、全屏通知）
- 时间选择器（iOS 风格垂直滚轮，模运算实现）

#### ✅ UI/UX 优化（100%）
- 选择框交互（替代长按）
- 声波图像可视化
- 播放进度实时显示
- 颜色标记优化（16dp 圆形，位于播放按钮左侧）
- 时间选择器（垂直滚轮，渐变效果，惯性滚动，触觉反馈）

### 待实现功能模块

#### ⏳ Phase 4: 大模型集成
- [ ] API 配置管理（Base URL、API Key）
- [ ] 标题生成功能（基于 Whisper 识别结果优化）
- [ ] 左侧抽屉式对话界面
- [ ] Markdown 渲染
- [ ] 单轮对话实现

#### ⏳ Phase 4: 设置页面
- [ ] 大模型 API 设置界面
- [ ] 主题切换（亮色/暗色）
- [ ] 数据管理（清除所有感言）
- [ ] 关于页面（版本号）

#### ⏳ 优化项
- [ ] 声波图像真实数据（MediaMetadataRetriever）
- [x] ~~Whisper 语音识别真实实现~~ ✅ 已完成（2026-01-30）
- [ ] 错误处理完善
- [ ] 加载状态显示

### 技术债务
- **Whisper 模型文件未内置**：需要用户手动下载 AAR 和模型文件（见 README_WHISPER.md）
- 代码结构清晰，遵循 MVVM 架构
- 所有已知 Bug 已修复

### 下一步建议
1. **优先级 1**：内置 Whisper 模型文件到 APK（或实现首次启动自动下载）
2. **优先级 2**：实现大模型集成（对话功能、标题生成）
3. **优先级 3**：实现设置页面（API 配置、主题切换）
4. **优先级 4**：优化声波图像（真实音频数据）

---

*最后更新: 2026-01-30*
