package org.haokee.recorder.ui.component

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Markdown Text Component
 *
 * Renders markdown content using Markwon library
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    // Convert Compose Color to Android Color (ARGB integer)
    val textColorInt = android.graphics.Color.argb(
        (textColor.alpha * 255).toInt(),
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt()
    )

    val linkColorInt = android.graphics.Color.argb(
        (linkColor.alpha * 255).toInt(),
        (linkColor.red * 255).toInt(),
        (linkColor.green * 255).toInt(),
        (linkColor.blue * 255).toInt()
    )

    val markwon = remember {
        Markwon.builder(context)
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorInt)
                setLinkTextColor(linkColorInt)
                textSize = 14f
                setTextIsSelectable(true)
            }
        },
        update = { textView ->
            textView.setTextColor(textColorInt)
            textView.setLinkTextColor(linkColorInt)
            markwon.setMarkdown(textView, markdown)
        }
    )
}
