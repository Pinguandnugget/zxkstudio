# Escotilla Abisal V4 (Fabric 1.21.1)

Puerta circular mural de 3 × 3 bloques, modelo original de 266 piezas, textura militar integrada y dos paneles que se desplazan en direcciones opuestas. La animación dura 32 ticks (1,6 segundos a 20 TPS).

## Instalación y uso

Instalar el mismo JAR de zxkstudio en el servidor y en los clientes, con Fabric API. No se requiere GeckoLib ni un resource pack externo.

- `/give @s zxkstudio:escotilla_abisal`: ítem de instalación para operadores; colocar haciendo clic en el apoyo inferior central. La cara exterior apunta hacia quien coloca.
- `/escotilla crear <x> <y> <z> <north|south|east|west>`: las coordenadas corresponden al bloque central (segundo bloque de altura), y la dirección indica **el exterior**, no la cámara. Debe existir un hueco vertical de 3 × 3 de aire o agua, sin entidades en el plano. Reservar espacio lateral en la pared para ocultar los paneles retraídos.
- `/escotilla manual <x> <y> <z>`: quita el bloqueo del evento y habilita clic derecho para abrir/cerrar.
- `/escotilla grupo <x> <y> <z>`: reinicia la puerta para otro evento.
- `/escotilla estado <x> <y> <z>`: muestra el estado.
- `/escotilla eliminar <x> <y> <z>`: retira la estructura completa.

Los comandos exigen permiso de operador nivel 2. Una vez instalada, cualquiera puede operar una puerta en modo manual. No se puede romper en supervivencia. Clic derecho sobre el marco si está abierta.

## Modo grupal

1. Espera cerrada hasta que exista al menos un jugador en supervivencia conectado y **todos** los jugadores en supervivencia del servidor estén vivos, en la misma dimensión y dentro del radio esférico de 30 bloques.
2. Captura los UUID de ese grupo al abrir. Creativo, aventura y espectador no cuentan. Jugadores que se conecten después no se incorporan al grupo ya iniciado. Cada miembro debe atravesar físicamente la entrada desde el exterior hacia el interior.
3. Cuando todos hayan cruzado y estén a suficiente distancia del umbral por el lado interior, comienza el cierre. Si alguno vuelve al exterior antes del cierre, se reabre. Si hay una entidad viva en el plano, se evita cerrar sobre ella.
4. Al terminar, queda sellada permanentemente en modo de evento; su estado se guarda en el mundo. Desconectar, morir o cambiar de modo durante el acceso no elimina al participante ni provoca un sellado prematuro. Un operador puede recuperar el acceso mediante el modo manual.

## Pruebas de integración pendientes en Minecraft

- Colocar las cuatro orientaciones, comprobar el modelo/textura y que ambos paneles se alejan del centro.
- Dos supervivientes: uno a 31 bloques, otro cerca (no abre); ambos dentro de 30 (abre); solo uno entra (no cierra); ambos entran (sella).
- Un creativo y un espectador lejos no impiden iniciar el evento.
- Volver al exterior antes de terminar el cierre, obstruir con un jugador, desconectar y volver a conectar.
- Reiniciar el servidor con puerta abierta/sellada, activar modo manual, abrir y cerrar.
- Verificar colisiones, iluminación, comportamiento del agua y compatibilidad con Arclight en un mundo de pruebas antes del evento.

La puerta usa una aproximación de colisión circular mediante celdas de cuatro píxeles y una abertura física de 3 × 3; no sustituye las paredes de la cámara.
