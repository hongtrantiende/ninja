import os
import pathlib
import zipfile

root = pathlib.Path(__file__).resolve().parent.parent
original = root / "Aeharuna_148TB2.jar"
patched = root / "build" / "tb2_step1_patched"
classes = root / "build" / "tb2_step1_classes"
output = root / "Aeharuna_148TB2_NamMod.jar"
temporary = root / "Aeharuna_148TB2_NamMod.jar.tmp"

overrides = {
    "Class_fi.class": patched / "Class_fi.class",
    "Class_ds.class": patched / "Class_ds.class",
    "Class_am.class": patched / "Class_am.class",
    "Class_er.class": patched / "Class_er.class",
}
new_classes = {
    "NamModMenu.class": classes / "NamModMenu.class",
    "TB2TachDoLe.class": classes / "TB2TachDoLe.class",
    "TB2TachDoLe$1.class": classes / "TB2TachDoLe$1.class",
    "TB2AutoGaoDa.class": classes / "TB2AutoGaoDa.class",
    "TB2AutoDoiDiem.class": classes / "TB2AutoDoiDiem.class",
    "TB2EventCommands.class": classes / "TB2EventCommands.class",
    "TB2AutoPickup.class": classes / "TB2AutoPickup.class",
    "TB2ThongTinBoss.class": classes / "TB2ThongTinBoss.class",
    "TB2ThongTinBoss$BossData.class": classes / "TB2ThongTinBoss$BossData.class",
    "TB2AutoSanBoss.class": classes / "TB2AutoSanBoss.class",
    "TB2AutoSanBoss$1.class": classes / "TB2AutoSanBoss$1.class",
    "TB2AutoSanBoss$2.class": classes / "TB2AutoSanBoss$2.class",
    "TB2AutoSanBoss$3.class": classes / "TB2AutoSanBoss$3.class",
    "TB2SanBossHolder.class": classes / "TB2SanBossHolder.class",
}

with zipfile.ZipFile(original, "r") as source, zipfile.ZipFile(temporary, "w") as target:
    for info in source.infolist():
        if info.filename == "META-INF/MANIFEST.MF":
            for name, path in new_classes.items():
                target.writestr(name, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED)
        data = overrides[info.filename].read_bytes() if info.filename in overrides else source.read(info)
        target.writestr(info, data)

with zipfile.ZipFile(temporary, "r") as jar:
    assert jar.namelist()[-1] == "META-INF/MANIFEST.MF"
    assert jar.testzip() is None
    assert len(jar.namelist()) == 533
    for name in list(overrides) + list(new_classes):
        assert name in jar.namelist()

os.replace(temporary, output)
print("Packed", output, output.stat().st_size)