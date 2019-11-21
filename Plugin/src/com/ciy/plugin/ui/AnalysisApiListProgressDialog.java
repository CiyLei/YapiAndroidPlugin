package com.ciy.plugin.ui;

import com.ciy.plugin.Constants;
import com.ciy.plugin.modle.ApiBean;
import com.ciy.plugin.modle.ApiInfoBean;
import com.ciy.plugin.modle.YapiResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisApiListProgressDialog extends JDialog {
    private JPanel contentPane;
    private JProgressBar pb;
    private JLabel lb;
    private List<ApiBean> apiList;
    private List<ApiInfoBean> apiInfoBeans;
    private int apiListIndex = 0;
    private OkHttpClient httpClient;

    public AnalysisApiListProgressDialog(List<ApiBean> apiList) {
        setContentPane(contentPane);
        setModal(true);
        setSize(700, 100);
        setTitle("分析中");
        setLocationRelativeTo(null);

        this.apiList = apiList;
        httpClient = new OkHttpClient();
        apiInfoBeans = new ArrayList<>();

        pb.setMaximum(apiList.size());

        startAnalysis(nextApi());
    }

    private ApiBean nextApi() {
        if (apiList != null && apiListIndex < apiList.size()) {
            return apiList.get(apiListIndex++);
        }
        return null;
    }

    private void startAnalysis(ApiBean apiBean) {
        if (apiBean != null && apiListIndex < 20) {
            int id = apiBean.get_id();
            HttpUrl url = HttpUrl.parse(Constants.yapiUrl + "/api/interface/get").newBuilder()
                    .addQueryParameter("id", String.valueOf(id))
                    .addQueryParameter("token", Constants.token).build();
            httpClient.newCall(new Request.Builder().get().url(url).build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    lb.setText(e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    YapiResult<ApiInfoBean> apiInfoBean = new Gson().fromJson(response.body().string(),
                            new TypeToken<YapiResult<ApiInfoBean>>() {}.getType());
                    lb.setText(apiBean.getTitle());
                    pb.setValue(apiListIndex);
                    apiInfoBeans.add(apiInfoBean.getData());
                    startAnalysis(nextApi());
                }
            });
        } else {
            System.out.println(1);
        }
    }

    public static void main(String[] args) {
        AnalysisApiListProgressDialog dialog = new AnalysisApiListProgressDialog(new ArrayList<>());
        dialog.setVisible(true);
        System.exit(0);
    }
}
