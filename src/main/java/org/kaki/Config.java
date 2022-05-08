package org.kaki;

import java.util.List;

public class Config {
    private long masterId;
    private List<Long> botId;
    private List<String> botPassword;
    private List<Long> list_Black;
    private List<Long> list_White;
    private List<Long> list_Group;
    private String configPath;

    Config() {
    }

    public void setList_White(List<Long> list_White) {
        this.list_White = list_White;
    }

    public void setBotPassword(List<String> botPassword) {
        this.botPassword = botPassword;
    }

    public void setMasterId(long masterId) {
        this.masterId = masterId;
    }

    public void setList_Group(List<Long> list_Group) {
        this.list_Group = list_Group;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setBotId(List<Long> botId) {
        this.botId = botId;
    }

    public void setList_Black(List<Long> list_Black) {
        this.list_Black = list_Black;
    }

    public List<Long> getList_Black() {
        return list_Black;
    }

    public List<Long> getBotId() {
        return botId;
    }

    public List<Long> getList_Group() {
        return list_Group;
    }

    public List<Long> getList_White() {
        return list_White;
    }

    public List<String> getBotPassword() {
        return botPassword;
    }

    public long getMasterId() {
        return masterId;
    }

    public String getConfigPath() {
        return configPath;
    }

    public String toString() {
        return "botId: " + botId + "\n" +
                "botPassword: " + botPassword + "\n" +
                "configPath: " + configPath + "\n" +
                "list_Black: " + list_Black + "\n" +
                "list_Group: " + list_Group + "\n" +
                "list_White: " + list_White + "\n" +
                "masterId: " + masterId;
    }
}
