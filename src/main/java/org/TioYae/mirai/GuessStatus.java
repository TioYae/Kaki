package org.TioYae.mirai;

class GuessStatus {
    int n; // 猜的次数
    Guess roleGuess; // 随机选择的角色及众多方法
    StringBuilder str = new StringBuilder();

    GuessStatus(String[] title, String path) {
        n = -100;
        roleGuess = new Guess(title, path);

        str.append("   ");
        for (String s : title) {
            str.append(s).append(" ");
        }
    }

    String guess(String s) {
        String t = roleGuess.guess(s);
        if (t == null) return null; // 输入不规范或抽取角色失败返回null
        if (t.startsWith("    ✔")) n = 100;
        else n++;
        str.append("\n");
        str.append(n);
        str.append(t);

        return str.toString();
    }
}