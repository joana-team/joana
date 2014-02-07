
package android.view;

import android.view.ContextMenu;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.MenuItem;
import android.content.ComponentName;
import android.content.Intent;
import android.view.SubMenu;
import android.view.KeyEvent;

public class DummyContextMenu implements ContextMenu {
    public DummyContextMenu() {

    }

    public void   clearHeader() { }
    public ContextMenu    setHeaderIcon(Drawable icon) { return this; }
    public ContextMenu    setHeaderIcon(int iconRes) { return this; }
    public ContextMenu    setHeaderTitle(CharSequence title) { return this; }
    public ContextMenu    setHeaderTitle(int titleRes) { return this; }
    public ContextMenu    setHeaderView(View view) { return this; }


    public MenuItem   add(CharSequence title) { return null; }
    public  MenuItem   add(int groupId, int itemId, int order, int titleRes) { return null; }
    public MenuItem   add(int titleRes)  { return null; }
    public MenuItem   add(int groupId, int itemId, int order, CharSequence title)  { return null; }
    public int    addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems)  { return 0; }
    public SubMenu    addSubMenu(int groupId, int itemId, int order, CharSequence title) { return null; }
    public SubMenu    addSubMenu(int groupId, int itemId, int order, int titleRes) { return null; }
    public SubMenu    addSubMenu(CharSequence title) { return null; }
    public SubMenu    addSubMenu(int titleRes) { return null; }
    public void   clear() {}
    public void   close() {}
    public MenuItem   findItem(int id) { return null; }
    public MenuItem   getItem(int index) { return null; }
    public boolean    hasVisibleItems() { return false; }
    public boolean    isShortcutKey(int keyCode, KeyEvent event) { return false; }
    public boolean    performIdentifierAction(int id, int flags) { return false; }
    public boolean    performShortcut(int keyCode, KeyEvent event, int flags) { return false; }
    public void   removeGroup(int groupId) {}
    public void   removeItem(int id) {}
    public void   setGroupCheckable(int group, boolean checkable, boolean exclusive) {}
    public void   setGroupEnabled(int group, boolean enabled) {}
    public void   setGroupVisible(int group, boolean visible) {}
    public void   setQwertyMode(boolean isQwerty) {}
    public int    size() { return 0; }

}
