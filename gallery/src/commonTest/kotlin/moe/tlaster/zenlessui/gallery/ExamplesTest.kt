package moe.tlaster.zenlessui.gallery

import kotlin.test.*
import moe.tlaster.zenlessui.*

class ExamplesTest {
    @Test fun pillExampleKeepsSelectionEnabledWidthAndTextInSync() {
        val code = pillExample("A \"pill\"", true, false, true)
        listOf("mutableStateOf(true)", "enabled = false", "Modifier.fillMaxWidth()", "selected = !selected", "ZenlessText(\"A \\\"pill\\\"\")")
            .forEach { assertTrue(it in code, it) }
        assertFalse("fillMaxWidth" in pillExample("Short", false, true, false))
    }
    @Test fun codeEscapesTextIncludingInterpolation() {
        assertEquals("\"a\\\"b\\\\c\\n\\\$value\"", quote("a\"b\\c\n\$value"))
    }
    @Test fun exampleReflectsEveryLiveButtonParameter() {
        val code = buttonExample("继续", ZenlessTone.Danger, ZenlessSize.Comfortable, ZenlessButtonVariant.Hollow, false, true, true, false)
        listOf("ZenlessTone.Danger", "ZenlessSize.Comfortable", "ZenlessButtonVariant.Hollow", "enabled = false", "loading = true", "selected = true", "round = false", "\"继续\"").forEach { assertTrue(it in code, it) }
        assertFalse("leadingIcon" in code)
        val withIcon=buttonExample("继续",ZenlessTone.Neutral,ZenlessSize.Default,ZenlessButtonVariant.Filled,true,false,false,true,leadingIcon=true)
        listOf("leadingIcon = {","val ink = zenlessContentColor","drawCircle(ink)").forEach { assertTrue(it in withIcon,it) }
    }
    @Test fun examplesCarryTheSizeOfTheWholePreview() {
        for(size in ZenlessSize.entries) {
            assertEquals("ZenlessTheme(size = ZenlessSize.${size.name}) {\n    ZenlessTextField(value, onChange)\n}",sizedExample("ZenlessTextField(value, onChange)",size))
        }
    }
    @Test fun feedCardExampleKeepsSlotsAndCallerOwnedParameters() {
        val code = feedCardExample(.75f, false, true, true, false, "Author", "A \"title\"", "Summary", "Featured", "Read")
        listOf("coverAspectRatio = 0.75f", "enabled = false", "mutableStateOf(true)", "cover = {", "avatar = {", "author = {",
            "title = { ZenlessText(\"A \\\"title\\\"\") }", "summary = {", "label = {", "status = null", "ContentScale.Crop").forEach { assertTrue(it in code, it) }
        val noLabel = feedCardExample(1f, true, false, false, true, "A", "T", "S", "L", "R")
        assertTrue("label = null" in noLabel)
        assertTrue("status = { ZenlessText(\"R\", maxLines = 1) }" in noLabel)
    }
}
