package com.example.nimons360.ui.family.detail.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    if (showSheet) {
        val sheetState = rememberModalBottomSheetState()
        var messageInput by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Send Message",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(15.dp))
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onSend(messageInput)
                            messageInput = ""
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}