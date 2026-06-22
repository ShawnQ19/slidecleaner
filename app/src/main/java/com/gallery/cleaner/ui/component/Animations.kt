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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object Motion {

    object Duration {
        const val Instant = 50
        const val Fast = 120
        const val Normal = 250
        const val Slow = 400
        const val Glacial = 600
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
        val Fluid = spring<Float>(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    object Enter {
        fun fadeIn(duration: Int = Duration.Normal): EnterTransition =
            fadeIn(tween(duration))

        fun scaleIn(
            initialScale: Float = 0.85f
        ): EnterTransition = scaleIn(
            initialScale = initialScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        fun slideInFromBottom(): EnterTransition = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun slideInFromTop(): EnterTransition = slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun slideInFromLeft(): EnterTransition = slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun slideInFromRight(): EnterTransition = slideInHorizontally(
            initialOffsetX = { it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun bottomBarEnter(): EnterTransition = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun dialogEnter(): EnterTransition = scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(Duration.Fast))

        fun expandVertically(): EnterTransition = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(tween(Duration.Fast))

        fun staggeredFadeIn(index: Int, delayPerItem: Int = 40): EnterTransition = fadeIn(
            animationSpec = tween(
                durationMillis = Duration.Normal,
                delayMillis = index * delayPerItem
            )
        ) + slideInVertically(
            initialOffsetY = { it / 8 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    object Exit {
        fun fadeOut(duration: Int = Duration.Fast): ExitTransition =
            fadeOut(tween(duration))

        fun scaleOut(
            targetScale: Float = 0.85f,
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

        fun slideOutToLeft(): ExitTransition = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun slideOutToRight(): ExitTransition = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun bottomBarExit(): ExitTransition = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.Normal)
        ) + fadeOut(tween(Duration.Fast))

        fun dialogExit(): ExitTransition = scaleOut(
            targetScale = 0.92f,
            duration = Duration.Fast
        ) + fadeOut(tween(Duration.Instant))

        fun shrinkVertically(): ExitTransition = shrinkVertically(
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
