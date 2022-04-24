package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

class Guess {
    // 名称 星级 性别 属性 武器 归属
    private final String[] title;
    private final JSONObject role;
    private final JSONArray roleData;

    Guess(String[] title, String path) {
        this.title = title;
        // 随机选择一个角色
        Role_JOSNIO jsonIO = new Role_JOSNIO(path);
        this.roleData = jsonIO.getJsonArray();
        if (roleData != null) {
            Random random = new Random();
            int i = random.nextInt(roleData.length());
            role = roleData.getJSONObject(i);
        } else role = null;
    }

    // -1错误 0小于 1正确 2大于
    // 取消数值替代，合并分析数值步骤
    public String guess(String name) {
        JSONObject role = getInformation(name);
        StringBuilder str = new StringBuilder();
        if (role == null || this.role == null) { // 输入不规范或抽取角色失败返回null
            return null;
        } else {
            for (String s : title) {
                if (role.getString(s).equals(this.role.getString(s)))
                    str.append("    ✔");
                else str.append("    ✘ ");
            }
        }
        return str.toString();
    }

    // 名称 星级 性别 属性 武器 归属
    JSONObject getInformation(String name) {
        if (roleData != null) {
            int n = this.roleData.length();
            for (int i = 0; i < n; i++) {
                if (this.roleData.getJSONObject(i).getString(title[0]).equals(name))
                    return this.roleData.getJSONObject(i);
            }
        }
        return null;
    }

    String[] getAnswer() {
        if (role == null) return null;
        String[] ans = new String[title.length];
        for (int i = 0; i < title.length; i++) {
            ans[i] = role.getString(title[i]);
        }
        return ans;
    }
}