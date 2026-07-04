import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports if they don't exist
if "import androidx.compose.ui.platform.LocalHapticFeedback" not in content:
    content = content.replace("import androidx.compose.ui.platform.LocalContext", 
                              "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalHapticFeedback\nimport androidx.compose.ui.hapticfeedback.HapticFeedbackType")

# Add haptics to Switch in DeviceCard
content = re.sub(
    r'(Switch\(\s*checked = device\.status,\s*onCheckedChange = \{)',
    r'\1 \n                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)\n                       ',
    content
)

# We need to add `val haptic = LocalHapticFeedback.current` to DeviceCard
content = re.sub(
    r'(@Composable\s*fun DeviceCard[^{]*\{)',
    r'\1\n    val haptic = LocalHapticFeedback.current',
    content
)

# Add haptics to DashboardDeviceControls
content = re.sub(
    r'(@Composable\s*fun DashboardDeviceControls[^{]*\{)',
    r'\1\n    val haptic = LocalHapticFeedback.current',
    content
)

# Add haptics to NavItem
content = re.sub(
    r'(@Composable\s*fun RowScope.NavItem[^{]*\{)',
    r'\1\n    val haptic = LocalHapticFeedback.current',
    content
)

content = re.sub(
    r'(Modifier\s*\.clickable\s*\{\s*)(onClick\(\))(\s*\})',
    r'\1 haptic.performHapticFeedback(HapticFeedbackType.LongPress); \2 \3',
    content
)

# Also add haptics to the BottomNavBar clickable areas
content = re.sub(
    r'(@Composable\s*fun BottomNavBar[^{]*\{)',
    r'\1\n    val haptic = LocalHapticFeedback.current',
    content
)
# We already covered NavItem which handles the clicks for the bottom nav bar.

# Find the tween for AnimatedContent
content = re.sub(
    r'fadeIn\(animationSpec = tween\(220\)\) togetherWith\s*fadeOut\(animationSpec = tween\(220\)\)',
    r'fadeIn(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))',
    content
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

