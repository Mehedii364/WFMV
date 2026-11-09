package com.example.core.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.storage.ApkMetadata
import com.example.core.storage.ArchiveManager
import com.example.core.storage.DeveloperToolsHelper
import com.example.core.storage.FileDiagnostics
import com.example.core.storage.FileItem
import com.example.core.storage.HexRow
import com.example.core.storage.ZipEntryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AppInsightItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val apkSizeBytes: Long,
    val icon: Drawable?
) {
    val formattedSize: String
        get() = FileItem.formatBytes(apkSizeBytes)
}

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    // Developer Tools Diagnostics
    private val _selectedDiagnosticFile = MutableStateFlow<File?>(null)
    val selectedDiagnosticFile: StateFlow<File?> = _selectedDiagnosticFile.asStateFlow()

    private val _diagnostics = MutableStateFlow<FileDiagnostics?>(null)
    val diagnostics: StateFlow<FileDiagnostics?> = _diagnostics.asStateFlow()

    private val _hexDump = MutableStateFlow<List<HexRow>>(emptyList())
    val hexDump: StateFlow<List<HexRow>> = _hexDump.asStateFlow()

    private val _apkInfo = MutableStateFlow<ApkMetadata?>(null)
    val apkInfo: StateFlow<ApkMetadata?> = _apkInfo.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Archive Preview
    private val _zipPreviewEntries = MutableStateFlow<List<ZipEntryItem>>(emptyList())
    val zipPreviewEntries: StateFlow<List<ZipEntryItem>> = _zipPreviewEntries.asStateFlow()

    // App Insights
    private val _installedApps = MutableStateFlow<List<AppInsightItem>>(emptyList())
    val installedApps: StateFlow<List<AppInsightItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Feedback
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun clearToast() {
        _toast.value = null
    }

    fun analyzeFile(file: File) {
        viewModelScope.launch {
            _selectedDiagnosticFile.value = file
            _isAnalyzing.value = true
            _diagnostics.value = DeveloperToolsHelper.analyzeFile(file)
            _hexDump.value = DeveloperToolsHelper.generateHexDump(file)

            if (file.extension.equals("apk", ignoreCase = true)) {
                _apkInfo.value = DeveloperToolsHelper.inspectApk(getApplication(), file)
            } else {
                _apkInfo.value = null
            }

            if (file.extension.equals("zip", ignoreCase = true)) {
                _zipPreviewEntries.value = ArchiveManager.previewZip(file)
            } else {
                _zipPreviewEntries.value = emptyList()
            }

            _isAnalyzing.value = false
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                packages.mapNotNull { appInfo ->
                    try {
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val pkg = appInfo.packageName
                        val pInfo = pm.getPackageInfo(pkg, 0)
                        val version = pInfo.versionName ?: "1.0"
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val apkFile = File(appInfo.sourceDir)
                        val size = if (apkFile.exists()) apkFile.length() else 0L
                        val icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                        AppInsightItem(name, pkg, version, isSystem, size, icon)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.apkSizeBytes }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun openAppSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            _toast.value = "Unable to open app settings"
        }
    }

    fun launchApp(packageName: String) {
        try {
            val pm = getApplication<Application>().packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            } else {
                _toast.value = "Cannot launch $packageName directly"
            }
        } catch (e: Exception) {
            _toast.value = "Failed to launch app"
        }
    }
}
