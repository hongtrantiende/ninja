# build_share.ps1 — Build ban mod share (An toan bo tinh nang San Boss)
$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Magenta
Write-Host " BAT DAU BUILD BAN SHARE (AN SAN BOSS)" -ForegroundColor Magenta
Write-Host "=========================================" -ForegroundColor Magenta

# 1. Bat flag HIDE_BOSS_FEATURES trong NamMod.java (dung python tranh loi UTF8 BOM)
Write-Host "[1/5] Kich hoat HIDE_BOSS_FEATURES trong NamMod.java..." -ForegroundColor Cyan
python -c "
with open('src/NamMod.java', 'r', encoding='utf-8') as f:
    s = f.read()
s = s.replace('HIDE_BOSS_FEATURES = false;', 'HIDE_BOSS_FEATURES = true;')
with open('src/NamMod.java', 'w', encoding='utf-8') as f:
    f.write(s)
"

try {
    # 2. Chay build pipeline
    Write-Host "[2/5] Compiling & Packaging..." -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File build.ps1

    # 3. Tao file NinjaShare.jar va copy ra Downloads
    Write-Host "[3/5] Tao NinjaShare.jar..." -ForegroundColor Cyan
    Copy-Item NinjaNamod.jar -Destination "NinjaShare.jar" -Force
    Copy-Item "NinjaShare.jar" -Destination "$env:USERPROFILE\Downloads\NinjaShare.jar" -Force
    Copy-Item "NinjaShare.jar" -Destination "$env:USERPROFILE\Downloads\NinjaNamod_Share.jar" -Force

    Write-Host ">> Da luu ban Share tai: $env:USERPROFILE\Downloads\NinjaShare.jar" -ForegroundColor Green
} finally {
    # 4. Khoi phuc NamMod.java ve ban full (HIDE_BOSS_FEATURES = false)
    Write-Host "[4/5] Khoi phuc NamMod.java ve ban full..." -ForegroundColor Cyan
    python -c "
with open('src/NamMod.java', 'r', encoding='utf-8') as f:
    s = f.read()
s = s.replace('HIDE_BOSS_FEATURES = true;', 'HIDE_BOSS_FEATURES = false;')
with open('src/NamMod.java', 'w', encoding='utf-8') as f:
    f.write(s)
"

    # 5. Build lai ban goc NinjaNamod.jar
    Write-Host "[5/5] Re-building ban full cho NinjaNamod.jar..." -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File build.ps1
}

Write-Host "=========================================" -ForegroundColor Green
Write-Host " HOAN TAT! BAN SHARE VA BAN FULL SAN SANG" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
