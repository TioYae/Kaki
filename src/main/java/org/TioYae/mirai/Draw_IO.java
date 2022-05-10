package org.TioYae.mirai;

import java.io.*;
import java.util.HashMap;

public class Draw_IO {
    // 读取每日抽卡记录
    HashMap<Long, DrawStatus> loadDailyDrawStatus(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return null;
            }
            FileInputStream in = new FileInputStream(path);
            ObjectInputStream read = new ObjectInputStream(in);
            return (HashMap<Long, DrawStatus>) read.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 写入抽卡数据文件
    boolean saveDailyDrawStatus (HashMap<Long, DrawStatus> drawStatus) {
        try {
            String path = System.getProperty("user.dir") + "/config/org.kaki/drawStatus.txt";
            File f = new File(path);
            if (!f.getParentFile().exists()) { // 如果父目录不存在，创建父目录
                f.getParentFile().mkdirs();
            }

            FileOutputStream out = new FileOutputStream(f);
            ObjectOutputStream save = new ObjectOutputStream(out);
            save.writeObject(drawStatus);
            save.flush();
            save.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
