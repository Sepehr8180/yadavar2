package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AiQuickAddBar(
    isAiProcessing: Boolean,
    onAiSubmit: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "سرویس تشخیص صوت روی این دستگاه در دسترس نیست", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "بگویید: مثلاً فردا ساعت ۹ جلسه با تیم #کار")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognized = matches[0]
                    inputText = recognized
                    onAiSubmit(recognized)
                    inputText = ""
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "خطا در شروع ضبط صدا", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_quick_add_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "در حال شنیدن صدای شما..." else "فرمان صوتی یا متنی: فردا ساعت ۹ جلسه #کار !!!",
                            fontSize = 13.sp,
                            color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputText.isNotBlank()) {
                            onAiSubmit(inputText)
                            inputText = ""
                        }
                    }),
                    trailingIcon = {
                        // AI button
                        IconButton(onClick = onOpenAiSettings) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "تنظیمات هوش مصنوعی",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Mic Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(micPulse)
                                .border(2.dp, Color(0xFFD32F2F), CircleShape)
                        )
                    }

                    Surface(
                        color = if (isListening) Color(0xFFD32F2F) else MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                if (isListening) stopListening() else startListening()
                            }
                            .testTag("voice_mic_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "فرمان صوتی",
                                tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Submit Button
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(enabled = !isAiProcessing && inputText.isNotBlank()) {
                            onAiSubmit(inputText)
                            inputText = ""
                        }
                        .testTag("quick_add_submit_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAiProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزودن هوشمند",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isListening) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ لطفاً صحبت کنید (به عنوان، ساعت، روز و اولویت اشاره کنید...)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
