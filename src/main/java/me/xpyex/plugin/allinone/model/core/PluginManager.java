package me.xpyex.plugin.allinone.model.core;

import java.util.TreeSet;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.core.CoreModel;
import me.xpyex.plugin.allinone.core.Model;
import net.mamoe.mirai.contact.Contact;

public class PluginManager extends CoreModel {
    @Override
    public void register() {
        registerCommand(Contact.class, ((source, sender, label, args) -> {
            if (sender.getId() == 1723275529) {
                if (args.length == 0) {
                    CommandMenu menu = new CommandMenu(label)
                            .add("enable <模块>", "启用该模块")
                            .add("disable <模块>", "禁用该模块")
                            .add("list", "查询所有模块");
                    source.sendMessage(menu.toString());
                } else if (args[0].equalsIgnoreCase("disable")) {
                    if (args.length == 1) {
                        source.sendMessage("参数不足");
                        return;
                    }
                    Model target = Model.getModel(args[1]);
                    if (target == null) {
                        source.sendMessage("模块不存在\n执行 #" + label + " list 查看所有列表");
                        return;
                    }
                    if (target instanceof CoreModel) {
                        source.sendMessage("不允许操作核心模块");
                        return;
                    }
                    if (target.disable()) {
                        source.sendMessage("已禁用 " + target.getName() + " 模块");
                    } else {
                        source.sendMessage("模块 " + target.getName() + " 已被禁用，无需重复禁用");
                    }
                } else if (args[0].equalsIgnoreCase("enable")) {
                    if (args.length == 1) {
                        source.sendMessage("参数不足");
                        return;
                    }
                    Model target = Model.getModel(args[1]);
                    if (target == null) {
                        source.sendMessage("模块不存在\n执行 #" + label + " list 查看所有列表");
                        return;
                    }
                    if (target instanceof CoreModel) {
                        source.sendMessage("不允许操作核心模块");
                        return;
                    }
                    if (target.enable()) {
                        source.sendMessage("已启用 " + target.getName() + " 模块");
                    } else {
                        source.sendMessage("模块 " + target.getName() + " 已被启用，无需重复启用");
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    TreeSet<String> list = new TreeSet<>();
                    for (Model loadedModel : Model.LOADED_MODELS.values()) {
                        list.add(Model.DISABLED_MODELS.contains(loadedModel) ? loadedModel.getName() + "(未启用)" : loadedModel.getName());
                    }
                    source.sendMessage("所有模块列表: " + list);
                } else {
                    source.sendMessage("未知子命令");
                }
            } else {
                source.sendMessage("你没有权限");
            }
        }), "pl", "plugin");
    }
}
