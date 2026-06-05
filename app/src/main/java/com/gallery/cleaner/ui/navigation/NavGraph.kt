package com.gallery.cleaner.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gallery.cleaner.ui.component.Motion
import com.gallery.cleaner.ui.screen.gallery.GalleryScreen
import com.gallery.cleaner.ui.screen.media.MediaSwipeScreen
import com.gallery.cleaner.ui.screen.preview.PreviewScreen
import com.gallery.cleaner.ui.screen.processed.ProcessedGalleryScreen
import com.gallery.cleaner.ui.screen.processed.ProcessedMonthScreen
import com.gallery.cleaner.ui.screen.random.RandomCleanupScreen
import com.gallery.cleaner.ui.screen.settings.ThemeSettingsScreen
import com.gallery.cleaner.ui.screen.trash.TrashScreen
import com.gallery.cleaner.util.log.AppLogger

private const val TAG = "NavGraph"

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    navController.addOnDestinationChangedListener { _, destination, arguments ->
        AppLogger.userAction(TAG, "页面导航", "route=${destination.route}, args=$arguments")
    }

    val sharedEnterTransition = fadeIn(tween(Motion.Duration.Normal)) +
            scaleIn(
                initialScale = 0.985f,
                animationSpec = tween(Motion.Duration.Normal)
            )

    val sharedExitTransition = fadeOut(tween(Motion.Duration.Fast)) +
            scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(Motion.Duration.Fast)
            )



    val previewEnterTransition = fadeIn(tween(Motion.Duration.Normal)) +
            scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(Motion.Duration.Normal)
            )

    val previewExitTransition = fadeOut(tween(Motion.Duration.Fast)) +
            scaleOut(
                targetScale = 0.97f,
                animationSpec = tween(Motion.Duration.Fast)
            )

    NavHost(
        navController = navController,
        startDestination = Routes.GALLERY,
        enterTransition = { sharedEnterTransition },
        exitTransition = { sharedExitTransition },
        popEnterTransition = { sharedEnterTransition },
        popExitTransition = { sharedExitTransition }
    ) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                onMonthClick = { yearMonth ->
                    AppLogger.userAction(TAG, "点击月份", "yearMonth=$yearMonth")
                    navController.navigate(Routes.mediaSwipe(yearMonth))
                },
                onProcessedClick = {
                    AppLogger.userAction(TAG, "点击已整理入口")
                    navController.navigate(Routes.processedGallery())
                },
                onRandomCleanupClick = {
                    AppLogger.userAction(TAG, "点击随机清理入口")
                    navController.navigate(Routes.randomCleanup())
                },
                onTrashClick = {
                    AppLogger.userAction(TAG, "点击回收站入口")
                    navController.navigate(Routes.TRASH)
                },
                onThemeSettingsClick = {
                    AppLogger.userAction(TAG, "点击主题设置")
                    navController.navigate(Routes.THEME_SETTINGS)
                }
            )
        }
        composable(
            route = Routes.PROCESSED_GALLERY,
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) {
            ProcessedGalleryScreen(
                onBackClick = { navController.popBackStack() },
                onMonthClick = { yearMonth ->
                    AppLogger.userAction(TAG, "点击已整理月份", "yearMonth=$yearMonth")
                    navController.navigate(Routes.processedMonth(yearMonth))
                }
            )
        }
        composable(
            route = Routes.MEDIA_SWIPE,
            arguments = listOf(navArgument("yearMonth") { type = NavType.StringType }),
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) { backStackEntry ->
            val yearMonth = backStackEntry.arguments?.getString("yearMonth") ?: ""
            MediaSwipeScreen(
                yearMonth = yearMonth,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { mediaId ->
                    AppLogger.userAction(TAG, "点击媒体预览", "mediaId=$mediaId")
                    navController.navigate(Routes.preview(mediaId))
                }
            )
        }
        composable(
            route = Routes.PROCESSED_MONTH,
            arguments = listOf(navArgument("yearMonth") { type = NavType.StringType }),
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) { backStackEntry ->
            val yearMonth = backStackEntry.arguments?.getString("yearMonth") ?: ""
            ProcessedMonthScreen(
                yearMonth = yearMonth,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { mediaId ->
                    AppLogger.userAction(TAG, "点击已整理媒体预览", "mediaId=$mediaId")
                    navController.navigate(Routes.preview(mediaId))
                }
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
            enterTransition = { previewEnterTransition },
            exitTransition = { previewExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            PreviewScreen(
                mediaId = mediaId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.RANDOM_CLEANUP,
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) {
            RandomCleanupScreen(
                onBackClick = { navController.popBackStack() },
                onMediaClick = { mediaId ->
                    AppLogger.userAction(TAG, "点击随机媒体预览", "mediaId=$mediaId")
                    navController.navigate(Routes.preview(mediaId))
                }
            )
        }
        composable(
            route = Routes.TRASH,
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) {
            TrashScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.THEME_SETTINGS,
            enterTransition = { sharedEnterTransition },
            exitTransition = { sharedExitTransition },
            popEnterTransition = { sharedEnterTransition },
            popExitTransition = { sharedExitTransition }
        ) {
            ThemeSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
