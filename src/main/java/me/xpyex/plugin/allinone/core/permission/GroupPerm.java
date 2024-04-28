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

@Accessors(chain = true)
@Data
public class GroupPerm implements Perms {
    private String name;
    private TreeSet<String> permissions = new TreeSet<>();
    private TreeSet<String> denyPerms = new TreeSet<>();
    private boolean isDefaultGroup = false;

    public GroupPerm(String name) {
        this.name = name;
        //
    }

    @Override
    @SneakyThrows
    public void save() {
        File f = new File(Module.getModule(PermManager.class).getDataFolder(), "Groups/" + name + ".json");
        FileUtil.writeFile(f, JSONUtil.toJsonPrettyStr(this));
    }
}
