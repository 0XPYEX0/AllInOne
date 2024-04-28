package me.xpyex.plugin.allinone.core.module;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import me.xpyex.plugin.allinone.Main;
import me.xpyex.plugin.allinone.api.TryConsumer;
import me.xpyex.plugin.allinone.api.TryRunnable;
import me.xpyex.plugin.allinone.core.command.CommandBus;
import me.xpyex.plugin.allinone.core.command.CommandExecutor;
import me.xpyex.plugin.allinone.core.event.EventBus;
import me.xpyex.plugin.allinone.utils.ExceptionUtil;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import me.xpyex.plugin.allinone.utils.Util;
import me.xpyex.plugin.allinone.utils.ValueUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 这是所有模块的根类，所有模块应继承Module类以实现自动注册及所有管理 <br>
 * <p>
 * 覆写 getName()         方法 - 自定义模块名 <br>
 * 覆写 register()        方法 - 注册 <br>
 * <p>
 * <p>
 * <p>
 * 调用 listenEvent()     方法 - 监听Mirai事件 <br>
 * 调用 registerCommand() 方法 - 注册AllInOne命令 <br>
 * 调用 runTaskLater()    方法 - 延时执行任务 <br>
 * 调用 runTaskTimer()    方法 - 创建循环任务 <br>
 * 调用 disable()         方法 - 禁用本模块 <br>
 * 调用 enable()          方法 - 启用本模块 <br>
 * 调用 info()            方法 - 向控制台发送信息 <br>
 * <p>
 * <p>
 * <p>
 * 获取 disable()         方法 - 禁用模块，并获取结果是否成功 <br>
 * 获取 enable()          方法 - 启用模块，并获取结果是否成功 <br>
 * 获取 getLogger()       方法 - 获取AllInOne的MiraiLogger <br>
 * 获取 isCore()          方法 - 获取该模块是否为核心模块 <br>
 * 获取 isDisabled()      方法 - 获取该模块是否已被禁用 <br>
 * 获取 isEnabled()       方法 - 获取该模块是否已被启用 <br>
 */

public abstract class Module {
    public static final HashMap<String, Module> LOADED_MODELS = new HashMap<>();
    private static final HashSet<Module> DISABLED_MODULES = new HashSet<>();  //使用HashSet是为了避免重复.ArrayList可出现重复值
    private static final HashMap<Module, HashSet<UUID>> TASKS = new HashMap<>();  //使用HashSet是为了避免重复.ArrayList可出现重复值
    private final File configFolder = new File(Main.INSTANCE.getConfigFolder(), getName());
    private final File dataFolder = new File(Main.INSTANCE.getDataFolder(), getName());
    protected boolean DEFAULT_DISABLED = false;

    protected Module() {
        getLogger().info("正在加载 " + getName() + " 模块");

        ValueUtil.mustTrue("模块名不应为空", !getName().trim().isEmpty());
        ValueUtil.mustTrue("已存在使用该名称的模块，不允许重复注册", LOADED_MODELS.get(getName()) == null);

        TASKS.put(this, new HashSet<>());
        try {
            register();
            LOADED_MODELS.put(getName(), this);
        } catch (Throwable e) {
            e.printStackTrace();
            getLogger().error("加载模块 " + getName() + " 时出错: " + e);
            return;
        }
        if (this.DEFAULT_DISABLED) {
            if (!isCore()) {
                this.disable();
                getLogger().info("模块 " + getName() + " 注册时选用默认禁用，已禁用它");
            } else {
                getLogger().info("模块 " + getName() + " 注册时选用默认禁用，但核心模块不允许被禁用");
            }
        }
        getLogger().info("成功加载" + (isCore() ? "核心" : "") + "模块: " + getName());
        getLogger().info(" ");
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <M extends Module> M getModule(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        if (LOADED_MODELS.containsKey(name)) return (M) LOADED_MODELS.get(name);

        for (String s : LOADED_MODELS.keySet()) {
            if (s.equalsIgnoreCase(name)) return (M) LOADED_MODELS.get(s);
        }
        return null;
    }

    @NotNull
    public static <M extends Module> M getModule(Class<M> clazz) {
        if (clazz == null) throw new IllegalArgumentException("参数为null");
        if (!clazz.isAssignableFrom(Module.class)) throw new IllegalArgumentException("参数不是Module的子类");
        if (clazz.equals(Module.class)) throw new IllegalArgumentException("参数不能是Module类本身");
        M module = getModule(clazz.getSimpleName());
        assert module != null;
        return module;
    }

    public abstract void register() throws Throwable;

    public final <C extends Contact> void registerCommand(Class<C> contactType, CommandExecutor<C> exec, String... aliases) {
        for (String s : aliases) {
            if (s == null) throw new IllegalArgumentException("注册命令怎么会混进来一个null？");
            if (s.contains(" ")) {
                throw new IllegalArgumentException("注册的命令不应包含空格，应作为参数判断");
            }
        }
        CommandBus.takeInBus(contactType, this, exec, aliases);
        getLogger().info(getName() + " 模块注册命令: " + Arrays.toString(aliases) + ", 命令监听范围: " + contactType.getSimpleName());
    }

    public final <E extends Event> void listenEvent(Class<E> eventType, TryConsumer<E> listener) {
        EventBus.takeInBus(eventType, this, listener);
        getLogger().info(getName() + " 模块注册监听事件: " + eventType.getSimpleName());
    }

    @NotNull
    public String getName() {
        return this.getClass().getSimpleName();
        //
    }

    public final boolean disable() {
        return DISABLED_MODULES.add(this);
        //
    }

    public final boolean enable() {
        return DISABLED_MODULES.remove(this);
        //
    }

    public final <T> T info(T obj) {
        getLogger().info("[" + getName() + "] " + obj);
        return obj;
    }

    public final void info(Throwable e) {
        getLogger().info("[" + getName() + "]", e);
        //
    }

    public final <T> T debug(T obj) {
        getLogger().debug("[" + getName() + "] " + obj);
        return obj;
    }

    public final void debug(Throwable e) {
        getLogger().debug("[" + getName() + "]", e);
        //
    }

    @SuppressWarnings("unchecked")
    public final <C extends Contact> C getRealSender(MessageEvent event) {
        return (C) MsgUtil.getRealSender(event);
        //
    }

    public final void autoSendMsg(MessageEvent event, Message msg) {
        if (msg == null) return;

        getRealSender(event).sendMessage(msg);
    }

    public final void autoSendMsg(MessageEvent event, String msg) {
        if (msg == null || msg.isEmpty()) return;

        autoSendMsg(event, new PlainText(msg));
    }

    public final void runTaskLater(TryRunnable r, long seconds) {
        new Thread(() -> {
            UUID uuid = UUID.randomUUID();
            TASKS.get(this).add(uuid);
            if (seconds > 0L) {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
            }

            if (!TASKS.get(this).contains(uuid)) {
                return;
            }
            if (isDisabled()) {
                return;
            }

            try {
                r.run();
            } catch (Throwable e) {
                handleException(e);
            }
        }, "AllInOne-Task-" + this.getName()).start();
    }

    public final UUID runTaskTimer(TryRunnable r, long repeatPeriodSeconds) {
        return runTaskTimer(r, repeatPeriodSeconds, 0);
        //
    }

    public final UUID runTaskTimer(TryRunnable r, long repeatPeriodSeconds, long waitSeconds) {
        if (r == null) {
            throw new IllegalArgumentException("执行的任务为空");
        }
        if (repeatPeriodSeconds <= 0) {
            throw new IllegalArgumentException("周期为0将堵塞任务线程");
        }
        if (waitSeconds < 0) {
            throw new IllegalArgumentException("你可以不填这个参数的，谢谢");
        }
        UUID uuid = UUID.randomUUID();
        TASKS.get(this).add(uuid);
        info("注册了定时任务. 间隔: " + repeatPeriodSeconds + " ，等待: " + waitSeconds + " ，UUID: " + uuid);

        runTaskLater(() -> {
            while (TASKS.get(this).contains(uuid)) {
                if (this.isEnabled()) {
                    try {
                        r.run();
                    } catch (Throwable e) {
                        handleException(e);
                    }
                }

                try {
                    Thread.sleep(repeatPeriodSeconds * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, waitSeconds);
        return uuid;
    }

    public final boolean shutdownRepeatTask(UUID uuid) {
        if (uuid == null) return false;

        return TASKS.get(this).remove(uuid);
    }

    public final Bot getBot() {
        return Bot.getInstances().get(0);
        //
    }

    public final void handleException(Throwable e) {
        ExceptionUtil.handleException(e);
        //
    }

    public final void handleException(Throwable e, Event event) {
        ExceptionUtil.handleException(e, event);
        //
    }

    public final void handleException(Throwable e, boolean noticeOwner, Event event) {
        ExceptionUtil.handleException(e, noticeOwner, event);
        //
    }

    public final void handleException(Throwable e, boolean noticeOwner) {
        ExceptionUtil.handleException(e, noticeOwner);
        //
    }

    public final String getPlainText(MessageChain message) {
        return MsgUtil.getPlainText(message);
        //
    }

    public final void sendMsgToOwner(String msg) {
        if (msg == null || msg.isEmpty()) return;

        sendMsgToOwner(new PlainText(msg));
    }

    public final void sendMsgToOwner(Message msg) {
        if (msg == null) return;

        getBot().getFriend(Util.OWNER_ID).sendMessage(msg);
    }

    private final MiraiLogger getLogger() {
        return Main.LOGGER;
        //
    }

    public final boolean isDisabled() {
        return DISABLED_MODULES.contains(this);
        //
    }

    public final boolean isEnabled() {
        return LOADED_MODELS.containsKey(getName()) && !DISABLED_MODULES.contains(this);
        //
    }

    public final boolean isCore() {
        return this instanceof CoreModule;
        //
    }

    public File getDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        ValueUtil.mustTrue("该位置已有同名文件，且非文件夹！", dataFolder.isDirectory());
        return dataFolder;
    }

    public File getConfigFolder() {
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        ValueUtil.mustTrue("该位置已有同名文件，且非文件夹！", configFolder.isDirectory());
        return configFolder;
    }

    public <E extends Event> void executeOnce(Class<E> eventType, TryConsumer<E> executor) {
        EventBus.executeOnce(eventType, executor);
        getLogger().info(getName() + " 模块 注册了一次性任务: 监听 " + eventType.getSimpleName() + " 事件");
    }
}
