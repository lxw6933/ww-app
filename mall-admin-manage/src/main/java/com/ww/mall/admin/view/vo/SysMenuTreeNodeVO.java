package com.ww.mall.admin.view.vo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.enums.SysMenuType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-22- 09:13
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuTreeNodeVO extends TreeNode<Long> {

    /**
     * 菜单类型
     */
    private SysMenuType type;

    /**
     * URL地址
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    public SysMenuTreeNodeVO(Long id, Long pid, String name, Integer sort, SysMenuType type, String url, String icon) {
        super(id, pid, name, sort);
        this.type = type;
        this.url = url;
        this.icon = icon;
    }

    public static List<Tree<Long>> menuTree(List<SysMenu> sysMenuList) {
        if (CollectionUtils.isEmpty(sysMenuList)) {
            return CollUtil.newArrayList();
        }
        List<SysMenuTreeNodeVO> nodeList = CollUtil.newArrayList();
        sysMenuList.forEach(menu -> {
            SysMenuTreeNodeVO node = new SysMenuTreeNodeVO(menu.getId(), menu.getPid(), menu.getName(), menu.getSort(), menu.getType(), menu.getUrl(), menu.getIcon());
            nodeList.add(node);
        });
        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();
        // 自定义属性名 都要默认值的
        treeNodeConfig.setWeightKey("sort");
        // 最大递归深度
        treeNodeConfig.setDeep(3);
        return TreeUtil.build(nodeList, 0L, treeNodeConfig,
                (treeNode, tree) -> {
                    tree.setId(treeNode.getId());
                    tree.setParentId(treeNode.getParentId());
                    tree.setWeight(treeNode.getWeight());
                    tree.setName(treeNode.getName());
                    tree.putExtra("type", treeNode.getType());
                    tree.putExtra("url", treeNode.getUrl());
                    tree.putExtra("icon", treeNode.getIcon());
                }
        );
    }

}
