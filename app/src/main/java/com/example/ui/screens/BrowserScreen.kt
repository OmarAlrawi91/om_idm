package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.FileType
import com.example.ui.components.OneUiFloatingChip
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VividEmerald
import java.net.URLEncoder
import java.util.Locale

data class InterceptedDownload(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long
)

data class QuickBookmark(
    val name: String,
    val url: String,
    val category: String
)

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    onStartDownload: (url: String, fileName: String, fileType: FileType, totalBytes: Long) -> Unit,
    onAddTorrent: (magnetOrUrl: String) -> Unit,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl?.takeIf { it.isNotBlank() } ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(if (currentUrl != "about:blank") currentUrl else "") }
    var pageTitle by remember { mutableStateOf("OM IDM Browser") }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var interceptedDownload by remember { mutableStateOf<InterceptedDownload?>(null) }

    val focusManager = LocalFocusManager.current

    val quickBookmarks = remember {
        listOf(
            QuickBookmark("Google", "https://www.google.com", "Search"),
            QuickBookmark("GitHub", "https://github.com", "Dev"),
            QuickBookmark("Internet Archive", "https://archive.org", "Archive"),
            QuickBookmark("SourceForge", "https://sourceforge.net", "Open Source"),
            QuickBookmark("Wikipedia", "https://www.wikipedia.org", "Reference"),
            QuickBookmark("Ubuntu Releases", "https://releases.ubuntu.com", "Linux")
        )
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank() && initialUrl != currentUrl) {
            currentUrl = initialUrl
            inputUrl = initialUrl
            webViewInstance?.loadUrl(initialUrl)
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    fun navigateTo(rawInput: String) {
        focusManager.clearFocus()
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return

        val target = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("magnet:?") -> {
                onAddTorrent(trimmed)
                return
            }
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }

        currentUrl = target
        inputUrl = target
        webViewInstance?.loadUrl(target)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High-performance One UI Eye-Comfort Browser Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(38.dp).testTag("btn_browser_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(38.dp).testTag("btn_browser_forward")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentUrl = "about:blank"
                            inputUrl = ""
                            webViewInstance?.loadUrl("about:blank")
                        },
                        modifier = Modifier.size(38.dp).testTag("btn_browser_home")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Omnibox URL Text Field
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        singleLine = true,
                        placeholder = { Text("Search or type URL...", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { navigateTo(inputUrl) }),
                        trailingIcon = {
                            if (inputUrl.isNotBlank()) {
                                IconButton(onClick = { inputUrl = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("input_browser_url")
                    )

                    IconButton(
                        onClick = {
                            if (loadingProgress in 1..99) {
                                webViewInstance?.stopLoading()
                            } else {
                                webViewInstance?.reload()
                            }
                        },
                        modifier = Modifier.size(38.dp).testTag("btn_browser_refresh")
                    ) {
                        Icon(
                            imageVector = if (loadingProgress in 1..99) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Refresh or Stop",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewInstance?.settings?.let { settings ->
                                settings.userAgentString = if (isDesktopMode) {
                                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                                } else {
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                }
                                webViewInstance?.reload()
                            }
                        },
                        modifier = Modifier.size(38.dp).testTag("btn_browser_desktop_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                            contentDescription = "Toggle Desktop Mode",
                            tint = if (isDesktopMode) VividEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (loadingProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    color = accentColor,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Hardware Acceleration Enabled
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = false
                            allowContentAccess = false
                            // Modern mobile Chrome User-Agent
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)

                        // Built-in Download Interceptor
                        setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            val guessedName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            interceptedDownload = InterceptedDownload(
                                url = url,
                                fileName = guessedName,
                                mimeType = mimetype ?: "application/octet-stream",
                                contentLength = contentLength
                            )
                        })

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val urlStr = request?.url?.toString() ?: return false
                                return handleCustomUrl(urlStr)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return false
                                return handleCustomUrl(url)
                            }

                            private fun handleCustomUrl(targetUrl: String): Boolean {
                                // Intercept Magnet URIs directly
                                if (targetUrl.startsWith("magnet:?")) {
                                    onAddTorrent(targetUrl)
                                    return true
                                }

                                // Intercept .torrent links directly
                                if (targetUrl.endsWith(".torrent", ignoreCase = true) || targetUrl.contains(".torrent?")) {
                                    onAddTorrent(targetUrl)
                                    return true
                                }

                                // Intercept direct downloadable binary/media files
                                val lower = targetUrl.lowercase(Locale.ROOT)
                                val downloadExts = listOf(".iso", ".apk", ".zip", ".rar", ".7z", ".tar.gz", ".exe", ".mp4", ".mkv", ".mp3", ".pdf")
                                if (downloadExts.any { lower.endsWith(it) }) {
                                    val guessedName = URLUtil.guessFileName(targetUrl, null, null)
                                    interceptedDownload = InterceptedDownload(
                                        url = targetUrl,
                                        fileName = guessedName,
                                        mimeType = "application/octet-stream",
                                        contentLength = -1L
                                    )
                                    return true
                                }

                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let {
                                    if (it != "about:blank") {
                                        currentUrl = it
                                        inputUrl = it
                                    }
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                pageTitle = view?.title ?: "OM IDM Browser"
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                        if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                            loadUrl(currentUrl)
                        }
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // When page is "about:blank", render clean quick web bookmarks
            if (currentUrl == "about:blank" || currentUrl.isBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentColor.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = accentColor)
                                    }
                                    Column {
                                        Text(
                                            text = "OM IDM High-Speed Browser",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = accentColor
                                        )
                                        Text(
                                            text = "Hardware accelerated browsing with automatic download & torrent interception",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Quick Web Bookmarks",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickBookmarks.forEach { bookmark ->
                                OneUiFloatingChip(
                                    selected = false,
                                    onClick = { navigateTo(bookmark.url) },
                                    label = bookmark.name,
                                    icon = Icons.Default.Public,
                                    accentColor = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Intercepted Download Dialog
    interceptedDownload?.let { intercepted ->
        AlertDialog(
            onDismissRequest = { interceptedDownload = null },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text(
                    text = "OM IDM Intercepted Download",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The internal browser intercepted a downloadable file:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = intercepted.fileName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (intercepted.contentLength > 0) {
                                val sizeMb = String.format(Locale.US, "%.2f MB", intercepted.contentLength / (1024f * 1024f))
                                Text(
                                    text = "Size: $sizeMb",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VividEmerald
                                )
                            }
                            Text(
                                text = intercepted.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileType = FileType.fromFileNameOrMime(intercepted.fileName, intercepted.mimeType)
                        onStartDownload(intercepted.url, intercepted.fileName, fileType, intercepted.contentLength)
                        interceptedDownload = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Download with OM IDM", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { interceptedDownload = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
