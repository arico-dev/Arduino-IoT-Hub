# Arduino IoT Hub

Aplicación Android nativa para controlar y monitorizar una placa Arduino a través de una conexión Bluetooth. Permite interactuar con componentes electrónicos como un LED y un sensor de luz (LDR) desde un panel de control intuitivo y moderno.

## Características Principales

- **Conexión Bluetooth:** Escaneo y conexión robusta con dispositivos Bluetooth, optimizada para módulos de puerto serie como el HC-05.
- **Panel de Control Centralizado:** Una interfaz de usuario clara para interactuar con todos los componentes del hardware.
- **Control de LED:** Encendido y apagado remoto de un LED mediante un interruptor (Switch) de Material Design.
- **Monitor de Sensor LDR:** Visualización en tiempo real del valor de un sensor de luz (LDR) con una barra de progreso animada.
- **Consola Serial:** Envío de comandos de texto personalizados y recepción de mensajes desde el Arduino para depuración y control avanzado.
- **Diseño Moderno:** Interfaz de usuario pulida siguiendo las guías de Material Design 3, incluyendo tarjetas (`CardView`), campos de texto `TextInputLayout` y botones estilizados.
- **Soporte para Tema Oscuro:** La interfaz se adapta automáticamente al tema claro u oscuro del dispositivo, manteniendo la legibilidad.
- **Flujo de Usuario Completo:** Incluye una pantalla de inicio (`Splash`), login de usuario, pantalla de descripción del proyecto y navegación coherente.

## Tecnologías Utilizadas

### Aplicación Android
- **Lenguaje:** Kotlin
- **IDE:** Android Studio
- **Librerías Principales:**
    - Componentes de Material Design 3
    - View Binding para la interacción con las vistas
    - AndroidX AppCompat y Activity

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

    if (ldrStatus <= 300) {
        digitalWrite(ledPin, LOW); // Apaga el LED si está oscuro
        mensaje = "Luz: " + String(ldrStatus) + " | " + String(porcentajeLuz) + "% -> Oscuro, LED APAGADO";
    } else {
        digitalWrite(ledPin, HIGH); // Enciende el LED si hay luz
        mensaje = "Luz: " + String(ldrStatus) + " | " + String(porcentajeLuz) + "% -> Claro, LED ENCENDIDO";
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
            btSerial.println("LED ENCENDIDO");
        }

        if (comando == "LED_OFF") {
            digitalWrite(ledPin, LOW);
            btSerial.println("LED APAGADO");
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
1.  **Montar el circuito:**
    - Conectar el pin del LED al pin digital `13` del Arduino.
    - Conectar el sensor LDR al pin analógico `A0`.
    - Conectar el módulo Bluetooth: pin `TX` del módulo al pin `2` del Arduino (RX) y pin `RX` del módulo al pin `3` del Arduino (TX).
2.  **Cargar el Código:** Abrir el IDE de Arduino, pegar el código `.ino` de arriba y cargarlo a la placa.

### Software
1.  Clonar o descargar este repositorio.
2.  Abrir el proyecto con la última versión de Android Studio.
3.  Construir y ejecutar la aplicación en un dispositivo Android físico.
4.  En la pantalla de login, usar las credenciales:
    - **Usuario:** `user`
    - **Contraseña:** `1234`
5.  Navegar a la pantalla de conexión, escanear y seleccionar el módulo Bluetooth de tu Arduino.

## Autores

- Anthony Rico Encinas
- Felipe Zurita Hidalgo
- Matías Cerda González
