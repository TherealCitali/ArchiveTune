/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * The random-walk drift and the footprint maths below are adapted from
 * 4nx3b/ArchiveTune (GPL-3.0, © Rukamori and contributors — github.com/4nx3b/ArchiveTune),
 * which solved the two problems this file exists for: a periodic path turns
 * around at full speed, and a rotating rectangle only covers its inscribed
 * circle. Kept under GPL-3.0 with the original notices intact.
 */

package dev.citali.lunartune.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drift for the heavily blurred artwork behind lyrics, shared by every surface that draws a
 * moving-blur backdrop so they move identically.
 *
 * ### Why a random walk and not a formula
 *
 * The obvious version is a pair of `RepeatMode.Reverse` tweens: the artwork sweeps to one
 * extreme and turns around at full speed, which reads as the colours whipping back. A Lissajous
 * path (two sines per axis off one ever-advancing phase) removes the hard turnaround but not the
 * underlying problem — a closed periodic path still spends half its cycle travelling back the way
 * it came, so the colours are still seen "suddenly travelling in the opposite direction", just
 * more smoothly.
 *
 * This drives the drift as a walk between random waypoints instead:
 *
 *  * each leg carries the artwork from where it currently rests to a new waypoint drawn at
 *    random inside a disc of [WanderRadiusDp],
 *  * each leg also turns the artwork by a random angle — see [rotationDeg],
 *  * the interpolation is a raised cosine, so speed starts and ends at zero — the artwork
 *    *finishes its path*, settles, and only then sets off again, which means no reversal ever
 *    happens while it is moving,
 *  * the next waypoint is at least [MinTurnRadians] away in angle from the leg that just
 *    finished, so the colours visibly head off in a genuinely different direction rather than
 *    retracing the previous path,
 *  * legs are timed off their own length at a constant [WanderSpeedDpPerSecond], so a long leg
 *    doesn't race and a short one doesn't crawl.
 *
 * The walk starts at the centre, so the first frame after the backdrop appears has zero offset
 * and the drift grows out of nothing.
 *
 * ### Why translation alone was not enough
 *
 * Translating the backdrop moves every colour by the *same* vector, so their arrangement is
 * rigid: whatever sits in the top third of the artwork stays in the top third of the screen,
 * free to shuffle by at most [WanderRadiusDp]. That is why some colours "only spread at the top
 * and never reach the bottom" however long you watch. No amount of panning can carry them there;
 * it was never a matter of tuning the path.
 *
 * [rotationDeg] is what breaks the rigidity. Turning the artwork about its centre sweeps a colour
 * from one edge to the opposite one over half a turn. Rotation walks per leg like the offsets do,
 * rather than spinning at a constant rate, which keeps the same "settle, then set off again"
 * character and stops it reading as a turntable.
 *
 * ### Bounds
 *
 * [WanderRadiusDp] is the largest offset this can ever produce, and callers rely on that: the
 * blurred artwork is drawn scaled up so that it still covers the screen at maximum offset. See
 * [blurBackdropFootprint], which is what callers use to size the layer.
 *
 * ### Threading / recomposition
 *
 * [xDp], [yDp] and [rotationDeg] are [FloatState]s meant to be read **only** from draw-phase
 * lambdas (`Modifier.graphicsLayer { }`). Reading them during composition would invalidate the
 * whole subtree on every animation frame. Applying them through `graphicsLayer` rather than
 * `Modifier.offset` is what lets this run on Android 11 and below as well: a canvas transform
 * costs one matrix multiply per frame, where a layout-phase offset re-measures and re-places the
 * backdrop 60 times a second and tears on older devices.
 */
internal class BlurWanderDrift(
    private val random: Random = Random.Default,
) {
    private val xState = mutableFloatStateOf(0f)
    private val yState = mutableFloatStateOf(0f)
    private val rotationState = mutableFloatStateOf(0f)

    /** Horizontal offset in dp, in `-WanderRadiusDp..WanderRadiusDp`. */
    val xDp: FloatState get() = xState

    /** Vertical offset in dp, in `-WanderRadiusDp..WanderRadiusDp`. */
    val yDp: FloatState get() = yState

    /**
     * Rotation of the backdrop about its own centre, in degrees.
     *
     * Unbounded on purpose. It is an angle, so it wraps for free, and letting it accumulate is
     * what lets a colour keep travelling the same way past a half-turn instead of being tugged
     * back toward a nominal zero.
     */
    val rotationDeg: FloatState get() = rotationState

    private var fromX = 0f
    private var fromY = 0f
    private var toX = 0f
    private var toY = 0f
    private var fromRotation = 0f
    private var toRotation = 0f
    private var legAngle = random.nextFloat() * TwoPi
    private var legDurationMs = 0f
    private var legElapsedMs = 0f

    init {
        startNextLeg()
    }

    /**
     * Advances the walk by [deltaMs] of wall time. Legs roll over inside the same call, so a
     * dropped frame is caught up rather than skipped.
     */
    fun advance(deltaMs: Float) {
        if (deltaMs <= 0f) return
        legElapsedMs += deltaMs
        while (legElapsedMs >= legDurationMs) {
            legElapsedMs -= legDurationMs
            startNextLeg()
        }
        // Raised cosine: zero velocity at both ends of the leg.
        val t = legElapsedMs / legDurationMs
        val eased = 0.5f - 0.5f * cos(PI.toFloat() * t)
        xState.floatValue = fromX + (toX - fromX) * eased
        yState.floatValue = fromY + (toY - fromY) * eased
        rotationState.floatValue = fromRotation + (toRotation - fromRotation) * eased
    }

    private fun startNextLeg() {
        fromX = toX
        fromY = toY
        fromRotation = toRotation
        // Turn by at least MinTurnRadians so the new leg is never a retread of the one that ended.
        val turn = MinTurnRadians + random.nextFloat() * (TwoPi - 2f * MinTurnRadians)
        legAngle = (legAngle + turn) % TwoPi
        // Waypoints sit in the outer half of the disc: the backdrop reads as more alive when the
        // colours actually reach the edges.
        val radius = WanderRadiusDp * (MinRadiusFraction + random.nextFloat() * (1f - MinRadiusFraction))
        toX = cos(legAngle) * radius
        // No vertical squash. Shaving the vertical amplitude worked directly against the symptom
        // rotation is here to fix, by making the axis that already struggled to reach the bottom
        // of the screen the shorter of the two.
        toY = sin(legAngle) * radius
        // Rotation direction is drawn per leg rather than taken from the leg's own angle: tying
        // the two together would make the backdrop appear to roll along its path, which reads as
        // a mechanism instead of as drifting colour.
        val rotationSign = if (random.nextBoolean()) 1f else -1f
        val rotationSpan =
            MinLegRotationDegrees + random.nextFloat() * (MaxLegRotationDegrees - MinLegRotationDegrees)
        toRotation = fromRotation + rotationSign * rotationSpan
        val distance = hypot(toX - fromX, toY - fromY)
        legDurationMs =
            (distance / WanderSpeedDpPerSecond * 1000f)
                .coerceIn(MinLegDurationMs, MaxLegDurationMs)
    }

    internal companion object {
        /**
         * Largest offset the walk can ever produce, in dp.
         *
         * Rotation supplies far more travel than translation alone ever did, and measures its
         * covering budget to the container's corner rather than its edge, so trading a little pan
         * for the headroom is the better deal.
         */
        const val WanderRadiusDp = 120f

        /**
         * Average travel speed. Deliberately slow — this sits behind lyrics. Was 26dp/s, which
         * read as slightly busy when you were trying to read.
         */
        private const val WanderSpeedDpPerSecond = 19f

        private const val MinLegDurationMs = 6_000f
        private const val MaxLegDurationMs = 22_000f

        /**
         * Degrees of rotation one leg may add. Against the ~15s median leg that is roughly 2°/s,
         * so a colour crosses the screen — half a turn — in about a minute and a half: ambient,
         * rather than something you notice while reading lyrics.
         */
        private const val MinLegRotationDegrees = 15f
        private const val MaxLegRotationDegrees = 45f

        /** Waypoints are never closer to the centre than this fraction of the radius. */
        private const val MinRadiusFraction = 0.5f

        /** ~72°: enough that a new leg is unmistakably a new direction. */
        private const val MinTurnRadians = 1.25f

        private const val TwoPi = (2.0 * PI).toFloat()
    }
}

/**
 * Footprint the blurred backdrop layer has to occupy so that [BlurWanderDrift]'s rotation can
 * never swing one of the layer's own corners into view.
 *
 * ### Why the layer cannot simply be the container
 *
 * The backdrop is built as `Modifier.graphicsLayer { scale / translate / rotate }.blur(radius)`:
 * the blur is applied to the still, centred artwork and the drift transform is applied to the
 * blurred result. `Modifier.blur`'s default `BlurredEdgeTreatment.Rectangle` clips the layer it
 * creates to the composable's bounds, so the blurred result is an opaque rectangle of **exactly
 * the composable's bounds**.
 *
 * A rectangle rotated by an arbitrary angle only reliably covers its own inscribed circle, whose
 * radius comes from the rectangle's *short* side. With the layer sized to the container that is
 * `scale * min(W, H) / 2` — 432dp on a 360x800 phone at 2.4x — while the point that has to stay
 * covered is the container's furthest corner plus the drift, `hypot(W, H) / 2 + WanderRadiusDp` =
 * 559dp. The shortfall is not theoretical: it is a near-black wedge sweeping through a corner of
 * the backdrop in time with the rotation, and it is there even at zero drift.
 *
 * ### What this returns
 *
 * A footprint whose *shorter* side is long enough that the inscribed circle reaches that corner.
 * Only the short side grows: `max(width, height)` comes out unchanged in every real window shape,
 * and because `ContentScale.Crop` of a square artwork renders it at side `max(w, h)`, the artwork
 * is still rasterised at exactly the same scale and the backdrop looks identical. The extra area
 * exists purely for the rotation to swing into.
 *
 * If the cost ever needs to come down, the lever is resolution rather than footprint: halving the
 * footprint while doubling the caller's scales and halving its blur radius is pixel-identical (the
 * on-screen blur is `radius * scale`, the visible window is `containerSide / scale` of an artwork
 * drawn at `max(footprint)`, and the coverage product `scale * footprint` is unchanged) at a
 * quarter of the pixels.
 *
 * @param restScale the scale the layer sits at while it carries no drift and no rotation (equal to
 *   [driftScale] for surfaces that never ramp).
 * @param driftScale the scale the layer reaches once it carries the full drift and rotation.
 * @param maxDriftDp the largest translation the walk can produce, i.e. [BlurWanderDrift.WanderRadiusDp].
 */
internal fun blurBackdropFootprint(
    width: Dp,
    height: Dp,
    restScale: Float,
    driftScale: Float,
    maxDriftDp: Float = BlurWanderDrift.WanderRadiusDp,
): DpSize {
    val w = width.value
    val h = height.value
    if (w <= 0f || h <= 0f || restScale <= 0f || driftScale <= 0f) return DpSize(width, height)

    val corner = hypot(w, h) / 2f
    // Callers ramp scale, translation and rotation off one progress value, so the requirement
    // along the ramp is `2 * (corner + drift * p) / (restScale + (driftScale - restScale) * p)`.
    // That is a Mobius function of p, so it has no interior extremum and the worst case is an
    // endpoint: either resting (no drift, but the smallest scale) or fully drifting (largest
    // scale, but the corner has moved out by the whole wander radius).
    val requiredAtRest = 2f * corner / restScale
    val requiredAtFullDrift = 2f * (corner + maxDriftDp) / driftScale
    val required = max(requiredAtRest, requiredAtFullDrift) * BlurBackdropCoverSafety

    return DpSize(max(w, required).dp, max(h, required).dp)
}

/**
 * A little headroom on [blurBackdropFootprint]'s result. The derivation is exact, so this only
 * absorbs rounding between the dp maths here and the pixel maths the layer is actually rasterised
 * with — 2% is a couple of dp on a phone.
 */
private const val BlurBackdropCoverSafety = 1.02f

/**
 * Remembers a [BlurWanderDrift] and advances it from the frame clock while [active].
 *
 * The loop is gated because it is the only thing keeping the frame clock busy: an
 * `InfiniteTransition` (what this replaces) keeps asking for a frame every ~16ms for as long as it
 * is composed, even when nothing reads its value. When [active] goes false the walk freezes where
 * it is and resumes from there, so closing and reopening lyrics doesn't restart the path.
 */
@Composable
internal fun rememberBlurWanderDrift(active: Boolean): BlurWanderDrift {
    val drift = remember { BlurWanderDrift() }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos != 0L) {
                    drift.advance((frameTimeNanos - lastFrameNanos) / 1_000_000f)
                }
                lastFrameNanos = frameTimeNanos
            }
        }
    }
    return drift
}
