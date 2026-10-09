# zenless-ui

Compose Multiplatform UI 库，首版 `0.1.0`。提供完整深色主题、固定语义颜色、按压与选中动效，以及中英双语组件 Gallery。

- [在线 Gallery](https://tlaster.github.io/zenless-ui/)
- [各平台构建和验证记录](https://github.com/Tlaster/zenless-ui/actions/workflows/verify.yml)
- [英文使用与构建说明](../README.md)
- [Maven Central 发布说明](RELEASING.md)

支持 Android、Windows x64、Linux x64、macOS Apple Silicon、iOS ARM64 与现代浏览器。Android 最低 API 31，以支持实时背景模糊；不包含 Intel Mac、Windows/Linux ARM64 和旧浏览器。库坐标为 `moe.tlaster.zenlessui:zenless-ui:0.1.0`，首次 Central 发布需要仓库所有者配置命名空间与发布密钥；配置完成不等于已经上架。

组件涵盖按钮、返回/关闭、文件夹页签、导航、输入框、下拉选择、滑条、Checkbox、RadioButton、Switch、卡片、徽章、进度、折叠、Alert、Drawer 和 Tooltip。组件状态由调用方管理。使用弹层和 Tooltip 时，需要在应用根部包裹 `ZenlessOverlayHost`。

不提供自定义颜色或主题替换接口，不做浅色主题、关闭动画选项、键盘导航和手柄支持。文字输入、输入法和剪贴板使用 Compose 的默认实现。不附带图标库、字体、音效或游戏业务组件；业务图标由内容插槽传入，控件固有图形在内部绘制。

长按时，按钮文字、返回箭头和独立图标使用黑色；左侧黑色圆底内的图标与按钮底板同步保持黄绿呼吸色，Alert 操作图标也遵循此规则。业务图标通过 `ZenlessButton.leadingIcon` 传入，在插槽内读取只读的 `zenlessContentColor` 并用于绘制或 tint。普通内容插槽中的图标也可读取此颜色；它不提供主题替换能力。Gallery 的按钮页可切换「左侧图标」并复制对应示例。

Gallery 中可以分类浏览、操作示例、修改参数并复制对应 Kotlin 代码。窄屏采用分类列表与详情两级布局，宽屏采用侧栏。使用平台默认字体，Linux 系统需要安装支持中文的系统字体。

所有通用按钮变体与独立图标按钮共用灰色边框和文字状态：启用时白色、禁用时 `#565657`、按住时黑色。带左侧图标的 Filled 按钮保留深色底板，`tone` 选择圆形图标底座的预设颜色；禁用时底座保留变暗的原色，外壳与棋盘纹理保持不变。Alert 操作按钮直接复用带 `leadingIcon` 的 `ZenlessButton`。

快速调整 UI 时，推送只运行桌面组件/Gallery 测试和 Web 构建，并自动更新在线 Gallery。完整的各平台打包、Android/iOS 模拟器检查改为手动运行 **Build and verify**，发布版本前再执行。

MIT 开源；上游版权声明见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。
