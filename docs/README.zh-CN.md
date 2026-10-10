# zenless-ui

Compose Multiplatform UI 库，首版 `0.1.0`。提供完整深色主题、固定语义颜色、按压与选中动效，以及中英双语组件 Gallery。

- [在线 Gallery](https://tlaster.github.io/zenless-ui/)
- [各平台构建和验证记录](https://github.com/Tlaster/zenless-ui/actions/workflows/verify.yml)
- [英文使用与构建说明](../README.md)
- [Maven Central 发布说明](RELEASING.md)

支持 Android、Windows x64、Linux x64、macOS Apple Silicon、iOS ARM64 与现代浏览器。Android 最低 API 31，以支持实时背景模糊；不包含 Intel Mac、Windows/Linux ARM64 和旧浏览器。库坐标为 `moe.tlaster.zenlessui:zenless-ui:0.1.0`，首次 Central 发布需要仓库所有者配置命名空间与发布密钥；配置完成不等于已经上架。

组件涵盖按钮、返回/关闭、文件夹页签、导航、输入框、下拉选择、滑条、Checkbox、RadioButton、Switch、卡片、徽章、进度、折叠、Alert、Drawer 和 Tooltip。组件状态由调用方管理。使用弹层和 Tooltip 时，需要在应用根部包裹 `ZenlessOverlayHost`。

控件共用三档尺寸：`Compact` 为 40 dp／14 sp，`Default` 为 52 dp／20 sp，`Comfortable` 为 62 dp／24 sp。通过 `ZenlessTheme(size = ZenlessSize.Compact) { ... }` 设置整组控件，也可以传入单个组件的 `size`。高度是最小值，换行或放大字体时可以增长。按钮、输入框、下拉框、Switch、Checkbox、RadioButton、Slider、普通页签与导航、信息行和折叠标题共用这些档位；内边距、图标插槽随档位变化，Switch 轨道按比例缩放。返回／关闭、文件夹页签及 Alert 保留独立几何；Alert 操作按钮使用常规字号，最小高度保持 56 dp。

首次 Central 发布前，原有五档按钮尺寸收敛为这三档：原 `Mini`／`Small` 改用 `Compact`，原 `Large`／`Extra` 改用 `Default`。原 `Default` 的 40 dp／14 sp 对应新的 `Compact`。Gallery 的「控件尺寸」会同步调整预览与可复制代码，「视觉基础」中可对照三档效果。业务图标可在有尺寸约束的插槽内使用 `Modifier.fillMaxSize()` 跟随组件大小。

不提供自定义颜色或主题替换接口，不做浅色主题、全局关闭动画选项、自定义键盘导航和手柄支持。文字输入、输入法和剪贴板使用 Compose 的默认实现。不附带图标库、字体、音效或游戏业务组件；业务图标由内容插槽传入，控件固有图形在内部绘制。

`ZenlessFeedCard` 是信息流卡片，提供必填的 `cover`、`avatar`、`author`、`title`、`summary` Composable 插槽，以及带蓝色／深色胶囊背景的可选 `label`、`status`。宽度由父布局决定，`coverAspectRatio` 使用封面宽／高，必须为有限正数，并在图片加载前预留空间。图片填充、居中裁剪、加载／失败内容、头像底色均由调用方提供。`ZenlessText` 继承插槽的默认字体样式，行数和省略由调用方决定，卡片不限制文字行数。可选插槽为 `null` 时不画背景，传入空函数仍会画背景。

信息流卡片整卡只有一个 `onClick`，插槽内放展示内容。`selected` 由外部管理，选中、按住和实际输入焦点共用两秒黄绿呼吸描边；没有缩放、悬停高亮或松开闪烁。禁用时变暗并停止交互、呼吸。这一组件支持 Compose 原生焦点和键盘激活，不增加自定义导航规则或手柄映射。Gallery 的「信息流卡片」展示横图／方图／竖图瀑布流、选中、两个可选胶囊、禁用与长标题，并提供同步更新的 Kotlin 示例。Windows density=2 的原图叠加、差异图及复现步骤见 [像素校准说明](SPEC.md#feed-card-calibration)。

`ZenlessPillContainer` 提供不透明渐变胶囊外壳和一个 `RowScope` 展示插槽。宽度由内容或 `Modifier` 决定，三档尺寸提供最小高度、继承字号和按比例缩放的基础内边距（Default 为横向 18 dp、纵向 6 dp）。较高内容可以撑高外壳，明确固定尺寸时裁剪。省略 `onClick` 时只展示；提供回调时整条只有一个操作目标，支持原生键盘焦点和激活，插槽内不放独立交互控件。按住、焦点和外部 `selected` 共用两秒黄绿颜色呼吸；松开后由选中／焦点决定，不自动切换选中，无缩放、悬停高亮或释放闪烁。禁用保留外壳，停止交互与呼吸。高亮向外延伸 Default 3 dp（随档位缩放），不改变布局尺寸，父容器裁剪时需要预留空间。Gallery「胶囊容器」提供状态、宽度、长内容、只读组合和可复制示例；原图叠加及差异验证见 [胶囊校准](SPEC.md#pill-container-calibration)。

长按时，按钮文字、返回箭头和独立图标使用黑色；左侧黑色圆底内的图标与按钮底板同步保持黄绿呼吸色，Alert 操作图标也遵循此规则。业务图标通过 `ZenlessButton.leadingIcon` 传入，在插槽内读取只读的 `zenlessContentColor` 并用于绘制或 tint。普通内容插槽中的图标也可读取此颜色；它不提供主题替换能力。Gallery 的按钮页可切换「左侧图标」并复制对应示例。

Gallery 中可以分类浏览、操作示例、修改参数并复制对应 Kotlin 代码。窄屏采用分类列表与详情两级布局，宽屏采用侧栏。使用平台默认字体，Linux 系统需要安装支持中文的系统字体。

非文件夹样式的 `ZenlessTabs` 在最左、最右使用半圆外沿，内部连接处保留斜边。切换时，选中滑块用 280ms 缓出曲线移动并变形；连续切换从当前位置转向，呼吸周期保持连续，文字按滑块实际覆盖范围切换黑白色。设置 `expand = false` 可只保留颜色呼吸，停止缩放呼吸，切换时仍会移动和变形。Gallery 的「导航与页签」中关闭「文件夹样式」后，可切换「缩放呼吸」。文件夹样式忽略 `expand`。

所有通用按钮变体与独立图标按钮共用灰色边框和文字状态：启用时白色、禁用时 `#565657`、按住时黑色。带左侧图标的 Filled 按钮保留深色底板，`tone` 选择圆形图标底座的预设颜色；禁用时底座保留变暗的原色，外壳与棋盘纹理保持不变。Alert 操作按钮直接复用带 `leadingIcon` 的 `ZenlessButton`。有无图标都使用相同的正常字重，不强制斜体；带图标时，文字在左侧圆最右侧与右侧半圆圆心之间居中，RTL 下镜像，无图标时在整个按钮中居中。内部半圆环为带柔边的平整深灰色，只有按钮外沿带高光。

快速调整 UI 时，本地只做相关组件的轻量验证，不做完整构建，也不等待 CI 结束。推送和 PR 仍自动运行完整的 **Build and verify**，包括各平台打包及 Android/iOS 模拟器检查。**UI checks and Gallery** 独立运行桌面组件/Gallery 测试与 Web 构建并更新在线预览，不依赖完整平台任务结束。

MIT 开源；上游版权声明见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。
