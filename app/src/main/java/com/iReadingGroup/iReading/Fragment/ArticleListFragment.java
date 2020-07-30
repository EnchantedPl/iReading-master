package com.iReadingGroup.iReading.Fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.iReadingGroup.iReading.Activity.ArticleDetailActivity;
import com.iReadingGroup.iReading.Adapter.ArticleInfoAdapter;
import com.iReadingGroup.iReading.AsyncTask.BaseAsyncTask;
import com.iReadingGroup.iReading.Bean.ArticleEntity;
import com.iReadingGroup.iReading.Bean.ArticleEntityDao;
import com.iReadingGroup.iReading.Event.ArticleDatabaseChangedEvent;
import com.iReadingGroup.iReading.Event.ArticleSearchDoneEvent;
import com.iReadingGroup.iReading.Event.ArticleSearchEvent;
import com.iReadingGroup.iReading.Event.BackToTopEvent;
import com.iReadingGroup.iReading.Event.SourceSelectEvent;
import com.iReadingGroup.iReading.Function;
import com.iReadingGroup.iReading.R;
import com.iReadingGroup.iReading.SpeedyLinearLayoutManager;
import com.iReadingGroup.iReading.TimeUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.bingoogolapple.refreshlayout.BGAMoocStyleRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;


/**
 * ArticleListFragment
 * Load article when refresh, change source when source section changes
 * <p>
 * Load the articles from database and add them into ArrayList
 * Update through refreshing or loading using AsyncTask
 * If source changes, the view changes accordingly
 * search in corresponding source
 */
public class ArticleListFragment extends Fragment implements BGARefreshLayout.BGARefreshLayoutDelegate {
    private String numberPerLoading;
    private BGARefreshLayout mRefreshLayout; //Layout for refreshing and loading
    //private ListView infoListView;////infoListView for list of brief info of each article
    private RecyclerView infoListView;
    private ArticleInfoAdapter articleInfoAdapter;//Custom adapter for article info
    private List<ArticleEntity> alArticleInfo = new ArrayList<>();//ArrayList linked to adapter for listview
    private ArrayList<ArticleEntity> alArticleInfoCache = new ArrayList<>();//cache of ArrayList linked to adapter for listview when searching
    private boolean flag_search = false;
    private ArrayList<String> current_uri_list = new ArrayList<>();
    private View view;
    private ArticleEntityDao daoArticle;
    private String requestUrl;
    private HashMap<String, Integer> pageMap = new HashMap<>();
    private String current_source = "所有";
    private String requestUrlCache;
    private String searchUrlPrefix;
    private SpeedyLinearLayoutManager layoutManager;
    private String apiKey;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        numberPerLoading = Function.getMyApplication(getContext()).getNumberSetting();
        apiKey = Function.getMyApplication(getContext()).getApiKeySetting();
        boolean history = Function.getMyApplication(getContext()).getHistoryStatus();
        if (!history) {
            Function.clearAllUncollectedArticles(daoArticle);
            alArticleInfo.clear();
            alArticleInfo.addAll(daoArticle.loadAll());
            articleInfoAdapter.notifyDataSetChanged();
            Function.getMyApplication(getContext()).saveSetting("history", true);


        }
        if (requestUrl == null)
            requestUrl = Function.getDefaultRequestUrl(numberPerLoading, apiKey);
        else {
            requestUrl = requestUrl.replaceAll("(articlesCount=)[^&]*(&)", String.format("$1%s$2", numberPerLoading));
            requestUrl = requestUrl.replaceAll("(apiKey=)[^&]*(&)", String.format("$1%s$2", apiKey));
        }

        searchUrlPrefix = requestUrl + "&keywordLoc=title&keyword=";


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Must add in every fragments' onCreateView to avoid duplicate creating.
        //防止重复创建
        if (null != view) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null != parent) {
                parent.removeView(view);
            }
        } else {
            //start initializing
            view = inflater.inflate(com.iReadingGroup.iReading.R.layout.fragment_article_info, container, false);//set layout
            initializeUI();

            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int first = layoutManager.findFirstVisibleItemPosition();
                    int last = layoutManager.findLastVisibleItemPosition();
                    articleInfoAdapter.notifyItemRangeChanged(first, last, "payload");
                    //要做的事情
                    handler.postDelayed(this, 60000);
                }
            };
            handler.postDelayed(runnable, 60000);//每60秒执行一次runnable.刷新一次加载时间

        }
        return view;
    }

    /**
     * Initialize ui.
     */
    public void initializeUI() {
        initializeRefreshingLayout();//Initialize refreshing and loading layout
        initializeListView();
    }

    /**
     * Initialize refreshing layout.
     */
    public void initializeRefreshingLayout() {
        mRefreshLayout = view.findViewById(R.id.rl_modulename_refresh);
        mRefreshLayout.setDelegate(this);
        BGAMoocStyleRefreshViewHolder refreshViewHolder = new BGAMoocStyleRefreshViewHolder(getContext(), true);
        refreshViewHolder.setOriginalImage(R.mipmap.icon1);
        refreshViewHolder.setUltimateColor(com.iReadingGroup.iReading.R.color.custom_imoocstyle);
        mRefreshLayout.setIsShowLoadingMoreView(true);
        refreshViewHolder.setLoadingMoreText("加载历史文章……");
        mRefreshLayout.setRefreshViewHolder(refreshViewHolder);
        //first time,show intro
        boolean isFirstUser = Function.getMyApplication(getContext()).getFirstStatus();
        if (isFirstUser)
            mRefreshLayout.setBackgroundResource(R.mipmap.intro);
    }


    /**
     * Initialize list view.
     */
    public void initializeListView() {//Establish the connection among listView,adapter and arrayList.

        infoListView = view.findViewById(R.id.list);//
        alArticleInfo = daoArticle.queryBuilder().orderDesc(ArticleEntityDao.Properties.Time).list();
        alArticleInfoCache.addAll(alArticleInfo);
        articleInfoAdapter = new ArticleInfoAdapter(getActivity(),
                com.iReadingGroup.iReading.R.layout.listitem_article_info, alArticleInfo);
        infoListView.setAdapter(articleInfoAdapter);//link the adapter to ListView
        layoutManager = new SpeedyLinearLayoutManager(getContext(), SpeedyLinearLayoutManager.VERTICAL, false);
        infoListView.setLayoutManager(layoutManager);
        //Set click event for listView and pass the arguments through Bundle to the following activity.

        articleInfoAdapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN);
        articleInfoAdapter.isFirstOnly(true);

        //当列表中的文章被点击时，将文章的信息封装成一个文章实体对象，跳转到文章详情页
        //并根据传递的信息去网络上查询该文章的资源，展示在页面上
        infoListView.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void onSimpleItemClick(BaseQuickAdapter parent, View view, int position) {
                ArticleEntity h = alArticleInfo.get(position);
                String number = h.getName();
                String uri = h.getUri();
                Intent intent = new Intent(getActivity(), ArticleDetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("name", number);
                bundle.putString("uri", uri);
                bundle.putString("time", TimeUtil.getCurrentTimeFromUTC(h.getTime()));
                bundle.putString("source", h.getSource());
                intent.putExtras(bundle);
                startActivity(intent);
                //FruitList.this.finish();
            }
        });
        initializeMap();

    }

    /**
     * Map for source:current page
     */
    private void initializeMap() {
        pageMap.put("National Geographic", 0);
        pageMap.put("Nature", 0);
        pageMap.put("The Economist", 0);
        pageMap.put("TIME", 0);
        pageMap.put("The New York Times", 0);
        pageMap.put("Bloomberg Business", 0);
        pageMap.put("CNN", 0);
        pageMap.put("Fox News", 0);
        pageMap.put("Forbes", 0);
        pageMap.put("Washington Post", 0);
        pageMap.put("The Guardian", 0);
        pageMap.put("The Times", 0);
        pageMap.put("Mail Online", 0);
        pageMap.put("BBC", 0);
        pageMap.put("PEOPLE", 0);
        pageMap.put("所有", 0);

    }


    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
        if (Function.getMyApplication(getContext()).getFirstStatus()) {//if first
            mRefreshLayout.setBackgroundColor(Color.TRANSPARENT);
            Function.getMyApplication(getContext()).saveSetting("first", false);
        }
        // Refreshing the latest data from server.
        if (Function.isNetworkAvailable(getContext())) {
            // if the network is good, continue.
            new RefreshingTask(this).execute(requestUrl + "&articlesPage=1");
        } else {
            // network is not connected,end
            mRefreshLayout.endRefreshing();
        }
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        //  Loading more (history) data from server or cache, Return false to disable the refreshing action.

        if (Function.isNetworkAvailable(getContext())) {
            pageMap.put(current_source, pageMap.get(current_source) + 1);
            new LoadingTask(this).execute(requestUrl + "&articlesPage=" + pageMap.get(current_source) + "");
            return true;
        } else {
            // The network is not connected
            Toast.makeText(getContext(), "网络不可用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    /**
     * On message event.
     *
     * @param event the event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSourceSelectEvent(SourceSelectEvent event) {
        numberPerLoading = Function.getMyApplication(getContext()).getNumberSetting();
        int size;
        List<ArticleEntity> cache;
        current_source = event.title;
        switch (event.title) {
            case "所有": {//all
                requestUrl = Function.getDefaultRequestUrl(numberPerLoading, apiKey);
                setSourceForView("所有");

                break;
            }
            case "National Geographic": {   //national geographic
                requestUrl = getRequestUrl("news.nationalgeographic.com");
                setSourceForView("National Geographic");
                break;
            }
            case "Nature": {   //nature
                requestUrl = getRequestUrl("nature.com");
                setSourceForView("Nature");
                break;
            }
            case "The Economist": {   //the economist
                requestUrl = getRequestUrl("economist.com");
                setSourceForView("The Economist");
                break;
            }
            case "TIME": {   //TIME
                requestUrl = getRequestUrl("time.com");
                setSourceForView("TIME");
                break;
            }
            case "The New York Times": {   //The New York Times
                requestUrl = getRequestUrl("nytimes.com");
                setSourceForView("The New York Times");
                break;
            }
            case "Bloomberg Business": {   //Bloomberg Business
                requestUrl = getRequestUrl("bloomberg.com");
                setSourceForView("Bloomberg Business");
                break;
            }
            case "CNN": {   //CNN
                requestUrl = getRequestUrl("edition.cnn.com");
                setSourceForView("CNN");
                break;
            }
            case "Fox News": {   //Fox
                requestUrl = getRequestUrl("foxnews.com");
                setSourceForView("Fox News");
                break;
            }
            case "Forbes": {   //Forbes
                requestUrl = getRequestUrl("forbes.com");
                setSourceForView("Forbes");
                break;
            }
            case "Washington Post": {   //Washington Post
                requestUrl = getRequestUrl("washingtonpost.com");
                setSourceForView("Washington Post");
                break;
            }
            case "The Guardian": {   //The Guardian
                requestUrl = getRequestUrl("theguardian.com");
                setSourceForView("The Guardian");
                break;
            }
            case "The Times": {   //The Times
                requestUrl = getRequestUrl("thetimes.co.uk");
                setSourceForView("The Times");
                break;
            }
            case "Mail Online": {   //Mail Online
                requestUrl = getRequestUrl("dailymail.co.uk");
                setSourceForView("Mail Online");
                break;
            }
            case "BBC": {   //BBC
                requestUrl = getRequestUrl("bbc.com");
                setSourceForView("BBC");
                break;
            }
            case "PEOPLE": {   //PEOPLE
                requestUrl = getRequestUrl("people.com");
                setSourceForView("PEOPLE");
                break;
            }
        }
        searchUrlPrefix = requestUrl + "&keywordLoc=title&keyword=";
        alArticleInfoCache.clear();
        alArticleInfoCache.addAll(alArticleInfo);

    }

    /**
     * On article search event.
     *
     * @param event the event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onArticleSearchEvent(ArticleSearchEvent event) {
        if (!flag_search) {
            flag_search = true;
            requestUrlCache = requestUrl;

        }
        requestUrl = searchUrlPrefix + event.keyword;
        alArticleInfo.clear();
        articleInfoAdapter.notifyDataSetChanged();
        mRefreshLayout.beginRefreshing();


    }

    /**
     * On article search done event.
     *
     * @param event the event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onArticleSearchDoneEvent(ArticleSearchDoneEvent event) {
        flag_search = false;

        alArticleInfo.clear();
        alArticleInfo.addAll(alArticleInfoCache);
        articleInfoAdapter.notifyDataSetChanged();
        requestUrl = requestUrlCache;

    }

    private String getRequestUrl(String source_url) {
        return Function.getSourceRequestUrl(source_url, numberPerLoading, apiKey);

    }

    private void setSourceForView(String source) {
        alArticleInfo.clear();
        if (!source.equals("所有")) {
            //add this source
            alArticleInfo.addAll(daoArticle.queryBuilder().orderDesc(ArticleEntityDao.Properties.Time).where(ArticleEntityDao.Properties.Source.like(source + "%")).list());
        } else
            alArticleInfo.addAll(daoArticle.queryBuilder().orderDesc(ArticleEntityDao.Properties.Time).list());
        articleInfoAdapter.notifyDataSetChanged();
        pageMap.put(source, pageMap.get(source) / Integer.parseInt(numberPerLoading));

    }

    /**
     * On article database changed event.
     *
     * @param event the event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onArticleDatabaseChangedEvent(ArticleDatabaseChangedEvent event) {
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        articleInfoAdapter.notifyItemRangeChanged(first, last, "ChangeSwipeButton");
    }

    /**
     * On back to top event.
     *
     * @param event the event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackToTopEvent(BackToTopEvent event) {
        infoListView.smoothScrollToPosition(0);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        daoArticle = Function.getMyApplication(getContext()).getDaoArticle();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);

    }

    /**
     * Begin refreshing.
     */
// 通过代码方式控制进入正在刷新状态。应用场景：某些应用在 activity 的 onStart 方法中调用，自动进入正在刷新状态获取最新数据
    public void beginRefreshing() {
        mRefreshLayout.beginRefreshing();
    }

    /**
     * Begin loading more.
     */
// 通过代码方式控制进入加载更多状态
    public void beginLoadingMore() {
        mRefreshLayout.beginLoadingMore();
    }

    private boolean getArticleCachedStatus(String uri) {
        current_uri_list = new ArrayList<>();
        for (ArticleEntity exist : daoArticle.loadAll()) {
            current_uri_list.add(exist.getUri());
        }
        return current_uri_list.contains(uri);
    }

    /**
     * The type Refreshing task.
     */
    static class RefreshingTask extends BaseAsyncTask {
        private WeakReference<ArticleListFragment> weakFragmentRef;

        /**
         * Instantiates a new Refreshing task.
         *
         * @param fragment the fragment
         */
        public RefreshingTask(ArticleListFragment fragment) {
            weakFragmentRef = new WeakReference<ArticleListFragment>(fragment);
        }

        //解析从网络上获取的资源

        @Override
        protected void onPostExecute(String result) {

            ArticleListFragment fragment = weakFragmentRef.get();
            if (fragment == null) return;

            fragment.pageMap.put(fragment.current_source, 1);
            int count = 0;
            if (result == null) {
                fragment.mRefreshLayout.endRefreshing();
                Toast.makeText(fragment.getContext(), "无网络或apiKey错误", Toast.LENGTH_SHORT).show();
            } else if (result.equals("Timeout")) {
                fragment.mRefreshLayout.endRefreshing();
                Toast.makeText(fragment.getContext(), "连接超时", Toast.LENGTH_SHORT).show();

            } else {//fetch succeed

                try {   //parse word from json
                    //sample link.:http://dict-co.iciba.com/api/dictionary.php?w=go&key=341DEFE6E5CA504E62A567082590D0BD&type=json
                    String uri, title, source_title, time;
                    JSONObject reader = new JSONObject(result);
                    JSONObject articles = reader.getJSONObject("articles");
                    JSONArray results = articles.getJSONArray("results");
                    for (int i = results.length() - 1; i > -1; i--) {
                        JSONObject article = results.getJSONObject(i);
                        uri = article.getString("uri");
                        if (fragment.getArticleCachedStatus(uri) && (!fragment.flag_search))
                            continue;
                        //if (article.getBoolean("isDuplicate")) continue;
                        title = article.getString("title");
                        time = article.getString("dateTime");
                        JSONObject source = article.getJSONObject("source");
                        source_title = source.getString("title");
                        String imageurl = article.getString("image");
                        ArticleEntity lin = new ArticleEntity(uri, title, time, source_title, imageurl, false, null);
                        fragment.alArticleInfo.add(0, lin);
                        count++;
                        if (!fragment.getArticleCachedStatus(uri))
                            fragment.daoArticle.insert(lin);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //sync to the listView
                fragment.articleInfoAdapter.notifyItemRangeInserted(0, count);
                fragment.infoListView.smoothScrollToPosition(0);
                fragment.mRefreshLayout.endRefreshing();// finish fetching from sever

            }

        }
    }

    /**
     * The type Loading task.
     */
    static class LoadingTask extends BaseAsyncTask {
        private WeakReference<ArticleListFragment> weakFragmentRef;

        /**
         * Instantiates a new Loading task.
         *
         * @param fragment the fragment
         */
        public LoadingTask(ArticleListFragment fragment) {
            weakFragmentRef = new WeakReference<ArticleListFragment>(fragment);
        }


        @Override
        protected void onPostExecute(String result) {
            ArticleListFragment fragment = weakFragmentRef.get();
            if (fragment == null) return;

            int size = fragment.alArticleInfo.size();
            int count = 0;
            if (result == null) {
                fragment.mRefreshLayout.endLoadingMore();
                Toast.makeText(fragment.getContext(), "无网络或fapiKey错误", Toast.LENGTH_SHORT).show();
            } else if (result.equals("Timeout")) {
                fragment.mRefreshLayout.endLoadingMore();
                Toast.makeText(fragment.getContext(), "连接超时", Toast.LENGTH_SHORT).show();
            } else {

                try {
                    fragment.mRefreshLayout.endLoadingMore();
                    String uri, title, source_title, time;
                    JSONObject reader = new JSONObject(result);
                    JSONObject articles = reader.getJSONObject("articles");
                    JSONArray results = articles.getJSONArray("results");
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject article = results.getJSONObject(i);
                        uri = article.getString("uri");
                        title = article.getString("title");
                        time = article.getString("dateTime");
                        JSONObject source = article.getJSONObject("source");
                        source_title = source.getString("title");
                        String imageurl = article.getString("image");
                        ArticleEntity lin = new ArticleEntity(uri, title, time, source_title, imageurl, false, null);

                        count++;
                        if (!fragment.getArticleCachedStatus(uri)) {
                            fragment.alArticleInfo.add(lin);
                            fragment.daoArticle.insert(lin);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //sync to the listView
                fragment.articleInfoAdapter.notifyItemRangeInserted(size, count);
            }
        }
    }

}



