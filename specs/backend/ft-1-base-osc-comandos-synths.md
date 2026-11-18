---
id: ft-1
layer: backend
status: draft
---

# Base Maven, cliente OSC y comandos de synths

## General description

- Establecer la entrada y constantes de la aplicación Maven y la jerarquía Java que invoca los tres synths de SuperCollider mediante el patrón Bridge.
- Enviar comandos OSC a `scsynth` y publicar un `EventoMusical` a los observadores registrados cuando el envío local del comando sea exitoso.
- `Main` ejecuta una secuencia de prueba de piano, bajo y órgano.

## Inputs

- `ComandoPiano`: nota fundamental MIDI, duración en milisegundos y tercera de 4 semitonos (mayor) o 3 (menor).
- `ComandoBajo`: nota MIDI y duración en milisegundos.
- `ComandoOrgano.noteOn`: nota MIDI, tres ratios y tres amplitudes.
- `ComandoOrgano.noteOff`: nota MIDI cuya instancia activa debe localizarse.
- Observadores registrados mediante `IObservadorComandos`.
- Destino OSC definido en `util/IConstants.java`.

## Outputs

- Datagrama OSC UDP a `127.0.0.1:57110`, con dirección `/s_new` o `/n_set` y argumentos compatibles con el synth correspondiente.
- Un `dto/EventoMusical` con instante en milisegundos, synth, acción (`NOTE_ON`, `NOTE_OFF` o `DISPARO`), nota y los campos de duración, tercera, ratios y amplitudes que correspondan. Los parámetros no aplicables se representan como `null`.
- Excepciones `ConexionSynthException` para fallos locales de envío y `PowerBandException` como excepción base del proyecto.

## Test criteria

- La entrada Maven está en `src/main/java/powerband/Main.java` y ejecuta comandos de los tres synths.
- `ComandoPiano` produce `/s_new` para `classicPiano` con nota, duración y tercera mayor o menor válidas.
- `ComandoBajo` produce `/s_new` para `stringBass` con nota y duración.
- `ComandoOrgano.noteOn` produce `/s_new` para `organ70s`, convierte la nota MIDI a `freq` y pasa los tres ratios y amplitudes.
- `ComandoOrgano.noteOff` localiza el ID asignado a la nota y produce `/n_set` con `gate` igual a cero. Un `noteOn` repetido para una nota activa libera la instancia anterior antes de reemplazarla.
- Cada envío local exitoso produce un evento con synth, acción y parámetros correspondientes y lo notifica a los observadores; un fallo de envío no se presenta como un envío exitoso.

## Assets

- [`supercollider/myband.sc`](../../supercollider/myband.sc) define `classicPiano` (`note`, `durMs`, `amp`, `third`), `stringBass` (`note`, `durMs`, `amp`) y `organ70s` (`freq`, `amp`, `gate`, `r1`–`r3`, `a1`–`a3`). Sus funciones convierten la nota MIDI del órgano a frecuencia; `noteOff` baja `gate`.
- **`BandClient.java` no está disponible en el workspace y no fue consultado.** Si el profesor lo entrega, comparar con él el formato y ciclo de conexión OSC antes de aprobar la spec.
- `pom.xml` declara Maven, Java 25, JUnit 5 y Jamepad. Esta funcionalidad no requiere ampliar dependencias.
- Se asume que `scsynth` está ejecutándose y que los SynthDefs ya están cargados. El mecanismo para garantizar esa precondición queda pendiente de revisión.

## Implementation guidelines

- Java 25 según `pom.xml`; nombres, comentarios y mensajes en español, salvo nombres requeridos por el protocolo OSC o por los SynthDefs.
- Crear `src/main/java/powerband/Main.java`, `util/IConstants.java`, `service/ClienteSuperCollider.java`, `service/IObservadorComandos.java`, `model/ComandoSynth.java`, `model/ComandoPiano.java`, `model/ComandoBajo.java`, `model/ComandoOrgano.java`, `dto/EventoMusical.java`, `excepciones/PowerBandException.java` y `excepciones/ConexionSynthException.java`.
- Mantener `ComandoSynth` abstracto y separado del transporte OSC; las subclases encapsulan los parámetros de cada synth. Los instrumentos concretos no forman parte de esta feature.
- Codificar OSC sin dependencia adicional: enteros y floats de 32 bits en orden de red; strings UTF-8 terminados en NUL y completados con bytes NUL hasta múltiplos de cuatro.
- Centralizar direcciones, puerto, nombres, acciones, valores numéricos y demás constantes en `IConstants`. Los parámetros de métodos empiezan con `p`; campos `private`; un tipo público por archivo.
- `ClienteSuperCollider` administra el envío UDP; no afirmar que un datagrama UDP fue recibido o ejecutado por `scsynth` sin confirmación del servidor.
- **Pendiente de revisión:** definir el origen del instante de `EventoMusical` y el rango/estrategia de asignación de IDs OSC de nodo.
- **Pendiente de revisión:** definir cómo se inicia `scsynth` y se cargan los SynthDefs antes de la secuencia de `Main`.
- **Pendiente de revisión:** confirmar la titularidad de las tareas con el equipo y excluir explícitamente cualquier funcionalidad asignada a Ignacio.

## Business workflow

1. `Main` obtiene los parámetros de prueba y prepara el cliente OSC.
2. Cada comando construye el mensaje OSC correspondiente y lo envía a `scsynth`.
3. Para `noteOn`, `ComandoOrgano` conserva el ID de nodo asociado a la nota; para `noteOff`, recupera ese ID y envía `/n_set` con `gate = 0`.
4. Después de un envío local exitoso, se crea el evento musical y se notifica a los observadores registrados.
5. Si el envío local falla, se propaga `ConexionSynthException` como `PowerBandException`.
