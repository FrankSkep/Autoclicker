# AutoClicker

## Descripción

Aplicación de Java que permite automatizar clics en el ratón. Puedes ajustar la velocidad de los clics, elegir entre clic izquierdo o derecho y configurar una tecla o botón del mouse para activar o desactivar el autoclicker. Utiliza la biblioteca [JNativeHook](https://github.com/kwhat/jnativehook) para detectar eventos a nivel global y mapea códigos de tecla nativos a códigos de `KeyEvent` de Java.

La aplicación utiliza hilos (threads) para gestionar el autoclicker, permitiendo que el clic automático ocurra de manera independiente del hilo principal, lo que garantiza una interfaz de usuario receptiva.

<div align="center">
	<img src="https://i.ibb.co/d44RDv7/img.png" alt="imagen">
</div>

## Características

- **Ajuste de velocidad de clics**: Configura los clics por segundo para controlar la frecuencia del autoclicker.
- **Tipo de clic**: Elige entre clic izquierdo o derecho.
- **Activación global**: Usa JNativeHook para detectar eventos a nivel global, incluso si la aplicación está minimizada.
- **Configuración de teclas**: Configura una tecla o botón del mouse para activar o desactivar el autoclicker.
- **Interfaz gráfica**: Una ventana fácil de usar para configurar y controlar el autoclicker.
- **Hilos (Threads)**: Utiliza hilos para ejecutar el autoclicker en segundo plano sin afectar la interfaz gráfica.


## Instalación

1. **Requisitos**
   - Java JDK 17 o superior.
   - Biblioteca [JNativeHook](https://github.com/kwhat/jnativehook) (incluida en el proyecto).

2. **Ejecutar el Proyecto**
   - Clona el repositorio.
   - Importa el proyecto en un IDE.
   - Compila y ejecuta la clase `Autoclicker`.