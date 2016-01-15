package com.example.xlm.mydrawerdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.xlm.mydrawerdemo.API.ArticleService;
import com.example.xlm.mydrawerdemo.API.FormListService;
import com.example.xlm.mydrawerdemo.Activity.ChildArticleActivity;
import com.example.xlm.mydrawerdemo.R;
import com.example.xlm.mydrawerdemo.adapter.ArticleRecyclerAdapter;
import com.example.xlm.mydrawerdemo.adapter.StringListRecyclerViewAdapter;
import com.example.xlm.mydrawerdemo.base.BaseFragment;
import com.example.xlm.mydrawerdemo.bean.Article;
import com.example.xlm.mydrawerdemo.bean.ChangeTitleEvent;
import com.example.xlm.mydrawerdemo.bean.ChildArticle;
import com.example.xlm.mydrawerdemo.bean.Form;
import com.example.xlm.mydrawerdemo.http.Httptools;
import com.example.xlm.mydrawerdemo.utils.SpaceItemDecoration;
import com.example.xlm.mydrawerdemo.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by xlm on 2015/11/16.
 */
public class NormalContentFragment extends BaseFragment {
    private View rootView;
    private RecyclerView mRecyclerView, module;
    private SwipeRefreshLayout mSwipRefreshLayout;
    private LinearLayoutManager mLinearLayoutManager, moduleLayoutManager;
    private ArticleRecyclerAdapter adapterArticle;
    private String formId = "";//板块id,默认综合1
    private String formTitle = "";//板块名
    private int page = 1;//页数
    private DrawerLayout drawerForm;
    private boolean isLoadingMore = false;//是否正在加载中
    //右侧模块adapter
    private StringListRecyclerViewAdapter adaprerModule;
    //帖子内容
    private List<Article> data = new ArrayList<>();
    //右侧板块名
    private List<ChildArticle> modules = new ArrayList<>();
    //帖子评论
    private List<String> commentContentList = new ArrayList<>();
    private Retrofit retrofit;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int layoutId = R.layout.fragment_article_layout;
        if (null != rootView) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (null != parent) {
                parent.removeView(rootView);
            }
        } else {
            rootView = inflater.inflate(layoutId, null);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
    }

    private void initView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mSwipRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        module = (RecyclerView) view.findViewById(R.id.recycler_module);
        drawerForm = (DrawerLayout) view.findViewById(R.id.drawer_form);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void initData() {
        mSwipRefreshLayout.setRefreshing(true);
        retrofit = Httptools.getInstance().getRetrofit();
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        moduleLayoutManager = new LinearLayoutManager(getActivity());
        moduleLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        adapterArticle = new ArticleRecyclerAdapter(data, getActivity());
        adaprerModule = new StringListRecyclerViewAdapter(modules, getActivity());

        module.setAdapter(adaprerModule);
        module.setLayoutManager(moduleLayoutManager);
        module.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(adapterArticle);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.recycler_space);
        mRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));
        //设置刷新时颜色切换
        mSwipRefreshLayout.setColorSchemeResources(android.R.color.holo_red_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_blue_light);
        mSwipRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
                int totalItemCount = mLinearLayoutManager.getItemCount();
                //lastVisibleItem >= totalItemCount - 4 表示剩下4个item自动加载，各位自由选择
                // dy>0 表示向下滑动
                if (lastVisibleItem >= totalItemCount - 4 && dy > 0) {
                    if (isLoadingMore) {
                        Toast.makeText(getActivity(), "加载中", Toast.LENGTH_SHORT).show();
                    } else {
                        loadPage();//这里多线程也要手动控制isLoadingMore
                    }
                }
            }
        });
        //右侧板块点击切换板块
        adaprerModule.setOnItemClickListener(new StringListRecyclerViewAdapter.onItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (drawerForm.isDrawerOpen(Gravity.RIGHT)) {
                    drawerForm.closeDrawer(Gravity.RIGHT);
                }
                formId = modules.get(position).getId();
                page = 1;
                mSwipRefreshLayout.setRefreshing(true);
                getData(false);
                eventBus.post(new ChangeTitleEvent(modules.get(position).getName()));
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        //点击进入串内部
        adapterArticle.setOnItemClickListener(new ArticleRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent intent=new Intent(getActivity(), ChildArticleActivity.class);
                intent.putExtra("id",data.get(position).getId());
                intent.putExtra("title",data.get(position).getTitle());
                startActivity(intent);
            }
        });
        setModuleList();
    }

    public void onEventMainThread(Article obj) {
        data.add(obj);
        adapterArticle.notifyDataSetChanged();
    }

    //加载下一页
    private void loadPage() {
        page++;
        getData(true);
    }

    public void refresh() {
        mSwipRefreshLayout.setRefreshing(true);
        page = 1;
        getData(false);
    }

    private void getData(final boolean isLoad) {
        ArticleService articleService = retrofit.create(ArticleService.class);
        Call<List<Article>> articleCall = articleService.getArticleList(page + "", formId);
        articleCall.enqueue(new Callback<List<Article>>() {
            @Override
            public void onResponse(Response<List<Article>> response, Retrofit retrofit) {
                mSwipRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                //是否是加载下一页,是就不清空
                if (!isLoad) {
                    data.clear();
                }
                data.addAll(response.body());
                adapterArticle.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Throwable throwable) {
                mSwipRefreshLayout.setRefreshing(false);
                ToastUtils.showShort("网络不通");
            }
        });
    }

    //设置板块列表
    private void setModuleList() {
        FormListService formList = retrofit.create(FormListService.class);
        Call<List<Form>> formsCall = formList.getFormList();
        formsCall.enqueue(new Callback<List<Form>>() {
            @Override
            public void onResponse(Response<List<Form>> response, Retrofit retrofit) {
                List<Form> forms = response.body();
                if (forms != null) {
                    for (int i = 0; i < forms.size(); i++) {
                        modules.addAll(forms.get(i).getForums());
                    }
                }
                adaprerModule.notifyDataSetChanged();
                //设置正面内容
                formId = modules.get(0).getId();
                formTitle = modules.get(0).getName();
                eventBus.post(new ChangeTitleEvent(formTitle));
                getData(false);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }
}
