package powerband.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.helpers.LectorOsc;
import powerband.helpers.TransporteOscFalso;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class ComandoOrganoTest {
    @Test
    void convierteNotaMidiAPuntoFlotanteYGeneraEventosNoteOnYNoteOff() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);

            organo.noteOn(
                69,
                new float[] { IConstants.RATIO_DEFAULT_1, IConstants.RATIO_DEFAULT_2, IConstants.RATIO_DEFAULT_3 },
                new float[] {
                    IConstants.AMPLITUD_DEFAULT_1,
                    IConstants.AMPLITUD_DEFAULT_2,
                    IConstants.AMPLITUD_DEFAULT_3
                }
            );
            List<Object> argumentosOn = LectorOsc.leerArgumentos(transporte.obtenerMensajes().get(0));
            assertEquals(
                List.of(
                    IConstants.SYNTH_ORGANO,
                    IConstants.OSC_PRIMER_ID_NODO,
                    IConstants.OSC_ADD_TO_HEAD,
                    IConstants.OSC_GRUPO_DEFAULT,
                    IConstants.CONTROL_FRECUENCIA,
                    440.0f,
                    IConstants.CONTROL_RATIO_1,
                    IConstants.RATIO_DEFAULT_1,
                    IConstants.CONTROL_RATIO_2,
                    IConstants.RATIO_DEFAULT_2,
                    IConstants.CONTROL_RATIO_3,
                    IConstants.RATIO_DEFAULT_3,
                    IConstants.CONTROL_AMPLITUD_1,
                    IConstants.AMPLITUD_DEFAULT_1,
                    IConstants.CONTROL_AMPLITUD_2,
                    IConstants.AMPLITUD_DEFAULT_2,
                    IConstants.CONTROL_AMPLITUD_3,
                    IConstants.AMPLITUD_DEFAULT_3
                ),
                argumentosOn
            );

            organo.noteOff(69);
            List<byte[]> mensajes = transporte.obtenerMensajes();
            assertEquals(IConstants.OSC_N_SET, LectorOsc.leerDireccion(mensajes.get(1)));
            assertEquals(
                List.of(IConstants.OSC_PRIMER_ID_NODO, IConstants.CONTROL_GATE, IConstants.VALOR_GATE_APAGADO),
                LectorOsc.leerArgumentos(mensajes.get(1))
            );
            assertEquals(2, eventos.size());
            assertEquals(EventoMusical.Accion.NOTE_ON, eventos.get(0).getAccion());
            assertEquals(69, eventos.get(0).getNota());
            assertEquals(IConstants.RATIO_DEFAULT_2, eventos.get(0).getRatio2());
            assertEquals(IConstants.AMPLITUD_DEFAULT_3, eventos.get(0).getAmplitud3());
            assertNull(eventos.get(0).getDuracionMs());
            assertEquals(EventoMusical.Accion.NOTE_OFF, eventos.get(1).getAccion());
        }
    }

    @Test
    void retriggerLiberaElNodoAnteriorAntesDeCrearElSiguiente() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);

            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);

            List<byte[]> mensajes = transporte.obtenerMensajes();
            assertEquals(3, mensajes.size());
            assertEquals(IConstants.OSC_S_NEW, LectorOsc.leerDireccion(mensajes.get(0)));
            assertEquals(IConstants.OSC_N_SET, LectorOsc.leerDireccion(mensajes.get(1)));
            assertEquals(IConstants.OSC_S_NEW, LectorOsc.leerDireccion(mensajes.get(2)));
            assertEquals(IConstants.OSC_PRIMER_ID_NODO + 1, LectorOsc.leerArgumentos(mensajes.get(2)).get(1));
            assertEquals(3, eventos.size());
            assertEquals(EventoMusical.Accion.NOTE_ON, eventos.get(0).getAccion());
            assertEquals(EventoMusical.Accion.NOTE_OFF, eventos.get(1).getAccion());
            assertEquals(EventoMusical.Accion.NOTE_ON, eventos.get(2).getAccion());
        }
    }

    @Test
    void noteOffDeNotaInactivaNoEnviaNiNotifica() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            new ComandoOrgano(cliente).noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);

            assertEquals(0, transporte.obtenerMensajes().size());
            assertEquals(0, eventos.size());
        }
    }

    @Test
    void rechazaCantidadIncorrectaDeParciales() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente(new TransporteOscFalso())) {
            ComandoOrgano organo = new ComandoOrgano(cliente);
            assertThrows(
                IllegalArgumentException.class,
                () -> organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1, new float[] { 1.0f }, new float[] { 1.0f })
            );
        }
    }

    @Test
    void actualizaParametrosDelNodoActivoYConservaSuIdentidad() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);
            float[] amplitudes = {
                IConstants.AMPLITUD_MINIMA_ORGANO,
                IConstants.AMPLITUD_DEFAULT_2,
                IConstants.AMPLITUD_MAXIMA_ORGANO
            };

            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            organo.ejecutar(
                EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                IConstants.NOTA_ORGANO_PRUEBA_1,
                null,
                null,
                null,
                amplitudes
            );

            List<byte[]> mensajes = transporte.obtenerMensajes();
            assertEquals(2, mensajes.size());
            assertEquals(IConstants.OSC_N_SET, LectorOsc.leerDireccion(mensajes.get(1)));
            assertEquals(
                List.of(
                    IConstants.OSC_PRIMER_ID_NODO,
                    IConstants.CONTROL_AMPLITUD_1,
                    amplitudes[0],
                    IConstants.CONTROL_AMPLITUD_2,
                    amplitudes[1],
                    IConstants.CONTROL_AMPLITUD_3,
                    amplitudes[2]
                ),
                LectorOsc.leerArgumentos(mensajes.get(1))
            );
            assertEquals(2, eventos.size());
            EventoMusical evento = eventos.get(1);
            assertEquals(EventoMusical.Accion.ACTUALIZAR_PARAMETROS, evento.getAccion());
            assertEquals(IConstants.SYNTH_ORGANO, evento.getTipoSynth());
            assertEquals(IConstants.NOTA_ORGANO_PRUEBA_1, evento.getNota());
            assertNull(evento.getDuracionMs());
            assertNull(evento.getTercera());
            assertNull(evento.getRatio1());
            assertNull(evento.getRatio2());
            assertNull(evento.getRatio3());
            assertEquals(amplitudes[0], evento.getAmplitud1());
            assertEquals(amplitudes[1], evento.getAmplitud2());
            assertEquals(amplitudes[2], evento.getAmplitud3());

            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_2);
            mensajes = transporte.obtenerMensajes();
            assertEquals(
                IConstants.OSC_PRIMER_ID_NODO,
                LectorOsc.leerArgumentos(mensajes.get(2)).get(0)
            );
            assertEquals(
                IConstants.OSC_PRIMER_ID_NODO + 1,
                LectorOsc.leerArgumentos(mensajes.get(3)).get(1)
            );
        }
    }

    @Test
    void actualizacionDeNotaInactivaNoEnviaNiNotifica() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            new ComandoOrgano(cliente).ejecutar(
                EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                IConstants.NOTA_ORGANO_PRUEBA_1,
                null,
                null,
                null,
                new float[] {
                    IConstants.AMPLITUD_DEFAULT_1,
                    IConstants.AMPLITUD_DEFAULT_2,
                    IConstants.AMPLITUD_DEFAULT_3
                }
            );

            assertEquals(0, transporte.obtenerMensajes().size());
            assertEquals(0, eventos.size());
        }
    }

    @Test
    void rechazaAmplitudesInvalidasSinEnviarNiNotificar() throws Exception {
        TransporteOscFalso transporte = new TransporteOscFalso();
        List<EventoMusical> eventos = new ArrayList<>();
        try (ClienteSuperCollider cliente = crearCliente(transporte)) {
            cliente.agregarObservador(eventos::add);
            ComandoOrgano organo = new ComandoOrgano(cliente);
            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);

            float[][] amplitudesInvalidas = {
                null,
                new float[0],
                new float[] { IConstants.AMPLITUD_PRUEBA_1, IConstants.AMPLITUD_PRUEBA_2 },
                new float[] {
                    IConstants.AMPLITUD_PRUEBA_1,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3,
                    IConstants.AMPLITUD_PRUEBA_4
                },
                new float[] { Float.NaN, IConstants.AMPLITUD_PRUEBA_2, IConstants.AMPLITUD_PRUEBA_3 },
                new float[] {
                    Float.POSITIVE_INFINITY,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                },
                new float[] {
                    Float.NEGATIVE_INFINITY,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                },
                new float[] {
                    -IConstants.AMPLITUD_MAXIMA_ORGANO,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                },
                new float[] {
                    IConstants.AMPLITUD_MAXIMA_ORGANO + IConstants.AMPLITUD_MAXIMA_ORGANO,
                    IConstants.AMPLITUD_PRUEBA_2,
                    IConstants.AMPLITUD_PRUEBA_3
                }
            };

            for (float[] amplitudes : amplitudesInvalidas) {
                assertThrows(
                    IllegalArgumentException.class,
                    () -> organo.ejecutar(
                        EventoMusical.Accion.ACTUALIZAR_PARAMETROS,
                        IConstants.NOTA_ORGANO_PRUEBA_1,
                        null,
                        null,
                        null,
                        amplitudes
                    )
                );
            }

            assertEquals(1, transporte.obtenerMensajes().size());
            assertEquals(1, eventos.size());
            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
            assertEquals(2, transporte.obtenerMensajes().size());
        }
    }

    private ClienteSuperCollider crearCliente(TransporteOscFalso pTransporte) throws Exception {
        return new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            pTransporte,
            new AtomicLong()::get
        );
    }
}
