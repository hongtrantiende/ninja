# build.ps1 — Standard JAR Mod Build Script
$ErrorActionPreference = "Stop"

Write-Host "[1/6] Restoring base JAR from git..." -ForegroundColor Cyan
git checkout NinjaNamod.jar

Write-Host "[2/6] Unpacking & cleaning old mod classes..." -ForegroundColor Cyan
Remove-Item -Recurse -Force build/unpacked -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build/unpacked | Out-Null
Push-Location build/unpacked
jar xf ../../NinjaNamod.jar
Pop-Location

Get-ChildItem src/*.java | ForEach-Object {
    $base = $_.BaseName
    Remove-Item -Force "build/unpacked/$base.class" -ErrorAction SilentlyContinue
    Remove-Item -Force "build/unpacked/$base`$*.class" -ErrorAction SilentlyContinue
}

Write-Host "[3/6] Compiling mod sources..." -ForegroundColor Cyan
javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/*.java

Write-Host "[4/6] Applying bytecode patches..." -ForegroundColor Cyan
python scripts/patch_class_j2me.py build/unpacked/

$env:PYTHONIOENCODING = "utf-8"
python scripts/patch_gamescr_hienexp.py build/unpacked/GameScr.class
python scripts/fix_gamescr_thongke.py build/unpacked/GameScr.class
python scripts/patch_effectauto.py build/unpacked/EffectAuto.class
python scripts/patch_hsluong_pos.py build/unpacked/GameScr.class

# Restore ChatManager.class from ban goc.jar
$restorePy = @"
import zipfile
z = zipfile.ZipFile('ban goc.jar', 'r')
with open('build/unpacked/ChatManager.class', 'wb') as f:
    f.write(z.read('ChatManager.class'))
z.close()
"@
python -c $restorePy

Write-Host "[5/6] Packaging JAR..." -ForegroundColor Cyan
Push-Location build/unpacked
Remove-Item -Recurse -Force javax -ErrorAction SilentlyContinue
Remove-Item -Force Char.class.bak_effects -ErrorAction SilentlyContinue
Pop-Location

git checkout NinjaNamod.jar
$modClasses = Get-ChildItem build/unpacked/*.class | ForEach-Object { $_.Name }
Push-Location build/unpacked
jar uf ../../NinjaNamod.jar $modClasses
Pop-Location

Write-Host "[6/6] Copying to Downloads..." -ForegroundColor Cyan
Copy-Item NinjaNamod.jar -Destination "$env:USERPROFILE\Downloads\NinjaNamod.jar" -Force

Write-Host "=========================================" -ForegroundColor Green
Write-Host " BUILD SUCCESSFUL! JAR READY IN DOWNLOADS" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
