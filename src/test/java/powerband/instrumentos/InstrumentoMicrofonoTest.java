package powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import powerband.dto.EventoMusical;
import powerband.helpers.TransporteOscFalso;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

class InstrumentoMicrofonoTest {
    @Test
    void decodificaPCM16LittleEndianYCalculaRmsNormalizado() {
        byte[] pcm = {
            (byte) IConstants.PCM_BYTE_MUESTRA_POSITIVA_BAJA,
            (byte) IConstants.PCM_BYTE_MUESTRA_POSITIVA_ALTA,
            (byte) IConstants.PCM_BYTE_MUESTRA_NEGATIVA_BAJA,
            (byte) IConstants.PCM_BYTE_MUESTRA_NEGATIVA_ALTA
        };
        short[] muestras = InstrumentoMicrofono.decodificarMuestras(pcm, pcm.length);

        assertArrayEquals(
            new short[] {
                IConstants.MUESTRA_PCM_PRUEBA_POSITIVA,
                IConstants.MUESTRA_PCM_PRUEBA_NEGATIVA
            },
            muestras
        );
        assertEquals(
            IConstants.RMS_PCM_PRUEBA,
            InstrumentoMicrofono.calcularRms(muestras),
            IConstants.TOLERANCIA_FLOAT_PRUEBA
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> InstrumentoMicrofono.decodificarMuestras(new byte[] { IConstants.BYTE_NULO }, 1)
        );
    }

    @Test
    void estimaFrecuenciaPorCrucesYSeleccionaNotaDeEscalaMasCercana() {
        short[] muestras = generarTono(
            IConstants.FRECUENCIA_NOTA_BAJO_ESCALA_15_HZ,
            IConstants.AMPLITUD_SENAL_PRUEBA
        );
        double frecuencia = InstrumentoMicrofono.estimarFrecuencia(muestras);

        assertEquals(
            IConstants.FRECUENCIA_NOTA_BAJO_ESCALA_15_HZ,
            frecuencia,
            IConstants.TOLERANCIA_FRECUENCIA_PRUEBA_HZ
        );
        assertEquals(
            IConstants.NOTA_BAJO_ESCALA_15,
            InstrumentoMicrofono.seleccionarNota(frecuencia)
        );
        assertFalse(Double.isFinite(InstrumentoMicrofono.estimarFrecuencia(new short[0])));
        assertNull(InstrumentoMicrofono.seleccionarNota(Double.NaN));
        assertNull(InstrumentoMicrofono.seleccionarNota(IConstants.FRECUENCIA_INVALIDA_BAJA_HZ));
        assertNull(InstrumentoMicrofono.seleccionarNota(IConstants.FRECUENCIA_INVALIDA_ALTA_HZ));
    }

    @Test
    void empateEntreNotasSeleccionaLaNotaMasBaja() {
        double frecuenciaNotaBaja = IConstants.FRECUENCIA_LA4_HZ * Math.pow(
            IConstants.BASE_LOGARITMO_DOS,
            (IConstants.NOTA_BAJO_ESCALA_14 - IConstants.NOTA_MIDI_LA4)
                / IConstants.SEMITONOS_POR_OCTAVA
        );
        double frecuenciaEmpate = frecuenciaNotaBaja * Math.pow(
            IConstants.BASE_LOGARITMO_DOS,
            IConstants.FRACCION_MIDI / IConstants.SEMITONOS_POR_OCTAVA
        );

        assertEquals(
            IConstants.NOTA_BAJO_ESCALA_14,
            InstrumentoMicrofono.seleccionarNota(frecuenciaEmpate)
        );
    }

    @Test
    void disparaEnFlancoDeUmbralRequiereRearmeYRespetaEnfriamiento() throws Exception {
        AtomicLong reloj = new AtomicLong();
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoMicrofono instrumento = new InstrumentoMicrofono(
                comando,
                FuenteAudioFalsa::new,
                reloj::get
            );
            byte[] tono = codificarMuestras(generarTono(
                IConstants.FRECUENCIA_NOTA_BAJO_ESCALA_15_HZ,
                IConstants.AMPLITUD_SENAL_PRUEBA
            ));
            byte[] silencio = new byte[
                IConstants.MUESTRAS_BLOQUE_MICROFONO * IConstants.BYTES_FRAME_MICROFONO
            ];

            instrumento.procesarBloque(tono, tono.length);
            instrumento.procesarBloque(tono, tono.length);
            assertEquals(1, comando.obtenerLlamadas().size());

            instrumento.procesarBloque(silencio, silencio.length);
            reloj.set(IConstants.ENFRIAMIENTO_MICROFONO_MS
                * IConstants.NANOSEGUNDOS_POR_MILISEGUNDO
                - IConstants.NANOSEGUNDOS_POR_MILISEGUNDO);
            instrumento.procesarBloque(tono, tono.length);
            assertEquals(1, comando.obtenerLlamadas().size());

            instrumento.procesarBloque(silencio, silencio.length);
            reloj.set(IConstants.ENFRIAMIENTO_MICROFONO_MS
                * IConstants.NANOSEGUNDOS_POR_MILISEGUNDO);
            instrumento.procesarBloque(tono, tono.length);
            assertEquals(2, comando.obtenerLlamadas().size());
            ComandoSynthFalso.Llamada llamada = comando.obtenerLlamadas().get(1);
            assertEquals(EventoMusical.Accion.DISPARO, llamada.accion());
            assertEquals(IConstants.NOTA_BAJO_ESCALA_15, llamada.nota());
            assertEquals(IConstants.DURACION_DISPARO_BAJO_MS, llamada.duracionMs());
        }
    }

    @Test
    void sonidoBajoElUmbralNoDispara() throws Exception {
        try (ClienteSuperCollider cliente = crearCliente()) {
            ComandoSynthFalso comando = new ComandoSynthFalso(cliente);
            InstrumentoMicrofono instrumento = new InstrumentoMicrofono(
                comando,
                FuenteAudioFalsa::new,
                System::nanoTime
            );
            byte[] tonoBajo = codificarMuestras(generarTono(
                IConstants.FRECUENCIA_NOTA_BAJO_ESCALA_15_HZ,
                IConstants.UMBRAL_RMS_MICROFONO / IConstants.FACTOR_FASE_COMPLETA
            ));

            instrumento.procesarBloque(tonoBajo, tonoBajo.length);
            assertEquals(0, comando.obtenerLlamadas().size());
        }
    }

    static short[] generarTono(double pFrecuenciaHz, double pAmplitud) {
        short[] muestras = new short[IConstants.MUESTRAS_BLOQUE_MICROFONO];
        for (int indice = 0; indice < muestras.length; indice++) {
            double fase = IConstants.FACTOR_FASE_COMPLETA * Math.PI * pFrecuenciaHz * indice
                / IConstants.FRECUENCIA_MUESTREO_MICROFONO_HZ;
            muestras[indice] = (short) (Math.sin(fase) * pAmplitud * Short.MAX_VALUE);
        }
        return muestras;
    }

    private static byte[] codificarMuestras(short[] pMuestras) {
        byte[] pcm = new byte[pMuestras.length * IConstants.BYTES_MUESTRA_MICROFONO];
        for (int indice = 0; indice < pMuestras.length; indice++) {
            pcm[indice * IConstants.BYTES_MUESTRA_MICROFONO] = (byte) pMuestras[indice];
            pcm[indice * IConstants.BYTES_MUESTRA_MICROFONO + IConstants.INDICE_BYTE_ALTO_PCM] =
                (byte) (pMuestras[indice] >> Byte.SIZE);
        }
        return pcm;
    }

    private static ClienteSuperCollider crearCliente() throws Exception {
        return new ClienteSuperCollider(
            IConstants.HOST_SUPERCOLLIDER,
            IConstants.PUERTO_SUPERCOLLIDER,
            new TransporteOscFalso(),
            System::nanoTime
        );
    }

    private static final class FuenteAudioFalsa implements InstrumentoMicrofono.FuenteAudio {
        @Override
        public void abrir() {
        }

        @Override
        public int leer(byte[] pBuffer, int pDesplazamiento, int pLongitud) {
            return 0;
        }

        @Override
        public void cerrar() {
        }
    }
}
