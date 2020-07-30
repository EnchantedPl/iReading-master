package com.iReadingGroup.iReading;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.iReadingGroup.iReading.Bean.ArticleEntity;
import com.iReadingGroup.iReading.Bean.ArticleEntityDao;
import com.iReadingGroup.iReading.Bean.DaoMaster;
import com.iReadingGroup.iReading.Bean.DaoSession;
import com.iReadingGroup.iReading.Bean.OfflineDictBeanDao;
import com.iReadingGroup.iReading.Bean.WordCollectionBean;
import com.iReadingGroup.iReading.Bean.WordCollectionBeanDao;
import com.iReadingGroup.iReading.Event.ChangeWordCollectionDBEvent;
import com.iReadingGroup.iReading.Event.changeArticleCollectionDBEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.database.Database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * The type My application.
 */
public class MyApplication extends Application {
    @SuppressLint("SdCardPath")
    private static final String DB_PATH = "/data/data/com.iReadingGroup.iReading/databases/";//database external path
    private static final String DB_NAME = "wordDetail.db";//database name
    /**
     * The Count activity.
     */
    public int countActivity = 0;
    private ArticleEntityDao daoArticle;
    private OfflineDictBeanDao daoDictionary;
    private WordCollectionBeanDao daoCollection;
    private SharedPreferences settings;

    /**
     * Gets dao article.
     *
     * @return the dao article
     */
    public ArticleEntityDao getDaoArticle() {
        return daoArticle;
    }

    /**
     * Sets dao article.
     *
     * @param daoArticle the dao article
     */
    public void setDaoArticle(ArticleEntityDao daoArticle) {
        this.daoArticle = daoArticle;
    }

    /**
     * Gets dao dictionary.
     *
     * @return the dao dictionary
     */
    public OfflineDictBeanDao getDaoDictionary() {
        return daoDictionary;
    }

    /**
     * Sets dao dictionary.
     *
     * @param daoDictionary the dao dictionary
     */
    public void setDaoDictionary(OfflineDictBeanDao daoDictionary) {
        this.daoDictionary = daoDictionary;
    }

    /**
     * Gets dao collection.
     *
     * @return the dao collection
     */
    public WordCollectionBeanDao getDaoCollection() {
        return daoCollection;
    }

    /**
     * Sets dao collection.
     *
     * @param daoCollection the dao collection
     */
    public void setDaoCollection(WordCollectionBeanDao daoCollection) {
        this.daoCollection = daoCollection;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        copyDBToDatabases();
        initializeDatabase();
        EventBus.getDefault().register(this);//注册eventBus
        BroadcastReceiver br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();//广播过滤器
        filter.addAction("com.iReadingGroup.iReading.WORD_DB_CHANGE");
        filter.addAction("com.iReadingGroup.iReading.ARTICLE_DB_CHANGE");
        this.registerReceiver(br, filter);
        settings = getSharedPreferences("setting", 0);

    }

    private void copyDBToDatabases() {//将离线词库拷贝到数据库中
        //copy offline database to external.
        try {
            String outFileName = DB_PATH + DB_NAME;
            File file = new File(DB_PATH);
            if (!file.mkdirs()) {//创建文件夹以及子文件夹
                file.mkdirs();
            }
            File dataFile = new File(outFileName);
            if (dataFile.exists()) {
                return;
            }
            InputStream myInput;
            myInput = getApplicationContext().getAssets().open(DB_NAME);
            OutputStream myOutput = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {//将离线词库通过流读入到程序并放入byte数组，而通过输出流写入到数据库中
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();
            myInput.close();//对资源进行关闭
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Initialize the database into instance
     */
    private void initializeDatabase() {//数据库中表的初始化

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, DB_NAME);
        helper.setWriteAheadLoggingEnabled(true);
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        daoDictionary = daoSession.getOfflineDictBeanDao();//this is the offline dictionary database

        DaoMaster.DevOpenHelper helper_collection = new DaoMaster.DevOpenHelper(this, "userCollection.db");
        helper_collection.setWriteAheadLoggingEnabled(true);
        Database db_collection = helper_collection.getWritableDb();
        DaoSession daoSession_collection = new DaoMaster(db_collection).newSession();
        daoCollection = daoSession_collection.getWordCollectionBeanDao();// this is the database recording user's word collection

        DaoMaster.DevOpenHelper helper_article = new DaoMaster.DevOpenHelper(this, "userArticle.db");
        helper_article.setWriteAheadLoggingEnabled(true);
        Database db_article = helper_article.getWritableDb();
        DaoSession daoSession_article = new DaoMaster(db_article).newSession();
        daoArticle = daoSession_article.getArticleEntityDao();// this is the database(cache) recording user's articles

    }

    /**
     * On change word collection db event.
     *
     * @param event the event
     *
     *  处理数据库更改事件，完成对数据库的增删以及发出广播通知其他进程进行UI的更新
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangeWordCollectionDBEvent(ChangeWordCollectionDBEvent event) {
        String word = event.word;
        String meaning = event.meaning;
        String operation = event.operation;

        if (operation.equals("add")) {   //add into database
            daoCollection.insert(new WordCollectionBean(word, meaning));
            Toast.makeText(getApplicationContext(), "已收藏单词: " + word, Toast.LENGTH_SHORT).show();
        } else {    //delete from database
            daoCollection.delete(new WordCollectionBean(word, meaning));
            Toast.makeText(getApplicationContext(), "已取消收藏单词: " + word, Toast.LENGTH_SHORT).show();

        }
        Intent intent = new Intent("com.iReadingGroup.iReading.WORD_DB_CHANGE");
        intent.putExtra("word", word);
        intent.putExtra("meaning", meaning);
        intent.putExtra("operation", operation);
        sendBroadcast(intent);


    }

    /**
     * On change article collection db event.
     *
     * @param event the event
     *  文章表只涉及到收藏字段状态的修改
     *
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangeArticleCollectionDBEvent(changeArticleCollectionDBEvent event) {
        String uri = event.uri;
        String operation = event.operation;
        final ArticleEntity article = daoArticle.queryBuilder().where(ArticleEntityDao.Properties.Uri.eq(uri)).list().get(0);
        if (operation.equals("remove")) {
            article.setCollectStatus(false);
            daoArticle.update(article);
            Toast.makeText(getApplicationContext(), "已取消收藏该文章", Toast.LENGTH_SHORT).show();

        } else {
            //add collection
            article.setCollectStatus(true);
            Date currentTime = Calendar.getInstance().getTime();
            article.setCollectTime(currentTime);
            daoArticle.update(article);
            Toast.makeText(getApplicationContext(), "已收藏该文章", Toast.LENGTH_SHORT).show();


        }
        Intent intent = new Intent("com.iReadingGroup.iReading.ARTICLE_DB_CHANGE");
        intent.putExtra("uri", uri);
        intent.putExtra("operation", operation);
        sendBroadcast(intent);
    }









    /**
     * Save setting.
     *
     * @param name  the name
     * @param value the value
     */
    public void saveSetting(String name, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();
    }

    public void saveSetting(String name, int value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(name, value);
        editor.apply();
    }

    public void saveSetting(String name, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    /**
     * Gets setting.
     *
     * @return the setting
     */
    public String getNumberSetting() {
        return settings.getString("number", "10");
    }

    public String getApiKeySetting() {
        return settings.getString("key", "be273a73-e558-4db6-b903-5e925aa6eada");
    }

    public int getPageSetting() {
        return settings.getInt("page", 0);
    }

    public boolean getFirstStatus() {
        return settings.getBoolean("first", true);

    }

    public int getFetchingPolicy() {
        return settings.getInt("policy", Constant.SETTING_POLICY_ONLINE_FIRST);

    }

    public boolean getHistoryStatus() {
        return settings.getBoolean("history", true);

    }
}