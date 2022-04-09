package org.example;

import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public final class Kaki extends JavaPlugin {
    public static final Kaki INSTANCE = new Kaki();

    public static final List<String> black = Arrays.asList(new String[]{});
    public static final List<String> white = Arrays.asList("1246336370");
    public static final List<String> group = Arrays.asList("1020335236", "745184152", "563125969");
    /*GenshinStatus status = null;// 群组可以试试哈希表存每个人的答题状态
    Listener listener = null;*/
    boolean roleLock = false;
    HashMap<String, Boolean> usersLock = new HashMap<>();
    HashMap<String, Integer> fileNum = new HashMap<>();

    private Kaki() {
        super(new JvmPluginDescriptionBuilder("org.example.Kaki", "1.0")
                .author("Tio Yae")
                .build()
        );
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {

    }

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded!");

        GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, this::hear);
    }

    // 监听方法
    void hear(MessageEvent event) {
        String id = String.valueOf(event.getSender().getId());
        // 限定监听白名单的群
        if (event.getClass().getName().contains("Group") && !group.contains(String.valueOf(event.getSubject().getId())))
            return;
        String content = event.getMessage().contentToString();
        System.out.println(content);


        if (content.startsWith(">")) {
            // 获取请求指令操作用户状态
            boolean userLock = usersLock.getOrDefault(id, false);
            System.out.println(id + " lock(outer): " + userLock);

            // 进程锁
            if (userLock) {
                event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
            } else {
                if (content.length() > 1)
                    content = content.substring(1);
                else
                    content = "";

                // 创建新进程
                User user = new User(id);
                String[] sentences = content.split(" ");
                String[] title;
                switch (sentences[0]) {
                    case "help":
                    case "帮助":
                        if (sentences.length > 1) help(sentences[1], event);
                        else help("", event);
                        break;
                    case "猜":
                    case "猜D":
                    case "猜d":
                    case "猜原神":
                    case "猜原神D":
                    case "猜原神d":
                        // 更新进程锁状态
                        changeStatus(id, true);

                        System.out.println("outer: " + content);
                        title = new String[]{"名称", "星级", "性别", "属性", "武器", "归属"};
                        if (!Guess(event, user, title, "原神"))
                            event.getSubject().sendMessage("获取数据失败");
                        break;
                    case "猜崩3":
                    case "猜崩3D":
                    case "猜崩3d":
                        // 更新进程锁状态
                        /*changeStatus(id, true);

                        System.out.println("outer: " + content);
                        title = new String[]{"装甲", "角色", "评级", "武器", "归属"};
                        if (!Guess(event, user, title, "崩3"))
                            event.getSubject().sendMessage("获取数据失败");*/
                        break;
                    case "添加原神角色":
                        // 更新进程锁状态
                        changeStatus(id, true);

                        System.out.println("白名单：" + white);
                        System.out.println("请求人：" + event.getSender().getId());
                        if (!white.contains(String.valueOf(event.getSender().getId())))
                            event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                        else addRole("原神", event, user);
                        break;
                    case "添加崩3角色":
                        // 更新进程锁状态
                        changeStatus(id, true);

                        System.out.println("白名单：" + white);
                        System.out.println("请求人：" + event.getSender().getId());
                        if (!white.contains(String.valueOf(event.getSender().getId())))
                            event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                        else addRole("崩3", event, user);
                        break;
                    case "创建原神文件夹":
                        System.out.println("白名单：" + white);
                        System.out.println("请求人：" + event.getSender().getId());
                        if (!white.contains(String.valueOf(event.getSender().getId())))
                            event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                        else {
                            if (buildFolder("原神")) event.getSubject().sendMessage("创建文件夹成功");
                            else event.getSubject().sendMessage("创建文件夹失败");
                        }
                        break;
                    case "创建崩3文件夹":
                        System.out.println("白名单：" + white);
                        System.out.println("请求人：" + event.getSender().getId());
                        if (!white.contains(String.valueOf(event.getSender().getId())))
                            event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                        else {
                            if (buildFolder("崩3")) event.getSubject().sendMessage("创建文件夹成功");
                            else event.getSubject().sendMessage("创建文件夹失败");
                        }
                        break;
                    case "图片重载":
                        fileNum.clear();
                        event.getSubject().sendMessage("图片重载成功");
                        break;
                    case "test":
                        if (!white.contains(String.valueOf(event.getSender().getId())))
                            event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                        else
                            test(event);
                        break;
                    default:
                        System.out.println("default in \">\" order");
                        break;
                }
            }
        } else {
            respond(content, event);
        }
    }

    // 更改对应用户的状态
    boolean changeStatus(String id, boolean lock) {
        if (id.equals("")) return false;
        usersLock.remove(id);
        usersLock.put(id, lock);
        return true;
    }

    // 预设回复
    void respond(String content, MessageEvent event) {
        if (content.contains("早上好")) {
            event.getSubject().sendMessage("早上好");
        } else if (content.contains("上午好")) {
            event.getSubject().sendMessage("上午好");
        } else if (content.contains("中午好")) {
            event.getSubject().sendMessage("中午好");
        } else if (content.contains("下午好")) {
            event.getSubject().sendMessage("下午好");
        } else if (content.contains("晚上好")) {
            event.getSubject().sendMessage("晚上好");
        }

        if (content.contains("早安")) {
            event.getSubject().sendMessage("早安");
        } else if (content.contains("午安")) {
            event.getSubject().sendMessage("午安");
        } else if (content.contains("晚安")) {
            event.getSubject().sendMessage("晚安");
        }

        //带称呼触发
        if (content.startsWith("Kaki") || content.startsWith("kaki")) {
            if (content.length() > 4)
                content = content.substring(4);
            else
                content = "";
            System.out.println(content);

            if (content.equals("")) {
                event.getSubject().sendMessage("咋了？");
            }

            if (content.contains("吃了吗")) {
                event.getSubject().sendMessage("吃了");
            }
        }
    }

    // 帮助
    void help(String content, MessageEvent event) {
        String ans;
        switch (content) {
            case "":
                System.out.println("输出帮助列表");
                ans = "目前存在的指令操作如下：\n" +
                        ">猜原神（>猜）\n" +
                        ">猜崩3（未启用）\n" +
                        ">添加原神角色\n" +
                        ">添加崩3角色（未启用）\n" +
                        "\n可以通过输入「>帮助 指令名」来获取更多帮助";
                break;
            case "猜":
            case "猜原神":
                ans = "「猜原神」指令：\n" +
                        "Kaki将会在原神角色库中随机抽取一位角色，每位玩家每轮拥有5次猜测机会 ^_^\n" +
                        "\nPS：有一位彩蛋角色哦~\n" +
                        "\n在指令后追加「D」或「d」可在每一次猜测时输出本次猜测角色的信息\n" +
                        "（在「猜崩3」指令启用前，可直接使用「>猜」及其衍生指令快捷调用）";
                break;
            case "猜崩3":
                ans = "「猜崩3」指令：\n" +
                        "Kaki将会在崩坏3装甲库中随机抽取一位角色的一件装甲，每位玩家每轮拥有5次猜测机会 ^_^\n" +
                        "在指令后追加「D」或「d」可在每一次猜测时输出本次猜测角色的信息";
                break;
            case "添加原神角色":
                ans = "「添加原神角色」指令：\n" +
                        "字面意思，仅管理员可用";
                break;
            case "添加崩3角色":
                ans = "「添加崩3角色」指令：\n" +
                        "字面意思，仅管理员可用";
                break;
            default:
                ans = "未找到「" + content + "」指令";
                break;
        }
        event.getSubject().sendMessage(ans);
    }

    // 批量创建文件夹
    boolean buildFolder(String game) {
        Role_JOSNIO roleIO = null;
        if (game.equals("原神")) roleIO = new Role_JOSNIO("./src/config/roleData_Genshin.json");
        else if (game.equals("崩3")) roleIO = new Role_JOSNIO("./src/config/roleData_Honkai.json");
        if (roleIO == null) return false;
        JSONArray roleData = roleIO.getJsonArray();
        if (roleData == null) {
            System.out.println("空数据");
            return false;
        }
        int n = roleData.length();
        for (int i = 0; i < n; i++) {
            String name = roleData.getJSONObject(i).getString("名称");
            File file = new File("./src/config/picture/" + game + "/" + name);
            if (!file.exists()) {
                if (file.mkdirs()) System.out.println("已创建文件夹：" + name);
                else return false;
            }
        }
        return true;
    }

    // 加载图片数量
    void loadImage(String name, String game) {
        File[] files = new File("./src/config/picture/" + game + "/" + name).listFiles();
        if (files == null) fileNum.put(name, 0);
        else fileNum.put(name, files.length);
    }

    // 猜
    boolean Guess(MessageEvent event, User person, String[] title, String game) {
        String g = "";
        if (game.equals("原神")) {
            g = "一位角色";
            person.status = new GuessStatus(title, "./src/config/roleData_Genshin.json");
        } else if (game.equals("崩3")) {
            g = "一件装甲";
            person.status = new GuessStatus(title, "./src/config/roleData_Honkai.json");
        }
        String[] answer = person.status.roleGuess.getAnswer();
        boolean details = event.getMessage().toString().contains("D") || event.getMessage().toString().contains("d");
        if (answer == null) {
            System.out.println("读取数据为空");
            return false;
        }

        if (!fileNum.containsKey(answer[0])) loadImage(answer[0], game);
        System.out.println(fileNum);
        int imageNum = fileNum.get(answer[0]);
        Random random = new Random();
        int i = random.nextInt(imageNum) + 1;
        File f = new File("./src/config/picture/" + game + "/" + answer[0] + "/" + i + ".jpg");
        if (!f.exists()) f = new File("./src/config/picture/" + game + "/" + answer[0] + "/" + i + ".png");
        Image image = net.mamoe.mirai.contact.Contact.uploadImage(event.getSubject(), f);
        StringBuilder str = new StringBuilder();
        for (int j = 0; j < answer.length; j++) {
            str.append("\n").append(title[j]).append("：").append(answer[j]);
        }
        MessageChain role = new MessageChainBuilder().append(image).append(str).build();

        if (person.status.n < 0) {
            // 加上at大概率发不出去，弃用，未修复
            // 原因：风控，与代码无关
            if (event.getClass().getName().contains("Group")) { // 群组消息要@
                At at = new At(event.getSender().getId());
                MessageChain message = new MessageChainBuilder().append(at).append("\n").append("已随机抽取").append(g).append("，请开始您的猜测 ^_^").build();
                event.getSubject().sendMessage(message);
            } else if (event.getClass().getName().contains("Friend")) // 好友消息不@
                event.getSubject().sendMessage("已随机抽取" + g + "，请开始您的猜测 ^_^");
            person.status.n = 0; // 用MessageEvent时不需要跳过指令句
            person.listener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, e -> {
                if (e.getMessage().contentToString().contains("取消") || e.getMessage().contentToString().contains("不玩了")) {
                    person.listener.complete();
                    e.getSubject().sendMessage("操作取消。正确答案是「" + answer[0] + "」");
                    e.getSubject().sendMessage(role);

                    // 更新进程锁状态
                    changeStatus(person.id, false);
                    return;
                }

                String id = String.valueOf(e.getSender().getId());
                // 判断为同一个人的消息才执行
                if (person.id.equals(id)) {
                    String c = e.getMessage().contentToString();
                    System.out.println(person.status.n + ": inner: " + c);

                    /*if (person.status.n == 0) { // 跳过指令句 // 用FriendMessageEvent时需要跳过
                        person.status.n++;
                    } else */
                    if (person.status.n < 5 && !c.startsWith(">")) { // 输入指令语句不执行猜的动作
                        // 加上at大概率发不出去，弃用，未修复
                        // 原因：风控，与代码无关
                        if (e.getClass().getName().contains("Group")) { // 群组消息要@
                            At at = new At(e.getSender().getId());
                            String ans = person.status.guess(c);
                            // 追加角色详情
                            if (details) {
                                JSONObject r = person.status.roleGuess.getInformation(c);
                                if (r != null) {
                                    StringBuilder m = new StringBuilder();
                                    m.append(ans);
                                    for (String s : title) {
                                        m.append("\n").append(s).append("：").append(r.getString(s));
                                    }
                                    ans = m.toString();
                                }
                            }
                            MessageChain message = new MessageChainBuilder().append(at).append("\n").append(ans).build();
                            // 防止消息轰炸
                            if (person.status.n >= 1 && person.status.n <= 5) e.getSubject().sendMessage(message);
                        } else if (e.getClass().getName().contains("Friend")) {// 好友消息不@
                            String ans = person.status.guess(c);
                            // 追加角色详情
                            if (details) {
                                JSONObject r = person.status.roleGuess.getInformation(c);
                                if (r != null) {
                                    StringBuilder m = new StringBuilder();
                                    m.append(ans);
                                    for (String s : title) {
                                        m.append("\n").append(s).append("：").append(r.getString(s));
                                    }
                                    ans = m.toString();
                                }
                            }
                            // 防止消息轰炸
                            // n为5即失败，照样输出；因为正确时立刻赋值100，所以正确时不输出
                            if (person.status.n >= 1 && person.status.n <= 5) e.getSubject().sendMessage(ans);
                        }
                    }
                    if (person.status.n == 100) { // 100为猜对了
                        person.status.n = 0;
                        e.getSubject().sendMessage("猜对啦！答案是「" + answer[0] + "」");
                        e.getSubject().sendMessage(role);

                        person.listener.complete();

                        // 更新进程锁状态
                        changeStatus(person.id, false);
                    } else if (person.status.n >= 5) {
                        person.status.n = -100;
                        e.getSubject().sendMessage("猜错啦！正确答案是「" + answer[0] + "」");
                        e.getSubject().sendMessage(role);

                        person.listener.complete();

                        // 更新进程锁状态
                        changeStatus(person.id, false);
                    }
                }
            });
        }
        return true;
    }

    // 新增角色
    void addRole(String game, MessageEvent event, User person) {
        switch (game) {
            case "原神":
                if (roleLock) {
                    event.getSubject().sendMessage("有用户正在进行文件处理，请等待");

                    // 更新进程锁状态
                    changeStatus(person.id, false);
                    break;
                }
                event.getSubject().sendMessage("请输入「名称,星级,性别,属性,武器,归属」，用中/英文逗号隔开，不支持空格");
                person.listener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, e -> {
                    // 判断为同一个人的消息才执行
                    if (String.valueOf(e.getSender().getId()).equals(person.id)) {
                        roleLock = true; // MessageEvent
                        String c = e.getMessage().contentToString();
                        String[] title = new String[]{"名称", "星级", "性别", "属性", "武器", "归属"};
                        System.out.println("addRole: " + c);
                        String[] arr = c.split(",|，");
                        if (title.length != arr.length) {
                            if (arr[0].equals("取消")) {
                                e.getSubject().sendMessage("已取消添加角色操作");
                                roleLock = false;
                                person.listener.complete();

                                // 更新进程锁状态
                                changeStatus(person.id, false);
                                return;
                            }
                            e.getSubject().sendMessage("输入有误，请重新输入");
                            e.getSubject().sendMessage("请输入「名称,星级,性别,属性,武器,归属」，用中/英文逗号隔开，不支持空格");
                            return;
                        }

                        Role_JOSNIO jsonIO = new Role_JOSNIO("./src/config/roleData_Genshin.json");
                        try {
                            if (jsonIO.jsonAdd(title, arr)) {
                                jsonIO.jsonWrite();
                                e.getSubject().sendMessage("角色添加成功");
                            } else {
                                e.getSubject().sendMessage("该角色已存在");
                            }
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                        roleLock = false;
                        person.listener.complete();

                        // 更新进程锁状态
                        changeStatus(person.id, false);
                    }
                });
                break;
            case "崩3":


                // 更新进程锁状态
                changeStatus(person.id, false);
                break;
            default:
                System.out.println("addRole default");

                // 更新进程锁状态
                changeStatus(person.id, false);
                break;
        }
    }

    // 测试
    void test(MessageEvent event) {
        File f = new File("./src/config/picture/原神/刻晴/9.jpg");
        if (!f.exists()) f = new File("./src/config/picture/原神/刻晴/9.png");
        Image image = net.mamoe.mirai.contact.Contact.uploadImage(event.getSubject(), f);
        event.getSubject().sendMessage(image);
    }
}