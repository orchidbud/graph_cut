package cn.yanglj65;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button doCutButton;
    private ImageView imageViewSrc;
    private Bitmap imgSrcBitMap;
    private Bitmap imgSrcBitMapCopy;
    private ImageView imageViewDst;
    private Paint paint;
    private int drawWhere = 0;
    private Uri imageUri;
    private Line currentLine = new Line();
    private RadioGroup drawGroup;
    private ArrayList<Line> foregroundLines = new ArrayList<>();
    private ArrayList<Line> backgroundLines = new ArrayList<>();
    int pos = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu_toolbar,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
        switch (item.getItemId()) {
            case R.id.reset:
                imgSrcBitMap=null;
                foregroundLines.clear();
                backgroundLines.clear();
                currentLine=new Line();
                imageViewSrc.setImageResource(R.drawable.upload);

                Glide.with(this).load(R.drawable.loading).apply(options).into(imageViewDst);
                return true;
            case R.id.clear:
                if(imgSrcBitMap!=null){
                    imgSrcBitMapCopy=imgSrcBitMap;
                    imageViewSrc.setImageBitmap(imgSrcBitMap);
                }
                foregroundLines.clear();
                backgroundLines.clear();
                currentLine=new Line();
                Glide.with(this).load(R.drawable.loading).apply(options).into(imageViewDst);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doCutButton = findViewById(R.id.doGraphCut);
        imageViewSrc = findViewById(R.id.imageSrc);
        imageViewDst=findViewById(R.id.imageDst);
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
        Glide.with(this).load(R.drawable.loading).apply(options).into(imageViewDst);

        doCutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "开始执行图片分割", Toast.LENGTH_SHORT).show();
            }
        });
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(18);
        drawGroup = findViewById(R.id.drawChoice);
        drawGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch ((checkedId)) {
                    case R.id.drawForeground:
                        drawWhere = 0;
                        paint.setColor(Color.GREEN);
                        break;
                    case R.id.drawBackground:
                        drawWhere = 1;
                        paint.setColor(Color.BLUE);
                        break;
                    default:
                        drawWhere = 0;
                        paint.setColor(Color.GREEN);
                        break;
                }
            }
        });
        imageViewSrc.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (imgSrcBitMap == null && event.getAction() == MotionEvent.ACTION_DOWN ) {
                    Intent chooseInAlbum = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(chooseInAlbum, 0);
                    return false;
                }
                if (imgSrcBitMap != null) {
                    float clickX = event.getX();
                    float clickY = event.getY();
                    float[] dst = new float[2];
                    Matrix imageViewSrcMatrix = imageViewSrc.getImageMatrix();
                    Matrix inverseMatrix = new Matrix();
                    imageViewSrcMatrix.invert(inverseMatrix);
                    inverseMatrix.mapPoints(dst, new float[]{clickX, clickY});
                    Point point = new Point(dst[0], dst[1]);
                    //在移动时添加所经过的点
                    currentLine.points.add(point);
                    drawImage(pos);
                    pos++;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Log.i("位置：", dst[0] + "," + dst[1]);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //添加画过的线
                        if (drawWhere == 0) {
                            foregroundLines.add(currentLine);
                        } else {
                            backgroundLines.add(currentLine);
                        }
                        pos = 0;
                        //  drawImage();
                        Log.i("位置：", dst[0] + "," + dst[1]);
                        currentLine = new Line();
                    }
                }
                return false;
            }
        });
    }

    private void drawImage(int pos) {
        if (pos <= 0) {
            return;
        }
        Canvas canvas = new Canvas(imgSrcBitMapCopy);
        Point startPos = currentLine.points.get(pos - 1);
        Point endPos = currentLine.points.get(pos);
        canvas.drawLine(startPos.x, startPos.y, endPos.x, endPos.y, paint);
        canvas.save();
        imageViewSrc.setImageBitmap(imgSrcBitMapCopy);
    }

    private void drawImage() {
        Canvas canvas = new Canvas(imgSrcBitMapCopy);
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);
        Point startPos = currentLine.points.get(0);
        Point endPos = currentLine.points.get(currentLine.points.size() - 1);
        canvas.drawLine(startPos.x, startPos.y, endPos.x, endPos.y, paint);
        canvas.save();
        imageViewSrc.setImageBitmap(imgSrcBitMapCopy);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //从相册获取头像
            case 0:
                if (resultCode == RESULT_OK) {
                    try {
                        imageUri = data.getData();
                        imgSrcBitMap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(imageUri));
                        imageViewSrc.setImageBitmap(imgSrcBitMap);
                        imgSrcBitMapCopy = imgSrcBitMap.copy(Bitmap.Config.ARGB_8888, true);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
