package com.ww.app.admin.view.vo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.ww.app.admin.entity.SysMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-05-22- 09:13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuTreeNodeVO extends TreeNode<Long> {

    public static List<Tree<Long>> menuTree(List<SysMenu> sysMenuList) {
        if (CollectionUtils.isEmpty(sysMenuList)) {
            return CollUtil.newArrayList();
        }
        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();
        // 自定义属性名 都要默认值的
        treeNodeConfig.setWeightKey("sort");
        // 最大递归深度
        treeNodeConfig.setDeep(3);
        return TreeUtil.build(sysMenuList, 0L, treeNodeConfig,
                (treeNode, tree) -> {
                    tree.setId(treeNode.getId());
                    tree.setParentId(treeNode.getPid());
                    tree.setWeight(treeNode.getSort());
                    tree.setName(treeNode.getName());
                    tree.putExtra("pid", treeNode.getPid());
                    tree.putExtra("type", treeNode.getType().getValue());
                    tree.putExtra("path", treeNode.getUrl());
                    tree.putExtra("authCode", treeNode.getPermission());
                    tree.putExtra("component", treeNode.getComponent());
                    tree.putExtra("status", Boolean.TRUE.equals(treeNode.getValid()) ? 1 : 0);
                    Map<String, Object> meta = ObjectUtils.defaultIfNull(treeNode.getMeta(), new HashMap<>());
                    meta.putIfAbsent("icon", treeNode.getIcon());
                    meta.putIfAbsent("title", treeNode.getName());
                    tree.putExtra("meta", meta);
                }
        );
    }

}
