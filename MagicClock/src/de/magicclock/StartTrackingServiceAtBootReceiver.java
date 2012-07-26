/**
 * MagicClock, a clock that displays people's locations
 * Copyright (C) 2012 www.magicclock.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * You can contact the authors via authors@magicclock.de or www.magicclock.de
 */
package de.magicclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//TODO

public class StartTrackingServiceAtBootReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent serviceIntent = new Intent("de.magicclock.LocationService");
            serviceIntent.putExtra("de.magicclock.locService", "start");
            context.startService(serviceIntent);
        }
	}

}
