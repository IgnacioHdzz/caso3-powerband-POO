---
id: ft-2
layer: backend
status: draft
---

# Instrumentos de control y micrófono

## General description

- Implementar el lado de abstracción del patrón Bridge en `powerband.instrumentos`: un instrumento recibe `ComandoSynth` y administra su propio inicio, ejecución y detención.
- Leer un control de PlayStation mediante Jamepad y capturar audio desde un micrófono para accionar el órgano o el bajo.
- Mantener aislados los recursos e hilos de ambos dispositivos; la falla o ausencia de uno no debe impedir el uso del otro.

## Inputs

- `Instrumento` recibe un `powerband.model.ComandoSynth` en su constructor.
- Control: estados de botones y palancas de un control Jamepad; nota asociada a cada botón y amplitudes parciales configuradas en `powerband.util.IConstants`.
- Micrófono: bloques de PCM, formato de captura, umbral RMS, tiempo mínimo entre golpes y escala de notas configurados en `IConstants`.
- `iniciar()` y `detener()` de cada instrumento.

## Outputs

- `Instrumento` abstracto, `InstrumentoControl`, `InstrumentoMicrofono` y `DispositivoNoDisponibleException` en `src/main/java/powerband/instrumentos/` y `src/main/java/powerband/excepciones/`, respectivamente.
- Llamadas al método público existente `ComandoSynth.ejecutar(EventoMusical.Accion, int, Integer, Integer, float[], float[])`:
  - Control: `NOTE_ON` y `NOTE_OFF`, con nota MIDI y tres ratios/tres amplitudes en `NOTE_ON`.
  - Micrófono: `DISPARO`, nota MIDI elegida y duración en milisegundos.
- `DispositivoNoDisponibleException` como subclase de `PowerBandException`, para fallas al inicializar o acceder al dispositivo.

## Test criteria

- Cada instrumento recibe por constructor un `ComandoSynth`, conserva sus recursos privados y ofrece `iniciar()`/`detener()` sin que los hilos de lectura bloqueen al llamador.
- El control traduce los diez botones configurados a notas distintas: X (cruz), círculo, cuadrado, triángulo, L1, R1 y las cuatro direcciones. Una transición suelto→presionado emite un solo `NOTE_ON`; presionado→suelto emite un solo `NOTE_OFF`; una pulsación sostenida no repite eventos.
- Al detener o perder el control, libera cada nota que el instrumento aún considera activa. Al detener se señala el fin del hilo, se espera su terminación y se liberan los recursos Jamepad.
- Al presionar un botón, las tres amplitudes enviadas con `NOTE_ON` se calculan a partir de los ejes asignados y quedan acotadas al intervalo configurado; la zona muerta evita fluctuaciones en reposo. No enviar `NOTE_ON` periódicamente para actualizar una nota sostenida.
- El micrófono lee bloques PCM, calcula RMS normalizado y dispara solo ante un flanco de subida por encima del umbral, tras haber rearmado por debajo de este. El enfriamiento evita disparos demasiado próximos.
- Para un bloque que cruza el umbral, el estimador cuenta cruces por cero y calcula frecuencia aproximada como `(cruces / 2.0) * frecuenciaMuestreo / cantidadMuestras`; selecciona la nota configurada más cercana. Sin cruces suficientes o frecuencia válida no dispara.
- Al detener el micrófono, se detiene/cierra la línea de captura, se termina su hilo y se liberan los recursos; el procesamiento de audio no requiere UI ni sincronización con el hilo del control.
- La ausencia o desconexión de un dispositivo produce `DispositivoNoDisponibleException`, se reporta explícitamente y no impide intentar iniciar o mantener activo el otro instrumento.
- Las pruebas unitarias de mapeo, procesamiento y ciclos de vida funcionan sin control ni micrófono físicos.

## Assets

- `pom.xml` ya declara `com.badlogicgames.jamepad:jamepad:2.0.14.0` y `com.badlogicgames.gdx:gdx-jnigen-loader:2.0.0`; no agregar dependencias para dispositivos ni audio.
- Jamepad 2.0.14.0 expone `ControllerManager.initSDLGamepad()`, `getState(int)`, `getNumControllers()` y `quitSDLGamepad()`. `ControllerState` ofrece `isConnected`, `a`, `b`, `x`, `y`, `lb`, `rb`, `dpadUp/Down/Left/Right` y valores de palancas `leftStickX`, `leftStickY`, `rightStickX`, `rightStickY`.
- Correspondencia física de botones según el orden estándar Jamepad/SDL: botón inferior A = cruz/X de PlayStation; B = círculo; X = cuadrado; Y = triángulo; `LEFTBUMPER`/`RIGHTBUMPER` = L1/R1; D-pad según dirección.
- [`ComandoSynth`](../../src/main/java/powerband/model/ComandoSynth.java) solo ofrece `ejecutar(Accion, nota, duracionMs, tercera, ratios, amplitudes)`. No expone un getter público para el comando ni funciones de fábrica para instrumentos.
- [`ComandoOrgano`](../../src/main/java/powerband/model/ComandoOrgano.java) acepta `NOTE_ON` con arrays de tres ratios/amplitudes y `NOTE_OFF` con la nota; [`ComandoBajo`](../../src/main/java/powerband/model/ComandoBajo.java) acepta `DISPARO` con nota y duración. Ambos implementan `ejecutar` con la firma heredada.
- [`ClienteSuperCollider`](../../src/main/java/powerband/service/ClienteSuperCollider.java) y [`IConstants`](../../src/main/java/powerband/util/IConstants.java) confirman el transporte/valores públicos disponibles; los instrumentos no deben invocarlos directamente.
- [`Main`](../../src/main/java/powerband/Main.java) ejecuta actualmente la demo FT-1 y trata `args[0]`/`args[1]` como host/puerto. La integración FT-2 requiere añadir una selección explícita sin eliminar la demo ni alterar las funcionalidades/contratos aprobados de FT-1.

## Implementation guidelines

- Usar Java 25, `javax.sound.sampled.TargetDataLine` y la dependencia Jamepad existente. Nombres de clases, métodos, campos, parámetros y mensajes en español; cada parámetro inicia con `p`; campos `private`; constantes en `IConstants.java`.
- Crear `src/main/java/powerband/instrumentos/Instrumento.java`, `InstrumentoControl.java`, `InstrumentoMicrofono.java` y `src/main/java/powerband/excepciones/DispositivoNoDisponibleException.java`. Actualizar `Main.java` e `IConstants.java` solo para la integración FT-2 descrita aquí.
- `Instrumento` es abstracto, guarda el `ComandoSynth` recibido por constructor y define `iniciar()` y `detener()`. Los lectores concretos llaman exclusivamente a `ComandoSynth.ejecutar(...)`; no conocen OSC, `ClienteSuperCollider` ni SuperCollider.
- `InstrumentoControl` inicializa Jamepad, lee el estado del primer control en su propio hilo y usa transiciones entre estados sucesivos para note-on/off. Mapear botón inferior A→nota de escala 0; B→1; X→2; Y→3; L1→4; R1→5; D-pad arriba→6, derecha→7, abajo→8, izquierda→9. Un botón retenido no debe volver a disparar note-on.
- Pasar las acciones con los argumentos de la API real: `ejecutar(NOTE_ON, pNota, null, null, pRatios, pAmplitudes)`, `ejecutar(NOTE_OFF, pNota, null, null, null, null)` y `ejecutar(DISPARO, pNota, pDuracionMs, null, null, null)`. No invocar `ComandoOrgano.noteOn/noteOff` ni `ComandoBajo.disparar` por medio de `ComandoSynth`, pues esos métodos no pertenecen a la clase base.
- Leer el eje izquierdo X para parcial 1, el izquierdo Y para parcial 2 y el derecho Y para parcial 3. Convertir cada eje Jamepad `[-1, 1]` a amplitud `[0, 1]`, sujetar al rango y aplicar zona muerta configurada. Tomar los valores al comenzar la nota; no emitir `NOTE_ON` por cada cambio del eje porque en FT-1 esa acción retrigger y reemplaza el nodo activo. Copiar los arrays de configuración antes de modificarlos; no compartir arrays mutables entre notas simultáneas.
- La propuesta de PCM para el micrófono es mono, PCM firmado de 16 bits little-endian a 44 100 Hz. Decodificar cada bloque según ese formato; RMS normalizado es `sqrt(sum((muestra / 32768.0)^2) / cantidadMuestras)`. Detectar el umbral en bloque y exigir que el RMS regrese bajo el umbral para rearmar. Enviar al comando la duración definida en constantes.
- El cálculo de cruces por cero usa el mismo bloque que disparó, cuenta cambios de signo excluyendo ceros consecutivos, estima frecuencia y convierte a MIDI mediante A4=440 Hz/MIDI 69 antes de elegir la nota configurada más cercana; en empate, elegir la menor. No disparar con un bloque insuficiente o sin frecuencia positiva.
- Leer/capturar en hilos independientes, usar una espera/polling de intervalo configurado para el control y bloques acotados para el micrófono. `detener()` solicita fin, desbloquea la lectura cerrando la línea si fuese necesario, une el hilo y libera recursos. No usar esperas activas sin pausa.
- Traducir ausencia de control, inicialización Jamepad fallida, falta de línea de captura o `LineUnavailableException` a `DispositivoNoDisponibleException`. En `Main`, manejar el arranque de cada instrumento por separado; reportar errores en español y dejar que el otro continúe. Los errores asíncronos no se silencian: se informa el motivo, se detiene y limpia únicamente el instrumento afectado.
- Para probar sin hardware, separar las transiciones/mapeos y cálculos de audio en lógica determinista pequeña y permitir fuentes de estado/bloques sustituibles. No crear un framework de dispositivos ni cambiar FT-1.
- Extender la línea de comandos de `Main` con modo explícito de dispositivos sin romper la demo por defecto ni la invocación posicional `host port` ya existente. Mantener el modo demo de FT-1 y cerrar los instrumentos y `ClienteSuperCollider` al interrumpir/salir.
- No tocar FT-3, FT-4, FT-5, FT-6 ni funcionalidades asignadas a Ignacio.
- **Pendiente de revisión antes de aprobar:** confirmar la escala/rango MIDI y el orden exacto del mapeo de los diez botones; la propuesta inicial es Do mayor MIDI 60, 62, 64, 65, 67, 69, 71, 72, 74, 76.
- **Pendiente de revisión antes de aprobar:** confirmar la escala del bajo; propuesta inicial Do mayor MIDI 36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60.
- **Pendiente de revisión antes de aprobar:** confirmar mapeo y zona muerta de palancas, RMS de disparo, enfriamiento, frecuencia/formato de PCM y duración del disparo. Propuestas iniciales: amplitud por eje indicado con zona muerta 0.10; umbral RMS 0.10; enfriamiento 150 ms; duración de bajo 450 ms.
- **Pendiente de revisión antes de aprobar:** confirmar la sintaxis de selector CLI; propuesta: `--demo [host] [port]` y `--instrumentos [host] [port]`, conservar la demo sin argumentos y la forma anterior `host port`.
- **Decisión bloqueante antes de aprobar:** la solicitud de amplitudes de palanca en tiempo real no se puede cumplir con las APIs FT-1 actuales. `ComandoSynth.ejecutar` solo permite `NOTE_ON`/`NOTE_OFF` para órgano; repetir `NOTE_ON` retrigger la nota y `ClienteSuperCollider` no expone una operación pública para actualizar controles de un nodo. Elegir entre aplicar amplitudes solo al iniciar cada nota (y aceptar que mover las palancas no altere notas sostenidas) o aprobar primero una ampliación acotada de FT-1 que actualice `a1`–`a3` mediante `/n_set`. No asumir ni implementar esa ampliación dentro de FT-2.

## Business workflow

1. `Main` selecciona la demo existente o el modo explícito de dispositivos y crea los comandos existentes de órgano y bajo con el mismo `ClienteSuperCollider`.
2. `Main` intenta iniciar control y micrófono de forma independiente; reporta el dispositivo no disponible sin abortar el arranque del otro.
3. El lector del control compara estados consecutivos, convierte transiciones de los diez botones a notas de escala y cambios de palanca a amplitudes; llama a `ComandoSynth.ejecutar` para cada transición.
4. El lector de audio obtiene bloques PCM, calcula RMS, aplica umbral/rearme/enfriamiento, estima frecuencia por cruces por cero, escoge la nota de bajo más cercana y llama a `ejecutar(DISPARO, ...)`.
5. Si un dispositivo falla durante la lectura, se informa el error, se detiene y libera solo ese dispositivo; el otro conserva su operación.
6. Al salir, `Main` detiene ambos instrumentos, libera las notas activas del control y cierra sus recursos, sin perder la ruta de demo de FT-1.
