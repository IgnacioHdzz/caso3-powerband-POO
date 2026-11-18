---
id: ft-1
layer: backend
status: approved
---

# Base Maven, cliente OSC y comandos de synths

## General description

- Crear la entrada y constantes de la aplicación Maven y la jerarquía de comandos que implementa el lado synth del patrón Bridge.
- Enviar a `scsynth` los mismos mensajes OSC que `BandClient.java` del profesor para `classicPiano`, `stringBass` y `organ70s`.
- Publicar un `EventoMusical` por cada datagrama OSC enviado localmente con éxito.

## Inputs

- `ComandoPiano`: nota fundamental MIDI, duración en milisegundos y tercera mayor (4 semitonos) o menor (3).
- `ComandoBajo`: nota MIDI y duración en milisegundos.
- `ComandoOrgano.noteOn`: nota MIDI, tres ratios y tres amplitudes.
- `ComandoOrgano.noteOff`: nota MIDI cuya instancia activa debe localizarse.
- Observadores registrados mediante `IObservadorComandos`.
- Destino OSC por defecto definido en `util/IConstants.java`; se permiten los argumentos opcionales `host` y `port` usados por el cliente del profesor.

## Outputs

- Datagramas OSC UDP al destino configurado (por defecto `127.0.0.1:57110`).
- Para cada datagrama enviado con éxito, un `dto/EventoMusical` con timestamp monotónico en milisegundos, synth, acción (`NOTE_ON`, `NOTE_OFF` o `DISPARO`), nota y parámetros correspondientes. Los parámetros no aplicables son `null`.
- `ConexionSynthException` para fallos locales de inicialización, resolución o envío UDP, como subclase de `PowerBandException`. UDP no confirma la recepción ni ejecución por `scsynth`.

## Test criteria

- `Main` ejecuta la secuencia de prueba del cliente del profesor para piano, bajo y órgano.
- `ComandoPiano` envía `/s_new` a `classicPiano` con `note`, `durMs` y `third`; `ComandoBajo` lo envía a `stringBass` con `note` y `durMs`.
- `ComandoOrgano.noteOn` envía `/s_new` a `organ70s` con `freq`, `r1`–`r3` y `a1`–`a3`; `noteOff` envía `/n_set` con el node ID activo, `gate` y `0`.
- Cada `/s_new`, `/n_set` de retrigger, note-off explícito o note-off de cierre que se envíe con éxito genera un evento y notifica a los observadores. Un note-off sin nodo activo no envía datagrama ni genera evento.
- El timestamp se toma de un reloj monotónico al despachar cada datagrama. El consumidor de grabación futuro calcula el tiempo relativo restando el instante monotónico de inicio de la grabación.
- Un fallo local se propaga y no genera evento de envío exitoso. No se afirma que un envío UDP exitoso compruebe que SuperCollider está activo.

## Assets

- [`supercollider/myband.sc`](../../supercollider/myband.sc) define `classicPiano` (`note`, `durMs`, `amp`, `third`), `stringBass` (`note`, `durMs`, `amp`) y `organ70s` (`freq`, `amp`, `gate`, `r1`–`r3`, `a1`–`a3`). Piano y bajo reciben nota MIDI; el órgano recibe frecuencia y libera su envolvente con `gate = 0`.
- `BandClient.java` del profesor es la referencia de codificación OSC, argumentos, IDs y secuencia de prueba. Debe conservarse disponible durante la implementación de FT-1.
- `pom.xml` declara Maven, Java 25 y JUnit 5. FT-1 no requiere nuevas dependencias.
- Precondición de ejecución: `scsynth` está iniciado y los SynthDefs de `myband.sc` cargados antes de ejecutar `Main`.

## Implementation guidelines

- Crear `src/main/java/powerband/Main.java`, `util/IConstants.java`, `service/ClienteSuperCollider.java`, `service/IObservadorComandos.java`, `model/ComandoSynth.java`, `model/ComandoPiano.java`, `model/ComandoBajo.java`, `model/ComandoOrgano.java`, `dto/EventoMusical.java`, `excepciones/PowerBandException.java` y `excepciones/ConexionSynthException.java`.
- Mantener `ComandoSynth` abstracto como implementación Bridge, independiente de dispositivos. Cada `Instrumento` futuro recibe un `ComandoSynth` y no conoce OSC ni SuperCollider. Mantener estable este contrato y el de `EventoMusical`/observadores para sus consumidores futuros; no implementar lectores o mapeos de FT-2, funcionalidades de FT-3 ni funcionalidades asignadas a Ignacio. Confirmar la titularidad antes de implementar.
- Codificar cada mensaje como dirección, string de tags con coma inicial y argumentos en el mismo orden. Enteros y bits de floats de 32 bits son big-endian; strings UTF-8 terminados en NUL y rellenados con NUL hasta múltiplos de cuatro. En este contrato todos los strings son ASCII y sus bytes deben coincidir exactamente con el `US_ASCII` de `BandClient.java`. Solo se admiten argumentos `Integer`, `Float` y `String`.
- `/s_new`: synth name, node ID, `addAction = 0`, target group `1`, luego pares control/value en orden: piano `note`, `durMs`, `third`; bajo `note`, `durMs`; órgano `freq`, `r1`, `r2`, `r3`, `a1`, `a2`, `a3`. Omitir `amp` para conservar los defaults de `myband.sc`. `/n_set`: node ID, `"gate"`, `0`.
- Usar IDs globales desde `10000`; incrementar por cada `/s_new` de cualquier synth, incluso si el envío falla, como en el cliente del profesor. Para el órgano, mantener el mapa MIDI note → node ID activo. Un `noteOn` repetido envía primero el `/n_set` de liberación y después crea el nuevo nodo. Un `noteOff` inactivo es una operación vacía. Para coincidir con `BandClient.java`, retirar la asociación del mapa antes de enviar `/n_set`; si el envío falla, propagar el error y no restaurar la asociación.
- Convertir la nota MIDI del órgano con `(float) (440.0 * Math.pow(2.0, (pNota - 69) / 12.0))`; enviar `freq` como float OSC. Validar que ratios y amplitudes tengan exactamente tres elementos.
- Serializar de manera segura la asignación de IDs, el mapa del órgano y los envíos para llamadas concurrentes desde futuros lectores de FT-2. Usar sincronización simple sobre el estado compartido; no agregar una cola o subsistema de concurrencia independiente.
- Usar `DatagramSocket` y `DatagramPacket` de la biblioteca estándar. No implementar handshake, reintentos ni detección de servidor remoto: convertir `IOException` local en `ConexionSynthException`. Al cerrar, intentar liberar las notas activas y cerrar siempre el socket; no silenciar errores de liberación. Mantener el comportamiento de red compatible con el cliente del profesor.
- Capturar `instanteMs` desde `System.nanoTime()` convertido a milisegundos al despachar cada datagrama exitoso. No usar reloj de pared. Al grabar, la capa futura persiste `instanteMs - instanteInicioGrabacionMs`.
- Mantener números, strings de protocolo, host/puerto y valores de prueba en `IConstants.java`; Java 25, paquetes y nombres según las guías, parámetros con prefijo `p`, campos `private`.
- En SuperCollider IDE, evaluar el bloque parentizado que define `~myband` sin las dos expresiones ejecutables finales y luego invocar `~myband.load` una sola vez; esperar el mensaje de SynthDefs cargados antes de ejecutar `Main`. Si se evalúa el archivo completo, sus líneas finales inician `~myband.load` y `~myband.test`; esperar que terminen antes de `Main` y no lanzar otra carga/prueba concurrente. Aceptar `host` y `port` opcionales como en el cliente del profesor; por defecto usar `IConstants`.
- `Main` reproduce la secuencia de referencia: piano (48, 1500 ms, mayor), (57, 1500 ms, menor), (53, 2000 ms); bajo 36, 43, 45 y 41 por 450 ms; órgano 60, 64 y 67 con ratios/amplitudes de `BandClient.java`, luego note-off. Los retardos de demostración se mantienen en constantes.
- Confirmar con el equipo la titularidad antes de implementar; limitar los cambios a los archivos de FT-1 y no modificar funcionalidades asignadas a Ignacio ni contratos que deban consumir FT-2/FT-3.

## Business workflow

1. Iniciar SuperCollider, definir `~myband`, ejecutar `~myband.load` una vez y esperar que los SynthDefs estén cargados; luego ejecutar `Main` con el destino por defecto u overrides opcionales.
2. `ClienteSuperCollider` codifica y envía el datagrama correspondiente a cada comando.
3. Para cada `/s_new`, asignar el siguiente ID global; almacenar el ID del órgano por nota.
4. En un órgano repetido, enviar primero `/n_set` al nodo previo; en note-off, retirar su asociación y enviar `/n_set` al node ID obtenido.
5. Capturar el timestamp monotónico de cada datagrama exitoso, crear su `EventoMusical` y notificar observadores.
6. Al cerrar, enviar note-off a las notas de órgano activas y cerrar el socket; propagar errores locales sin afirmar que el servidor recibió los datagramas.
