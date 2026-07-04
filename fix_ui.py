import re
import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Make corners rounder
content = re.sub(r'RoundedCornerShape\(12\.dp\)', 'RoundedCornerShape(16.dp)', content)
content = re.sub(r'RoundedCornerShape\(16\.dp\)', 'RoundedCornerShape(24.dp)', content)
content = re.sub(r'RoundedCornerShape\(8\.dp\)', 'RoundedCornerShape(12.dp)', content)
content = re.sub(r'RoundedCornerShape\(4\.dp\)', 'RoundedCornerShape(8.dp)', content)

# Improve padding
content = re.sub(r'padding\(horizontal = 14\.dp', 'padding(horizontal = 20.dp', content)
content = re.sub(r'padding\(horizontal = 16\.dp', 'padding(horizontal = 24.dp', content)
content = re.sub(r'padding\(24\.dp\)', 'padding(32.dp)', content)
content = re.sub(r'padding\(16\.dp\)', 'padding(24.dp)', content)
content = re.sub(r'padding\(12\.dp\)', 'padding(16.dp)', content)

# Better text colors
content = re.sub(r'TextMuted', 'TextSecondary', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

