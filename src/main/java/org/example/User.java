package org.example;

import net.mamoe.mirai.event.Listener;

import java.util.Timer;

public class User {
    String id;
    GuessStatus status;
    Listener listener;
    boolean group;
    Timer timer; // 定时器

    User(String id){
        this.id = id;
        status = null;
        listener = null;
        group = false;
        timer = null;
    }
}
