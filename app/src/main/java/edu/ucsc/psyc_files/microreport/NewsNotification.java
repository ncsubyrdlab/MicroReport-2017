package edu.ucsc.psyc_files.microreport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * The structure of the notification that is called by the Notification Service.
 */
public class NewsNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private static final String NOTIFICATION_TAG = "News";

    public static void notify(final Context context,
                              final String notificationTitle, final String notificationDetail, final int number) {
        final Resources res = context.getResources();
        final Bitmap picture = BitmapFactory.decodeResource(res, R.drawable.ic_microreport);
        final String ticker = "Good News: "+ notificationTitle;
        final String title = "Good News: "+ notificationTitle;
        final String text = notificationDetail;

        //navigate to Activity when click on notification
        Intent resultIntent = new Intent(context, BulletinBoard.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(BulletinBoard.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder builder = new Notification.Builder(context)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setLargeIcon(picture)
                .setTicker(ticker)
                //.setNumber(number)
                .setStyle(new Notification.BigTextStyle()   //expanded text
                        .bigText(text)
                        .setBigContentTitle(title))
                //        .setSummaryText("MicroReport Good News"))
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        //todo: add code to update with multiple notifications

        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_TAG, 0, builder.build());
    }
}