package de.andidog.mobiprint;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class OrderDetailActivity extends Activity
{
    private OrderCollectionAdapter adapter;

    private Order orderToShow;

    private boolean showCurrentOrder;

    private static final String TAG = "OrderDetailActivity";

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        relayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // TODO: either this or the order ID
        showCurrentOrder = getIntent().getExtras().getBoolean("showCurrentOrder", false);

        adapter = OrderCollectionAdapter.getInstance(this);

        orderToShow = null;

        relayout();
    }

    private void relayout()
    {
        if(showCurrentOrder)
        {
            adapter.setNotifyOnChange(true);
            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged()
                {
                    relayout();
                }
            });
            PictureUploadTask.registerObserver(new DataSetObserver() {
                @Override
                public void onChanged()
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            relayout();
                        }
                    });
                }
            });

            ArrayList<Order> allOrders;
            synchronized(adapter)
            {
                allOrders = adapter.getAllOrders();
            }

            for(Order order : allOrders)
                if(order.getSubmissionDate() == null)
                {
                    orderToShow = order;
                    break;
                }

            if(orderToShow == null)
                orderToShow = new Order();
        }
        else
            throw new UnsupportedOperationException("not implemented yet");

        setContentView(R.layout.order_detail);

        String numPicturesText = String.format(getResources().getString(R.string.order_num_pictures_fmt),
                                               orderToShow.getPictureIds().length);
        ((TextView)findViewById(R.id.order_num_pictures)).setText(numPicturesText);

        // Lay out in a way that each thumbnail is at least 1.5 cm wide.
        // Cannot use order_detail_layout's width because on orientation change, it stays the same and only after this
        // call gets a different width.
        DisplayMetrics screenMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screenMetrics);
        int viewWidthPx = screenMetrics.widthPixels;
        float viewWidthCm = (viewWidthPx / screenMetrics.xdpi) * 2.54f;

        TableLayout table = (TableLayout)findViewById(R.id.thumbnails_table);

        final int numPicturesX = (int)Math.max(1, Math.round(Math.floor(viewWidthCm / 1.5)));
        final int border =  5;
        final int thumbMargin = 5;
        int cx = (viewWidthPx - 2 * border) / numPicturesX - thumbMargin;
        TableRow row = null;

        Set<String> uploadingPictures = PictureUploadTask.getUploadingPictures();
        Iterator<String> uploadingPicturesIterator = uploadingPictures.iterator();
        int numOrderPictures = orderToShow.getPictureIds().length;
        int numUploadingPictures = uploadingPictures.size();

        for(int i = 0; i < numOrderPictures + numUploadingPictures; ++i)
        {
            int pictureId = -1;
            String uploadingFilename = null;
            int statePictureId;
            int stateStringId;

            if(i >= numOrderPictures)
            {
                uploadingFilename = uploadingPicturesIterator.next();
                statePictureId = R.drawable.uploading;
                stateStringId = R.string.uploading;
            }
            else
            {
                pictureId = orderToShow.getPictureIds()[i];

                if(orderToShow.getSubmissionDate() == null)
                {
                    statePictureId = R.drawable.uploaded;
                    stateStringId = R.string.uploaded;
                }
                else
                {
                    statePictureId = R.drawable.printed;
                    stateStringId = R.string.printed;
                }
            }


            if(i % numPicturesX == 0)
            {
                if(row != null)
                    table.addView(row);

                row = new TableRow(this);
                row.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, cx));
                //row.setBackgroundColor(0xffffffff);
            }

            // Setting margin still does not work
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FrameLayout thumbnailLayout = (FrameLayout)inflater.inflate(R.layout.thumbnail, null);

            final ImageView img = (ImageView)thumbnailLayout.findViewById(R.id.thumbnail_image);
            ViewGroup.LayoutParams layout = img.getLayoutParams();
            layout.width = cx;
            layout.height = cx;
            img.setLayoutParams(layout);
            img.setImageDrawable(getResources().getDrawable(R.drawable.test_thumbnail));

            if(i >= numOrderPictures)
            {
                Bitmap bmp = BitmapFactory.decodeFile(uploadingFilename);
                if(bmp == null)
                    Log.e(TAG, "Failed to decode picture " + uploadingFilename);
                else
                {
                    int imageWidthPx = Math.min(bmp.getWidth(), cx);
                    int imageHeightPx = bmp.getHeight() * imageWidthPx / bmp.getWidth();

                    Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, imageWidthPx, imageHeightPx, true);
                    bmp.recycle();

                    img.setImageBitmap(scaledBmp);
                }
            }
            else
            {
                File thumbnailFile = DownloadThumbnailTask.getFile(this, pictureId);
                if(thumbnailFile.exists() && thumbnailFile.length() > 500)
                    img.setImageURI(Uri.fromFile(thumbnailFile));
                else
                {
                    // Have to download thumbnail
                    new DownloadThumbnailTask(this, getWindowManager(), pictureId)
                    {
                        protected void onPostExecute(Void result)
                        {
                            super.onPostExecute(result);
                            File newThumbnailFile = getFile(false);
                            if(newThumbnailFile.exists() && newThumbnailFile.length() > 500)
                                img.setImageURI(Uri.fromFile(newThumbnailFile));
                        }
                    }.execute();
                }
            }

            ImageView stateImg = (ImageView)thumbnailLayout.findViewById(R.id.thumbnail_state_image);
            stateImg.setImageDrawable(getResources().getDrawable(statePictureId));

            TextView stateTextView = (TextView)thumbnailLayout.findViewById(R.id.state_text);
            stateTextView.setText(getResources().getString(stateStringId));

            row.addView(thumbnailLayout);
        }

        if(row != null)
            table.addView(row);
    }
}
