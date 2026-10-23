package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.Screen
import com.example.security.BiometricHelper
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

/**
 * STEP 6: Fingerprint Setup
 * → Title: "Secure your app"
 * → Message: "Add fingerprint for quick unlock"
 * → Button: [Setup Fingerprint]
 * → Button: [Skip for now]
 * → Save: biometric_enabled (true/false)
 *
 * STEP 7: Home Screen
 * → User is logged in
 * → App starts normally
 */
@Composable
fun FingerprintSetupScreen(
    viewModel: ThiefHunterViewModel,
    onboardingViewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scrollState = rememberScrollState()

    val isBiometricAvailable = remember(context) {
        BiometricHelper.checkIfBiometricAvailable(context)
    }

    fun finishAndGoHome(enableBio: Boolean) {
        onboardingViewModel.setBiometricChoice(enableBio, context)
        onboardingViewModel.completeOnboarding(context)
        viewModel.navigateTo(Screen.HOME)
    }

    fun handleSetupFingerprint() {
        if (!isBiometricAvailable) {
            Toast.makeText(
                context,
                "⚠️ Biometric hardware unavailable or not enrolled. Enabling setting anyway.",
                Toast.LENGTH_LONG
            ).show()
            finishAndGoHome(enableBio = true)
            return
        }

        if (activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Link Fingerprint",
                subtitle = "Touch sensor to activate quick biometric unlock"
            ) { success ->
                if (success) {
                    Toast.makeText(context, "✓ Fingerprint linked successfully!", Toast.LENGTH_SHORT).show()
                    finishAndGoHome(enableBio = true)
                } else {
                    Toast.makeText(context, "Verification cancelled. You can retry or skip.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            finishAndGoHome(enableBio = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NavyDark,
                        NavyPrimary,
                        Color(0xFF070B1F)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fingerprint Icon Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(3.dp, SafeGreen.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Secure your app",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add fingerprint for quick unlock",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = YellowAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Step 5 of 5 • Instantly unlock the app on opening and silence perimeter alarms without typing passwords.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Action Buttons Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = { handleSetupFingerprint() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_setup_fingerprint")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Setup Fingerprint",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { finishAndGoHome(enableBio = false) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_skip_fingerprint")
                    ) {
                        Text(
                            text = "Skip for now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = YellowAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "You can always change your biometric preference later in Settings.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
