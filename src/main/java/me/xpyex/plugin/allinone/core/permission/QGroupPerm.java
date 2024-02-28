package me.xpyex.plugin.allinone.core.permission;

import cn.hutool.json.JSONUtil;
import java.io.File;
import java.util.ArrayList;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.xpyex.plugin.allinone.core.module.Module;
import me.xpyex.plugin.allinone.module.core.PermManager;
import me.xpyex.plugin.allinone.utils.FileUtil;

@Data
@Accessors(chain = true)
public class QGroupPerm implements Perms {
    private long groupID;
    private ArrayList<String> permissions = new ArrayList<>();
    private ArrayList<String> denyPerms = new ArrayList<>();
    private ArrayList<String> extendsGroups = new ArrayList<>();  //内容是GroupPerm

    public QGroupPerm(long groupID) {
        this.groupID = groupID;
        for (GroupPerm groupPerm : PermManager.GROUPS.values()) {
            if (groupPerm.isDefaultGroup()) {
                extendsGroups.add(groupPerm.getName());
            }
        }
    }

    @Override
    @SneakyThrows
    public void save() {
        File f = new File(Module.getModule(PermManager.class).getDataFolder(), "QQGroups/" + groupID + ".json");
        FileUtil.writeFile(f, JSONUtil.toJsonPrettyStr(this));
    }
}
