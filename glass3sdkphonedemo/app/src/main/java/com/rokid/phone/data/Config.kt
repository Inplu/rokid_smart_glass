package com.rokid.phone.data

/**
 * 描述需要同步到眼镜端启动器的应用配置。
 * Describes the app configuration to sync to the glasses launcher.
 * 手机端从 assets 读取配置后，通过 SDK 下发到眼镜端。
 * The phone reads the configuration from assets and sends it to the glasses through the SDK.
 */
data class Config(
    // 当前版本
    // Current version.
    val configVersion: Int = 0,
    // 当前环境
    // Current environment.
    val envType: String = "",
    // 默认显示的配置成隐藏
    // Configure items that are visible by default to be hidden.
    val showToHide: List<String> = emptyList(),
    // 默认隐藏的配置成显示
    // Configure items that are hidden by default to be visible.
    val hideToShow: List<String> = emptyList(),
    // 语音指令
    // Voice commands.
    val voiceAction: List<VoiceAction> = emptyList()
)

data class VoiceAction(
    val text: String = "",
    val pinyin: String = ""
)
