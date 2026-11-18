package com.wesal.app

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

private val Ink = Color(0xFF101827)
private val Teal = Color(0xFF20C6B7)
private val SurfaceDark = Color(0xFF1B2638)
private val Muted = Color(0xFF9AA8BA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WesalApp() }
    }
}

@Composable
fun WesalApp() {
    var signedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }
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
                    Text("مرحباً بك في وصال", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("أنشئ حسابك أو سجّل الدخول", color = Muted)
                    Spacer(Modifier.height(24.dp))
                    WesalField(username, { username = it }, "اسم المستخدم")
                    Spacer(Modifier.height(12.dp))
                    WesalField(password, { password = it }, "كلمة المرور", true)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { if (username.isNotBlank() && password.isNotBlank()) signedIn = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal), shape = RoundedCornerShape(16.dp)) {
                        Text("دخول تجريبي", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("نسخة تجريبية: لا يتم إنشاء حساب حقيقي بعد", color = Muted, fontSize = 12.sp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("WESAL", color = Teal, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, null, tint = Color.White) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Send, null, tint = Color.White) }
                    }
                    when (selected) {
                        0 -> FeedScreen()
                        1 -> CenterPage("استكشاف", "ابحث عن أشخاص واهتمامات جديدة")
                        2 -> CenterPage("إنشاء منشور", "مشاركة الصور والمنشورات ستتوفر بعد ربط الخادم")
                        3 -> CenterPage("النشاط", "ستظهر هنا الإشعارات والتفاعلات")
                        else -> CenterPage("الرسائل", "المراسلة الحقيقية ستتوفر بعد ربط الخادم")
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
    val names = listOf("ليث الرياضي", "سارة", "أحمد")
    val captions = listOf("بداية يوم جميل! أهلاً بأصدقائي في وصال ✨", "لحظات تستحق أن نتشاركها 💚", "مرحباً بالجميع في مجتمع وصال")
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("مرحباً بك 👋", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("آخر المنشورات من مجتمعك", color = Muted)
            Spacer(Modifier.height(8.dp))
        }
        items(names.indices.toList()) { i ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) { Text(names[i].take(1), color = Ink, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Column { Text(names[i], color = Color.White, fontWeight = FontWeight.Bold); Text("منذ قليل • نسخة تجريبية", color = Muted, fontSize = 11.sp) }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.MoreHoriz, null, tint = Muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF26384B), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, tint = Teal, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(captions[i], color = Color.White, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, null, tint = Teal)
                        Spacer(Modifier.width(6.dp))
                        Text("إعجاب", color = Muted)
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Default.ModeComment, null, tint = Muted)
                        Spacer(Modifier.width(6.dp))
                        Text("تعليق", color = Muted)
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
