package com.wesal.app

import android.content.Context
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
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
import java.util.UUID

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
                        1 -> ExploreScreen(prefs.getString("access_token", "") ?: "", prefs.getString("user_id", "") ?: "")
                        2 -> CreatePostScreen(prefs.getString("access_token", "") ?: "", prefs.getString("user_id", "") ?: "")
                        3 -> CenterPage("النشاط", "سيتم تفعيل الإشعارات في المرحلة التالية")
                        5 -> ProfileScreen(prefs.getString("access_token", "") ?: "", prefs.getString("user_id", "") ?: "", username)
                        else -> CenterPage("الرسائل", "سيتم تفعيل المحادثات في المرحلة التالية")
                    }
                    Spacer(Modifier.weight(1f))
                    NavigationBar(containerColor = SurfaceDark) {
                        val labels = listOf("الرئيسية", "استكشاف", "إضافة", "النشاط", "الرسائل", "حسابي")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.AddCircle, Icons.Default.Notifications, Icons.Default.Chat, Icons.Default.Person)
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
fun ProfileScreen(token: String, userId: String, username: String) {
    var display by remember { mutableStateOf(username) }
    var bio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(userId) {
        try {
            val arr=org.json.JSONArray(supabaseRest("profiles?select=display_name,bio&id=eq.$userId",token))
            if(arr.length()>0) { display=arr.getJSONObject(0).optString("display_name",username); bio=arr.getJSONObject(0).optString("bio","") }
        } catch(_:Exception){}
        loading=false
    }
    Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(90.dp).background(Teal,CircleShape),contentAlignment=Alignment.Center){Text("و",fontSize=42.sp,color=Ink,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(12.dp))
        Text("@$username",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp)
        Text("ملفك الشخصي",color=Muted)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value=display,onValueChange={display=it},modifier=Modifier.fillMaxWidth(),label={Text("الاسم الظاهر")},singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=bio,onValueChange={bio=it},modifier=Modifier.fillMaxWidth(),label={Text("نبذة عنك")},minLines=3)
        Spacer(Modifier.height(14.dp))
        Button(enabled=!saving,onClick={
            saving=true; status=""
            Thread {
                try { supabaseRest("profiles?id=eq.$userId",token,"PATCH",JSONObject().put("display_name",display.trim()).put("bio",bio.trim())); status="تم حفظ الملف الشخصي" } catch(_:Exception){status="تعذر الحفظ"}
                saving=false
            }.start()
        },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Teal)){Text(if(saving) "جارٍ الحفظ..." else "حفظ التعديلات",color=Ink)}
        if(status.isNotBlank()) Text(status,color=Teal,modifier=Modifier.padding(top=10.dp))
    }
}
@Composable
fun ExploreScreen(token: String, userId: String) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("ابحث عن الأشخاص باستخدام اسم المستخدم") }
    LaunchedEffect(query) {
        if(query.trim().length >= 2) {
            loading=true
            try {
                val q=java.net.URLEncoder.encode(query.trim(),"UTF-8")
                val arr=org.json.JSONArray(supabaseRest("profiles?select=id,username,display_name,avatar_url&username=ilike.%25$q%25&limit=30",token))
                results=(0 until arr.length()).map { arr.getJSONObject(it) }.filter { it.optString("id") != userId }
                message=if(results.isEmpty()) "لم نعثر على مستخدمين" else ""
            } catch(_:Exception) { message="تعذر البحث حالياً" }
            loading=false
        } else { results=emptyList(); message="اكتب حرفين على الأقل للبحث" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("استكشاف الأشخاص",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("اسم المستخدم")},leadingIcon={Icon(Icons.Default.Search,null)})
        if(loading) CircularProgressIndicator(color=Teal,modifier=Modifier.padding(16.dp))
        if(message.isNotBlank()) Text(message,color=Muted,modifier=Modifier.padding(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(results,key={it.optString("id")}) { profile ->
                Card(colors=CardDefaults.cardColors(containerColor=SurfaceDark)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(Teal,CircleShape),contentAlignment=Alignment.Center){Text("و",color=Ink,fontWeight=FontWeight.Bold)}
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("@${profile.optString("username")}",color=Color.White,fontWeight=FontWeight.Bold); if(profile.optString("display_name").isNotBlank()) Text(profile.optString("display_name"),color=Muted) }
                        TextButton(onClick={
                            Thread { try { supabaseRest("follows",token,"POST",JSONObject().put("follower_id",userId).put("following_id",profile.optString("id"))) } catch(_:Exception){} }.start()
                        }) { Text("متابعة",color=Teal) }
                    }
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
}
private fun uploadMedia(context: Context, token: String, userId: String, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("تعذر قراءة الصورة")
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
    val filename = "${UUID.randomUUID()}.$ext"
    val path = "$userId/$filename"
    val connection = (URL("$SUPABASE_URL/storage/v1/object/wesal-media/$path").openConnection() as HttpURLConnection)
    connection.requestMethod = "POST"
    connection.connectTimeout = 20000
    connection.readTimeout = 30000
    connection.doOutput = true
    connection.setRequestProperty("apikey", SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", mime)
    connection.setRequestProperty("x-upsert", "false")
    connection.outputStream.use { it.write(bytes) }
    if (connection.responseCode !in 200..299) {
        val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Upload failed"
        connection.disconnect()
        throw IllegalStateException(err)
    }
    connection.disconnect()
    return "$SUPABASE_URL/storage/v1/object/public/wesal-media/$path"
}

private fun supabaseRest(path: String, token: String, method: String = "GET", body: JSONObject? = null): String {
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
fun PostActions(postId: String, userId: String, token: String) {
    var likes by remember { mutableIntStateOf(0) }
    var liked by remember { mutableStateOf(false) }
    var comments by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(postId) {
        try {
            val a = org.json.JSONArray(supabaseRest("likes?select=user_id&post_id=eq.$postId", token))
            likes = a.length(); liked = (0 until a.length()).any { a.getJSONObject(it).optString("user_id") == userId }
            comments = org.json.JSONArray(supabaseRest("comments?select=id&post_id=eq.$postId", token)).length()
        } catch (_: Exception) {}
    }
    Row {
        TextButton(onClick={
            Thread {
                try { if(liked) supabaseRest("likes?post_id=eq.$postId&user_id=eq.$userId",token,"DELETE") else supabaseRest("likes",token,"POST",JSONObject().put("post_id",postId).put("user_id",userId)) } catch (_:Exception) {}
            }.start(); liked=!liked; likes += if(liked) 1 else -1
        }) { Icon(if(liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(liked) Color.Red else Muted); Text(" $likes",color=Muted) }
        TextButton(onClick={show=true}) { Icon(Icons.Default.Comment,null,tint=Muted); Text(" $comments",color=Muted) }
    }
    if(show) AlertDialog(onDismissRequest={show=false},title={Text("إضافة تعليق")},text={OutlinedTextField(value=text,onValueChange={text=it},label={Text("تعليقك")})},confirmButton={TextButton(onClick={
        val body=text.trim(); if(body.isNotEmpty()) Thread { try { supabaseRest("comments",token,"POST",JSONObject().put("post_id",postId).put("user_id",userId).put("body",body)); comments++ } catch(_:Exception){} }.start(); text=""; show=false
    }){Text("إرسال")}},dismissButton={TextButton(onClick={show=false}){Text("إلغاء")}})
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
                    if (post.optString("media_url").isNotBlank()) AsyncImage(model = post.optString("media_url"), contentDescription = "صورة المنشور", modifier = Modifier.fillMaxWidth().height(240.dp))
                    PostActions(post.optString("id"), userId, token)
                    Row(Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.End) { Text("وصال", color = Muted, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
fun CreatePostScreen(token: String, userId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var caption by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
        status = ""
    }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text("إنشاء منشور", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("شارك أفكارك وصورك مع مجتمع وصال", color = Muted)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(16.dp)).padding(16.dp)) {
            BasicTextField(caption, { caption = it }, modifier = Modifier.fillMaxWidth().heightIn(min=120.dp), textStyle = androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=16.sp), decorationBox = { inner -> if(caption.isEmpty()) Text("ماذا تريد أن تشارك؟", color=Muted); inner() })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, null, tint=Teal)
            Spacer(Modifier.width(8.dp))
            Text(if(selectedImage == null) "إضافة صورة" else "تغيير الصورة", color=Teal)
        }
        if (selectedImage != null) Text("تم اختيار صورة لإرفاقها", color=Teal, modifier=Modifier.padding(top=8.dp))
        Spacer(Modifier.height(14.dp))
        Button(onClick = {
            if (caption.trim().isEmpty() && selectedImage == null) status = "اكتب نصاً أو اختر صورة"
            else {
                busy = true; status = ""
                val imageUri = selectedImage
                val text = caption.trim()
                Thread {
                    try {
                        val mediaUrl = if (imageUri != null) uploadMedia(context, token, userId, imageUri) else ""
                        val body = JSONObject().put("user_id", userId).put("caption", text)
                            .put("media_type", if(mediaUrl.isNotBlank()) "image" else "text")
                            .put("media_url", mediaUrl)
                        supabaseRest("posts", token, "POST", body)
                        (context as? ComponentActivity)?.runOnUiThread { busy = false; status = "تم نشر منشورك بنجاح"; caption = ""; selectedImage = null }
                    } catch (e: Exception) {
                        (context as? ComponentActivity)?.runOnUiThread { busy = false; status = "تعذر النشر: تحقق من الاتصال وحاول مجدداً" }
                    }
                }.start()
            }
        }, enabled = !busy, modifier=Modifier.fillMaxWidth().height(50.dp), colors=ButtonDefaults.buttonColors(containerColor=Teal)) {
            Text(if(busy) "جارٍ النشر..." else "نشر", color=Ink,fontWeight=FontWeight.Bold)
        }
        if(status.isNotBlank()) Text(status, color=Teal, modifier=Modifier.padding(top=12.dp))
    }
}

