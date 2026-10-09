package moe.tlaster.zenlessui.gallery

import kotlin.test.*
import moe.tlaster.zenlessui.*

class ExamplesTest {
    @Test fun codeEscapesTextIncludingInterpolation() {
        assertEquals("\"a\\\"b\\\\c\\n\\\$value\"", quote("a\"b\\c\n\$value"))
    }
    @Test fun exampleReflectsEveryLiveButtonParameter() {
        val code = buttonExample("继续", ZenlessTone.Danger, ZenlessSize.Extra, ZenlessButtonVariant.Hollow, false, true, true, false)
        listOf("ZenlessTone.Danger", "ZenlessSize.Extra", "ZenlessButtonVariant.Hollow", "enabled = false", "loading = true", "selected = true", "round = false", "\"继续\"").forEach { assertTrue(it in code, it) }
        assertFalse("leadingIcon" in code)
        val withIcon=buttonExample("继续",ZenlessTone.Neutral,ZenlessSize.Default,ZenlessButtonVariant.Filled,true,false,false,true,leadingIcon=true)
        listOf("leadingIcon = {","val ink = zenlessContentColor","drawCircle(ink)").forEach { assertTrue(it in withIcon,it) }
    }
}
