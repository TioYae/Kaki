package org.kaki;

import net.mamoe.mirai.event.Listener;

import java.util.Timer;

public class User {
    long id;
    GuessStatus status;
    Listener listener;
    boolean group;
    Timer timer; // 定时器

    User(long id){
        this.id = id;
        status = null;
        listener = null;
        group = false;
        timer = null;
    }
}
