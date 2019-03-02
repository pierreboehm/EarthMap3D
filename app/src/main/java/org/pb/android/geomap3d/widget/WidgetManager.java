package org.pb.android.geomap3d.widget;

import android.util.Log;

import org.androidannotations.annotations.EBean;

@EBean(scope = EBean.Scope.Singleton)
public class WidgetManager {

    private Widget widget;

    public void setWidgetForInitiation(Widget widget) {
        this.widget = widget;

        if (!widget.isInitialized()) {
            Log.v("WidgetManager", "start widget initiation for widget: " + widget.toString());
            widget.initWidget();
        } else {
            Log.v("WidgetManager", "widget almost initiated. (" + widget.toString() + ")");
        }
    }

    public Widget getWidget() {
        return widget;
    }
}
