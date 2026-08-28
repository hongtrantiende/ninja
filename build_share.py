import os
import sys
import glob
import shutil
import zipfile
import subprocess

root = os.path.abspath(os.path.dirname(__file__))
build_dir = os.path.join(root, "build")
unpacked_dir = os.path.join(build_dir, "unpacked")
stubs_dir = os.path.join(build_dir, "stubs_compiled")
share_jar_path = os.path.join(root, "Aeharuna_share.jar")
base_jar_path = os.path.join(root, "Aeharuna.jar")
src_backup_dir = os.path.join(build_dir, "src_backup")

print("=== [SHARE BUILD] 1. Backing up original src/ ===")
if os.path.exists(src_backup_dir):
    shutil.rmtree(src_backup_dir)
shutil.copytree(os.path.join(root, "src"), src_backup_dir)

try:
    print("=== [SHARE BUILD] 2. Applying share-specific configs (VIP Map 160ms, native PkBoss for normal maps, simplified BossConfig) in src/ ===")

    # 1. Write simplified BossConfig.java (only Map VIP 1 and Map VIP 2 checkboxes)
    bossconfig_content = """import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;

public final class BossConfig implements CommandListener {
    private static BossConfig instance;

    private Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;

    private ChoiceGroup cgMapVIP;
    private ChoiceGroup cgMapVIP2;

    private BossConfig() {
        cmdLuu = new Command("L\\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\\u1ee7y", Command.BACK, 2);
        buildForm();
    }

    private void buildForm() {
        form = new Form("C\\u00e0i \\u0111\\u1eb7t S\\u0103n Boss");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.setCommandListener(this);

        cgMapVIP = new ChoiceGroup("Map VIP", Choice.MULTIPLE);
        cgMapVIP.append("S\\u0103n Map VIP (M195)", null);
        form.append(cgMapVIP);

        cgMapVIP2 = new ChoiceGroup("Map VIP2", Choice.MULTIPLE);
        cgMapVIP2.append("S\\u0103n Map VIP2 (M196)", null);
        form.append(cgMapVIP2);
    }

    private void loadCurrentState() {
        cgMapVIP.setSelectedIndex(0, AutoSanBoss.isMapEnabled(195));
        cgMapVIP2.setSelectedIndex(0, AutoSanBoss.isMapEnabled(196));
    }

    public static void select() {
        AutoSanBoss.loadFromRMS();
        if (instance == null) {
            instance = new BossConfig();
        }
        instance.loadCurrentState();
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            boolean en195 = cgMapVIP.isSelected(0);
            boolean en196 = cgMapVIP2.isSelected(0);

            for (int i = AutoSanBoss.disabledMaps.size() - 1; i >= 0; i--) {
                Integer m = (Integer) AutoSanBoss.disabledMaps.elementAt(i);
                if (m != null && (m.intValue() == 195 || m.intValue() == 196)) {
                    AutoSanBoss.disabledMaps.removeElementAt(i);
                }
            }
            if (!en195) AutoSanBoss.disabledMaps.addElement(new Integer(195));
            if (!en196) AutoSanBoss.disabledMaps.addElement(new Integer(196));
            AutoSanBoss.saveToRMS();

            GameScr.gameAC("Boss Config: \\u0110\\u00e3 l\\u01b0u (VIP1: " + (en195 ? "B\\u1eadt" : "T\\u1eaft") + ", VIP2: " + (en196 ? "B\\u1eadt" : "T\\u1eaft") + ")");
        }
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
    }
}
"""
    bossconfig_file = os.path.join(root, "src", "BossConfig.java")
    with open(bossconfig_file, "w", encoding="utf-8") as f:
        f.write(bossconfig_content)

    # 2. Modify NamMod.java: Hide ExploitConfig, keep BossConfig
    nammod_file = os.path.join(root, "src", "NamMod.java")
    with open(nammod_file, "r", encoding="utf-8") as f:
        nammod_code = f.read()

    old_exploit_cfg = """        // === Exploit / Test ===
        items.addElement(command("C\\u00e0i \\u0111\\u1eb7t CN Test \\u25b8", CFG_EXPLOIT_MENU));"""
    nammod_code = nammod_code.replace(old_exploit_cfg, "")

    with open(nammod_file, "w", encoding="utf-8") as f:
        f.write(nammod_code)

    # 3. Modify AutoBossEvent.java: disable pre-spawn in share (set to 0s)
    autobosseven_file = os.path.join(root, "src", "AutoBossEvent.java")
    with open(autobosseven_file, "r", encoding="utf-8") as f:
        event_code = f.read()

    event_code = event_code.replace("private static final int PRE_SPAWN_SECONDS = 30;", "private static final int PRE_SPAWN_SECONDS = 0;")
    with open(autobosseven_file, "w", encoding="utf-8") as f:
        f.write(event_code)

    # 4. Modify AutoSanBoss.java: Map VIP zone scan delay = 160ms, native PkBoss for normal maps
    autosanboss_file = os.path.join(root, "src", "AutoSanBoss.java")
    with open(autosanboss_file, "r", encoding="utf-8") as f:
        sanboss_code = f.read()

    # Map VIP delay = 160ms
    sanboss_code = sanboss_code.replace("try { Auto.gameAA(zone); } catch (Exception e) {}\n            sleep(50);", "try { Auto.gameAA(zone); } catch (Exception e) {}\n            sleep(100);")

    # Restore native PkBoss for normal maps in pkBossOnMap
    pk_start_idx = sanboss_code.find("private boolean pkBossOnMap(int mapID) {")
    pk_end_idx = sanboss_code.find("private void sleepSeconds(int seconds) {")

    if pk_start_idx != -1 and pk_end_idx != -1:
        native_pk_method = """private boolean pkBossOnMap(int mapID) {
        if (!checkStillRunning()) return false;
        if (mapID == 135 || mapID == 136) {
            return pkLangCoMap(mapID);
        } else if (mapID == 195) {
            return pkBossMapVIP();
        } else if (mapID == 196) {
            return pkBossMapVIP2();
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
            if (TileMap.mapID == 195 || TileMap.mapID == 196) {
                GameScr.gameAC("TSB: Tho\\u00e1t Map VIP \\u0111\\u1ec3 s\\u0103n map kh\\u00e1c...");
                suicideAndEnsureAlive();
            }
        }
        GameScr.gameAC("TSB: PK M" + mapID);

        // Leader start PkBoss solo - quet khu, tim boss bang PkBoss goc
        try {
            Code.gameAA(new PkBoss(mapID));
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                Code.gameAA(new PkBoss(mapID));
            } else {
                return false;
            }
        }

        long startTime = System.currentTimeMillis();
        boolean sentPartyCmd = false;
        boolean bossKilled = false;
        boolean keepFighting = true;
        int deathCount = 0;
        final int MAX_DEATH_RETRIES = 10;
        final long MAX_FIGHT_TIME_MS = 10 * 60 * 1000;

        while (checkStillRunning() && keepFighting) {
            if (System.currentTimeMillis() - startTime > MAX_FIGHT_TIME_MS) {
                GameScr.gameAC("TSB: Timeout M" + mapID + " (10 phut)");
                break;
            }

            if (!(Code.gameAB instanceof PkBoss)) {
                if (sentPartyCmd && deathCount < MAX_DEATH_RETRIES && checkStillRunning()) {
                    sleep(500);
                    if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                        GameScr.gameAC("TSB: Chet lan " + deathCount + "! Hoi sinh...");
                        respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        sleep(1000);
                    }
                    if (checkStillRunning() && Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                        GameScr.gameAC("TSB: Quay lai M" + mapID + " (lan " + (deathCount + 1) + ")");
                        Code.gameAA(new PkBoss(mapID));
                        if (TileMap.mapID != mapID) {
                            sentPartyCmd = false;
                        }
                        sleep(500);
                        continue;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            try {
                if (isDisconnected()) {
                    GameScr.gameAC("TSB: M\\u1ea5t k\\u1ebft n\\u1ed1i khi PK M" + mapID + "!");
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }

                if (!sentPartyCmd && hasBossOnCurrentMap()) {
                    sentPartyCmd = true;
                    GameScr.gameAC("TSB: Boss! Goi nhom M" + mapID + " K" + TileMap.zoneID);
                    sendPartyCommand("pkm -1");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(500);
                    sendPartyCommand("pkk " + TileMap.zoneID);
                }

                if (sentPartyCmd) {
                    lockBossFocus();
                }

                if (sentPartyCmd && !hasBossOnCurrentMap()) {
                    bossKilled = true;
                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    break;
                }

                if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                    deathCount++;
                    GameScr.gameAC("TSB: Chet lan " + deathCount + "! Hoi sinh...");
                    respawnFast();
                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }
                    sleep(500);
                    continue;
                }
            } catch (Exception e) {
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }
            }
            sleep(200);
        }

        restoreDummyAuto();

        if (bossKilled) {
            long elapsed = System.currentTimeMillis() - startTime;
            GameScr.gameAC("TSB: Xong M" + mapID + " (" + (elapsed / 1000) + "s)");
            grabAllItems();
            return true;
        }
        if (deathCount >= MAX_DEATH_RETRIES) {
            GameScr.gameAC("TSB: M" + mapID + " chet " + deathCount + " lan, bo qua");
        }
        return false;
    }

    """
        sanboss_code = sanboss_code[:pk_start_idx] + native_pk_method + sanboss_code[pk_end_idx:]

    with open(autosanboss_file, "w", encoding="utf-8") as f:
        f.write(sanboss_code)

    print("=== [SHARE BUILD] 3. Clean & Unpack base Aeharuna.jar ===")
    if os.path.exists(unpacked_dir):
        shutil.rmtree(unpacked_dir)
    if os.path.exists(stubs_dir):
        shutil.rmtree(stubs_dir)
    os.makedirs(unpacked_dir, exist_ok=True)
    os.makedirs(stubs_dir, exist_ok=True)

    with zipfile.ZipFile(base_jar_path, 'r') as z:
        z.extractall(unpacked_dir)

    print("=== [SHARE BUILD] 4. Compile Stubs & Src ===")
    stubs_files = glob.glob(os.path.join(root, "stubs", "**", "*.java"), recursive=True)
    src_files = glob.glob(os.path.join(root, "src", "**", "*.java"), recursive=True)

    cmd_stubs = ["javac", "--release", "8", "-encoding", "UTF-8", "-d", stubs_dir] + stubs_files
    subprocess.run(cmd_stubs, check=True)

    cp_path = os.pathsep.join([unpacked_dir, stubs_dir])
    cmd_src = ["javac", "--release", "8", "-encoding", "UTF-8", "-cp", cp_path, "-d", unpacked_dir] + src_files
    subprocess.run(cmd_src, check=True)

    print("=== [SHARE BUILD] 5. Run Python Patches ===")
    subprocess.run(["python3", "scripts/patch_class_j2me.py", unpacked_dir], check=True)

    effect_auto_class = os.path.join(unpacked_dir, "EffectAuto.class")
    if os.path.exists(effect_auto_class):
        subprocess.run(["python3", "scripts/patch_effectauto.py", effect_auto_class], check=True)

    print("=== [SHARE BUILD] 6. Pack Aeharuna_share.jar ===")
    for root_d, dirs_d, files_d in os.walk(unpacked_dir):
        for f in files_d:
            if f.endswith(".bak") or "bak_effects" in f:
                os.remove(os.path.join(root_d, f))

    manifest_file = os.path.join(unpacked_dir, "META-INF", "MANIFEST.MF")
    subprocess.run(["jar", "cfm", share_jar_path, manifest_file, "."], cwd=unpacked_dir, check=True)
    print(f"=== SUCCESS! Aeharuna_share.jar created: {os.path.getsize(share_jar_path)} bytes ===")

finally:
    print("=== [SHARE BUILD] 7. Restoring original src/ files from backup ===")
    if os.path.exists(src_backup_dir):
        shutil.rmtree(os.path.join(root, "src"))
        shutil.copytree(src_backup_dir, os.path.join(root, "src"))
        shutil.rmtree(src_backup_dir)
    print("=== Original src/ restored cleanly with 50ms boss hunting! ===")
