---
applyTo: "src/main/java/**/*.java,src/test/java/**/*.java"
description: "Estado de persistencia y guía para cambios futuros en datos"
---
# Database

## Estado actual

No hay base de datos, esquema, migraciones, DAO ni capa de persistencia implementados. Tampoco existe `src/main/java/powerband/repository/`. Los datos actuales de ejecución son eventos en memoria y mensajes OSC; `src/main/java/powerband/dto/EventoMusical.java` es un DTO, no un registro persistido.

Las referencias a `recordings.music`, `recordings.index`, `recordings.list`, ordenamiento externo y reproducción en `copilot-instructions.md` son requisitos futuros; no corresponden a archivos o clases existentes. No inventar contratos de almacenamiento ni mover lógica de `ClienteSuperCollider` a un repositorio.

## Antes de añadir persistencia

- Confirmar una spec aprobada de persistencia y revisar las instrucciones vigentes en `.github/instructions/`.
- Acordar el formato y ciclo de vida con las funcionalidades que consumen `EventoMusical`; mantener separados DTO, servicio y repositorio.
- Mantener las pruebas deterministas y sin archivos compartidos salvo que la spec requiera persistencia real.

No hay entidades Java ni métodos públicos de base de datos que documentar en el estado actual.
