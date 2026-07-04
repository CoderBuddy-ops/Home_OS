import re
import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = re.sub(r'Divider\(color = BorderWhite, thickness = 1\.dp\)', 'HorizontalDivider(color = BorderWhite, thickness = 1.dp)', content)
content = re.sub(r'import androidx\.compose\.material3\.Divider', 'import androidx.compose.material3.HorizontalDivider', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

