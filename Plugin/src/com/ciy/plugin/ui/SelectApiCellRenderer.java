package com.ciy.plugin.ui;

import com.ciy.plugin.modle.ApiBean;

import javax.swing.*;
import java.awt.*;

public class SelectApiCellRenderer implements ListCellRenderer{

    private JPanel pRoot;
    private JLabel lbGroup;
    private JPanel pChild;
    private JCheckBox cbSelect;
    private JLabel lbUrl;
    private JLabel lbMethod;
    private JPanel pGroup;

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        pGroup.setVisible(false);
        pChild.setVisible(false);
        if (value instanceof String) {
            pGroup.setVisible(true);
            lbGroup.setText((String) value);
        } else if (value instanceof ApiBean) {
            pChild.setVisible(true);
            cbSelect.setSelected(((ApiBean) value).getSelect());
            cbSelect.setText(((ApiBean) value).getTitle());
            lbUrl.setText(((ApiBean) value).getPath());
            lbMethod.setText(((ApiBean) value).getMethod());
        }
        return pRoot;
    }
}
