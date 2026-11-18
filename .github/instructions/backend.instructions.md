---
applyTo: "src/main/java/**/*.java"
description: "Mapa del backend implementado y contratos que deben revisarse antes de editarlo"
---
# Backend

## Alcance y estructura

El código Java de producción está en `src/main/java/powerband/`. No hay capa `repository/`, persistencia de grabaciones ni base de datos implementadas. Las referencias a grabaciones y editor Swing en `copilot-instructions.md` describen alcance futuro, no código actual.

- `model/`: comandos de synths; `service/`: transporte OSC y observadores; `dto/`: eventos; `instrumentos/`: Bridge de dispositivos; `excepciones/`: excepciones de aplicación; `util/`: constantes.
- FT-1 y FT-2 tienen specs aprobadas en `specs/backend/`; revisar las dos antes de cambiar sus contratos.
- Pruebas automáticas sustituyen transporte, control, micrófono y reloj. No se han validado dispositivos físicos ni integración funcional con SuperCollider o con funcionalidades de Ignacio.

## Contratos y tipos implementados

- [`ComandoSynth`](../../src/main/java/powerband/model/ComandoSynth.java): implementación del Bridge; conserva `ClienteSuperCollider` y define `ejecutar(EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, float[] pRatios, float[] pAmplitudes)`.
- [`ComandoPiano`](../../src/main/java/powerband/model/ComandoPiano.java): `ComandoPiano(ClienteSuperCollider pClienteSuperCollider)`; `disparar(int pNota, int pDuracionMs, boolean pEsMayor)` y `ejecutar(EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, float[] pRatios, float[] pAmplitudes)`.
- [`ComandoBajo`](../../src/main/java/powerband/model/ComandoBajo.java): `ComandoBajo(ClienteSuperCollider pClienteSuperCollider)`; `disparar(int pNota, int pDuracionMs)` y `ejecutar(EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, float[] pRatios, float[] pAmplitudes)`.
- [`ComandoOrgano`](../../src/main/java/powerband/model/ComandoOrgano.java): `ComandoOrgano(ClienteSuperCollider pClienteSuperCollider)`; `noteOn(int pNota)`, `noteOn(int pNota, float[] pRatios, float[] pAmplitudes)`, `noteOff(int pNota)` y `ejecutar(EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, float[] pRatios, float[] pAmplitudes)`. Las acciones de órgano delegan ciclo de vida y actualización de amplitudes al cliente.
- [`ClienteSuperCollider`](../../src/main/java/powerband/service/ClienteSuperCollider.java): `ClienteSuperCollider()`, `ClienteSuperCollider(String pHost, int pPuerto)` y `ClienteSuperCollider(String pHost, int pPuerto, TransporteOsc pTransporte, LongSupplier pRelojMonotonico)`; `agregarObservador(IObservadorComandos pObservador)`, `quitarObservador(IObservadorComandos pObservador)`, `crearEvento(String pTipoSynth, EventoMusical.Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, Float pRatio1, Float pRatio2, Float pRatio3, Float pAmplitud1, Float pAmplitud2, Float pAmplitud3)`, `crearNodo(String pNombreSynth, EventoMusical pEvento, Object... pControles)`, `iniciarOrgano(int pNota, float pFrecuencia, float[] pRatios, float[] pAmplitudes, EventoMusical pEventoNoteOn)`, `apagarOrgano(int pNota)`, `actualizarAmplitudesOrgano(int pNota, float[] pAmplitudes)` y `close()`.
- [`TransporteOsc`](../../src/main/java/powerband/service/TransporteOsc.java): interfaz sustituible; `enviar(byte[] pMensaje)`.
- [`IObservadorComandos`](../../src/main/java/powerband/service/IObservadorComandos.java): Observer; `actualizar(EventoMusical pEvento)`.
- [`EventoMusical`](../../src/main/java/powerband/dto/EventoMusical.java): DTO inmutable; constructor `EventoMusical(long pInstanteMs, String pTipoSynth, Accion pAccion, int pNota, Integer pDuracionMs, Integer pTercera, Float pRatio1, Float pRatio2, Float pRatio3, Float pAmplitud1, Float pAmplitud2, Float pAmplitud3)` y getters `getInstanteMs()`, `getTipoSynth()`, `getAccion()`, `getNota()`, `getDuracionMs()`, `getTercera()`, `getRatio1()`, `getRatio2()`, `getRatio3()`, `getAmplitud1()`, `getAmplitud2()` y `getAmplitud3()`. `Accion` contiene `NOTE_ON`, `NOTE_OFF`, `DISPARO` y `ACTUALIZAR_PARAMETROS`.
- [`Instrumento`](../../src/main/java/powerband/instrumentos/Instrumento.java): abstracción Bridge que contiene un `ComandoSynth`; declara `iniciar()` y `detener()`.
- [`InstrumentoControl`](../../src/main/java/powerband/instrumentos/InstrumentoControl.java): constructor `InstrumentoControl(ComandoSynth pComandoSynth)`; implementa `iniciar()` y `detener()`. Usa `com.studiohartman.jamepad.ControllerManager` y `ControllerState` en hilo propio, traduce botones a NOTE_ON/NOTE_OFF y actualiza amplitudes de notas activas. Sus tipos internos `BotonControl`, `EstadoControl` y `FuenteControl` definen el mapeo y el puerto sustituible; `FuenteJamepad` es el adaptador privado al dispositivo real.
- [`InstrumentoMicrofono`](../../src/main/java/powerband/instrumentos/InstrumentoMicrofono.java): constructor `InstrumentoMicrofono(ComandoSynth pComandoSynth)`; implementa `iniciar()` y `detener()`. Captura PCM mono signed de 16 bits little-endian a 44 100 Hz, calcula RMS y estima nota por cruces por cero. Sus tipos internos `CreadorFuenteAudio` y `FuenteAudio` permiten sustituir la captura; `FuenteAudioSistema` adapta `TargetDataLine`.
- [`Main`](../../src/main/java/powerband/Main.java): entrada `main(String[] pArgs)`; elige demo o dispositivos y administra su cierre.
- [`IConstants`](../../src/main/java/powerband/util/IConstants.java): centraliza protocolo, host/puerto, notas, escalas, parámetros de dispositivos, mensajes y valores de prueba; no tiene métodos públicos.
- [`PowerBandException`](../../src/main/java/powerband/excepciones/PowerBandException.java): excepción base; constructores `PowerBandException(String pMensaje)` y `PowerBandException(String pMensaje, Throwable pCausa)`.
- [`ConexionSynthException`](../../src/main/java/powerband/excepciones/ConexionSynthException.java): error OSC; constructores `ConexionSynthException(String pMensaje, Throwable pCausa)` y `ConexionSynthException(String pMensaje)`.
- [`DispositivoNoDisponibleException`](../../src/main/java/powerband/excepciones/DispositivoNoDisponibleException.java): error de dispositivo; constructores `DispositivoNoDisponibleException(String pMensaje)` y `DispositivoNoDisponibleException(String pMensaje, Throwable pCausa)`.

## Reglas al modificar

- Conservar Bridge: los instrumentos dependen de `ComandoSynth`, no de OSC ni del cliente. El cliente es UDP/OSC para `scsynth`; los mensajes usan `/s_new` y `/n_set`, con enteros y floats big-endian y strings OSC padded.
- `ClienteSuperCollider` notifica a `IObservadorComandos` luego del envío local exitoso. UDP no confirma recepción ni ejecución remota. Los timestamps de eventos son monotónicos en milisegundos.
- Las amplitudes del órgano se actualizan con `ACTUALIZAR_PARAMETROS` sobre el node ID activo; no crear nodos ni repetir NOTE_ON. Mantener serialización con note-off/retrigger.
- Los instrumentos tienen hilos y recursos independientes; fallos de dispositivo se reportan como `DispositivoNoDisponibleException`. No convertir la prueba sin hardware en afirmación de funcionamiento físico.
- Abrir las clases de la sección anterior y las specs aprobadas de la función afectada antes de cambiar contratos. Mantener nombres/parámetros en español y constantes de aplicación en `IConstants`.
