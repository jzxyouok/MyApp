package com.example.xlm.mydrawerdemo.Activity;

import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputEditText;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xlm.mydrawerdemo.API.FormListService;
import com.example.xlm.mydrawerdemo.API.NewThreadService;
import com.example.xlm.mydrawerdemo.R;
import com.example.xlm.mydrawerdemo.base.BaseActivity;
import com.example.xlm.mydrawerdemo.bean.ChildForm;
import com.example.xlm.mydrawerdemo.bean.Form;
import com.example.xlm.mydrawerdemo.fragment.ChooseEmojiDialogFragment;
import com.example.xlm.mydrawerdemo.http.Httptools;
import com.example.xlm.mydrawerdemo.utils.Constant;
import com.example.xlm.mydrawerdemo.utils.SPUtiles;
import com.example.xlm.mydrawerdemo.utils.ToastUtils;
import com.example.xlm.mydrawerdemo.utils.Tools;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Multipart;
import retrofit.http.PartMap;

/**
 * Created by 鹏祺 on 2017/6/7.
 */

public class NewThreadActivity extends BaseActivity implements View.OnLongClickListener {
    public static final int TYPE_REPLY = 1;
    public static final int TYPE_NEW = 2;

    private ImageView imgShowMoreTitle;
    private LinearLayout llMoreTitle;
    private Toolbar mToolbar;
    private EditText edtContent;
    private TextView tvTag;
    private CardView cardContent;
    private RelativeLayout rlRoot;
    private RelativeLayout rlTag;
    private String tagName, tagId;
    private Retrofit retrofit;
    private List<ChildForm> listTag = new ArrayList<>();
    private ImageView imgEmoji, imgPic, imgDraw, imgSend;
    private ImageView imgPicContent;
    private String imgPath;
    private TextInputEditText edtName, edtTitle, edtEmail;
    private boolean isWater;
    private int type;
    private String resto;
    private TextView tvTagLeft;


    public static void gotoNewThreadActivity(Context context, String tagName, String tagId) {
        Intent intent = new Intent(context, NewThreadActivity.class);
        intent.putExtra("type", TYPE_NEW);
        intent.putExtra("tagName", tagName);
        intent.putExtra("tagId", tagId);
        context.startActivity(intent);
    }

    //reply
    public static void gotoReplyThreadActivity(Context context, String resto) {
        Intent intent = new Intent(context, NewThreadActivity.class);
        intent.putExtra("type", TYPE_REPLY);
        intent.putExtra("resto", resto);
        context.startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ChooseTagActivity.CHOOSE_TAG:
                if (resultCode == RESULT_OK) {
                    tagName = data.getStringExtra("tagName");
                    tagId = data.getStringExtra("tagId");
                    tvTag.setText(tagName);
                }
                break;
            case Tools.PIC_FROM_PHOTO:
                if (resultCode != RESULT_OK) {
                    ToastUtils.SnakeShowShort(rlRoot, "获取图片失败");
                    break;
                }
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                    imgPath = Tools.saveImage(this, bitmap, "temp.jpg");
                    imgPicContent.setVisibility(View.VISIBLE);
                    imgPicContent.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case Tools.PIC_FROM_CAMERA:
                if (resultCode != RESULT_OK) {
                    ToastUtils.SnakeShowShort(rlRoot, "拍照失败");
                    break;
                }
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(Tools.imgUri, "image/*");
                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Tools.imgUri);
                NewThreadActivity.this.startActivityForResult(intent, Tools.CROP_PHOTO);
                break;
            case Tools.CROP_PHOTO:
                if (resultCode != RESULT_OK) {
                    ToastUtils.SnakeShowShort(rlRoot, "剪裁失败");
                    break;
                }
                Bitmap bitmap;
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Tools.imgUri));
                    imgPath = Tools.imgUri.getPath();
                    imgPicContent.setVisibility(View.VISIBLE);
                    imgPicContent.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case DrawingActivity.REQUEST_DRAWING:
                if (resultCode == RESULT_OK) {
                    String drawPath = data.getStringExtra("path");
                    imgPath = drawPath;
                    Bitmap bitmapDraw;
                    FileInputStream fileInputStream = null;
                    try {
                        File file = new File(drawPath);
                        fileInputStream = new FileInputStream(file);
                        bitmapDraw = BitmapFactory.decodeStream(fileInputStream);
                        imgPicContent.setVisibility(View.VISIBLE);
                        imgPicContent.setImageBitmap(bitmapDraw);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (null != fileInputStream) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_thread);
        initView();
        initData();
    }

    private void initView() {
        imgShowMoreTitle = (ImageView) findViewById(R.id.img_show_more_title);
        llMoreTitle = (LinearLayout) findViewById(R.id.ll_more_title);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        edtContent = (EditText) findViewById(R.id.edt_content);
        tvTag = (TextView) findViewById(R.id.tv_tag);
        cardContent = (CardView) findViewById(R.id.card_content);
        rlRoot = (RelativeLayout) findViewById(R.id.rl_root);
        rlTag = (RelativeLayout) findViewById(R.id.rl_choose_tag);
        imgEmoji = (ImageView) findViewById(R.id.img_emoji);
        imgPic = (ImageView) findViewById(R.id.img_pic);
        imgDraw = (ImageView) findViewById(R.id.img_draw);
        imgSend = (ImageView) findViewById(R.id.img_send);
        imgPicContent = (ImageView) findViewById(R.id.img_pic_content);
        edtName = (TextInputEditText) findViewById(R.id.txtEdt_name);
        edtTitle = (TextInputEditText) findViewById(R.id.txtEdt_title);
        edtEmail = (TextInputEditText) findViewById(R.id.txtEdt_email);
        tvTagLeft = (TextView) findViewById(R.id.tv_tag_left);

        imgPicContent.setOnLongClickListener(this);
        imgSend.setOnClickListener(this);
        imgShowMoreTitle.setOnClickListener(this);
        tvTag.setOnClickListener(this);
        imgEmoji.setOnClickListener(this);
        imgPic.setOnClickListener(this);
        imgDraw.setOnClickListener(this);
    }

    private void initTitle(String title) {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(title);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewThreadActivity.this.finish();
            }
        });
    }

    private void initData() {
        type = getIntent().getIntExtra("type", TYPE_NEW);
        retrofit = Httptools.getInstance().getRetrofit();
        switch (type) {
            case TYPE_NEW:
                initNewThreadData();
                break;
            case TYPE_REPLY:
                initReplyThreadData();
                break;
        }
//        getKeyBoardHeight();
    }

    //初始化发表新串
    private void initNewThreadData() {
        tagName = getIntent().getStringExtra("tagName");
        tagId = getIntent().getStringExtra("tagId");

        tvTag.setText(tagName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvTag.setTransitionName(tagId);
        }
        tvTagLeft.setText("板块");
        initTitle("发串");
        getTagData();
    }

    //初始化回复
    private void initReplyThreadData() {
        resto = getIntent().getStringExtra("resto");
        tvTag.setVisibility(View.GONE);
        tvTagLeft.setText("更多可填写项");
        initTitle("回复");
    }

    private void getTagData() {
        FormListService formList = retrofit.create(FormListService.class);
        Call<List<Form>> formsCall = formList.getFormList();
        formsCall.enqueue(new Callback<List<Form>>() {
            @Override
            public void onResponse(Response<List<Form>> response, Retrofit retrofit) {
                List<Form> forms = response.body();
                if (forms != null) {
                    for (int i = 0; i < forms.size(); i++) {
                        listTag.addAll(forms.get(i).getForums());
                    }
                }
                SPUtiles.saveTags(listTag);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    //获得软键盘高度
    private void getKeyBoardHeight() {
        rlRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rlRoot.getWindowVisibleDisplayFrame(r);

                int screenHeight = rlRoot.getRootView().getHeight();
                int heightDifference = screenHeight - (r.bottom - r.top);
                Log.d("spq", "Size    " + heightDifference);
            }
        });
    }

    public void setTagName(String name) {
        tvTag.setText(name);
    }

    //更多标题参数
    private void showMoreTitle() {
        llMoreTitle.setVisibility(View.VISIBLE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(imgShowMoreTitle, "rotation", 0f, 180f);
        anim.setDuration(400);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();

        //layoutAnimation
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.new_thread_anim);
        LayoutAnimationController layoutAnimationController = new LayoutAnimationController(animation);
        layoutAnimationController.setDelay(.1f);
        layoutAnimationController.setInterpolator(new AccelerateDecelerateInterpolator());
        layoutAnimationController.setOrder(LayoutAnimationController.ORDER_REVERSE);
        llMoreTitle.setLayoutAnimation(layoutAnimationController);
        llMoreTitle.startLayoutAnimation();
        edtContent.startAnimation(animation);
        imgPicContent.startAnimation(animation);
    }

    private void dismisMoreTitle() {
        //动画监听
        Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                llMoreTitle.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        //img anim
        ObjectAnimator anim = ObjectAnimator.ofFloat(imgShowMoreTitle, "rotation", 180f, 0f);
        anim.setDuration(400);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();

        //layoutAnimation
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.new_thread_anim_close);
        animation.setAnimationListener(mAnimationListener);
        LayoutAnimationController layoutAnimationController = new LayoutAnimationController(animation);
        layoutAnimationController.setDelay(.1f);
        layoutAnimationController.setInterpolator(new AccelerateDecelerateInterpolator());
        layoutAnimationController.setOrder(LayoutAnimationController.ORDER_NORMAL);
        llMoreTitle.setLayoutAnimation(layoutAnimationController);
        llMoreTitle.startLayoutAnimation();

        //edt anim
        edtContent.startAnimation(animation);
        imgPicContent.startAnimation(animation);
    }

    private void chooseTag() {
        Pair<View, String> pair1 = Pair.create((View) tvTag, tagId);
        Pair<View, String> pair2 = Pair.create((View) rlTag, "tag_group");
        ChooseTagActivity.gotoChooseTagActivity(this, tagId, pair1);
    }

    private void showEmojiDialog() {
        new ChooseEmojiDialogFragment(onEmojiClickListener).show(getSupportFragmentManager(), "chooseEmoji");
    }

    private ChooseEmojiDialogFragment.OnEmojiClickListener onEmojiClickListener = new ChooseEmojiDialogFragment.OnEmojiClickListener() {
        @Override
        public void onClick(String word, int id) {
            if (TextUtils.isEmpty(word)) {
                //选择了图片
                if (id == 0)
                    return;
                imgPicContent.setImageResource(id);
                imgPicContent.setVisibility(View.VISIBLE);
            } else {
                //选择了文字
                edtContent.append(word);
            }
        }
    };

    private void choosePhote() {
        Tools.showChoosePicDialog(this);
    }

    //发表新串
    private void send() {
        int water = isWater ? 1 : 0;//水印
        RequestBody requestBody = null;
        if (!TextUtils.isEmpty(imgPath)) {
            File file = new File(imgPath);
            if (file.exists()) {
                requestBody = Httptools.getInstance().getRequestBody(file);
            }
        }
        NewThreadService newThreadService = retrofit.create(NewThreadService.class);
        Call<ResponseBody> call;

        RequestBody tagIdBody = RequestBody.create(MediaType.parse("text/plain"), tagId);
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), edtContent.getText().toString());
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), edtName.getText().toString());
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), edtTitle.getText().toString());
        RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), edtEmail.getText().toString());
        RequestBody waterBody = RequestBody.create(MediaType.parse("text/plain"), water + "");

        if (null == requestBody) {
            call = newThreadService.newThread(tagIdBody, nameBody, titleBody, emailBody, contentBody);
        } else {
            call = newThreadService.newThread(tagIdBody, nameBody, titleBody, emailBody, contentBody, waterBody, requestBody);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                if (writeResponseBodyToDisk(response.body())) {
                    File futureStudioIconFile = new File(
                            Environment.getExternalStorageDirectory().getPath()
                                    + Constant.SD_CACHE_DIR
                                    + Constant.NEW_THREAD_RESPONSE_PATH);
                    try {
                        Document document = Jsoup.parse(futureStudioIconFile, "UTF-8");
                        Elements content = document.getElementsByClass("system-message");
                        for (Element element : content) {
                            String success = element.getElementsByClass("success").text();
                            if (!TextUtils.isEmpty(success)) {
                                ToastUtils.showShort(success);
                                NewThreadActivity.this.finish();
                            }
                            String error = element.getElementsByClass("error").text();
                            if (!TextUtils.isEmpty(error)) {
                                ToastUtils.SnakeShowShort(rlRoot, error);
                                break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
    }

    //回复
    private void reply() {
        int water = isWater ? 1 : 0;//水印
        RequestBody requestBody = null;

        NewThreadService newThreadService = retrofit.create(NewThreadService.class);
        Call<ResponseBody> call;

        if (!TextUtils.isEmpty(imgPath)) {
            File file = new File(imgPath);
            if (file.exists()) {
                requestBody = Httptools.getInstance().getRequestBody(file);
            }
        }
        RequestBody restoBody = RequestBody.create(MediaType.parse("text/plain"), resto);
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), edtContent.getText().toString());
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), edtName.getText().toString());
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), edtTitle.getText().toString());
        RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), edtEmail.getText().toString());
        RequestBody waterBody = RequestBody.create(MediaType.parse("text/plain"), water + "");
        if (null == requestBody) {
            call = newThreadService.replyThread(restoBody, contentBody, nameBody, titleBody, emailBody);
        } else {
            call = newThreadService.replyThread(restoBody, contentBody, nameBody, titleBody, emailBody, waterBody, requestBody);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                if (writeResponseBodyToDisk(response.body())) {
                    File futureStudioIconFile = new File(
                            Environment.getExternalStorageDirectory().getPath()
                                    + Constant.SD_CACHE_DIR
                                    + Constant.NEW_THREAD_RESPONSE_PATH);
                    try {
                        Document document = Jsoup.parse(futureStudioIconFile, "UTF-8");
                        Elements content = document.getElementsByClass("system-message");
                        for (Element element : content) {
                            String success = element.getElementsByClass("success").text();
                            if (!TextUtils.isEmpty(success)) {
                                ToastUtils.showShort(success);
                                NewThreadActivity.this.finish();
                            }
                            String error = element.getElementsByClass("error").text();
                            if (!TextUtils.isEmpty(error)) {
                                ToastUtils.SnakeShowShort(rlRoot, error);
                                break;
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                ToastUtils.SnakeShowShort(rlRoot, throwable.getMessage());
            }
        });
    }

    //返回结果写入本地文件夹中
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File sd = Environment.getExternalStorageDirectory();
            String path = sd.getPath() + Constant.SD_CACHE_DIR;
            File file = new File(path);
            if (!file.exists())
                file.mkdir();
//            File cacheDir = getExternalCacheDir();
            File futureStudioIconFile = new File(file + Constant.NEW_THREAD_RESPONSE_PATH);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("spq", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.img_show_more_title:
                if (llMoreTitle.getVisibility() == View.VISIBLE) {
                    dismisMoreTitle();
                } else {
                    showMoreTitle();
                }
                break;
            case R.id.tv_tag:
                chooseTag();
                break;
            case R.id.img_emoji:
                showEmojiDialog();
                break;
            case R.id.img_pic:
                choosePhote();
                break;
            case R.id.img_draw:
                DrawingActivity.gotoDrawingActivity(NewThreadActivity.this);
                break;
            case R.id.img_send:
                switch (type) {
                    case TYPE_NEW:
                        send();
                        break;
                    case TYPE_REPLY:
                        reply();
                        break;
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.img_pic_content:
                imgPicContent.setVisibility(View.GONE);
                break;
        }
        return false;
    }
}
