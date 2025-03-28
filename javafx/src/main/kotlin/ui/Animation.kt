package ui

import javafx.animation.Interpolator
import javafx.util.Duration

val FluentInterpolator: Interpolator = Interpolator.SPLINE(0.05, 0.965, 0.005, 0.955)
val EASING: Interpolator = Interpolator.SPLINE(.02, .98, .46, .95)
val ANIM_DURATION: Duration = Duration.millis(800.0)
