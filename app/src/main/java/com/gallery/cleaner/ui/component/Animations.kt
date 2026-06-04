package com.gallery.cleaner.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object Motion {

    object Duration {
        const val Instant = 50
        const val Fast = 100
        const val Normal = 200
        const val Slow = 350
        const val Glacial = 500
    }

    object SpringSpec {
        val Bounce = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        val Snappy = spring<Float>(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        )
        val Gentle = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
        val Stiff = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
        val Overshoot = spring<Float>(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    object Enter {
        fun fadeIn(duration: Int = Duration.Normal): EnterTransition =
            fadeIn(tween(duration))

        fun scaleIn(
            initialScale: Float = 0.92f,
            duration: Int = Duration.Normal
        ): EnterTransition = scaleIn(
            initialScale = initialScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        fun slideInFromBottom(): EnterTransition = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(tween(Duration.Fast))

        fun slideInFromTop(): EnterTransition = slideInVertically(
            initialOffsetY = { -it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(tween(Duration.Fast))

        fun bottomBarEnter(): EnterTransition = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(tween(Duration.Fast))

        fun dialogEnter(): EnterTransition = scaleIn(
            initialScale = 0.88f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(Duration.Fast))

        fun expandVertically(): EnterTransition =
            androidx.compose.animation.expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            ) + fadeIn(tween(Duration.Fast))
    }

    object Exit {
        fun fadeOut(duration: Int = Duration.Fast): ExitTransition =
            fadeOut(tween(duration))

        fun scaleOut(
            targetScale: Float = 0.92f,
            duration: Int = Duration.Fast
        ): ExitTransition = scaleOut(
            targetScale = targetScale,
            animationSpec = tween(duration)
        )

        fun slideOutToBottom(): ExitTransition = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun slideOutToTop(): ExitTransition = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun bottomBarExit(): ExitTransition = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun dialogExit(): ExitTransition = scaleOut(
            targetScale = 0.88f,
            duration = Duration.Fast
        ) + fadeOut(tween(Duration.Instant))

        fun shrinkVertically(): ExitTransition =
            androidx.compose.animation.shrinkVertically(
                animationSpec = tween(Duration.Normal)
            ) + fadeOut(tween(Duration.Fast))
    }
}

@Composable
fun AnimatedBottomBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = Motion.Enter.bottomBarEnter(),
        exit = Motion.Exit.bottomBarExit(),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun AnimatedFadeScale(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = Motion.Enter.scaleIn() + Motion.Enter.fadeIn(),
        exit = Motion.Exit.scaleOut() + Motion.Exit.fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}
