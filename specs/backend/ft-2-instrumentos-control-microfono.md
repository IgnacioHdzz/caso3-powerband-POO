---
id: ft-2
layer: backend
status: approved
---

# Instrumentos de control y micrófono

## General description

- Implementar el lado de abstracción del patrón Bridge en `powerband.instrumentos`: un instrumento recibe `ComandoSynth` y administra su propio inicio, ejecución y detención.
- Leer un control de PlayStation mediante Jamepad y capturar audio desde un micrófono para accionar el órgano o el bajo.
- Mantener aislados los recursos e hilos de ambos dispositivos; la falla o ausencia de uno no debe impedir el uso del otro.

## Inputs

- `Instrumento` recibe un `powerband.model.ComandoSynth` en su constructor.
- Control: estados de botones y palancas de un control Jamepad; escala inicial Do mayor MIDI, configuración de ejes, zona muerta y rangos de amplitud definidos en `powerband.util.IConstants`.
- Micrófono: bloques PCM mono firmado de 16 bits little-endian a 44 100 Hz, umbral RMS normalizado, enfriamiento, duración del bajo y escala Do mayor MIDI configurados en `IConstants`.
- `iniciar()` y `detener()` de cada instrumento.

## Outputs

- `Instrumento` abstracto, `InstrumentoControl`, `InstrumentoMicrofono` y `DispositivoNoDisponibleException` en `src/main/java/powerband/instrumentos/` y `src/main/java/powerband/excepciones/`, respectivamente.
- Llamadas al método público existente `ComandoSynth.ejecutar(EventoMusical.Accion, int, Integer, Integer, float[], float[])`:
  - Control: `NOTE_ON` y `NOTE_OFF`, con nota MIDI y tres ratios/tres amplitudes en `NOTE_ON`.
  - Control sostenido: acción `ACTUALIZAR_PARAMETROS` de FT-1, con nota y tres amplitudes.
  - Micrófono: `DISPARO`, nota MIDI elegida y duración en milisegundos.
- `DispositivoNoDisponibleException` como subclase de `PowerBandException`, para fallos al inicializar o acceder al dispositivo.

## Test criteria

- Cada instrumento recibe por constructor un `ComandoSynth`, conserva sus recursos privados y ofrece `iniciar()`/`detener()` sin que los hilos de lectura bloqueen al llamador.
- El control traduce cada transición de botón a una nota distinta de la escala inicial Do mayor. Asignación exacta:
  - X/cruz → MIDI 60 (Do4); círculo → 62 (Re4); cuadrado → 64 (Mi4); triángulo → 65 (Fa4).
  - L1 → 67 (Sol4); R1 → 69 (La4); flecha arriba → 71 (Si4); derecha → 72 (Do5); abajo → 74 (Re5); izquierda → 76 (Mi5).
  - Correspondencia Jamepad: A→X/cruz, B→círculo, X→cuadrado, Y→triángulo; `LEFTBUMPER`→L1, `RIGHTBUMPER`→R1 y `DPAD_UP/RIGHT/DOWN/LEFT` a sus respectivas flechas.
  - Las notas se guardan en constantes configurables. Una transición suelto→presionado emite un solo `NOTE_ON`; presionado→suelto emite un solo `NOTE_OFF`; una pulsación sostenida no repite `NOTE_ON`.
- Al detener o perder el control, libera cada nota que el instrumento aún considera activa. Al detener se señala el fin del hilo, se espera su terminación y se liberan los recursos Jamepad.
- Las palancas modifican en tiempo real los tres parciales de cada nota sostenida mediante `/n_set`, sin recrear el nodo ni repetir `NOTE_ON`. La transformación respeta los rangos seguros y la zona muerta configurados.
- La escala del control es configurable en `IConstants`, inicialmente Do mayor MIDI `{60, 62, 64, 65, 67, 69, 71, 72, 74, 76}`; la escala del bajo es configurable, inicialmente Do mayor MIDI `{36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60}`.
- El micrófono lee PCM mono firmado de 16 bits little-endian a 44 100 Hz, calcula `RMS = sqrt(sum((muestra / 32768.0)^2) / cantidadMuestras)` y dispara ante un flanco de subida del umbral configurable después de haberse rearmado por debajo. Los valores iniciales configurables son umbral RMS `0.10`, enfriamiento `150 ms`, bloque de `4096` muestras y duración del bajo `450 ms`.
- Para un bloque que cruza el umbral, contar cruces de signo no nulos, ignorando ceros consecutivos, y calcular `frecuenciaHz = (cruces / 2.0) * 44100 / cantidadMuestras`. Convertir con `MIDI = 69 + 12 * log2(frecuenciaHz / 440)` y seleccionar la nota MIDI de la escala Do mayor más cercana; en empate, elegir la menor.
- Silencio/bloque vacío, RMS bajo umbral, menos de dos cruces, frecuencia no positiva/no finita, frecuencia fuera del rango cubierto por la escala, o un disparo dentro del enfriamiento no producen comando. El umbral descarta ruido de bajo nivel; no agregar un clasificador de ruido fuera del alcance.
- Al detener el micrófono, se detiene/cierra la línea de captura, se termina su hilo y se liberan los recursos; el procesamiento de audio no requiere UI ni sincronización con el hilo del control.
- La ausencia o desconexión de un dispositivo produce `DispositivoNoDisponibleException`, se reporta explícitamente y no impide intentar iniciar o mantener activo el otro instrumento.
- Las pruebas unitarias de mapeo, procesamiento y ciclos de vida funcionan sin control ni micrófono físicos.

## Assets

- `pom.xml` ya declara `com.badlogicgames.jamepad:jamepad:2.0.14.0` y `com.badlogicgames.gdx:gdx-jnigen-loader:2.0.0`; no agregar dependencias para dispositivos ni audio.
- Jamepad 2.0.14.0 expone `ControllerManager.initSDLGamepad()`, `getState(int)`, `getNumControllers()` y `quitSDLGamepad()`. `ControllerState` ofrece `isConnected`, `a`, `b`, `x`, `y`, `lb`, `rb`, `dpadUp/Down/Left/Right` y valores de palancas `leftStickX`, `leftStickY`, `rightStickX`, `rightStickY`.
- Correspondencia física de botones según el orden estándar Jamepad/SDL: botón inferior A = cruz/X de PlayStation; B = círculo; X = cuadrado; Y = triángulo; `LEFTBUMPER`/`RIGHTBUMPER` = L1/R1; D-pad según dirección.
- [`ComandoSynth`](../../src/main/java/powerband/model/ComandoSynth.java) ofrece `ejecutar(Accion, nota, duracionMs, tercera, ratios, amplitudes)`; FT-2 invoca esa firma heredada para las acciones del órgano.
- [`ComandoOrgano`](../../src/main/java/powerband/model/ComandoOrgano.java) actualmente acepta `NOTE_ON` con arrays de tres ratios/amplitudes y `NOTE_OFF`; [`ComandoBajo`](../../src/main/java/powerband/model/ComandoBajo.java) acepta `DISPARO` con nota y duración. Ambos implementan la firma heredada de `ejecutar`.
- [`EventoMusical.Accion`](../../src/main/java/powerband/dto/EventoMusical.java), [`ComandoOrgano`](../../src/main/java/powerband/model/ComandoOrgano.java) y [`ClienteSuperCollider`](../../src/main/java/powerband/service/ClienteSuperCollider.java) ya implementan `ACTUALIZAR_PARAMETROS`: `ComandoOrgano.ejecutar` la delega a `ClienteSuperCollider.actualizarAmplitudesOrgano(int pNota, float[] pAmplitudes)`. El método sincronizado valida exactamente tres floats finitos en `[0,1]`, localiza el ID de la nota activa y envía `/n_set` con el node ID y pares `a1`, amplitud 1, `a2`, amplitud 2, `a3`, amplitud 3. Notifica el evento únicamente tras un envío local exitoso; para una nota inactiva no envía ni genera evento. FT-2 consume este contrato existente sin modificar FT-1.
- [`IConstants`](../../src/main/java/powerband/util/IConstants.java) contiene hoy valores FT-1; agregar las escalas/configuraciones FT-2 como constantes. Los instrumentos no acceden directamente a `ClienteSuperCollider`.
- [`Main`](../../src/main/java/powerband/Main.java) ejecuta actualmente la demo FT-1 y trata `args[0]`/`args[1]` como host/puerto. La integración FT-2 requiere añadir una selección explícita sin eliminar la demo ni alterar las funcionalidades/contratos aprobados de FT-1.

## Implementation guidelines

- Usar Java 25, `javax.sound.sampled.TargetDataLine` y la dependencia Jamepad existente. Nombres de clases, métodos, campos, parámetros y mensajes en español; cada parámetro inicia con `p`; campos `private`; constantes en `IConstants.java`.
- Crear `src/main/java/powerband/instrumentos/Instrumento.java`, `InstrumentoControl.java`, `InstrumentoMicrofono.java` y `src/main/java/powerband/excepciones/DispositivoNoDisponibleException.java`. Actualizar `Main.java` e `IConstants.java` solo para la integración FT-2 descrita aquí.
- `Instrumento` es abstracto, guarda el `ComandoSynth` recibido por constructor y define `iniciar()` y `detener()`. Los lectores concretos llaman exclusivamente a `ComandoSynth.ejecutar(...)`; no conocen OSC, `ClienteSuperCollider` ni SuperCollider.
- `InstrumentoControl` inicializa Jamepad, lee `getState(0)` en su propio hilo y detecta ausencia/desconexión con `isConnected`. Usa las transiciones de estado para note-on/off y la tabla exacta de botones/notas indicada en `Test criteria`.
- Pasar las acciones con los argumentos de la API real: `ejecutar(NOTE_ON, pNota, null, null, pRatios, pAmplitudes)`, `ejecutar(NOTE_OFF, pNota, null, null, null, null)` y `ejecutar(DISPARO, pNota, pDuracionMs, null, null, null)`. No invocar `ComandoOrgano.noteOn/noteOff` ni `ComandoBajo.disparar` por medio de `ComandoSynth`, pues esos métodos no pertenecen a la clase base.
- Leer el eje izquierdo X para parcial 1, izquierdo Y para parcial 2 y derecho Y para parcial 3. Para cada eje `x∈[-1,1]`, zona muerta `d=0.10` y amplitud base `b` (`1.0`, `0.5`, `0.25` respectivamente): si `|x|≤d`, usar `b`; si `x<−d`, usar `b*(1−((-x−d)/(1−d)))`; si `x>d`, usar `b+(1−b)*((x−d)/(1−d))`. Sujetar a `[0,1]`. Muestrear cada 20 ms y mandar `ACTUALIZAR_PARAMETROS` solo si alguna amplitud cambió al menos `0.01`.
- Para actualizar amplitudes, llamar `ComandoSynth.ejecutar(ACTUALIZAR_PARAMETROS, pNota, null, null, null, pAmplitudes)` a través de `InstrumentoControl`; la acción existente de `ComandoOrgano` delega la validación y el envío `/n_set` al cliente. No llamar al cliente desde el instrumento. `ComandoBajo` continúa aceptando únicamente `DISPARO`.
- Capturar PCM con `TargetDataLine` en formato mono, signed 16-bit, little-endian, frecuencias de muestreo y de frame de 44 100 Hz, frame de 2 bytes; procesar bloques de 4096 muestras configurables, manteniendo alineación de frames. Decodificar muestras signed en `[-32768,32767]`; RMS normalizado es `sqrt(sum((muestra / 32768.0)^2) / cantidadMuestras)`. Umbral inicial configurable `0.10`; rearmar solo cuando RMS sea menor que el umbral. El tiempo mínimo inicial entre disparos es 150 ms y la duración del bajo 450 ms; usar reloj monotónico inyectable en la lógica de detección.
- Para un bloque con flanco sobre umbral y enfriamiento cumplido, contar cruces de signo entre muestras no nulas, ignorando ceros consecutivos; estimar `frecuenciaHz = (cruces / 2.0) * 44100 / cantidadMuestras`. Rechazar silencio, menos de dos cruces, frecuencia no positiva/no finita o fuera del rango entre el mínimo y máximo MIDI de la escala. Convertir frecuencia a MIDI con `69 + 12*log2(frecuenciaHz/440)` y escoger la nota configurada con menor distancia de semitonos; empate resuelto por nota inferior. Ruido bajo umbral se ignora; no añadir filtros/clasificadores adicionales.
- Leer/capturar en hilos independientes, usar una espera/polling de intervalo configurado para el control y bloques acotados para el micrófono. `detener()` solicita fin, desbloquea la lectura cerrando la línea si fuese necesario, une el hilo y libera recursos. No usar esperas activas sin pausa.
- Traducir ausencia de control, inicialización Jamepad fallida, falta de línea de captura o `LineUnavailableException` a `DispositivoNoDisponibleException`. En `Main`, manejar el arranque de cada instrumento por separado; reportar errores en español y dejar que el otro continúe. Los errores asíncronos no se silencian: se informa el motivo, se detiene y limpia únicamente el instrumento afectado.
- Para probar sin hardware, separar las transiciones/mapeos y cálculos de audio en lógica determinista pequeña y permitir fuentes de estado/bloques sustituibles. No crear un framework de dispositivos ni cambiar FT-1.
- Extender la línea de comandos de `Main` con `--demo [host] [port]` y `--instrumentos [host] [port]`. Sin argumentos y con la forma posicional existente `host` o `host port`, conservar la demo FT-1. Mantener los modos mutuamente excluyentes y cerrar instrumentos y `ClienteSuperCollider` al interrumpir/salir.
- Mantener la demo y los contratos actuales de FT-1, incluida la actualización de amplitudes del órgano mediante `ACTUALIZAR_PARAMETROS`; no volver a implementar ni alterar esa funcionalidad en FT-2. No tocar FT-3, FT-4, FT-5, FT-6 ni funcionalidades asignadas a Ignacio.

## Business workflow

1. `Main` selecciona la demo existente o el modo explícito de dispositivos y crea los comandos existentes de órgano y bajo con el mismo `ClienteSuperCollider`.
2. `Main` intenta iniciar control y micrófono de forma independiente; reporta el dispositivo no disponible sin abortar el arranque del otro.
3. El lector del control compara estados consecutivos; botones generan `NOTE_ON`/`NOTE_OFF`. Mientras una nota está sostenida, cambios de amplitud fuera de la zona muerta generan `ACTUALIZAR_PARAMETROS`, que se traduce a `/n_set` sin retrigger.
4. El lector de audio obtiene bloques PCM, calcula RMS, aplica umbral/rearme/enfriamiento, estima frecuencia por cruces por cero, descarta estimaciones inválidas, escoge la nota de bajo más cercana y llama a `ejecutar(DISPARO, ...)`.
5. Si un dispositivo falla durante la lectura, se informa el error, se detiene y libera solo ese dispositivo; el otro conserva su operación.
6. Al salir, `Main` detiene ambos instrumentos, libera las notas activas del control y cierra sus recursos, sin perder la ruta de demo de FT-1.
