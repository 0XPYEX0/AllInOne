package me.xpyex.plugin.allinone.core.command;

import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ClassUtil;
import java.util.ArrayList;
import java.util.HashMap;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.mirai.ContactTarget;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.utils.ExceptionUtil;
import me.xpyex.plugin.allinone.utils.MsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;

public class CommandBus {
    private static final ArrayList<Tuple> COMMAND_BUSES = new ArrayList<>();
    private static final HashMap<String, Module> COMMAND_LIST = new HashMap<>();

    public static boolean isCmd(String cmd) {
        return COMMAND_LIST.containsKey(cmd.toLowerCase());
        //
    }

    public static boolean isCmd(Module module, String cmd) {
        return COMMAND_LIST.containsKey(cmd.toLowerCase()) && COMMAND_LIST.get(cmd.toLowerCase()).equals(module);
    }

    public static String[] getCommands(Module module) {
        ArrayList<String> list = new ArrayList<>();
        for (String key : COMMAND_LIST.keySet()) {
            if (COMMAND_LIST.get(key) == module) {
                list.add(key);
            }
        }
        return list.toArray(new String[0]);
    }

    public static <C extends Contact> void takeInBus(Class<C> contactType, Module module, CommandExecutor<C> executor, String... aliases) {
        for (String alias : aliases) {
            COMMAND_LIST.put(alias.toLowerCase(), module);  //注册
        }
        COMMAND_BUSES.add(new Tuple(contactType, module, new Command<>(executor, aliases)));
        //
    }

    public static void callCommands(MessageEvent event, String msg) {
        String cmd = msg.split(" ")[0];
        String[] args = msg.substring(cmd.length()).trim().split(" ");
        if (args.length == 0 || (args.length == 1 && args[0].trim().isEmpty())) {
            args = new String[0];
        }
        for (Tuple commandBus : COMMAND_BUSES) {
            if (ClassUtil.isAssignable(commandBus.get(0), MsgUtil.getRealSender(event).getClass())) {
                Module module = commandBus.get(1);
                if (module.isEnabled()) {
                    if (isCmd(module, cmd.substring(1))) {
                        Command<Contact> command = commandBus.get(2);
                        ArgParser.setParseObj(event.getSender());
                        for (String alias : command.aliases()) {
                            if (alias.equalsIgnoreCase(cmd.substring(1))) {
                                try {
                                    ContactTarget<Contact> contact = new ContactTarget<>(MsgUtil.getRealSender(event));
                                    CommandExecutor.EVENT_POOL.put(contact.getCreatedTime(), event);
                                    command.executor().execute(contact, new ContactTarget<>(event.getSender()), cmd.substring(1), args);
                                } catch (Throwable e) {
                                    ExceptionUtil.handleException(e, false);
                                    MsgUtil.sendMsgToOwner("模块 " + module.getName() + " 在处理命令 " + cmd + " 时出现异常，已被捕获: " + e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
