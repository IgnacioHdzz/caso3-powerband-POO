package powerband.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.excepciones.ConexionSynthException;
import powerband.helpers.LectorOsc;
import powerband.helpers.TransporteOscFalso;
import powerband.model.ComandoBajo;
import powerband.model.ComandoOrgano;
import powerband.model.ComandoPiano;
import powerband.util.IConstants;

class ClienteSuperColliderTest {
    @Test
    void codificaEnterosFloatYStringsConBytesEsperados() {
        byte[] esperado = bytes(
            "2f 78 00 00 "
                + "2c 69 66 73 00 00 00 00 "
                + "01 02 03 04 "
                + "3f 80 00 00 "
                + "61 62 63 00"
        );

        assertArrayEquals(esperado, ClienteSuperCollider.codificarMensaje("/x", 0x01020304, 1.0f, "abc"));
    }

    @Test
    void rellenaStringsConNulosHastaMultiploDeCuatro() {
        assertEquals(12, ClienteSuperCollider.codificarMensaje("/x", "a").length);
        assertEquals(12, ClienteSuperCollider.codificarMensaje("/x", "ab").length);
        assertEquals(12, ClienteSuperCollider.codificarMensaje("/x", "abc").length);
        assertEquals(16, ClienteSuperCollider.codificarMensaje("/x", "abcd").length);
    }

    @Test
    void rechazaTiposOscNoAdmitidos() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ClienteSuperCollider.codificarMensaje("/x", 1.0d)
        );
    }

    @Test
    void enviaPianoConBytesDelContratoDelProfesorYNotificaEvento() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        AtomicLong reloj = new AtomicLong(IConstants.NANOSEGUNDOS_POR_MILISEGUNDO * 7L);
        List<EventoMusical> eventos = new ArrayList<>();

        try (ClienteSuperCollider cliente = crearCliente(transporte, reloj)) {
            cliente.agregarObservador(eventos::add);
            new ComandoPiano(cliente).disparar(48, 1500, true);

            byte[] esperado = bytes(
                "2f 73 5f 6e 65 77 00 00 "
                    + "2c 73 69 69 69 73 69 73 69 73 69 00 "
                    + "63 6c 61 73 73 69 63 50 69 61 6e 6f 00 00 00 00 "
                    + "00 00 27 10 00 00 00 00 00 00 00 01 "
                    + "6e 6f 74 65 00 00 00 00 00 00 00 30 "
                    + "64 75 72 4d 73 00 00 00 00 00 05 dc "
                    + "74 68 69 72 64 00 00 00 00 00 00 04"
            );
            assertArrayEquals(esperado, transporte.obtenerMensajes().get(0));
            assertEquals(1, eventos.size());
            assertEquals(7L, eventos.get(0).getInstanteMs());
            assertEquals(EventoMusical.Accion.DISPARO, eventos.get(0).getAccion());
            assertEquals(IConstants.SYNTH_PIANO, eventos.get(0).getTipoSynth());
            assertEquals(48, eventos.get(0).getNota());
            assertEquals(1500, eventos.get(0).getDuracionMs());
            assertEquals(IConstants.TERCERA_MAYOR_SEMITONOS, eventos.get(0).getTercera());
        }
    }

    @Test
    void actualizaAmplitudesConBytesOscEsperadosYNotificaEvento() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        AtomicLong reloj = new AtomicLong(IConstants.NANOSEGUNDOS_POR_MILISEGUNDO * 7L);
        List<EventoMusical> eventos = new ArrayList<>();

        try (ClienteSuperCollider cliente = crearCliente(transporte, reloj)) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);
            float[] amplitudes = {
                IConstants.AMPLITUD_MINIMA_ORGANO,
                IConstants.AMPLITUD_DEFAULT_2,
                IConstants.AMPLITUD_MAXIMA_ORGANO
            };
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            cliente.actualizarAmplitudesOrgano(IConstants.NOTA_ORGANO_PRUEBA_1, amplitudes);

            byte[] esperado = bytes(
                "2f 6e 5f 73 65 74 00 00 "
                    + "2c 69 73 66 73 66 73 66 00 00 00 00 "
                    + "00 00 27 10 "
                    + "61 31 00 00 "
                    + "00 00 00 00 "
                    + "61 32 00 00 "
                    + "3f 00 00 00 "
                    + "61 33 00 00 "
                    + "3f 80 00 00"
            );
            List<byte[]> mensajes = transporte.obtenerMensajes();
            assertEquals(2, mensajes.size());
            assertArrayEquals(esperado, mensajes.get(1));
            assertEquals(2, eventos.size());
            EventoMusical evento = eventos.get(1);
            assertEquals(7L, evento.getInstanteMs());
            assertEquals(IConstants.SYNTH_ORGANO, evento.getTipoSynth());
            assertEquals(EventoMusical.Accion.ACTUALIZAR_PARAMETROS, evento.getAccion());
            assertEquals(IConstants.NOTA_ORGANO_PRUEBA_1, evento.getNota());
            assertEquals(amplitudes[0], evento.getAmplitud1());
            assertEquals(amplitudes[1], evento.getAmplitud2());
            assertEquals(amplitudes[2], evento.getAmplitud3());

            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_2);
            assertEquals(
                IConstants.OSC_PRIMER_ID_NODO + 1,
                LectorOsc.leerArgumentos(transporte.obtenerMensajes().get(2)).get(1)
            );
        }
    }

    @Test
    void actualizacionFallidaNoNotificaYConservaElNodoActivo() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong())) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            transporte.fallarUnaVez(new IOException(IConstants.ERROR_ENVIAR_OSC));

            assertThrows(
                ConexionSynthException.class,
                () -> cliente.actualizarAmplitudesOrgano(
                    IConstants.NOTA_ORGANO_PRUEBA_1,
                    new float[] {
                        IConstants.AMPLITUD_PRUEBA_1,
                        IConstants.AMPLITUD_PRUEBA_2,
                        IConstants.AMPLITUD_PRUEBA_3
                    }
                )
            );
            assertEquals(1, transporte.obtenerMensajes().size());
            assertEquals(1, eventos.size());

            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
            List<byte[]> mensajes = transporte.obtenerMensajes();
            assertEquals(2, mensajes.size());
            assertEquals(
                List.of(
                    IConstants.OSC_PRIMER_ID_NODO,
                    IConstants.CONTROL_GATE,
                    IConstants.VALOR_GATE_APAGADO
                ),
                LectorOsc.leerArgumentos(mensajes.get(1))
            );
            assertEquals(EventoMusical.Accion.NOTE_OFF, eventos.get(1).getAccion());
        }
    }

    @Test
    void actualizacionDeNotaInactivaNoEnviaNiNotifica() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong())) {
            cliente.agregarObservador(eventos::add);
            cliente.actualizarAmplitudesOrgano(
                IConstants.NOTA_ORGANO_PRUEBA_1,
                new float[] {
                    IConstants.AMPLITUD_PRUEBA_1,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                }
            );

            assertEquals(0, transporte.obtenerMensajes().size());
            assertEquals(0, eventos.size());
        }
    }

    @Test
    void contadorGlobalConsumeIdInclusoSiFallaUnEnvio() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        try (ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong())) {
            ComandoPiano piano = new ComandoPiano(cliente);
            ComandoBajo bajo = new ComandoBajo(cliente);
            transporte.fallarUnaVez(new IOException(IConstants.ERROR_ENVIAR_OSC));

            assertThrows(ConexionSynthException.class, () -> piano.disparar(48, 1500, true));
            bajo.disparar(36, 450);

            List<Object> argumentos = LectorOsc.leerArgumentos(transporte.obtenerMensajes().get(0));
            assertEquals(IConstants.OSC_PRIMER_ID_NODO + 1, argumentos.get(1));
        }
    }

    @Test
    void cierreLiberaNodosActivosInformaFallosYSiempreCierraCliente() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong());
        ComandoPiano piano = new ComandoPiano(cliente);
        powerband.model.ComandoOrgano organo = new powerband.model.ComandoOrgano(cliente);
        piano.disparar(48, 1500, true);
        organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
        organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_2);
        transporte.fallarUnaVez(new IOException(IConstants.ERROR_ENVIAR_OSC));

        assertThrows(ConexionSynthException.class, cliente::close);
        assertEquals(4, transporte.obtenerMensajes().size());
        assertThrows(
            ConexionSynthException.class,
            () -> cliente.crearNodo(IConstants.SYNTH_BAJO, null, IConstants.CONTROL_NOTA, 36)
        );
        cliente.close();
    }

    @Test
    void asignaIdsUnicosConLlamadasConcurrentes() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        try (ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong())) {
            ComandoBajo bajo = new ComandoBajo(cliente);
            ExecutorService ejecutor = Executors.newFixedThreadPool(IConstants.HILOS_PRUEBA_CONCURRENCIA);
            List<Future<?>> tareas = new ArrayList<>();
            try {
                for (int indice = 0; indice < IConstants.CANTIDAD_COMANDOS_CONCURRENTES; indice++) {
                    tareas.add(ejecutor.submit(() -> {
                        bajo.disparar(IConstants.NOTA_BAJO_PRUEBA_1, IConstants.DURACION_BAJO_PRUEBA_MS);
                        return null;
                    }));
                }
                for (Future<?> tarea : tareas) {
                    tarea.get();
                }
            } finally {
                ejecutor.shutdownNow();
            }

            List<byte[]> mensajes = transporte.obtenerMensajes();
            Set<Integer> ids = new HashSet<>();
            for (byte[] mensaje : mensajes) {
                ids.add((Integer) LectorOsc.leerArgumentos(mensaje).get(1));
            }
            assertEquals(IConstants.CANTIDAD_COMANDOS_CONCURRENTES, mensajes.size());
            assertEquals(IConstants.CANTIDAD_COMANDOS_CONCURRENTES, ids.size());
            assertTrue(ids.contains(IConstants.OSC_PRIMER_ID_NODO));
            assertTrue(ids.contains(IConstants.OSC_PRIMER_ID_NODO + IConstants.CANTIDAD_COMANDOS_CONCURRENTES - 1));
        }
    }

    @Test
    void serializaActualizacionesConNoteOffYRetriggerConcurrentes() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        try (ClienteSuperCollider cliente = crearCliente(transporte, new AtomicLong())) {
            ComandoOrgano organo = new ComandoOrgano(cliente);
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            organo.ejecutar(
                EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                IConstants.NOTA_ORGANO_PRUEBA_1,
                null,
                null,
                null,
                new float[] {
                    IConstants.AMPLITUD_PRUEBA_1,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                }
            );

            ExecutorService ejecutor = Executors.newFixedThreadPool(IConstants.HILOS_PRUEBA_CONCURRENCIA);
            CyclicBarrier barrera = new CyclicBarrier(IConstants.CANTIDAD_PARTICIPANTES_PRUEBA_CONCURRENCIA);
            try {
                Future<?> actualizaciones = ejecutor.submit(() -> {
                    barrera.await();
                    for (int indice = 0; indice < IConstants.CANTIDAD_COMANDOS_CONCURRENTES; indice++) {
                        organo.ejecutar(
                            EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                            IConstants.NOTA_ORGANO_PRUEBA_1,
                            null,
                            null,
                            null,
                            new float[] {
                                IConstants.AMPLITUD_PRUEBA_1,
                                IConstants.AMPLITUD_PRUEBA_2,
                                IConstants.AMPLITUD_PRUEBA_3
                            }
                        );
                    }
                    return null;
                });
                Future<?> liberaciones = ejecutor.submit(() -> {
                    barrera.await();
                    for (int indice = 0; indice < IConstants.CANTIDAD_COMANDOS_CONCURRENTES; indice++) {
                        organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
                        organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
                    }
                    return null;
                });

                actualizaciones.get();
                liberaciones.get();
                organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
            } finally {
                ejecutor.shutdownNow();
            }

            Integer nodoActivo = null;
            int siguienteIdNodo = IConstants.OSC_PRIMER_ID_NODO;
            int cantidadActualizaciones = 0;
            for (byte[] mensaje : transporte.obtenerMensajes()) {
                String direccion = LectorOsc.leerDireccion(mensaje);
                List<Object> argumentos = LectorOsc.leerArgumentos(mensaje);
                if (IConstants.OSC_S_NEW.equals(direccion)) {
                    assertNull(nodoActivo);
                    assertEquals(IConstants.SYNTH_ORGANO, argumentos.get(0));
                    assertEquals(siguienteIdNodo, argumentos.get(1));
                    nodoActivo = siguienteIdNodo;
                    siguienteIdNodo++;
                    continue;
                }

                assertEquals(IConstants.OSC_N_SET, direccion);
                assertEquals(nodoActivo, argumentos.get(0));
                if (argumentos.size() == IConstants.CANTIDAD_ARGUMENTOS_N_SET_LIBERACION) {
                    nodoActivo = null;
                } else {
                    assertEquals(
                        IConstants.CANTIDAD_ARGUMENTOS_N_SET_ACTUALIZACION_AMPLITUDES,
                        argumentos.size()
                    );
                    assertEquals(IConstants.CONTROL_AMPLITUD_1, argumentos.get(1));
                    assertEquals(IConstants.CONTROL_AMPLITUD_2, argumentos.get(3));
                    assertEquals(IConstants.CONTROL_AMPLITUD_3, argumentos.get(5));
                    cantidadActualizaciones++;
                }
            }

            assertNull(nodoActivo);
            assertTrue(cantidadActualizaciones > 0);
        }
    }

    private ClienteSuperCollider crearCliente(TransporteOscFalso pTransporte, AtomicLong pReloj) throws Exception {
        return new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            pTransporte,
            pReloj::get
        );
    }

    private byte[] bytes(String pHexadecimal) {
        String[] partes = pHexadecimal.trim().split("\\s+");
        byte[] resultado = new byte[partes.length];
        for (int indice = 0; indice < partes.length; indice++) {
            resultado[indice] = (byte) Integer.parseInt(partes[indice], IConstants.RADIX_HEXADECIMAL);
        }
        return resultado;
    }
}
