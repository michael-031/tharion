package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionStatus
import com.example.data.SimulationConfig
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray
import kotlin.math.roundToInt

@Composable
fun SimulationBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .testTag("simulation_mode_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = "Simulation Mode Active",
                tint = AbyssBlack,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = "PRACTICE MODE • TESTING WITH PRETEND WIFI",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                color = AbyssBlack,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SimulationLabCard(
    simulation: SimulationConfig,
    sessionStatus: SessionStatus,
    pauseThresholdDbm: Int,
    onToggleSimulation: (Boolean) -> Unit,
    onSetSimulatedRssi: (Int) -> Unit,
    onToggleSimulateUnreachable: (Boolean) -> Unit = {},
    onFastForwardExpiry: () -> Unit
) {
    GradientGlassCard(
        topColor = SlateCard,
        bottomColor = SlateCard,
        borderBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(SlateBorder, SlateBorder)
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = PureWhite
                    )
                    Column {
                        Text(
                            text = "Practice Mode (No Router Needed)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Test how auto-pause works with a pretend WiFi slider",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = simulation.enabled,
                    onCheckedChange = onToggleSimulation,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AbyssBlack,
                        checkedTrackColor = PureWhite,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SlateCardElevated
                    ),
                    modifier = Modifier.testTag("simulation_mode_switch")
                )
            }

            AnimatedVisibility(visible = simulation.enabled) {
                val simPct = dbmToSignalPercent(simulation.rssiDbm)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pretend WiFi Strength",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$simPct% (${simulation.rssiDbm} dBm)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (simulation.rssiDbm <= pauseThresholdDbm) RoseDanger else PureWhite
                        )
                    }

                    Slider(
                        value = simulation.rssiDbm.toFloat(),
                        onValueChange = { onSetSimulatedRssi(it.roundToInt()) },
                        valueRange = -95f..-40f,
                        colors = SliderDefaults.colors(
                            thumbColor = PureWhite,
                            activeTrackColor = PureWhite,
                            inactiveTrackColor = TrackGray
                        ),
                        modifier = Modifier.testTag("simulated_rssi_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSetSimulatedRssi(-55) },
                            border = BorderStroke(1.dp, SlateBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sim_preset_strong")
                        ) {
                            Text("Near", style = MaterialTheme.typography.labelSmall, color = PureWhite)
                        }

                        OutlinedButton(
                            onClick = { onSetSimulatedRssi(pauseThresholdDbm - 3) },
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sim_preset_weak")
                        ) {
                            Text("Walk Far", style = MaterialTheme.typography.labelSmall, color = RoseDanger)
                        }

                        OutlinedButton(
                            onClick = { onSetSimulatedRssi(-62) },
                            border = BorderStroke(1.dp, SlateBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sim_preset_return")
                        ) {
                            Text("Come Back", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    if (sessionStatus == SessionStatus.PAUSED) {
                        OutlinedButton(
                            onClick = onFastForwardExpiry,
                            border = BorderStroke(1.dp, AmberCaution.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AmberCaution.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("sim_fast_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = AmberCaution,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test 5-Min Expiry Alert",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberCaution
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pretend 10.0.0.1 Unreachable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Test Smart Retry backoff (4s → 8s → 16s...)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = simulation.simulateUnreachablePortal,
                            onCheckedChange = onToggleSimulateUnreachable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AbyssBlack,
                                checkedTrackColor = AmberCaution,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateCardElevated
                            ),
                            modifier = Modifier.testTag("sim_unreachable_portal_switch")
                        )
                    }
                }
            }
        }
    }
}
