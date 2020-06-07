package com.example.imagetotext_v2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DriverReport extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_report);

        final TextView mDate=findViewById(R.id.date);
        final TextView mUser=findViewById(R.id.user);
        final TextView mOriginMile=findViewById(R.id.originMile);
        final TextView mOriginTime=findViewById(R.id.originTime);
        final TextView mDestinationMile=findViewById(R.id.destinationMile);
        final TextView mDestinationTime=findViewById(R.id.destinationTime);
        final TextView mTotalDistance=findViewById(R.id.totalMile);
        final TextView mTripId=findViewById(R.id.trip);
        final TextView mSumDistance=findViewById(R.id.totalDistance);

        // ref: https://www.akexorcist.com/2013/02/android-code-intent-with-bundle-extra.html
        // See how to pass values with Intent
        Bundle bundle=getIntent().getExtras();
        String user=bundle.getString("username");
        String tripId=bundle.getString("tripId");
        //double sumDistance=bundle.getDouble("sumDistance");
        String sumDistance=bundle.getString("sumDistance");

        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY");
        String currentDatetime=sdf.format(new Date());

        String refCol=user+"-"+tripId+"-"+currentDatetime;

        Log.i("Input","refCol : "+refCol);

        mDate.setText("Date : "+currentDatetime);
        mUser.setText("Driver : "+user);
        mTripId.setText("Trip ID :"+tripId);

        //DecimalFormat df = new DecimalFormat("#.##");
        //String dummy = df.format(sumDistance);
        String dummy=sumDistance;

        Log.i("report"," sumDistance :"+sumDistance+", formatted :"+dummy+ " .");


        mSumDistance.setText("Total Coverage (LatLng) :"+dummy+" ");

        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("DriverMonitor");
        query.whereEqualTo("refCol",refCol);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e==null && objects.size()>0) {
                    mOriginMile.setText("Origin :"+objects.get(0).getString("goMile")+" , ");
                    mOriginTime.setText(objects.get(0).getString("goDate")+" , ");
                    mDestinationMile.setText("Destination :"+objects.get(0).getString("backMile"));
                    mDestinationTime.setText(objects.get(0).getString("backDate"));
                    mTotalDistance.setText("Total Coverage : "+objects.get(0).getNumber("calTotTrip").toString()+" km");
                    Log.i("test", " Reported !!!" );

                }else{
                    Log.i("test", " Null ");
                    e.printStackTrace();
                }


            }



        }); // query find




    }
}
