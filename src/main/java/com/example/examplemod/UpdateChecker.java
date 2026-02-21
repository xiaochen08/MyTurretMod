package com.example.examplemod;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// ✨ @Mod.EventBusSubscriber 标签会自动把这个类注册到 Forge 的事件总线里
// 注意：这里的 "examplemod" 必须是你的模组ID (MODID)，请确保拼写正确！
@Mod.EventBusSubscriber(modid = "examplemod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UpdateChecker {

    // 1. 设置当前的模组版本号 (记得每次打包新版本前，把这里也改一下)
    public static final String CURRENT_VERSION = "1.4.0.61";

    // 2. 刚才获取的 Gitee/GitHub "原始数据(Raw)" 网址
    public static final String UPDATE_URL = "https://gitee.com/chen-xuan-zzy/example-mod-updates/raw/master/version.txt"; // ⚠️ 请替换为你的真实网址！

    // 3. 玩家点击更新后，跳转的下载页面 (比如 CurseForge, MCBBS, 或者网盘链接)
    public static final String DOWNLOAD_URL = "https://pan.baidu.com/s/1aC7501jvCwjHCGveGVH0qQ?pwd=8888"; // ⚠️ 请替换！

    // 监听玩家登录游戏/进入存档的事件
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // 我们只需要在服务端发送消息，客户端就能收到，防止发两遍
        if (player.level().isClientSide) return;

        // 👨‍🏫 导师小课堂：为什么要用 new Thread()？
        // 因为网络请求可能会卡顿！如果不放到新线程（雇佣通讯兵）里，
        // 一旦玩家网络不好，整个 Minecraft 游戏画面就会卡死在加载界面。
        // 放到新线程里，游戏会正常进入，通讯兵在后台慢慢查，查到了再发消息！
        new Thread(() -> {
            try {
                // 模拟延迟一小会儿，等玩家彻底进游戏看清画面了再发消息 (3000毫秒 = 3秒)
                Thread.sleep(3000);

                // 发起网络请求读取小纸条
                URL url = new URL(UPDATE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 最多等5秒，连不上就算了
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = reader.readLine(); // 读取第一行内容
                reader.close();

                // 检查拿到的内容是不是空的
                if (latestVersion != null && !latestVersion.trim().isEmpty()) {
                    latestVersion = latestVersion.trim(); // 清除开头结尾的空格和换行符

                    // 核心逻辑：如果网上的版本号，和本地的版本号不一样
                    if (!CURRENT_VERSION.equals(latestVersion)) {

                        // 组装一条华丽的聊天信息
                        Component message = Component.literal("§e[战术系统] 侦测到模组新版本！当前: v" + CURRENT_VERSION + " -> 最新: v" + latestVersion + " ")
                                .append(Component.literal("§a§n[点击这里前往下载更新]")
                                        // 赋予点击事件：点击后在浏览器打开下载链接
                                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL))));

                        // 发送给刚上线的玩家
                        player.sendSystemMessage(message);
                    }
                }
            } catch (Exception e) {
                // 如果断网了或者连不上，就不打扰玩家，只在后台日志里说一声
                System.out.println("[UpdateChecker] 检查更新失败: " + e.getMessage());
            }
        }).start(); // 启动通讯兵！
    }
}
