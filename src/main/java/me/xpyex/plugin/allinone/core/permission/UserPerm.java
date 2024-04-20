package me.xpyex.plugin.allinone.core.permission;

import cn.hutool.json.JSONUtil;
import java.io.File;
import java.util.TreeSet;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.module.core.PermManager;
import me.xpyex.plugin.allinone.utils.FileUtil;

@Data
@Accessors(chain = true)
public class UserPerm implements Perms {
    private long id;
    private TreeSet<String> extendsGroups = new TreeSet<>();
    private TreeSet<String> permissions = new TreeSet<>();
    private TreeSet<String> denyPerms = new TreeSet<>();
    private boolean hasAllPerms = false;

    public UserPerm(long id) {
        this.id = id;
        for (GroupPerm groupPerm : PermManager.GROUPS.values()) {
            if (groupPerm.isDefaultGroup()) {
                extendsGroups.add(groupPerm.getName());
            }
        }
    }

    public boolean hasAllPerms() {
        return hasAllPerms;
        //
    }

    private boolean getHasAllPerms() {
        return hasAllPerms();
        // For JavaBean
        // private for hasAllPerms()
    }

    @Override
    @SneakyThrows
    public void save() {
        File f = new File(Module.getModule(PermManager.class).getDataFolder(), "Users/" + getId() + ".json");
        FileUtil.writeFile(f, JSONUtil.toJsonPrettyStr(this));
    }
}
