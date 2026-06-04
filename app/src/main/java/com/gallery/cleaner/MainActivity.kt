package com.gallery.cleaner

import android.app.RecoverableSecurityException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gallery.cleaner.ui.navigation.NavGraph
import com.gallery.cleaner.ui.theme.GalleryCleanerTheme
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val REQUEST_CODE_TRASH = 1001
        const val REQUEST_CODE_DELETE = 1002
        const val REQUEST_CODE_RESTORE = 1003
        var pendingTrashUris: List<android.net.Uri> = emptyList()
        var pendingDeleteUris: List<android.net.Uri> = emptyList()
        var onTrashResult: ((Boolean, Int) -> Unit)? = null
        var onDeleteResult: ((Boolean, Int) -> Unit)? = null
        var onRestoreResult: ((Boolean, Int) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.enter(TAG, "onCreate", "savedInstanceState" to (savedInstanceState != null))
        val startTime = System.currentTimeMillis()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppLogger.d(TAG, "设置 Compose UI 内容")
        setContent {
            GalleryCleanerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }

        AppLogger.perf(TAG, "Activity onCreate", startTime)
        AppLogger.exit(TAG, "onCreate")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AppLogger.enter(TAG, "onActivityResult", "requestCode" to requestCode, "resultCode" to resultCode)
        when (requestCode) {
            REQUEST_CODE_TRASH -> {
                val success = resultCode == RESULT_OK
                val count = if (success) pendingTrashUris.size else 0
                AppLogger.i(TAG, "回收站请求结果: success=$success, count=$count")
                onTrashResult?.invoke(success, count)
                pendingTrashUris = emptyList()
                onTrashResult = null
            }
            REQUEST_CODE_DELETE -> {
                val success = resultCode == RESULT_OK
                val count = if (success) pendingDeleteUris.size else 0
                AppLogger.i(TAG, "删除请求结果: success=$success, count=$count")
                onDeleteResult?.invoke(success, count)
                pendingDeleteUris = emptyList()
                onDeleteResult = null
            }
            REQUEST_CODE_RESTORE -> {
                val success = resultCode == RESULT_OK
                val count = if (success) pendingTrashUris.size else 0
                AppLogger.i(TAG, "恢复请求结果: success=$success, count=$count")
                onRestoreResult?.invoke(success, count)
                onRestoreResult = null
            }
        }
        AppLogger.exit(TAG, "onActivityResult")
    }

    override fun onStart() {
        AppLogger.enter(TAG, "onStart")
        super.onStart()
        AppLogger.exit(TAG, "onStart")
    }

    override fun onResume() {
        AppLogger.enter(TAG, "onResume")
        super.onResume()
        AppLogger.exit(TAG, "onResume")
    }

    override fun onPause() {
        AppLogger.enter(TAG, "onPause")
        super.onPause()
        AppLogger.exit(TAG, "onPause")
    }

    override fun onStop() {
        AppLogger.enter(TAG, "onStop")
        super.onStop()
        AppLogger.exit(TAG, "onStop")
    }

    override fun onDestroy() {
        AppLogger.enter(TAG, "onDestroy")
        super.onDestroy()
        AppLogger.flush()
        AppLogger.exit(TAG, "onDestroy")
    }
}
