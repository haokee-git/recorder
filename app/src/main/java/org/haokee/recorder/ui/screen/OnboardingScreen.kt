package org.haokee.recorder.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.haokee.recorder.R

private data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    val title: String,
    val subtitle: String
)

private val pages = listOf(
    OnboardingPage(
        imageRes = R.drawable.onboarding_list,
        title = "一键录音，留住灵感",
        subtitle = "随时随地，用声音捕捉转瞬即逝的念头"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_categories,
        title = "井然有序，分类归档",
        subtitle = "已转换、原始录音、过期提醒，一目了然"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_color,
        title = "色彩标记，赋予意义",
        subtitle = "八种颜色，让每条感言拥有专属印记"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_filter,
        title = "精准筛选，快速定位",
        subtitle = "按颜色筛选，在纷繁中找到所需"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_alarm,
        title = "定时提醒，不再遗忘",
        subtitle = "为感言设置闹钟，让重要的事准时浮现"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_ai_chat,
        title = "AI 相伴，智慧对话",
        subtitle = "与 AI 交流你的感言，获得全新视角"
    ),
    OnboardingPage(
        imageRes = R.drawable.onboarding_dark_mode,
        title = "暗夜模式，温柔护眼",
        subtitle = "深色主题，在夜晚也能舒适记录"
    )
)

private val ActiveDotColor = Color(0xFF64B5F6)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = page.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // Text area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = page.subtitle,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Dots indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { index, _ ->
                val isActive = index == currentPage
                val size by animateDpAsState(
                    targetValue = if (isActive) 10.dp else 6.dp,
                    animationSpec = tween(200),
                    label = "dotSize"
                )
                val color by animateColorAsState(
                    targetValue = if (isActive) ActiveDotColor
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(200),
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    text = "跳过",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentPage < pages.lastIndex) "继续" else "开始使用"
                )
            }
        }
    }
}
