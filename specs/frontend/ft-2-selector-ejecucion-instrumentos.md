---
id: ft-2
layer: frontend
status: draft
---

# Selector de demo e instrumentos

## General description

- Extender la entrada de consola existente para elegir explícitamente la demo de FT-1 o la ejecución de los instrumentos de control y micrófono de FT-2.
- Mantener la demo accesible y preservar compatibilidad con la invocación actual de `Main` mediante host y puerto posicionales.

## Inputs

- Argumentos de consola para elegir `demo` o `instrumentos`.
- Host y puerto OSC opcionales, manteniendo el destino por defecto de `IConstants`.
- Disponibilidad de control PlayStation, micrófono y SuperCollider.

## Outputs

- En modo demo, se ejecuta la secuencia actual de piano, bajo y órgano.
- En modo instrumentos, se inicia de forma independiente cada dispositivo disponible; los mensajes de dispositivos no disponibles se informan en español y el otro instrumento puede continuar.
- Al solicitar el cierre/interrumpir, ambos instrumentos y sus recursos se detienen ordenadamente.

## Test criteria

- Sin selector y con los argumentos actuales, `Main` conserva el comportamiento demo FT-1.
- Un selector explícito inicia el modo pedido y no inicia a la vez el modo alternativo.
- Un error de inicialización de un dispositivo se reporta y no impide intentar el segundo.
- La salida del modo instrumentos invoca la detención de los instrumentos iniciados y cierra recursos aun si uno falla.
- Las pruebas del selector no acceden a hardware ni envían datagramas reales.

## Assets

- [`Main.java`](../../src/main/java/powerband/Main.java) actualmente ejecuta la demo al invocarse y acepta `host`/`port` en `args[0]`/`args[1]`.
- [`IConstants.java`](../../src/main/java/powerband/util/IConstants.java) contiene el destino y parámetros de demo.
- La clase abstracta `Instrumento` de FT-2 expone `iniciar()` y `detener()`; las implementaciones concretas son `InstrumentoControl` e `InstrumentoMicrofono`.
- Las etiquetas candidatas de CLI son `--demo [host] [port]` y `--instrumentos [host] [port]`; su adopción debe confirmarse antes de aprobar.

## Implementation guidelines

- Cambiar únicamente `src/main/java/powerband/Main.java` y las constantes de modo requeridas en `src/main/java/powerband/util/IConstants.java`, además de los tipos backend que define la spec FT-2.
- Mantener la demo existente como modo predeterminado. Preservar la forma histórica `Main host port` como demo, si se elige el selector explícito descrito en la spec backend.
- En modo instrumentos, iniciar control y micrófono de forma independiente; capturar `DispositivoNoDisponibleException` por dispositivo, notificar sin terminar el otro y detener todo en la ruta de salida.
- No crear UI Swing ni modificar la secuencia de demo o contratos aprobados de FT-1 salvo la integración explícita de selección/inicio/cierre.
- Mantener mensajes, nombres y parámetros según las convenciones del proyecto. No implementar ni invocar funcionalidades FT-3 a FT-6 ni de Ignacio.

## Business workflow

1. Interpretar el selector de modo y los parámetros OSC; si no hay selector, elegir demo conservando la forma anterior de host/puerto.
2. Para demo, ejecutar la secuencia FT-1 sin cambios.
3. Para modo instrumentos, construir cliente y comandos, intentar iniciar cada instrumento independientemente y reportar disponibilidad.
4. Mantener el proceso activo mientras exista al menos un instrumento iniciado.
5. En interrupción o salida, detener los instrumentos iniciados y cerrar el cliente OSC.
