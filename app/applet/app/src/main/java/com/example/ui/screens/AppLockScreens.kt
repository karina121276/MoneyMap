package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    onPinSetupComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var createdPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set App Lock PIN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pin_setup_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (step == 1) "Create 6-Digit PIN" else "Confirm PIN",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (step == 1) "Keep your financial information private." else "Enter the same PIN again to finish setup.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                val currentLen = if (step == 1) createdPin.length else confirmPin.length
                PinIndicatorRow(pinLength = currentLen)

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = MoneyMapRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NumericKeypad(
                onDigitClick = { digit ->
                    if (step == 1) {
                        if (createdPin.length < 6) {
                            createdPin += digit
                            errorMessage = null
                            if (createdPin.length == 6) {
                                step = 2
                                confirmPin = ""
                            }
                        }
                    } else {
                        if (confirmPin.length < 6) {
                            confirmPin += digit
                            errorMessage = null
                            if (confirmPin.length == 6) {
                                val fullConfirm = confirmPin
                                if (fullConfirm == createdPin) {
                                    onPinSetupComplete(createdPin)
                                } else {
                                    errorMessage = "PINs do not match. Try again."
                                    confirmPin = ""
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (step == 1) {
                        if (createdPin.isNotEmpty()) createdPin = createdPin.dropLast(1)
                    } else {
                        if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                    }
                    errorMessage = null
                },
                onClearClick = {
                    if (step == 1) {
                        createdPin = ""
                    } else {
                        confirmPin = ""
                    }
                    errorMessage = null
                }
            )

            Spacer(modifier = Modifier.height(52.dp))
        }
    }
}

@Composable
fun ChangePinScreen(
    onVerifyCurrentPin: suspend (String) -> Boolean,
    onChangePinComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stage by remember { mutableIntStateOf(1) } // 1: enter current PIN, 2: enter new PIN, 3: confirm new PIN
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val titleText = when (stage) {
        1 -> "Enter Current PIN"
        2 -> "Enter New PIN"
        else -> "Confirm New PIN"
    }

    val subtitleText = when (stage) {
        1 -> "Verify your existing 6-digit app lock PIN"
        2 -> "Choose a secure 6-digit new PIN"
        else -> "Re-enter your new 6-digit PIN to confirm"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change PIN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("change_pin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(titleText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(subtitleText, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                val currentLen = when (stage) {
                    1 -> currentPinInput.length
                    2 -> newPinInput.length
                    else -> confirmPinInput.length
                }
                PinIndicatorRow(pinLength = currentLen)

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = MoneyMapRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NumericKeypad(
                onDigitClick = { digit ->
                    when (stage) {
                        1 -> {
                            if (currentPinInput.length < 6 && !isVerifying) {
                                currentPinInput += digit
                                errorMessage = null
                                if (currentPinInput.length == 6) {
                                    isVerifying = true
                                    val candidate = currentPinInput
                                    coroutineScope.launch {
                                        val ok = onVerifyCurrentPin(candidate)
                                        isVerifying = false
                                        if (ok) {
                                            stage = 2
                                            errorMessage = null
                                        } else {
                                            errorMessage = "Incorrect current PIN. Try again."
                                            currentPinInput = ""
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (newPinInput.length < 6) {
                                newPinInput += digit
                                errorMessage = null
                            }
                        }
                        3 -> {
                            if (confirmPinInput.length < 6) {
                                confirmPinInput += digit
                                errorMessage = null
                                if (confirmPinInput.length == 6) {
                                    if (confirmPinInput == newPinInput) {
                                        onChangePinComplete(newPinInput)
                                    } else {
                                        errorMessage = "PINs do not match. Try again."
                                        confirmPinInput = ""
                                    }
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    when (stage) {
                        1 -> if (currentPinInput.isNotEmpty()) currentPinInput = currentPinInput.dropLast(1)
                        2 -> if (newPinInput.isNotEmpty()) newPinInput = newPinInput.dropLast(1)
                        3 -> if (confirmPinInput.isNotEmpty()) confirmPinInput = confirmPinInput.dropLast(1)
                    }
                    errorMessage = null
                },
                onClearClick = {
                    when (stage) {
                        1 -> currentPinInput = ""
                        2 -> newPinInput = ""
                        3 -> confirmPinInput = ""
                    }
                    errorMessage = null
                }
            )

            if (stage == 2) {
                Button(
                    onClick = {
                        if (newPinInput.length == 6) {
                            stage = 3
                            confirmPinInput = ""
                            errorMessage = null
                        }
                    },
                    enabled = newPinInput.length == 6,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyMapNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(52.dp))
            }
        }
    }
}

@Composable
fun AppUnlockScreen(
    biometricsEnabled: Boolean,
    deviceCredentialEnabled: Boolean,
    onVerifyPin: suspend (String) -> Boolean,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler {
        activity?.finish()
    }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(48.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MoneyMapNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MoneyMapNavy,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MoneyMap is Locked",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your 6-digit PIN to unlock",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                PinIndicatorRow(pinLength = enteredPin.length)

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = MoneyMapRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NumericKeypad(
                onDigitClick = { digit ->
                    if (enteredPin.length < 6 && !isVerifying) {
                        enteredPin += digit
                        errorMessage = null
                        if (enteredPin.length == 6) {
                            isVerifying = true
                            val candidate = enteredPin
                            coroutineScope.launch {
                                val isValid = onVerifyPin(candidate)
                                isVerifying = false
                                if (isValid) {
                                    onUnlockSuccess()
                                } else {
                                    errorMessage = "Incorrect PIN. Try again."
                                    enteredPin = ""
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                    errorMessage = null
                },
                onClearClick = {
                    enteredPin = ""
                    errorMessage = null
                }
            )
        }
    }
}

/** * Reusable 6-digit PIN indicator row */
@Composable
fun PinIndicatorRow(pinLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val isFilled = i < pinLength
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isFilled) MoneyMapNavy else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isFilled) 0.dp else 1.5.dp,
                        color = if (isFilled) Color.Transparent else BorderSubtle,
                        shape = CircleShape
                    )
            )
        }
    }
}

/** * Reusable custom Numeric Keypad */
@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "BACK")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        keys.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { key ->
                    Surface(
                        onClick = {
                            when (key) {
                                "BACK" -> onBackspaceClick()
                                "C" -> onClearClick()
                                else -> onDigitClick(key)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("keypad_key_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (key) {
                                "BACK" -> Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MoneyMapNavy,
                                    modifier = Modifier.size(22.dp)
                                )
                                "C" -> Text(
                                    text = "CLR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyMapRed
                                )
                                else -> Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
