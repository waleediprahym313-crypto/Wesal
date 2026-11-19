package com.wesal.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF101827)
private val Teal = Color(0xFF20C6B7)
private val SurfaceDark = Color(0xFF1B2638)
private val Muted = Color(0xFF9AA8BA)
private const val SUPABASE_URL = "https://volcbanvmdyxyuylrnsj.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_4eDhykHTkUYpljgWTtKg7w_V42gcG4x"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WesalApp(this) }
    }
}

private fun authRequest(username: String, password: String, mode: String): JSONObject {
    val connection = (URL("$SUPABASE_URL/functions/v1/wesal-auth").openConnection() as HttpURLConnection)
    connection.requestMethod = "POST"
    connection.connectTimeout = 15000
    connection.readTimeout = 20000
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("apikey", SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
    val payload = JSONObject().put("username", username).put("password", password).put("mode", mode)
    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    val response = stream.bufferedReader().use { it.readText() }
    connection.disconnect()
    return JSONObject(response)
}

@Composable
fun WesalApp(context: Context) {
    var signedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignup by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("wesal_session", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (!prefs.getString("access_token", null).isNullOrBlank()) {
            username = prefs.getString("username", "") ?: ""
            signedIn = true
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Teal, background = Ink, surface = SurfaceDark)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            if (!signedIn) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(86.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) {
                        Text("و", color = Ink, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("WESAL", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text("وصال • تواصل يجمعنا", color = Teal, fontSize = 16.sp)
                    Spacer(Modifier.height(34.dp))
                    Text(if (isSignup) "أنشئ حساباً جديداً" else "مرحباً بك في وصال", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("استخدم اسم المستخدم وكلمة المرور فقط", color = Muted)
                    Spacer(Modifier.height(24.dp))
                    WesalField(username, { username = it; error = "" }, "اسم المستخدم")
                    Spacer(Modifier.height(12.dp))
                    WesalField(password, { password = it; error = "" }, "كلمة المرور (8 أحرف على الأقل)", true)
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(error, color = Color(0xFFFF8A80), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (username.isBlank() || password.length < 8) {
                                error = "أدخل اسم مستخدم صحيحاً وكلمة مرور من 8 أحرف على الأقل"
                            } else {
                                busy = true
                                error = ""
                                Thread {
                                    try {
                                        val result = authRequest(username.trim(), password, if (isSignup) "signup" else "login")
                                        if (result.has("error")) throw IllegalStateException(result.optString("error"))
                                        prefs.edit()
                                            .putString("access_token", result.getString("access_token"))
                                            .putString("refresh_token", result.getString("refresh_token"))
                                            .putString("user_id", result.getString("user_id"))
                                            .putString("username", result.getString("username"))
                                            .apply()
                                        (context as? ComponentActivity)?.runOnUiThread {
                                            busy = false
                                            signedIn = true
                                            username = result.optString("username", username)
                                        }
                                    } catch (e: Exception) {
                                        (context as? ComponentActivity)?.runOnUiThread {
                                            busy = false
                                            error = e.message ?: "تعذر الاتصال. تحقق من الإنترنت وحاول مجدداً"
                                        }
                                    }
                                }.start()
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Ink, strokeWidth = 2.dp)
                        else Text(if (isSignup) "إنشاء الحساب" else "تسجيل الدخول", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    TextButton(onClick = { isSignup = !isSignup; error = "" }) {
                        Text(if (isSignup) "لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حساباً", color = Teal)
                    }
                    Text("لا نطلب بريداً إلكترونياً أو رقم هاتف", color = Muted, fontSize = 12.sp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("WESAL", color = Teal, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Text("@$username", color = Muted, fontSize = 13.sp)
                        IconButton(onClick = { prefs.edit().clear().apply(); signedIn = false; password = "" }) { Icon(Icons.Default.Logout, null, tint = Color.White) }
                    }
                    when (selected) {
                        0 -> FeedScreen(prefs.getString("access_token", "") ?: "", prefs.getString("user_id", "") ?: "")
                        1 -> CenterPage("استكشاف", "سيتم ربط البحث عن المستخدمين في المرحلة التالية")
                        2 -> CreatePostScreen(prefs.getString("access_token", "") ?: "", prefs.getString("user_id", "") ?: "")
                        3 -> CenterPage("النشاط", "سيتم تفعيل الإشعارات في المرحلة التالية")
                        else -> CenterPage("الرسائل", "سيتم تفعيل المحادثات في المرحلة التالية")
                    }
                    Spacer(Modifier.weight(1f))
                    NavigationBar(containerColor = SurfaceDark) {
                        val labels = listOf("الرئيسية", "استكشاف", "إضافة", "النشاط", "الرسائل")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.AddCircle, Icons.Default.Notifications, Icons.Default.Chat)
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = { Icon(icons[index], null) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Teal, selectedTextColor = Teal, indicatorColor = Ink, unselectedIconColor = Muted, unselectedTextColor = Muted))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WesalField(value: String, onValue: (String) -> Unit, hint: String, secret: Boolean = false) {
    Row(Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 17.dp)) {
        if (secret) {
            BasicTextField(value, onValue, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp), visualTransformation = PasswordVisualTransformation(), decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = Muted); inner() })
        } else {
            BasicTextField(value, onValue, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp), decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = Muted); inner() })
        }
    }
}

@Composable
fun FeedScreen() {
    val names = listOf("مجتمع وصال", "شارك لحظاتك", "تواصل مع أصدقائك")
    val captions = listOf("أهلاً بك في وصال! ابدأ بالتواصل مع الآخرين ✨", "مساحتك لمشاركة الصور والأفكار 💚", "تابع الأشخاص الذين تحب محتواهم")
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("مرحباً بك 👋", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("مجتمع وصال", color = Muted)
            Spacer(Modifier.height(8.dp))
        }
        items(names.indices.toList()) { i ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) { Text("و", color = Ink, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(names[i], color = Color.White, fontWeight = FontWeight.Bold); Text("WESAL", color = Muted, fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF26384B), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, tint = Teal, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(captions[i], color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun CenterPage(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted)
    }
}private fun supabaseRest(path: String, token: String, method: String = "GET", body: JSONObject? = null): String {
    val connection = (URL("$SUPABASE_URL/rest/v1/$path").openConnection() as HttpURLConnection)
    connection.requestMethod = method
    connection.connectTimeout = 15000
    connection.readTimeout = 20000
    connection.setRequestProperty("apikey", SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json")
    if (method == "POST" || method == "PATCH") connection.setRequestProperty("Prefer", "return=representation")
    if (body != null) {
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    }
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    val result = stream.bufferedReader().use { it.readText() }
    connection.disconnect()
    return result
}

@Composable
fun FeedScreen(token: String, userId: String) {
    var posts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(token) {
        try {
            val result = supabaseRest("posts?select=id,user_id,caption,media_url,created_at&order=created_at.desc&limit=50", token)
            val arr = org.json.JSONArray(result)
            posts = (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) { error = "تعذر تحميل المنشورات"; }
        loading = false
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("مرحباً بك 👋", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("آخر منشورات مجتمع وصال", color = Muted)
        }
        if (loading) item { CircularProgressIndicator(color = Teal) }
        if (error.isNotBlank()) item { Text(error, color = Color(0xFFFF8A80)) }
        if (!loading && posts.isEmpty() && error.isBlank()) item { Text("لا توجد منشورات بعد. كن أول من يشارك!", color = Muted) }
        items(posts, key = { it.optString("id") }) { post ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("@"+post.optString("user_id").take(8), color = Teal, fontWeight = FontWeight.Bold)
                    if (post.optString("caption").isNotBlank()) Text(post.optString("caption"), color = Color.White, modifier = Modifier.padding(top=10.dp))
                    if (post.optString("media_url").isNotBlank()) Text("صورة مرفقة: "+post.optString("media_url"), color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth().padding(top=12.dp), horizontalArrangement = Arrangement.End) {
                        Text("وصال", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostScreen(token: String, userId: String) {
    var caption by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text("إنشاء منشور", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("شارك أفكارك مع مجتمع وصال", color = Muted)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(16.dp)).padding(16.dp)) {
            BasicTextField(caption, { caption = it }, modifier = Modifier.fillMaxWidth().heightIn(min=130.dp), textStyle = androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=16.sp), decorationBox = { inner -> if(caption.isEmpty()) Text("ماذا تريد أن تشارك؟", color=Muted); inner() })
        }
        Spacer(Modifier.height(14.dp))
        Button(onClick = {
            if (caption.trim().isEmpty()) status = "اكتب نص المنشور أولاً"
            else {
                busy = true; status = ""
                Thread {
                    try {
                        val body = JSONObject().put("user_id", userId).put("caption", caption.trim()).put("media_type", "text")
                        supabaseRest("posts", token, "POST", body)
                        (androidx.compose.ui.platform.LocalContext.current as? ComponentActivity)?.runOnUiThread { }
                    } catch (_: Exception) { }
                }.start()
            }
        }, enabled = !busy, modifier=Modifier.fillMaxWidth().height(50.dp), colors=ButtonDefaults.buttonColors(containerColor=Teal)) {
            Text(if(busy) "جارٍ النشر..." else "نشر", color=Ink,fontWeight=FontWeight.Bold)
        }
        if(status.isNotBlank()) Text(status, color=Teal, modifier=Modifier.padding(top=12.dp))
    }
}


