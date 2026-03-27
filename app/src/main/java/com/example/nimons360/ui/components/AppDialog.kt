package com.example.nimons360.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        icon = icon,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton ?: {},
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
fun AppDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    AppDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title?.let { t -> { 
                Text(t) 
            } 
        },
        text = message?.let { m -> {
                Text(m) 
            } 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let { label ->
            {
                TextButton(
                    onClick = {
                        onDismiss?.invoke()
                        onDismissRequest()
                    },
                ) {
                    Text(label)
                }
            }
        },
    )
}
