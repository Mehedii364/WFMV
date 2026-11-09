package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.VaultItemEntity
import com.example.core.database.VaultNoteEntity
import com.example.core.storage.FileItem
import com.example.core.viewmodels.PrivateSpaceViewModel
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PrivateSpaceScreen(
    viewModel: PrivateSpaceViewModel
) {
    val isPinConfigured by viewModel.isPinConfigured.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("private_space_screen")
    ) {
        if (!isUnlocked) {
            VaultLockScreen(
                isPinConfigured = isPinConfigured,
                statusMessage = statusMessage,
                onSetupPin = { viewModel.setupPin(it) },
                onUnlock = { viewModel.unlockWithPin(it) },
                onRecover = { key, pin -> viewModel.recoverWithKey(key, pin) }
            )
        } else {
            VaultContentScreen(viewModel)
        }
    }
}

@Composable
private fun VaultLockScreen(
    isPinConfigured: Boolean,
    statusMessage: String?,
    onSetupPin: (String) -> Boolean,
    onUnlock: (String) -> Boolean,
    onRecover: (String, String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceElevated)
                .border(2.dp, GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield",
                tint = GoldPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WAFA PRIVATE SPACE",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Your Personal Digital Sanctuary",
            style = MaterialTheme.typography.labelMedium,
            color = GoldSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Indicators (dots)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val length = if (isPinConfigured) 4 else 4
            for (i in 0 until length) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) GoldPrimary else ObsidianSurfaceElevated)
                        .border(1.dp, if (filled) GoldPrimary else ObsidianBorder, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        statusMessage?.let { msg ->
            Text(
                text = msg,
                color = if (msg.contains("Denied") || msg.contains("Invalid")) CrimsonError else EmeraldSuccess,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad Grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "OK")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, CircleShape)
                                .clickable {
                                    when (key) {
                                        "C" -> pin = ""
                                        "OK" -> {
                                            if (isPinConfigured) {
                                                onUnlock(pin)
                                            } else {
                                                if (pin.length >= 4) {
                                                    onSetupPin(pin)
                                                }
                                            }
                                            pin = ""
                                        }
                                        else -> {
                                            if (pin.length < 6) {
                                                pin += key
                                                if (isPinConfigured && pin.length == 4) {
                                                    onUnlock(pin)
                                                    pin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = if (key == "OK") GoldPrimary else TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isPinConfigured) {
            TextButton(onClick = { showRecoveryDialog = true }) {
                Text("Forgot PIN? Use Recovery Key", color = GoldSecondary, fontSize = 12.sp)
            }
        } else {
            Text(
                text = "Enter a 4-digit PIN to establish your AES-256 encrypted vault.",
                color = TextTertiary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showRecoveryDialog) {
        var recKeyInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Vault Key Recovery", color = GoldPrimary) },
            text = {
                Column {
                    Text("Enter your 16-character recovery key (e.g. WFMV-XXXX-XXXX-XXXX):", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recKeyInput,
                        onValueChange = { recKeyInput = it },
                        placeholder = { Text("WFMV-...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = ObsidianBorder)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Enter new 4-digit PIN:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        placeholder = { Text("1234") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = ObsidianBorder)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = onRecover(recKeyInput, newPinInput)
                        if (ok) showRecoveryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Reset & Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
private fun VaultContentScreen(viewModel: PrivateSpaceViewModel) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    val vaultNotes by viewModel.vaultNotes.collectAsState()
    val recoveryKey by viewModel.recoveryKey.collectAsState()
    val activeNote by viewModel.activeDecryptedNote.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Gallery", "Documents", "Notes", "Security")

    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showRecoveryKeyDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vault Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Unlocked", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Personal Sanctuary", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Text("AES-256-GCM Hardware Encrypted", style = MaterialTheme.typography.labelSmall, color = GoldSecondary)
            }

            Button(
                onClick = { viewModel.lockVault() },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock Vault", fontSize = 12.sp)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) GoldPrimary else TextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> VaultGalleryTab(vaultItems.filter { it.category in listOf("PHOTO", "VIDEO") }, viewModel)
                1 -> VaultDocumentsTab(vaultItems.filter { it.category in listOf("DOCUMENT", "ARCHIVE", "OTHER") }, viewModel)
                2 -> VaultNotesTab(vaultNotes, viewModel, onCreateNote = { showCreateNoteDialog = true })
                3 -> VaultSecurityTab(recoveryKey, onShowRecovery = { showRecoveryKeyDialog = true })
            }
        }
    }

    // Dialogs
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = { Text("New Encrypted Note", color = GoldPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        placeholder = { Text("Note Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = ObsidianBorder)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        placeholder = { Text("Encrypted content...") },
                        modifier = Modifier.height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = ObsidianBorder)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() || noteBody.isNotBlank()) {
                            viewModel.createOrUpdateNote(title = noteTitle, content = noteBody)
                            showCreateNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    activeNote?.let { decNote ->
        AlertDialog(
            onDismissRequest = { viewModel.closeActiveNote() },
            title = { Text(decNote.entity.title, color = GoldPrimary) },
            text = {
                Column {
                    Text(text = decNote.decryptedContent, color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Updated: ${FileItem.formatDate(decNote.entity.updatedAt)}", color = TextTertiary, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.closeActiveNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deleteNote(decNote.entity) }) {
                    Text("Delete Note", color = CrimsonError)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showRecoveryKeyDialog) {
        val clipboard = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRecoveryKeyDialog = false },
            title = { Text("Master Recovery Key", color = GoldPrimary) },
            text = {
                Column {
                    Text("Write down or securely copy this key. It is your only way to regain access if you forget your PIN:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceElevated)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = recoveryKey,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(recoveryKey))
                        copied = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text(if (copied) "Copied!" else "Copy Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryKeyDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
private fun VaultGalleryTab(items: List<VaultItemEntity>, viewModel: PrivateSpaceViewModel) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Photo, contentDescription = "Gallery", tint = GoldPrimary, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Private Gallery is Empty", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Encrypt sensitive photos & videos to shield them from public scans.", color = TextTertiary, fontSize = 11.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = "Item", tint = GoldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.originalName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "${FileItem.formatBytes(item.sizeBytes)} • Encrypted AES-256", color = TextTertiary, fontSize = 10.sp)
                        }
                        IconButton(onClick = { viewModel.deleteVaultItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDocumentsTab(items: List<VaultItemEntity>, viewModel: PrivateSpaceViewModel) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Description, contentDescription = "Documents", tint = GoldPrimary, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Private Documents Sanctuary", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Store sensitive PDF contracts, financial records, and private archives.", color = TextTertiary, fontSize = 11.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Doc", tint = GoldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.originalName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "${FileItem.formatBytes(item.sizeBytes)} • Encrypted", color = TextTertiary, fontSize = 10.sp)
                        }
                        IconButton(onClick = { viewModel.deleteVaultItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultNotesTab(
    notes: List<VaultNoteEntity>,
    viewModel: PrivateSpaceViewModel,
    onCreateNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Encrypted Notes", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(
                onClick = onCreateNote,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No encrypted notes yet. Create your first secure note.", color = TextTertiary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openNote(note) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = note.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Updated: ${FileItem.formatDate(note.updatedAt)}", color = TextTertiary, fontSize = 10.sp)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultSecurityTab(
    recoveryKey: String,
    onShowRecovery: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CRYPTOGRAPHIC SECURITY AUDIT", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Cipher: AES-256-GCM (Galois/Counter Mode)", color = TextPrimary, fontSize = 13.sp)
                    Text("• Key Derivation: PBKDF2WithHmacSHA256 (65,536 iterations)", color = TextPrimary, fontSize = 13.sp)
                    Text("• Initialization Vector: 96-bit CSPRNG SecureRandom", color = TextPrimary, fontSize = 13.sp)
                    Text("• Authentication Tag: 128-bit authentication integrity", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowRecovery() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, contentDescription = "Key", tint = GoldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("View Recovery Key", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Key: ${recoveryKey.take(8)}...", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
