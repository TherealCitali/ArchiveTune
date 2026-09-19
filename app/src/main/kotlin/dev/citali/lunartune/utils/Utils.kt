/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

fun reportException(throwable: Throwable) {
    // printStackTrace() writes to System.err, which Android redirects to logcat one line at a
    // time as `W/System.err`, synchronising on the stream for every frame. This runs dozens of
    // times a minute during lyrics/stream fallbacks; Log.w batches the trace into one call.
    Log.w("LunarTune", "reportException", throwable)
}

@Suppress("DEPRECATION")
fun setAppLocale(
    context: Context,
    locale: Locale,
) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
