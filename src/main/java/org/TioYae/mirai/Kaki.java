package org.TioYae.mirai;

import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Kaki extends JavaPlugin {
    public static final Kaki INSTANCE = new Kaki();

    public static List<Long> black; // 黑名单
    public static List<Long> white; // 白名单
    public static List<Long> group; // 启用功能的群组
    public static List<Long> botId; // bot备用列表
    public static long masterId; // 最高管理者id

    boolean roleLock = false; // 角色添加(文件IO)时上锁
    Listener<MessageEvent> mainListener; // 总监听
    HashMap<Long, Boolean> usersLock = new HashMap<>(); // 用户指令锁
    HashMap<String, Integer> fileNum = new HashMap<>(); // 读取的图片数量
    HashMap<Long, DrawStatus> drawStatus; // 每日抽卡记录
    Draw_IO draw_io = new Draw_IO(); // 抽卡记录存取对象
    Queue<String> logMessages = new ArrayDeque<>(); // 日志记录表，默认大小为50，可到logAdd处修改

    private Kaki() {
        super(new JvmPluginDescriptionBuilder("org.TioYae.mirai.Kaki", "1.6.0")
                .author("Tio Yae")
                .build()
        );
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {
        loadConfig();

        drawStatus = draw_io.loadDailyDrawStatus(System.getProperty("user.dir") + "/config/org.kaki/drawStatus.txt");
        if (drawStatus == null) logAdd("读取抽卡数据失败");
    }

    @Override
    public void onEnable() {
        // 主监听
        mainListener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, this::hear);

        // 交互式控制台（开发者专用）
        GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, event -> {
            if (event.getSender().getId() == masterId) {
                boolean at = isAt(event);
                String str = event.getMessage().contentToString();
                if (at || str.startsWith(">") || str.startsWith(" ")) {
                    if (at) str = str.substring(12);
                    else str = str.substring(1);
                    String[] order = str.split(" ");
                    switch (order[0]) {
                        case "重启":
                        case "relogin":
                        case "restart":
                            mainListener.complete();
                            usersLock.clear();
                            mainListener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, this::hear);
                            event.getSubject().sendMessage("重启成功");
                            break;
                        case "日志":
                        case "log":
                        case "error":
                            if (order.length > 1 && order[1].matches("[0-9]+"))
                                logSend(event, Integer.parseInt(order[1]));
                            else
                                logSend(event, 10);
                            break;
                        case "用户锁":
                        case "lock":
                        case "usersLock":
                            event.getSubject().sendMessage("用户锁如下：\n" + usersLock.toString());
                            break;
                        case "黑名单":
                        case "black":
                            event.getSubject().sendMessage("黑名单如下：\n" + black.toString());
                            break;
                        case "白名单":
                        case "white":
                            event.getSubject().sendMessage("白名单如下：\n" + white.toString());
                            break;
                        case "测试":
                        case "test":
                            test(event);
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    // 读取yaml文件配置
    void loadConfig() {
        Config config = null;
        String path = System.getProperty("user.dir") + "/config/org.kaki/config.yml";
        File f = new File(path);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        if (!f.getParentFile().exists()) { // 如果父目录不存在，创建父目录
            if (f.getParentFile().mkdirs())
                logAdd("父目录不存在，创建父目录");
            else
                logAdd("创建父目录失败");
        }
        if (!f.exists()) {
            try {
                if (f.createNewFile()) {
                    logAdd("创建文件: " + path);
                    config = new Config();

                    config.setMasterId(111111L);
                    config.setBotId(Collections.singletonList(222222L));
                    config.setBotPassword(Collections.singletonList("123456"));
                    config.setList_Black(null);
                    config.setList_White(Collections.singletonList(111111L));
                    config.setList_Group(Collections.singletonList(654321L));

                    yaml.dump(config, new FileWriter(path));
                } else logAdd("创建文件: " + path + "失败");
            } catch (IOException e) {
                e.printStackTrace();
                logAdd("文件不存在，创建文件失败");
            }
        }
        try {
            config = yaml.loadAs(new FileReader(path), Config.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logAdd("读取配置失败");
        }

        if (config == null) {
            logAdd("配置为空");
            return;
        }
        masterId = config.getMasterId();
        botId = config.getBotId();
        black = config.getList_Black();
        white = config.getList_White();
        group = config.getList_Group();
    }

    // 判断是否被@
    boolean isAt(MessageEvent event) {
        boolean ans;
        for (long bId : botId) {
            ans = event.getMessage().serializeToMiraiCode().contains("[mirai:at:" + bId + "]");
            if (ans) return true;
        }
        return false;
    }

    // 监听方法
    void hear(MessageEvent event) {
        long id = event.getSender().getId();
        // 限定监听白名单的群
        if (event.getClass().getName().contains("Group") && group != null && !group.contains(event.getSubject().getId()))
            return;
        String content = event.getMessage().contentToString();
        System.out.println(content);

        if (content.startsWith(">") || isAt(event)) {
            // 拦截黑名单
            if (black != null && black.contains(id)) {
                logAdd(id + "在黑名单中");
                event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                return;
            }

            // 获取请求指令操作用户状态
            boolean userLock = usersLock.getOrDefault(id, false);
            System.out.println(id + " lock(outer): " + userLock);
            System.out.println(id + "的状态锁: " + userLock);


            if (content.startsWith(">")) {
                if (content.length() > 1)
                    content = content.substring(1);
                else
                    content = "";
            } else {
                int t = 12;
                if (content.charAt(11) != ' ') t--;
                if (content.length() > t)
                    content = content.substring(t);
                else
                    content = "";
            }
            System.out.println("处理后的内容：" + content);

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
                case "猜原神":
                    // 进程锁
                    if (userLock) {
                        logAdd(id + "正处于指令运行状态");
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    // 群答模式id为群号
                    if ((content.contains("g") || content.contains("G")) && event.getClass().getName().contains("Group")) {
                        id = event.getSubject().getId();
                        user = new User(id);
                        user.group = true;
                    }

                    // 更新进程锁状态
                    changeStatus(id, true);

//                  System.out.println("outer: " + content);
                    title = new String[]{"名称", "星级", "性别", "属性", "武器", "归属"};
                    if (!guess(event, user, title, "原神"))
                        event.getSubject().sendMessage("获取数据失败");
                    break;
                case "猜崩3":
                    // 进程锁
                    if (userLock) {
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    // 群答模式id为群号
                    /*if ((content.contains("g") || content.contains("G")) && event.getClass().getName().contains("Group")) {
                        id = String.valueOf(event.getSubject().getId());
                        user = new User(id);
                        user.group = true;
                    }

                    // 更新进程锁状态
                    changeStatus(id, true);

                    System.out.println("outer: " + content);
                    title = new String[]{"装甲", "角色", "评级", "武器", "归属"};
                    if (!Guess(event, user, title, "崩3"))
                        event.getSubject().sendMessage("获取数据失败");*/
                    break;
                case "添加原神角色":
                    // 进程锁
                    if (userLock) {
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    // 更新进程锁状态
                    changeStatus(id, true);

                    if (white != null && !white.contains(event.getSender().getId())) {
                        logAdd(event.getSender().getId() + "没有添加角色的权限");
                        event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                    } else addRole("原神", event, user);
                    break;
                case "添加崩3角色":
                    // 进程锁
                    if (userLock) {
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    // 更新进程锁状态
                    changeStatus(id, true);

                    if (white != null && !white.contains(event.getSender().getId())) {
                        logAdd(event.getSender().getId() + "没有添加角色的权限");
                        event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                    } else addRole("崩3", event, user);
                    break;
                case "创建原神文件夹":
                    // 进程锁
                    if (userLock) {
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    if (white != null && !white.contains(event.getSender().getId())) {
                        logAdd(event.getSender().getId() + "没有创建文件夹的权限");
                        event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                    } else {
                        if (buildFolder("原神")) event.getSubject().sendMessage("创建文件夹成功");
                        else event.getSubject().sendMessage("创建文件夹失败");
                    }
                    break;
                case "创建崩3文件夹":
                    // 进程锁
                    if (userLock) {
                        event.getSubject().sendMessage("有指令正在运行，取消本次指令操作");
                        break;
                    }

                    if (white != null && !white.contains(event.getSender().getId())) {
                        logAdd(event.getSender().getId() + "没有创建文件夹的权限");
                        event.getSubject().sendMessage("Kaki不想干活啦，去找Tio吧");
                    } else {
                        if (buildFolder("崩3")) event.getSubject().sendMessage("创建文件夹成功");
                        else event.getSubject().sendMessage("创建文件夹失败");
                    }
                    break;
                case "图片重载":
                    fileNum.clear();
                    event.getSubject().sendMessage("图片重载成功");
                    break;
                case "draw":
                case "抽卡":
                    dailyDrawCard(event);
                    break;
                default:
                    System.out.println("default in \">\" order");
                    break;
            }
        }
        respond(event);
    }

    // 添加错误记录
    void logAdd(String err) {
        System.out.println(err);
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (logMessages.size() >= 50) {
            logMessages.poll();
        }
        logMessages.offer(dateFormat.format(date) + "\n" + err);
    }

    // 给开发者发送错误记录
    void logSend(MessageEvent event, int n) {
        int l = logMessages.size();
        if (l == 0) {
            event.getSubject().sendMessage("无错误记录");
            return;
        }
        if (n > l) n = l;
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String t = logMessages.poll();
            if (i != 0) message.append("\n\n");
            message.append(t);
        }
        event.getSubject().sendMessage(message.toString());
    }

    // 更改对应用户的状态
    void changeStatus(long id, boolean lock) {
        if (id == -1) {
            logAdd("修改用户状态时获取id失败");
            return;
        }
        usersLock.remove(id);
        usersLock.put(id, lock);
    }

    // 预设回复
    void respond(MessageEvent event) {
        String content = event.getMessage().contentToString();
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

        boolean at = isAt(event);
        //带称呼或@触发
        if (content.startsWith("Kaki") || content.startsWith("kaki") || at) {
            if (at) {
                int t = 12;
                if (content.charAt(11) != ' ') t--;
                if (content.length() > t)
                    content = content.substring(t);
                else
                    content = "";
            } else {
                if (content.length() > 4)
                    content = content.substring(4);
                else
                    content = "";
            }

            System.out.println(content);

            if (content.equals("") || content.contains("在不在")) {
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
//                        ">猜崩3（未启用）\n" +
                        ">添加原神角色\n" +
//                        ">添加崩3角色（未启用）\n" +
                        ">抽卡\n" +
                        "\n可以通过输入「>帮助 指令名」来获取更多帮助" +
                        "\n在群聊中可以通过「@Kaki」取代「>」符号";
                break;
            case "猜":
            case "猜原神":
                ans = "「猜原神」指令：\n" +
                        "Kaki将会在原神角色库中随机抽取一位角色，每位玩家每轮拥有5次猜测机会 ^_^\n" +
                        "\nPS：有一位彩蛋角色哦~\n" +
                        "\n在指令后追加参数「D」或「d」可在每一次猜测时输出本次猜测角色的信息\n" +
                        "群聊中在指令后追加参数「G」或「g」变成群答模式\n" +
                        "（在「猜崩3」指令启用前，可直接使用「>猜」及其衍生指令快捷调用）";
                break;
            case "猜崩3":
                ans = "「猜崩3」指令：\n" +
                        "Kaki将会在崩坏3装甲库中随机抽取一位角色的一件装甲，每位玩家每轮拥有5次猜测机会 ^_^\n" +
                        "在指令后追加参数「D」或「d」可在每一次猜测时输出本次猜测角色的信息";
                break;
            case "添加原神角色":
                ans = "「添加原神角色」指令：\n" +
                        "字面意思，仅管理员可用";
                break;
            case "添加崩3角色":
                ans = "「添加崩3角色」指令：\n" +
                        "字面意思，仅管理员可用";
                break;
            case "抽卡":
                ans = "「抽卡」指令：\n" +
                        "每位用户每天可以抽一次原神角色，角色数量区间为[0, 3]";
                break;
            case "控制台":
            case "console":
                if (event.getSender().getId() == masterId) {
                    ans = "「控制台指令」：\n" +
                            "重启|relogin|restart: 重启主监听\n" +
                            "日志|log|error: 获取控制台日志消息\n" +
                            "用户锁|lock|usersLock: 查询用户锁哈希表\n" +
                            "黑名单|black: 查看黑名单\n" +
                            "白名单|white: 查看黑名单\n" +
                            "\n仅开发者可用";
                } else {
                    ans = "您没有权限查看控制台指令";
                }
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
        if (game.equals("原神"))
            roleIO = new Role_JOSNIO(System.getProperty("user.dir") + "/config/org.kaki/roleData_Genshin.json");
        else if (game.equals("崩3"))
            roleIO = new Role_JOSNIO(System.getProperty("user.dir") + "/config/org.kaki/roleData_Honkai.json");
        if (roleIO == null) {
            logAdd("读取角色数据失败");
            return false;
        }
        JSONArray roleData = roleIO.getJsonArray();
        if (roleData == null) {
            logAdd("读取角色数据失败");
            return false;
        }
        int n = roleData.length();
        for (int i = 0; i < n; i++) {
            String name = roleData.getJSONObject(i).getString("名称");
            File file = new File(System.getProperty("user.dir") + "/config/org.kaki/picture/" + game + "/" + name);
            if (!file.exists()) {
                if (file.mkdirs()) {
                    logAdd("已创建文件夹：" + name);
                } else return false;
            }
        }
        return true;
    }

    // 加载图片数量
    void loadImage(String name, String game) {
        File[] files = new File(System.getProperty("user.dir") + "/config/org.kaki/picture/" + game + "/" + name).listFiles();
        if (files == null) {
            fileNum.put(name, 0);
            logAdd("未找到" + game + name + "的图片");
        } else fileNum.put(name, files.length);
    }

    // 猜
    boolean guess(MessageEvent event, User person, String[] title, String game) {
        String g = "";
        if (game.equals("原神")) {
            g = "一位角色";
            person.status = new GuessStatus(title, System.getProperty("user.dir") + "/config/org.kaki/roleData_Genshin.json");
        } else if (game.equals("崩3")) {
            g = "一件装甲";
            person.status = new GuessStatus(title, System.getProperty("user.dir") + "/config/org.kaki/roleData_Honkai.json");
        }
        String[] answer = person.status.roleGuess.getAnswer();
        boolean details = event.getMessage().toString().contains("D") || event.getMessage().toString().contains("d");
        if (answer == null) {
            logAdd("读取角色数据失败");
            // 更新进程锁状态
            changeStatus(person.id, false);
            return false;
        }

        if (!fileNum.containsKey(answer[0])) loadImage(answer[0], game);
        System.out.println(fileNum);
        int imageNum = fileNum.get(answer[0]);
        Random random = new Random();
        int i = random.nextInt(imageNum) + 1;
        String pathname = System.getProperty("user.dir") + "/config/org.kaki/picture/" + game + "/" + answer[0] + "/" + i;
        File f = new File(pathname + ".jpg");
        if (!f.exists()) // 尝试jpg后缀不对就是png后缀
            f = new File(pathname + ".png");

        StringBuilder str = new StringBuilder();
        for (int j = 0; j < answer.length; j++) {
            str.append("\n").append(title[j]).append("：").append(answer[j]);
        }
        MessageChain role;
        if (f.exists()) {
            Image image = net.mamoe.mirai.contact.Contact.uploadImage(event.getSubject(), f);
            role = new MessageChainBuilder().append(image).append(str).build();
        } else {
            logAdd("读取图片失败：" + answer[0] + i);
            role = new MessageChainBuilder().append(str).build();
        }

        if (person.status.n < 0) {
            if (person.group) { // 群答模式
                event.getSubject().sendMessage("「群答模式」\n已随机抽取" + g + "，请开始你们的猜测 ^_^");
            } else if (event.getClass().getName().contains("Group")) { // 群组消息要@
                At at = new At(event.getSender().getId());
                MessageChain message = new MessageChainBuilder().append(at).append("\n").append("已随机抽取").append(g).append("，请开始您的猜测 ^_^").build();
                event.getSubject().sendMessage(message);
            } else if (event.getClass().getName().contains("Friend")) // 好友消息不@
                event.getSubject().sendMessage("已随机抽取" + g + "，请开始您的猜测 ^_^");

            long delay = 60000L;
            person.timer = new Timer();
            person.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timeout(person, event, role, answer[0]);
                }
            }, delay);
            person.status.n = 0; // 用MessageEvent时不需要跳过指令句

            person.listener = GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, e -> {
                if (e.getMessage().contentToString().contains("取消") || e.getMessage().contentToString().contains("不玩了")) {
                    if (person.id == e.getSender().getId() || person.group) { // 对应的人或者是群答
                        person.listener.complete();
                        e.getSubject().sendMessage("操作取消。正确答案是「" + answer[0] + "」");
                        e.getSubject().sendMessage(role);

                        // 更新进程锁状态
                        changeStatus(person.id, false);
                        person.timer.cancel(); // 关闭定时器
                        person.timer = null; // 定时器置空
                        return;
                    }
                }

                long id = e.getSender().getId();
                // 判断为同一个人的消息才执行 (或群答)
                if (person.id == id || person.group) {
                    String c = e.getMessage().contentToString();
                    System.out.println(person.status.n + ": inner: " + c);

                    /*if (person.status.n == 0) { // 跳过指令句 // 用FriendMessageEvent时需要跳过
                        person.status.n++;
                    } else */
                    if (person.status.n < 5 && !c.startsWith(">") && !isAt(e)) { // 输入指令语句或@不执行猜的动作
                        if (e.getClass().getName().contains("Group") && !person.group) { // 群组消息要@(非群答模式)
                            At at = new At(e.getSender().getId());
                            String ans = person.status.guess(c); // 获取猜的结果
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
                            if (ans == null) {
                                logAdd("输入不规范或读取角色数据失败");
                                return; // 输入不规范或抽取角色失败返回null
                            }

                            // 当输入规范时，重置计时器
                            if (person.listener == null) return; // 若定时器已触发，结束监听
                            person.timer.cancel(); // 关闭定时器
                            // 重置定时器
                            person.timer = new Timer();
                            person.timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    timeout(person, e, role, answer[0]);
                                }
                            }, delay);

                            MessageChain message = new MessageChainBuilder().append(at).append("\n").append(ans).build();
                            // 防止消息轰炸
                            if (person.status.n >= 1 && person.status.n <= 5) e.getSubject().sendMessage(message);
                        } else if (e.getClass().getName().contains("Friend") || person.group) { // 好友消息不@(群答模式)
                            String ans = person.status.guess(c); // 获取猜的结果
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
                            if (ans == null) {
                                logAdd("输入不规范或读取角色数据失败");
                                return; // 输入不规范或抽取角色失败返回null
                            }

                            // 当输入规范时，重置计时器
                            if (person.listener == null) return; // 若定时器已触发，结束监听
                            person.timer.cancel(); // 关闭定时器
                            // 重置定时器
                            person.timer = new Timer();
                            person.timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    timeout(person, e, role, answer[0]);
                                }
                            }, delay);

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
                        person.timer.cancel(); // 关闭定时器
                        person.timer = null; // 定时器置空
                    } else if (person.status.n >= 5) {
                        person.status.n = -100;
                        e.getSubject().sendMessage("猜错啦！正确答案是「" + answer[0] + "」");
                        e.getSubject().sendMessage(role);

                        person.listener.complete();

                        // 更新进程锁状态
                        changeStatus(person.id, false);
                        person.timer.cancel(); // 关闭定时器
                        person.timer = null; // 定时器置空
                    }
                }
            });
        }
        return true;
    }

    // 计时器运行内容
    void timeout(User person, MessageEvent event, MessageChain role, String ans) {
        person.status.n = -100;
        if (!person.group && event.getClass().getName().contains("Group")) { // 群组消息且非群答要@
            At at = new At(event.getSender().getId());
            MessageChain message = new MessageChainBuilder().append(at).append("\n").append("应答超时！正确答案是「").append(ans).append("」").build();
            event.getSubject().sendMessage(message);
        } else if (person.group || event.getClass().getName().contains("Friend")) {// 好友消息或群答不@
            event.getSubject().sendMessage("应答超时！正确答案是「" + ans + "」");
        }
        event.getSubject().sendMessage(role); // 发送角色详情

        person.listener.complete(); // 停止监听
        person.listener = null; // 监听置空，用于判断定时器是否已触发

        // 更新进程锁状态
        changeStatus(person.id, false);
        person.timer.cancel(); // 关闭定时器
        person.timer = null; // 定时器置空
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
                    if (e.getSender().getId() == person.id) {
                        roleLock = true; // MessageEvent
                        String c = e.getMessage().contentToString();
                        String[] title = new String[]{"名称", "星级", "性别", "属性", "武器", "归属"};
                        logAdd("添加角色：" + c);
                        String[] arr = c.split("[,，]");
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

                        Role_JOSNIO jsonIO = new Role_JOSNIO(System.getProperty("user.dir") + "/config/org.kaki/roleData_Genshin.json");
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

    // 每日抽卡
    void dailyDrawCard(MessageEvent event) {
        DrawStatus status;
        long id = event.getSender().getId();
        if (drawStatus == null) drawStatus = new HashMap<>();
        if (drawStatus.containsKey(id)) {
            status = drawStatus.get(id);
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            if (!date.equals(status.date))
                status = new DrawStatus(System.getProperty("user.dir") + "/config/org.kaki/roleData_Genshin.json");
        } else
            status = new DrawStatus(System.getProperty("user.dir") + "/config/org.kaki/roleData_Genshin.json");

        List<String> name = status.role;
        drawStatus.put(id, status);
        if (status.num == null) {
            status.num = new HashMap<>();
            for (String s : name) {
                if (!fileNum.containsKey(s)) loadImage(s, "原神");
                int imageNum = fileNum.get(s);
                Random random = new Random();
                int i = random.nextInt(imageNum) + 1;
                status.num.put(s, i);
            }
        }

        MessageChainBuilder messages = new MessageChainBuilder();
        if (event.getClass().getName().contains("Group")) { // 群组消息要@
            At at = new At(event.getSender().getId());
            messages.append(at).append("\n");
        }

        if (name == null) {
            messages.append("恭喜你，什么也没抽到！");
        } else {
            messages.append("恭喜你，抽到以下角色：\n");
            for (String s : name) {
                int i = status.num.get(s);
                String pathname = System.getProperty("user.dir") + "/config/org.kaki/picture/原神/" + s + "/" + i;
                File f = new File(pathname + ".jpg");
                if (!f.exists()) // 尝试jpg后缀不对就是png后缀
                    f = new File(pathname + ".png");

                if (f.exists()) {
                    Image image = net.mamoe.mirai.contact.Contact.uploadImage(event.getSubject(), f);
                    messages.append(s).append(": \n").append(image).append("\n");
                } else {
                    logAdd("读取图片失败：" + s + i);
                    messages.append(s).append(": \n图片读取失败\n");
                }
            }
        }

        event.getSubject().sendMessage(messages.build());
        if (!draw_io.saveDailyDrawStatus(drawStatus)) logAdd("保存抽卡数据失败");
    }

    // 测试
    void test(MessageEvent event) {
        System.out.println(masterId);
        System.out.println(botId);
        event.getSubject().sendMessage("控制台输出");
    }
}