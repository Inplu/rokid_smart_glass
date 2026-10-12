package com.rokid.glass.speech

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

object OnlineAsrStatusMessages {
    val serviceUnavailable: String get() = demoString(R.string.demo_online_asr_service_unavailable_check_the_glasses_system)
    val connecting: String get() = demoString(R.string.demo_starting_speech_recognition)
    val connected: String get() = demoString(R.string.demo_online_asr_connected_waiting_for_recognition_to_start)
    val started: String get() = demoString(R.string.demo_speech_recognition_started_please_speak)
    val connectionFailed: String get() = demoString(R.string.demo_online_asr_connection_failed_check_connectivity_on_the)
    val connectionTimeout: String get() = demoString(R.string.demo_online_asr_connection_timed_out_check_connectivity_on)

    fun error(code: Int): String =
        demoString(R.string.demo_online_asr_recognition_failed_code_check_the_network, code)
}
