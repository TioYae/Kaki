package org.kaki;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

class Role_JOSNIO {
    private final String path;
    private JSONArray jsonArray;

    Role_JOSNIO(String path) {
        this.path = path;
        buildJsonArray();
    }

    // 读取json对象数组
    void buildJsonArray() {
        String jsonStr = "";
        try {
            File file = new File(this.path);
            FileReader fileReader = new FileReader(file);
            Reader reader = new InputStreamReader(new FileInputStream(file), "Utf-8");
            int ch;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
//            System.out.println(jsonStr);
            this.jsonArray = new JSONArray(jsonStr);
//            System.out.println(this.jsonArray);
        } catch (Exception e) {
        }
    }

    // 获取json对象数组
    public JSONArray getJsonArray() {
        return jsonArray;
    }

    // 查找数据中是否已有某角色
    boolean findName(String name) {
        if (jsonArray == null) return false;
        int n = jsonArray.length();
        for (int i = 0; i < n; i++) {
            if (jsonArray.getJSONObject(i).getString("名称").equals(name)) return true;
        }
        return false;
    }

    // 添加角色
    boolean jsonAdd(String[] title, String[] arr) {
        if (findName(arr[0])) {
            System.out.println("该角色已存在");
            return false;
        }
        int n = title.length;
        JSONObject jsonObject = new JSONObject();
        // json不需要顺序
        // JSONObject jsonObject = new JSONObject(new LinkedHashMap()); // 保证数据顺序不打乱

        for (int i = 0; i < n; i++) {
            jsonObject.put(title[i], arr[i]);
//            System.out.println(jsonObject);
        }
        if (this.jsonArray == null) this.jsonArray = new JSONArray();
        this.jsonArray.put(jsonObject);
        System.out.println(jsonObject.toString());
        return true;
    }

    // 写入json文件
    void jsonWrite() throws Exception {
        File file = new File(this.path);
        if (!file.getParentFile().exists()) { // 如果父目录不存在，创建父目录
            file.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(this.path);
        OutputStreamWriter os = new OutputStreamWriter(fos);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
        w.write(jsonArray.toString());
        w.close();
        System.out.println(this.jsonArray.toString());
    }
}