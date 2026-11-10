package com.nova.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import java.util.Locale

private val Ink = Color(0xFF17243A)
private val Blue = Color(0xFF2864F0)
private val Pale = Color(0xFFF4F7FC)
private val Muted = Color(0xFF7D8AA1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NovaApp() }
    }
}

private data class BrowserTab(
    val id: Int,
    var url: String = "",
    var title: String = "New tab",
    var progress: Int = 0,
    var webView: WebView? = null,
    val incognito: Boolean = false
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NovaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tabs = remember { mutableStateListOf(BrowserTab(1)) }
    var activeId by remember { mutableIntStateOf(1) }
    var address by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("Nova Browser") }
    var loading by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var adBlock by remember { mutableStateOf(true) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var isHome by remember { mutableStateOf(true) }
    var pageUrl by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val activeTab = tabs.firstOrNull { it.id == activeId }

    fun navigate(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val target = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(input, "UTF-8")
        }
        address = target
        pageUrl = target
        isHome = false
        activeTab?.let { it.url = target; it.webView?.loadUrl(target) }
        keyboard?.hide()
    }

    fun newTab(isPrivate: Boolean = false) {
        val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        tabs.add(BrowserTab(id, incognito = isPrivate))
        activeId = id
        address = ""
        pageUrl = ""
        isHome = true
        showTabs = false
    }

    BackHandler(enabled = !isHome || showTabs || showMenu) {
        when {
            showMenu -> showMenu = false
            showTabs -> showTabs = false
            activeTab?.webView?.canGoBack() == true -> activeTab.webView?.goBack()
            else -> { isHome = true; address = "" }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Blue, background = Color.White, surface = Color.White,
        onBackground = Ink, onSurface = Ink
    )) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            // Brand / top controls
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).background(Blue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Public, null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("NOVA", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Ink, letterSpacing = 2.sp)
                    Text("BROWSE BEYOND", fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp)
                }
                IconButton(onClick = { showTabs = true }) {
                    BadgedBox(badge = { Badge { Text(tabs.size.toString()) } }) {
                        Icon(Icons.Default.Tab, "Tabs", tint = Ink)
                    }
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = Ink)
                }
            }

            // Search / address bar
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(18.dp))
                    .background(Pale, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Blue)
                Spacer(Modifier.width(9.dp))
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                    decorationBox = { inner ->
                        if (address.isEmpty()) Text("Search or enter website", color = Muted, fontSize = 14.sp)
                        inner()
                    }
                )
                if (address.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Clear", Modifier.clickable { address = "" }.size(19.dp), tint = Muted)
                } else {
                    Icon(Icons.Default.MicNone, "Voice search", tint = Muted)
                }
            }

            AnimatedVisibility(visible = loading in 1..99, enter = fadeIn(), exit = fadeOut()) {
                LinearProgressIndicator(progress = { loading / 100f }, Modifier.fillMaxWidth().height(2.dp), color = Blue)
            }

            if (showTabs) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your tabs", fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { newTab() }) { Icon(Icons.Default.Add, null); Text("New") }
                    }
                    tabs.forEach { tab ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .background(Pale, RoundedCornerShape(14.dp))
                                .clickable { activeId = tab.id; address = tab.url; pageUrl = tab.url; isHome = tab.url.isEmpty(); showTabs = false }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (tab.incognito) Icons.Default.VisibilityOff else Icons.Default.Language, null, tint = Blue)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tab.title, fontWeight = FontWeight.SemiBold)
                                Text(tab.url.ifEmpty { "New tab" }, color = Muted, fontSize = 11.sp, maxLines = 1)
                            }
                            IconButton(onClick = {
                                tab.webView?.destroy()
                                tabs.remove(tab)
                                if (tabs.isEmpty()) newTab() else if (activeId == tab.id) activeId = tabs.first().id
                            }) { Icon(Icons.Default.Close, "Close tab") }
                        }
                    }
                }
            } else if (isHome) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().background(Color.White).padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(54.dp))
                    Box(Modifier.size(76.dp).background(Color(0xFFEAF0FF), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Public, null, tint = Blue, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Explore without limits.", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("A faster, cleaner way to browse.", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(34.dp))
                    Text("QUICK ACCESS", Modifier.fillMaxWidth(), color = Muted, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    val shortcuts = listOf("Google" to "https://www.google.com", "YouTube" to "https://m.youtube.com", "Wikipedia" to "https://www.wikipedia.org", "Bing" to "https://www.bing.com")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shortcuts.size) { i ->
                            val item = shortcuts[i]
                            Column(
                                Modifier.width(76.dp).clickable { navigate(item.second) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.size(54.dp).background(Pale, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                                    Text(item.first.take(1), color = Blue, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(item.first, fontSize = 11.sp, color = Ink)
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF0F6FF), RoundedCornerShape(16.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Blue)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Protection is on", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Built-in blocking for known ad domains", color = Muted, fontSize = 11.sp)
                        }
                        Switch(checked = adBlock, onCheckedChange = { adBlock = it })
                    }
                    Spacer(Modifier.weight(1f))
                    Text("PRIVATE BY DESIGN  •  MADE FOR SPEED", color = Muted, fontSize = 9.sp, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(22.dp))
                }
            } else {
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        val wv = WebView(ctx)
                        wv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        wv.settings.javaScriptEnabled = true
                        wv.settings.domStorageEnabled = true
                        wv.settings.loadsImagesAutomatically = true
                        wv.settings.useWideViewPort = true
                        wv.settings.loadWithOverviewMode = true
                        wv.settings.setSupportZoom(true)
                        wv.settings.builtInZoomControls = true
                        wv.settings.displayZoomControls = false
                        wv.webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) { loading = newProgress }
                            override fun onReceivedTitle(view: WebView?, title: String?) { currentTitle = title ?: "Nova Browser"; activeTab?.title = currentTitle }
                        }
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = 8; url?.let { pageUrl = it; address = it; activeTab?.url = it }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) { loading = 100; isHome = false }
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val host = request?.url?.host?.lowercase(Locale.ROOT) ?: return null
                                val blocked = adBlock && AdDomains.matches(host)
                                if (blocked) blockedCount++
                                return if (blocked) WebResourceResponse("text/plain", "utf-8", 204, "Blocked", emptyMap(), java.io.ByteArrayInputStream(ByteArray(0))) else null
                            }
                        }
                        activeTab?.webView = wv
                        wv.loadUrl(pageUrl)
                        wv
                    },
                    update = { wv -> if (activeTab?.webView !== wv) activeTab?.webView = wv }
                )
            }

            // Bottom toolbar
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 13.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomAction(Icons.Default.ArrowBack, "Back") { activeTab?.webView?.let { if (it.canGoBack()) it.goBack() else { isHome = true; address = "" } } }
                BottomAction(Icons.Default.ArrowForward, "Forward") { activeTab?.webView?.let { if (it.canGoForward()) it.goForward() } }
                BottomAction(Icons.Default.Home, "Home") { isHome = true; address = ""; pageUrl = "" }
                BottomAction(Icons.Default.Refresh, "Reload") { activeTab?.webView?.reload() }
                BottomAction(Icons.Default.Shield, "Privacy") { showMenu = true }
            }
        }

        if (showMenu) {
            ModalBottomSheet(onDismissRequest = { showMenu = false }, containerColor = Color.White) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 24.dp)) {
                    Text("Nova controls", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    MenuRow(Icons.Default.Add, "New tab", "Open a fresh browsing tab") { showMenu = false; newTab() }
                    MenuRow(Icons.Default.VisibilityOff, "New incognito tab", "Do not keep browsing history") { showMenu = false; newTab(true) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Blue)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) { Text("Ad blocker", fontWeight = FontWeight.SemiBold); Text("$blockedCount requests blocked this session", color = Muted, fontSize = 11.sp) }
                        Switch(checked = adBlock, onCheckedChange = { adBlock = it })
                    }
                    MenuRow(Icons.Default.BookmarkBorder, "Bookmarks", "Bookmark support coming next") { showMenu = false }
                    MenuRow(Icons.Default.History, "History", "Regular-tab history is session-only in this starter") { showMenu = false }
                    MenuRow(Icons.Default.Download, "Downloads", "Use Android download notifications where supported") { showMenu = false }
                }
            }
        }
    }
}

@Composable
private fun BottomAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, action: () -> Unit) {
    Column(Modifier.clickable { action() }.padding(horizontal = 8.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(21.dp))
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Blue)
        Spacer(Modifier.width(14.dp))
        Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 11.sp) }
    }
}

private object AdDomains {
    private val domains = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "adform.net", " taboola.com",
        "outbrain.com", "popads.net", "popcash.net", "adsterra.com",
        "exoclick.com", "smartadserver.com", "criteo.com", "scorecardresearch.com"
    ).map { it.trim() }

    fun matches(host: String): Boolean = domains.any { host == it || host.endsWith(".$it") }
}
