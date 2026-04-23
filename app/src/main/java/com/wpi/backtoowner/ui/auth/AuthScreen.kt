package com.wpi.backtoowner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpi.backtoowner.ui.theme.WpiCrimson
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiOnCrimson

private val WpiEmailPattern = Regex("""^[a-zA-Z0-9._%+-]+@wpi\.edu$""", RegexOption.IGNORE_CASE)

@Composable
fun AuthScreen(
    sessionViewModel: SessionViewModel,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WpiHeaderMaroon,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("WPI", color = WpiHeaderMaroon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = " | ",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Text(
                        text = "BackToOwner",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Get Started",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF0F0F0),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AuthTabButton(
                    text = "Login",
                    selected = tab == 0,
                    onClick = { tab = 0; error = null },
                    modifier = Modifier.weight(1f),
                )
                AuthTabButton(
                    text = "Sign Up",
                    selected = tab == 1,
                    onClick = { tab = 1; error = null },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = "Find what you lost. Return what you found.",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tab == 1) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (tab == 1) "WPI Email" else "Email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
            )
            if (tab == 1) {
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    error = null
                    if (!WpiEmailPattern.matches(email.trim())) {
                        error = "Email must be a valid @wpi.edu address."
                        return@Button
                    }
                    if (tab == 1) {
                        if (name.isBlank()) {
                            error = "Enter your full name."
                            return@Button
                        }
                        if (password != confirm) {
                            error = "Passwords do not match."
                            return@Button
                        }
                    }
                    busy = true
                    if (tab == 0) {
                        sessionViewModel.login(email.trim(), password) { result ->
                            busy = false
                            error = result.exceptionOrNull()?.message
                        }
                    } else {
                        sessionViewModel.signup(name.trim(), email.trim(), password) { result ->
                            busy = false
                            error = result.exceptionOrNull()?.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WpiHeaderMaroon,
                    contentColor = WpiOnCrimson,
                    disabledContainerColor = WpiHeaderMaroon.copy(alpha = 0.45f),
                    disabledContentColor = WpiOnCrimson.copy(alpha = 0.75f),
                ),
            ) {
                Text(if (tab == 0) "Login" else "Sign Up", fontWeight = FontWeight.SemiBold)
            }

            if (tab == 0) {
                TextButton(
                    onClick = { error = "Use Appwrite console or password recovery if enabled for your project." },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Forgot Password?", color = WpiCrimson)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Secure access via Appwrite Auth.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = if (selected) WpiHeaderMaroon else Color(0xFFE0E0E0),
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                color = if (selected) WpiOnCrimson else Color(0xFF555555),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
