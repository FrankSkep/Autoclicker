package com.frank;

// Importar las clases necesarias
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Autoclicker {
    private static Robot robot; // Robot para simular clics del mouse
    private static Thread clickerThread; // Hilo para el autoclicker
    private static final AtomicBoolean running = new AtomicBoolean(false); // Estado del autoclicker
    private static int clickButton = java.awt.event.InputEvent.BUTTON1_DOWN_MASK; // Botón por defecto: izquierdo
    private static int activationKeyCode = NativeKeyEvent.VC_F6; // Tecla para activar por defecto
    private static int activationMouseButton = -1; // Botón del mouse para activar por defecto (ninguno)
    private static boolean isSettingActivationKey = false; // Bandera para indicar si la tecla de activación está siendo configurada

    // Mapa para almacenar los mapeos de teclas
    private static final Map<Integer, Integer> keyCodeMap = new HashMap<>();

    public static void main(String[] args) throws AWTException {
        // Establecer el Look and Feel Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Si Nimbus no está disponible, usar el Look and Feel por defecto
            System.err.println("No se pudo establecer el Look and Feel Nimbus");
        }

        // Desactivar los logs de JNativeHook
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        // Inicializar el mapa de códigos de tecla
        initializeKeyCodeMap();

        JFrame frame = new JFrame("AutoClicker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
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

        String[] clickOptions = { "Izquierdo", "Derecho" };
        JComboBox<String> clickTypeCombo = new JComboBox<>(clickOptions);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(clickTypeCombo, gbc);

        JLabel activationKeyLabel = new JLabel("Tecla de activación:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(activationKeyLabel, gbc);

        JButton activationKeyButton = new JButton("Ajustar tecla activadora");
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(activationKeyButton, gbc);

        JButton startButton = new JButton("Iniciar");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(startButton, gbc);

        JButton stopButton = new JButton("Detener");
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(stopButton, gbc);

        frame.add(panel);

        // Inicializar el Robot
        robot = new Robot();

        // Centrar la ventana
        frame.setLocationRelativeTo(null);

        // Acción para el botón "Iniciar"
        startButton.addActionListener(e -> iniciarAutoclicker(cpsField, clickTypeCombo));

        // Acción para el botón "Detener"
        stopButton.addActionListener(e -> detenerAutoclicker());

        // Acción para el botón "Presiona una tecla o botón del mouse"
        activationKeyButton.addActionListener(e -> {
            isSettingActivationKey = true; // Indicar que la tecla de activación está siendo configurada
            activationKeyButton.setText("Esperando tecla...");
        });

        // Registrar un key listener global usando JNativeHook para detectar teclas globales
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    int mappedKeyCode = mapKeyCode(e.getKeyCode());
                    if (isSettingActivationKey) {
                        // Configurar la tecla de activación
                        activationKeyCode = mappedKeyCode;
                        activationMouseButton = -1; // Desactivar el botón del mouse como activador
                        activationKeyButton.setText(KeyEvent.getKeyText(activationKeyCode));
                        isSettingActivationKey = false; // Restablecer la bandera
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

            // Registrar un mouse listener global usando JNativeHook para detectar clics del mouse
            GlobalScreen.addNativeMouseListener(new NativeMouseInputListener() {
                @Override
                public void nativeMousePressed(NativeMouseEvent e) {
                    if (isSettingActivationKey) {
                        // Configurar el botón del mouse de activación
                        activationMouseButton = e.getButton();
                        activationKeyCode = -1; // Desactivar la tecla del teclado como activador
                        activationKeyButton.setText("Botón del mouse " + e.getButton());
                        isSettingActivationKey = false; // Restablecer la bandera
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

        // Mostrar la ventana
        frame.setVisible(true);
    }

    private static void iniciarAutoclicker(JTextField cpsField, JComboBox<String> clickTypeCombo) {
        if (!running.get()) {

            int cps;
            try {
                cps = Integer.parseInt(cpsField.getText()); // Clics por segundo ingresados
                running.set(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Valor inválido para clics por segundo.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                running.set(false);
                return;
            }

            int interval = 1000 / cps; // Convertir clics por segundo a milisegundos del intervalo

            // Seleccionar el boton de clic basado en la seleccion del usuario
            String selectedClick = (String) clickTypeCombo.getSelectedItem();
            if (selectedClick != null && selectedClick.equals("Izquierdo")) {
                clickButton = java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
            } else {
                clickButton = java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
            }

            // Crear el hilo del autoclicker
            clickerThread = new Thread(() -> {
                while (running.get()) {
                    robot.mousePress(clickButton);
                    robot.mouseRelease(clickButton);
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            });

            clickerThread.start(); // Iniciar el hilo
        }
    }

    private static void detenerAutoclicker() {
        if (running.get()) {
            running.set(false); // Detener el hilo
            if (clickerThread != null) {
                clickerThread.interrupt(); // esto asegura que se detenga el hilo
            }
        }
    }

    // Método para inicializar el mapa de códigos de tecla
    private static void initializeKeyCodeMap() {
        try {
            // Obtener todos los campos de KeyEvent
            Field[] keyEventFields = KeyEvent.class.getDeclaredFields();
            for (Field field : keyEventFields) {
                if (field.getName().startsWith("VK_")) {
                    int keyEventCode = field.getInt(null);
                    String keyName = field.getName().substring(3); // Quitar el prefijo "VK_"
                    // Buscar el campo correspondiente en NativeKeyEvent
                    try {
                        Field nativeKeyEventField = NativeKeyEvent.class.getField("VC_" + keyName);
                        int nativeKeyEventCode = nativeKeyEventField.getInt(null);
                        keyCodeMap.put(nativeKeyEventCode, keyEventCode);
                    } catch (NoSuchFieldException ignored) {
                        // Si no se encuentra el campo correspondiente, ignorarlo
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Método para mapear los códigos de tecla
    private static int mapKeyCode(int nativeKeyCode) {
        return keyCodeMap.getOrDefault(nativeKeyCode, nativeKeyCode);
    }
}