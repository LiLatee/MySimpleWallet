package com.example.marcin.mysimplewallet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;
import android.widget.TableRow;

// Class needed to get info about view which launched context view.
public class TableRowWithContextMenuInfo extends TableRow
{
    public TableRowWithContextMenuInfo(Context context) {
        super(context);
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return new TableRowContextMenuInfo(this);
    }

    public TableRowWithContextMenuInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static class TableRowContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public TableRowContextMenuInfo(View targetView) {
            this.targetView = (TableRow) targetView;
        }

        public TableRow targetView;
    }
}
