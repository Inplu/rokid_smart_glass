package com.rokid.glass.speech

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechServiceNamingContractTest {
    @Test
    fun onlineSpeechScreenUsesSdkBasedName() {
        val home = File("src/main/java/com/rokid/glass/HomeActivity.kt").readText()
        val privateLayout = File("src/main/res/layout/activity_private_speech.xml").readText()

        assertTrue(home.contains("R.string.demo_standalone_asr_tts"))
        assertTrue(privateLayout.contains("@string/demo_standalone_asr_tts"))
        assertTrue(privateLayout.contains("@string/demo_this_standalone_online_asr_tts_app_runs_on"))
        assertFalse(home.contains("Online-Speech ASR/TTS"))
        assertFalse(home.contains("在线ASR/TTS"))
        assertFalse(home.contains("GlassSdk 在线语音"))
    }
}
