package com.frank;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Autoclicker {
    private static Robot robot;
    private static ScheduledExecutorService scheduler;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static int clickButton = java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
    private static int activationKeyCode = NativeKeyEvent.VC_F6;
    private static int activationMouseButton = -1;
    private static boolean isSettingActivationKey = false;
    private static final Map<Integer, Integer> keyCodeMap = new HashMap<>();

    public static void main(String[] args) throws AWTException {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo establecer el Look and Feel Nimbus");
        }

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        initializeKeyCodeMap();

        JFrame frame = new JFrame("AutoClicker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 350);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuración"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel intervalLabel = new JLabel("Clics por segundo:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(intervalLabel, gbc);

        JTextField cpsField = new JTextField("10", 10);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(cpsField, gbc);

        JLabel clickTypeLabel = new JLabel("Tipo de clic:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(clickTypeLabel, gbc);

        String[] clickOptions = {"Izquierdo", "Derecho"};
        JComboBox<String> clickTypeCombo = new JComboBox<>(clickOptions);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(clickTypeCombo, gbc);

        JLabel activationKeyLabel = new JLabel("Tecla de activación:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(activationKeyLabel, gbc);

        JButton activationKeyButton = new JButton("Seleccionar tecla");
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(activationKeyButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(15), gbc);

        JButton startButton = new JButton("Iniciar");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(startButton, gbc);

        JButton stopButton = new JButton("Detener");
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(stopButton, gbc);

        frame.add(panel);

        robot = new Robot();
        frame.setLocationRelativeTo(null);

        startButton.addActionListener(e -> iniciarAutoclicker(cpsField, clickTypeCombo));
        stopButton.addActionListener(e -> detenerAutoclicker());

        activationKeyButton.addActionListener(e -> {
            isSettingActivationKey = true;
            activationKeyButton.setText("Esperando tecla...");
        });

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    int mappedKeyCode = mapKeyCode(e.getKeyCode());
                    if (isSettingActivationKey) {
                        activationKeyCode = mappedKeyCode;
                        activationMouseButton = -1;
                        activationKeyButton.setText(KeyEvent.getKeyText(activationKeyCode));
                        isSettingActivationKey = false;
                        return;
                    }
                    if (mappedKeyCode == activationKeyCode) {
                        if (running.get()) {
                            detenerAutoclicker();
                        } else {
                            iniciarAutoclicker(cpsField, clickTypeCombo);
                        }
                    }
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                }

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {
                }
            });

            GlobalScreen.addNativeMouseListener(new NativeMouseInputListener() {
                @Override
                public void nativeMousePressed(NativeMouseEvent e) {
                    if (isSettingActivationKey) {
                        activationMouseButton = e.getButton();
                        activationKeyCode = -1;
                        activationKeyButton.setText("Botón del mouse " + e.getButton());
                        isSettingActivationKey = false;
                        return;
                    }
                    if (e.getButton() == activationMouseButton) {
                        if (running.get()) {
                            detenerAutoclicker();
                        } else {
                            iniciarAutoclicker(cpsField, clickTypeCombo);
                        }
                    }
                }

                @Override
                public void nativeMouseReleased(NativeMouseEvent e) {
                }

                @Override
                public void nativeMouseClicked(NativeMouseEvent e) {
                }
            });
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

        frame.setVisible(true);
    }

    private static void iniciarAutoclicker(JTextField cpsField, JComboBox<String> clickTypeCombo) {
        if (!running.get()) {
            int cps;
            try {
                cps = Integer.parseInt(cpsField.getText());
                running.set(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Valor inválido para clics por segundo.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                running.set(false);
                return;
            }

            int interval = 1000 / cps;
            String selectedClick = (String) clickTypeCombo.getSelectedItem();
            if (selectedClick != null && selectedClick.equals("Izquierdo")) {
                clickButton = java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
            } else {
                clickButton = java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
            }

            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                if (running.get()) {
                    robot.mousePress(clickButton);
                    robot.mouseRelease(clickButton);
                }
            }, 0, interval, TimeUnit.MILLISECONDS);
        }
    }

    private static void detenerAutoclicker() {
        if (running.get()) {
            running.set(false);
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }
    }

    private static void initializeKeyCodeMap() {
        try {
            Field[] keyEventFields = KeyEvent.class.getDeclaredFields();
            for (Field field : keyEventFields) {
                if (field.getName().startsWith("VK_")) {
                    int keyEventCode = field.getInt(null);
                    String keyName = field.getName().substring(3);
                    try {
                        Field nativeKeyEventField = NativeKeyEvent.class.getField("VC_" + keyName);
                        int nativeKeyEventCode = nativeKeyEventField.getInt(null);
                        keyCodeMap.put(nativeKeyEventCode, keyEventCode);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static int mapKeyCode(int nativeKeyCode) {
        return keyCodeMap.getOrDefault(nativeKeyCode, nativeKeyCode);
    }
}