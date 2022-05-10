package org.TioYae.mirai;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DrawStatus implements Serializable {
    String date;
    List<String> role;
    HashMap<String, Integer> num;

    DrawStatus(String path) {
        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        role = new DailyDrawCard(path).draw();
        num = null;
    }
}
