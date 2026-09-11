package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageNotice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun PageNoticeBanner(
    pageId: String,
    modifier: Modifier = Modifier
) {
    var activeNotice by remember { mutableStateOf<PageNotice?>(null) }
    var isDismissed by remember { mutableStateOf(false) }

    DisposableEffect(pageId) {
        val db = FirebaseFirestore.getInstance()
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("page_notices")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val notices = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PageNotice::class.java)?.copy(id = doc.id)
                    }
                    // Prioritize specific page notice over 'all'
                    val match = notices.firstOrNull { it.pageId.equals(pageId, ignoreCase = true) }
                        ?: notices.firstOrNull { it.pageId.equals("all", ignoreCase = true) }
                    activeNotice = match
                    isDismissed = false
                }
        } catch (e: Exception) {
            // Ignore
        }

        onDispose {
            listener?.remove()
        }
    }

    AnimatedVisibility(
        visible = activeNotice != null && !isDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        activeNotice?.let { notice ->
            val (accentColor, bgColor, borderColor, icon) = when (notice.type.lowercase().trim()) {
                "error" -> Quadruple(
                    Color(0xFFFF5252),
                    Color(0xFF2B1116),
                    Color(0xFFFF5252).copy(alpha = 0.5f),
                    Icons.Filled.ErrorOutline
                )
                "warning" -> Quadruple(
                    Color(0xFFFFB300),
                    Color(0xFF281E0B),
                    Color(0xFFFFB300).copy(alpha = 0.5f),
                    Icons.Filled.WarningAmber
                )
                "success" -> Quadruple(
                    Color(0xFF00E676),
                    Color(0xFF0C2417),
                    Color(0xFF00E676).copy(alpha = 0.5f),
                    Icons.Filled.CheckCircle
                )
                else -> Quadruple(
                    Color(0xFF00E5FF),
                    Color(0xFF0D1F2D),
                    Color(0xFF00E5FF).copy(alpha = 0.5f),
                    Icons.Filled.Info
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Notice Icon",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notice.title.ifBlank { "Notice / Announcement" },
                                color = accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { isDismissed = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (notice.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = notice.message,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
