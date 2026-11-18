---
applyTo: "src/main/java/**/*.java"
description: "Mapa de la interfaz de consola y selector de ejecución actualmente implementados"
---
# Frontend

## Alcance actual

No existe interfaz gráfica Swing/JavaFX implementada. La interfaz actual es la línea de comandos en [`Main.java`](../../src/main/java/powerband/Main.java); no confundirla con el editor musical futuro descrito en `copilot-instructions.md`.

`Main.main(String[] pArgs)` es el único método público de la entrada. La selección interna acepta:

- Sin argumentos: demo FT-1 con destino por defecto.
- `host` o `host port`: demo FT-1 conservando compatibilidad posicional.
- `--demo [host] [port]`: demo.
- `--instrumentos [host] [port]`: intenta iniciar control y micrófono independientemente; informa los dispositivos no disponibles y administra el cierre.

El método package-private `seleccionarOpciones(String[] pArgs)` analiza los argumentos; el enum anidado `ModoEjecucion` distingue demo/instrumentos y el record `OpcionesEjecucion` transporta modo, host y puerto. Se prueban en [`MainTest.java`](../../src/test/java/powerband/MainTest.java); no son API pública.

## Dependencias y límites

`Main` presenta la secuencia demo de piano, bajo y órgano usando comandos de `model/`, y crea `InstrumentoControl` con `ComandoOrgano` e `InstrumentoMicrofono` con `ComandoBajo` en modo instrumentos. El cliente OSC requiere que `scsynth` esté previamente preparado para que la demo produzca sonido. El cierre detiene instrumentos y cierra el cliente.

Antes de editar el selector, abrir `Main.java`, [`IConstants.java`](../../src/main/java/powerband/util/IConstants.java), [`Instrumento.java`](../../src/main/java/powerband/instrumentos/Instrumento.java), las implementaciones de instrumentos y la spec aprobada `specs/frontend/ft-2-selector-ejecucion-instrumentos.md`.

No agregar UI gráfica, editor, grabación, reproducción ni controles de MIDI como parte del selector actual. No afirmar que las pruebas del CLI validan dispositivos físicos o funcionalidades de Ignacio.
