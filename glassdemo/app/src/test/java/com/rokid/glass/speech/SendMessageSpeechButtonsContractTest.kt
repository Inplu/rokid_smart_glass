package com.rokid.glass.speech

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageSpeechButtonsContractTest {
    @Test
    fun messagePageShowsThreeCompactSpeechButtonsInServiceOrder() {
        val layout = File("src/main/res/layout/activity_sendmessage.xml").readText()
        val onlineTts = layout.indexOf("btnOnlineTts")
        val offlineTts = layout.indexOf("btnOfflineTts")
        val onlineAsr = layout.indexOf("btnOnlineAsr")

        assertTrue(onlineTts >= 0)
        assertTrue(onlineTts < offlineTts)
        assertTrue(offlineTts < onlineAsr)
        assertTrue(layout.contains("@string/demo_online_tts_2"))
        assertTrue(layout.contains("@string/demo_offline_tts"))
        assertTrue(layout.contains("@string/demo_online_asr_2"))
        listOf("btnOnlineTts", "btnOfflineTts", "btnOnlineAsr").forEach { buttonId ->
            val button = layout.substringAfter("android:id=\"@+id/$buttonId\"")
                .substringBefore("/>")
            assertTrue(button.contains("android:gravity=\"center\""))
            assertTrue(button.contains("android:singleLine=\"false\""))
            assertTrue(button.contains("android:paddingVertical=\"4dp\""))
            assertTrue(button.contains("android:textSize=\"12sp\""))
        }
        assertFalse(layout.contains("android:id=\"@+id/btTts\""))
        assertFalse(layout.contains("android:id=\"@+id/btAsr\""))
    }

    @Test
    fun eachSpeechButtonDispatchesToItsMatchingGlassSdkService() {
        val source = File(
            "src/main/java/com/rokid/glass/SendMessageActivity.kt"
        ).readText()

        assertTrue(source.contains("R.id.btnOnlineTts -> playOnlineTts()"))
        assertTrue(source.contains("R.id.btnOfflineTts -> playOfflineTts()"))
        assertTrue(source.contains("R.id.btnOnlineAsr -> startAsr()"))
        assertTrue(source.contains("getGlassTtsService()"))
        assertTrue(source.contains("doSpeechTts(ONLINE_TTS_DEMO_TEXT)"))
        assertTrue(source.contains("getGlassOfflineTtsService()"))
        assertTrue(source.contains("playTtsMsg(OFFLINE_TTS_DEMO_TEXT)"))
        assertTrue(
            source.lastIndexOf("binding.btnOnlineTts") < source.lastIndexOf("binding.btnOfflineTts")
        )
        assertTrue(
            source.lastIndexOf("binding.btnOfflineTts") < source.lastIndexOf("binding.btnOnlineAsr")
        )
    }

    @Test
    fun ttsDemoReportsNetworkAuthenticationCompletionAndPlaybackFailures() {
        val source = File(
            "src/main/java/com/rokid/glass/SendMessageActivity.kt"
        ).readText()

        assertTrue(source.contains("R.string.demo_this_is_an_online_text_to_speech_demo"))
        assertTrue(source.contains("R.string.demo_this_is_an_offline_text_to_speech_demo"))
        assertTrue(source.contains("SpeechCompleteListener.Stub()"))
        assertTrue(source.contains("R.string.demo_online_tts_playback_complete_remaining"))
        assertTrue(source.contains("R.string.demo_no_network_available_in_direct_mode_connect_glasses"))
        assertTrue(source.contains("R.string.demo_no_online_tts_completion_callback_cancellation_requested_continuing"))
        assertTrue(source.contains("R.string.demo_online_tts_authentication_or_permission_failed_check_lingmou"))
        assertTrue(source.contains("R.string.demo_offline_tts_request_sent"))
        assertTrue(source.contains("removeSpeechCompleteListener"))
        assertFalse(source.contains("OnlineTtsStabilityRun"))
        assertFalse(source.contains("ONLINE_TTS_REPEAT_COUNT"))
    }

    @Test
    fun onlineTtsDispatchesOneRequestWithoutWaitingForConnectionReplay() {
        val source = File(
            "src/main/java/com/rokid/glass/SendMessageActivity.kt"
        ).readText()

        assertTrue(source.contains("service.doSpeechTts(ONLINE_TTS_DEMO_TEXT)"))
        assertTrue(source.contains("R.string.demo_online_tts_service_connected"))
        assertTrue(source.contains("R.string.demo_online_tts_request_sent"))
        assertFalse(source.contains("onlineTtsWaitingForConnection"))
        assertFalse(source.contains("正在连接在线 TTS 服务，请稍候..."))

    }

    @Test
    fun onlineAsrReportsNetworkConnectionAuthorizationAndTimeoutFailures() {
        val source = File(
            "src/main/java/com/rokid/glass/SendMessageActivity.kt"
        ).readText()

        assertTrue(source.contains("checkSpeechNetwork(demoString(R.string.demo_online_asr))"))
        assertTrue(source.contains("OnlineAsrStatusMessages.serviceUnavailable"))
        assertFalse(source.contains("log(OnlineAsrStatusMessages.connectionFailed)"))
        assertTrue(source.contains("OnlineAsrStatusMessages.connectionTimeout"))
        assertTrue(source.contains("OnlineAsrStatusMessages.error(code)"))
        assertTrue(source.contains("onlineAsrTimeoutJob"))
        assertTrue(source.contains("scheduleOnlineAsrTimeout()"))
        assertTrue(source.contains("cancelOnlineAsrTimeout()"))
        assertTrue(
            source.substringAfter("private fun startAsr()")
                .substringBefore("private fun playOnlineTts()")
                .contains("checkSpeechNetwork(demoString(R.string.demo_online_asr))")
        )
    }
}
