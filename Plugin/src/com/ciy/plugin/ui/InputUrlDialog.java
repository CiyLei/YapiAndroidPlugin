package com.ciy.plugin.ui;

import com.ciy.plugin.Constants;
import com.ciy.plugin.modle.ProjectInfoBean;
import com.ciy.plugin.modle.YapiResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class InputUrlDialog extends JDialog {
    private JPanel contentPane;
    private JButton btnNext;
    private JTextField tfUrl;
    private JTextField tfToken;
    private JLabel lb;
    private Project project;
    private InputUrlDialogListener listener;
    private OkHttpClient httpClient;

    public InputUrlDialog(Project project, InputUrlDialogListener listener) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("ApiList生成");
        setSize(550, 200);
        // 屏幕居中
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(btnNext);
        this.project = project;
        this.listener = listener;
        httpClient = new OkHttpClient();
        if (!PropertiesComponent.getInstance().getValue(Constants.KEY_YAPI, "").isEmpty()) {
            // 读取历史记录
            tfUrl.setText(PropertiesComponent.getInstance().getValue(Constants.KEY_YAPI));
            tfToken.setText(PropertiesComponent.getInstance().getValue(Constants.KEY_TOKEN));
        }

        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNext();
            }
        });
    }

    private void onNext() {
        // add your code here
        if (!tfUrl.getText().isEmpty() && !tfToken.getText().isEmpty() && tfUrl.getText().startsWith("http")) {
            httpClient.newCall(new Request.Builder().get().url(tfUrl.getText() + "/api/project/get?token="
                    + tfToken.getText()).build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    lb.setText(e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    PropertiesComponent.getInstance().setValue(Constants.KEY_YAPI, tfUrl.getText());
                    PropertiesComponent.getInstance().setValue(Constants.KEY_TOKEN, tfToken.getText());
                    YapiResult<ProjectInfoBean> projectInfo = new Gson().fromJson(response.body().string(),
                            new TypeToken<YapiResult<ProjectInfoBean>>() {
                            }.getType());
                    if (projectInfo.getErrcode() == 0 && projectInfo.getData() != null) {
                        if (listener != null) {
                            listener.onNext(projectInfo.getData());
                        }
                        dispose();
                    }
                    lb.setText(projectInfo.getErrmsg());
                }
            });
        } else {
            lb.setText("格式不对");
        }
    }

    public static void main(String[] args) {
        InputUrlDialog dialog = new InputUrlDialog(null, projectInfo -> new SelectApiDialog(null, projectInfo, (module, packName, apiBeans) -> {

        }).setVisible(true));
        dialog.setVisible(true);
        System.exit(0);
    }

    public interface InputUrlDialogListener {
        void onNext(ProjectInfoBean projectInfo);
    }
}
