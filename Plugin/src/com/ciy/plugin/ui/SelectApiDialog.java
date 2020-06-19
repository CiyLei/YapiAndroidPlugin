package com.ciy.plugin.ui;

import com.ciy.plugin.Constants;
import com.ciy.plugin.ShowInputDialogAction;
import com.ciy.plugin.modle.*;
import com.ciy.plugin.utils.URLConstantGenerate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SelectApiDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList lsApiList;
    private JLabel lb;
    private JComboBox cbModule;
    private JTextField tfPack;
    private JLabel lbTitle;
    private JButton btnReverse;
    private JButton btnSelectAll;
    private Project project;
    private ProjectInfoBean projectInfo;
    private SelectApiDialogListener listener;
    private OkHttpClient httpClient;
    private List<Module> moduleList;
    private List<CatMenuBean> catMenuBeanList;
    private List<ApiBean> apiList;

    public SelectApiDialog(Project project, ProjectInfoBean projectInfo, SelectApiDialogListener listener) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("选择Api");
        setSize(1300, 800);
        // 屏幕居中
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(buttonOK);
        this.project = project;
        this.projectInfo = projectInfo;
        this.listener = listener;
        httpClient = new OkHttpClient();
        moduleList = Arrays.asList(ModuleManager.getInstance(project).getModules());
        catMenuBeanList = new ArrayList<>();
        apiList = new ArrayList<>();
        lbTitle.setText(projectInfo.getName());

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        btnReverse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                apiList.stream().forEach(it -> {
                    it.setSelect(!it.getSelect());
                });
                refreshListData();
            }
        });

        btnSelectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                apiList.stream().forEach(it -> {
                    it.setSelect(true);
                });
                refreshListData();
            }
        });

        tfPack.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readSelectApi();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        getCatMenu();

        loadModule();
    }

    /**
     * 获取所有分类
     */
    private void getCatMenu() {
        Integer projectId = projectInfo.get_id();
        HttpUrl url = HttpUrl.parse(PropertiesComponent.getInstance().getValue(Constants.KEY_YAPI) + "/api/interface/getCatMenu").newBuilder()
                .addQueryParameter("project_id", projectId.toString())
                .addQueryParameter("token", PropertiesComponent.getInstance().getValue(Constants.KEY_TOKEN)).build();
        httpClient.newCall(new Request.Builder().get().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                lb.setText(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                YapiResult<List<CatMenuBean>> catMenuList = new Gson().fromJson(response.body().string(),
                        new TypeToken<YapiResult<List<CatMenuBean>>>() {
                        }.getType());
                if (catMenuList.getErrcode() == 0 && catMenuList.getData() != null) {
                    catMenuBeanList.clear();
                    catMenuBeanList.addAll(catMenuList.getData());
                    getApiList();
                } else {
                    lb.setText(catMenuList.getErrmsg());
                }
            }
        });
    }

    /**
     * 获取api列表
     */
    private void getApiList() {
        Integer projectId = projectInfo.get_id();
        HttpUrl url = HttpUrl.parse(PropertiesComponent.getInstance().getValue(Constants.KEY_YAPI) + "/api/interface/list").newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("limit", String.valueOf(Integer.MAX_VALUE))
                .addQueryParameter("project_id", projectId.toString())
                .addQueryParameter("token", PropertiesComponent.getInstance().getValue(Constants.KEY_TOKEN)).build();
        httpClient.newCall(new Request.Builder().get().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                lb.setText(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                YapiResult<ApiListBean> apiListBean = new Gson().fromJson(response.body().string(),
                        new TypeToken<YapiResult<ApiListBean>>() {
                        }.getType());
                if (apiListBean.getErrcode() == 0 && apiListBean.getData() != null) {
                    apiList.clear();
                    apiList.addAll(apiListBean.getData().getList());
                    readSelectApi();
                    refreshListData();
                } else {
                    lb.setText(apiListBean.getErrmsg());
                }
            }
        });
    }

    /**
     * 读取已经选择的api
     */
    private void readSelectApi() {
        Module selectModule = moduleList.get(cbModule.getSelectedIndex());
        if (selectModule != null) {
            VirtualFile sourcePath = getSourcePath(selectModule);
            if (sourcePath == null) {
                lb.setText("无法找到源代码文件");
            } else {
                String pack = tfPack.getText().replace(".", "/");
                VirtualFile apiServiceFile = sourcePath.findFileByRelativePath(pack + "/" +
                        ShowInputDialogAction.Companion.getUrlConstantClassName() + ".kt");
                if (apiServiceFile != null && apiServiceFile.exists()) {
                    // URLConstant 存在读取里面的url
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(apiServiceFile.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String len = "";
                        while ((len = br.readLine()) != null) {
                            sb.append(len);
                        }
                        String sourceText = sb.toString();
                        if (!sourceText.isEmpty()) {
                            Pattern pattern = Pattern.compile("\\$" + URLConstantGenerate.INSTANCE.getPrefixPropertyName() +
                                    "(.+?)\\$" + URLConstantGenerate.INSTANCE.getSuffixPropertyName());
                            Matcher matcher = pattern.matcher(sourceText);
                            List<String> urlList = new ArrayList<>();
                            while (matcher.find()) {
                                if (matcher.group(1) != null) {
                                    urlList.add(matcher.group(1));
                                }
                            }
                            apiList.stream().forEach(it -> {
                                it.setSelect(urlList.contains(it.getPath()));
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // URLConstant 不存在则全选
                    apiList.forEach(it -> {
                        it.setSelect(false);
                    });
                }
                refreshListData();
            }
        }
    }

    /**
     * 刷新列表数据
     */
    private void refreshListData() {
        List<Object> listData = new ArrayList<>();
        catMenuBeanList.forEach(it -> {
            listData.add(it.getName());
            listData.addAll(Arrays.asList(apiList.stream().filter(it2 -> it2.getCatid() == it.get_id()).toArray()));
        });
        lsApiList.setListData(listData.toArray());
        lsApiList.setCellRenderer(new SelectApiCellRenderer());
        if (lsApiList.getListSelectionListeners().length == 0) {
            lsApiList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    // 每选择一次，就会触发2次这个回调，这个getValueIsAdjusting能控制只触发后一次
                    if (e.getValueIsAdjusting()) {
                        // 选择位置
                        int selectIndex = lsApiList.getSelectedIndex();
                        Object data = listData.get(selectIndex);
                        if (data instanceof ApiBean) {
                            ((ApiBean) data).setSelect(!((ApiBean) data).getSelect());
                        }
                    }
                }
            });
        }
    }

    /**
     * 加载所有模块
     */
    private void loadModule() {
        for (Module module : moduleList) {
            cbModule.addItem(module.getName());
        }
        // 选择root模块以外的第一个模块
        if (moduleList.size() > 1) {
            cbModule.setSelectedIndex(1);
        }
    }

    private void onOK() {
        if (cbModule.getSelectedIndex() < moduleList.size() && !tfPack.getText().isEmpty()) {
            Module selectModule = moduleList.get(cbModule.getSelectedIndex());
            VirtualFile sourcePath = getSourcePath(selectModule);
            if (sourcePath == null) {
                lb.setText("无法找到源代码文件");
            } else {
                if (listener != null) {
                    this.listener.onOk(selectModule, tfPack.getText(), apiList.stream().filter(ApiBean::getSelect).collect(Collectors.toList()));
                }
                dispose();
            }
        } else {
            lb.setText("模块或者包名格式错误");
        }
    }

    /**
     * 获取模块源代码文件
     *
     * @param module 模块
     * @return 源代码文件
     */
    private VirtualFile getSourcePath(Module module) {
        VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
        if (sourceRoots != null && sourceRoots.length > 0) {
            for (VirtualFile sourceRoot : sourceRoots) {
                if (sourceRoot.getPath().endsWith("src/main/java")) {
                    return sourceRoot;
                }
            }
        }
        return null;
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        SelectApiDialog dialog = new SelectApiDialog(null, null, null);
        dialog.setVisible(true);
        System.exit(0);
    }

    public interface SelectApiDialogListener {
        void onOk(Module module, String packName, List<ApiBean> apiBeans);
    }
}
