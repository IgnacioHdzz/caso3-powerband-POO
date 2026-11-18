package powerband.service;

import powerband.dto.EventoMusical;

@FunctionalInterface
public interface IObservadorComandos {
    void actualizar(EventoMusical pEvento);
}
