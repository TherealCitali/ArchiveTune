/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.consumeUnhandledPointerInput(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                event.changes.forEach { pointerInputChange ->
                    if (!pointerInputChange.isConsumed) {
                        pointerInputChange.consume()
                    }
                }
            }
        }
    }

/**
 * Reports taps anywhere inside the node without taking them away from whatever is underneath:
 * the gesture is only watched on the initial pass and nothing is consumed, so lyric lines,
 * buttons and sliders keep working exactly as before. A press that moves past the touch slop or
 * gains a second finger is a scroll or a pinch, not a tap, and is ignored.
 */
internal fun Modifier.observeTaps(onTap: () -> Unit): Modifier =
    pointerInput(onTap) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var isTap = true
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size > 1) isTap = false
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > touchSlop) isTap = false
                if (change.changedToUpIgnoreConsumed()) {
                    if (isTap) onTap()
                    break
                }
                if (!change.pressed) break
            }
        }
    }

/**
 * Swallows every touch while [enabled], so controls that have faded out cannot be pressed by
 * accident. Consuming on the initial pass means the children never see an unconsumed press;
 * ancestors watching the same pass (such as [observeTaps]) still do.
 */
internal fun Modifier.blockPointerInput(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    }
