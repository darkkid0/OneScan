package burp.vaycore.onescan.ui.tab;

import burp.vaycore.common.filter.FilterRule;
import burp.vaycore.common.filter.TableFilter;
import burp.vaycore.common.filter.TableFilterPanel;
import burp.vaycore.common.helper.UIHelper;
import burp.vaycore.common.layout.HLayout;
import burp.vaycore.common.layout.VLayout;
import burp.vaycore.common.utils.IPUtils;
import burp.vaycore.common.utils.Utils;
import burp.vaycore.common.widget.HintTextField;
import burp.vaycore.onescan.bean.FpData;
import burp.vaycore.onescan.bean.TaskData;
import burp.vaycore.onescan.common.Config;
import burp.vaycore.onescan.common.DialogCallbackAdapter;
import burp.vaycore.onescan.manager.FpManager;
import burp.vaycore.onescan.ui.base.BaseTab;
import burp.vaycore.onescan.ui.widget.SimpleWordlist;
import burp.vaycore.onescan.ui.widget.TaskTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据看板
 * <p>
 * Created by vaycore on 2022-08-07.
 */
public class DataBoardTab extends BaseTab {

    public static final String EVENT_IMPORT_URL = "event-import-url";
    public static final String EVENT_STOP_TASK = "event-stop-task";

    private TaskTable mTaskTable;
    private JCheckBox mListenProxyMessage;
    private JCheckBox mExcludeHeader;
    private JCheckBox mReplaceHeader;
    private JCheckBox mDirScan;
    private ArrayList<FilterRule> mLastFilters;
    private HintTextField mFilterRuleText;

    @Override
    protected void initData() {
    }

    @Override
    protected void initView() {
    }

    public String getTitleName() {
        return "Databoard";
    }

    public void testInit() {
        init(new JTextArea("Request"), new JTextArea("Response"));
        // 添加测试数据
        for (int i = 0; i < 100; i++) {
            TaskData data = new TaskData();
            data.setMethod(i % 12 == 0 ? "POST" : "GET");
            data.setHost("https://www.baidu.com");
            data.setUrl("/?s=" + i);
            data.setTitle("百度一下，你就知道");
            data.setIp(IPUtils.randomIPv4());
            data.setStatus(200);
            data.setLength(Utils.randomInt(99999));
            FpData fp = Utils.getRandomItem(FpManager.getList());
            if (fp != null) {
                data.setFingerprint(fp.getName());
            }
            data.setComment("");
            data.setFrom("Proxy");
            data.setReqResp(new Object());
            getTaskTable().addTaskData(data);
        }
    }

    public void init(Component requestTextEditor, Component responseTextEditor) {
        if (requestTextEditor == null || responseTextEditor == null) {
            return;
        }
        setLayout(new VLayout(0));
        // 控制栏
        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(new EmptyBorder(0, 0, 0, 5));
        controlPanel.setFocusable(false);
        controlPanel.setLayout(new HLayout(5, true));
        add(controlPanel);
        // 代理监听开关
        mListenProxyMessage = newJCheckBox(controlPanel, "Listen Proxy Message", Config.KEY_ENABLE_LISTEN_PROXY);
        // 请求头排除开关
        mExcludeHeader = newJCheckBox(controlPanel, "Exclude Header", Config.KEY_ENABLE_EXCLUDE_HEADER);
        // 请求头替换开关
        mReplaceHeader = newJCheckBox(controlPanel, "Replace Header", Config.KEY_ENABLE_REPLACE_HEADER);
        // 递归扫描开关
        mDirScan = newJCheckBox(controlPanel, "DirScan", Config.KEY_ENABLE_DIR_SCAN);
        // 导入Url
        JButton importUrlBtn = new JButton("Import url");
        importUrlBtn.addActionListener((e) -> importUrl());
        controlPanel.add(importUrlBtn);
        // 停止按钮
        JButton stopBtn = new JButton("Stop");
        stopBtn.addActionListener((e) -> stopTask());
        controlPanel.add(stopBtn);
        // 过滤设置
        controlPanel.add(new JPanel(), "1w");
        mFilterRuleText = new HintTextField();
        mFilterRuleText.setEditable(false);
        mFilterRuleText.setHintText("No filter rules.");
        controlPanel.add(mFilterRuleText, "1w");
        JButton filterBtn = new JButton("Filter");
        filterBtn.addActionListener(e -> showSetupFilterDialog());
        controlPanel.add(filterBtn, "65px");
        // 主面板
        JSplitPane mainSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPanel.setResizeWeight(0.55D);
        mainSplitPanel.setDividerSize(3);
        // 请求列表
        mTaskTable = new TaskTable();
        JScrollPane scrollPane = new JScrollPane(mTaskTable);
        scrollPane.setPreferredSize(new Dimension(scrollPane.getWidth(), 0));

        // 请求和响应面板
        JSplitPane dataSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        dataSplitPanel.setResizeWeight(0.5D);
        dataSplitPanel.setDividerSize(3);
        dataSplitPanel.add(requestTextEditor, JSplitPane.LEFT);
        dataSplitPanel.add(responseTextEditor, JSplitPane.RIGHT);

        // 添加子面板控件
        mainSplitPanel.add(scrollPane, JSplitPane.LEFT);
        mainSplitPanel.add(dataSplitPanel, JSplitPane.RIGHT);
        // 将布局进行展示
        add(mainSplitPanel, "100%");
    }

    private JCheckBox newJCheckBox(JPanel panel, String text, String configKey) {
        JCheckBox checkBox = new JCheckBox(text, Config.getBoolean(configKey));
        checkBox.setFocusable(false);
        checkBox.setMargin(new Insets(5, 5, 5, 5));
        panel.add(checkBox);
        checkBox.addActionListener(e -> {
            boolean configSelected = Config.getBoolean(configKey);
            boolean selected = checkBox.isSelected();
            if (selected == configSelected) {
                return;
            }
            // 保存配置
            Config.put(configKey, String.valueOf(selected));
        });
        return checkBox;
    }

    private void importUrl() {
        JPanel panel = new JPanel(new VLayout());
        panel.setPreferredSize(new Dimension(440, 400));
        SimpleWordlist wordlist = new SimpleWordlist();
        panel.add(wordlist, "1w");
        int ret = UIHelper.showCustomDialog("Import Url", panel, this);
        if (ret == JOptionPane.OK_OPTION) {
            List<String> data = wordlist.getListData();
            if (data.isEmpty()) {
                return;
            }
            sendTabEvent(EVENT_IMPORT_URL, data);
        }
    }

    private void stopTask() {
        sendTabEvent(EVENT_STOP_TASK);
        // 提示信息
        String message = hasListenProxyMessage() ?
                "Stop OK!\nSet 'Listen Proxy Message' temporarily to off" :
                "Stop OK!";
        // 停止后，将代理监听关闭
        mListenProxyMessage.setSelected(false);
        UIHelper.showTipsDialog(message);
    }

    public TaskTable getTaskTable() {
        return mTaskTable;
    }

    public boolean hasListenProxyMessage() {
        return mListenProxyMessage != null && mListenProxyMessage.isSelected();
    }

    public boolean hasExcludeHeader() {
        return mExcludeHeader != null && mExcludeHeader.isSelected();
    }

    public boolean hasReplaceHeader() {
        return mReplaceHeader != null && mReplaceHeader.isSelected();
    }

    public boolean hasDirScan() {
        return mDirScan != null && mDirScan.isSelected();
    }

    /**
     * 设置过滤对话框
     */
    private void showSetupFilterDialog() {
        TableFilterPanel panel = new TableFilterPanel(TaskTable.TaskTableModel.COLUMN_NAMES, mLastFilters);
        panel.showDialog(new DialogCallbackAdapter() {
            @Override
            public void onConfirm(ArrayList<FilterRule> filterRules, ArrayList<TableFilter<AbstractTableModel>> filters, String rulesText) {
                mTaskTable.setRowFilter(RowFilter.andFilter(filters));
                mFilterRuleText.setText(rulesText);
                mLastFilters = filterRules;
            }

            @Override
            public void onReset() {
                mTaskTable.setRowFilter(null);
                mFilterRuleText.setText("");
                mLastFilters = null;
            }
        });
    }
}
