package com.example.application.presentation.main.components

import android.util.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.text.isDigitsOnly

@Composable
fun CustomDialog(
    labelSize: Size,
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (Size) -> Unit
) {
    var width by remember { mutableStateOf(labelSize.width.toString()) }
    var height by remember { mutableStateOf(labelSize.height.toString()) }

    var errorWidthState by remember { mutableStateOf(false) }
    var errorWidthMessage by remember { mutableStateOf("") }
    var errorHeightState by remember { mutableStateOf(false) }
    var errorHeightMessage by remember { mutableStateOf("") }


    Dialog(onDismissRequest = onDismiss) {

        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {

            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = "Укажите ширину и высоту этикетки",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextField(
                        modifier = Modifier.weight(1F),
                        value = width,
                        onValueChange = {
                            width = it
                            when {
                                width.isBlank() -> {
                                    errorWidthState = true
                                    errorWidthMessage = "Ширина не может быть пустой"
                                }
                                !width.isDigitsOnly() -> {
                                    errorWidthState = true
                                    errorWidthMessage = "Ширина должна быть числом"
                                }
                                else -> {
                                    errorWidthState = false
                                    errorWidthMessage = ""
                                }
                            }
                        },
                        label =
                        {
                            Text(
                                text = if (errorWidthState) errorWidthMessage
                                else "Ширина"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        modifier = Modifier.weight(1F),
                        value = height,
                        onValueChange = {
                            height = it
                            when {
                                height.isBlank() -> {
                                    errorHeightState = true
                                    errorHeightMessage = "Ширина не может быть пустой"
                                }
                                !height.isDigitsOnly() -> {
                                    errorHeightState = true
                                    errorHeightMessage = "Ширина должна быть числом"
                                }
                                else -> {
                                    errorHeightState = false
                                    errorHeightMessage = ""
                                }
                            }
                        },
                        label =
                        {
                            Text(
                                text = if (errorHeightState) errorHeightMessage
                                else "Высота"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onNegativeClick) {
                        Text(text = "ОТМЕНИТЬ")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = {
                        if (!errorWidthState || !errorHeightState)
                            onPositiveClick(Size(width.toInt(), height.toInt()))
                    }) {
                        Text(text = "ПРИНЯТЬ")
                    }
                }
            }
        }
    }
}