package com.ciy.plugin.ui;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShowErrorListDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea taError;
    private List<Throwable> errorList;

    public ShowErrorListDialog(List<Throwable> errorList) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("错误列表");
        setSize(800, 600);
        // 屏幕居中
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(buttonOK);

        this.errorList = errorList;

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

        taError.setText(errorList.stream().map(it -> (it.toString() + "\n\n")).collect(Collectors.joining()));
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        List<Throwable> es = new ArrayList<>();
        es.add(new Throwable("1232133"));
        es.add(new Throwable("6547447654"));
        ShowErrorListDialog dialog = new ShowErrorListDialog(es);
        dialog.setVisible(true);
        System.exit(0);
    }
}
