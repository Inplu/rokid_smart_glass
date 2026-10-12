package com.rokid.glass.component;

import androidx.annotation.Keep;

@Keep
public class MenuItem {

    private static final int PAGE_MAX_SIZE = 3;

    // 菜单名称
    // Menu name.
    private String menuName;
    /**
     * 菜单类型：
     * Menu type:
     */
    private String menuType;
    // 未选中的本地资源图片
    // Local image resource for the unselected state.
    private int unCheckIconId;
    // 选中的本地资源图片
    // Local image resource for the selected state.
    private int checkedIconId;

    // 菜单序号图片
    // Menu sequence number image.
    private int menuNumId;
    // 是否有新消息，用于标识红点
    // Whether there is a new message, used to indicate a red dot.
    private boolean hasNewMessage;
    // 是否是选中项
    // Whether this item is selected.
    private boolean checked;

    // 未选中的菜单项背景图片ID
    // Background image resource ID for the unselected menu item.
    private int unCheckItemBgId;
    // 选中的菜单项背景图片ID
    // Background image resource ID for the selected menu item.
    private int checkedItemBgId;

    /**
     * MainActivity 中固定
     * Fixed in MainActivity.
     * @param menuName
     * @param menuType
     * @param iconDefaultRecourseId
     * @param iconFocusRecourseId
     * @param checked
     * @param hasNewMessage
     */
    public MenuItem(String menuName, String menuType, int iconDefaultRecourseId, int iconFocusRecourseId,int itemBgDefaultResId,int itemBgFocusResId, boolean checked, boolean hasNewMessage) {
        this.menuName = menuName;
        this.menuType = menuType;
        this.unCheckIconId = iconDefaultRecourseId;
        this.checkedIconId = iconFocusRecourseId;
        this.checkedItemBgId = itemBgFocusResId;
        this.unCheckItemBgId = itemBgDefaultResId;
        this.checked = checked;
        this.hasNewMessage = hasNewMessage;
    }

    public MenuItem(String menuName, String menuType, int iconDefaultRecourseId, int iconFocusRecourseId, int itemBgDefaultResId,int itemBgFocusResId,boolean checked) {
        this.menuName = menuName;
        this.menuType = menuType;
        this.unCheckIconId = iconDefaultRecourseId;
        this.checkedIconId = iconFocusRecourseId;
        this.checkedItemBgId = itemBgFocusResId;
        this.unCheckItemBgId = itemBgDefaultResId;
        this.checked = checked;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getUnCheckIconId() {
        return unCheckIconId;
    }

    public int getCheckedIconId() {
        return checkedIconId;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isHasNewMessage() {
        return hasNewMessage;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setHasNewMessage(boolean hasNewMessage) {
        this.hasNewMessage = hasNewMessage;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getMenuNumId() {
        return menuNumId;
    }

    public int getUnCheckItemBgId() {
        return unCheckItemBgId;
    }

    public void setUnCheckItemBgId(int unCheckItemBgId) {
        this.unCheckItemBgId = unCheckItemBgId;
    }

    public int getCheckedItemBgId() {
        return checkedItemBgId;
    }

    public void setCheckedItemBgId(int checkedItemBgId) {
        this.checkedItemBgId = checkedItemBgId;
    }
}
