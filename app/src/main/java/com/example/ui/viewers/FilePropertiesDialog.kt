package com.example.ui.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.security.CryptoEngine
import com.example.core.storage.FileItem
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FilePropertiesDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit
) {
    var sha256 by remember { mutableStateOf<String?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "File Properties",
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                PropertyRow(label = "Name", value = fileItem.name)
                PropertyRow(label = "Path", value = fileItem.path)
                PropertyRow(
                    label = "Size",
                    value = "${fileItem.formattedSize} (${fileItem.sizeBytes} bytes)"
                )
                PropertyRow(label = "Modified", value = fileItem.formattedDate)
                PropertyRow(label = "MIME Type", value = fileItem.mimeType)
                PropertyRow(
                    label = "Permissions",
                    value = "Read: ${fileItem.file.canRead()} • Write: ${fileItem.file.canWrite()}"
                )

                if (sha256 != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SHA-256 Checksum",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary
                    )
                    Text(
                        text = sha256 ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (sha256 == null && !fileItem.isDirectory) {
                        Button(
                            onClick = {
                                isCalculating = true
                                scope.launch {
                                    val hash = withContext(Dispatchers.IO) {
                                        CryptoEngine.computeHash(fileItem.file, "SHA-256")
                                    }
                                    sha256 = hash
                                    isCalculating = false
                                }
                            },
                            enabled = !isCalculating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isCalculating) "Calculating..." else "Compute Hash")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
