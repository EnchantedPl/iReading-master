package com.iReadingGroup.iReading.Adapter;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.iReadingGroup.iReading.Bean.ArticleEntity;
import com.iReadingGroup.iReading.Event.changeArticleCollectionDBEvent;
import com.iReadingGroup.iReading.R;
import com.iReadingGroup.iReading.TimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * ArticleInfoAdapter
 * 从数据库中查出的是ArticleEntity对象的集合，需要适配器将对象填充到RecycleView(ListView)中
 */
public class ArticleInfoAdapter extends BaseQuickAdapter<ArticleEntity, BaseViewHolder> {
    private int time = 0;

    /**
     * 适配器初始化
     *
     * @param context     the context
     * @param layoutResId the layout res id
     * @param data        the data
     */
    public ArticleInfoAdapter(Context context, int layoutResId, List<ArticleEntity> data) {
        super(layoutResId, data);
        mContext = context;

    }
    @Override
    protected void convert(BaseViewHolder helper, final ArticleEntity item, List<Object> payloads) {
        String command = payloads.get(0).toString();
        if (payloads.isEmpty()) {
            convert(helper, item);
        } else if (command.equals("ChangeSwipeButton")) {
            Button a = helper.getView(R.id.swipe_collection);
            if (item.getCollectStatus()) a.setText("取消收藏");
            else a.setText("收藏");
        } else {
            Date currentTime = Calendar.getInstance().getTime();
            helper.setText(R.id.txt_time, getRelativeTime(item.getTime()));
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, final ArticleEntity item) {
        final String uri = item.getUri();
        helper.setText(R.id.txt_title, item.getName());
        helper.setText(R.id.txt_source, item.getSource());
        helper.setText(R.id.txt_time, getRelativeTime(item.getTime()));
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(R.mipmap.icon)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(true)
                .override(360, 360);
        Glide.with(mContext).load(item.getImageUrl()).apply(options).into((ImageView) helper.getView(R.id.img));
        final Button a = helper.getView(R.id.swipe_collection);

        if (item.getCollectStatus()) a.setText("取消收藏");
        else a.setText("收藏");
        a.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                //触发收藏/取消收藏功能，并发出数据库更改事件
                if (item.getCollectStatus()) {
                    a.setText("收藏");
                    EventBus.getDefault().post(new changeArticleCollectionDBEvent(uri, "remove"));
                } else {
                    a.setText("取消收藏");
                    EventBus.getDefault().post(new changeArticleCollectionDBEvent(uri, "add"));

                }

            }
        });

    }

    private String getRelativeTime(String uploadTime_UTC) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date value = formatter.parse(uploadTime_UTC);

            return TimeUtil.getTimeFormatText(value);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}