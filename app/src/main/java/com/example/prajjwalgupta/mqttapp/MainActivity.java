package com.example.prajjwalgupta.mqttapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    MqttAndroidClient mqttAndroidClient ;
    Button btn_pub, btn_scan;
    TextView tv;
    EditText veh_no;
    ProgressBar progressBar;

    TextView disp_veh_no,disp_driver_name,disp_device_status;
    CircleImageView driver_image;



    private IntentIntegrator qrScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mqttAndroidClient = new MqttAndroidClient(this.getApplicationContext(),Constants.MQTT_BROKER_URL,Constants.CLIENT_ID);
        btn_pub =  findViewById(R.id.btn_get);
        btn_scan = findViewById(R.id.btn_scan);
        veh_no = findViewById(R.id.veh_no);
        disp_veh_no = findViewById(R.id.disp_veh_no);
        disp_device_status = findViewById(R.id.disp_device_status);
        disp_driver_name = findViewById(R.id.disp_driver_name);
        driver_image = findViewById(R.id.driver_image);

        qrScan = new IntentIntegrator(this);
      //  new AsyncTaskLoadImage(driver_image).execute(Constants.IMAGES_URL); // Uncomment this and put url in constants class to load the image.
        driver_image.setImageResource(R.drawable.images);


        try {
            IMqttToken token = mqttAndroidClient.connect();
            token.setActionCallback(new IMqttActionListener()
            {
            @Override
            public void onSuccess(IMqttToken iMqttToken)
            {
                Log.d("mqtt", "Connected Successfully to: " + Constants.MQTT_BROKER_URL);
                setSubscription();
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

                Log.d("mqtt", "Could not connect to: " + Constants.MQTT_BROKER_URL);
            }
        });
        }
        catch (MqttException e) {
            e.printStackTrace();
        }


        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                String messageContent = new String(mqttMessage.getPayload());
                String parts[] = messageContent.split(",");
                String vehicle_number  = parts[0];
                String device_status  = parts [3];
                String driver_name = parts[1];


                disp_veh_no.setText(vehicle_number);

                disp_device_status.setText(device_status);
                disp_driver_name.setText(driver_name);


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                qrScan.initiateScan();


            }
        });

        btn_pub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String topic = Constants.PUBLISH_TOPIC;
                String payload = veh_no.getText().toString();
                try {
                    MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                    message.setQos(0);
                    mqttAndroidClient.publish(topic, message);
                    Log.d("mqtt", "Data published successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

      }

      private void setSubscription() {
          try {
              mqttAndroidClient.subscribe(Constants.SUBSCRIBE_TOPIC, 0);
          } catch (MqttException e) {
              e.printStackTrace();
          }
      }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                try {
                    //converting the data to json
                    JSONObject obj = new JSONObject(result.getContents());
                    //setting values to textviews
                    veh_no.setText(obj.getString("veh_no"));

                } catch (JSONException e) {
                    e.printStackTrace();
                    //if control comes here
                    //that means the encoded format not matches
                    //in this case you can display whatever data is available on the qrcode
                    //to a toast
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    public class AsyncTaskLoadImage  extends AsyncTask<String, String, Bitmap> {
        private final static String TAG = "AsyncTaskLoadImage";
        private ImageView imageView;

        public AsyncTaskLoadImage(ImageView imageView) {
            this.imageView = imageView;
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(params[0]);
                bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
            } catch (IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }


}