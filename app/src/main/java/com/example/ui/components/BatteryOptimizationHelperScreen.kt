package com.example.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray

enum class OemBrand(
    val displayName: String,
    val skinName: String,
    val summary: String
) {
    SAMSUNG(
        displayName = "Samsung",
        skinName = "One UI",
        summary = "Samsung's Device Care puts unused apps to sleep aggressively. Whitelist Eltharion under Battery & Never Sleeping Apps."
    ),
    XIAOMI(
        displayName = "Xiaomi",
        skinName = "HyperOS / MIUI",
        summary = "Xiaomi, Redmi & POCO devices restrict background services unless Autostart, No Restrictions, and Recents Lock are enabled."
    ),
    OPPO_REALME(
        displayName = "OPPO / Realme",
        skinName = "ColorOS / Realme UI",
        summary = "ColorOS and OnePlus OxygenOS suspend WiFi polling unless Background Activity and Auto-Launch are allowed."
    ),
    VIVO(
        displayName = "Vivo / iQOO",
        skinName = "Funtouch / OriginOS",
        summary = "Vivo kills background services on screen lock unless High Background Power Usage is turned on for Eltharion."
    ),
    STOCK(
        displayName = "Pixel / Stock",
        skinName = "Android AOSP",
        summary = "Standard Android throttles background services under Adaptive Battery unless Eltharion is set to Unrestricted."
    )
}

data class OemBatteryStep(
    val id: String,
    val title: String,
    val menuPath: String,
    val detail: String,
    val actionLabel: String? = null,
    val actionType: OemStepAction = OemStepAction.NONE
)

enum class OemStepAction {
    NONE,
    OPEN_APP_INFO,
    OPEN_BATTERY_LIST,
    OPEN_OEM_AUTOSTART
}

object OemBatteryGuideCatalog {

    fun detectOemBrand(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty()
    ): OemBrand {
        val combined = "$manufacturer $brand".lowercase()
        return when {
            combined.contains("samsung") -> OemBrand.SAMSUNG
            combined.contains("xiaomi") ||
                combined.contains("redmi") ||
                combined.contains("poco") -> OemBrand.XIAOMI
            combined.contains("oppo") ||
                combined.contains("realme") ||
                combined.contains("oneplus") -> OemBrand.OPPO_REALME
            combined.contains("vivo") ||
                combined.contains("iqoo") -> OemBrand.VIVO
            else -> OemBrand.STOCK
        }
    }

    fun stepsFor(brand: OemBrand): List<OemBatteryStep> = when (brand) {
        OemBrand.SAMSUNG -> listOf(
            OemBatteryStep(
                id = "samsung_unrestricted",
                title = "Set Eltharion Battery to Unrestricted",
                menuPath = "App Info → Battery → Unrestricted",
                detail = "Allows 2-second RSSI polling even when your Galaxy screen is locked.",
                actionLabel = "Open App Info",
                actionType = OemStepAction.OPEN_APP_INFO
            ),
            OemBatteryStep(
                id = "samsung_never_sleeping",
                title = "Add to Never Auto-Sleeping Apps",
                menuPath = "Settings → Device Care → Battery → Background usage limits → Never auto sleeping apps",
                detail = "Tap + and select Eltharion so One UI never moves it into Deep Sleep.",
                actionLabel = "Battery Settings",
                actionType = OemStepAction.OPEN_BATTERY_LIST
            ),
            OemBatteryStep(
                id = "samsung_adaptive",
                title = "Keep Remove Permissions Disabled",
                menuPath = "App Info → Permissions → Pause app activity if unused (OFF)",
                detail = "Prevents One UI from revoking WiFi Location access after days of background use.",
                actionLabel = "App Permissions",
                actionType = OemStepAction.OPEN_APP_INFO
            )
        )

        OemBrand.XIAOMI -> listOf(
            OemBatteryStep(
                id = "xiaomi_autostart",
                title = "Enable Background Autostart",
                menuPath = "Security App → Manage Apps → Eltharion → Autostart (ON)",
                detail = "Required on MIUI & HyperOS so Eltharion's guard service can stay alive or restart after memory cleanup.",
                actionLabel = "Try Autostart",
                actionType = OemStepAction.OPEN_OEM_AUTOSTART
            ),
            OemBatteryStep(
                id = "xiaomi_no_restrictions",
                title = "Set Battery Saver to No Restrictions",
                menuPath = "App Info → Battery saver → No restrictions",
                detail = "Switch away from 'MIUI Battery Saver (Recommended)' which kills background WiFi checks within minutes.",
                actionLabel = "Open App Info",
                actionType = OemStepAction.OPEN_APP_INFO
            ),
            OemBatteryStep(
                id = "xiaomi_lock_recents",
                title = "Lock Eltharion in Recent Apps",
                menuPath = "Open Recents → Long-press Eltharion → Tap Padlock icon",
                detail = "Stops the Xiaomi speed booster / 'Clear all' button from terminating active WiFi protection."
            )
        )

        OemBrand.OPPO_REALME -> listOf(
            OemBatteryStep(
                id = "oppo_background",
                title = "Allow Background Activity",
                menuPath = "App Info → Battery usage → Allow background activity (ON)",
                detail = "Lets Eltharion continuously check LPB signal strength while walking away from the router.",
                actionLabel = "Open App Info",
                actionType = OemStepAction.OPEN_APP_INFO
            ),
            OemBatteryStep(
                id = "oppo_autolaunch",
                title = "Allow Auto-Launch",
                menuPath = "App Info → Battery usage → Allow auto-launch (ON)",
                detail = "Prevents ColorOS / Realme UI from blocking service restarts.",
                actionLabel = "Try Auto-Launch",
                actionType = OemStepAction.OPEN_OEM_AUTOSTART
            ),
            OemBatteryStep(
                id = "oppo_lock",
                title = "Lock App in Recent Tasks",
                menuPath = "Recents → Three dots on Eltharion → Lock",
                detail = "Keeps the foreground guard running even if you swipe clear recent apps."
            )
        )

        OemBrand.VIVO -> listOf(
            OemBatteryStep(
                id = "vivo_high_bg",
                title = "Allow High Background Power Usage",
                menuPath = "Settings → Battery → Background power consumption → Eltharion → High background power usage",
                detail = "Vivo blocks all background services by default unless High Background Power Usage is selected.",
                actionLabel = "Battery Settings",
                actionType = OemStepAction.OPEN_BATTERY_LIST
            ),
            OemBatteryStep(
                id = "vivo_autostart",
                title = "Enable Autostart in iManager",
                menuPath = "iManager → App Manager → Autostart → Eltharion (ON)",
                detail = "Ensures Eltharion starts smoothly from Quick Settings or after reboots.",
                actionLabel = "Open App Info",
                actionType = OemStepAction.OPEN_APP_INFO
            )
        )

        OemBrand.STOCK -> listOf(
            OemBatteryStep(
                id = "stock_unrestricted",
                title = "Set App Battery Usage to Unrestricted",
                menuPath = "App Info → App battery usage → Allow background usage → Unrestricted",
                detail = "Disables Doze throttling for Eltharion so Auto-Pause fires within 4 seconds of walking away.",
                actionLabel = "Open App Info",
                actionType = OemStepAction.OPEN_APP_INFO
            ),
            OemBatteryStep(
                id = "stock_battery_list",
                title = "Verify All-Apps Battery Optimization List",
                menuPath = "Settings → Battery → Battery optimization → All apps → Eltharion → Don't optimize",
                detail = "Confirms system-wide exemptions match Unrestricted priority.",
                actionLabel = "Battery List",
                actionType = OemStepAction.OPEN_BATTERY_LIST
            )
        )
    }
}

@Composable
fun BatteryOptimizationHelperScreen(
    isBatteryOptimizationIgnored: Boolean,
    hasNotificationPermission: Boolean,
    hasLocationPermission: Boolean,
    onRefreshStatus: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenOemAutostart: () -> Unit,
    onSendGuideNotification: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val detectedBrand = remember { OemBatteryGuideCatalog.detectOemBrand() }
    var selectedBrandName by rememberSaveable { mutableStateOf(detectedBrand.name) }
    val selectedBrand = OemBrand.entries.firstOrNull { it.name == selectedBrandName } ?: detectedBrand
    val checkedSteps = remember { mutableStateMapOf<String, Boolean>() }

    val steps = remember(selectedBrand) { OemBatteryGuideCatalog.stepsFor(selectedBrand) }
    val completedCount = steps.count { checkedSteps[it.id] == true }
    val progressFraction = if (steps.isNotEmpty()) {
        completedCount.toFloat() / steps.size.toFloat()
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auroraDarkBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("battery_helper_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            // Top bar with Back arrow & live status refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("battery_helper_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.battery_helper_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Device detected: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} (${detectedBrand.skinName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshStatus,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(SlateCardElevated, CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                        .testTag("battery_helper_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-check Battery Status",
                        tint = PureWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            HorizontalDivider(color = SlateBorder, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live System Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCardElevated)
                        .border(
                            1.dp,
                            if (isBatteryOptimizationIgnored) PureWhite.copy(alpha = 0.4f) else AmberCaution.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SlateCard)
                                    .border(1.dp, SlateBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isBatteryOptimizationIgnored) PureWhite else AmberCaution,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Current Battery Priority",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isBatteryOptimizationIgnored) {
                                        "System optimization disabled"
                                    } else {
                                        "Android can throttle background checks"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isBatteryOptimizationIgnored) PureWhite else AmberCaution.copy(alpha = 0.16f))
                                .border(
                                    1.dp,
                                    if (isBatteryOptimizationIgnored) PureWhite else AmberCaution,
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isBatteryOptimizationIgnored) {
                                    stringResource(R.string.bg_priority_status_unrestricted)
                                } else {
                                    stringResource(R.string.bg_priority_status_optimized)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBatteryOptimizationIgnored) AbyssBlack else AmberCaution,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("battery_helper_status_badge")
                            )
                        }
                    }

                    HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f), thickness = 1.dp)

                    ReadinessMiniRow("Battery Optimization Disabled", isBatteryOptimizationIgnored)
                    ReadinessMiniRow("Foreground Service Notification", hasNotificationPermission)
                    ReadinessMiniRow("WiFi Location Telemetry", hasLocationPermission)
                }

                // OEM Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Select Phone Brand Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OemBrand.entries.forEach { brand ->
                            val selected = brand == selectedBrand
                            Column(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) PureWhite else SlateCardElevated)
                                    .border(1.dp, if (selected) PureWhite else SlateBorder, RoundedCornerShape(999.dp))
                                    .clickable { selectedBrandName = brand.name }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("oem_chip_${brand.name.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (brand == detectedBrand) "${brand.displayName} (You)" else brand.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) AbyssBlack else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Interactive OEM Guide Card
                AnimatedContent(
                    targetState = selectedBrand,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "oem_guide_content"
                ) { brand ->
                    val brandSteps = OemBatteryGuideCatalog.stepsFor(brand)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SlateCard)
                                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${brand.displayName} • ${brand.skinName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$completedCount/${brandSteps.size} done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (completedCount == brandSteps.size) PureWhite else TextMuted,
                                    modifier = Modifier.testTag("battery_helper_step_progress")
                                )
                            }

                            Text(
                                text = brand.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = PureWhite,
                                trackColor = TrackGray
                            )
                        }

                        brandSteps.forEachIndexed { index, step ->
                            val isChecked = checkedSteps[step.id] == true
                            OemInteractiveStepCard(
                                stepNumber = index + 1,
                                step = step,
                                checked = isChecked,
                                onToggleChecked = { checkedSteps[step.id] = !isChecked },
                                onTriggerAction = {
                                    when (step.actionType) {
                                        OemStepAction.OPEN_APP_INFO -> onOpenAppSettings()
                                        OemStepAction.OPEN_BATTERY_LIST -> onOpenBatterySettings()
                                        OemStepAction.OPEN_OEM_AUTOSTART -> onOpenOemAutostart()
                                        OemStepAction.NONE -> Unit
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom Direct Shortcuts
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenBatterySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = AbyssBlack
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("battery_helper_open_battery_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.bg_priority_open_battery),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("battery_helper_open_app_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.bg_priority_open_app_info),
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onSendGuideNotification,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("battery_helper_send_notif_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.bg_priority_send_notif),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ReadinessMiniRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (passed) PureWhite else SlateCard)
                .border(1.dp, if (passed) PureWhite else AmberCaution.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (passed) Icons.Default.Check else Icons.Default.WarningAmber,
                contentDescription = if (passed) "Ready" else "Action needed",
                tint = if (passed) AbyssBlack else AmberCaution,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (passed) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun OemInteractiveStepCard(
    stepNumber: Int,
    step: OemBatteryStep,
    checked: Boolean,
    onToggleChecked: () -> Unit,
    onTriggerAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) SlateCard else SlateCardElevated)
            .border(
                1.dp,
                if (checked) PureWhite.copy(alpha = 0.45f) else SlateBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggleChecked)
            .padding(14.dp)
            .testTag("oem_step_${step.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (checked) PureWhite else SlateCard)
                    .border(1.dp, if (checked) PureWhite else SlateBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Step completed",
                        tint = AbyssBlack,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = if (checked) TextSecondary else TextPrimary
                )
                Text(
                    text = step.menuPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.8f)
                )
            }
        }

        Text(
            text = step.detail,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (checked) "Marked done • tap to undo" else "Tap card to mark done",
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) PureWhite else TextMuted
            )

            if (step.actionLabel != null && step.actionType != OemStepAction.NONE) {
                OutlinedButton(
                    onClick = onTriggerAction,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("oem_step_action_${step.id}")
                ) {
                    Text(
                        text = step.actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite
                    )
                }
            }
        }
    }
}
