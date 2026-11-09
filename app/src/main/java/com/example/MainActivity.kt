package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.core.storage.FileCategory
import com.example.core.viewmodels.FileManagerViewModel
import com.example.core.viewmodels.PrivateSpaceViewModel
import com.example.core.viewmodels.StorageCleanerViewModel
import com.example.core.viewmodels.ToolsViewModel
import com.example.ui.components.WfmvBottomNav
import com.example.ui.components.WfmvNavDestination
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.PrivateSpaceScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.WfmvTheme

class MainActivity : ComponentActivity() {

    private val fileManagerViewModel: FileManagerViewModel by viewModels()
    private val storageCleanerViewModel: StorageCleanerViewModel by viewModels()
    private val privateSpaceViewModel: PrivateSpaceViewModel by viewModels()
    private val toolsViewModel: ToolsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WfmvTheme {
                WfmvApp(
                    fileManagerViewModel = fileManagerViewModel,
                    storageCleanerViewModel = storageCleanerViewModel,
                    privateSpaceViewModel = privateSpaceViewModel,
                    toolsViewModel = toolsViewModel
                )
            }
        }
    }
}

@Composable
fun WfmvApp(
    fileManagerViewModel: FileManagerViewModel,
    storageCleanerViewModel: StorageCleanerViewModel,
    privateSpaceViewModel: PrivateSpaceViewModel,
    toolsViewModel: ToolsViewModel
) {
    var currentDestination by remember { mutableStateOf(WfmvNavDestination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbars and toasts
    val fileMsg by fileManagerViewModel.snackbarMessage.collectAsState()
    val cleanerMsg by storageCleanerViewModel.toastMessage.collectAsState()
    val vaultMsg by privateSpaceViewModel.statusMessage.collectAsState()
    val toolsMsg by toolsViewModel.toast.collectAsState()

    LaunchedEffect(fileMsg) {
        fileMsg?.let {
            snackbarHostState.showSnackbar(it)
            fileManagerViewModel.clearSnackbar()
        }
    }

    LaunchedEffect(cleanerMsg) {
        cleanerMsg?.let {
            snackbarHostState.showSnackbar(it)
            storageCleanerViewModel.clearToast()
        }
    }

    LaunchedEffect(vaultMsg) {
        vaultMsg?.let {
            snackbarHostState.showSnackbar(it)
            privateSpaceViewModel.clearStatus()
        }
    }

    LaunchedEffect(toolsMsg) {
        toolsMsg?.let {
            snackbarHostState.showSnackbar(it)
            toolsViewModel.clearToast()
        }
    }

    // Permission handling launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        fileManagerViewModel.refreshCurrentDirectory()
        storageCleanerViewModel.refreshStorageInfo()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            WfmvBottomNav(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "NavTransition"
            ) { target ->
                when (target) {
                    WfmvNavDestination.HOME -> DashboardScreen(
                        fileManagerViewModel = fileManagerViewModel,
                        storageCleanerViewModel = storageCleanerViewModel,
                        onNavigate = { currentDestination = it },
                        onCategoryClick = { category ->
                            fileManagerViewModel.setFilterCategory(category)
                            currentDestination = WfmvNavDestination.FILES
                        }
                    )
                    WfmvNavDestination.FILES -> FileManagerScreen(
                        viewModel = fileManagerViewModel
                    )
                    WfmvNavDestination.STORAGE -> StorageScreen(
                        viewModel = storageCleanerViewModel
                    )
                    WfmvNavDestination.VAULT -> PrivateSpaceScreen(
                        viewModel = privateSpaceViewModel
                    )
                    WfmvNavDestination.TOOLS -> ToolsScreen(
                        viewModel = toolsViewModel
                    )
                }
            }
        }
    }
}
