package com.snepal.padpractice;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class GameActivity extends ActionBarActivity {
    private static final int DEFAULT_TIME = 4;
    private static final int CTW_TIME = 10;
    private static final int ROWS = 5;
    private static final int COLS = 6;

    // Countdown stuff
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private TextView countDownView;
    private TextView timeView;
    private CountDownTimer countDownTimer;
    private double userTime = DEFAULT_TIME;

    // Board related stuff
    private BoardView boardView;
    private Piece[][] pieces;
    private int windowHeight;
    private int boxSize;
    private int maxRadius;
    private int boardWidth;
    private int boardHeight;

    // CTW Stuff
    private boolean CTWEnabled = false;
    private boolean hasPickedUp = false;

    private Vibrator vibrator;
    private boolean shouldVibrate;

    private SharedPreferences sharedPreferences;

    static{ System.loadLibrary("opencv_java3"); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        shouldVibrate = sharedPreferences.getBoolean("vibrate", false);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Uri image = Uri.parse(getIntent().getStringExtra("uri"));

        // Add feedback link
        TextView feedbackText = (TextView) findViewById(R.id.feedbackText);
        feedbackText.setText(Html.fromHtml(getString(R.string.feedback)));
        Linkify.addLinks(feedbackText, Linkify.ALL);
        feedbackText.setMovementMethod(LinkMovementMethod.getInstance());

        initBoardView();
        initCountdown();

        startGenerateBoardTask(image);
    }

    private void initBoardView() {
        // Setup dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point windowSize = new Point();
        display.getSize(windowSize);

        int windowWidth = windowSize.x;
        windowHeight = windowSize.y;
        boxSize = windowWidth / 6;
        maxRadius = boxSize / 2;
        boardWidth = windowWidth;
        boardHeight = windowWidth - boxSize;

        boardView = (BoardView) findViewById(R.id.boardView);
        boardView.setLayoutParams(new LinearLayout.LayoutParams(boardWidth, boardHeight));
        boardView.setBoxWidth(boxSize);
        boardView.setOnTouchStartedListener(new BoardView.OnTouchStartedListener() {
            @Override
            public void onStart() {
                if (!CTWEnabled || !hasPickedUp) {
                    countDownTimer.start();
                }
            }
        });
        boardView.setOnTouchEndedListener(new BoardView.OnTouchEndedListener() {
            @Override
            public void onEnd() {
                if (!CTWEnabled) {
                    countDownTimer.cancel();
                } else {
                    hasPickedUp = true;
                }
            }
        });
    }

    private void initCountdown() {
        // Seekbar allows user to adjust time
        userTime = sharedPreferences.getFloat("user_time", DEFAULT_TIME);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress((int) ((userTime - DEFAULT_TIME) * 2));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                userTime = DEFAULT_TIME + (progress/2d);
                sharedPreferences.edit().putFloat("user_time", (float) userTime).apply();
                resetCountdown();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // Progress bar shows remaining time when moving orbs
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax((int) (userTime * 1000));
        progressBar.setProgress((int) (userTime * 1000));

        countDownView = (TextView) findViewById(R.id.countDown);
        countDownView.setText(String.valueOf(userTime));
        countDownTimer = new MyCountDownTimer((long) (userTime * 1000), 10);
    }

    private Bitmap getCroppedBitmap(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                (float) (width / 2)
                , (float) (height / 2)
                , (float) Math.min(width, (height / 2))
                , Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }

    private Bitmap getUnknownBitmap() {
        final int width = boxSize - 10;
        final int height = boxSize - 10;
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Path path = new Path();
        path.addCircle(
                (float) (width / 2)
                , (float) (height / 2)
                , (float) Math.min(width, (height / 2))
                , Path.Direction.CCW);
        final Canvas canvas = new Canvas(bitmap);
        canvas.clipPath(path);
        canvas.drawARGB(255, 0, 0, 0);
        return bitmap;
    }

    private void resetCountdown() {
        countDownTimer.cancel();
        if (CTWEnabled) {
            progressBar.setMax(CTW_TIME * 1000);
            progressBar.setProgress(CTW_TIME * 1000);

            countDownTimer = new MyCountDownTimer((long) (CTW_TIME * 1000), 10);
            countDownView.setText(String.valueOf(CTW_TIME));
        } else {
            progressBar.setMax((int) (userTime * 1000));
            progressBar.setProgress((int) (userTime * 1000));

            countDownTimer = new MyCountDownTimer((long) (userTime * 1000), 10);
            countDownView.setText(String.valueOf(userTime));
        }
    }

    private void setBoard() {
        Piece[][] copy = new Piece[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece p = pieces[i][j];
                copy[i][j] = new Piece(p.x, p.y, p.getBitmap());
            }
        }
        boardView.setPieces(copy);

        resetCountdown();
    }

    private void showInvalidFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.invalid_file_title))
                .setMessage(getResources().getString(R.string.invalid_file_msg))
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(i, 1);
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void startGenerateBoardTask(Uri uri) {
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ex) {
            // TODO Show file not found dialog
        }
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, getResources().getString(R.string.wait), getResources().getString(R.string.processing), true, false);
        } else {
            progressDialog.show();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
        if (boardWidth > bitmap.getWidth() || (windowHeight - boardWidth) > bitmap.getHeight()) {
            showInvalidFileDialog();
        } else {
            // Make the image to find orbs in a bit larger than the board since sometimes there is
            // weird padding on the bottom
            bitmap = Bitmap.createBitmap(bitmap, 0, windowHeight - boardWidth, boardWidth, boardWidth);
            new GenerateBoardTask().execute(bitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        menu.getItem(3).setChecked(shouldVibrate);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement.
        switch (id) {
            case R.id.action_reset:
                countDownTimer.cancel();
                setBoard();
                hasPickedUp = false;
                return true;
            case R.id.action_new:
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
                return true;
            case R.id.action_notify:
                shouldVibrate = ! item.isChecked();
                sharedPreferences.edit().putBoolean("vibrate", shouldVibrate).apply();
                item.setChecked(shouldVibrate);
                return true;
            case R.id.action_ctw:
                CTWEnabled = ! item.isChecked();
                item.setChecked(CTWEnabled);
                hasPickedUp = false;
                resetCountdown();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            Uri image = data.getData();
            startGenerateBoardTask(image);
        }
    }

    private class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long future, long interval) {
            super(future, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            progressBar.setProgress((int) millisUntilFinished);
            countDownView.setText(String.format("%.2f",millisUntilFinished/1000f));
        }

        @Override
        public void onFinish() {
            progressBar.setProgress(0);
            countDownView.setText("0.00");
            boardView.stopMovement();
            hasPickedUp = false;
            if (shouldVibrate) {
                vibrator.vibrate(400);

            }
        }
    }

    private class GenerateBoardTask extends AsyncTask<Bitmap, Void, Void> {
        @Override
        protected Void doInBackground(Bitmap... params) {
            pieces = new Piece[ROWS][COLS];
            Bitmap bitmap = params[0];
            Mat original = new Mat();
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, original);
            Imgproc.cvtColor(original, source, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(source, source, new Size(9, 9), 2, 2);
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.threshold(source, source, 100, 255, Imgproc.THRESH_OTSU);
            Imgproc.findContours(source.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            //For each contour found
            int numFound = 0;
            for (int i = 0; i < contours.size(); i++)
            {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);
                float centerX = rect.x + (rect.width / 2);
                float centerY = rect.y + (rect.height / 2);
                // Ignore if match is in top part of the image which has champion icons
                if (centerY < (maxRadius + (0.5 * maxRadius))) {
                    continue;
                }
                int col = (int) (centerX / boxSize);
                int row = (int) ((centerY - boxSize) / boxSize);
                // Out of Bounds
                if (col >= COLS || row >= ROWS) {
                    continue;
                }
                // some conditions for size
                boolean validRect = rect.height > (boxSize * 0.7) && rect.height < boxSize;
                // if already found a piece, only replace if this one is bigger
                boolean validPiece = pieces[row][col] == null || (pieces[row][col].getBitmap().getWidth() < rect.width || pieces[row][col].getBitmap().getHeight() < rect.height);
                if (validRect && validPiece) {
                    Rect roi = new Rect(rect.x, rect.y, rect.width, rect.height);
                    Mat cropped = new Mat(original, roi);
                    Bitmap b = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(cropped, b);
                    // circles are roughly 200 rows, poison > 300, hearts < 100
                    if (contour2f.rows() > 100 && contour2f.rows() < 300) {
                        b = getCroppedBitmap(b);
                    }
                    float x = col * boxSize + maxRadius;
                    float y = row * boxSize + maxRadius;
                    if (pieces[row][col] == null) {
                        numFound++;
                    }
                    pieces[row][col] = new Piece(x, y, b);
                }
                // jammers detected with ~150 rows and width larger than height
                else if (contour2f.rows() > 100 && rect.width > rect.height) {
                    int size = rect.width * 2;
                    int left = Math.max(0, (int) (centerX - (size / 2)));
                    int top = Math.max(0, (int) (centerY - (size / 2)));
                    // bounds check
                    if (size > boxSize || (left + size) > original.cols() || (top + size) > original.rows()) {
                        continue;
                    }
                    Rect roi = new Rect(left, top, size, size);
                    Mat cropped = new Mat(original, roi);
                    Bitmap b = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(cropped, b);
                    b = getCroppedBitmap(b);
                    float x = col * boxSize + maxRadius;
                    float y = row * boxSize + maxRadius;
                    if (pieces[row][col] == null) {
                        numFound++;
                    }
                    pieces[row][col] = new Piece(x, y, b);
                }
            }
            if (numFound < ROWS * COLS) {
                for (int i = 0; i < ROWS; i++) {
                    for (int j = 0; j < COLS; j++) {
                        if (pieces[i][j] != null) {
                            continue;
                        }
                        Bitmap b = getUnknownBitmap();
                        float x = j * boxSize + maxRadius;
                        float y = i * boxSize + maxRadius;
                        pieces[i][j] = new Piece(x, y, b);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setBoard();
            progressDialog.dismiss();
        }
    }
}
