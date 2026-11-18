---
id: ft-2
layer: testing
status: draft
---

# Pruebas de instrumentos de control y micrófono

## General description

- Verificar el mapeo de eventos físicos a comandos de FT-1, el procesamiento determinista de audio y los ciclos de vida sin hardware.
- Verificar la integración de modos de `Main` sin alterar las pruebas ya aprobadas de FT-1.

## Inputs

- Estados simulados sucesivos de botones y palancas Jamepad.
- Bloques de muestras PCM mono firmadas de 16 bits little-endian a 44 100 Hz y reloj monotónico simulado.
- Proveedor de dispositivos que puede devolver un control/línea falsos o simular que no existe/no se abre.
- Dobles de `ComandoSynth` y de los servicios usados por `Main`.

## Outputs

- Pruebas JUnit 5 deterministas que verifican acciones, notas, parámetros, errores y cierre.
- Ninguna apertura de hardware real ni envío de red a SuperCollider.

## Test criteria

- Probar las diez asignaciones botón→nota: A/cruz→60, B/círculo→62, X/cuadrado→64, Y/triángulo→65, `LEFTBUMPER`/L1→67, `RIGHTBUMPER`/R1→69, y D-pad arriba→71, derecha→72, abajo→74, izquierda→76; comprobar que son configurables en `IConstants`.
- Comprobar que la escala configurable inicial del bajo contiene exactamente las notas MIDI `{36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60}`.
- Para cada botón, estado neutral→presionado genera un solo `ejecutar(NOTE_ON, nota, null, null, ratios, amplitudes)`; estado presionado repetido no duplica; presionado→neutral genera un solo `ejecutar(NOTE_OFF, nota, null, null, null, null)`.
- Comprobar presiones simultáneas independientes y que detener/desconectar libera exactamente las notas activas una vez, con errores de liberación reportados.
- Verificar cada transformación de ejes (izquierdo X/Y y derecho Y), zona muerta 0.10, amplitudes base 1.0/0.5/0.25, paso mínimo 0.01, polling de 20 ms, límites `[0, 1]` y estabilidad en neutro usando estados falsos; comprobar que `NOTE_ON` recibe las amplitudes muestreadas al presionar sin generar retriggers periódicos.
- Con una nota sostenida, simular movimiento fuera de zona muerta y comprobar que se envía la nueva acción `ACTUALIZAR_PARAMETROS` al comando, sin `NOTE_OFF`/`NOTE_ON` adicionales. Confirmar que `/n_set` actualiza `a1`–`a3` en el mismo nodo y que no se genera evento si la nota no está activa.
- Para el micrófono, comprobar la decodificación PCM mono signed 16-bit little-endian a 44 100 Hz y comparar el RMS normalizado con buffers sintéticos conocidos; cubrir silencio, umbral inicial 0.10, bloque sobre umbral y bloque bajo umbral que rearma la detección.
- Comprobar que solo un flanco sobre umbral dispara, que no hay nuevo disparo antes del enfriamiento inicial de 150 ms y que un nuevo flanco después del enfriamiento dispara; verificar bloque inicial de 4096 muestras y duración de disparo de 450 ms.
- Verificar frecuencia de cruces por cero de señales sintéticas conocidas con `frecuenciaHz = (cruces / 2.0) * 44100 / cantidadMuestras`, rechazo de silencio/bloques vacíos/cortos/sin cruces, frecuencia no finita o fuera del rango de la escala, y selección de nota más cercana; en empate se selecciona la menor.
- Comprobar que cada disparo llama `ComandoSynth.ejecutar(DISPARO, notaElegida, duracionMs, null, null, null)` una vez.
- Simular control ausente o Jamepad no inicializable y `TargetDataLine` ausente/no disponible; cada caso resulta en `DispositivoNoDisponibleException`, se libera el recurso parcial y el otro instrumento aún puede iniciarse.
- Comprobar que `detener()` termina los hilos y cierra cada fuente de dispositivo una vez; el micrófono no queda bloqueado en lectura y el control no conserva notas activas.
- Probar el selector de `Main`: sin argumentos, con `host` o con `host port` se conserva demo; `--demo [host] [port]` elige demo; `--instrumentos [host] [port]` intenta iniciar ambos individualmente; al fallar uno, intenta el otro y detiene los iniciados al salir.
- Ejecutar `mvn test`; ejecutar repetidamente las pruebas de audio/control no debe requerir esperas reales, audio del sistema ni variaciones de hardware.

## Assets

- JUnit 5 5.11.4 y Surefire 3.5.2 están declarados en `pom.xml`.
- Jamepad 2.0.14.0 provee `ControllerState` y `ControllerButton`; `gdx-jnigen-loader:2.0.0` está declarado estable para resolver la dependencia del runtime.
- Las APIs a verificar son `ComandoSynth.ejecutar(...)`, `ComandoOrgano.ejecutar(...)`, `ComandoBajo.ejecutar(...)`, la acción existente `EventoMusical.Accion.ACTUALIZAR_PARAMETROS`, `ClienteSuperCollider.actualizarAmplitudesOrgano(...)`, `Instrumento.iniciar()`/`detener()` (nuevo en FT-2) y `Main`.
- `javax.sound.sampled` es de la biblioteca estándar; no se agregan librerías de audio ni de mocking.

## Implementation guidelines

- Escribir pruebas en `src/test/java/powerband/instrumentos/`, con clases `InstrumentoControlTest`, `InstrumentoMicrofonoTest` y `InstrumentoTest`; reflejar paquetes y usar JUnit 5.
- Probar algoritmos de botón/RMS/cruces y mapeo de escala mediante lógica determinista y fuentes falsas. No abrir `ControllerManager` real ni obtener `TargetDataLine` del sistema en pruebas unitarias.
- Implementar dobles mínimos del comando heredando `ComandoSynth` y registrando los argumentos exactos de `ejecutar`; no asumir métodos no públicos de FT-1.
- Para el procesamiento PCM, usar una fuente de audio sustituible que entregue bloques idénticos a los de `TargetDataLine`; simular ausencia y fallo de apertura con excepciones controladas.
- Sin `Thread.sleep` para medir algoritmos o enfriamiento; inyectar el reloj y avanzar el tiempo falso. Para pruebas de detención, usar señales de fin/unión con timeout finito y revisar resultado.
- Mantener constantes de pruebas/configuración de dispositivos en `IConstants`; evitar cambios en specs o pruebas de FT-1 y excluir FT-3 a FT-6 y tareas de Ignacio.
- La actualización en vivo se verifica contra el contrato FT-1 ya implementado: `ACTUALIZAR_PARAMETROS` debe producir `/n_set` sobre el nodo activo con `a1`–`a3`, sin crear otro nodo ni repetir `NOTE_ON`. La prueba de FT-2 no modifica ni reinterpreta `NOTE_ON`/`NOTE_OFF`.

## Business workflow

1. Alimentar el lector lógico de botones con estados falsos y comparar llamadas capturadas a `ComandoSynth.ejecutar`.
2. Alimentar el procesador de audio con buffers PCM sintéticos y reloj falso; verificar RMS, umbral, enfriamiento, cruces por cero y nota elegida.
3. Simular dispositivos ausentes y verificar excepción, limpieza y continuidad del otro lector.
4. Comprobar selector e integración de cierre con dobles, sin inicializar Jamepad, capturar audio ni conectar SuperCollider.
5. Ejecutar la suite de FT-2 con `mvn test`.
