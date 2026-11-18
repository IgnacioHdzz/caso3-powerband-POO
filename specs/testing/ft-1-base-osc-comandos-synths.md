---
id: ft-1
layer: testing
status: draft
---

# Pruebas de OSC y eventos de synths

## General description

- Verificar la codificación OSC y los eventos generados por los comandos de piano, bajo y órgano sin requerir que las pruebas envíen paquetes a SuperCollider.

## Inputs

- Mensajes OSC con argumentos enteros, floats y strings.
- Parámetros de prueba para `ComandoPiano`, `ComandoBajo` y `ComandoOrgano`.
- Un transporte de prueba que permita ejecutar comandos sin abrir una conexión UDP real.

## Outputs

- Pruebas JUnit 5 que aceptan o rechazan los bytes OSC producidos y los campos/notificaciones de cada `EventoMusical`.
- Ningún datagrama de prueba enviado a `scsynth`.

## Test criteria

- Comparar bytes OSC completos con valores esperados para enteros, floats y strings, incluidos terminador NUL, relleno a múltiplos de cuatro y orden de bytes de red.
- Comprobar la codificación de dirección OSC y etiquetas de tipo para `/s_new` y `/n_set`.
- Comprobar que `ComandoPiano` genera un evento `DISPARO` con synth, nota, duración y tercera correctos, tanto mayor como menor.
- Comprobar que `ComandoBajo` genera un evento `DISPARO` con synth, nota y duración correctos.
- Comprobar que `ComandoOrgano.noteOn` genera un evento `NOTE_ON` con nota, ratios y amplitudes; comprobar que `noteOff` genera `NOTE_OFF` para la nota y el nodo correspondientes.
- Comprobar que los observadores registrados reciben el evento correspondiente una vez por cada comando enviado localmente con éxito.
- Comprobar que un fallo simulado de envío no se reporta como envío exitoso.
- Ejecutar con JUnit 5 y Maven (`mvn test`); las pruebas no dependen de hardware ni de que SuperCollider esté activo.

## Assets

- Contratos de synths y parámetros de [`supercollider/myband.sc`](../../supercollider/myband.sc).
- `pom.xml` declara JUnit 5. No se encontró `BandClient.java`; no se usa como referencia de pruebas.
- Guías de prueba: `.github/instructions/testing.instructions.md` y `.github/instructions/testing-junit.instructions.md`.

## Implementation guidelines

- Escribir las pruebas Java en `src/test/java/powerband/`, reflejando los paquetes probados; nombrar las clases `ClienteSuperColliderTest`, `ComandoPianoTest`, `ComandoBajoTest` y `ComandoOrganoTest`.
- Usar JUnit 5 y nombres de métodos que describan el comportamiento verificado.
- Comparar buffers/bytes en memoria y usar un transporte sustituible en pruebas; no enviar datagramas ni requerir un dispositivo o servidor.
- Incluir las pruebas de parámetros no aplicables como `null` en el evento, conforme al contrato de backend.
- No cambiar código productivo ni corregir fallos ajenos al alcance de FT-1 como parte de esta spec.

## Business workflow

1. Construir un mensaje OSC de prueba y comparar el datagrama serializado byte por byte con el resultado esperado.
2. Ejecutar cada comando con un transporte de prueba y un observador capturador.
3. Verificar los campos del evento, su acción y la notificación del observador.
4. Simular fallo local de envío y verificar el manejo de la excepción sin notificación de éxito.
5. Ejecutar la suite de FT-1 con `mvn test`.
