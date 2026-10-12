package com.rokid.glass.speech

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

/** Only a known direct route with no local Internet network can be rejected locally.
 * Phone connectivity cannot be inferred from the glasses' ConnectivityManager.
 */
internal object SpeechNetworkPolicy {
    fun canAttempt(route: Int?, localInternet: Boolean?): Boolean =
        route != 1 || localInternet != false

    fun hint(route: Int?): String = when (route) {
        0 -> demoString(R.string.demo_using_phone_relay_keep_the_phone_app_connected)
        1 -> demoString(R.string.demo_using_glasses_direct_mode_ensure_the_glasses_can)
        else -> demoString(R.string.demo_unknown_speech_network_route_attempting_the_request_awaiting)
    }
}
