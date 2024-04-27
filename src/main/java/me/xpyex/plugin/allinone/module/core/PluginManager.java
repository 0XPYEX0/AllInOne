package me.xpyex.plugin.allinone.module.core;

import java.util.Arrays;
import java.util.TreeSet;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.api.MessageBuilder;
import me.xpyex.plugin.allinone.core.command.CommandBus;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.command.argument.ModuleParser;
import me.xpyex.plugin.allinone.core.event.EventBus;
import me.xpyex.plugin.allinone.core.module.CoreModule;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.utils.StringUtil;
import net.mamoe.mirai.contact.Contact;

@SuppressWarnings("unused")
public class PluginManager extends CoreModule {
    @Override
    public void register() {
        registerCommand(Contact.class, ((source, sender, label, args) -> {
            if (sender.hasPerm(getName() + ".use")) {
                if (args.length == 0) {
                    new CommandMenu(label)
                        .add("enable <模块>", "启用该模块")
                        .add("disable <模块>", "禁用该模块")
                        .add("list", "查询所有模块")
                        .add("info <模块>", "查看单个模块的信息")
                        .send(source);
                } else if (StringUtil.equalsIgnoreCaseOr(args[0], "enable", "disable")) {
                    ArgParser.of(ModuleParser.class)
                        .parse(() -> args[1], Module.class)
                        .ifPresentOrElse(module -> {
                            if (module.isCore()) {
                                source.sendMessage("不允许操作核心模块");
                                return;
                            }
                            String mode = "enable".equalsIgnoreCase(args[1]) ? "启用" : "禁用";
                            if ("enable".equalsIgnoreCase(args[1]) ? module.enable() : module.disable()) {
                                source.sendMessage("已" + mode + " " + module.getName() + " 模块");
                            } else {
                                source.sendMessage("模块 " + module.getName() + " 已被" + mode + "，无需重复" + mode);
                            }
                        }, () -> source.sendMessage("模块不存在\n执行 #" + label + " list 查看所有列表"));
                } else if ("list".equalsIgnoreCase(args[0])) {
                    TreeSet<String> list = new TreeSet<>();
                    for (Module loadedModule : Module.LOADED_MODELS.values()) {
                        list.add(loadedModule.getName() + (loadedModule.isDisabled() ? "(未启用)" : ""));
                    }
                    source.sendMessage("所有模块列表: " + list);
                } else if ("info".equalsIgnoreCase(args[0])) {
                    ArgParser.of(ModuleParser.class).parse(() -> args[1], Module.class)
                        .ifPresentOrElse(module -> new MessageBuilder("模块 " + module.getName())
                                                       .plus("是否禁用: " + module.isDisabled())
                                                       .plus("已注册的命令: " + Arrays.toString(CommandBus.getCommands(module)))
                                                       .plus("监听的事件: " + Arrays.toString(EventBus.getEvents(module)))
                                                       .send(source),
                            () -> source.sendMessage("模块不存在\n执行 #" + label + " list 查看所有列表"));
                } else {
                    source.sendMessage("未知子命令");
                }
            } else {
                source.sendMessage("你没有权限");
            }
        }), "pl", "plugin", "module");
    }
}
