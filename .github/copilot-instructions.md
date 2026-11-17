# Copilot project instructions

## Project description

Power Band es un programa de creación e interacción musical. Desde Java se invocan tres sintetizadores que corren en SuperCollider (`classicPiano`, `stringBass` y `organ70s`, definidos en `supercollider/myband.sc`). Cada sintetizador se controla con un instrumento físico distinto, que nunca es el teclado ni el mouse de la computadora:

| Instrumento (dispositivo) | Sintetizador | Cómo se toca |
|---|---|---|
| Control de PlayStation (USB o Bluetooth) | `organ70s` | Cada botón es una nota: suena mientras está presionado (`noteOn` / `noteOff`). Las palancas cambian las amplitudes de los parciales. |
| Micrófono | `stringBass` | Cada golpe o sonido fuerte dispara una nota del bajo. La nota depende del tono estimado. |
| Mezcladora de DJ (MIDI por USB) | `classicPiano` | Cada botón o pad dispara un acorde. Un fader define la duración y un control elige mayor o menor. |

El programa tiene además un editor musical (ventana Swing) que muestra en tiempo real los comandos que los instrumentos envían a SuperCollider, y permite grabar, listar, cargar y reproducir música.

Al iniciar, el programa activa los instrumentos, se conecta a SuperCollider y abre el editor.

### Grabación y archivos

- Al grabar, cada comando enviado a SuperCollider se guarda en tiempo real con su instante relativo al inicio de la grabación, el synth y sus parámetros.
- Al terminar, el usuario da un nombre único. Si ya existe, se rechaza y se pide otro.
- `recordings.music`: todas las grabaciones, una tras otra. Cada una tiene un registro de encabezado (nombre, posición de inicio y de fin de sus comandos dentro del archivo, fecha) seguido de sus registros de comandos. Todos los registros tienen el mismo tamaño fijo.
- `recordings.index`: registros de tamaño fijo con el nombre (longitud fija) y la posición del encabezado en `recordings.music`, ordenados por nombre.
- `recordings.list`: el nombre de cada grabación y su fecha y hora, en orden cronológico.
- Los archivos se escriben en binario (`DataOutputStream`, `RandomAccessFile`) para mantener el tamaño exacto de los registros.
- Al terminar cada grabación, el índice se ordena con Balanced Merge Sort sobre archivos temporales en disco.
- Las grabaciones se buscan en `recordings.index` con búsqueda binaria en disco (`seek` por número de registro).
- Está prohibido ordenar o recorrer en memoria arreglos o listas con los registros indexados: todo ocurre sobre los archivos.

### Reproducción

- Cargar muestra las grabaciones en orden cronológico, leídas de `recordings.list`.
- Al elegir una, su nombre se busca en el índice con búsqueda binaria, se lee su encabezado y se reproducen sus comandos respetando los intervalos de tiempo grabados, para que suene idéntica.

## Design rules

- **Patrón Bridge (obligatorio):** la jerarquía de instrumentos (abstracción: `Instrumento` y sus subclases por dispositivo) está separada de la jerarquía de comandos que invocan a los synths (implementación: `ComandoSynth` y sus subclases por synth). Un instrumento tiene un `ComandoSynth` y no sabe nada de OSC ni de SuperCollider. Cualquier instrumento puede usarse con cualquier synth.
- La comunicación con SuperCollider es por UDP con mensajes OSC (`/s_new`, `/n_set`) a `scsynth` en `127.0.0.1:57110`, a partir del `BandClient.java`.
- El editor se entera de cada comando enviado a SuperCollider sin que los instrumentos lo conozcan (Observer).
- Excepciones propias para los errores del programa (conexión, dispositivo no disponible, nombre repetido, grabación no encontrada, archivo dañado), que heredan de una excepción base del proyecto.
- Ningún valor quemado en el código: todos los números, textos, rutas y puertos van en `util/IConstants.java`.
- Uso intensivo de herencia y polimorfismo. Encapsulamiento: campos `private`.

## Tech stack

- Java (la versión declarada en `pom.xml`), proyecto Maven.
- Swing para la interfaz del editor.
- JUnit 5 para las pruebas.
- `javax.sound.sampled` para el micrófono y `javax.sound.midi` para la mezcladora (biblioteca estándar).
- `com.badlogicgames.jamepad:jamepad` para el control de PlayStation (única dependencia externa).
- SuperCollider como servidor de sonido, fuera de Java.
- GitHub para control de versiones.

## Conventions

Use Java only. Target the JDK version declared in `pom.xml`.

The entrypoint (`public static void main`) stays in `src/main/java/powerband/Main.java`.
Domain classes stay in `src/main/java/powerband/model/`.
Business logic stays in `src/main/java/powerband/service/`.
Data access (binary files, index, sorting on disk) stays in `src/main/java/powerband/repository/`.
Input and output types (records written to disk, commands) stay in `src/main/java/powerband/dto/`.
Device readers stay in `src/main/java/powerband/instrumentos/`. The Swing editor stays in `src/main/java/powerband/ui/`. Constants stay in `src/main/java/powerband/util/IConstants.java`. Custom exceptions stay in `src/main/java/powerband/excepciones/`.
Packages are lowercase; classes are `PascalCase`; methods and fields are `camelCase`; constants are `UPPER_SNAKE_CASE`.
Every method parameter starts with `p` (`pNota`, `pDuracionMs`).
One public top-level type per file, and the file name matches the type name.
Depend on interfaces for services and repositories; keep fields `private` and expose behavior through methods.
Prefer the standard library. Add a dependency only when the standard library cannot do the job.
Comments and user-facing text in Spanish.
Write class, method, field and variable names in Spanish (for example `Instrumento`, `tocarNota`, `pDuracionMs`).
Write specs and the updates of `.github/instructions/` in Spanish. Keep the section headings of the spec template in English.

For project-specific execution flow and layer instructions, follow the repository instructions under `.github/instructions/`.