---
applyTo: "**/*.{yml,yaml,xml,gradle,sh,bat,ps1,properties}"
description: "Mapa del build Maven y límites actuales de despliegue"
---
# Deploy

## Configuración implementada

El único archivo de build es [`pom.xml`](../../pom.xml): aplicación Java/Maven `powerband:powerband:1.0-SNAPSHOT`, release Java 25, UTF-8 y clase principal `powerband.Main`. Declara Jamepad 2.0.14.0, la exclusión de su `gdx-jnigen-loader` snapshot sustituida por la versión publicada 2.0.0, JUnit Jupiter 5.11.4, Surefire 3.5.2 y Exec Maven Plugin 3.5.0.

La clase de entrada y sus modos están en [`Main.java`](../../src/main/java/powerband/Main.java). Las pruebas automatizadas se ejecutan con `mvn test`. `scsynth` y `supercollider/myband.sc` son requisitos externos para probar audio real; el modo de instrumentos requiere además control y micrófono disponibles.

## Lo que no existe

No se encontraron archivos Dockerfile, workflows de GitHub Actions, manifiestos de Kubernetes, infraestructura como código ni scripts de despliegue. No hay configuración de publicación o pipeline que mantener. Las pruebas actuales no certifican el hardware ni el servidor SuperCollider.

Antes de cambiar versiones, dependencias, plugins o runtime, revisar `pom.xml`, las instrucciones del proyecto y las specs aprobadas afectadas. No modificar configuración de FT-3 a FT-6 ni de funcionalidades asignadas a Ignacio.
