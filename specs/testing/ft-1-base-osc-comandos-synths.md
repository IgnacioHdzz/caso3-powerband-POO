---
id: ft-1
layer: testing
status: approved
---

# Pruebas de OSC y eventos de synths

## General description

- Verificar con JUnit 5 la equivalencia del protocolo OSC de FT-1 con `BandClient.java` del profesor, el ciclo de vida de nodos y los eventos observables, sin requerir hardware ni un servidor SuperCollider.

## Inputs

- Mensajes OSC de piano, bajo y órgano, incluido note-off y retrigger.
- Cadenas ASCII de protocolo, enteros y floats de 32 bits.
- Un transporte UDP sustituible que capture datagramas o simule `IOException`.
- Comandos concurrentes y observadores de prueba.

## Outputs

- Pruebas JUnit 5 que comparan bytes OSC completos y verifican IDs, eventos, timestamps y errores.
- Ningún datagrama de prueba enviado a `scsynth`.

## Test criteria

- Comparar vectores dorados byte por byte con el formato de `BandClient.java`: dirección OSC, tags con coma inicial, argumentos en orden, enteros y floats big-endian y strings terminados en NUL.
- Comprobar padding de strings a múltiplos de cuatro, incluidos strings de longitudes que requieren uno, dos, tres y cuatro bytes NUL; verificar que los valores ASCII de protocolo coinciden byte por byte con el cliente del profesor.
- Comprobar tags `i`, `f` y `s`, conversión IEEE-754 de floats y rechazo de tipos OSC no admitidos.
- Comprobar `/s_new` completo con synth name, node ID, `addAction = 0`, grupo `1` y pares clave/valor: piano con `note`, `durMs`, `third`; bajo con `note`, `durMs`; órgano con `freq`, `r1`–`r3`, `a1`–`a3`. Comprobar `/n_set` como node ID, `"gate"`, `0`.
- Usar al menos un vector de piano (nota 48, 1500 ms, tercera 4) y uno de órgano. Aislar el contador para comprobar ID inicial 10000 y los siguientes IDs, también cuando piano y bajo consumen IDs.
- Comprobar que IDs avanzan en cada intento `/s_new`, aun si el transporte simulado falla; el cliente no reutiliza un ID que ya asignó.
- Comprobar MIDI 69 → `440.0f` y otra nota MIDI → frecuencia por la fórmula definida en backend, codificada como float. Verificar que `classicPiano` y `stringBass` reciben nota MIDI, no frecuencia.
- Comprobar que noteOn de órgano guarda el ID correcto; note-off usa el ID de esa nota; note-off inactivo no envía datagrama ni genera evento.
- Comprobar que note-off retira la asociación antes del envío y que un fallo local se propaga sin evento exitoso, manteniendo el comportamiento de estado de `BandClient.java`.
- Comprobar retrigger de la misma nota: primero `/n_set` del ID anterior y evento `NOTE_OFF`, luego `/s_new` con el ID siguiente y evento `NOTE_ON`. Ratios/amplitudes requieren exactamente tres elementos.
- Comprobar eventos `DISPARO` de piano y bajo; los campos que no aplican son `null`. Verificar synth, acción, nota, duración, tercera, ratios y amplitudes donde correspondan, así como la notificación a observadores una vez por cada datagrama exitoso.
- Comprobar timestamp monotónico en milisegundos al despachar cada datagrama, que no dependa de la hora de pared y que un consumidor pueda obtener tiempo relativo mediante `instanteMs - instanteInicioGrabacionMs`.
- Con transporte simulado, comprobar que una `IOException` se propaga como `ConexionSynthException`/`PowerBandException`, no crea evento de éxito y permite cerrar el socket. No inferir disponibilidad del servidor a partir de `send`.
- Comprobar concurrencia de comandos: los IDs no se duplican, el mapa nota→ID no se corrompe y los datagramas completos no se intercalan.
- Comprobar el cierre con notas activas: se intenta enviar `/n_set gate 0` para cada nodo y el socket se cierra aun si un envío de liberación falla; el error se informa y no se silencia.
- Ejecutar `mvn test`; las pruebas no abren sockets reales, no requieren `scsynth` y no implementan pruebas de dispositivos de FT-2.

## Assets

- [`supercollider/myband.sc`](../../supercollider/myband.sc) define los nombres, controles, defaults y envolvente de liberación de los synths.
- `BandClient.java` del profesor debe conservarse disponible como referencia y vector de comparación durante la implementación y prueba de FT-1.
- Guías: `.github/instructions/testing.instructions.md` y `.github/instructions/testing-junit.instructions.md`.

## Implementation guidelines

- Escribir pruebas en `src/test/java/powerband/`, reflejando paquetes; usar JUnit 5 y nombres `<Clase>Test`.
- Probar un codificador en memoria con vectores dorados; para comandos/eventos usar un transporte de prueba mínimo que capture bytes o lance `IOException`. No enviar tráfico de red real.
- Mantener los casos deterministas: inyectar el reloj monotónico o controlar el origen de tiempo en pruebas, sin `Thread.sleep` para medir timestamps.
- Para concurrencia, usar un número acotado de llamadas simultáneas y comprobar unicidad/estado y límites completos de cada datagrama; no probar carga ni añadir herramientas externas.
- No crear pruebas ni fixtures para lectores de control, micrófono, persistencia, editor o funcionalidades asignadas a Ignacio; esos comportamientos pertenecen a features posteriores.

## Business workflow

1. Codificar mensajes en memoria y comparar el arreglo completo con vectores dorados del contrato del profesor.
2. Ejecutar comandos con transporte y reloj sustituidos; inspeccionar los datagramas y eventos capturados.
3. Verificar IDs, nota activa, note-off/retrigger, timestamps y observadores.
4. Simular fallos UDP y concurrencia; comprobar errores explícitos y estado consistente.
5. Ejecutar las pruebas FT-1 con `mvn test`.
