import java.util.Vector;

public class MultiSkillAttack {
    public static void attackMultiSkill(Char myChar, MyVector reAE, MyVector reAF) {
        if (myChar == null) return;
        
        Skill[] assignedSkills = GameScr.gamePB;
        boolean firedAssigned = false;
        
        // Lưu lại skill người dùng đang tích chọn trên giao diện UI để giữ cố định giao diện
        Skill originalSelectedSkill = myChar.myskill;
        
        if (assignedSkills != null && assignedSkills.length > 0) {
            for (int i = 0; i < assignedSkills.length; i++) {
                try {
                    Skill s = assignedSkills[i];
                    if (s != null && s.template != null) {
                        // Chỉ chọn skill chủ động tấn công/chiêu đánh (bỏ qua skill buff thụ động type == 2)
                        if (s.template.type == 1 || s.template.type == 3 || s.template.type == 0) {
                            
                            // Lọc danh sách quái sống nằm đúng trong tầm đánh dx, dy thực tế của chiêu s
                            MyVector targetMobs = getTargetMobsForSkill(myChar, s);
                            if (targetMobs.size() == 0 && reAE != null && reAE.size() > 0) {
                                targetMobs = reAE;
                            }
                            
                            if (targetMobs.size() > 0) {
                                firedAssigned = true;
                                
                                // 1. Gửi gói tin Message 41 (Select Skill) để Server Ninja School ghi nhận skill được chọn
                                Service.gI().gameAG(s.template.id);
                                
                                // 2. Gửi gói tin Message 4/60 (Attack Packet) mang danh sách ID quái lan thực tế
                                Service.gI().gameAA(targetMobs, reAF != null ? reAF : new MyVector(), 1);
                                
                                // 3. Vẽ hiệu ứng kỹ năng chiêu thức cho nhân vật
                                if (GameScr.sks != null && s.template.id >= 0 && s.template.id < GameScr.sks.length) {
                                    myChar.gameAB(GameScr.sks[s.template.id], 0);
                                }
                                
                                // Trễ 40ms giữa mỗi chiêu để Server xử lý trơn tru không drop gói tin
                                try {
                                    Thread.sleep(40L);
                                } catch (Exception ex) {}
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }
        
        // Dự phòng: Nếu chưa gán ô phím tắt nào trên màn hình, dùng vSkillFight
        if (!firedAssigned && myChar.vSkillFight != null) {
            int size = myChar.vSkillFight.size();
            for (int i = 0; i < size; i++) {
                try {
                    Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                    if (s != null && s.template != null) {
                        if (s.template.type == 1 || s.template.type == 3 || s.template.type == 0) {
                            MyVector targetMobs = getTargetMobsForSkill(myChar, s);
                            if (targetMobs.size() == 0 && reAE != null && reAE.size() > 0) {
                                targetMobs = reAE;
                            }
                            if (targetMobs.size() > 0) {
                                Service.gI().gameAG(s.template.id);
                                Service.gI().gameAA(targetMobs, reAF != null ? reAF : new MyVector(), 1);
                                
                                if (GameScr.sks != null && s.template.id >= 0 && s.template.id < GameScr.sks.length) {
                                    myChar.gameAB(GameScr.sks[s.template.id], 0);
                                }
                                try {
                                    Thread.sleep(40L);
                                } catch (Exception ex) {}
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }
        
        // Khôi phục lại đúng ô skill chính mà người dùng đang chọn trên UI
        myChar.myskill = originalSelectedSkill;
    }
    
    // Quét danh sách quái sống trong tầm dx, dy thực tế và tối đa maxFight của chiêu s
    private static MyVector getTargetMobsForSkill(Char myChar, Skill s) {
        MyVector vTargets = new MyVector();
        if (GameScr.vMob == null) return vTargets;
        
        int maxTargets = s.maxFight > 0 ? s.maxFight : 1;
        // Lấy tầm đánh ngang (dx) và dọc (dy) từ thông số của skill s
        int rangeX = s.dx > 0 ? s.dx + 40 : 120;
        int rangeY = s.dy > 0 ? s.dy + 40 : 100;
        
        int size = GameScr.vMob.size();
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                // Kiểm tra quái còn sống và hợp lệ
                if (mob != null && mob.status != 0 && mob.status != 1 && mob.hp > 0) {
                    int diffX = Math.abs(myChar.cx - mob.x);
                    int diffY = Math.abs(myChar.cy - mob.y);
                    if (diffX <= rangeX && diffY <= rangeY) {
                        vTargets.addElement(mob);
                        if (vTargets.size() >= maxTargets) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {}
        }
        return vTargets;
    }
}
