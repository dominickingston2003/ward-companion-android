package com.wardcompanion.ui.photos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wardcompanion.ui.patients.PatientDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    patientId: String,
    initialIndex: Int,
    onBack: () -> Unit,
    vm: PatientDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(patientId) { vm.start(patientId) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val photos = ui.photos
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
    ) { photos.size }
    var chromeVisible by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = photos[page]
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            val req = ImageRequest.Builder(LocalContext.current)
                .data("${ui.baseUrl.trimEnd('/')}${photo.urlPath}")
                .apply { ui.authHeader?.let { addHeader("Authorization", it) } }
                .crossfade(true)
                .build()
            AsyncImage(
                model = req,
                contentDescription = photo.caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { chromeVisible = !chromeVisible },
                            onDoubleTap = {
                                if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
                                else scale = 2.5f
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else { offsetX = 0f; offsetY = 0f }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offsetX, translationY = offsetY,
                    ),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${photos.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                ),
            )
        }
    }
}
