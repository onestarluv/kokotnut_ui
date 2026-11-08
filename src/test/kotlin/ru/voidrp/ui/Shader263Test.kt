package ru.voidrp.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import ru.voidrp.ui.pack.Shaders

class Shader263Test {
    @Test
    fun `26_3 shader interfaces match and preserve OIT rendering`() {
        val motion = Shaders.motion
        val particles = Shaders.particles
        try {
            Shaders.motion = true
            Shaders.particles = true
            val vertex = Shaders.TEXT_VSH_MODERN
            val fragment = Shaders.TEXT_FSH_MODERN
            assertTrue("#moj_import" !in vertex && "#moj_import" !in fragment)
            assertTrue("gl_VertexID" !in vertex)
            assertTrue("gl_VertexIndex" in vertex)
            assertTrue("layout(location = 4) out float voidrpShape;" in vertex)
            assertTrue("layout(location = 4) in float voidrpShape;" in fragment)
            assertTrue("executeAlphaOnlyPhase" in fragment)
            assertTrue("sampleColorForAccumulation" in fragment)
            assertTrue("voidrpShape > 0.5 ? 0.004 : 0.1" in fragment)
            // Inputs for tools/VerifyShaders.java, which uses the real client's compiler.
            File("build/shaders263").mkdirs()
            File("build/shaders263/text.vsh").writeText(vertex)
            File("build/shaders263/text.fsh").writeText(fragment)
        } finally {
            Shaders.motion = motion
            Shaders.particles = particles
        }
    }
}
