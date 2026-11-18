package powerband;

import powerband.excepciones.PowerBandException;
import powerband.model.ComandoBajo;
import powerband.model.ComandoOrgano;
import powerband.model.ComandoPiano;
import powerband.service.ClienteSuperCollider;
import powerband.util.IConstants;

public final class Main {
    private Main() {
    }

    public static void main(String[] pArgs) throws PowerBandException, InterruptedException {
        String anfitrion = pArgs.length > 0 ? pArgs[0] : IConstants.HOST_SUPERCOLLIDER;
        int puerto = pArgs.length > 1 ? Integer.parseInt(pArgs[1]) : IConstants.PUERTO_SUPERCOLLIDER;

        try (ClienteSuperCollider cliente = new ClienteSuperCollider(anfitrion, puerto)) {
            ComandoPiano piano = new ComandoPiano(cliente);
            ComandoBajo bajo = new ComandoBajo(cliente);
            ComandoOrgano organo = new ComandoOrgano(cliente);

            piano.disparar(IConstants.NOTA_PIANO_PRUEBA_1, IConstants.DURACION_PIANO_PRUEBA_1_MS, true);
            Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_1_MS);
            piano.disparar(IConstants.NOTA_PIANO_PRUEBA_2, IConstants.DURACION_PIANO_PRUEBA_2_MS, false);
            Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_2_MS);
            piano.disparar(IConstants.NOTA_PIANO_PRUEBA_3, IConstants.DURACION_PIANO_PRUEBA_3_MS, true);
            Thread.sleep(IConstants.ESPERA_PIANO_PRUEBA_3_MS);

            int[] notasBajo = {
                IConstants.NOTA_BAJO_PRUEBA_1,
                IConstants.NOTA_BAJO_PRUEBA_2,
                IConstants.NOTA_BAJO_PRUEBA_3,
                IConstants.NOTA_BAJO_PRUEBA_4
            };
            for (int nota : notasBajo) {
                bajo.disparar(nota, IConstants.DURACION_BAJO_PRUEBA_MS);
                Thread.sleep(IConstants.ESPERA_BAJO_PRUEBA_MS);
            }
            Thread.sleep(IConstants.ESPERA_ENTRE_BAJO_Y_ORGANO_MS);

            organo.noteOn(IConstants.NOTA_ORGANO_PRUEBA_1);
            Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_1_MS);
            organo.noteOn(
                IConstants.NOTA_ORGANO_PRUEBA_2,
                new float[] { IConstants.RATIO_PRUEBA_1, IConstants.RATIO_PRUEBA_2, IConstants.RATIO_PRUEBA_3 },
                new float[] { IConstants.AMPLITUD_PRUEBA_1, IConstants.AMPLITUD_PRUEBA_2, IConstants.AMPLITUD_PRUEBA_3 });
            Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_2_MS);
            organo.noteOn(
                IConstants.NOTA_ORGANO_PRUEBA_3,
                new float[] { IConstants.RATIO_PRUEBA_4, IConstants.RATIO_PRUEBA_5, IConstants.RATIO_PRUEBA_6 },
                new float[] { IConstants.AMPLITUD_PRUEBA_1, IConstants.AMPLITUD_PRUEBA_4, IConstants.AMPLITUD_PRUEBA_5 });
            Thread.sleep(IConstants.ESPERA_ORGANO_PRUEBA_3_MS);
            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_1);
            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_2);
            organo.noteOff(IConstants.NOTA_ORGANO_PRUEBA_3);
            Thread.sleep(IConstants.ESPERA_LIBERACION_ORGANO_MS);
        }
    }
}
