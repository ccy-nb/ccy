package com.agentapp.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * V3 前端面板 — 用 WebView 渲染角色卡的 v3_style HTML/CSS/JS。
 * 仅在角色卡包含 v3_style 时渲染，否则不显示。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun V3StylePanel(
    html: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 600
) {
    if (html.isBlank()) return

    val baseHtml = remember(html) {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, sans-serif; 
            background: transparent; 
            color: #e5e7eb;
            padding: 12px;
        }
        $html
        </style>
        </head>
        <body></body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(null, baseHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = maxHeight.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}
