package org.example;

import net.mamoe.mirai.event.Listener;

public class User {
    String id;
    GuessStatus status;
    Listener listener;
    boolean group;

    User(String id){
        this.id = id;
        status = null;
        listener = null;
        group = false;
    }
}
