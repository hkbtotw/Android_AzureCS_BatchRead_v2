package com.example.imagetotext_v2;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class StarterApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Local Datastore.
        //Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("")
                .clientKey("")
                .server("")
                .build()
        );


        /*
        Log.i("check","Get in ");
        ParseObject object = new ParseObject("DriverMonitor");
        object.put("tripId", "123467");
        object.put("goMile", "Nim_1");
        object.put("goDate", "123467");
        object.put("backMile", "Nim_1");
        object.put("backDate", "123467");
        object.put("calTotTrip", 12346.7);
        object.put("MeasuredTotTrip", 1234.67);

        object.saveInBackground(new SaveCallback() {
          @Override
          public void done(ParseException ex) {
            if (ex == null) {
              Log.i("Parse Result", "Successful!");
            } else {
              Log.i("Parse Result", "Failed" + ex.toString());
            }
          }
        });


        ParseUser.enableAutomaticUser();

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
        */



    }// onCreate
}
