package org.pb.android.geomap3d.widget;

import android.util.Log;

import org.androidannotations.annotations.EBean;

@EBean(scope = EBean.Scope.Singleton)
public class WidgetManager {

    private Widget widget;

    public void setWidgetForInitiation(Widget widget, WidgetConfiguration widgetConfiguration) {
        this.widget = widget;

        if (!widget.isInitialized()) {
            Log.v("WidgetManager", "start widget initiation for widget: " + widget.toString());
            widget.initWidget(widgetConfiguration);
        } else {
            Log.v("WidgetManager", "update widget configuration. (" + widget.toString() + ")");
            widget.updateWidget(widgetConfiguration);

        }
    }

    public Widget getWidget() {
        return widget;
    }
}
