object Version {

    private const val KUIKLY_VERSION = "2.30.0"
    private const val KUIKLY_OHOS_VERSION = "2.15.0"
    private const val KOTLIN_VERSION = "2.1.21"
    private const val KOTLIN_OHOS_VERSION = "2.0.21-ohos"

    /**
     * Kuikly 主线版本，规则：${shortVersion}-${kotlinVersion}。
     * 与已发布的 KuiklyUI-A2UI `1.0.0-2.1.21` 对齐，坐标组是 `com.tencent.kuikly-open`。
     */
    fun getKuiklyVersion(): String {
        return "$KUIKLY_VERSION-$KOTLIN_VERSION"
    }

    /**
     * 鸿蒙仍走内部 `com.tencent.kuikly` 坐标，不跟随主线 open 版本。
     */
    fun getKuiklyOhosVersion(): String {
        return "$KUIKLY_OHOS_VERSION-$KOTLIN_OHOS_VERSION"
    }
}

object BuildPlugin {
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}

object A2ui {
    const val VERSION = "1.0.0-2.1.21"

    fun core(): String = "com.tencent.kuikly-a2ui-open:a2ui:$VERSION"

    fun renderAndroid(): String = "com.tencent.kuikly-a2ui-open:a2ui-render-android:$VERSION"
}
