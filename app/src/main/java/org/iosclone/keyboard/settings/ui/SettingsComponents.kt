package org.iosclone.keyboard.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IOSSectionHeader(title: String) {
    val dark = isSystemInDarkTheme()
    val textColor = if (dark) IOSDarkTextSecondary else IOSLightTextSecondary

    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp, end = 16.dp)
    )
}

@Composable
fun IOSGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    val dark = isSystemInDarkTheme()
    val bgColor = if (dark) IOSDarkCardBackground else IOSLightCardBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        content = content
    )
}

@Composable
fun IOSSettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val primaryText = if (dark) IOSDarkText else IOSLightText
    val secondaryText = if (dark) IOSDarkTextSecondary else IOSLightTextSecondary
    val separatorColor = if (dark) IOSDarkSeparator else IOSLightSeparator

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, color = primaryText)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = IOSGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = if (dark) Color(0xFF39393D) else Color(0xFFE9E9EA),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )
        }
    }
}

@Composable
fun IOSSettingsSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String = "${value.toInt()}%",
    showDivider: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val primaryText = if (dark) IOSDarkText else IOSLightText
    val secondaryText = if (dark) IOSDarkTextSecondary else IOSLightTextSecondary
    val separatorColor = if (dark) IOSDarkSeparator else IOSLightSeparator

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 16.sp, color = primaryText)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = valueDisplay, fontSize = 14.sp, color = secondaryText)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = if (dark) IOSBlueDark else IOSBlue,
                    inactiveTrackColor = if (dark) Color(0xFF39393D) else Color(0xFFE9E9EA)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )
        }
    }
}

@Composable
fun IOSSettingsActionRow(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    titleColor: Color? = null,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    showDivider: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val primaryText = if (dark) IOSDarkText else IOSLightText
    val secondaryText = if (dark) IOSDarkTextSecondary else IOSLightTextSecondary
    val separatorColor = if (dark) IOSDarkSeparator else IOSLightSeparator

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, color = titleColor ?: primaryText)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 15.sp,
                    color = secondaryText,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )
        }
    }
}
