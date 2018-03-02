/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.poliba.sisinflab.psw.beacon.utils;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import it.poliba.sisinflab.psw.beacon.BeaconingActivity;

/**
 * This class is for various static utilities, largely for manipulation of data structures provided
 * by the collections library.
 */
public class Utils {

    /**
     * Reads the data from the inputStream.
     * @param inputStream The input to read from.
     * @return The entire input stream as a byte array
     * @throws IOException
     */
    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /**
     * Surface a notification to the user that the Physical Web is broadcasting. The notification
     * specifies the transport or URL that is being broadcast and cannot be swiped away.
     * @param context
     * @param stopServiceReceiver
     * @param broadcastNotificationId
     * @param title
     * @param text
     * @param filter
     */
    public static void createBroadcastNotification(Context context,
                                                   BroadcastReceiver stopServiceReceiver, int broadcastNotificationId, CharSequence title,
                                                   CharSequence text, String filter) {
        Intent resultIntent = new Intent();
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(BeaconingActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        context.registerReceiver(stopServiceReceiver, new IntentFilter(filter));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, new Intent(filter),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Log.i("BroadcastNotification", "Start broadcast: " + title + " " + text);
    }

}
