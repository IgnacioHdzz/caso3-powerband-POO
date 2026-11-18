package powerband.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
