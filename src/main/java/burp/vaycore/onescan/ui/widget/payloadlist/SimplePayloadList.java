package burp.vaycore.onescan.ui.widget.payloadlist;

import burp.vaycore.common.helper.UIHelper;
import burp.vaycore.common.layout.HLayout;
import burp.vaycore.common.layout.VLayout;
import burp.vaycore.common.utils.ClassUtils;
import burp.vaycore.common.utils.StringUtils;
import burp.vaycore.common.widget.HintTextField;
import burp.vaycore.onescan.common.OnDataChangeListener;
import burp.vaycore.onescan.ui.widget.payloadlist.rule.AddPrefix;
import burp.vaycore.onescan.ui.widget.payloadlist.rule.AddSuffix;
import burp.vaycore.onescan.ui.widget.payloadlist.rule.MatchReplace;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单Payload列表
 * <p>
 * Created by vaycore on 2022-09-02.
 */
public class SimplePayloadList extends JPanel implements ActionListener {

    private static final Class<?>[] sRuleModules = {
            AddPrefix.class,
            AddSuffix.class,
            MatchReplace.class,
    };

    private final PayloadListModel mListModel;
    private JTable mListView;
    private String mAction;
    private OnDataChangeListener mOnDataChangeListener;
    private final List<HintTextField> mParamInputViews = new ArrayList<>();

    public SimplePayloadList() {
        this(null);
    }

    public SimplePayloadList(ArrayList<PayloadItem> list) {
        mListModel = new PayloadListModel();
        mListModel.addTableModelListener(e -> dataChanged());
        initData(list);
        initView();
    }

    public void setActionCommand(String action) {
        this.mAction = action;
    }

    public String getActionCommand() {
        if (StringUtils.isEmpty(this.mAction)) {
            return toString();
        }
        return this.mAction;
    }

    private void initData(ArrayList<PayloadItem> list) {
        setListData(list);
    }

    /**
     * 设置列表数据
     *
     * @param list 数据列表
     */
    public void setListData(ArrayList<PayloadItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        mListModel.clearAll();
        for (PayloadItem item : list) {
            mListModel.add(item);
        }
    }

    /**
     * 获取已启用的列表数据
     *
     * @return 列表数据
     */
    public ArrayList<PayloadItem> getEnableList() {
        return mListModel.getDataList(true);
    }

    /**
     * 获取列表数据
     *
     * @return 列表数据
     */
    public ArrayList<PayloadItem> getDataList() {
        return mListModel.getDataList(false);
    }

    /**
     * 添加数据监听器
     *
     * @param l 监听器
     */
    public void setOnDataChangeListener(OnDataChangeListener l) {
        this.mOnDataChangeListener = l;
    }

    private void initView() {
        setLayout(new HLayout(5));
        setPreferredSize(new Dimension(0, 200));

        add(newLeftPanel(), "85px");
        add(newRightPanel(), "400px");
    }

    private JPanel newLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new VLayout(3));
        panel.add(newButton("Add", "add-item"));
        panel.add(newButton("Edit", "edit-item"));
        panel.add(newButton("Remove", "remove-item"));
        panel.add(newButton("Clear", "clear-item"));
        panel.add(newButton("Up", "up-item"));
        panel.add(newButton("Down", "down-item"));
        return panel;
    }

    private JButton newButton(String text, String action) {
        JButton button = new JButton(text);
        button.setActionCommand(action);
        button.addActionListener(this);
        return button;
    }

    private JPanel newRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new VLayout());

        mListView = new JTable(mListModel);
        UIHelper.setTableHeaderAlign(mListView, SwingConstants.CENTER);
        mListView.getColumnModel().getColumn(0).setMinWidth(32);
        mListView.getColumnModel().getColumn(0).setMaxWidth(32);
        mListView.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(mListView);
        panel.add(scrollPane, "1w");
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        switch (action) {
            case "add-item":
                PayloadItem newItem = showItemOptionPane(null);
                mListModel.add(newItem);
                break;
            case "clear-item":
                int state = UIHelper.showOkCancelDialog("确认清空列表？");
                if (state == JOptionPane.OK_OPTION) {
                    mListModel.clearAll();
                }
                break;
        }
        int index = mListView.getSelectedRow();
        if (index < 0) {
            return;
        }
        switch (action) {
            case "edit-item":
                PayloadItem item = mListModel.get(index);
                item = showItemOptionPane(item);
                mListModel.set(index, item);
                break;
            case "remove-item":
                mListModel.remove(index);
                if (index > 0) {
                    mListView.changeSelection(--index, 0, false, false);
                } else {
                    mListView.changeSelection(0, 0, false, false);
                }
                break;
            case "up-item":
                int upIndex = index - 1;
                if (upIndex >= 0) {
                    PayloadItem temp = mListModel.get(upIndex);
                    mListModel.set(upIndex, mListModel.get(index));
                    mListModel.set(index, temp);
                    mListView.changeSelection(upIndex, 0, false, false);
                }
                break;
            case "down-item":
                int downIndex = index + 1;
                if (downIndex < mListModel.size()) {
                    PayloadItem temp = mListModel.get(index);
                    mListModel.set(index, mListModel.get(downIndex));
                    mListModel.set(downIndex, temp);
                    mListView.changeSelection(downIndex, 0, false, false);
                }
                break;
            default:
                break;
        }
    }

    private PayloadItem showItemOptionPane(PayloadItem item) {
        String title = "Add payload processing rule";
        String message = "Enter the details of the payload processing rule.";
        if (item != null) {
            title = "Edit payload processing rule";
        }

        // 布局
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(500, 200));
        panel.setLayout(new VLayout(10));

        // 描述信息
        JLabel label = new JLabel(message);
        panel.add(label);

        // 类型
        JComboBox<String> ruleTypeBox = new JComboBox<>();
        ruleTypeBox.addItem("Select rule type");
        for (Class<?> clz : sRuleModules) {
            Object rule = ClassUtils.newObjectByClass(clz);
            if (!(rule instanceof PayloadRule)) {
                continue;
            }
            ruleTypeBox.addItem(((PayloadRule) rule).ruleName());
        }
        ruleTypeBox.addActionListener(e -> {
            int index = ruleTypeBox.getSelectedIndex() - 1;
            if (index < 0) {
                clearAllParamItemView(panel);
                return;
            }
            clearAllParamItemView(panel);
            PayloadRule rule = getPayloadRuleByIndex(index);
            if (rule == null) {
                return;
            }
            mParamInputViews.clear();
            for (int i = 0; i < rule.paramCount(); i++) {
                JPanel itemView = newParamItemView(rule.paramName(i));
                mParamInputViews.add((HintTextField) itemView.getComponent(1));
                panel.add(itemView);
            }
            UIHelper.refreshUI(panel);
        });
        panel.add(ruleTypeBox);

        // 范围
        JComboBox<String> ruleScopeBox = new JComboBox<>();
        ruleScopeBox.addItem("Select rule scope");
        ruleScopeBox.addItem("URL");
        ruleScopeBox.addItem("Header");
        ruleScopeBox.addItem("Body");
        ruleScopeBox.addItem("Request");
        panel.add(ruleScopeBox);

        // 数据填充
        if (item != null) {
            PayloadRule rule = item.getRule();
            int ruleTypeIndex = getIndexByPayloadRule(rule);
            ruleTypeBox.setSelectedIndex(ruleTypeIndex + 1);
            ruleScopeBox.setSelectedIndex(item.getScope() + 1);
            // 填充参数值
            if (ruleTypeIndex >= 0) {
                for (int i = 0; i < mParamInputViews.size(); i++) {
                    HintTextField field = mParamInputViews.get(i);
                    field.setText(rule.getParamValues()[i]);
                }
            }
        }

        // 弹窗
        int showState = JOptionPane.showConfirmDialog(JOptionPane.getRootFrame(),
                panel, title, JOptionPane.OK_CANCEL_OPTION);
        // 用户确认，组合参数
        if (showState == JOptionPane.YES_OPTION) {
            if (item == null) {
                item = new PayloadItem();
                item.setEnabled(true);
                item.setId(mListModel.size());
                item.setScope(-1);
            }
            // 类型
            int ruleType = ruleTypeBox.getSelectedIndex() - 1;
            PayloadRule rule = getPayloadRuleByIndex(ruleType);
            if (rule == null) {
                UIHelper.showTipsDialog("Please select rule type.");
                return showItemOptionPane(item);
            }
            item.setRule(rule);

            // 参数提醒
            StringBuilder errorTips = new StringBuilder();
            // 范围
            int ruleScope = ruleScopeBox.getSelectedIndex() - 1;
            if (ruleScope < 0) {
                errorTips.append("Please select rule scope.\n");
            }
            item.setScope(ruleScope);
            // 参数填充
            for (int i = 0; i < rule.paramCount(); i++) {
                String paramName = rule.paramName(i).toLowerCase();
                String paramsValue = mParamInputViews.get(i).getText();
                if (StringUtils.isEmpty(paramsValue)) {
                    errorTips.append("Please input ").append(paramName).append(" param value.\n");
                    continue;
                }
                rule.setParamValue(i, paramsValue);
            }
            if (StringUtils.isNotEmpty(errorTips.toString())) {
                UIHelper.showTipsDialog(errorTips.toString());
                return showItemOptionPane(item);
            }
            return item;
        }
        return null;
    }

    private JPanel newParamItemView(String paramName) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(0, 25));
        panel.setLayout(new HLayout());

        JLabel label = new JLabel(paramName + ":");
        label.setBorder(new EmptyBorder(0, 1, 0, 5));
        panel.add(label);

        HintTextField textField = new HintTextField();
        textField.setHintText("Please enter the " + paramName.toLowerCase() + " value");
        panel.add(textField, "1w");
        return panel;
    }

    private void clearAllParamItemView(JPanel panel) {
        if (panel.getComponentCount() <= 3) {
            return;
        }
        for (int i = panel.getComponentCount() - 1; i >= 3; i--) {
            panel.remove(i);
        }
        SwingUtilities.updateComponentTreeUI(panel);
    }

    private PayloadRule getPayloadRuleByIndex(int index) {
        if (index < 0 || index >= sRuleModules.length) {
            return null;
        }
        return (PayloadRule) ClassUtils.newObjectByClass(sRuleModules[index]);
    }

    private int getIndexByPayloadRule(PayloadRule rule) {
        if (rule == null) {
            return -1;
        }
        for (int i = 0; i < sRuleModules.length; i++) {
            Class<?> clz = sRuleModules[i];
            if (clz.getSimpleName().equals(rule.getClass().getSimpleName())) {
                return i;
            }
        }
        return -1;
    }

    public static PayloadRule getPayloadRuleByType(String ruleType) {
        if (StringUtils.isEmpty(ruleType)) {
            return null;
        }
        for (int i = 0; i < sRuleModules.length; i++) {
            Class<?> clz = sRuleModules[i];
            if (clz.getSimpleName().equals(ruleType)) {
                return (PayloadRule) ClassUtils.newObjectByClass(clz);
            }
        }
        return null;
    }

    /**
     * 列表数据有修改
     */
    private void dataChanged() {
        if (mOnDataChangeListener != null) {
            mOnDataChangeListener.onDataChange(getActionCommand());
        }
    }
}
