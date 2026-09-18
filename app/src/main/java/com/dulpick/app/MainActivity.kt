package com.dulpick.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dulpick.app.feature.root.DulpickRoot
import com.dulpick.app.ui.theme.DulpickTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // 공유로 들어온 인스타 URL. DulpickRoot 가 읽어 장소 추출 화면으로 보낸다
    private val importUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DulpickTheme {
                DulpickRoot(
                    importUrl = importUrl,
                    onImportUrlConsumed = { importUrl.value = null },
                )
            }
        }
        // 새로 실행됐을 때만 실행 인텐트를 본다(구성 변경 재생성 시 중복 처리 방지)
        if (savedInstanceState == null) handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    // '공유 → Dulpick' 하면 URL 텍스트가 EXTRA_TEXT 로 온다. 그 안에서 http(s) URL 을 뽑는다
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != MIME_TEXT) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        importUrl.value = extractWebUrl(text) ?: return
    }

    // 텍스트에서 첫 http(s) URL 을 찾는다 (iOS httpURL(from:) 대응)
    private fun extractWebUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group()
            if (candidate.startsWith("http://", ignoreCase = true) ||
                candidate.startsWith("https://", ignoreCase = true)
            ) {
                return candidate
            }
        }
        return null
    }

    private companion object {
        const val MIME_TEXT = "text/plain"
    }
}
