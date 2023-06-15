package com.yyf.testipc1;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.icu.text.DateFormat;
import android.widget.RemoteViews;

import java.util.Date;

/**
 * Implementation of App Widget functionality.
 */
public class ExampleAppWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE_TIME = "com.example.app.ACTION_UPDATE_TIME";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE_TIME.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.example_app_widget);

        // Set the current time in the TextView
        String currentTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        views.setTextViewText(R.id.widget_time, currentTime);

        // Set up the PendingIntent for the update button
        Intent updateIntent = new Intent(context, ExampleAppWidgetProvider.class);
        updateIntent.setAction(ACTION_UPDATE_TIME);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_update_button, updatePendingIntent);

        // Update the App Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
