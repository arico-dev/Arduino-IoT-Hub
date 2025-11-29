# Arduino IoT Hub

Aplicación Android nativa para controlar y monitorizar una placa Arduino a través de una conexión Bluetooth. Cuenta con un sistema de autenticación de usuarios y un registro de eventos en la nube utilizando Firebase.

## Características Principales

- **Autenticación de Usuarios con Firebase:** Sistema completo de registro e inicio de sesión de usuarios con `Firebase Authentication`, incluyendo validaciones de seguridad.
- **Conexión Bluetooth Robusta:** Escaneo y conexión con dispositivos Bluetooth, optimizada para módulos de puerto serie como el HC-05 mediante una arquitectura de hilos segura.
- **Panel de Control Centralizado:** Una interfaz de usuario clara para interactuar con todos los componentes del hardware.
- **Control de LED:** Encendido y apagado remoto de un LED mediante un interruptor (`Switch`) de Material Design.
- **Monitor de Sensor LDR:** Visualización en tiempo real del valor de un sensor de luz (LDR) con una barra de progreso animada.
- **Registro de Eventos en la Nube:** Guardado de eventos significativos (conexiones, acciones del usuario, cambios de estado del sensor) en `Cloud Firestore`.
- **Historial de Eventos:** Funcionalidad para cargar y visualizar el historial de eventos directamente en la consola de la aplicación.
- **Consola Serial:** Envío de comandos de texto personalizados y recepción de mensajes desde el Arduino para depuración y control avanzado.
- **Diseño Moderno:** Interfaz de usuario pulida siguiendo las guías de Material Design 3, incluyendo tarjetas (`CardView`), campos de texto `TextInputLayout` y soporte para tema oscuro.
- **Flujo de Usuario Completo:** Incluye pantalla de inicio (`Splash`), registro/login, pantalla de descripción del proyecto y navegación coherente.

## Tecnologías Utilizadas

### Aplicación Android
- **Lenguaje:** Kotlin
- **IDE:** Android Studio
- **Librerías Principales:**
    - Componentes de Material Design 3
    - View Binding para la interacción con las vistas
    - AndroidX AppCompat y Activity
    - Firebase Authentication
    - Cloud Firestore

### Hardware
- **Placa:** Compatible con Arduino (Uno, Nano, etc.)
- **Lenguaje:** C++ (Framework de Arduino)
- **Módulo Bluetooth:** HC-05, HC-06 o similar (SPP - Serial Port Profile)

## Código de Arduino (.ino)

Este es el código que debe ser cargado en la placa Arduino para que la comunicación con la app funcione correctamente.

```cpp
#include <SoftwareSerial.h>

// Pines para la comunicación Bluetooth
const int btRxPin = 2;
const int btTxPin = 3;
SoftwareSerial btSerial(btRxPin, btTxPin);

// Pines de los componentes
const int ledPin = 13;  // LED integrado en la mayoría de las placas
const int ldrPin = A0;  // Pin analógico para el sensor LDR

// Constantes para mapear el valor del LDR a un porcentaje
const int LECTURA_OSCURIDAD_MAXIMA = 150;
const int LECTURA_BRILLO_MINIMO    = 800;

void setup() {
    // Iniciar comunicación serial (para depuración en PC y Bluetooth)
    Serial.begin(9600);
    btSerial.begin(9600);

    // Configurar pines
    pinMode(ledPin, OUTPUT);
    pinMode(ldrPin, INPUT);

    // Mensaje de inicio
    btSerial.println("Sistema LDR iniciado. Listo para recibir y enviar.");
    Serial.println("Sistema LDR iniciado. Listo para recibir y enviar.");
}

void loop() {

    // --- 1. Envío periódico de la lectura del LDR ---
    int ldrStatus = analogRead(ldrPin);
    int porcentajeLuz = map(
        ldrStatus,
        LECTURA_OSCURIDAD_MAXIMA,
        LECTURA_BRILLO_MINIMO,
        0, 100
    );

    String mensaje = "";

    // Envía el estado del LED y del LDR
    if (digitalRead(ledPin) == HIGH) {
        mensaje = "Luz: " + String(ldrStatus) + " | " + String(porcentajeLuz) + "% -> LED ENCENDIDO";
    } else {
        mensaje = "Luz: " + String(ldrStatus) + " | " + String(porcentajeLuz) + "% -> LED APAGADO";
    }

    btSerial.println(mensaje);
    Serial.println(mensaje); // Para depurar en el Serial Monitor del IDE de Arduino

    // --- 2. Recepción de comandos desde la app ---
    if (btSerial.available()) {

        String comando = btSerial.readStringUntil('\n');
        comando.trim(); // Limpiar espacios en blanco

        Serial.print("Comando recibido: ");
        Serial.println(comando);

        btSerial.println("Recibido: " + comando);

        // ---- Acciones basadas en el comando ----
        if (comando == "LED_ON") {
            digitalWrite(ledPin, HIGH);
        }

        if (comando == "LED_OFF") {
            digitalWrite(ledPin, LOW);
        }

        if (comando == "LDR?") {
            int valor = analogRead(ldrPin);
            btSerial.println("Valor LDR: " + String(valor));
        }
    }

    delay(1000); // Esperar un segundo antes de repetir el ciclo
}
```

## Instalación y Uso

### Hardware
1.  **Montar el circuito:** Conectar los componentes (LED, LDR, módulo Bluetooth) a los pines correspondientes del Arduino como se especifica en el código.
2.  **Cargar el Código:** Abrir el IDE de Arduino, pegar el código `.ino` de arriba y cargarlo a la placa.

### Software
1.  **Configurar Firebase:**
    - Crear un nuevo proyecto en la [Consola de Firebase](https://console.firebase.google.com/).
    - Dentro del proyecto, habilitar **Authentication** (con el proveedor de Email/Contraseña) y **Cloud Firestore** (iniciándolo en modo de prueba o producción).
    - Registrar una nueva app de Android en el proyecto, usando `com.example.arduino_iot_hub` como nombre de paquete.
2.  **Clonar el Repositorio:** `git clone https://github.com/arico-dev/Arduino-IoT-Hub.git`
3.  **Añadir Configuración de Firebase:** Descargar el archivo `google-services.json` desde tu proyecto de Firebase y colocarlo en el directorio `app/` de este proyecto.
4.  **Abrir en Android Studio:** Abrir el proyecto clonado con la última versión de Android Studio.
5.  **Construir y Ejecutar:** Construir y ejecutar la aplicación en un dispositivo Android físico.
6.  **Crear una Cuenta:** Usar la pantalla de registro para crear un nuevo usuario y luego iniciar sesión.

## Autores

- Anthony Rico Encinas
- Felipe Zurita Hidalgo
- Matías Cerda González
