---
applyTo: "src/test/java/**/*.java"
description: "Mapa de pruebas JUnit y dobles de FT-1/FT-2"
---
# Testing

## Proyecto y alcance

Las pruebas están en `src/test/java/powerband/`, reflejando los paquetes de producción. Se usa JUnit Jupiter 5.11.4 y Maven Surefire 3.5.2, declarados en `pom.xml`. El comando de suite es `mvn test`.

- [`ClienteSuperColliderTest`](../../src/test/java/powerband/service/ClienteSuperColliderTest.java): bytes OSC, padding, envío y eventos, IDs/nodos, errores, cierre y concurrencia.
- [`ComandoPianoTest`](../../src/test/java/powerband/model/ComandoPianoTest.java), [`ComandoBajoTest`](../../src/test/java/powerband/model/ComandoBajoTest.java) y [`ComandoOrganoTest`](../../src/test/java/powerband/model/ComandoOrganoTest.java): validación y eventos para cada synth, incluidos retrigger y `ACTUALIZAR_PARAMETROS`.
- [`InstrumentoControlTest`](../../src/test/java/powerband/instrumentos/InstrumentoControlTest.java): mapeo de botones, transiciones, pulsaciones simultáneas, palancas, liberación y desconexión.
- [`InstrumentoMicrofonoTest`](../../src/test/java/powerband/instrumentos/InstrumentoMicrofonoTest.java): PCM little-endian, RMS, umbral, flancos, enfriamiento, frecuencia y cuantización.
- [`InstrumentoTest`](../../src/test/java/powerband/instrumentos/InstrumentoTest.java): Bridge, recursos y ausencia de dispositivos.
- [`MainTest`](../../src/test/java/powerband/MainTest.java): selector, argumentos posicionales y arranque independiente de dispositivos.
- [`LectorOsc`](../../src/test/java/powerband/helpers/LectorOsc.java): decodifica dirección y argumentos OSC capturados; métodos públicos `static String leerDireccion(byte[] pMensaje)` y `static List<Object> leerArgumentos(byte[] pMensaje)`.
- [`TransporteOscFalso`](../../src/test/java/powerband/helpers/TransporteOscFalso.java): transporte sustituible; constructor público `TransporteOscFalso()`, métodos `void enviar(byte[] pMensaje) throws IOException`, `void fallarUnaVez(IOException pFallo)` y `List<byte[]> obtenerMensajes()`.
- [`ComandoSynthFalso`](../../src/test/java/powerband/instrumentos/ComandoSynthFalso.java): doble de `ComandoSynth`; implementa `void ejecutar(EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, float[] pRatios, float[] pAmplitudes)` y guarda una instantánea de cada llamada.
- Los dobles internos de `InstrumentoControlTest` simulan estado, bloqueo y desconexión del control; los de `InstrumentoTest` simulan fuente ausente y lectura/cierre de audio; `MainTest.InstrumentoFalso` simula disponibilidad individual. `ComandoSynthFalso.Llamada` es el record de argumentos capturados.

La última ejecución reportada de `mvn test` fue **37 pruebas aprobadas, 0 fallidas**. Esta suite usa transportes y fuentes falsas: no verifica físicamente `scsynth`, el control ni el micrófono, ni la integración con funcionalidades de Ignacio.

## Reglas para pruebas nuevas

- Mantener JUnit 5, clases `<Clase>Test` y el paquete de la clase probada.
- No crear sockets/hardware reales. Usar `TransporteOscFalso`, `LectorOsc`, `ComandoSynthFalso`, fuentes sustituibles y reloj monotónico inyectado donde corresponda.
- Para concurrencia/cierre, coordinar con latches y timeout acotado; evitar `Thread.sleep` para medir algoritmos o tiempo.
- Revisar la spec aprobada del feature y no debilitar ni omitir pruebas que fallen. Ejecutar `mvn test` para la validación completa.
