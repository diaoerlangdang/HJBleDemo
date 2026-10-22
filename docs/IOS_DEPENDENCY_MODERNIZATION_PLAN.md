# iOS 连接管理与第三方依赖整理计划

制定日期：2026-10-21

最近核对：2026-10-22；App 基线提交 `29c2324`，版本 `4.4.1`。

状态：App 断电重连修复已实机通过；wiseBle 库端改造已完成，1.4.0 代码与 tag 已推送 GitHub，公开 CocoaPods 发布此前遇到服务端错误，本次未重新查询发布状态；App 已通过 GitHub tag 接入 1.4.0 并完成同步接口迁移，用户已于 2026-10-22 确认 App 实机测试无问题；本轮六批代码已完成，保留 Masonry；依赖、存储、输入和布局变更的自动验证与待实机验收项见第 7 节。

## 1. 目标和范围

- 将通用的连接状态管理能力收进维护者自己的 wiseBle 库，减少 App 对连接内部状态的管理。
- 保留 wiseBle 同步发送、同步发送并接收的核心设计，App 在后台按业务步骤顺序调用，库统一处理等待并返回操作结果。
- 分批移除无用依赖，将适合系统实现的功能迁回 UIKit / Foundation。
- 保留现有扫描、连接、配置初始化、收发、吞吐测试、设置及自定义指令功能。
- 升级过程中保留已有设置和自定义指令数据。
- 继续使用 Objective-C + UIKit，按需调整现有代码，不同时重写整个项目。

本计划针对 iOS 工程。Android、Web BLE、原生 macOS target 不在本次修改范围内。既有 Mac Catalyst 能力在涉及相关代码时检查兼容性，不将本次工作扩大为完整桌面适配。

## 2. 源码位置与依赖管理

| 项目 | 本机路径 | 主要修改位置 |
| --- | --- | --- |
| HJBleDemo | `/Users/wise/Desktop/github/HJBleDemo` | `iOS/HJBleDemo/` |
| wiseBle 库 | `/Users/wise/Desktop/github/WiseBle` | `WiseBle/`、`wiseBle.podspec`、库的测试及文档 |

wiseBle 当前 podspec 版本为 `1.4.0`、最低 iOS 版本为 `12.0`。Demo 和测试 target 已通过 CocoaPods 直接引用 GitHub 的 `1.4.0` tag，避免依赖 Trunk 发布状态。本轮继续使用该 tag，其他依赖按下方六批计划整理。

实施规则：

1. wiseBle 的正式修改写入库源码仓库，不仅修改 Demo 的 `Pods/wiseBle`。
2. 联调阶段可使用指向本地仓库的开发 Pod；App 和测试 target 应使用同一份库源码。
3. 联调前比较库源码与 Demo 当前 Pods 中的实现，确认已有修复不会因切换依赖而丢失。
4. 交付时记录两个仓库对应的提交及库版本；开发机的绝对路径不作为其他开发者或 CI 的最终安装要求。
5. 库版本号、tag、远程发布与 Demo 的正式引用需要配套安排，避免引用尚不存在的版本。本计划本身不执行发布。

## 3. wiseBle 与 App 的职责

| 能力 | 归属 |
| --- | --- |
| CoreBluetooth 扫描、连接、发现服务、订阅、读写及分包 | wiseBle |
| 连接状态、超时、有限重试、取消及重复断开的处理 | wiseBle |
| 设备与连接会话隔离、旧回调隔离、连接结果只结算一次 | wiseBle |
| 同步操作的等待、超时、取消及断开唤醒 | wiseBle |
| 按顺序发送指令、校验业务响应及决定下一步 | App，通过库的同步接口执行 |
| 页面跳转、连接中的加载提示、中文错误提示 | App |
| 页面退出时决定是否取消连接或断开 | App 作出决定，通过库接口执行 |
| 板子特有的初始化协议、流控查询及业务状态 | App |
| 用户设置、自定义指令、收发日志和统计展示 | App |

迁移目标是统一状态管理，不是把 `HJBleConnectionCoordinator` 原样复制成一个新的公共类。库内部可以保留必要的连接状态实现，但调用者应通过少量清晰的接口操作，不需要自己维护重试、清理和代理切换的时序。

连接管理只保留一套有效实现；App 完成切换后再删除重复逻辑。维持现有 wiseBle 接口的兼容性，明确新旧接口共用同一状态管理时的规则，不让两套流程对同一设备并行操作。

优先增强现有 `WWBluetoothLE`，不预先要求 App 管理新的公开连接对象。同步流程运行在后台线程，不在主线程或库自己的蓝牙回调队列中等待；扫描、主动上报和意外断开仍使用事件回调。

## 4. 分步实施

### 第 1 步：固定断电重连修复基线

现状：App 已修复异常断开后退出页面残留“正在断开”记录的问题，并补充回归测试。修复已提交为 `5ab7503 修复被动断开问题`，用户已确认实机验证通过；此前 20 项逻辑测试及 iOS 真机 SDK 构建通过。

基线提交中的涉及文件（协调器及专属测试已在第 2 步迁移后删除）：

- `Home/model/HJBleConnectionCoordinator.m`
- `Home/controller/BleViewController.m`
- `HJBleDemoTests/HJBleConnectionCoordinatorTests.m`

执行内容：

- 独立保留本次修复及测试，作为后续库迁移前的行为基线。
- 记录异常断开、正常返回、取消、超时和旧页面清理的预期行为。

验收：连接设备 → 板子断电 → 手动返回扫描页面 → 板子上电 → 重新扫描并连接，连续操作多轮，连接和收发正常，无需杀掉 App。正常返回、侧滑返回及进入设置后返回也应正常。

### 第 2 步：将通用连接管理收进 wiseBle

详细设计见 [wiseBle 同步接口与连接管理更新方案](WISEBLE_API_EVOLUTION_PLAN.md)，按连接管理、同步发送与等待管理、同步请求响应三批实施。

执行顺序：

1. 对比库仓库与 Demo 当前使用的实现，明确连接状态的唯一管理位置。
2. 确定最小连接、取消及断开接口，保留同步连接方式，明确调用线程、操作归属、重试范围和清理完成条件。
3. 处理已断开的设备、重复取消、重复断开、超时后的迟到回调，避免永久阻止后续连接。
4. 完善库内同步发送，统一操作等待、超时和断开唤醒；保留多设备能力及旧同步 / 异步接口约定。
5. 将通用回归测试迁入库中；Demo 保留业务初始化和页面生命周期相关测试。
6. Demo 使用本地开发 Pod 分批联调，保留后台同步初始化和发送循环；等价验证后移除协调器及库已承担的状态和等待代码，保留必要的页面及连接归属判断。
7. 完善同步请求响应的显式特征参数及调用级错误；响应匹配和拼包扩展按实际协议需求再确定。
8. 更新库的同步用法说明、版本安排及 Demo 依赖记录。

实际完成情况：库端为 `1c7c46c` / tag `1.4.0`，App 迁移和版本号为提交 `29c2324` / `4.4.1`，用户已确认测试无问题。实际联调直接使用已推送的 GitHub tag，没有采用原计划中的本地开发 Pod；App 和测试 target 使用同一版本，其他依赖保持原样。

验收：

- 正常连接和主动断开成功。
- 断电、返回、上电后可以再次连接。
- 连接中取消后不会稍后误报成功、进入发送页面。
- 重试有上限，前一次连接清理完成后再开始下一次。
- 重复断开不会留下等待记录或重复结算结果。
- 旧页面清理、旧设备回调、旧尝试回调不影响新连接。
- 同步业务可以在一个后台任务中按步骤执行，失败后不继续下一步，断开或取消及时结束相关等待。
- 有响应写入等待所有分包确认，无响应写入等待所有分包交给系统；设备业务执行成功仍通过响应判断。
- 库原有相关回归检查及 Demo 的构建、初始化和收发检查通过。

### 第 3 步：核对并移除无用依赖

已核实并移除 SDWebImage、MBProgressHUD、MMPopupView，以及未使用的 WWAlertView 本地源码和工程引用。

先检查直接调用、基类调用及间接依赖，再调整 Podfile、锁文件和工程引用。每批移除后完成构建及页面功能检查。

验收：扫描、连接加载、设置输入、自定义指令及提示功能正常；依赖安装后可重现相同构建结果。

### 第 4 步：替换 WiseBaseKit，并清理间接依赖

已将 AppDelegate 的 `WWNavigationController` 替换为原生导航，并将 5 个页面（`ScanViewController`、`BleViewController`、`SetViewController`、`AppConfigSetViewController`、`BleServiceConfigViewController`）迁至本地 UIKit 表格 / 滚动容器；工具调用已替换。WiseBaseKit 及其间接依赖 MJRefresh 已移除。

执行内容：

- 逐页使用 UIKit 控制器、导航和滚动视图承担当前实际使用的功能。
- 替换颜色、字体、字符串校验、图片和视图尺寸等工具调用，只保留业务所需实现。
- 保留返回按钮、侧滑返回、取消侧滑和页面退出清理的正确行为。
- 明确布局依赖的导入，避免依靠 WiseBaseKit 间接导入 Masonry。
- 页面迁移完成后移除 WiseBaseKit，确认是否可以同时移除其间接引入的 MJRefresh。

验收：扫描、收发及设置页面的导航、滚动、中文文案和布局正常；主动返回、侧滑返回、取消侧滑不产生连接残留。

### 第 5 步：替换 YYCache，迁移已有数据

已完成 `config/HJConfigInfo.m` 和 `Home/model/HJCustomCommandStore.m` 的存储迁移，移除 YYCache Pod。旧格式读取器保留在 `Storage/HJLegacyCacheReader`，支持跳过中间版本直接升级。

目标：

- 普通设置使用 `NSUserDefaults`。
- 自定义指令使用 Application Support 目录下的 JSON 文件，采用原子写入，并向调用者返回实际保存结果。
- 保持存储接口稳定，页面不接触文件格式和迁移细节。

执行顺序：

1. 列出旧设置键、默认值、旧缓存位置，以及指令字段、标识和顺序。
2. 实现新存储，补充读取、保存、错误及迁移验证。
3. 使用系统 SQLite + Foundation 只读读取旧 YYCache，确认新数据保存成功后才标记迁移完成，保留直接跨版本升级通道。
4. 新存储已存在的数据不得被旧缓存反复覆盖；失败迁移允许后续重试。
5. 完成旧格式回归测试后移除 YYCache 依赖，保留旧格式读取代码。

不能仅按时间删除迁移能力：用户可能跳过过渡版本直接升级，彻底移除前必须明确此类升级路径的数据保留方案。

验收：旧版升级后设置、指令内容、类型、标识和顺序保留；新安装默认值正确；增删改、重启恢复、重复启动及写入失败处理正常。

### 第 6 步：整理输入栏、键盘、选择弹窗和提示

已使用项目内 `HJMessageInputBar`、`HJFeedback` 和 UIKit action sheet 替换相关实现；DXMessageToolBar、PopoverView 本地代码及 IQKeyboardManager、Toast、SVProgressHUD 依赖已移除。

执行内容：

- 替换旧 DXMessageToolBar，保留实际使用的多行输入、发送及输入高度变化行为。
- 使用 UIKit 安全区域和键盘布局能力处理键盘，覆盖当前最低 iOS 版本。
- 核对 IQKeyboardManager 的默认行为后移除，验证全部可输入页面。
- 用 UIKit 的菜单、选择界面或弹窗替换 PopoverView，正确配置 iPad 弹出位置。
- 将 Toast、SVProgressHUD 的实际需求统一为项目内简单提示实现，覆盖连接加载、取消及失败后的关闭。

验收：iPhone、iPad 的输入区域不被键盘遮挡，长文本与多行输入正常；设置输入、自定义指令编辑、连接加载及消息提示均正常。界面调整不改变数据编码和发送语义。

## 5. 验证与交付规则

- 每一步独立形成可检查、可回退的变更，记录修改范围和验证结果。
- 自动化验证优先覆盖实际失败路径，避免仅测试辅助函数而漏掉库与调用方的配合。
- 不运行模拟器。可在不启动模拟器的条件下运行适用的逻辑测试，并使用 iOS 真机 SDK 构建。
- 蓝牙硬件、页面交互和升级数据由 Pad 实机验收；构建通过不能代替实机结果。
- App 与库分别管理变更，最终能确定 Demo 对应的库源码版本。
- 保留现有不相关的工作区改动，不顺带修改 Android、Web BLE 文档或 Xcode 用户状态。

## 6. 本轮六批实施（2026-10-22）

原第 1、2 步已完成；本轮按以下顺序开发，保留 Masonry。Masonry 是 Auto Layout 的封装，可以正常处理安全区域、横竖屏和窗口变化，无需为适配新设备更换布局库。

| 批次 | 修改范围 | 验证 |
| --- | --- | --- |
| 1 | 移除 SDWebImage、MBProgressHUD、MMPopupView 和未使用的 WWAlertView | 依赖安装、真机 SDK 构建 |
| 2 | UIKit 替换 WiseBaseKit、导航、基类与工具，移除 MJRefresh 间接依赖 | 真机 SDK 构建、导航生命周期回归 |
| 3 | 设置迁至 NSUserDefaults，指令迁至 JSON；系统 SQLite 只读迁移旧 YYCache | 旧归档内联/外置、升级、重复迁移、损坏及写入失败测试 |
| 4 | 原生输入栏、键盘布局、选择菜单、加载和提示；移除旧 UI 库 | 构建，实机检查输入与连接取消 |
| 5 | 安全区域、横竖屏、可变窗口、滚动表单、独立指令遮罩 | 构建，实机检查窄窗口、横屏及键盘 |
| 6 | wiseBle 添加 SPM 分发入口，保持同步调用核心；统一回归并更新文档 | 包结构、SDK 编译、App 逻辑测试及构建 |

### 用户所称 iPhone Duo 的适配范围

2026-10-22 核对 Apple 官方要求页 https://developer.apple.com/news/upcoming-requirements/ ，未查到 iPhone Duo 或“2027 年 4 月后必须支持”的正式要求。页面列出的 SDK 要求包含自 2026-04-28 起使用 Xcode 26 / iOS 26 SDK。后续以 Apple 正式设备、SDK 和审核要求为准。

当前按可变尺寸设备准备：布局依据所在窗口和 safe area，不硬编码新机尺寸；解除 iPhone 仅竖屏、iPad 强制全屏限制；输入区跟随键盘；表单在紧凑高度下可滚动；自定义指令仍为独立全屏半透明页面。最低系统保持现有 App 的 iOS 15.6。不能据此宣称已通过尚未确认机型的适配或审核。

不启动模拟器；真机 SDK 构建和本机逻辑测试由代码侧完成，设备旋转、窗口变化、蓝牙与升级数据由用户实机验收。不自动提交、发布或升级版本。

## 7. 六批实施结果与验收记录（2026-10-22）

本轮源码修改已完成，尚未提交。App 版本保持 `4.4.1`，wiseBle 保持 `1.4.0`；没有创建新 tag、push 或执行 CocoaPods 发布。

| 批次 | 代码结果 | 验证状态 |
| --- | --- | --- |
| 1 | 无用 Pod、WWAlertView 及旧弹窗引用已删除 | 最终依赖安装及构建通过 |
| 2 | UIKit 导航、本地表格 / 滚动容器及必要 UI 辅助替换 WiseBaseKit；MJRefresh 随之移除 | 真机 SDK 构建、原有页面生命周期回归通过；导航手势待实机 |
| 3 | 设置使用 NSUserDefaults，指令使用 JSON 原子写入；保留系统 SQLite 只读迁移 | 9 项存储回归通过；真实安装覆盖升级待实机 |
| 4 | 原生多行输入栏、键盘布局、选择弹窗、中文提示与局部连接加载 | 真机 SDK 构建通过；输入、键盘及取消待实机 |
| 5 | 单窗口 SceneDelegate、安全区域、横屏、iPad 可变窗口、滚动设置 / 编辑页与窄屏列表 | 真机 SDK 构建通过；旋转、分屏和缩放待实机；具体 Duo 机型未验证 |
| 6 | wiseBle 仓库添加 Package.swift 与公共头入口；App 继续使用 Pod Git tag | 库 65 项回归通过；SPM 本机构建与独立 Objective-C / Swift 示例的 iOS SDK 构建通过 |

### 最终依赖与接口

- App 仅保留 Masonry `1.1.0`、wiseBle `1.4.0`。测试 target 使用同一 wiseBle Git tag。
- Masonry 继续负责页面约束，safe area 使用明确的 top / bottom / left / right 属性；键盘和滚动布局使用 UIKit layout guides。
- `UIWindowScene` 创建当前窗口，不从物理屏幕取固定尺寸；仍为单窗口，避免让多窗口争用现有蓝牙单例。iPad 可调整该窗口的尺寸，不声明支持多个独立连接窗口。
- 蓝牙的同步顺序调用、会话隔离、发送队列和协议保持原有实现；本轮没有改成业务回调编排。
- 自定义指令按钮仍在“开始测试”左侧；指令面板仍是独立 `overFullScreen` 页面，半透明背景，面板随窗口宽度变化且限制大屏最大宽度。编辑页在键盘出现时可滚动。
- 手动输入保留 HEX 校验、键盘回车发送、按钮发送和粘贴多行后的高度变化；高度上限后在输入框内滚动。

### 数据迁移细节

- 旧设置：`Caches/bleCache/manifest.sqlite`，读取 `isBleHex`、`isClearInput`、`isAddReturn`、`isScanFilter`、`dataMainService`、`dataSendSubService`、`dataReceiveSubService`、`isRespone`、`dataTotalLen`、`sendDataGap`。新设置写入 `NSUserDefaults` 的 `HJSettings.` 命名空间。
- App 已加入 `PrivacyInfo.xcprivacy`，为仅供本 App 保存 / 读取设置的 NSUserDefaults 声明 `CA92.1` 使用理由。
- 各设置已有新值时优先使用新值；旧文件读取失败不写入默认值作为迁移结果，后续可重试。默认值沿用旧实现（包括清空输入默认为否）；配置模式继续为内存状态。
- 旧指令位于 `Application Support/HJBleDemo/CustomCommands` 的 YYCache，键为 `commands.v1`。新文件为同目录 `commands.v1.json`。新文件存在时不再回退到旧列表，避免已删除指令被重新导入。
- 只读旧 SQLite 的 `filename` / `inline_data`，支持外置 `data/<filename>` 归档。旧缓存文件保留，不删除。迁移成功写入新 JSON 后才使用新结果；损坏、缺文件、保存失败均不以空列表覆盖旧数据。
- ID、类型、创建时间及列表顺序保留。新增、编辑和删除返回真实保存结果；保存失败保留编辑内容并提示，不提前关闭编辑页。
- 9 项回归覆盖设置全部键、新值优先、损坏重试、内联 / 外置指令、CRUD 与重启、损坏 JSON 拒绝覆盖、非法旧列表保护及文件系统失败。

### 自动验证

```bash
cd iOS/HJBleDemo
pod install
./scripts/run-ble-tests.sh
xcodebuild -workspace HJBleDemo.xcworkspace -scheme HJBleDemo \
  -configuration Debug -destination 'generic/platform=iOS' \
  -derivedDataPath /tmp/HJBleModernizationDerivedData \
  CODE_SIGNING_ALLOWED=NO build-for-testing
```

- App 本机 XCTest：20 项、0 失败（原有 11 项 + 存储 9 项）。真机 SDK 的 App 和测试 target 构建通过，未执行 iOS 测试、未启动模拟器。
- wiseBle：`scripts/run-tests.sh`，65 项、0 失败。`swift package dump-package`、`swift build --scratch-path /tmp/WiseBleSPMBuild` 通过。
- 独立临时包 `/tmp/WiseBlePackageCheck` 使用本地 wiseBle，Objective-C 通过 `@import wiseBle` 调用接口，Swift 导入库并调用 Objective-C 示例；`xcodebuild -scheme Consumer -destination 'generic/platform=iOS'` 构建通过，最低 iOS 12。
- 日志：`/tmp/hjble-modernization-tests.log`、`/tmp/hjble-modernization-build.log`、`/tmp/wiseble-modernization-tests.log`、`/tmp/wiseble-spm-ios-build.log`。

### 请用 Pad 实机验收

1. **先直接覆盖安装，保留旧 App 数据**：检查原有设置及指令名称、内容、类型、排序；重启再检查。确认后再测试增删改，避免先卸载导致迁移无法验收。
2. 正常扫描连接，HEX / ASCII、CRLF、自定义指令、有 / 无响应发送、吞吐开始 / 停止正常。
3. 板子断电、返回扫描、上电重连；连接中取消 / 离开再连接；进入设置和指令页面后返回仍可收发；侧滑取消和完成返回符合原行为。
4. 横屏 / 竖屏及调整 iPad 窗口宽度，扫描长设备名、收发长日志、全部设置项可见或可滚动；统计与输入栏不侵入系统栏。
5. 输入栏粘贴长文本、多行、发送、清空及弹出 / 收起键盘；服务 UUID 和指令编辑的输入位置可见，保存 / 取消中文按钮可操作。
6. 指令遮罩覆盖当前 App 窗口，点击背景或关闭退出；窄窗口长指令名、编辑 / 删除按钮可用；iPad 设置选择弹窗锚定按钮。

以上是待用户验证的检查项，不表示已逐项完成。iPhone 尺寸和具体新机需要对应设备 / 正式 SDK 再验收；构建通过不能代替这些结果。
