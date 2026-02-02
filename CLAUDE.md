# Recorder 项目规格说明

## 项目概述

**项目名称**：Recorder
**包名**：org.haokee.recorder
**类型**：Android 原生应用
**核心价值**：让用户随时随地用录音记录想法，闲暇时转换为文本整理

---

## 核心概念：感言（Thought）

感言是本应用的核心数据单元，分为两种状态：

### 1. 原始感言（Original Thought）
- **形式**：音频录音文件
- **存储**：本地文件系统
- **可操作**：播放、删除、转换为文本、设置提醒、标记颜色

### 2. 转换后感言（Translated Thought）
- **属性**：
  - `title`（标题）：文本字符串
  - `content`（内容）：文本字符串
  - `audioPath`（音频路径）：原始录音文件路径
  - `color`（颜色标记）：枚举值（红/橙/黄/绿/青/蓝/紫/黑）
  - `alarmTime`（提醒时间）：可选的 DateTime
  - `createdAt`（创建时间）：DateTime
  - `transcribedAt`（转换时间）：DateTime

---

## 功能规格

### 一、录音管理

#### 录音功能
- 点击录音按钮开始录制
- 再次点击停止并保存
- 音频格式：建议使用 AAC/M4A（兼容性好，文件小）
- 存储位置：应用私有目录

#### 批量操作
- **多选模式**：点击感言左侧的圆角矩形选择框进入多选模式
- **批量转换**：将多条原始感言转换为文本
- **批量删除**：删除选中的感言（需二次确认）
- **批量设置颜色**：为选中的感言统一设置颜色
- **批量设置提醒**：为选中的感言设置相同的提醒时间

### 二、语音转文本

#### 技术实现
- **模型**：OpenAI Whisper (tiny) - 本地离线模型
- **库**：whisper.cpp Android 绑定
- **模型文件**：内置在 APK 的 assets 目录（~75MB）
- **触发方式**：
  - 单条：点击感言的"转换"按钮
  - 批量：选中多条后点击工具栏"批量转换"按钮
- **转换逻辑**：
  1. 加载 Whisper 模型（首次启动时从 assets 复制到内部存储）
  2. 调用 Whisper 推理引擎识别音频文件
  3. 获取识别文本结果
  4. 默认 `title = content = 识别文本`
  5. 如果启用大模型 API，调用大模型生成标题
  6. 保存到数据库，状态变为"已转换"

#### 手动编辑
- **单选模式**：选中单条感言时，启用"编辑"按钮（蓝色可点击）
- **多选模式**：选中多条感言时，"编辑"按钮灰色禁用
- **编辑界面**：弹出对话框，可修改标题和内容

### 三、大模型集成

#### API 配置
- **协议**：OpenAI 兼容接口
- **配置项**：
  - Base URL（支持自定义或预设）
  - API Key（用户自己提供）
  - 启用/禁用开关
- **功能**：
  - **生成标题**：基于 content 总结出简短标题
  - **对话功能**：侧边栏聊天（单轮对话，支持清除上下文）

#### 对话界面
- **位置**：左侧抽屉式弹出页面
- **功能**：
  - 发送消息到大模型
  - 显示 AI 回复（支持 Markdown 渲染）
  - "清除上下文"按钮：用分割线表示，但不删除历史消息显示
  - 不支持多轮上下文（每次发送独立请求）

### 四、提醒功能

#### 闹钟设置
- **实现方式**：使用 Android `AlarmManager` 设置系统闹钟
- **唤醒能力**：即使设备休眠也能响铃
- **闹钟标题**：
  - 已转换感言：使用感言的 `title`
  - 原始感言：使用默认文本"感言提醒"
- **批量设置**：可为多条感言设置相同时间

#### 闹钟管理
- **已过期闹钟**：在列表中以灰色显示
- **取消闹钟**：支持单独取消或批量取消

### 五、颜色标记

- **颜色选项**：红、橙、黄、绿、青、蓝、紫、黑（共 8 种）
- **显示样式**：小圆形纯色块，位于播放按钮左侧
- **筛选功能**：
  - 工具栏右侧筛选按钮
  - 可多选颜色进行筛选
  - 全选/全不选快捷按钮

---

## UI/UX 设计规范

### 主题风格
- **默认主题**：浅色（Light Mode）
- **可选主题**：暗色（Dark Mode）
- **主色调**：蓝色系
- **设计理念**：现代、简洁、优雅

### 布局结构

#### 1. 顶部标题栏（固定）
```
[大模型对话按钮] [Recorder 大标题] [设置按钮]
                [记录一时感言 小标题]
```

- **大模型对话按钮**：点击后左侧滑出抽屉式对话页面
- **设置按钮**：打开设置页面

#### 2. 工具栏（固定）
```
[批量转换] [设置提醒] [设置颜色] [删除]    [筛选按钮]
```

- **按钮状态**：
  - 未选中感言：所有按钮灰色禁用
  - 选中单条：所有按钮启用
  - 选中多条："编辑"按钮禁用，其他启用
- **筛选按钮**：展开颜色筛选面板

#### 3. 感言列表（可滚动）

**分为三个区域**：

1. **已转换感言**
   - 左侧：圆角矩形选择框
   - 中间：标题、内容预览、声波图像、播放进度、录音长度
   - 右侧：颜色标记（小圆形）、播放按钮
   - 显示创建时间和提醒时间（如果有）

2. **原始感言**
   - 左侧：圆角矩形选择框
   - 中间：声波图像、播放进度、录音时长
   - 右侧：颜色标记（小圆形）、播放按钮
   - 显示"未转换"标签、创建时间

3. **闹钟已过的感言**
   - 灰色显示
   - 其他信息同上

#### 4. 录音按钮（浮动）
- **位置**：屏幕右下角 FAB（Floating Action Button）
- **样式**：蓝色圆形按钮，麦克风图标
- **状态**：
  - 未录音：蓝色，麦克风图标
  - 录音中：红色，方块图标（停止）

### 设置页面

**选项列表**：

1. **大模型 API 设置**
   - 启用/禁用开关
   - Base URL 输入框（或预设选择）
   - API Key 输入框
   - 测试连接按钮

2. **界面主题**
   - 亮色/暗色切换

3. **数据管理**
   - 清除所有感言按钮（红色，需二次确认）

4. **关于**
   - 显示应用版本号

---

## 技术实现要点

### 数据存储
- **数据库**：使用 Room 或 SQLite 存储感言元数据
- **文件存储**：音频文件存储在应用私有目录

### 权限需求
- `RECORD_AUDIO`：录音权限
- `SCHEDULE_EXACT_ALARM`：精确闹钟权限（Android 12+）
- `POST_NOTIFICATIONS`：通知权限（Android 13+）

### 关键库/API
- **录音**：Android MediaRecorder
- **语音识别**：Android SpeechRecognizer
- **闹钟**：AlarmManager
- **网络请求**：Retrofit/OkHttp（调用大模型 API）
- **Markdown 渲染**：Markwon 或类似库

---

## 开发优先级建议

### Phase 1: 核心功能
1. 录音与播放
2. 感言列表展示
3. 基本的增删改查

### Phase 2: 语音转文本
1. 集成 SpeechRecognizer
2. 单条/批量转换
3. 手动编辑功能

### Phase 3: 高级功能
1. 大模型 API 集成
2. 闹钟提醒
3. 颜色标记与筛选

### Phase 4: 体验优化
1. 主题切换
2. 对话功能
3. UI/UX 细节打磨

---

## 代码规范提示

- **语言**：Kotlin（推荐）或 Java
- **架构**：推荐使用 MVVM 或 MVI
- **命名约定**：
  - 数据类：`Thought`, `ThoughtColor`, `AlarmInfo`
  - ViewModel：`ThoughtListViewModel`, `SettingsViewModel`
  - Repository：`ThoughtRepository`
- **错误处理**：网络请求和语音识别需要完善的错误提示

---

## AI 助手开发指南

在为本项目编写代码时，请遵循以下原则：

1. **优先阅读现有代码**：在修改功能前，先用 Read 工具查看相关文件
2. **遵循项目架构**：保持代码风格一致，遵循已有的架构模式
3. **完整实现功能**：每个功能应包含完整的错误处理和用户反馈
4. **注重用户体验**：
   - 按钮状态应准确反映可用性（启用/禁用）
   - 异步操作要显示加载状态
   - 错误信息要清晰友好
5. **安全第一**：
   - API Key 应安全存储（使用 EncryptedSharedPreferences）
   - 权限请求要有明确说明
   - 数据删除操作需二次确认
6. **版本管理**：在完成阶段性开发后先暂存所有更改，然后进行一次提交（提交信息有意义）。不推送到远程仓库。
7. **构建规则**：修改完代码之后不要自己运行命令构建，而是由我手动构建。
8. **文档更新规则**：**每次接收到修改需求时，必须先更新本文档（CLAUDE.md）**，将需求变更记录到"需求变更记录"章节。这是强制要求。
9. **禁止创建文档**：**绝对不要创建 README、指南、配置文档等任何文档文件。所有说明和指导信息必须直接在对话中告知用户。**

---

## 需求变更记录

### 2026-01-28 - UI/UX 重大改进

#### 1. 播放器改进
- **问题修复**：播放暂停后图标未从暂停符号变回播放符号
- **新增功能**：
  - 为每条录音添加声波图像可视化（类似音频处理软件）
  - 在声波图像上显示播放进度竖线
  - 显示录音长度信息

#### 2. 时间选择器重新设计
- **旧设计**：简单的文本输入框
- **新设计**：
  - 年月选择：点击弹出浮动框切换年份和月份
  - 日期选择：半圆形轮盘式滑动调整，流畅动画
  - 时间选择：半圆形轮盘式滑动调整（小时和分钟）
  - 需要正确计算大月小月、平年闰年

#### 3. 颜色标记 UI 优化
- **调整内容**：
  - 缩小颜色圆形半径（原来太大）
  - 将颜色圆形移至播放按钮左侧（提升美观度）

#### 4. 选择逻辑改进
- **旧设计**：长按进入多选模式
- **新设计**：
  - 在每条感言左侧添加圆角矩形选择框
  - 默认不选中状态
  - 通过点击选择框实现多选
  - 移除长按选择逻辑

#### 5. Bug 修复
- **闹钟设置闪退**：设置闹钟时应用崩溃，需要定位并修复（过期功能正常）
- **闹钟通知**：设置闹钟后未收到系统通知，需要检查权限和通知实现

---

### 2026-01-30 - Whisper 多语言支持（中英文混合识别）

#### 需求背景
用户反馈只能识别中文，需要支持中英文混合音频识别。

#### 技术方案
- **模型切换**：从英文专用模型（`tiny.en`）切换到多语言模型（`tiny`）
- **语言配置**：将 `language` 参数从 `"en"` 改为 `"zh"`，优先识别中文，同时支持英文
- **文件更名**：模型文件名从 `tiny.en-*.onnx` 改为 `tiny-*.onnx`

#### 实现步骤
1. 修改 `WhisperHelper.kt` 配置：
   - 更新文件名常量
   - 更新语言配置为 `"zh"`
   - 更新注释和文档链接
2. 更新 `README_WHISPER.md`：
   - 修改下载链接指向多语言版本
   - 更新文件名说明
   - 更新常见问题解答
3. 创建 `UPGRADE_TO_MULTILINGUAL.md`：
   - 提供从英文版本升级到多语言版本的详细指南
   - 包含测试用例和常见问题

#### 支持能力
- ✅ 纯中文语音识别
- ✅ 纯英文语音识别
- ✅ 中英文混合语音识别
- ✅ 完全离线运行

---

### 2026-01-30 - 自动繁简转换

#### 需求背景
Whisper 多语言模型识别结果可能包含繁体中文，用户需要简体中文输出。

#### 技术方案
- **库选择**：OpenCC4J (com.github.houbb:opencc4j:1.8.1)
- **集成位置**：SpeechToTextHelper.convertThought()
- **转换时机**：Whisper 识别完成后，保存到数据库前

#### 实现细节
1. 添加 OpenCC4J 依赖到 build.gradle.kts
2. 创建 ChineseConverter.kt 工具类：
   - toSimplified(): 繁体转简体
   - containsTraditional(): 检测是否含繁体
   - ensureSimplified(): 智能转换
3. 在 SpeechToTextHelper 中调用 ChineseConverter.toSimplified()
4. 添加日志记录转换前后的文本

#### 效果
- ✅ 自动将识别结果中的繁体字转换为简体
- ✅ 支持混合文本（中英文、繁简体混合）
- ✅ 转换失败时返回原文，不影响功能

---

### 2026-01-30 - UX 细节优化

#### 需求背景
提升用户体验，优化录音和播放的交互细节。

#### 具体需求
1. **自动选择新增/转换的感言**
   - 录音完成后，自动选择新增的感言
   - 转换完成后，自动选择当前感言
   - 取消其他感言的选择（单选模式）

2. **录音时禁用播放**
   - 开始录音时，停止当前播放的音频
   - 录音过程中，所有播放按钮变灰且不可点击
   - 停止录音后，恢复播放按钮状态

#### 实现要点
- ViewModel 中添加自动选择逻辑
- 录音状态与播放状态互斥
- UI 根据录音状态禁用播放按钮

---

### 2026-01-29 - 时间选择器重新设计（垂直滚轮）

#### 需求背景
将时间选择器从半圆形轮盘改为 iOS 风格的垂直滚轮选择器（Drum Roll Picker），提升用户体验和交互自然度。

#### 设计规格

**1. 布局分组**
- **第一组**：年/月/日（大字展示，用 `/` 分割）
  - 三列垂直滚轮：年、月、日
- **第二组**：时:分（大字展示，用 `:` 分割）
  - 两列垂直滚轮：小时、分钟

**2. 滚轮视觉效果**
- **中心行**：
  - 黑色粗体
  - 最大字号
  - 当前选中值
- **上下相邻行**：
  - 中等灰色
  - 字号稍小
- **更远的行**：
  - 浅灰色
  - 字号更小
- **渐变淡出**：字号和透明度从中心向两端递减，形成渐变效果

**3. 交互特性**
- **上下滑动**：弹性动画效果
- **振动反馈**：每次切换选项时触发振动
- **默认值**：当前系统时间
- **循环滚动**：
  - 1月往上滚动 → 12月
  - 12月往下滚动 → 1月
  - 其他数值同理循环

**4. 日期动态计算**
- 根据选中的年份和月份动态计算最大天数
- 当日期超出范围时：
  - 自动调整为该月最大日期
  - 立即刷新控件显示

**5. 技术实现要点**
- 使用 `LazyColumn` 实现垂直滚动
- 使用 `Vibrator` 或 `HapticFeedback` 提供触觉反馈
- 使用 `graphicsLayer` 实现字号和透明度渐变
- 监听滚动状态，自动吸附到最近项

---

### 2026-01-30 - 语音转文本真实实现（Whisper）

#### 需求背景
将占位实现的语音转文本功能替换为真实的 Whisper (tiny) 模型实现，实现完全离线的语音识别能力。

#### 技术方案
- **模型选择**：OpenAI Whisper tiny 模型（~75MB）
- **集成方式**：本地模型，内置在 APK 中（不需要启动后二次下载）
- **技术库**：whisper.cpp 的 Android 绑定
- **优势**：
  - 完全离线运行，符合"随时随地"核心价值
  - 无 API 调用成本
  - 响应速度快，隐私性好
  - 支持多语言识别

#### 实现步骤
1. 集成 whisper.cpp Android 绑定库
2. 下载并内置 Whisper tiny 模型文件到 assets 目录
3. 创建 WhisperHelper.kt 封装推理逻辑
4. 替换 SpeechToTextHelper.kt 的占位实现
5. 添加加载状态和错误处理
6. 更新 ViewModel 调用逻辑

---

### 2026-01-31 - 筛选功能重大改进

#### 需求背景
优化颜色筛选功能的交互体验，修复筛选窗口位置和动画问题，改进"无色"筛选逻辑。

#### 具体改进

**1. 筛选窗口位置调整**
- **旧设计**：筛选窗口对齐到整个顶栏，遮挡筛选按钮
- **新设计**：
  - 筛选窗口上边框对齐到"已选择感言数"那一行的下边框
  - 绝对居右对齐（Alignment.TopEnd）
  - 位置调整为 top: 88.dp（工具栏 + 选择信息栏的高度）
  - 筛选按钮完全露出，不被遮挡

**2. 筛选窗口动画优化**
- **旧设计**：整个窗口从顶部滑下（slideInVertically）
- **新设计**：
  - 使用 expandVertically / shrinkVertically 动画
  - 上边框位置固定不动
  - 下边框向下展开，内容从上到下逐渐显示
  - 动画时长：200ms（原 300ms）

**3. "无色"筛选逻辑重构**
- **旧设计**：
  - "无色"选项等同于"清除所有筛选"
  - isSelected = selectedColors.isEmpty()
  - onClick = onClearAll
- **新设计**：
  - "无色"是独立的筛选选项（用 null 值表示）
  - 可以单独选中（只显示无颜色标签的感言）
  - 可以和其他颜色组合多选（显示无颜色 + 某些颜色的感言）
  - 默认不选中状态
  - isSelected = null in selectedColors
  - onClick = { onColorToggle(null) }

#### 技术实现

**类型系统改进**
- `ThoughtListUiState.selectedColors` 类型从 `List<ThoughtColor>` 改为 `List<ThoughtColor?>`
- `ColorFilterDropdown` 参数类型支持 nullable
- `ThoughtListViewModel.setColorFilter` 参数类型改为 `List<ThoughtColor?>`
- `filterByColors` 方法支持 null 值筛选

**UI 层改进**
- RecorderScreen.kt: 修改筛选窗口位置、动画、层级
- ColorFilterDropdown: 修改 NoColorFilterCircle 的逻辑
- 背景遮罩层放在筛选窗口下层，确保窗口在最上方

#### 影响文件
- RecorderScreen.kt: 筛选窗口布局和动画
- ThoughtListViewModel.kt: selectedColors 类型定义和筛选逻辑
- ColorFilterDropdown 组件逻辑

---

### 2026-01-31 - UI/UX 细节优化（第二批）

#### 需求背景
进一步提升用户体验，优化交互细节和视觉反馈。

#### 具体优化

**1. 筛选框展开速度优化**
- 从 300ms 加快到 200ms
- 提升响应速度，减少等待感

**2. 全选按钮选择框尺寸调整**
- **旧设计**：20.dp，与字体不协调
- **新设计**：16.dp，与"全选"文字大小相对应
- 圆角半径相应调整（8.dp → 4.dp）
- 内部勾选标记从 13.dp 缩小到 10.dp

**3. 播放触发逻辑优化**
- **旧设计**：点击感言卡片任何地方都触发播放
- **新设计**：只有点击播放按钮才触发播放
- 点击卡片其他地方不触发任何操作
- 提升交互准确性，避免误触

**4. 自动定位展开优化**
- **问题**：录音完成/转换完成后自动定位，如果目标区域被折叠会导致定位失败
- **解决方案**：
  - 检测目标感言所在的区域（已转换/原始/已过期）
  - 如果该区域是折叠状态，先展开（保留展开动画）
  - 等待 250ms 让动画完成
  - 然后滚动定位到目标感言
- **实现位置**：ThoughtList.kt 的 LaunchedEffect(scrollToThoughtId)

#### 影响文件
- RecorderScreen.kt: 筛选框动画速度、播放触发逻辑
- ThoughtList.kt: 全选按钮尺寸、自动展开逻辑

#### 技术要点
- 使用 LaunchedEffect 监听 scrollToThoughtId 变化
- 通过修改 collapsed 状态触发展开动画
- kotlinx.coroutines.delay 等待动画完成
- 确保折叠/展开状态正确同步

---

### 2026-02-01 - 振动反馈与布局优化

#### 需求背景
修复振动反馈和布局问题，提升交互体验的流畅度和准确性。

#### 具体修复

**1. 时间选择器振动优化**
- **问题**：年月滚动时会触发两次振动（年/月本身振动 + 日联动振动）
- **解决方案**：
  - 为年、月 DrumRollPicker 添加 `suppressVibration = true` 参数
  - 移除年月的振动反馈，仅保留日期变化时的振动
  - 日期自动调整时（如从 31 日调整到 30 日）也抑制振动，避免重复反馈
- **实现位置**：WheelTimePickerDialog.kt

**2. 工具栏按钮双重振动修复**
- **问题**：点击提醒、颜色等按钮时会振动两次（手动振动 + Material3 默认振动）
- **解决方案**：
  - 为 ToolbarButton 和 DeleteButton 添加自定义 `interactionSource`
  - 禁用 Material3 的默认触摸反馈效果
  - 保留手动调用的 `HapticFeedback.KEYBOARD_TAP` 振动
- **实现位置**：ThoughtToolbar.kt

**3. 工具栏按钮尺寸恢复**
- **问题**：菜单栏按钮被压缩得太小，影响可用性
- **解决方案**：
  - 按钮高度：24.dp → 32.dp
  - 图标尺寸：14.dp → 18.dp
  - 内边距：horizontal 6.dp → 8.dp, vertical 0.dp → 4.dp
  - 文字样式：labelSmall → labelMedium
  - 图标间距：3.dp → 4.dp
- **影响组件**：ToolbarButton、DeleteButton、"取消选中"按钮

**4. 进度条浮动布局修复**
- **问题**：底部加载进度条占用空间，导致录音按钮上移
- **旧方案**：
  - 进度条在 Scaffold 的 bottomBar 槽位
  - 会影响 paddingValues，推动内容上移
- **新方案**：
  - 将 Scaffold 包裹在外层 Box 中
  - 进度条作为独立的浮动元素，直接对齐到外层 Box 的底部
  - 完全脱离 Scaffold 的布局系统，不影响任何内容的位置
- **实现位置**：RecorderScreen.kt

#### 技术实现

**振动抑制机制**
```kotlin
// DrumRollPicker 支持 suppressVibration 参数
fun DrumRollPicker(
    suppressVibration: Boolean = false
) {
    if (actualValue != lastNotifiedValue) {
        onItemSelected(actualValue)
        lastNotifiedValue = actualValue
        if (!suppressVibration) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
```

**自定义 InteractionSource**
```kotlin
TextButton(
    interactionSource = remember { MutableInteractionSource() }
) { ... }
```

**真正的浮动布局**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(...) { ... }

    // 浮动进度条（不影响布局）
    if (uiState.isLoading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}
```

#### 影响文件
- WheelTimePickerDialog.kt: suppressVibration 参数
- ThoughtToolbar.kt: remember import, 按钮尺寸, interactionSource
- RecorderScreen.kt: 外层 Box 包裹, 浮动进度条

#### 效果
- ✅ 时间选择器滚动时只触发一次振动
- ✅ 工具栏按钮点击时只触发一次振动
- ✅ 按钮尺寸恢复到合理大小，易于点击
- ✅ 进度条完全浮动，录音按钮位置固定不变

---

### 2026-02-01 - 闹钟功能修复与即时通知

#### 需求背景
用户反馈设置提醒后没有收到闹钟声音和通知，需要修复闹钟功能并添加设置成功的即时反馈。

#### 问题分析
1. **闹钟不响的原因**：
   - 通知渠道未正确配置铃声
   - 通知优先级不够高
   - 缺少明确的系统闹钟铃声设置
   - AudioAttributes 未设置为 USAGE_ALARM

2. **缺少即时反馈**：
   - 用户设置闹钟后没有立即的确认反馈
   - 无法确认闹钟是否设置成功

#### 解决方案

**1. 修复 AlarmReceiver.kt**
- **添加系统闹钟铃声**：
  ```kotlin
  val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
  ```
- **配置通知渠道的音频属性**：
  ```kotlin
  val audioAttributes = AudioAttributes.Builder()
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .setUsage(AudioAttributes.USAGE_ALARM)
      .build()
  setSound(alarmSound, audioAttributes)
  ```
- **提高通知优先级**：
  - 渠道重要性：IMPORTANCE_HIGH
  - 通知优先级：PRIORITY_MAX
  - 设置为 ongoing 防止误删
  - 添加 VISIBILITY_PUBLIC 在锁屏显示
- **增强振动模式**：
  - 振动模式：`[0, 500, 200, 500, 200, 500]`
  - 持续时间更长，更容易唤醒用户

**2. 创建 NotificationHelper.kt**
- **功能**：发送闹钟设置成功的即时确认通知
- **通知内容**：
  - 标题："提醒设置成功"
  - 内容："你已为「[感言标题]」设置 [年月日 时:分] 的提醒"
  - 标题长度超过 20 字时自动截断并添加省略号
  - 标题为空时显示"此感言"
- **通知样式**：
  - 使用 BigTextStyle 支持长文本显示
  - 优先级：DEFAULT（不打扰用户，但确保可见）
  - 自动消失（setAutoCancel）
- **通知渠道**：独立渠道 "thought_alarm_confirmation_channel"，与闹钟通知分离

**3. 集成即时通知**
- 在 `AlarmHelper.scheduleAlarm()` 方法的最后调用：
  ```kotlin
  NotificationHelper.sendAlarmSetNotification(context, thoughtTitle, alarmTime)
  ```
- 设置闹钟的同时立即发送确认通知

#### 技术实现

**新增导入**
```kotlin
// AlarmReceiver.kt
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

// NotificationHelper.kt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
```

**时间格式化**
```kotlin
val timeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
val formattedTime = alarmTime.format(timeFormatter)
```

#### 影响文件
- AlarmReceiver.kt: 添加铃声、提高优先级、增强振动
- NotificationHelper.kt: 新建文件，处理即时通知
- AlarmHelper.kt: 调用即时通知

#### 效果
- ✅ 闹钟到时会播放系统闹钟铃声
- ✅ 闹钟通知优先级提高，更容易被注意到
- ✅ 设置闹钟后立即收到确认通知
- ✅ 确认通知显示完整的设置信息（感言标题 + 时间）
- ✅ 即使设备休眠也能正常响铃和振动

---

### 2026-02-01 - 通知点击自动定位功能

#### 需求背景
用户点击闹钟通知打开应用后，希望能自动定位到对应的感言，而不是需要手动查找。

#### 具体需求
点击通知后，应用应该：
1. 自动选择该感言（单选模式）
2. 清除其他感言的选择状态
3. 配合滚动动画定位到该感言位置

#### 技术实现

**1. MainActivity 处理通知 Intent**
- 在 `onCreate()` 中调用 `handleNotificationIntent(intent)`
- 重写 `onNewIntent()` 处理应用已启动时的通知点击
- 从 Intent 中提取 `thought_id` 并传递给 ViewModel

**2. ViewModel 添加选择并滚动方法**
```kotlin
fun selectAndScrollToThought(thoughtId: String) {
    _uiState.update { state ->
        state.copy(
            selectedThoughts = setOf(thoughtId),
            isMultiSelectMode = true,
            scrollToThoughtId = thoughtId
        )
    }
}
```

**3. 利用现有滚动机制**
- 复用现有的 `scrollToThoughtId` 和 `clearScrollRequest()` 机制
- 自动触发展开折叠区域的逻辑（已在 ThoughtList.kt 中实现）

#### 影响文件
- MainActivity.kt: 添加 `handleNotificationIntent()` 和 `onNewIntent()`
- ThoughtListViewModel.kt: 添加 `selectAndScrollToThought()` 方法

#### 效果
- ✅ 点击通知后自动选择对应感言
- ✅ 清除其他感言的选择状态（只选择一条）
- ✅ 配合滚动动画定位到感言位置
- ✅ 自动展开折叠区域（如果感言在折叠区域中）
- ✅ 支持应用未启动和已启动两种情况

---

### 2026-02-02 - 全屏闹钟界面

#### 需求背景
当闹钟提醒触发时，需要一个全屏界面显示提醒信息，确保用户不会错过重要的感言提醒。

#### 具体需求

**全屏闹钟界面特性**：
1. **显示于最上层**：覆盖所有应用，即使锁屏状态也能显示
2. **显示内容**：
   - 感言标题（已转换感言）或"感言提醒"（原始感言）
   - 感言内容（如果有）
   - 提醒时间
3. **音频播放**：自动播放该感言的录音文件
4. **交互要求**：
   - "关闭"按钮：停止播放，关闭界面
   - "查看详情"按钮：跳转到主界面并定位到该感言
   - 必须通过按钮才能退出，禁止返回键和外部点击关闭

#### 技术实现

**1. AlarmActivity - 全屏闹钟界面**
- 继承自 ComponentActivity
- 使用 Jetpack Compose 实现 UI
- 设置 WindowManager 标志：
  - `FLAG_SHOW_WHEN_LOCKED`: 锁屏显示
  - `FLAG_DISMISS_KEYGUARD`: 解锁显示
  - `FLAG_KEEP_SCREEN_ON`: 保持屏幕常亮
  - `FLAG_TURN_SCREEN_ON`: 自动点亮屏幕

**2. UI 设计**
- 全屏半透明背景（防止误操作）
- 中心卡片式布局
- 显示感言标题、内容
- 播放进度条和波形图像（复用现有组件）
- 底部两个操作按钮：
  - "关闭"（红色）：停止播放并关闭
  - "查看详情"（蓝色）：跳转到主界面定位

**3. AlarmReceiver 修改**
- 使用 Full Screen Intent 启动 AlarmActivity
- 传递感言 ID 和其他必要信息

**4. 权限配置**
- AndroidManifest.xml 中添加：
  - `USE_FULL_SCREEN_INTENT` 权限
  - AlarmActivity 声明

#### 影响文件
- 新建 AlarmActivity.kt: 全屏闹钟界面
- AlarmReceiver.kt: 修改为启动 AlarmActivity
- AndroidManifest.xml: 添加 Activity 声明和权限

#### 技术要点
- 使用 `setFullScreenIntent()` 实现锁屏唤醒
- 自动播放音频，播放完成后循环或停止
- 处理设备旋转和状态保存
- 确保音频播放器正确释放资源
- 防止意外关闭（禁用返回键和外部点击）

#### 预期效果
- ✅ 闹钟触发时自动唤醒屏幕并全屏显示
- ✅ 锁屏状态也能正常显示和播放
- ✅ 用户必须主动操作才能关闭
- ✅ 可以快速跳转到主界面查看详情
- ✅ 音频自动播放，提供更强的提醒效果
