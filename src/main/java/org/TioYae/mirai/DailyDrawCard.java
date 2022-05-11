package org.TioYae.mirai;

import org.json.JSONArray;

import java.util.*;

public class DailyDrawCard {
    private final String role_path;
    private JSONArray roleData;

    DailyDrawCard(String role_path) {
        this.role_path = role_path;
        roleData = new Role_JOSNIO(role_path).getJsonArray();
    }

    public List<String> draw() {
        HashSet<Integer> draw = new HashSet<>();
        Random random = new Random();
        int n = random.nextInt(100); // 数量区间为[0, 3]
        if (n < 10) n = 0; // 抽不到的概率为10%
        else if (n < 50) n = 1; // 抽到一个的概率为40%
        else if (n < 90) n = 2; // 抽到两个的概率为40%
        else n = 3; // 抽到三个的概率为10%
        if (n == 0) return null;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int t = random.nextInt(roleData.length());
            while (draw.contains(t)) {
                t = random.nextInt(roleData.length());
            }
            draw.add(t);
            list.add(roleData.getJSONObject(t).getString("名称"));
        }
        return list;
    }
}
