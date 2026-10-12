package com.rokid.glass.speech.privateservice

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSpeechActionContractTest {
    @Test
    fun sourceDemoExposesAllTenActions() {
        assertEquals(
            listOf(
                "INIT",
                "RELEASE",
                "ASR_CONNECT",
                "ASR_START",
                "ASR_STOP",
                "ASR_CLOSE",
                "TTS_CONNECT",
                "TTS_SPEAK",
                "TTS_STOP",
                "TTS_CLOSE",
            ),
            PrivateSpeechAction.entries.map { it.name },
        )
    }

    @Test
    fun asrAndTtsCloseActionsRemainIndependent() {
        val activitySource = File(
            "src/main/java/com/rokid/glass/speech/private/PrivateSpeechActivity.kt"
        ).readText()

        assertTrue(activitySource.contains("PrivateSpeechAction.ASR_CLOSE -> closeAsrClient()"))
        assertTrue(activitySource.contains("PrivateSpeechAction.TTS_CLOSE -> closeTtsClient()"))
    }

    @Test
    fun ttsDemoDescribesIndependentGlassDeployment() {
        val activitySource = File(
            "src/main/java/com/rokid/glass/speech/private/PrivateSpeechActivity.kt"
        ).readText()

        assertTrue(
            activitySource.contains(
                "DEFAULT_TTS_TEXT: String get() = demoString(R.string.demo_this_is_an_online_asr_tts_app_running)"
            )
        )
    }
}
