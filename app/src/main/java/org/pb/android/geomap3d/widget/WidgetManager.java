package org.pb.android.geomap3d.widget;

import android.util.Log;

import org.androidannotations.annotations.EBean;

@EBean(scope = EBean.Scope.Singleton)
public class WidgetManager {

    private static final String TAG = WidgetManager.class.getSimpleName();
    private Widget widget;

    public void setWidgetForInitiationOrUpdate(Widget widget, WidgetConfiguration widgetConfiguration) {
        this.widget = widget;

        if (!widget.isInitialized()) {
            Log.v(TAG, "start widget initiation for widget: " + widget.toString());
            widget.initWidget(widgetConfiguration);
        } else {
            Log.v(TAG, "update widget configuration. (" + widget.toString() + ")");
            widget.updateWidget(widgetConfiguration);

        }
    }

    public boolean hasWidget() {
        return widget != null && widget.isInitialized();
    }

    public Widget getWidget() {
        return widget;
    }
}
