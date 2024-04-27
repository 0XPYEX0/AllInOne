package me.xpyex.plugin.allinone.module.core;

import cn.hutool.json.JSONUtil;
import java.io.File;
import java.util.HashMap;
import java.util.WeakHashMap;
import lombok.SneakyThrows;
import me.xpyex.plugin.allinone.api.CommandMenu;
import me.xpyex.plugin.allinone.core.command.argument.ArgParser;
import me.xpyex.plugin.allinone.core.command.argument.GroupParser;
import me.xpyex.plugin.allinone.core.command.argument.UserParser;
import me.xpyex.plugin.allinone.core.module.CoreModule;
import me.xpyex.plugin.allinone.core.permission.GroupPerm;
import me.xpyex.plugin.allinone.core.permission.Perms;
import me.xpyex.plugin.allinone.core.permission.QGroupPerm;
import me.xpyex.plugin.allinone.core.permission.UserPerm;
import me.xpyex.plugin.allinone.utils.FileUtil;
import me.xpyex.plugin.allinone.utils.StringUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.User;
import org.jetbrains.annotations.Nullable;

public class PermManager extends CoreModule {
    public static final HashMap<String, GroupPerm> GROUPS = new HashMap<>();
    private static final WeakHashMap<Long, UserPerm> USERS = new WeakHashMap<>();
    private static final WeakHashMap<Long, QGroupPerm> QQ_GROUPS = new WeakHashMap<>();
    private static File GROUPS_FOLDER;
    private static File USERS_FOLDER;
    private static File QQ_GROUPS_FOLDER;

    public static boolean hasPerm(User user, String perm, @Nullable MemberPermission adminPass) {
        if (user == null || perm == null || perm.isEmpty()) {
            return false;
        }
        perm = perm.toLowerCase();
        if (adminPass != null && user instanceof Member && ((Member) user).getPermission().getLevel() >= adminPass.getLevel()) {
            return true;
        }
        if (user instanceof Member) {
            QGroupPerm qGroupPerm = getQGroupPerm(((Member) user).getGroup().getId());
            if (Perms.getLowerCaseSet(qGroupPerm.getDenyPerms()).contains(perm)) {
                return false;
            }
            if (Perms.getLowerCaseSet(qGroupPerm.getPermissions()).contains(perm)) {
                return true;
            }
            for (String groupName : qGroupPerm.getExtendsGroups()) {
                if (GROUPS.containsKey(groupName)) {
                    if (GROUPS.get(groupName).getPermissions().contains(perm)) {
                        return true;
                    }
                }
            }
        }
        return hasPerm(user.getId(), perm);
    }

    public static boolean hasPerm(long id, String perm) {
        UserPerm userPerm = getUserPerm(id);
        if (userPerm.getDenyPerms().contains(perm)) {
            return false;
        }
        if (userPerm.hasAllPerms())
            return true;
        if (userPerm.getPermissions().contains(perm)) {
            return true;
        }
        for (String groupName : userPerm.getExtendsGroups()) {
            if (GROUPS.containsKey(groupName)) {
                if (GROUPS.get(groupName).getPermissions().contains(perm)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SneakyThrows
    private static UserPerm getUserPerm(long id) {
        if (!USERS.containsKey(id)) {
            File userFile = new File(USERS_FOLDER, id + ".json");
            if (!userFile.exists()) {
                USERS.put(id, new UserPerm(id));
            } else {
                USERS.put(id, JSONUtil.toBean(FileUtil.readFile(userFile), UserPerm.class));
            }
        }
        return USERS.get(id);
    }

    @SneakyThrows
    private static QGroupPerm getQGroupPerm(long id) {
        if (!QQ_GROUPS.containsKey(id)) {
            File qGroupFile = new File(QQ_GROUPS_FOLDER, id + ".json");
            if (!qGroupFile.exists()) {
                QQ_GROUPS.put(id, new QGroupPerm(id));
            } else {
                QQ_GROUPS.put(id, JSONUtil.toBean(FileUtil.readFile(qGroupFile), QGroupPerm.class));
            }
        }
        return QQ_GROUPS.get(id);
    }

    public void reload() {
        GROUPS_FOLDER = new File(getDataFolder(), "Groups");
        USERS_FOLDER = new File(getDataFolder(), "Users");
        QQ_GROUPS_FOLDER = new File(getDataFolder(), "QQGroups");
        GROUPS_FOLDER.mkdirs();
        USERS_FOLDER.mkdirs();
        QQ_GROUPS_FOLDER.mkdirs();

        GROUPS.clear();
        USERS.clear();
        QQ_GROUPS.clear();

        for (File file : GROUPS_FOLDER.listFiles()) {
            if (!file.getName().endsWith(".json")) {
                continue;
            }
            try {
                String groupName = file.getName().substring(0, file.getName().lastIndexOf(".json"));
                GROUPS.put(groupName, JSONUtil.toBean(FileUtil.readFile(file), GroupPerm.class));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void register() {
        reload();

        registerCommand(Contact.class, (source, sender, label, args) -> {
            if (!sender.hasPerm(getName() + ".admin")) {
                source.sendMessage("你没有权限");
                return;
            }
            if (args.length == 0) {
                new CommandMenu(label)
                    .add("newGroup <Name> <isDefault>", "创建新的权限组")
                    .add("reload", "尝试重载所有权限内容")
                    .add("set <Group|User|QQGroup> <Name|ID> <Perm> <State>", "给<组|用户|QQ群>修改权限状态")
                    .add("setAll <UserID> <true/false>", "给予用户所有权限")
                    .send(source);
                return;
            }
            if ("set".equalsIgnoreCase(args[0])) {
                if (args.length < 5) {
                    source.sendMessage("参数不足");
                    return;
                }
                String type;
                if ("group".equalsIgnoreCase(args[1])) {
                    type = "组";
                } else if ("user".equalsIgnoreCase(args[1])) {
                    type = "用户";
                } else if ("qGroup".equalsIgnoreCase(args[1])) {
                    type = "群";
                } else {
                    source.sendMessage("参数错误: " + args[1]);
                    return;
                }
                String id = args[2];
                String perm = args[3].toLowerCase();
                int state = Integer.parseInt(args[4]);
                Perms permInstance = switch (type) {
                    case "组" -> GROUPS.get(id);
                    case "用户" -> getUserPerm(ArgParser.of(UserParser.class).getParsedId(id));
                    case "群" -> getQGroupPerm(ArgParser.of(GroupParser.class).getParsedId(id));
                    default -> null;
                };
                if (permInstance == null) {
                    source.sendMessage("错误: <" + type + " " + id + "> 不存在");
                    return;
                }
                if (switch (state) {
                    case -1 -> permInstance.getPermissions().remove(perm) | permInstance.getDenyPerms().add(perm);
                    case 0 -> permInstance.getDenyPerms().remove(perm) | permInstance.getPermissions().remove(perm);
                    case 1 -> permInstance.getPermissions().add(perm) | permInstance.getDenyPerms().remove(perm);
                    default -> false;
                }) {
                    permInstance.save();
                    source.sendMessage("设置 <" + type + " " + id + "> 的权限 <" + perm + "> 状态为 <" + state + ">");
                } else {
                    source.sendMessage("设置 <" + type + " " + id + "> 的权限 <" + perm + "> 失败: 无变化");
                }
            } else if (StringUtil.equalsIgnoreCaseOr(args[0], "setAll", "op")) {
                if (!sender.hasPerm(getName() + ".setOp")) {
                    source.sendMessage("你没有权限");
                    return;
                }
                if (args.length < 3) {
                    source.sendMessage("参数不足");
                    return;
                }
                long id = Long.parseLong(args[1]);
                boolean newState = "true".equalsIgnoreCase(args[2]);
                getUserPerm(id).setHasAllPerms(newState).save();
                source.sendMessage("已设定 " + id + " 管理员权限为 " + newState);
            } else if ("reload".equalsIgnoreCase(args[0])) {
                reload();
                source.sendMessage("尝试重载");
            } else if ("newGroup".equalsIgnoreCase(args[0])) {
                if (args.length < 3) {
                    source.sendMessage("参数不足");
                    return;
                }
                File f = new File(GROUPS_FOLDER, args[1] + ".json");
                if (f.exists()) {
                    source.sendMessage("已存在同名权限组: " + args[1]);
                    return;
                }
                FileUtil.writeFile(f, JSONUtil.toJsonPrettyStr(new GroupPerm(args[1]).setDefaultGroup("true".equalsIgnoreCase(args[2]))));
                reload();
                source.sendMessage("成功创建组: " + args[1]);
            }
        }, "permission", "permissions", "perm", "perms");
    }
}
